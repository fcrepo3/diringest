package fedora.services.diringest;

import java.util.*;

/**
 * A node in the tree of objects in the SIP.
 */
public interface TreeNode {

    public TreeNode getParent();
    public List getChildren();

    public String getType();
    public String getLabel();
    public List getDatastreams();

}