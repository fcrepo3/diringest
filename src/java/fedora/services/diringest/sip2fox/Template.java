package fedora.services.diringest.sip2fox;

import java.util.*;

public interface Template {

    /** Get the type of node this template applies to. */
    public String getNodeType();

    /** Get all Attributes specified by this template. */
    public List getAttributes();

}