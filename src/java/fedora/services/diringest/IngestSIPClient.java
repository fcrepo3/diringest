package fedora.services.diringest;

import java.io.*;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

public class IngestSIPClient {

    private MultiThreadedHttpConnectionManager m_cManager=
            new MultiThreadedHttpConnectionManager();

    private String m_endpoint;

    /**
     * Construct an uploader to a certain repository as a certain user.
     */
    public IngestSIPClient(String endpoint)
            throws IOException {
        m_endpoint=endpoint;
//        m_creds=new UsernamePasswordCredentials(user, pass);
    }

    /**
     * Get a file's size, in bytes.  Return -1 if size can't be determined.
    private long getFileSize(File f) {
        long size=0;
        InputStream in=null;
        try {
            in=new FileInputStream(f);
            byte[] buf = new byte[8192];
            int len;
            while ( ( len = in.read( buf ) ) > 0 ) {
                size+=len;
            }
        } catch (IOException e) {
        } finally {
            size=-1;
            try {
                if (in!=null) {
                    in.close();
                }
            } catch (IOException e) {
                System.err.println("WARNING: Could not close stream.");
            }
        }
        return size;
    }
     */

    /**
     * Send a file to the server, getting back the identifier.
     */
    public String upload(File in) throws IOException {
        MultipartPostMethod post=null;
        try {
            HttpClient client=new HttpClient(m_cManager);
            client.setConnectionTimeout(20000); // wait 20 seconds max
            client.setConnectionTimeout(1000 * 60 * 20); // 20 minutes max
//            client.getState().setCredentials(null, null, m_creds);
//            client.getState().setAuthenticationPreemptive(true); // don't bother with challenges
            post=new MultipartPostMethod(m_endpoint);
//            post.setDoAuthentication(true);
            post.addParameter("sip", in);
            int resultCode=client.executeMethod(post);
            if (resultCode!=201) {
                throw new IOException(HttpStatus.getStatusText(resultCode)
                        + ": " 
                        + replaceNewlines(post.getResponseBodyAsString(), " "));
            }
            return replaceNewlines(post.getResponseBodyAsString(), "");
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            if (post!=null) post.releaseConnection();
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
