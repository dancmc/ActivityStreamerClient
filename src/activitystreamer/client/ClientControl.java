package activitystreamer.client;

import activitystreamer.util.JsonCreator;
import activitystreamer.util.Settings;
import com.sun.istack.internal.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.Socket;

public class ClientControl extends Thread {
    private static final Logger log = LogManager.getLogger();
    private static ClientControl clientSolution;
    private boolean term;
    private boolean shouldLoginAfterRego;
    private ClientGui clientGui;

    private Connection connection;


    public static ClientControl getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientControl();
        }
        return clientSolution;
    }

    private ClientControl() {

        // GUI starts on the EDT
        clientGui = new ClientGui();
        start();
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
        if(result){
            outputInfo(log,"ACTION - sent activity object : " + activityObject);
        } else {
            outputError(log,"ERROR - connection closed, could not send activity object");
        }
        return result;
    }


    /**
     * Connect to given hostname and port. Disconnects any existing connection first.
     *
     * @param hostname hostname string, null defaults to loopback address
     * @param port int between 0 and 65535
     */
    public void reconnect(@Nullable String hostname, int port) {
        try {

            // disconnect existing connection
            if (connection != null && connection.isOpen()) {
                disconnect();
            }

            // make new socket connection
            Socket socket = new Socket(hostname, port);
            connection = new Connection(socket);

            // if no exception thrown, save new hostname and port
            Settings.setRemoteHostname(socket.getInetAddress().getHostName());
            Settings.setRemotePort(port);

            outputInfo(log, "INFO - connected to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort());
            clientGui.connected(Settings.getRemoteHostname(), Settings.getRemotePort());
        } catch (IOException e) {
            outputError(log, "ERROR - failed connection to " + hostname + ":" + port + " : " + e.getMessage());
        } catch (IllegalArgumentException e){
            outputError(log, "ERROR - port number out of range (0-65535)");
        }
    }

    /**
     * Perform initial login
     * As per LMS :
     *  - if the user the gives no username on the command line arguments then login as anonymous on start
     *  - if the user gives a username and secret then login on start
     *  - if the user gives only a username but no secret then first register the user, by generating a new secret
     *    (print to screen for subsequent use), then login after/if receiving register success
     */
    private void initialLogin() {


        String username = Settings.getUsername();
        String secret = Settings.getSecret();

        // if no -u given, default is anonymous
        if ("anonymous".equalsIgnoreCase(username)) {
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
     * @param username username as string
     * @param secret secret as string
     */
    public void login(String username, String secret) {

        if ("anonymous".equalsIgnoreCase(username)) {
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
     * Send registration attempt if username and secret not null
     *
     * @param username
     * @param secret
     */
    public void register(String username, String secret) {
        if ("anonymous".equalsIgnoreCase(username)) {
            outputError("cannot register using anonymous");
        } else if (username != null && secret != null) {
            connection.writeMsg(JsonCreator.register(username, secret));
        } else {
            outputError("invalid format for username or secret");
        }
    }

    public void logout() {
        connection.writeMsg(JsonCreator.logout());
        outputInfo(Settings.getUsername() + " logging out");

        if (!term) {
            clientGui.loggedOut();
        }

    }

    public void disconnect() {
        connection.closeCon();
        clientGui.disconnected();
    }

    // CONNECTION METHODS

    public void connectionClosed(Socket socket) {
        String info = "connection to " + Settings.socketAddress(socket) + " closed";
        log.info(info);

        if(!term) {
            clientGui.disconnected();
            clientGui.appendLog(info);
        }
    }


    public synchronized boolean processData(Connection connection, String data) {

        log.debug("received : " + data);

        try {
            JSONObject json = new JSONObject(data);
            String command = json.getString("command");

            switch (command) {

                case "INVALID_MESSAGE": {
                    String info = json.getString("info");
                    String error = "INVALID_MESSAGE : " + info;
                    return termConnection(null, error);
                }

                case "AUTHENTICATION_FAIL": {
                    String info = json.getString("info");
                    disconnect();
                    return termConnection(null,
                            "failed authentication to remote host " + Settings.getRemoteHostname() +
                                    ":" + Settings.getRemotePort() +
                                    " using username " + Settings.getUsername() +
                                    " and secret " + Settings.getSecret() +
                                    " : " + info);
                }

                case "LOGIN_SUCCESS": {

                    String info = json.getString("info");
                    connection.setLoggedIn(true);
                    clientGui.loggedIn();
                    outputInfo("LOGIN_SUCCESS : " + info);
                    break;
                }

                case "REDIRECT": {

                    String hostname = json.getString("hostname");
                    int port = json.getInt("port");
                    outputInfo("REDIRECT : " + hostname + ":" + port);

                    reconnect(hostname, port);
                    break;
                }

                case "LOGIN_FAILED": {
                    String info = json.getString("info");
                    String error = "LOGIN_FAILED : " + info;
                    return termConnection(null, error);
                }

                case "ACTIVITY_BROADCAST": {
                    JSONObject activity = json.getJSONObject("activity");
                    String user = activity.getString("authenticated_user");
                    outputInfo("ACTIVITY_BROADCAST : " + activity.toString());

                    clientGui.appendActivity(activity, false);
                    break;
                }

                case "REGISTER_SUCCESS": {

                    String info = json.getString("info");
                    outputInfo("REGISTER_SUCCESS : " + info);

                    if (shouldLoginAfterRego) {
                        shouldLoginAfterRego = false;
                        login(Settings.getUsername(), Settings.getSecret());
                    }

                    break;
                }

                case "REGISTER_FAILED": {
                    String info = json.getString("info");
                    String error = "LOGIN_FAILED : " + info;

                    if (shouldLoginAfterRego) {
                        shouldLoginAfterRego = false;
                    }

                    return termConnection(null, error);
                }

                default: {
                    String error = "Error : Unknown command";
                    return termConnection(JsonCreator.invalidMessage(error), error);
                }

            }
        } catch (JSONException e) {
            String error = "JSON parse exception : " + e.getMessage();
            return termConnection(JsonCreator.invalidMessage(error), error);
        }


        return false;
    }

    private boolean termConnection(String messageToServer, String errorMessage) {
        if (messageToServer != null) {
            connection.writeMsg(messageToServer);
        }

        String error = "connection " + Settings.socketAddress(connection.getSocket()) + " closed : " + errorMessage;
        log.error(error);

        clientGui.appendLog(error);

        return true;
    }

    // UTILITY METHODS

    public void setTerm(boolean term) {
        this.term = term;
    }

    public void outputError(Logger logger, String error) {
        logger.error(error);
        if(!term) {
            clientGui.appendLog(error);
        }
    }

    public void outputInfo(Logger logger, String info) {
        logger.info(info);

        if(!term) {
            clientGui.appendLog(info);
        }
    }

    public boolean isLoggedIn() {
        return connection.isLoggedIn();
    }

    public boolean isConnected() {
        return connection.isOpen();
    }


    public void run() {

        reconnect(Settings.getRemoteHostname(), Settings.getRemotePort());
        initialLogin();

        clientGui.setup();

        while (!term) {
            // do something with 5 second intervals in between
            try {
                Thread.sleep(Settings.getActivityInterval());
            } catch (InterruptedException e) {
                log.info("received an interrupt, system is shutting down");
                break;
            }
        }

        // this is needed in case any method choose to end the program purely through setting term on Control
        // * no such method exists at the moment
        System.exit(0);

    }

    public void exit(){
        logout();
        log.info("closing connection");
        connection.closeCon();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {

        }
    }


}
