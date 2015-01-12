package org.bbop.paint.panther.PantherServer;

import com.sri.panther.paintCommon.FixedInfo;
import com.sri.panther.paintCommon.RawComponentContainer;
import com.sri.panther.paintCommon.TransferInfo;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/**
 * Created by suzi on 12/15/14.
 */
class PantherIO {

    static final String pantherURL = "http://173.255.211.222:8080";
    private static final String REQUEST_OPEN_BOOK = "OpenBook";

    private static Hashtable<String, Hashtable<String, Vector<String>>> dbToUPLInfo;
    private static String currentDB;
    private static String currentVersionKey;

    private static final Logger log = Logger.getLogger(PantherIO.class);

    public static FixedInfo getFixedInfoFromServer() {
        Vector objs = null;
        String server_status = "";
        try {
            // try to get if from the session
            // connect to the servlet
            URL servlet = new URL(pantherURL + "/servlet/com.sri.panther.paintServer.servlet.Client2Servlet?action=FixedInfo");
            URLConnection servletConnection = servlet.openConnection();

            // Don't used a cached version of URL connection.
            servletConnection.setUseCaches(false);
            servletConnection.setDefaultUseCaches(false);
            // Read the input from the servlet.
            //
            // The servlet will return a serialized vector containing a DataTransfer object
            //
            ObjectInputStream inputFromServlet = new ObjectInputStream(new GZIPInputStream(servletConnection.getInputStream()));
            objs = (Vector) inputFromServlet.readObject();
            inputFromServlet.close();
        } catch (MalformedURLException muex) {
            server_status = muex.getLocalizedMessage();
        } catch (IOException ioex) {
            server_status = ioex.getLocalizedMessage();
        } catch (ClassNotFoundException cnfex) {
            server_status = cnfex.getLocalizedMessage();
        }
        if (null != objs) {
            TransferInfo ti = (TransferInfo) objs.elementAt(0);
            if (0 != ti.getInfo().length()) {
                server_status = ("Server cannot access information for transfer: " + ti.getInfo());
                return null;
            }
            return setFixedInfo((FixedInfo) objs.elementAt(1));
        }
        return null;
    }

    public static RawComponentContainer getRawPantherFamily(Vector<?> userInfo, String familyID) {
        Vector objs = new Vector();

        if (!PantherLogin.getLoggedIn()) {
            PantherLogin.login();
        }
        objs.addElement(userInfo);
        objs.addElement(getDbAndVersionKey());
        objs.addElement(familyID);
        Object o = sendAndReceive(REQUEST_OPEN_BOOK, objs);

        if (null == o) {
            return null;
        }

        Vector output = (Vector) o;

        TransferInfo ti = (TransferInfo) output.elementAt(0);

        if (0 != ti.getInfo().length()) {
            log.error("Server cannot access information for transfer: " + ti.getInfo());
            return null;
        }

        return (RawComponentContainer) output.elementAt(1);

    }

   /**
     * Method declaration
     *
     * @param actionRequest
     * @param sendInfo
     * @return
     * @see
     */
    public static Object sendAndReceive(String actionRequest, Object sendInfo) {
        String message; // if no message, then it's all lovely
        Object outputFromServlet = null;
        try {
            // connect to the servlet
            URL servlet =
                    new URL(pantherURL + "/servlet/com.sri.panther.paintServer.servlet.Client2Servlet?action="
                            + actionRequest);
            java.net.URLConnection servletConnection = servlet.openConnection();

            servletConnection.setRequestProperty("Content-Type", "application/octet-stream");

            // Connection should ignore caches if any
            servletConnection.setUseCaches(false);

            // Indicate sending and receiving information from the server
            servletConnection.setDoInput(true);
            servletConnection.setDoOutput(true);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(servletConnection.getOutputStream());

            objectOutputStream.writeObject(sendInfo);
            objectOutputStream.flush();
            objectOutputStream.close();
            ObjectInputStream servletOutput = new ObjectInputStream(new GZIPInputStream(servletConnection.getInputStream()));
            outputFromServlet = servletOutput.readObject();

            servletOutput.close();
        } catch (MalformedURLException muex) {
            message = ("MalformedURLException " + muex.getMessage()
                    + " has been returned while sending and receiving information from server");
            log.error(message);
            muex.printStackTrace();
        } catch (IOException ioex) {
            message = ("IOException " + ioex.getMessage()
                    + " has been returned while sending and receiving information from server");
            log.error(message);
        } catch (Exception e) {
            message = ("Exception " + e.getMessage()
                    + " has been returned while sending and receiving information from server");
            log.error(message);
        }

        if (outputFromServlet == null) {
            log.error("Unable to get user information");
        } else {
            Vector output = (Vector) outputFromServlet;
            TransferInfo ti = (TransferInfo) output.elementAt(0);
            if (ti.getInfo() == null) {
                log.error("Unable to verify user information");
                outputFromServlet = null;
            }
            else {
                if (ti.getInfo().length() > 0)
                    outputFromServlet = null;
            }
        }

        return outputFromServlet;
    }

    private static String getDbAndVersionKey() {
        if (currentDB != null && currentVersionKey != null)
            return currentDB + '|' + currentVersionKey;
        else
            return null;
    }

    /**
     * Method declaration
     * dbClsID is the db name concatenated with a pipe to the version. e.g. dev_3_panther_upl|UPL 8.0
     *
     * @return
     *
     * @see
     */
    static String getDbAndVersionName() {
        if (currentDB != null && currentVersionKey != null)
            return currentDB + '|' + getCurrentVersionName();
        else
            return null;
    }

    private static String getCurrentVersionName() {
        if (dbToUPLInfo != null && currentDB != null && currentVersionKey != null)
            return dbToUPLInfo.get(currentDB).get(currentVersionKey).firstElement();
        else
            return "";
    }

    private static FixedInfo setFixedInfo(FixedInfo fi) {
        dbToUPLInfo = null;
        currentDB = null;
        currentVersionKey = null;
        dbToUPLInfo = (Hashtable<String, Hashtable<String, Vector<String>>>) fi.getDbToUploadInfo();
        if (dbToUPLInfo != null) {
            // Get upl version from server
            Set<String> db_names = dbToUPLInfo.keySet();
            for (String db_name : db_names) {
                if (currentDB == null)
                    currentDB = db_name;
                Hashtable<String, Vector<String>> versions = dbToUPLInfo.get(db_name);
                Set<String> version_keys = versions.keySet();
                for (String version_key : version_keys) {
                    Vector<String> version_date = versions.get(version_key);
                    if (currentVersionKey == null) {
                        currentVersionKey = version_key;
                    }
                    if (version_date.firstElement().equals("dev_3_panther_upl|UPL 9.0")) {
                        currentDB = db_name;
                        currentVersionKey = version_key;
                    }
                }
            }
        } else {
            return null;
        }
        return fi;
    }


}


