package fedora.services.diringest.sip2fox.pidgen;

import java.io.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.log4j.*;

import fedora.common.*;

/**
 * PIDGenerator that uses a remote Fedora instance to generate PIDs.
 *
 * http://localhost:8080/fedora/mgmt/getNextPID?numPIDs=10&namespace=nara&xml=true
 */
public class RemotePIDGenerator implements PIDGenerator {

    public static final int CONNECTION_TIMEOUT_MS = 20000;

    private static final Logger logger =
                Logger.getLogger(RemotePIDGenerator.class.getName());

    private MultiThreadedHttpConnectionManager m_cManager;

    private String m_host;
    private String m_urlStart;

    public RemotePIDGenerator(String namespace, 
                              String host, 
                              int port) {
        String ns;
        if (namespace == null) {
            ns = "";
            logger.info("No namespace provided; will use default of server");
        } else {
            ns = "&namespace=" + namespace;
            logger.info("Using namespace '" + namespace + "'");
        }
        m_host = host;
        m_urlStart = "http://" + host + ":" + port + "/fedora/mgmt/getNextPID?xml=true" + ns + "&numPIDs=";
        m_cManager = new MultiThreadedHttpConnectionManager();
    }

    public PID getNextPID(String user, String pass) throws IOException {
        return getNextPIDs(1, user, pass)[0];
    }

    public PID[] getNextPIDs(int howMany, String user, String pass) throws IOException {
        GetMethod getMethod = null;
        try {
            getMethod = get(m_urlStart + howMany, user, pass);
            PIDListReader reader = new PIDListReader(getMethod.getResponseBodyAsStream());
            return reader.getPIDArray();
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
        }
    }

    private GetMethod get(String url, String user, String pass) throws IOException {
        UsernamePasswordCredentials creds = null;
        if (user != null) creds = new UsernamePasswordCredentials(user, pass);
        GetMethod get = null;
        boolean ok = false;
        try {
            HttpClient client = new HttpClient(m_cManager);
            client.setConnectionTimeout(CONNECTION_TIMEOUT_MS); // wait x / 1000 seconds max
            if (creds != null) {
                client.getState().setCredentials(null, m_host, creds);
                client.getState().setAuthenticationPreemptive(true); // don't bother with challenges
            }
            int resultCode = 300; // not really, but enter the loop that way
            get = new GetMethod(url);
            if (creds != null) {
                get.setDoAuthentication(true);
            }
            get.setFollowRedirects(true);
            resultCode = client.executeMethod(get);
            if (resultCode != 200) {
                logger.warn("Error body: \n" + get.getResponseBodyAsString());
                throw new IOException("Server returned error: "
                        + resultCode + " " + HttpStatus.getStatusText(resultCode));
            }
            ok = true;
            return get;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getName();
            logger.warn("Error getting remote PID(s)", e);
            throw new IOException("Error getting remote PID(s): " + msg);
        } finally {
            if (get != null && !ok) get.releaseConnection();
        }
    }

}
