package fedora.services.diringest.sip2fox.pidgen;

import java.io.*;
import org.apache.log4j.*;

import fedora.client.FedoraClient;
import fedora.services.diringest.*;
import fedora.common.*;

/**
 * PIDGenerator that uses a remote Fedora instance to generate PIDs.
 *
 * http://localhost:8080/fedora/management/getNextPID?numPIDs=10&namespace=nara&xml=true
 */
public class RemotePIDGenerator implements PIDGenerator {

    private static final Logger logger =
                Logger.getLogger(RemotePIDGenerator.class.getName());

    private String m_fedoraBaseURL;
    private String m_urlStart;

    public RemotePIDGenerator(String namespace, 
                              String fedoraBaseURL) {
        m_fedoraBaseURL = fedoraBaseURL;
        String ns;
        if (namespace == null) {
            ns = "";
            logger.info("No namespace provided; will use default of server");
        } else {
            ns = "&namespace=" + namespace;
            logger.info("Using namespace '" + namespace + "'");
        }
        m_urlStart = "/management/getNextPID?xml=true" + ns + "&numPIDs=";
    }

    public PID getNextPID(String user, String pass) throws IOException {
        return getNextPIDs(1, user, pass)[0];
    }

    public PID[] getNextPIDs(int howMany, String user, String pass) throws IOException {
        InputStream in = null;
        try {
            FedoraClient fedora = new FedoraClient(m_fedoraBaseURL, user, pass);
            in = fedora.get(m_urlStart + howMany, true);
            return new PIDListReader(in).getPIDArray();
        } catch (Exception e) {
            throw new IOException("Error getting remote pid list: " + e.getMessage());
        } finally {
            if (in != null) try { in.close(); } catch (Exception e) { }
        }
    }

}
