package fedora.services.diringest;

/**
 * Provides access to the contents of a SIP.
 */
public interface SIPReader {

    public TreeNode getRoot();

    public void close();

}