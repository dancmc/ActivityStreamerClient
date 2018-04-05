package activitystreamer.client;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

public class Connection extends Thread {

    private static final Logger log = LogManager.getLogger();

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private BufferedReader inreader;
    private PrintWriter outwriter;

    private boolean loggedIn=false;
    private boolean open=true;
    private boolean term;


    Connection(Socket socket) throws IOException{
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        inreader = new BufferedReader(new InputStreamReader(in));
        outwriter = new PrintWriter(out, true);
        start();
    }

    /*
     * returns true if the message was written, otherwise false
     */
    public boolean writeMsg(String msg) {
        if (open) {
            outwriter.println(msg);
            outwriter.flush();
            return true;
        }
        return false;
    }

    public void closeCon() {
        if (open) {
            log.info("closing connection " + Settings.socketAddress(socket));
            try {
                term = true;
                in.close();
                out.close();
            } catch (IOException e) {
                // already closed?
                log.error("received exception closing the connection " + Settings.socketAddress(socket) + ": " + e);
            }
        }
    }


    public void run() {
        try {
            String data;
            while (!term && (data = inreader.readLine()) != null) {

                term = ClientControl.getInstance().processData(this, data) || term;
            }
            in.close();
            outwriter.close();
        } catch (IOException e) {
            log.error("connection " + Settings.socketAddress(socket) + " closed with exception: " + e);

        } finally {
            if(loggedIn) {
                ClientControl.getInstance().outputInfo(log, "ACTION - passive logout");
                loggedIn = false;
            }
            open = false;
            ClientControl.getInstance().connectionClosed(socket);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
