package fedora.services.diringest;

import java.util.*;

/**
 * A node in the tree of divs (representing objects OR datastreams) in the SIP.
 *
 * This tree exists only to aid in parsing of div sections.
 * When parsing is finished, it is used to construct the objects
 * we really want -- TreeNodes.
 */
public class DivNode implements TreeNode {

    private DivNode m_parent;
    private String m_label;
    private String m_type;
    private List m_children;
    private List m_objectChildren;
    private SIPContent m_sipContent;
    private List m_sipContents;

    public DivNode(DivNode parent, String label, String type) {
        m_parent = parent;
        m_label = label;
        m_type = type;
        m_children = new ArrayList();
    }

    //
    // Methods from the TreeNode interface
    //

    /**
     * Get the parent, or null if this is the root.
     */
    public TreeNode getParent() { return (TreeNode) m_parent; }

    /**
     * Return DivNode children that are objects.
     */
    public List getChildren() {
        if (m_objectChildren == null) {
            m_objectChildren = new ArrayList();
            for (int i = 0; i < m_children.size(); i++) {
                DivNode child = (DivNode) m_children.get(i);
                if (child.getSIPContent() == null)
                    m_objectChildren.add(child);
            }
        }
        return m_objectChildren;
    }

    public String getType() { return m_type; }
    public String getLabel() { return m_label; }

    public List getSIPContents() {
        // Return a list of SIPContent for each child DIV that's a datastream
        if (m_sipContents == null) {
            m_sipContents = new ArrayList();
            for (int i = 0; i < m_children.size(); i++) {
                DivNode child = (DivNode) m_children.get(i);
                SIPContent content = child.getSIPContent();
                if (content != null)
                    m_sipContents.add(content);
            }
        }
        return m_sipContents;
    }

    //
    // Other methods
    //

    // Used while building the tree
    public DivNode getParentDivNode() { return m_parent; }

    // Used while building the tree
    public void addChild(DivNode child) {
        m_children.add(child); 
    }

    // If this returns null, it can be inferred that this DivNode 
    // represents an object.  Otherwise, it represents a datastraem.
    public SIPContent getSIPContent() { return m_sipContent; }

    // Set the SIPContent (so this is a datastream DivNode)
    public void setSIPContent(SIPContent content) { 
        content.setType(m_type);
        content.setLabel(m_label);
        m_sipContent = content;
    }

}