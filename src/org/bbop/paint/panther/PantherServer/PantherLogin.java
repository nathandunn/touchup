package org.bbop.paint.panther.PantherServer;

import com.sri.panther.paintCommon.FixedInfo;
import org.apache.log4j.Logger;

import java.util.Vector;

/**
 * Created by suzi on 12/15/14.
 */
class PantherLogin {
    // stored when the settings are written into a property file
    private static boolean logged_in = false;  // Indicates user login status.  Note:  This information is not stored when the settings are written into a property file
    private static final String username = "gouser";
    private static final String pw = "welcome";


    public static boolean login() {
        if (!logged_in) {
            login(username, pw);
        }
        return logged_in;
    }

    public static void logout() {
        logged_in = false;
    }

    private static final Logger log = Logger.getLogger(PantherLogin.class);

    /**
     * @param username The first element in the vector is a String holding the user name to use.  The Second it an character array that contains the password.
     */
    private static void login(String username, String pw) {
        // Determine what databases and upl's are available from the server
        // Get information that does not change.
        logged_in = false;

        if (!InternetChecker.getInstance().isConnectionPresent(true)) {
            return;
        }

        log.debug("Logging in to Panther URL: " + PantherIO.pantherURL);
        FixedInfo fi = PantherIO.getFixedInfoFromServer();
       if (fi != null) {
           // Now that a valid database has been specified.  Verify user name and password from that database
           Vector<Object> results = new Vector<Object>();
           results.addElement(username);
           results.addElement(pw.toCharArray());

           Vector objs = new Vector();
           objs.addElement(results);
           objs.addElement(FixedInfo.getDb(PantherIO.getDbAndVersionName()));

           Object o = PantherIO.sendAndReceive("GetUserInfo", objs);
           logged_in = o != null;
       }
    }

    public static boolean getLoggedIn() {
        return logged_in;
    }

    public static Vector<Object> getUserInfo() {
        Vector<Object> userInfo = new Vector<Object>();
        userInfo.addElement(username);
        userInfo.addElement(pw.toCharArray());
        return userInfo;
    }
}


