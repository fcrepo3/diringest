package fedora.services.diringest.ingest;

import java.net.*;
import java.io.*;

public interface DatastreamStage {

    public URL stageContent(File file) throws IOException;

    public void unStageContent(URL location) throws IOException;

}
