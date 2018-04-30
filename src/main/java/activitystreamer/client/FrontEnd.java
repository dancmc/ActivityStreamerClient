package activitystreamer.client;

import org.json.JSONObject;

/**
 * Project : Activity Streamer Client
 * Author : Daniel Chan (mchan@student.unimelb.edu.au)
 * Date : 22 Mar 2018
 */

/**
 * Probably unnecessary in this small app
 */
public interface FrontEnd {

    /*
    Things that need to happen :
        - setup (set input hostname & port to Settings, set input username and secret as well)
        - connected (set hostname and port labels, change indicator to green)
        - disconnected (set hostname and port labels, change indicator to red)
        - loggedin (set username label, change indicator to green)
        - loggedout (set username label, change indicator to red)
        - appendLog
        - appendActivity
        - sendActivity
     */


    void setup();

    void connected(String hostname, int port);

    void disconnected();

    void loggedIn();

    void loggedOut();

    void appendLog(String message);

    void appendActivity(JSONObject activity);

    void sendActivity(String message);


}
