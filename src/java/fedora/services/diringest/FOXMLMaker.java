package fedora.services.diringest;

import java.io.*;
import org.apache.log4j.*;

import fedora.services.diringest.pidgen.*;

/**
 * Makes a series of self-contained FOXML files given a tree,
 * a pid generator, and conversion rules.
 */
public class FOXMLMaker {

    private static final Logger logger =
            Logger.getLogger(FOXMLMaker.class.getName());

    private PIDGenerator m_pidgen;
    private TreeNode m_rootNode;

    public FOXMLMaker(PIDGenerator pidgen, TreeNode rootNode) {
        m_pidgen = pidgen;
        m_rootNode = rootNode;
    }

    public boolean hasNext() {
        return false; // TODO: impl
    }

    public FOXMLResult makeNext() throws IOException {
        return null; // TODO: impl
    }

}