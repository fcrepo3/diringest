package fedora.services.diringest.sip2fox;

/**
 * Provides access to the contents of a SIP.
 */
public interface SIPReader {

    public TreeNode getRoot();

    public void close();

}