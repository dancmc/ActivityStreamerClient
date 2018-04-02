package activitystreamer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import activitystreamer.client.ClientControl;
import activitystreamer.util.Settings;

public class Client {

    private static final Logger log = LogManager.getLogger();


    public static void main(String[] args) {


        log.info("starting client");

        Settings.parseArguments(args);

        ClientControl c = ClientControl.getInstance();


    }


}
