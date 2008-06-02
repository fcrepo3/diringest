package fedora.services.diringest.sip2fox;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.*;

import fedora.common.*;
import fedora.services.diringest.common.*;
import fedora.services.diringest.sip2fox.pidgen.*;

/**
 * Makes a series of self-contained FOXML files given a tree,
 * a pid generator, and conversion rules.
 */
public class FOXMLMaker implements fedora.common.Constants {

    public static final String  OUTPUT_ENCODING         = "UTF-8";
    public static final boolean INCLUDE_SCHEMA_LOCATION = true;

    private static final Logger logger =
            Logger.getLogger(FOXMLMaker.class.getName());

    private ConversionRules m_rules;
    private List m_nodeQueue;
    private Map m_pidMap;

    public FOXMLMaker(PIDGenerator pidgen, 
                      ConversionRules rules, 
                      TreeNode rootNode,
                      String user,
                      String pass) throws Exception {
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

        // Generate PIDs and add to map for later
        m_pidMap = new HashMap();
        PID[] pids = pidgen.getNextPIDs(m_nodeQueue.size(), user, pass);
        for (int i = 0; i < m_nodeQueue.size(); i++) {
            TreeNode node = (TreeNode) m_nodeQueue.get(i);
            PID pid = pids[i];
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
        serializeManagedDatastreams(node, out, outStream);
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
        List rels = m_rules.getRelationships(node);
        // TODO: If they specified a RELS-EXT datastream in the SIP, 
        //       add those here.  Currently the only data RELS-EXT will hold
        //       is that which is inferred by the rules.
        if (rels.size() > 0) {
            startDatastream("RELS-EXT", "X", "A", "true", "application/rdf+xml", null,
                            "Relationship Metadata", out);
            out.println("      <xmlContent>");
            out.print(  "        <rdf:RDF xmlns:rdf=\"" + m_rules.getNamespaceURI("rdf") + "\"");
            // determine which namespaces should be declared
            Map toDeclare = new HashMap();
            for (int i = 0; i < rels.size(); i++) {
                String prefix = ((Relationship) rels.get(i)).getPrefix();
                toDeclare.put(prefix, m_rules.getNamespaceURI(prefix));
            }
            // ... then declare them
            Iterator iter = toDeclare.keySet().iterator();
            while (iter.hasNext()) {
                String prefix = (String) iter.next();
                if (!prefix.equals("rdf")) {
                    out.println();
                    out.print("                 xmlns:" + prefix + "=\""
                            + enc((String) toDeclare.get(prefix)) + "\"");
                }
            }
            out.println(">");
            out.println("          <rdf:Description rdf:about=\"" + "info:fedora/" + pid.toString() + "\">");
            for (int i = 0; i < rels.size(); i++) {
                Relationship rel = (Relationship) rels.get(i);
                String target = rel.getExternalTarget();
                if (target == null) {
                    target = ((PID) m_pidMap.get(rel.getTarget())).toString();
                }
                out.println("            <" + rel.getName() 
                        + " rdf:resource=\"" + "info:fedora/" + enc(target) + "\"/>");
            }
            out.println("          </rdf:Description>");
            out.println("        </rdf:RDF>");
            out.println("      </xmlContent>");
            endDatastream(out);
        }
    }

    private void serializeOtherInlineDatastreams(TreeNode node, PrintWriter out) throws IOException {
        logger.debug("Serializing inline datastreams for : " + node.getLabel());
        Iterator iter = node.getSIPContents().iterator();
        while (iter.hasNext()) {
            SIPContent content = (SIPContent) iter.next();
            if (content.wasInline()) {
                String id = getID(content);
                if (!id.equals("RELS-EXT")) {
                    logger.debug("Serializing inline datastream: " + id);
                    startDatastream(id, content, out);
                    out.println("      <xmlContent>");
                    if (logger.isDebugEnabled()) {
                        ByteArrayOutputStream outs = new ByteArrayOutputStream();
                        StreamUtil.pipe(content.getInputStream(), outs);
                        logger.debug(outs.toString("UTF-8"));
                    }
                    printXML(content.getInputStream(), out);
                    out.println("      </xmlContent>");
                    endDatastream(out);
                }
            }
        }
    }

    private void printXML(InputStream in, PrintWriter writer) throws IOException {
        try {
            // first format it nicely
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            OutputFormat fmt = new OutputFormat("XML", "UTF-8", true);
            fmt.setIndent(2);
            fmt.setLineWidth(120);
            fmt.setPreserveSpace(false);
            fmt.setOmitXMLDeclaration(true);
            fmt.setOmitDocumentType(true);
            XMLSerializer ser = new XMLSerializer(out, fmt);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(in);
            ser.serialize(doc);
            // ... then indent it
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                writer.println("        " + line);
                line = reader.readLine();
            }
        } catch (Exception e) {
            logger.warn("Error formatting inline XML.", e);
            throw new IOException("Unexpected error formatting inline XML.");
        } finally {
            in.close();
        }
    }

