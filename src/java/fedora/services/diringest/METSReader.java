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
    public static final String INLINE_XML_MIMETYPE = "text/xml";

    private static final Logger logger =
            Logger.getLogger(METSReader.class.getName());

    private TreeNode m_root;
    private DataResolver m_dataResolver;

    private Map m_contentMap;

    private Map m_prefixMap;
    private List m_prefixList;

    private String m_currentID;
    private File m_xmlDataFile;
    private List m_filesToDelete;
    private Writer m_xmlData;

    public METSReader(InputStream xml, DataResolver dataResolver) throws Exception {
        m_dataResolver = dataResolver;
        m_contentMap = new HashMap();
        m_prefixMap = new HashMap();
        m_prefixList = new ArrayList();
        m_filesToDelete = new ArrayList();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        parser.parse(xml, this);
        /*
        Iterator iter = m_contentMap.values().iterator();
        while (iter.hasNext()) {
            SIPContent c = (SIPContent) iter.next();
            System.out.println("## Content id = " + c.getID());
            System.out.println("    wasInline = " + c.wasInline());
            System.out.println("    mimeType  = " + c.getMIMEType());
            BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            reader.close();
        }
        */
    }

    public TreeNode getRoot() {
        return m_root;
    }

    /**
     * Clean up temporary storage area used for disk-backed access to inline
     * XML datastreams.
     */
    public void close() throws IOException {
        logger.info("Deleting " + m_filesToDelete.size() + " temporary files.");
        for (int i = 0; i < m_filesToDelete.size(); i++) {
            File f = (File) m_filesToDelete.get(i);
            f.delete();
        }
    }

    public void startElement(String uri, 
                             String localName, 
                             String qName, 
                             Attributes a) throws SAXException {
        if (uri.equals(METS) && localName.equals("dmdSec") && m_currentID == null) {
            // Starting a dmdSec
            m_currentID = a.getValue("", "ID");
            if (m_currentID == null) {
                throw new SAXException("dmdSec element must have an ID attribute");
            }
            logger.info("Started parsing dmdSec (ID = " + m_currentID + ")");
        } else if (m_currentID != null) {
            // Inside a dmdSec
            if (uri.equals(METS) && localName.equals("xmlData") && m_xmlData == null) {
                // Starting an xmlData 
                try {
                    m_xmlDataFile = File.createTempFile("diringest-xmlData", null);
                    m_xmlDataFile.deleteOnExit();
                    m_xmlData = new OutputStreamWriter(new FileOutputStream(m_xmlDataFile), "UTF-8");
                    logger.info("Started buffering xmlData for " + m_currentID + " to file: " + m_xmlDataFile.getPath());
                } catch (Exception e) {
                    String msg = "Unable to create temporary file for xmlData";
                    logger.warn(msg, e);
                    throw new SAXException(msg);
                }
            } else if (m_xmlData != null) {
                // inside xmlData
                try {
                    writeElementStart(uri, localName, qName, a, m_xmlData);
                } catch (Exception e) {
                    String msg = "Unable to write element start for xmlData";
                    logger.warn(msg, e);
                    throw new SAXException(msg);
                }
            }
        } else if (uri.equals(METS) && localName.equals("div")) {
            // if has no parent
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (m_xmlData != null) {
            try {
                m_xmlData.write(ch, start, length);
            } catch (Exception e) {
                String msg = "Unable to write characters for xmlData";
                logger.warn(msg, e);
                throw new SAXException(msg);
            }
        }
    }

    private void writeElementStart(String uri,
                                    String localName,
                                    String qName,
                                    Attributes a,
                                    Writer out) throws IOException {
        out.write("<" + qName);
        // do we have any newly-mapped namespaces?
        while (m_prefixList.size() > 0) {
            String prefix = (String) m_prefixList.remove(0);
            out.write(" xmlns:" + prefix + "=\"" + enc((String) m_prefixMap.get(prefix)) + "\"");
        }
        for (int i = 0; i < a.getLength(); i++) {
            out.write(" " + a.getQName(i) + "=\"" + enc(a.getValue(i)) + "\"");
        }
        out.write(">");
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
            if (m_xmlDataFile == null) {
                throw new SAXException("dmdSec element must contain xmlData");
            }
            m_filesToDelete.add(m_xmlDataFile);
            m_contentMap.put(m_currentID, new FileBasedSIPContent(m_currentID, true, INLINE_XML_MIMETYPE, m_xmlDataFile));
            m_xmlDataFile = null;
            m_currentID = null;
        } else if (m_currentID != null) {
            if (uri.equals(METS) && localName.equals("xmlData") && m_xmlData != null) {
                try {
                    m_xmlData.close();
                } catch (Exception e) {
                    String msg = "Unable to close temporary xmlData file";
                    logger.warn(msg, e);
                    throw new SAXException(msg);
                }
                logger.info("Finished buffering xmlData for " + m_currentID);
                m_xmlData = null;
            } else if (m_xmlData != null) {
                // inside xmlData
                try {
                    m_xmlData.write("</" + qName + ">");
                } catch (Exception e) {
                    String msg = "Unable to write element end for xmlData";
                    logger.warn(msg, e);
                    throw new SAXException(msg);
                }
            }
        }
    }

}