package fedora.services.diringest;

import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;

import com.oreilly.servlet.multipart.*;

import fedora.common.*;
import fedora.services.diringest.common.*;
import fedora.services.diringest.sip2fox.*;
import fedora.services.diringest.sip2fox.pidgen.*;

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
public class IngestSIP extends HttpServlet {

    private ConversionRules m_defaultRules;
    private Converter m_converter;
    private String m_fedoraUser;
    private String m_fedoraPass;

    /**
     * The servlet entry point.  http://host:port/diringest/ingestSIP
     */
    public void doPost(HttpServletRequest request, 
            HttpServletResponse response) 
            throws IOException {
        //
        // 
        //
        File tempSIPFile = null;
        File tempRulesFile = null;
        FOXMLResult[] results = null;
		try {
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
            results = m_converter.convert(rules, 
                                          tempSIPFile, 
                                          m_fedoraUser, 
                                          m_fedoraPass);
            // FIXME: Do ingest (StagingIngester)
            PID[] pids = new PID[results.length];
            for (int i = 0; i < results.length; i++) {
                pids[i] = results[i].getPID();
            }
            doSuccess(pids, response);
		} catch (Exception e) {
            // FIXME: Use log4j here
            e.printStackTrace();
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

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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
	}

    public void doSuccess(PID[] pids, 
                          HttpServletResponse response) {
        try {
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("text/xml");
            PrintWriter w = response.getWriter();
            w.println("<pidList>");
            for (int i = 0; i < pids.length; i++) {
                w.println("  <pid>" + pids[i].toString() + "</pid>");
            }
            w.println("</pidList>");
            w.flush();
            w.close();
        } catch (Exception e) {
            // FIXME: use log4j here
            e.printStackTrace();
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
            // FIXME: use log4j here
            e.printStackTrace();
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
        try {
            File defaultRulesFile = null;  // FIXME: Determine from config, or default location
            m_defaultRules = new ConversionRules(new FileInputStream(defaultRulesFile));

            String pidNamespace = null;  // FIXME: Determine from config (ok if null)
            String host = null; // FIXME: Determine from config
            int port = -1; // FIXME: Determine from config
            PIDGenerator pidgen = new RemotePIDGenerator(pidNamespace, host, port);
            m_converter = new Converter(pidgen);

            m_fedoraUser = null; // FIXME: Determine from config
            m_fedoraPass = null; // FIXME: Determine from config
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Unable to initialize", e);
        }
    }

}
