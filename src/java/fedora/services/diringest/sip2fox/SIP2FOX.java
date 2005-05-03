package fedora.services.diringest.sip2fox;

import java.io.*;
import org.apache.log4j.*;
import org.apache.log4j.xml.*;

import fedora.services.diringest.sip2fox.pidgen.*;

/**
 * Command-line application for converting SIPs to FOXML files.
 */
public abstract class SIP2FOX {

    private static final Logger logger = 
            Logger.getLogger(SIP2FOX.class.getName());

    public static void initLogging() {
        String homeDir = System.getProperty("sip2fox.home");
        if (homeDir == null) {
            System.out.println("ERROR: sip2fox.home property not set.");
            System.exit(1);
        }
        // tell commons-logging to use log4j
        System.setProperty("org.apache.commons.logging.LogFactory", "org.apache.commons.logging.impl.Log4jFactory");
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Log4JLogger");
        // log4j
        File log4jConfig = new File(new File(homeDir), "config/log4j.xml");
        DOMConfigurator.configure(log4jConfig.getPath());
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        System.out.println();
        if (args.length != 2 && args.length != 6 && args.length != 7) {
            System.err.println("ERROR: Wrong number of arguments.");
            System.err.println();
            System.err.println("Usage: sip2fox sipFile.zip outputDir [host port user pass [namespace]]");
            System.exit(1);
        }
        String host = null;
        int port = 0;
        String user = null;
        String pass = null;
        String namespace = null;
        if (args.length > 2) {
            host = args[2];
            try {
                port = Integer.parseInt(args[3]);
                if (port < 0) throw new IOException("");
            } catch (Exception e) {
                System.err.println("ERROR: Bad port number: " + args[3]);
                System.exit(1);
            }
            user = args[4];
            pass = args[5];
            if (args.length == 7) {
                namespace = args[6];
            }
        }
        initLogging();
        try {
            long startTime = System.currentTimeMillis();
            logger.info("Initializing...");
            PIDGenerator pidgen;
            if (user == null) {
                pidgen = new TestPIDGenerator();
            } else {
                pidgen = new RemotePIDGenerator(namespace, "http://" + host + ":" + port + "/fedora/");
            }
            Converter c = new Converter(pidgen);
            String homeDir = System.getProperty("sip2fox.home");
            if (homeDir == null) {
                System.out.println("ERROR: sip2fox.home property not set.");
                System.exit(1);
            }
            File cRules = new File(new File(homeDir), "config/crules.xml");
            if (!cRules.exists()) {
                System.out.println("ERROR: Conversion rules not found at " + cRules.getPath());
                System.exit(1);
            }
            ConversionRules rules = new ConversionRules(new FileInputStream(cRules));
            logger.info("Processing...");
            FOXMLResult[] r = c.convert(rules, new File(args[0]), user, pass);
            logger.info("Saving results...");
            File dir = new File(args[1]);
            for (int i = 0; i < r.length; i++) {
                File foxmlFile = new File(dir, r[i].getPID().toFilename() + ".xml");
                if (i == 0) dir.mkdirs();
                FileOutputStream outStream = new FileOutputStream(foxmlFile);
                r[i].dump(outStream);
                outStream.close();
                r[i].close();
                logger.info("Wrote " + foxmlFile.getPath());
            }
            long ms = System.currentTimeMillis() - startTime;
            System.out.println("SUCCESS: Converted SIP to " + r.length 
                    + " FOXML files in " + ms + "ms.");
        } catch (Throwable th) {
            if (logger.isDebugEnabled() || th.getMessage() == null) {
                th.printStackTrace();
            } else {
                System.err.println("ERROR: " + th.getMessage());
            }
            System.exit(1);
        }
    }
}
