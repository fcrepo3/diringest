package fedora.services.diringest.ingest;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;

import net.iharder.base64.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import fedora.services.diringest.common.*;

/**
 * Parses a FOXML stream with base64Binary datastreams, converting them
 * to staged URL references.
 *
 * Implementation note: This class assumes that the default namespace
 * of the input is the foxml namespace, for simplicity's sake.
 */
public class StagingFOXMLParser extends DefaultHandler {

    private static final Logger logger =
            Logger.getLogger(StagingFOXMLParser.class.getName());

    private DatastreamStage m_stage;
    private List m_stagedURLs;

    private StringBuffer m_out;

    private int m_xmlContentLevel;
    private boolean m_inBinaryContent;

    private File m_stageFile;
    private Writer m_stageWriter;

    public StagingFOXMLParser(InputStream foxml, 
                              DatastreamStage stage,
                              List stagedURLs) throws Exception {
        m_stage = stage;
        m_stagedURLs = stagedURLs;
        m_xmlContentLevel = 0;
        m_inBinaryContent = false;
        m_out = new StringBuffer();
        m_out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false); // This is intentional.  It makes 
                                      // the identity transform simpler.
        SAXParser parser = spf.newSAXParser();
        parser.parse(foxml, this);
    }

    public void startElement(String uri, 
                             String localName, 
                             String qName, 
                             Attributes a) throws SAXException {
        if (qName.equals("xmlContent")) {
            m_xmlContentLevel++;
        }
        if (m_xmlContentLevel == 0 && qName.equals("binaryContent")) {
            m_inBinaryContent = true;
            try {
                m_stageFile = File.createTempFile("diringest-stage", ".tmp");
                m_stageFile.deleteOnExit();
                m_stageWriter = new BufferedWriter(
                                    new OutputStreamWriter(
                                        new Base64.OutputStream(
                                            new FileOutputStream(m_stageFile), Base64.DECODE)));
            } catch (Exception e) {
                if (m_stageFile != null) {
                    m_stageFile.delete();
                }
                logger.warn("Unable to begin staging content", e);
                throw new SAXException("Unable to begin staging content", e);
            }
        } else {
            m_out.append("<" + qName);
            for (int i = 0; i < a.getLength(); i++) {
                m_out.append(" " + a.getQName(i) + "=\"" 
                        + StreamUtil.xmlEncode(a.getValue(i)) + "\"");
            }
            m_out.append(">");
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (m_inBinaryContent) {
            try {
                m_stageWriter.write(ch, start, length);
            } catch (IOException e) {
                String msg = "Error writing to stage file";
                logger.warn(msg, e);
                throw new SAXException(msg, e);
            }
        } else {
            m_out.append(ch, start, length);
        }
    }

    public void endElement(String uri, 
                           String localName, 
                           String qName) throws SAXException {
        if (qName.equals("xmlContent")) {
            m_xmlContentLevel--;
        }
        if (m_xmlContentLevel == 0 && qName.equals("binaryContent")) {
            // Stage the content, add the URL to the list, and use it in xml
            try {
                m_stageWriter.flush();
                m_stageWriter.close();
                URL stagedLocation = m_stage.stageContent(m_stageFile);
                m_stagedURLs.add(stagedLocation);
                m_out.append("<contentLocation REF=\"" + stagedLocation.toString() + "\" TYPE=\"URL\"/>");
                m_stageFile = null;
                m_inBinaryContent = false;
            } catch (IOException e) {
                String msg = "Unable to stage content";
                logger.warn(msg, e);
                throw new SAXException(msg, e);
            }
        } else {
            m_out.append("</" + qName + ">");
        }
    }

    public String getFOXMLString() {
        return m_out.toString();
    }

}
