package activitystreamer.client;

import activitystreamer.util.JsonCreator;
import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

public class ClientControl {
    private static final Logger log = LogManager.getLogger();
    private static ClientControl clientSolution = new ClientControl();
    private boolean term;
    private boolean shouldLoginAfterRego; // only applicable to initial rego
    private ClientGui clientGui;

    private Connection connection;


    public static ClientControl getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientControl();
        }
        return clientSolution;
    }

    public void start() {
        // GUI starts on the EDT
        clientGui = new ClientGui();

        reconnect(Settings.getRemoteHostname(), Settings.getRemotePort());
        if (isConnected()) {
            initialLogin();
        }

        if (!term) {
            clientGui.setup();
        }
    }

    /**
     * Called by shutdown hook after System.exit invoked
     * Should be synchronous and therefore have enough time for connections to close
     */
    public void cleanup() {
        log.info("INFO - cleaning up connection for shutdown");
        disconnect();

        // some extra grace time in case of asynchronous closing
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {

        }
    }

    // METHODS INVOKED BY UI

    /**
     * Writes activity message to connection
     *
     * @param activityObject JSONObject to be packaged into an activity message
     * @return true if socket is open and write occurred
     */
    public boolean sendActivityObject(JSONObject activityObject) {
        boolean result = connection.writeMsg(JsonCreator.activityMessage(Settings.getUsername(), Settings.getSecret(), activityObject));
        if (result) {
            outputInfo(log, "ACTION - sent activity object : " + activityObject);
        } else {
            outputError(log, "ERROR - connection closed, could not send activity object");
        }
        return result;
    }


    /**
     * Connect to given hostname and port. Disconnects any existing connection first.
     *
     * @param hostname hostname string, null defaults to loopback address
     * @param port     int between 0 and 65535
     */
    public void reconnect(String hostname, int port) {
        try {

            // disconnect existing connection
            if (connection != null && connection.isOpen()) {
                disconnect();
            }

            // make new socket connection
            outputInfo(log, "INFO - connecting to " + hostname + ":" + port);
            Socket socket = new Socket(hostname, port);
            connection = new Connection(socket);

            // if no exception thrown, save new hostname and port
            Settings.setRemoteHostname(socket.getInetAddress().getHostName());
            Settings.setRemotePort(port);

            outputInfo(log, "INFO - connected to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort());
            clientGui.connected(Settings.getRemoteHostname(), Settings.getRemotePort());
        } catch (IOException e) {
            outputError(log, "ERROR - failed connection to " + hostname + ":" + port + " : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            outputError(log, "ERROR - port number out of range (0-65535)");
        }
    }

    /**
     * Perform initial login
     * As per LMS :
     * - if the user the gives no username on the command line arguments then login as anonymous on start
     * - if the user gives a username and secret then login on start
     * - if the user gives only a username but no secret then first register the user, by generating a new secret
     * (print to screen for subsequent use), then login after/if receiving register success
     */
    private void initialLogin() {


        String username = Settings.getUsername();
        String secret = Settings.getSecret();

        // if no -u was given, default is anonymous
        if ("anonymous".equals(username)) {
            Settings.setSecret("");
            login("anonymous", "");
        } else if (secret != null) {
            // if -u and -s given
            login(username, secret);
        } else {
            // if only -u given
            String newSecret = Settings.nextSecret();
            Settings.setSecret(newSecret);

            outputInfo(log, "INFO - no secret provided, random one generated : " + newSecret);
            shouldLoginAfterRego = true;
            register(username, newSecret);
        }

    }

    /**
     * Send login attempt, if username and secret both not null
     *
     * @param username username as string, should not be null
     * @param secret   secret as string, should not be null
     */
    public void login(String username, String secret) {

        if ("anonymous".equals(username)) {
            Settings.setUsername(username);
            Settings.setSecret(secret);
            connection.writeMsg(JsonCreator.login(username, ""));
        } else if (username != null && secret != null) {
            Settings.setUsername(username);
            Settings.setSecret(secret);
            connection.writeMsg(JsonCreator.login(username, secret));
        } else {
            outputError(log, "ERROR - username or secret was null");
        }

    }

    /**
     * Send registration attempt if not anonymous, username and secret not null
     *
     * @param username username as string, should not be null
     * @param secret   secret as string, should not be null
     */
    public void register(String username, String secret) {
        if ("anonymous".equals(username)) {
            outputError(log, "ERROR - cannot register using anonymous");
        } else if (username != null && secret != null) {
            Settings.setUsername(username);
            Settings.setSecret(secret);
            outputInfo(log, "INFO - sending registration attempt");
            connection.writeMsg(JsonCreator.register(username, secret));
        } else {
            outputError(log, "ERROR - username or secret was null");
        }
    }


    /**
     * Close connection to server and release associated resources
     */
    public void disconnect() {

        if (isLoggedIn()) {
            boolean result = connection.writeMsg(JsonCreator.logout());
            if (result) {
                outputInfo(log, "INFO - sent logout message");
            } else {
                outputInfo(log, "INFO - failed to send logout message");
            }

            connection.setLoggedIn(false);
        }

        if (connection != null) {
            connection.closeCon();
        }

        if (!term) {
            clientGui.loggedOut();
            clientGui.disconnected();
        }
    }

    // CONNECTION METHODS

    /**
     * Used by a closing connection to notify Control to cleanup
     *
     * @param socket reference to associated socket, for information
     */
    public void connectionClosed(Socket socket) {

        if (!term) {
            clientGui.disconnected();
        }
    }

    /**
     * Called by a connection to process data strings received
     *
     * @param connection reference to connection calling this method
     * @param data       data received by socket decoded to string
     * @return true if connection should terminate, either based on data received, or if data cannot be parsed
     * to valid JSON. false otherwise.
     */
    public synchronized boolean processData(Connection connection, String data) {


        log.debug("DEBUG - received : " + data);

        try {
            JSONObject json = new JSONObject(data);
            String command = json.getString("command");

            switch (command) {

                case "INVALID_MESSAGE": {
                    String info = json.getString("info");
                    return termConnection(null, "INVALID_MESSAGE : " + info);
                }

                case "AUTHENTICATION_FAIL": {
                    String info = json.getString("info");
                    disconnect();
                    return termConnection(null,
                            "AUTHENTICATION_FAIL -  to remote host " + Settings.getRemoteHostname() +
                                    ":" + Settings.getRemotePort() +
                                    " using username " + Settings.getUsername() +
                                    " and secret " + Settings.getSecret() +
                                    " : " + info);
                }

                case "LOGIN_SUCCESS": {

                    String info = json.getString("info");
                    connection.setLoggedIn(true);
                    if (!term) {
                        clientGui.loggedIn();
                    }
                    outputInfo(log, "LOGIN_SUCCESS - " + info);
                    break;
                }

                case "REDIRECT": {

                    String hostname = json.getString("hostname");
                    int port = json.getInt("port");
                    outputInfo(log, "REDIRECT - " + hostname + ":" + port);
                    reconnect(hostname, port);
                    login(Settings.getUsername(), Settings.getSecret());

                    break;
                }

                case "LOGIN_FAILED": {
                    String info = json.getString("info");
                    outputInfo(log, "LOGIN_FAILED - " + info);

                    // Specification indicates that only server closes connection
                    // return termConnection(null, "LOGIN_FAILED - " + info);
                    break;
                }

                case "ACTIVITY_BROADCAST": {
                    JSONObject activity = json.getJSONObject("activity");

                    // *BUG* slightly ambiguous whether this check is implied in specification for client, only server
                    // it should be considered a required field, but apparently isn't
//                    String user = activity.getString("authenticated_user");
                    outputInfo(log, "ACTIVITY_BROADCAST - " + activity.toString());

                    if (!term) {
                        clientGui.appendActivity(activity);
                    }
                    break;
                }

                case "REGISTER_SUCCESS": {

                    String info = json.getString("info");
                    outputInfo(log, "REGISTER_SUCCESS - " + info);

                    if (shouldLoginAfterRego) {
                        shouldLoginAfterRego = false;
                        login(Settings.getUsername(), Settings.getSecret());
                    }

                    break;
                }

                case "REGISTER_FAILED": {
                    String info = json.getString("info");

                    if (shouldLoginAfterRego) {
                        shouldLoginAfterRego = false;
                    }
                    outputInfo(log, "LOGIN_FAILED - " + info);
                    // Likewise, specification says only server actively closes connection
//                    return termConnection(null, "LOGIN_FAILED - " + info);
                    break;
                }

                default: {
                    String error = "received message with unknown command";
                    return termConnection(JsonCreator.invalidMessage(error), "ERROR - " + error);
                }

            }
        } catch (JSONException e) {
            String error = "JSON parse exception : " + e.getMessage();
            return termConnection(JsonCreator.invalidMessage(error), "ERROR - " + error);
        }


        return false;
    }

    // UTILITY METHODS

    /**
     * Utility method to reply to server and log error if connection should terminate based on data received
     *
     * @param messageToServer message to send to server if not null
     * @param errorMessage    message to log, if not null
     * @return true if connection should terminate, false otherwise
     */
    private boolean termConnection(String messageToServer, String errorMessage) {
        if (messageToServer != null) {
            connection.writeMsg(messageToServer);
        }

        if (errorMessage != null) {
            outputError(log, errorMessage);
        }
        return true;
    }

    /**
     * Set term if Control should terminate loop and shut down
     *
     * @param term true if Control should terminate
     */
    public void setTerm(boolean term) {
        this.term = term;
    }

    /**
     * Utility method to log error to console and GUI (GUI only if not shutting down or will freeze)
     * Cannot be made static as needs to access term instance variable
     *
     * @param logger logger to use, as may be called from other classes
     * @param error  error string to use
     */
    public void outputError(Logger logger, String error) {
        logger.error(error);
        if (!term) {
            clientGui.appendLog(error);
        }
    }

    /**
     * Utility method to log info to console and GUI (GUI only if not shutting down or will freeze)
     * Cannot be made static as needs to access term instance variable
     *
     * @param logger logger to use, as may be called from other classes
     * @param info   info string to use
     */
    public void outputInfo(Logger logger, String info) {
        logger.info(info);
        if (!term) {
            clientGui.appendLog(info);
        }
    }

    /**
     * @return whether user currently logged in
     */
    public boolean isLoggedIn() {
        return connection != null && connection.isLoggedIn();
    }

    /**
     * @return whether connection to server is open
     */
    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }


}
