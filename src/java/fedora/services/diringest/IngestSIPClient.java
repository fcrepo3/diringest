package fedora.services.diringest;

import java.io.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.methods.multipart.*;

public class IngestSIPClient {

    private static final int TIMEOUT_SECONDS        = 60000; // 100 min
    private static final int SOCKET_TIMEOUT_SECONDS = 60000;

    private MultiThreadedHttpConnectionManager m_cManager =
            new MultiThreadedHttpConnectionManager();

    private String m_endpoint;

    /**
     * Construct an uploader to a certain repository as a certain user.
     */
    public IngestSIPClient(String endpoint)
            throws IOException {
        m_endpoint=endpoint;
		m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);
		m_cManager.getParams().setSoTimeout(SOCKET_TIMEOUT_SECONDS * 1000);
    }

    /**
     * Upload the given file to the endpoint via HTTP POST.
     *
     * @return the response XML as a string.
     */
    public String upload(File file)
            throws IOException {
        PostMethod post = null;
        try {
            // prepare the post method
            post = new PostMethod(m_endpoint);
//            post.setDoAuthentication(true);
            post.getParams().setParameter("Connection","Keep-Alive");

            // chunked encoding is not required by the Fedora server,
            // but makes uploading very large files possible
            post.setContentChunked(true);

            // add the file part
            Part[] parts = { new FilePart("sip", file) };
            post.setRequestEntity(new MultipartRequestEntity(parts, 
                    post.getParams()));

            HttpClient client = new HttpClient();
//            client.getState().setCredentials(m_authScope, m_creds);
//            client.getParams().setAuthenticationPreemptive(true);

            // execute and get the response
            int responseCode = client.executeMethod(post);
            String body = null;
            try { 
                body = post.getResponseBodyAsString();
            } catch (Exception e) {
                IOException ioe = new IOException("Unable to read response body");
                ioe.initCause(e);
                throw ioe;
            }
            if (body == null) {
                body = "[empty response body]";
            }
            body = body.trim();
            if (responseCode != HttpStatus.SC_CREATED) {
                throw new IOException("Upload failed: " 
                        + HttpStatus.getStatusText(responseCode)
                        + ": " + replaceNewlines(body, " "));
            } else {
                return replaceNewlines(body, "");
            }
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
    }

    /**
     * Replace newlines with the given string.
     */
    private static String replaceNewlines(String in, String replaceWith) {
        return in.replaceAll("\r", replaceWith).replaceAll("\n", replaceWith);
    }

    /**
     * Test this class by uploading the given file to the given endpoint.
     */
    public static void main(String[] args) {
        fedora.services.diringest.sip2fox.SIP2FOX.initLogging();
        try {
            if (args.length==2) {
                IngestSIPClient client = new IngestSIPClient(args[0]);
                File file = new File(args[1]);
                System.out.println(client.upload(file));
            } else {
                System.err.println("Usage: IngestSIPClient endpoint file");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
