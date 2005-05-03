package fedora.services.diringest.ingest;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.log4j.*;

import fedora.client.*;
import fedora.common.*;
import fedora.services.diringest.*;
import fedora.services.diringest.sip2fox.*;
import fedora.server.management.*;

public class StagingIngester implements Ingester {

    private static final Logger logger =
            Logger.getLogger(StagingIngester.class.getName());

    private FedoraClient m_fedora;
    private DatastreamStage m_stage;

    public StagingIngester(FedoraClient fedora,
                           DatastreamStage stage) {
        m_fedora = fedora;
        m_stage = stage;
    }

    public void ingest(FOXMLResult result) throws Exception {
        List stagedURLs = new ArrayList();
        try {
            // Translate the FOXMLResult to a String (in memory)
            // with URLs to staged datastreams instead of inline base64.
            StagingFOXMLParser parser = 
                    new StagingFOXMLParser(result.getStream(), 
                                           m_stage,
                                           stagedURLs);
            logger.info("Parsed and staged " + result.getPID().toString());
            // Send the ingest request to Fedora
            m_fedora.getAPIM().ingest(parser.getFOXMLString().getBytes("UTF-8"),
                                      "foxml1.0",
                                      "Ingested using diringest service");
            logger.info("Ingested " + result.getPID().toString());
        } finally {
            // Finally, clean the content out of the staging area
            for (int i = 0; i < stagedURLs.size(); i++) {
                URL url = (URL) stagedURLs.get(i);
                try {
                    m_stage.unStageContent(url);
                } catch (IOException ioe) {
                    logger.warn("Unable to un-stage: " + url.toString());
                }
            }
        }
    }

}