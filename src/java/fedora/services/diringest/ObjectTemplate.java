package fedora.services.diringest;

import java.util.*;

public class ObjectTemplate implements Template {

    private String m_nodeType;
    private List m_attribs;
    private List m_relSpecs;

    public ObjectTemplate(String nodeType, List attribs, List relSpecs) {
        m_nodeType = nodeType;
        m_attribs = attribs;
        m_relSpecs = relSpecs;
    }

    public String getNodeType() {
        return m_nodeType;
    }

    public List getAttributes() {
        return m_attribs;
    }

    public List getRelSpecs() {
        return m_relSpecs;
    }

}