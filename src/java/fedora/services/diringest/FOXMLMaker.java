package fedora.services.diringest;

import java.io.*;
import java.util.*;
import org.apache.log4j.*;

import fedora.common.*;
import fedora.services.diringest.pidgen.*;

/**
 * Makes a series of self-contained FOXML files given a tree,
 * a pid generator, and conversion rules.
 */
public class FOXMLMaker implements fedora.common.Constants {

    public static final String  OUTPUT_ENCODING         = "UTF-8";
    public static final boolean INCLUDE_SCHEMA_LOCATION = false;

    private static final Logger logger =
            Logger.getLogger(FOXMLMaker.class.getName());

    private ConversionRules m_rules;
    private List m_nodeQueue;
    private Map m_pidMap;

    public FOXMLMaker(PIDGenerator pidgen, 
                      ConversionRules rules, 
                      TreeNode rootNode) throws Exception {
        m_rules = rules;
        m_nodeQueue = new ArrayList();
        queueNodes(rootNode);

        // Before generating PIDs, ensure referenced content can be resolved.
        for (int i = 0; i < m_nodeQueue.size(); i++) {
            TreeNode node = (TreeNode) m_nodeQueue.get(i);
            Iterator iter = node.getSIPContents().iterator();
            while (iter.hasNext()) {
                SIPContent content = (SIPContent) iter.next();
                content.check();
            }
        }
        logger.info("All content is resolvable, OK");

        // Generate PIDs while adding to map
        m_pidMap = new HashMap();
        for (int i = 0; i < m_nodeQueue.size(); i++) {
            TreeNode node = (TreeNode) m_nodeQueue.get(i);
            PID pid = pidgen.getNextPID();
            logger.info("Generated PID: " + pid.toString());
            m_pidMap.put(node, pid);
        }
    }

    public boolean hasNext() {
        return (m_nodeQueue.size() > 0);
    }

    public FOXMLResult next() throws IOException {
        if (!hasNext()) return null;
        TreeNode node = (TreeNode) m_nodeQueue.remove(0);
        PID pid = (PID) m_pidMap.get(node);
        File foxmlFile = make(pid, node);
        FOXMLResult result = new FOXMLResult(foxmlFile, pid);
        return result;
    }

    private void queueNodes(TreeNode node) {
        m_nodeQueue.add(node);
        Iterator iter = node.getChildren().iterator();
        while (iter.hasNext()) {
            queueNodes((TreeNode) iter.next());
        }
    }

    private File make(PID pid, TreeNode node) throws IOException {
        File file = File.createTempFile("diringest-" + pid.toFilename(), ".xml");
        file.deleteOnExit();
        FileOutputStream outStream = new FileOutputStream(file);
        PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(outStream, OUTPUT_ENCODING));
        try {
            serialize(pid, node, writer, outStream);
        } finally {
            writer.flush();
            writer.close();
        }
        return file;
    }

    private void serialize(PID pid, 
                           TreeNode node, 
                           PrintWriter out, 
                           OutputStream outStream) throws IOException {
        serializeHeader(pid.toString(), out);
        serializeProperties(node, out);
        serializeRelationships(pid, node, out);
        serializeOtherInlineDatastreams(node, out);
        serializeManagedDatastreams(node, out);
        serializeFooter(out);
    }

    private void serializeHeader(String pid, PrintWriter out) {
        out.println("<digitalObject xmlns=\"info:fedora/fedora-system:def/foxml#\"");
        if (INCLUDE_SCHEMA_LOCATION) {
            out.println("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            out.println("               xsi:schemaLocation=\"info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-0.xsd\"");
        }
        out.println("               PID=\"" + pid.toString() + "\">");
    }

    private void serializeProperties(TreeNode node, PrintWriter out) {
        out.println("  <objectProperties>");
        out.println("    <property NAME=\"" + MODEL.STATE.uri + "\""
                              + " VALUE=\"A\"/>");
        out.println("    <property NAME=\"" + MODEL.LABEL.uri + "\""
                              + " VALUE=\"" + enc(node.getLabel()) + "\"/>");
        String cModel = getContentModel(node.getType());
        if (cModel != null) {
            out.println("    <property NAME=\"" + MODEL.CONTENT_MODEL.uri + "\""
                                  + " VALUE=\"" + enc(cModel) + "\"/>");
        }
        out.println("  </objectProperties>");
    }   

    private void serializeRelationships(PID pid, TreeNode node, PrintWriter out) {
    }

    private void serializeOtherInlineDatastreams(TreeNode node, PrintWriter out) {
        Iterator iter = node.getSIPContents().iterator();
        while (iter.hasNext()) {
            SIPContent content = (SIPContent) iter.next();
            if (content.wasInline()) {
                String id = getID(content.getType());
                if (!id.equals("RELS-EXT")) {
                    startDatastream(id, "X", "A", "true", out);
                }
            }
        }
    }

    private void startDatastream(String id, 
                                 SIPContent content, 
                                 PrintWriter out) {
        String cg = "M";
        if (content.wasInline()) cg = "X";
        String st = "A";
        String v = "true";
        out.println("  <datastream ID=\"" + id + "\""
                               + " CONTROL_GROUP=\"" + cg + "\""
                               + " STATE=\"" + st + "\""
                               + " VERSIONABLE=\"" + v + "\">");
    }

    private void serializeManagedDatastreams(TreeNode node, PrintWriter out) {
        Iterator iter = node.getSIPContents().iterator();
        while (iter.hasNext()) {
            SIPContent content = (SIPContent) iter.next();
            if (!content.wasInline()) {
            }
        }
    }

    public void serializeFooter(PrintWriter out) {
        out.print("</digitalObject>");
    }

    private String getContentModel(String objectType) {

        // TODO: derive content model from rules, and document assumptions

        if (objectType == null) return null;
        return null;
    }

    private String getID(SIPContent content) {
        String type = content.getType();

        // TODO: derive datastream id from rules, and document assumptions

        if (type == null) return content.getID();
        return content.getID();
    }

    private String enc(String in) {
        return StreamUtil.xmlEncode(in);
    }

}