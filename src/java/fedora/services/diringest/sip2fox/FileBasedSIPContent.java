package fedora.services.diringest.sip2fox;

import java.io.*;

public class FileBasedSIPContent implements SIPContent {

    private String m_id;
    private boolean m_wasInline;
    private String m_mimeType;
    private String m_formatURI;
    private File m_file;

    private String m_label;
    private String m_type;

    public FileBasedSIPContent(String id,
                               boolean wasInline,
                               String mimeType,
                               String formatURI,
                               File file) {
        m_id = id;
        m_wasInline = wasInline;
        m_mimeType = mimeType;
        m_formatURI = formatURI;
        m_file = file;
    }

    public String getLabel() { return m_label; }
    public void setLabel(String label) { m_label = label; }

    public String getType() { return m_type; }
    public void setType(String type) { m_type = type; }

    public String getID() { return m_id; }

    public boolean wasInline() { return m_wasInline; }

    public String getMIMEType() { return m_mimeType; }

    public String getFormatURI() { return m_formatURI; }

    public InputStream getInputStream() throws IOException {
        return new FileInputStream(m_file);
    }

    public void check() throws Exception {
        if (!m_file.exists()) {
            throw new IOException("File does not exist: " + m_file.getPath());
        }
        if (m_file.isDirectory()) {
            throw new IOException("Not a file : " + m_file.getPath());
        }
    }

}