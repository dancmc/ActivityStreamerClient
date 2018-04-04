package activitystreamer.client;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;

import activitystreamer.util.JsonCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;

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

    public ClientControl() {

        clientGui = new ClientGui();
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//
//            }
//        });


//        for(int i=0;i<40;i++){
//            try {
//                Thread.sleep(1000);
//                textFrame.appendOutputText(JsonCreator.invalidMessage("This is invalid"));
//            } catch (InterruptedException e) {
//                log.info("received an interrupt, system is shutting down");
//                break;
//            }
//        }


        start();
    }

    // UI METHODS

    public boolean sendActivityObject(JSONObject activityObject) {
        boolean result = connection.writeMsg(JsonCreator.activityMessage(Settings.getUsername(), Settings.getSecret(), activityObject));
        outputInfo("sent activity object : " + activityObject);
        return result;
    }


    public void reconnect(String hostname, int port) {
        try {

            if (connection != null && connection.isOpen()) {
                disconnect();
            }
            Socket socket = new Socket(hostname, port);
            connection = new Connection(socket);

            Settings.setRemoteHostname(hostname);
            Settings.setRemotePort(port);
            if (hostname == null) {
                Settings.setRemoteHostname(socket.getInetAddress().getHostName());
            }

            outputInfo("logged out");
            outputInfo("connected to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort());
            clientGui.connected(Settings.getRemoteHostname(), Settings.getRemotePort());
        } catch (IOException e) {
            outputError("connection to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort() + " failed : " + e.getMessage());
        }
    }

    public void initialLogin() {
        String username = Settings.getUsername();
        String secret = Settings.getSecret();
        if ("anonymous".equalsIgnoreCase(username)) {
            login("anonymous", "");
        } else if (secret != null) {
            login(username, secret);
        } else {
            String newSecret = Settings.nextSecret();
            Settings.setSecret(newSecret);
            String secretInfo = "no secret provided, random one generated : " + newSecret;
            outputInfo(secretInfo);
            shouldLoginAfterRego = true;
            register(username, newSecret);
        }
    }

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
            outputError("invalid format for username or secret");
        }

    }

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
        clientGui.loggedOut();
    }

    public void disconnect() {
        connection.closeCon();
        clientGui.disconnected();
    }

    // CONNECTION METHODS

    public void connectionClosed(Socket socket) {
        String info = "connection to " + Settings.socketAddress(socket) + " closed";
        log.info(info);
        clientGui.disconnected();
        clientGui.appendLog(info);
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
        if (term) interrupt();
    }

    public void outputError(String error) {
        log.error(error);
        clientGui.appendLog(error);
    }

    private void outputInfo(String info) {
        log.info(info);
        clientGui.appendLog(info);
    }

    public boolean isLoggedIn() {
        return connection.isLoggedIn();
    }

    public boolean isConnected() {
        return connection.isOpen();
    }


    public void run() {



        // as per LMS :
        // if the user the gives no username on the command line arguments then login as anonymous on start
        // if the user gives only a username but no secret then first register the user, by generating a new secret (print to screen for subsequent use), then login after/if receiving register success
        // if the user gives a username and secret then login on start

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

        log.info("closing connection");
        connection.closeCon();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }
        System.exit(0);

    }


}
