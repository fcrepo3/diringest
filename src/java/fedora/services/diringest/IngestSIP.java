package fedora.services.diringest;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;

import com.oreilly.servlet.multipart.*;
import org.apache.log4j.*;

import fedora.client.*;
import fedora.common.*;
import fedora.services.diringest.common.*;
import fedora.services.diringest.ingest.*;
import fedora.services.diringest.sip2fox.*;
import fedora.services.diringest.sip2fox.pidgen.*;
import fedora.server.management.*;

/**
 * Accepts an HTTP Multipart POST of a SIP file, creates Fedora objects from
 * it, then ingests them into a Fedora repository.
 *
 * The SIP file is submitted as a parameter named "sip".
 * Optionally, a rules file is submitted as a parameter named "rules".
 *
 * This service will act on behalf of a user whose name/password is
 * configured.
 */
public class IngestSIP extends HttpServlet 
                       implements DatastreamStage {

    private static final Logger logger =
            Logger.getLogger(IngestSIP.class.getName());

    private ConversionRules m_defaultRules;
    private Converter m_converter;
    private String m_fedoraHost;
    private int m_fedoraPort;
    private String m_fedoraUser;
    private String m_fedoraPass;

    private Map m_stagedContent;

    private String m_requestURL;

    /**
     * The servlet entry point.  http://host:port/diringest/ingestSIP
     */
    public void doPost(HttpServletRequest request, 
            HttpServletResponse response) 
            throws IOException {
        if (m_requestURL == null) {
            m_requestURL = request.getRequestURL().toString();
        }
        File tempSIPFile = null;
        File tempRulesFile = null;
        FOXMLResult[] results = null;
		try {

            // Save the 'sip' and (optional) 'rules' streams as they come
            MultipartParser parser=new MultipartParser(request, 
                Long.MAX_VALUE, true, null);
			Part part = parser.readNextPart();
            while (part != null) {
                if (part.isFile()) {
                    FilePart filePart = (FilePart) part;
                    if (filePart.getName().equals("sip")) {
                        tempSIPFile = getTempFile(filePart);
                    } else if (filePart.getName().equals("rules")) {
                        tempRulesFile = getTempFile(filePart);
                    }
                }
			    part = parser.readNextPart();
            }
            if (tempSIPFile == null) {
                doError(HttpServletResponse.SC_BAD_REQUEST,
                        "Required parameter (sip) not provided", response);
            }
            ConversionRules rules;
            if (tempRulesFile == null) {
                rules = m_defaultRules;
            } else {
                rules = new ConversionRules(new FileInputStream(tempRulesFile));
            }

            // Convert the SIP to a bunch of FOXML
            results = m_converter.convert(rules, 
                                          tempSIPFile, 
                                          m_fedoraUser, 
                                          m_fedoraPass);

            // Ingest all the objects that resulted from the conversion
            Ingester ingester = new StagingIngester(m_fedoraHost, 
                                                    m_fedoraPort,
                                                    this); // DatastreamStage
            List ingestedPIDs = new ArrayList();
            try {
                logger.info("Starting ingest of " + results.length + " objects");
                for (int i = 0; i < results.length; i++) {
                    ingester.ingest(m_fedoraUser, m_fedoraPass, results[i]);
                    ingestedPIDs.add(results[i].getPID());
                }
                logger.info("Successfully ingested all objects");
            } catch (Exception e) {
                // If anything goes wrong, back out (delete) any objects
                // that were already created
                logger.warn("Failed to ingest a FOXMLResult", e);
                purgeAll(m_fedoraUser, m_fedoraPass, ingestedPIDs);
                throw new IOException(e.getMessage());
            }
            // At this point the ingest of the entire SIP was successful.


            // TODO: Put some kind of hook here to notify a search/other
            //       service and possibly pass data to it



            doSuccess(ingestedPIDs, response);
		} catch (Exception e) {
            logger.warn("Error", e);
            doError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getClass().getName() + ": " + e.getMessage(), response);
        } finally {
            if (tempSIPFile != null) tempSIPFile.delete();
            if (tempRulesFile != null) tempRulesFile.delete();
            if (results != null) {
                for (int i = 0; i < results.length; i++) {
                    results[i].close();
                }
            }
        }
    }

    private void purgeAll(String user, String pass, List pids) {
        logger.info("Backing out (purging) " + pids.size() + " already-ingested objects.");
        try {
            FedoraAPIM apim = APIMStubFactory.getStub(m_fedoraHost,
                                                      m_fedoraPort, 
                                                      user,
                                                      pass);
            for (int i = 0; i < pids.size(); i++) {
                String pid = ((PID) pids.get(i)).toString();
                try {
                    apim.purgeObject(pid,
                                     "Automatically backed-out by diringest service", 
                                     false);
                    logger.info("Purged " + pid);
                } catch (Exception e) {
                    logger.warn("Unable to purge " + pid, e);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to purge already-ingested objects (Can't get a Fedora APIM instance)", e);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String id = request.getParameter("getStagedContent");
        if (id == null) {
            response.setStatus(200);
            response.setContentType("text/html");
            PrintWriter w = response.getWriter();
            w.println("<html><body><h2>IngestSIP</h2><hr size=\"1\"/>");
            w.println("<form method=\"POST\" enctype=\"multipart/form-data\">");
            w.println("<input type=\"file\" size=\"20\" name=\"sip\"/><br/>");
            w.println("<input type=\"submit\"/><br/>");
            w.println("</body></html>");
            w.flush();
            w.close();
        } else {
            File f = (File) m_stagedContent.get(id);
            if (f == null) {
                doError(HttpServletResponse.SC_NOT_FOUND,
                        "No such staged content: " + id, response);
            } else {
                response.setStatus(200);
                response.setContentType("application/octet-stream");
                OutputStream out = response.getOutputStream();
                StreamUtil.pipe(new FileInputStream(f), out);
                out.close();
            }
        }
	}

    public void doSuccess(List pids, 
                          HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("text/xml");
            PrintWriter w = response.getWriter();
            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.println("<pidList>");
            for (int i = 0; i < pids.size(); i++) {
                w.println("  <pid>" + ((PID) pids.get(i)).toString() + "</pid>");
            }
            w.println("</pidList>");
            w.flush();
            w.close();
        } catch (Exception e) {
            logger.warn("Error sending successful pidList", e);
        }
    }

    public void doError(int status, 
                        String message, 
                        HttpServletResponse response) {
        try {
            response.setStatus(status);
            response.setContentType("text/plain");
            PrintWriter w = response.getWriter();
            w.println(message);
        } catch (Exception e) {
            logger.warn("Error sending error response", e);
        }
    }

    private File getTempFile(FilePart filePart) throws IOException {
        File tempFile = File.createTempFile("diringest", ".tmp");
        FileOutputStream out = new FileOutputStream(tempFile);
        try {
            StreamUtil.pipe(filePart.getInputStream(), out);
        } finally {
            out.close();
        }
		return tempFile;
	}

    /**
     * Initialize servlet.
     *
     * @throws ServletException If the servet cannot be initialized.
     */
    public void init() throws ServletException {
        m_stagedContent = new HashMap();
        try {
            // Get the default rules from WEB-INF/classes/crules.xml
            InputStream rulesStream = this.getClass().getResourceAsStream("/crules.xml");
            if (rulesStream == null) {
                throw new IOException("Error loading default conversion rules: /crules.xml not found in classpath");
            }
            m_defaultRules = new ConversionRules(rulesStream);

            // Get the properties from WEB/INF/classes/diringest.properties
            InputStream propStream = this.getClass().getResourceAsStream("/diringest.properties");
            if (propStream == null) {
                throw new IOException("Error loading configuration: /diringest.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(propStream);

            // Get the required properties
            m_fedoraHost = props.getProperty("fedora.host"); 
            if (m_fedoraHost == null) {
                throw new IOException("Required property (fedora.host) not specified in /diringest.properties");
            }
            String port = props.getProperty("fedora.port"); 
            if (port == null) {
                throw new IOException("Required property (fedora.port) not specified in /diringest.properties");
            }
            m_fedoraUser = props.getProperty("fedora.user"); 
            if (m_fedoraUser == null) {
                throw new IOException("Required property (fedora.user) not specified in /diringest.properties");
            }
            m_fedoraPass = props.getProperty("fedora.pass"); 
            if (m_fedoraPass == null) {
                throw new IOException("Required property (fedora.pass) not specified in /diringest.properties");
            }

            // Get the optional properties
            String pidNamespace = props.getProperty("pid.namespace");

            // Initialize the converter
            m_fedoraPort = Integer.parseInt(port);
            m_converter = new Converter( 
                              new RemotePIDGenerator(pidNamespace, 
                                                     m_fedoraHost,
                                                     m_fedoraPort));
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Unable to initialize", e);
        }
    }

    //
    // From DatastreamStage interface
    //

    public URL stageContent(File file) throws IOException {
        int c = file.hashCode();
        String id;
        if (c < 0) {
            id = "0" + c;
        } else {
            id = "1" + c;
        }
        m_stagedContent.put(id, file);
        return getURL(id);
    }

    public void unStageContent(URL location) throws IOException {
        String[] parts = location.toString().split("/");
        String id = parts[parts.length - 1];
        File f = (File) m_stagedContent.get(id);
        if (f == null) {
            throw new IOException("Error un-staging: Not staged: " + id);
        } else {
            f.delete();
        }
    }

    private URL getURL(String id) {
        URL url = null;
        String urlString = m_requestURL + "?getStagedContent=" + id;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {  
            logger.warn("Programmer error.  Bad url: " + urlString);
        }
        return url;
    }

}
