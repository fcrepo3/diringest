package fedora.services.diringest;

import java.util.*;

/**
 * A concrete TreeNode stored in memory.
 */
public class MemoryTreeNode implements TreeNode {

    private TreeNode m_parent;
    private List m_children;
    private String m_type;
    private String m_label;
    private List m_sipContents;

    public MemoryTreeNode(TreeNode parent,     // null means root
                          List children,       // null means none (yet)
                          String type,
                          String label,
                          List sipContents) {  // null means none (yet)
        m_parent = parent;
        m_children = children;
        if (m_children == null) m_children = new ArrayList();
        m_type = type;
        m_label = label;
        m_sipContents = sipContents;
        if (m_sipContents == null) m_sipContents = new ArrayList();
    }

    public void addChild(TreeNode child) {
        m_children.add(child);
    }

    public void addSIPContent(SIPContent content) {
        m_sipContents.add(content);
    }

    public TreeNode getParent() { return m_parent; }
    public List getChildren() { return m_children; }

    public String getType() { return m_type; }
    public String getLabel() { return m_label; }
    public List getSIPContents() { return m_sipContents; }

}