package fedora.services.diringest;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class METSReader extends DefaultHandler {

    public static final String METS = "http://www.loc.gov/METS/";

    private static final Logger logger =
            Logger.getLogger(METSReader.class.getName());

    private TreeNode m_root;
    private DataResolver m_dataResolver;

    private Map m_prefixMap;
    private List m_prefixList;

    private String m_currentID;
    private StringBuffer m_xmlData;
    private String m_xmlDataString;

    public METSReader(InputStream xml, DataResolver dataResolver) throws Exception {
        m_dataResolver = dataResolver;
        m_prefixMap = new HashMap();
        m_prefixList = new ArrayList();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        parser.parse(xml, this);
    }

    public TreeNode getRoot() {
        return m_root;
    }

    /**
     * Clean up temporary storage area used for disk-backed access to inline
     * XML datastreams.
     */
    public void close() throws IOException {
        throw new IOException("METSReader.close() not implemented");
    }

    public void startElement(String uri, 
                             String localName, 
                             String qName, 
                             Attributes a) throws SAXException {
        if (uri.equals(METS) && localName.equals("dmdSec") && m_currentID == null) {
            m_currentID = a.getValue("", "ID");
            if (m_currentID == null) {
                throw new SAXException("dmdSec element must have an ID attribute");
            }
            logger.info("Started parsing dmdSec (ID = " + m_currentID + ")");
        } else if (m_currentID != null) {
            if (uri.equals(METS) && localName.equals("xmlData") && m_xmlData == null) {
                m_xmlData = new StringBuffer();
                logger.info("Started buffering xmlData for " + m_currentID);
            } else if (m_xmlData != null) {
                // inside xmlData
                appendElementStart(uri, localName, qName, a, m_xmlData);
            }
        }
    }

    public void characters(char[] ch, int start, int length) {
        if (m_xmlData != null) {
            m_xmlData.append(ch, start, length);
        }
    }

    private void appendElementStart(String uri,
                                    String localName,
                                    String qName,
                                    Attributes a,
                                    StringBuffer out) {
        out.append("<" + qName);
        // do we have any newly-mapped namespaces?
        while (m_prefixList.size() > 0) {
            String prefix = (String) m_prefixList.remove(0);
            out.append(" xmlns:" + prefix + "=\"" + enc((String) m_prefixMap.get(prefix)) + "\"");
        }
        for (int i = 0; i < a.getLength(); i++) {
            out.append(" " + a.getQName(i) + "=\"" + enc(a.getValue(i)) + "\"");
        }
        out.append(">");
    }

    private void appendElementEnd(String uri,
                                  String localName,
                                  String qName,
                                  StringBuffer out) {
        out.append("</" + qName + ">");
    }

    public void startPrefixMapping(String prefix, String uri) {
        logger.info("Started prefix mapping: " + prefix + " => " + uri);
        m_prefixMap.put(prefix, uri);
        if (m_xmlData != null) {
            m_prefixList.add(prefix);
        }
    }

    public void endPrefixMapping(String prefix) {
        logger.info("Finished prefix mapping: " + prefix);
        m_prefixMap.remove(prefix);
    }

    private String enc(String in) {
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

    public void endElement(String uri, 
                           String localName, 
                           String qName) throws SAXException {
        if (uri.equals(METS) && localName.equals("dmdSec") && m_currentID != null) {
            logger.info("Finished parsing dmdSec (ID = " + m_currentID + ")");
            if (m_xmlDataString == null) {
                throw new SAXException("dmdSec element must contain xmlData");
            }
            // TODO: then save it
            System.out.println(m_xmlDataString);
            m_xmlDataString = null;
            m_currentID = null;
        } else if (m_currentID != null) {
            if (uri.equals(METS) && localName.equals("xmlData") && m_xmlData != null) {
                logger.info("Finished buffering xmlData for " + m_currentID);
                m_xmlDataString = m_xmlData.toString().trim();
                m_xmlData = null;
            } else if (m_xmlData != null) {
                // inside xmlData
                appendElementEnd(uri, localName, qName, m_xmlData);
            }
        }
    }

}