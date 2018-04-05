package activitystreamer.util;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.net.Socket;
import java.security.SecureRandom;

public class Settings {
    private static final Logger log = LogManager.getLogger();
    private static SecureRandom random = new SecureRandom();
    private static String remoteHostname = null;
    private static int remotePort = 3780;
    private static String secret = null;
    private static String username = "anonymous";


    private static void help(Options options){
        String header = "An ActivityStream Client for Unimelb COMP90015\n\n";
        String footer = "\ncontact mchan@student.unimelb.edu.au for issues.";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ActivityStreamer.Client", header, options, footer, true);
        System.exit(-1);
    }


    public static int getRemotePort() {
        return remotePort;
    }

    public static void setRemotePort(int remotePort) {
        if (remotePort < 0 || remotePort > 65535) {
            log.error("supplied port " + remotePort + " is out of range, using " + getRemotePort());
        } else {
            Settings.remotePort = remotePort;
        }
    }

    public static String getRemoteHostname() {
        return remoteHostname;
    }

    public static void setRemoteHostname(String remoteHostname) {
        Settings.remoteHostname = remoteHostname;
    }

    public static String getSecret() {
        return secret;
    }

    public static void setSecret(String s) {
        secret = s;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        Settings.username = username;
    }


    /*
     * Some general helper functions
     */

    public static String socketAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }

    public static String nextSecret() {
        return new BigInteger(130, random).toString(32);
    }

    public static void parseArguments(String[] args){
        log.info("reading command line options");

        Options options = new Options();
        options.addOption("rh",true,"remote hostname (default : loopback)");
        options.addOption("rp",true,"remote port number (default : 3780)");
        options.addOption("u",true,"username (default : anonymous)");
        options.addOption("s",true,"secret for username");


        // build the parser
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e1) {
            help(options);
        }

        if(cmd.hasOption("rh")){
            Settings.setRemoteHostname(cmd.getOptionValue("rh"));
        }

        if(cmd.hasOption("rp")){
            try{
                int port = Integer.parseInt(cmd.getOptionValue("rp"));
                Settings.setRemotePort(port);
            } catch (NumberFormatException e){
                log.error("-rp requires a port number, parsed: "+cmd.getOptionValue("rp"));
                help(options);
            }
        }

        if(cmd.hasOption("s")){
            Settings.setSecret(cmd.getOptionValue("s"));
        }

        if(cmd.hasOption("u")){
            Settings.setUsername(cmd.getOptionValue("u"));
        }
    }
}
