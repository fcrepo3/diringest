package fedora.services.diringest;

import java.util.*;

/**
 * A node in the tree of divs (representing objects OR datastreams) in the SIP.
 *
 * This tree exists only to aid in parsing of div sections.
 * When parsing is finished, it is used to construct the objects
 * we really want -- TreeNodes.
 */
public class DivNode {

    private DivNode m_parent;
    private String m_label;
    private String m_type;
    private List m_children;
    private SIPContent m_sipContent; // if null, this is an object node,
                                     // else it's a datastream

    public DivNode(DivNode parent, String label, String type) {
        m_parent = parent;
        m_label = label;
        m_type = type;
        m_children = new ArrayList();
    }

    public DivNode getParent() { return m_parent; }
    public List getChildren() { return m_children; }
    public void addChild(DivNode child) { m_children.add(child); }
    public String getType() { return m_type; }
    public String getLabel() { return m_label; }
    public SIPContent getSIPContent() { return m_sipContent; }
    public void setSIPContent(SIPContent content) { m_sipContent = content; }

}