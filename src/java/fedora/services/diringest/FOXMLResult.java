package fedora.services.diringest;

import java.io.*;

import fedora.common.*;

/**
 * A self-contained FOXML file.
 */
public class FOXMLResult {

    private File m_file;
    private PID m_pid;

    public FOXMLResult(File file, PID pid) {
        m_file = file;
        m_pid = pid;
    }

    public InputStream getStream() throws IOException {
        return new FileInputStream(m_file);
    }

    /** Send the contents to a stream (don't close the stream when done). */
    public void dump(OutputStream out) throws IOException {
    }

    public PID getPID() {
        return m_pid;
    }

    /** Delete the temporary file if it hasn't already been deleted. */
    public void close() {
        if (m_file.exists()) m_file.delete();
    }

    /** Ensure close() is called at garbage collection time. */
    public void finalize() {
        close();
    }

}