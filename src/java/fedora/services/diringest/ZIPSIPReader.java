package fedora.services.diringest;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.apache.log4j.*;

/**
 * A SIPReader for ZIP files.
 */
public class ZIPSIPReader implements SIPReader,
                                     DataResolver {

    private static final Logger logger =
            Logger.getLogger(ZIPSIPReader.class.getName());

    public static final String METS_ENTRY_PATH = "METS.xml";

    private ZipFile m_file;
    private METSReader m_metsReader;

    /**
     * Instantiate the reader parsing the METS and verifying
     * that the referenced files actually exist in the package.
     */
    public ZIPSIPReader(ZipFile file) throws Exception {
        m_file = file;
        m_metsReader = new METSReader(m_file.getInputStream(getMetsEntry()), this);

    }

    private ZipEntry getMetsEntry() throws IOException {
        ZipEntry metsEntry = m_file.getEntry(METS_ENTRY_PATH);
        if (metsEntry != null) {
            logger.info("Found METS in default location in zip: " + METS_ENTRY_PATH);
            return metsEntry;
        }
        Enumeration entries = m_file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().indexOf("/") == -1) {
                logger.info("Found METS in root directory of zip file: " + entry.getName());
                return entry;
            }
        }
        throw new IOException("Unable to locate METS in zip file.  It must be in the root.");
    }

    public TreeNode getRoot() {
        return m_metsReader.getRoot();
    }

    public void close() {
        try {
            m_file.close();
        } catch (IOException e) {
            logger.warn("Could not close ZIP file.", e);
        } finally {
            try {
                m_metsReader.close();
            } catch (IOException e) {
                logger.warn("Could not close METS reader.", e);
            }
        }
    }

    /** Currently only supports locators beginning with "file:" */
    public InputStream getData(String locatorType, String locator) throws IOException {
        throw new IOException("getData not implemented yet");
    }

}