package activitystreamer.client;

import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;

/**
 * Project : Activity Streamer Client
 * Author : Daniel Chan (mchan@student.unimelb.edu.au)
 * Date : 22 Mar 2018
 */

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
    private boolean manuallyClosed;


    Connection(Socket socket) throws IOException{
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        inreader = new BufferedReader(new InputStreamReader(in));
        outwriter = new PrintWriter(out, true);
        start();
    }

    /**
     * Utility method to write to socket out
     * @param msg string to be written to socket out
     * @return true if the message was written, otherwise false
     */
    public synchronized boolean writeMsg(String msg) {
        if (open) {
            outwriter.println(msg);
            outwriter.flush();
            return true;
        }
        ClientControl.getInstance().outputError(log, "ERROR - socket closed, failed to write : "+msg);
        return false;
    }

    /**
     * Close the connection and cleanup streams
     */
    public void closeCon() {
        if (open) {
            ClientControl.getInstance().outputInfo(log,
                    "INFO - closing connection " + Settings.socketAddress(socket));
            try {
                term = true;
                // open must be set to false synchronously, cannot rely on subsequent run loop asynchronously closing
                open = false;
                /* this boolean is needed to prevent GUI race condition when manually disconnecting
                   + creating new connection
                */
                manuallyClosed = true;
                socket.close();
            } catch (IOException e) {
                // already closed?
                ClientControl.getInstance().outputError(log,
                        "ERROR - exception closing connection " + Settings.socketAddress(socket) + " : " + e);
            }finally {

                ClientControl.getInstance().connectionClosed(socket);
            }
        }
    }


    public void run() {
        try {
            String data;
            while (!term && (data = inreader.readLine()) != null) {
                term = ClientControl.getInstance().processData(this, data) || term;
            }
            socket.close();
            ClientControl.getInstance().outputInfo(log,
                    "INFO - connection to " + Settings.socketAddress(socket) + " closed");
        } catch (IOException e) {
            ClientControl.getInstance().outputError(log,
                    "ERROR - connection to " + Settings.socketAddress(socket) + " closed with exception : " + e);

        } finally {
            if(loggedIn) {
                ClientControl.getInstance().outputInfo(log, "INFO - logged out (passive)");
                loggedIn = false;
            }
            open = false;
            // if not manually closed, need to indicate closure to GUI
            if(!manuallyClosed) {
                ClientControl.getInstance().connectionClosed(socket);
            }

            outwriter.close();
        }
    }

    /**
     * @return true if connection is open
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * @return true if user is logged in
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Set connection login status
     * @param loggedIn true if connection status is logged in
     */
    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }
}
