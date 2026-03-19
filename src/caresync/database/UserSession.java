package caresync.database;

public class UserSession {
    private static String loggedInUserName;
    private static int loggedInUserId;
    private static String loggedInRole;

    public static void setLoggedInUserName(String name) {
        loggedInUserName = name;
    }

    public static String getLoggedInUserName() {
        return loggedInUserName;
    }
    
    public static void setLoggedInUserId(int id) {
        loggedInUserId = id;
    }

    public static int getLoggedInUserId() {
        return loggedInUserId;
    }
    
    public static void setLoggedInRole(String role) {
        loggedInRole = role;
    }

    public static String getLoggedInRole() {
        return loggedInRole;
    }
    
    public static void clearSession() {
        loggedInUserName = null;
        loggedInUserId = 0;
        loggedInRole = null;
    }
}