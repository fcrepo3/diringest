package fedora.services.diringest;

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

}