package fedora.services.diringest;

import java.util.*;

public class RelSpec {

    private String m_name;
    private String m_prefix;
    private List m_targetSpecs;

    public RelSpec(String name, String prefix, List targetSpecs) {
        m_name = name;
        m_prefix = prefix;
        m_targetSpecs = targetSpecs;
    }

    public String getName() {
        return m_name;
    }

    public String getPrefix() {
        return m_prefix;
    }

    public List getTargetSpecs() {
        return m_targetSpecs;
    }

}