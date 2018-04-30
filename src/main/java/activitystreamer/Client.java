package activitystreamer;


import activitystreamer.client.ClientControl;
import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Project : Activity Streamer Client
 * Author : Daniel Chan (mchan@student.unimelb.edu.au)
 * Date : 22 Mar 2018
 */

public class Client {

    private static final Logger log = LogManager.getLogger();


    public static void main(String[] args) {


        log.info("INFO - starting client");

        Settings.parseArguments(args);

        final ClientControl c = ClientControl.getInstance();
        c.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                c.setTerm(true);
                c.cleanup();
            }
        });

    }


}
