package activitystreamer;

import activitystreamer.client.ClientControl;
import activitystreamer.util.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Client {

    private static final Logger log = LogManager.getLogger();


    public static void main(String[] args) {


        // This is a probably unnecessary attempt to speed up GUI initialisation
        // means that no logging to console from relaunched process, but most logging goes to GUI as well anyway
//        String exeName = new java.io.File(Client.class.getProtectionDomain()
//                .getCodeSource()
//                .getLocation()
//                .getPath())
//                .getName();
//
//        System.out.println(exeName);
//
//        if(exeName.endsWith(".jar")) {
//            if (args.length == 0 || !args[0].equals("-norelaunch")) {
//                try {
//
//                    ArrayList<String> newList = new ArrayList<>();
//                    newList.add("java");
//                    newList.add("-Dsun.java2d.d3d=false");
//                    newList.add("-jar");
//                    newList.add(exeName);
//                    newList.add("-norelaunch");
//                    newList.addAll(Arrays.asList(args));
//
//                    for (String s : newList) {
//                        System.out.println(s);
//                    }
//
//                    Runtime.getRuntime().exec(newList.toArray(new String[newList.size()]));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                System.exit(0);
//            } else {
//                ArrayList<String> newList = new ArrayList<>(Arrays.asList(args));
//                newList.remove(0);
//                args = newList.toArray(new String[newList.size()]);
//            }
//        }


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
