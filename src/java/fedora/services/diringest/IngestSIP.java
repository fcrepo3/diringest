package fedora.services.diringest;

import java.io.*;
import javax.servlet.http.*;
import javax.servlet.*;

import com.oreilly.servlet.multipart.*;

import fedora.services.diringest.common.*;

/**
 * Accepts an HTTP Multipart POST of a SIP file, creates Fedora objects from
 * it, then ingests them into a Fedora repository.
 *
 * The submitted file must be named "sip", must not be accompanied by any other
 * parameters, and cannot be over 2,047 MB in size (due to a trivially 
 * overcome cos.jar limitation).
 */
public class IngestSIP
        extends HttpServlet {

    /**
     * The servlet entry point.  http://host:port/diringest/ingestSIP
     */
    public void doPost(HttpServletRequest request, 
            HttpServletResponse response) 
            throws IOException {
		try {
            MultipartParser parser=new MultipartParser(request, 
                Long.MAX_VALUE, true, null);
			Part part=parser.readNextPart();
			if (part!=null && part.isFile()) {
			    if (part.getName().equals("sip")) {
                    doError(HttpServletResponse.SC_CREATED, 
                            saveAndGetId((FilePart) part), response);
				} else {
                    doError(HttpServletResponse.SC_BAD_REQUEST,
                            "Content must be named \"file\"", response);
				}
			} else {
			    if (part==null) {
			        doError(HttpServletResponse.SC_BAD_REQUEST, 
			                "No data sent.", response);
				} else {
			        doError(HttpServletResponse.SC_BAD_REQUEST, 
			                "No extra parameters allowed", response);
				}
			}
		} catch (Exception e) {
                    e.printStackTrace();
		    doError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    e.getClass().getName() + ": " + e.getMessage(), response);
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

    public void doError(int status, 
                        String message, 
                        HttpServletResponse response) {
        try {
            response.setStatus(status);
            response.setContentType("text/plain");
            PrintWriter w=response.getWriter();
            w.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String saveAndGetId(FilePart filePart) throws IOException {
        File tempFile = File.createTempFile("diringest", ".tmp");
        FileOutputStream out = new FileOutputStream(tempFile);
        try {
            StreamUtil.pipe(filePart.getInputStream(), out);
        } finally {
            out.close();
        }
		return tempFile.getPath();
	}

    /**
     * Initialize servlet.
     *
     * @throws ServletException If the servet cannot be initialized.
     */
    public void init() { //throws ServletException {
    }

}
