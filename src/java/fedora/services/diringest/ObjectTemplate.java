package fedora.services.diringest;

import java.util.*;

public class ObjectTemplate {

    private String m_nodeType;
    private List m_relSpecs;

    public ObjectTemplate(String nodeType, List relSpecs) {
        m_nodeType = nodeType;
        m_relSpecs = relSpecs;
    }

    public String getNodeType() {
        return m_nodeType;
    }

    public List getRelSpecs() {
        return m_relSpecs;
    }

}