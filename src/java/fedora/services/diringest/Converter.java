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
     * Convert the given SIP (a zip file) into a series of self-contained
     * FOXML files.
     *
     * @throws IOException If conversion failed for any reason.
     */
    public FOXMLResult[] convert(File zipFile) throws IOException {
        if (!zipFile.exists()) {
            throw new IOException("File not found: " + zipFile.getPath());
        }
        if (zipFile.isDirectory()) {
            throw new IOException("Not a file: " + zipFile.getPath());
        }
        SIPReader reader = new SIPReader(new ZipFile(zipFile));
        try {
            List resultList = new ArrayList();
            FOXMLMaker maker = new FOXMLMaker(m_pidgen, reader);
            while (maker.hasNext()) {
                resultList.add(maker.makeNext());
            }
            return (FOXMLResult[]) resultList.toArray(new FOXMLResult[0]);
        } finally {
            reader.close();
        }
    }



}