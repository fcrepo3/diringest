package fedora.services.diringest;

import java.net.*;
import java.util.*;

public class RelSpec {

    private URI m_uri;
    private List m_targetSpecs;

    public RelSpec(URI uri, List targetSpecs) {
        m_uri = uri;
        m_targetSpecs = targetSpecs;
    }

    public URI getURI() {
        return m_uri;
    }

    public List getTargetSpecs() {
        return m_targetSpecs;
    }

}