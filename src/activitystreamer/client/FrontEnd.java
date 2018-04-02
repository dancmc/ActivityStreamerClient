package activitystreamer.client;

import org.json.JSONObject;

public class FrontEnd {

    private static FrontEnd frontEnd;

    public static FrontEnd getInstance(){
        if(frontEnd==null){
            frontEnd = new FrontEnd();
        }
        return frontEnd;
    }

    FrontEnd(){
        setup();
    }

    public void setup(){

    }

    public void appendLog(String message){

    }

    public void appendActivities(JSONObject activity, boolean self){
        String user = activity.optString("authenticated_user", "unknown");

        if(self){
            // display different colour
            user = "self";
        }
    }

    public void loggedIn(){

    }

    public void connected(String hostname, int port){

    }

    public void disconnected(){

    }

}
