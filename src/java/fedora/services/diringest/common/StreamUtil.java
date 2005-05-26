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
        pipe(new Base64.InputStream(inStream, Base64.ENCODE), outStream);
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
            while ( ( len = inStream.read( buf ) ) != -1 ) {
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

    public static void base64Decode(File inFile, File outFile) throws IOException {
        OutputStream baseOut = new Base64.OutputStream(new FileOutputStream(outFile), Base64.DECODE);
        BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(baseOut));
        BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(inFile)));
        String sep = System.getProperty("line.separator");
        String line = reader.readLine();
        while (line != null) {
            writer.write(line + sep, 0, line.length() + sep.length());
            line = reader.readLine();
        }
        baseOut.flush();
        writer.flush();
        writer.close();
        reader.close();
    }

    public static void base64Encode(File inFile, File outFile) throws IOException {
        OutputStream baseOut = new Base64.OutputStream(new FileOutputStream(outFile), Base64.ENCODE);
        BufferedWriter writer = new BufferedWriter(
                                    new OutputStreamWriter(baseOut));
        BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(
                                        new FileInputStream(inFile)));
        String line = reader.readLine();
        String sep = System.getProperty("line.separator");
        boolean firstLine = true;
        while (line != null) {
            if (!firstLine) {
                writer.write(sep + line, 0, sep.length() + line.length());
            } else {
                firstLine = false;
                writer.write(line, 0, line.length());
            }
            line = reader.readLine();
        }
        baseOut.flush();
        writer.flush();
        writer.close();
        reader.close();
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 3) {
            File inFile = new File(args[1]);
            File outFile = new File(args[2]);
            if (args[0].startsWith("e") || args[0].startsWith("-e")) {
                base64Encode(inFile, outFile);
                base64Encode(new FileInputStream(inFile), System.out);
            } else if (args[0].startsWith("d") || args[0].startsWith("-d")) {
                base64Decode(inFile, outFile);
                base64Decode(new FileInputStream(inFile), System.out);
            } else {
                System.err.println("ERROR: First argument should be -e[ncode] or -d[ecode]");
            }
        } else {
            System.err.println("ERROR: Need three arguments:  ( -e | -d ) inputFile outputFile");
        }
    }

}
