package fedora.services.diringest.sip2fox.pidgen;

import java.io.*;

import fedora.common.*;

public interface PIDGenerator {

    /**
     * Get the next unique PID.
     *
     * @throws IOException if pid generation failed, for any reason.
     */
    public PID getNextPID(String user, String pass) throws IOException;

    public PID[] getNextPIDs(int howMany, String user, String pass) throws IOException;

}