package fedora.services.diringest;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.apache.log4j.*;

import fedora.services.diringest.pidgen.*;

/**
 * Utility for converting METS-encoded SIPs to self-contained FOXML objects.
 */
public class Converter {

    private static final Logger logger = 
            Logger.getLogger(Converter.class.getName());

    private PIDGenerator m_pidgen;

    public Converter(PIDGenerator pidgen) {
        m_pidgen = pidgen;
    }

    /**
     * Convert the given SIP into a series of self-contained FOXML files.
     *
     * @throws IOException If conversion failed for any reason.
     */
    public FOXMLResult[] convert(File sipFile) throws Exception {
        SIPReader reader = getSIPReader(sipFile);
        try {
            List resultList = new ArrayList();
            FOXMLMaker maker = new FOXMLMaker(m_pidgen, reader.getRoot());
            while (maker.hasNext()) {
                resultList.add(maker.makeNext());
            }
            return (FOXMLResult[]) resultList.toArray(new FOXMLResult[0]);
        } finally {
            reader.close();
        }
    }

    // only supports zip-based sips for now
    private SIPReader getSIPReader(File sipFile) throws Exception {
        if (!sipFile.exists()) {
            throw new IOException("File not found: " + sipFile.getPath());
        }
        if (sipFile.isDirectory()) {
            throw new IOException("Not a file: " + sipFile.getPath());
        }
        return new ZIPSIPReader(new ZipFile(sipFile));
    }

}