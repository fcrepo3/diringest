package fedora.services.diringest;

import java.io.*;

import net.iharder.base64.*;

/**
 * Static methods for working with streams.
 */
public abstract class StreamUtil {

    /**
     * Pipe the input stream to the output stream while encoding to base 64.
     */
    public static void base64Encode(InputStream inStream, 
                                    OutputStream outStream) throws IOException {
        OutputStream out = new Base64.OutputStream(outStream);
        try {
            byte[] buf = new byte[4096];
            int len;
            while ( ( len = inStream.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
            }
        } finally {
            inStream.close();
        }
    }

    /**
     * Pipe the input stream to the output stream while decoding from base 64.
     */
    public static void base64Decode(InputStream inStream, 
                                    OutputStream outStream) throws IOException {
        OutputStream out = new Base64.OutputStream(outStream, Base64.DECODE);
        try {
            byte[] buf = new byte[4096];
            int len;
            while ( ( len = inStream.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
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
