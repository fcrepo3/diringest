package fedora.services.diringest.sip2fox;

import java.util.*;

public class TargetSpec {

    public static final int PARENT     = 0;
    public static final int CHILD      = 1;
    public static final int ANCESTOR   = 2;
    public static final int DESCENDANT = 3;

    private int m_kinType;
    private String m_nodeType;

    public TargetSpec(int kinType, String nodeType) {
        m_kinType = kinType;
        m_nodeType = nodeType;
    }

    public int getKinType() {
        return m_kinType;
    }

    /**
     * Which nodeTypes are applicable.
     */
    public String getNodeType() {
        return m_nodeType;
    }

    /**
     * Get a list of nodes that match the given node according to this 
     * TargetSpec.
     */
    public List getMatches(TreeNode node) {
        List matches = new ArrayList();
        if (m_kinType == PARENT) {
            TreeNode parent = node.getParent();
            if (parent != null) {
                addIfTypesMatch(node, parent, matches);
            }
        } else if (m_kinType == CHILD) {
            Iterator iter = node.getChildren().iterator();
            while (iter.hasNext()) {
                TreeNode child = (TreeNode) iter.next();
                addIfTypesMatch(node, child, matches);
            }
        } else if (m_kinType == ANCESTOR) {
            addAncestorMatches(node, node, matches);
        } else if (m_kinType == DESCENDANT) {
            addDescendantMatches(node, node, matches);
        }
        return matches;
    }

    private void addAncestorMatches(TreeNode contextNode, TreeNode node, List list) {
        TreeNode parent = node.getParent();
        if (parent != null) {
            addIfTypesMatch(contextNode, parent, list);
            addAncestorMatches(contextNode, parent, list);
        }
    }

    private void addDescendantMatches(TreeNode contextNode, TreeNode node, List list) {
        Iterator iter = node.getChildren().iterator();
        while (iter.hasNext()) {
            TreeNode child = (TreeNode) iter.next();
            addIfTypesMatch(contextNode, child, list);
            addDescendantMatches(contextNode, child, list);
        }
    }

    private void addIfTypesMatch(TreeNode contextNode,
                                 TreeNode targetNode,
                                 List list) {
        if (m_nodeType.equals("*")) {
            list.add(targetNode);
        } else {
            String targetType = targetNode.getType();
            if (targetType != null) {
                if (targetType.equals(m_nodeType)) list.add(targetNode);
            }
        }
    }

}