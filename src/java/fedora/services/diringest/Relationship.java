package fedora.services.diringest;

import java.util.*;

public class Relationship {

    private String m_name;
    private String m_prefix;
    private TreeNode m_target;

    public Relationship(String name, String prefix, TreeNode target) {
        m_name = name;
        m_prefix = prefix;
        m_target = target;
    }

    public String getName() {
        return m_name;
    }

    public String getPrefix() {
        return m_prefix;
    }

    public TreeNode getTarget() {
        return m_target;
    }

    public String getExternalTarget() {
        // TODO: add alternate constructor for Relationships
        //       that have targets that aren't treeNodes.
        //       By returning null here, we indicate that the
        //       target is a TreeNode.
        return null;
    }

}