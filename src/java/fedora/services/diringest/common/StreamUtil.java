package fedora.services.diringest.common;

import java.io.*;

import net.iharder.base64.*;

/**
 * Static methods for working with streams.
 */
public abstract class StreamUtil {

    public static final int STREAM_BUFFER_SIZE = 4096;

    /**
     * Pipe the input stream to the output stream while encoding to base 64.
     */
    public static void base64Encode(InputStream inStream, 
                                    OutputStream outStream) throws IOException {
        pipe(inStream, new Base64.OutputStream(outStream));
    }

    /**
     * Pipe the input stream to the output stream while decoding from base 64.
     */
    public static void base64Decode(InputStream inStream, 
                                    OutputStream outStream) throws IOException {
        pipe(inStream, new Base64.OutputStream(outStream, Base64.DECODE));
    }

    /**
     * Pipe the input stream directly to the output stream.
     */
    public static void pipe(InputStream inStream,
                            OutputStream outStream) throws IOException {
        try {
            byte[] buf = new byte[STREAM_BUFFER_SIZE];
            int len;
            while ( ( len = inStream.read( buf ) ) > 0 ) {
                outStream.write( buf, 0, len );
            }
        } finally {
            inStream.close();
        }
    }

    public static String xmlEncode(String in) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '<') {
                out.append("&gt;");
            } else if (c == '>') {
                out.append("&lt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c == '"') {
                out.append("&quot;");
            } else if (c == '\'') {
                out.append("&apos;");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

}
