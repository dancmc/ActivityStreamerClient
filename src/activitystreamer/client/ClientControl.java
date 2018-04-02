package activitystreamer.client;

import java.io.IOException;
import java.net.Socket;

import activitystreamer.util.JsonCreator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.util.Settings;
import org.json.JSONException;
import org.json.JSONObject;

public class ClientControl extends Thread {
    private static final Logger log = LogManager.getLogger();
    private static ClientControl clientSolution;
    private boolean term;
    private boolean shouldLoginAfterRego;
    private TextFrame textFrame;
    private FrontEnd frontEnd;

    private Connection connection;


    public static ClientControl getInstance() {
        if (clientSolution == null) {
            clientSolution = new ClientControl();
        }
        return clientSolution;
    }

    public ClientControl() {


        textFrame = new TextFrame();
        frontEnd = FrontEnd.getInstance();
        start();
    }

    // UI METHODS

    public void sendActivityObject(JSONObject activityObject) {
        connection.writeMsg(JsonCreator.activityMessage(Settings.getUsername(), Settings.getSecret(), activityObject));
        frontEnd.appendActivities(activityObject, true);
        outputInfo("sent activity object : " + activityObject);
    }


    public void reconnect(String hostname, int port) {
        try {

            if(connection!=null && connection.isOpen()){
                disconnect();
            }

            Socket socket = new Socket(hostname, port);
            connection = new Connection(socket);
            outputError("connected to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort());
            frontEnd.connected(hostname, port);
        } catch (IOException e) {
            outputError("connection to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort() + " failed : " + e.getMessage());
        }
    }

    public void initialLogin(){
        String username = Settings.getUsername();
        String secret = Settings.getSecret();
        if (!"anonymous".equalsIgnoreCase(username)) {
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
            connection.writeMsg(JsonCreator.login(username, ""));
        } else if (username != null && secret != null) {
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
    }

    public void disconnect() {
        connection.closeCon();
        frontEnd.disconnected();
    }

    // CONNECTION METHODS

    public void connectionClosed() {
        frontEnd.appendLog("connection to " + Settings.getRemoteHostname() + ":" + Settings.getRemotePort() + " closed");
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

                case "LOGIN_SUCCESS":{

                    String info = json.getString("info");
                    outputInfo("LOGIN_SUCCESS : "+info);
                    break;
                }

                case "REDIRECT":{

                    String hostname = json.getString("hostname");
                    int port = json.getInt("port");
                    outputInfo("REDIRECT : "+hostname+":"+port);

                    reconnect(hostname, port);
                    break;
                }

                case "LOGIN_FAILED": {
                    String info = json.getString("info");
                    String error = "LOGIN_FAILED : " + info;
                    return termConnection(null, error);
                }

                case "ACTIVITY_BROADCAST":{
                    JSONObject activity = json.getJSONObject("activity");
                    String user = json.getString("authenticated_user");
                    outputInfo("ACTIVITY_BROADCAST : " + activity.toString());

                    frontEnd.appendActivities(activity, false);
                    break;
                }

                case "REGISTER_SUCCESS":{

                    String info = json.getString("info");
                    outputInfo("REGISTER_SUCCESS : "+info);

                    if(shouldLoginAfterRego){
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
        if (errorMessage != null) {
            log.error("connection " + Settings.socketAddress(connection.getSocket()) + " closed : " + errorMessage);
        }
        return true;
    }

    // UTILITY METHODS

    public void setTerm(boolean term) {
        this.term = term;
        if (term) interrupt();
    }

    public void outputError(String error) {
        log.error(error);
        frontEnd.appendLog(error);
    }

    private void outputInfo(String info) {
        log.info(info);
        frontEnd.appendLog(info);
    }


    public void run() {

        reconnect(Settings.getRemoteHostname(), Settings.getRemotePort());

        // as per LMS :
        // if the user the gives no username on the command line arguments then login as anonymous on start
        // if the user gives only a username but no secret then first register the user, by generating a new secret (print to screen for subsequent use), then login after/if receiving register success
        // if the user gives a username and secret then login on start

        initialLogin();


        while (!term) {
            while (!term) {
                // do something with 5 second intervals in between
                try {
                    Thread.sleep(Settings.getActivityInterval());
                } catch (InterruptedException e) {
                    log.info("received an interrupt, system is shutting down");
                    break;
                }
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
