package activitystreamer.util;


import org.json.JSONObject;

public class JsonCreator {

    public static JSONObject baseJson(String command){
        return new JSONObject().put("command", command);
    }

    public static String login(String username, String secret){
        JSONObject j = baseJson("LOGIN");
        j.put("username", username);
        j.put("secret", secret);
        return j.toString();
    }

    public static String register(String username, String secret){
        JSONObject j = baseJson("REGISTER");
        j.put("username", username);
        j.put("secret", secret);
        return j.toString();
    }

    public static String logout(){
        JSONObject j = baseJson("LOGOUT");
        return j.toString();
    }

    public static String activityMessage(String username, String secret, JSONObject activity){
        JSONObject j = baseJson("ACTIVITY_MESSAGE");
        j.put("username", username);
        j.put("secret", secret);
        j.put("activity", activity);
        return j.toString();
    }

    public static String invalidMessage(String info){
        JSONObject j = baseJson("INVALID_MESSAGE");
        j.put("info", info);
        return j.toString();
    }

}
