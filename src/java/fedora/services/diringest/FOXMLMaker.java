package fedora.services.diringest;

import java.io.*;
import org.apache.log4j.*;

import fedora.services.diringest.pidgen.*;

/**
 * Makes a series of self-contained FOXML files from a SIPReader.
 */
public class FOXMLMaker {

    private static final Logger logger =
            Logger.getLogger(FOXMLMaker.class.getName());

    private PIDGenerator m_pidgen;
    private SIPReader m_reader;

    public FOXMLMaker(PIDGenerator pidgen, SIPReader reader) {
        m_pidgen = pidgen;
        m_reader = reader;
    }

    public boolean hasNext() {
        return false; // TODO: impl
    }

    public FOXMLResult makeNext() throws IOException {
        return null; // TODO: impl
    }

}