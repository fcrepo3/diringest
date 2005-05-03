package fedora.services.diringest.ingest;

import fedora.common.*;
import fedora.services.diringest.sip2fox.*;

public interface Ingester {

    public void ingest(FOXMLResult result) throws Exception;

}