    private void startDatastream(String id, 
                                 SIPContent content, 
                                 PrintWriter out) {
        String cg = "M";
        if (content.wasInline()) cg = "X";
        String st = "A";
        String v = "true";
        String m = content.getMIMEType();
        String f = content.getFormatURI();
        String l = content.getLabel();
        startDatastream(id, cg, st, v, m, f, l, out);
    }

    private void startDatastream(String id,
                                 String cg,
                                 String st,
                                 String v,
                                 String m,
                                 String f,
                                 String l,
                                 PrintWriter out) {
        String labelInfo = "";
        if (l != null) {
            labelInfo = " LABEL=\"" + enc(l) + "\"";
        }
        String formatInfo = "";
        if (f != null && f.length() > 0) {
            formatInfo = " FORMAT_URI=\"" + enc(f) + "\"";
        }
        out.println("  <datastream ID=\"" + id + "\""
                               + " CONTROL_GROUP=\"" + cg + "\""
                               + " STATE=\"" + st + "\""
                               + " VERSIONABLE=\"" + v + "\">");
        out.println("    <datastreamVersion ID=\"" + id + ".0\""
                                        + " MIMETYPE=\"" + m + "\""
                                        + formatInfo
                                        + labelInfo + ">");
    }

    private void endDatastream(PrintWriter out) {
        out.println("    </datastreamVersion>");
        out.println("  </datastream>");
    }

    private void serializeManagedDatastreams(TreeNode node, 
                                             PrintWriter out,
                                             OutputStream outStream) throws IOException {
        Iterator iter = node.getSIPContents().iterator();
        while (iter.hasNext()) {
            SIPContent content = (SIPContent) iter.next();
            if (!content.wasInline()) {
                String id = getID(content);
                startDatastream(id, content, out);
                out.print("      <binaryContent>");
                out.flush();
                StreamUtil.base64Encode(content.getInputStream(), outStream);
                out.println("</binaryContent>");
                endDatastream(out);
            }
        }
    }

    public void serializeFooter(PrintWriter out) {
        out.print("</digitalObject>");
    }

    // Get the content model from the conversion rules, otherwise return null
    private String getContentModel(String objectType) {
        Map attribs = m_rules.getObjectAttributes(objectType);
        if (logger.isDebugEnabled()) {
            logger.debug(attribString(objectType, attribs));
        }
        return (String) attribs.get("contentModel");
    }

    // Get the id from the conversion rules, otherwise use the id from the SIP
    private String getID(SIPContent content) {
        String type = content.getType();
        Map attribs = m_rules.getDatastreamAttributes(type);
        if (logger.isDebugEnabled()) {
            logger.debug(attribString(type, attribs));
        }
        String id = (String) attribs.get("id");
        if (id == null) id = content.getID();
        return id;
    }

    private String attribString(String type, Map attribs) {
        StringBuffer out = new StringBuffer();
        out.append("Attributes for " + type + ": ");
        Iterator iter = attribs.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = (String) attribs.get(key);
            out.append("'" + key + "' = " + value + " ");
        }
        return out.toString();
    }

    private String enc(String in) {
        return StreamUtil.xmlEncode(in);
    }

}
