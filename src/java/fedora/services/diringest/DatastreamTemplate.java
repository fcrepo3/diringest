package fedora.services.diringest;

import java.util.*;

public class DatastreamTemplate implements Template {

    private String m_nodeType;
    private List m_attribs;

    public DatastreamTemplate(String nodeType, List attribs) {
        m_nodeType = nodeType;
        m_attribs = attribs;
    }

    public String getNodeType() {
        return m_nodeType;
    }

    public List getAttributes() {
        return m_attribs;
    }

}