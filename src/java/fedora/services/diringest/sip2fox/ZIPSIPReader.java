package fedora.services.diringest.sip2fox;

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

    /** Curently only supports locators beginning with "file:" */
    public InputStream getData(String locatorType, String locator) throws IOException {
        return m_file.getInputStream(getEntry(locator));
    }

    public void checkResolvability(String locatorType, String locator) throws Exception {
        getEntry(locator);
    }

    private ZipEntry getEntry(String locator) throws IOException {
        // remove leading file://\\/\/\ characters
        if (!locator.startsWith("file:")) {
            throw new IOException("Bad locator syntax: " + locator);
        }
        String path = locator.substring(5);
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        //Interpret "+" and "%20" as a space (" "), and ensure that the "/" character
        //is used as a directory seperator
        path = path.replaceAll("\\+", " ").replaceAll("%20", " ").replaceAll("\\\\", "/");
        
        ZipEntry entry = m_file.getEntry(path);
        if (entry == null) {
            // try same path, but with /'s as \'s
            entry = m_file.getEntry(path.replaceAll("/", "\\\\"));
        }
        if (entry == null) {
            logger.info("All ZIP entry names follow:");
            Enumeration entries = m_file.entries();
            while (entries.hasMoreElements()) {
                entry = (ZipEntry) entries.nextElement();
                logger.info("\"" + entry.getName() + "\"");
            }
            throw new IOException("ZIP entry not found: " + path);
        }
        return entry;
    }

}
