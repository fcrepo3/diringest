package fedora.services.diringest;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.*;
import org.apache.log4j.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class CRules extends DefaultHandler {

    private static final Logger logger =
            Logger.getLogger(CRules.class.getName());

    private Map m_oTemplates;
    private Map m_dTemplates;

    private String m_templateNodeType;
    private List m_relSpecs;
    private URI m_relationshipURI;
    private List m_targetSpecs;

    public CRules(Map oTemplates) {
        m_oTemplates = oTemplates;
    }

    public CRules(InputStream xml) throws Exception {
        m_oTemplates = new HashMap();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        parser.parse(xml, this);
    }

    public ObjectTemplate getObjectTemplate(String nodeType) {
        return (ObjectTemplate) m_oTemplates.get(nodeType);
    }

    public ObjectTemplate getDatastreamTemplate(String nodeType) {
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
            logger.info("Parsing template: " + m_templateNodeType);
            m_relSpecs = new ArrayList();
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
        } else if (localName.equals("objectTemplate")) {
            m_oTemplates.put(m_templateNodeType, new ObjectTemplate(m_templateNodeType, m_relSpecs));
        }
    }

    public Map getObjectTemplates() {
        return m_oTemplates;
    }

    public Map getDatastreamTemplates() {
        return m_dTemplates;
    }

    public String getDescription() {
        StringBuffer out = new StringBuffer();
        out.append("Contains " + m_dTemplates.keySet().size() + " datastream templates.\n");
        out.append("Contains " + m_oTemplates.keySet().size() + " object templates.\n");
        Iterator iter = m_oTemplates.values().iterator();
        int tNum = 0;
        while (iter.hasNext()) {
            tNum++;
            ObjectTemplate t = (ObjectTemplate) iter.next();
            out.append("For " + t.getNodeType() + " nodes:\n");
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

    public static void main(String[] args) throws Exception {
        SIP2FOX.initLogging();
        CRules r = new CRules(new FileInputStream(new File(args[0])));
        System.out.println(r.getDescription());
    }

}