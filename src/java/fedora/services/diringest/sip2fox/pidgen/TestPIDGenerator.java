package fedora.services.diringest.sip2fox.pidgen;

import java.io.*;

import fedora.common.*;

/**
 * Simple, local implementation of PIDGenerator for testing purposes.
 *
 * The first PID will be test:1, the next will be test:2, and so on.
 */
public class TestPIDGenerator implements PIDGenerator {

    private int m_number;

    public TestPIDGenerator() {
        m_number = 0;
    }

    public PID getNextPID(String user, String pass) {
        m_number++;
        return PID.getInstance("test:" + m_number);
    }

    public PID[] getNextPIDs(int howMany, String user, String pass) throws IOException {
        PID[] pids = new PID[howMany];
        for (int i = 0; i < howMany; i++) {
            pids[i] = getNextPID(user, pass);
        }
        return pids;
    }

}