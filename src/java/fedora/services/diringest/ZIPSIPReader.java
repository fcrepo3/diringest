package fedora.services.diringest;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.apache.log4j.*;

/**
 * A SIPReader for ZIP files.
 */
public class ZIPSIPReader implements SIPReader {

    private static final Logger logger =
            Logger.getLogger(ZIPSIPReader.class.getName());

    private ZipFile m_file;

    /**
     * Instantiate the reader parsing the METS and verifying
     * that the referenced files actually exist in the package.
     */
    public ZIPSIPReader(ZipFile file) {
        m_file = file;
        logger.info("Searching for METS file");
    }

    public TreeNode getRoot() {
        return null;
    }

    public void close() {
        try {
            m_file.close();
        } catch (IOException e) {
            logger.warn("Could not close ZIP file.", e);
        }
    }

//    public InputStream getContent(String datastreamID) {
//    }

}