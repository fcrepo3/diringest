package fedora.services.diringest;

import java.io.*;
import java.util.zip.*;
import org.apache.log4j.*;

/**
 * Parses and provides access to the contents of a SIP.
 */
public class SIPReader {

    private static final Logger logger =
            Logger.getLogger(SIPReader.class.getName());

    private ZipFile m_file;

    /**
     * Instantiate the reader parsing the METS and verifying
     * that the referenced files actually exist in the package.
     */
    public SIPReader(ZipFile file) {
        m_file = file;
        logger.info("Searching for METS file");

    }

    public void close() {
        try {
            m_file.close();
        } catch (IOException e) {
            logger.warn("Could not close ZIP file.", e);
        }
    }

}