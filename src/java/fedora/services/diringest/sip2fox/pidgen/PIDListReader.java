package fedora.services.diringest.sip2fox.pidgen;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import fedora.common.*;

public class PIDListReader extends DefaultHandler {

    private static final Logger logger =
            Logger.getLogger(PIDListReader.class.getName());

    private List m_pids;
    private StringBuffer m_pidBuffer;
    private boolean m_inPID;

    public PIDListReader(InputStream xml) throws IOException {
        m_pids = new ArrayList();
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            SAXParser parser = spf.newSAXParser();
            parser.parse(xml, this);
        } catch (Exception e) {
            logger.warn("Error parsing pidList", e);
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getName();
            throw new IOException("Error parsing pidList: " + msg);
        }
    }

    public PID[] getPIDArray() {
        PID[] array = new PID[m_pids.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = (PID) m_pids.get(i);
        }
        return array;
    }

    public void startElement(String uri, 
                             String localName, 
                             String qName, 
                             Attributes a) {
        if (localName.equals("pid")) {
            m_inPID = true;
            m_pidBuffer = new StringBuffer();
        }
    }

    public void characters(char[] ch, int start, int length) {
        if (m_inPID) {
            m_pidBuffer.append(ch, start, length);
        }
    }

    public void endElement(String uri, 
                           String localName, 
                           String qName) throws SAXException {
        if (localName.equals("pid")) {
            m_pids.add(PID.getInstance(m_pidBuffer.toString()));
            m_inPID = false;
        }
    }

}
