package fedora.services.diringest.sip2fox;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

import fedora.common.*;

public class ConversionRules extends DefaultHandler {

    private static final Logger logger =
            Logger.getLogger(ConversionRules.class.getName());

    private Map m_namespaces;
    private Map m_oTemplates;
    private Map m_dTemplates;

    private String m_templateNodeType;
    private List m_attribs;
    private List m_relSpecs;
    private String m_relationshipName;
    private String m_relationshipPrefix;
    private List m_targetSpecs;

    public ConversionRules(Map namespaces, Map dTemplates, Map oTemplates) {
        m_namespaces = new HashMap();
        m_namespaces.put("rdf", Constants.RDF.uri);
        m_namespaces.put("tree", "info:fedora/fedora-system:def/tree#");
        m_dTemplates = dTemplates;
        m_oTemplates = oTemplates;
    }

    public ConversionRules(InputStream xml) throws Exception {
        m_namespaces = new HashMap();
        m_namespaces.put("rdf", Constants.RDF.uri);
        m_namespaces.put("tree", "info:fedora/fedora-system:def/tree#");
        m_oTemplates = new HashMap();
        m_dTemplates = new HashMap();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        parser.parse(xml, this);
    }

    public List getRelationships(TreeNode node) {
        List rels = new ArrayList();
        // first, apply default template (*) if it exists
        ObjectTemplate dt = getObjectTemplate("*");
        if (dt != null) rels.addAll(getImpliedRels(node, dt.getRelSpecs().iterator()));
        // then, apply exact template (matching type name) if it exists
        if (node.getType() != null) {
            ObjectTemplate et = getObjectTemplate(node.getType());
            if (et != null) rels.addAll(getImpliedRels(node, et.getRelSpecs().iterator()));
        }
        return rels;
    }

    private List getImpliedRels(TreeNode node, Iterator specIter) {
        List rels = new ArrayList();
        while (specIter.hasNext()) {
            RelSpec spec = (RelSpec) specIter.next();
            Iterator iter = spec.getTargetSpecs().iterator();
            while (iter.hasNext()) {
                List matches = ((TargetSpec) iter.next()).getMatches(node);
                for (int i = 0; i < matches.size(); i++) {
                    TreeNode target = (TreeNode) matches.get(i);
                    rels.add(new Relationship(spec.getName(), spec.getPrefix(), target));
                }
            }
        }
        return rels;
    }

    public String getNamespaceURI(String alias) {
        return (String) m_namespaces.get(alias);
    }

    public ObjectTemplate getObjectTemplate(String nodeType) {
        return (ObjectTemplate) m_oTemplates.get(nodeType);
    }

    public DatastreamTemplate getDatastreamTemplate(String nodeType) {
        return (DatastreamTemplate) m_dTemplates.get(nodeType);
    }

    public void startElement(String uriPart, 
                             String localName, 
                             String qName, 
                             Attributes a) throws SAXException {
        if (localName.equals("namespace")) {
            String alias = a.getValue("alias");
            String uri = a.getValue("uri");
            if (alias == null || uri == null) {
                throw new SAXException("'namespace' requires both an 'alias' and a 'uri' attribute.");
            }
            if (m_namespaces.containsKey(alias)) {
                throw new SAXException("namespace alias '" + alias + "' was already declared");
            }
            try {
                URI u = new URI(uri);
            } catch (Exception e) {
                throw new SAXException("Bad namespace uri: " + uri);
            }
            m_namespaces.put(alias, uri);
        }
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
            m_relationshipName = a.getValue("", "name");
            if (m_relationshipName == null) {
                throw new SAXException("'relationship' requires a 'name' attribute.");
            }
            String[] parts = m_relationshipName.split(":");
            if (parts.length != 2) {
                throw new SAXException("'relationship' name must have a single colon (:) delimiter: " + m_relationshipName);
            }
            if (parts[0].length() == 0) {
                throw new SAXException("'relationship' name must start with an alpha character: " + m_relationshipName);
            }
            String uriPrefix = (String) m_namespaces.get(parts[0]);
            if (uriPrefix == null) {
                throw new SAXException("'relationship' name prefix does not match a declared namespace alias: " + parts[0]);
            }
            String fullURI = uriPrefix + parts[1];
            try {
                URI uri = new URI(fullURI);
            } catch (Exception e) {
                throw new SAXException("'relationship' name implies a bad uri: " + fullURI);
            }
            logger.info("Parsed relspec, fullURI = " + fullURI);
            m_relationshipPrefix = parts[0];
            m_targetSpecs = new ArrayList();
        }
        if (localName.equals("target")) {
            String ktString = a.getValue("", "primitiveRel");
            if (ktString == null) {
                throw new SAXException("'target' requires a 'primitiveRel' attribute.");
            }
            String kt = ktString.toLowerCase();
            int kinType;
            if (kt.equals("tree:parent")) {
                kinType = TargetSpec.PARENT;
            } else if (kt.equals("tree:child")) {
                kinType = TargetSpec.CHILD;
            } else if (kt.equals("tree:ancestor")) {
                kinType = TargetSpec.ANCESTOR;
            } else if (kt.equals("tree:descendant")) {
                kinType = TargetSpec.DESCENDANT;
            } else {
                throw new SAXException("target requires a primitiveRel of tree:parent, tree:child, tree:ancestor, or tree:descendant.");
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
            m_relSpecs.add(new RelSpec(m_relationshipName, m_relationshipPrefix, m_targetSpecs));
        } else if (localName.equals("datastreamTemplate")) {
            m_dTemplates.put(m_templateNodeType, new DatastreamTemplate(m_templateNodeType, m_attribs));
        } else if (localName.equals("objectTemplate")) {
            m_oTemplates.put(m_templateNodeType, new ObjectTemplate(m_templateNodeType, m_attribs, m_relSpecs));
        }
    }

    public Map getNamespaceMap() {
        return m_namespaces;
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
                out.append("  Assume relationship " + r.getName().toString() + "\n");
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
