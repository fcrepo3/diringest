package fedora.services.diringest.ingest;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

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

    public StagingFOXMLParser(InputStream foxml, 
                              DatastreamStage stage,
                              List stagedURLs) throws Exception {
        m_stage = stage;
        m_stagedURLs = stagedURLs;
        m_out = new StringBuffer();
        m_out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(false); // This is intentional.  It makes 
                                      // the identity transform simpler.
        SAXParser parser = spf.newSAXParser();
        parser.parse(foxml, this);
    }

    public String getFOXMLString() {
        return m_out.toString();
    }

}