package fedora.services.diringest;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class ConversionRules extends DefaultHandler {

    private static final Logger logger =
            Logger.getLogger(ConversionRules.class.getName());

    private Map m_oTemplates;
    private Map m_dTemplates;

    private String m_templateNodeType;
    private List m_attribs;
    private List m_relSpecs;
    private URI m_relationshipURI;
    private List m_targetSpecs;

    public ConversionRules(Map dTemplates, Map oTemplates) {
        m_dTemplates = dTemplates;
        m_oTemplates = oTemplates;
    }

    public ConversionRules(InputStream xml) throws Exception {
        m_oTemplates = new HashMap();
        m_dTemplates = new HashMap();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        parser.parse(xml, this);
    }

    public List getRelationships(TreeNode node) {
        List rels = new ArrayList();
        // TODO: Actually populate these
        return rels;
    }

    public ObjectTemplate getObjectTemplate(String nodeType) {
        return (ObjectTemplate) m_oTemplates.get(nodeType);
    }

    public DatastreamTemplate getDatastreamTemplate(String nodeType) {
        return (DatastreamTemplate) m_dTemplates.get(nodeType);
    }

    public void startElement(String uri, 
                             String localName, 
                             String qName, 
                             Attributes a) throws SAXException {
        if (localName.equals("objectTemplate")) {
            m_templateNodeType = a.getValue("", "nodeType");
            if (m_templateNodeType == null)
                throw new SAXException("'objectTemplate' requires a 'nodeType' attribute.");
            logger.info("Parsing object template: " + m_templateNodeType);
            m_attribs = new ArrayList();
            m_relSpecs = new ArrayList();
        }
        if (localName.equals("datastreamTemplate")) {
            m_templateNodeType = a.getValue("", "nodeType");
            if (m_templateNodeType == null)
                throw new SAXException("'datastreamTemplate' requires a 'nodeType' attribute.");
            logger.info("Parsing datastream template: " + m_templateNodeType);
            m_attribs = new ArrayList();
        }
        if (localName.equals("attribute")) {
            String name = a.getValue("", "name");
            if (name == null)
                throw new SAXException("'attribute' requires 'name' attribute.");
            String value = a.getValue("", "value");
            if (value == null)
                throw new SAXException("'attribute' requires 'value' attribute.");
            logger.info("Parsed attribute: " + name + " = " + value);
            m_attribs.add(new Attribute(name, value));
        }
        if (localName.equals("relationship")) {
            String uriString = a.getValue("", "uri");
            if (uriString == null)
                throw new SAXException("'relationship' requires a 'uri' attribute.");
            try {
                m_relationshipURI = new URI(uriString);
            } catch (Exception e) {
                throw new SAXException("'relationship' has bad 'uri' value: " + uriString);
            }
            logger.info("Parsing relspec: " + uriString);
            m_targetSpecs = new ArrayList();
        }
        if (localName.equals("target")) {
            String ktString = a.getValue("", "kinType");
            if (ktString == null) {
                throw new SAXException("'target' requires a 'kinType' attribute.");
            }
            String kt = ktString.toLowerCase();
            int kinType;
            if (kt.equals("parent")) {
                kinType = TargetSpec.PARENT;
            } else if (kt.equals("child")) {
                kinType = TargetSpec.CHILD;
            } else if (kt.equals("ancestor")) {
                kinType = TargetSpec.ANCESTOR;
            } else if (kt.equals("descendant")) {
                kinType = TargetSpec.DESCENDANT;
            } else {
                throw new SAXException("'target' requires a 'kinType' of 'parent', 'child', 'ancestor', or 'descendant'.");
            }
            String nodeType = a.getValue("", "nodeType");
            if (nodeType == null)
                throw new SAXException("'target' requires a 'nodeType' attribute.");
            m_targetSpecs.add(new TargetSpec(kinType, nodeType));
            logger.info("Parsed targetspec: " + kt + " => " + nodeType);
        }
    }

    public void endElement(String uri, 
                           String localName, 
                           String qName) {
        if (localName.equals("relationship")) {
            m_relSpecs.add(new RelSpec(m_relationshipURI, m_targetSpecs));
        } else if (localName.equals("datastreamTemplate")) {
            m_dTemplates.put(m_templateNodeType, new DatastreamTemplate(m_templateNodeType, m_attribs));
        } else if (localName.equals("objectTemplate")) {
            m_oTemplates.put(m_templateNodeType, new ObjectTemplate(m_templateNodeType, m_attribs, m_relSpecs));
        }
    }

    public Map getObjectTemplates() {
        return m_oTemplates;
    }

    public Map getDatastreamTemplates() {
        return m_dTemplates;
    }

    /**
     * Get a human-readable description of what the conversion rules specify.
     *
     * This is a good way of testing that a configuration does what you expect it to.
     */
    public String getDescription() {
        StringBuffer out = new StringBuffer();
        out.append("Contains " + m_dTemplates.keySet().size() + " datastream templates.\n");
        Iterator iter = m_dTemplates.values().iterator();
        while (iter.hasNext()) {
            DatastreamTemplate t = (DatastreamTemplate) iter.next();
            out.append("For " + t.getNodeType() + " nodes:\n");
            appendAttributes(t.getAttributes(), out);
        }
        out.append("Contains " + m_oTemplates.keySet().size() + " object templates.\n");
        iter = m_oTemplates.values().iterator();
        while (iter.hasNext()) {
            ObjectTemplate t = (ObjectTemplate) iter.next();
            out.append("For " + t.getNodeType() + " nodes:\n");
            appendAttributes(t.getAttributes(), out);
            for (int i = 0; i < t.getRelSpecs().size(); i++) {
                RelSpec r = (RelSpec) t.getRelSpecs().get(i);
                out.append("  Assume relationship " + r.getURI().toString() + "\n");
                for (int j = 0; j < r.getTargetSpecs().size(); j++) {
                    TargetSpec ts = (TargetSpec) r.getTargetSpecs().get(j);
                    String nodeType = ts.getNodeType();
                    String kinType = "descendant";
                    int k = ts.getKinType();
                    if (k == ts.PARENT) kinType = "parent";
                    if (k == ts.CHILD) kinType = "child";
                    if (k == ts.ANCESTOR) kinType = "ancestor";
                    out.append("  ..to any " + kinType + " of type " + nodeType + "\n");
                }
            }
        }
        return out.toString();
    }

    private void appendAttributes(List attribs, StringBuffer out) {
        for (int i = 0; i < attribs.size(); i++) {
            Attribute a = (Attribute) attribs.get(i);
            out.append("  Assume attribute " + a.getName() + " = " + a.getValue() + "\n");
        }
    }

    public static void main(String[] args) throws Exception {
        SIP2FOX.initLogging();
        ConversionRules r = new ConversionRules(new FileInputStream(new File(args[0])));
        System.out.println(r.getDescription());
    }

}