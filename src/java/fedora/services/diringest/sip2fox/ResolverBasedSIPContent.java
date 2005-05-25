package fedora.services.diringest.sip2fox;

import java.io.*;

public class ResolverBasedSIPContent implements SIPContent {

    private String m_id;
    private boolean m_wasInline;
    private String m_mimeType;
    private String m_formatURI;
    private DataResolver m_resolver;
    private String m_locatorType;
    private String m_locator;

    private String m_label;
    private String m_type;

    public ResolverBasedSIPContent(String id,
                                   boolean wasInline,
                                   String mimeType,
                                   String formatURI,
                                   DataResolver resolver,
                                   String locatorType,
                                   String locator) {
        m_id = id;
        m_wasInline = wasInline;
        m_mimeType = mimeType;
        m_formatURI = formatURI;
        m_resolver = resolver;
        m_locatorType = locatorType;
        m_locator = locator;
    }

    public String getID() { return m_id; }

    public String getLabel() { return m_label; }
    public void setLabel(String label) { m_label = label; }

    public String getType() { return m_type; }
    public void setType(String type) { m_type = type; }

    public boolean wasInline() { return m_wasInline; }

    public String getMIMEType() { return m_mimeType; }

    public String getFormatURI() { return m_formatURI; }

    public InputStream getInputStream() throws IOException {
        return m_resolver.getData(m_locatorType, m_locator);
    }

    public void check() throws Exception {
        m_resolver.checkResolvability(m_locatorType, m_locator);
    }

}