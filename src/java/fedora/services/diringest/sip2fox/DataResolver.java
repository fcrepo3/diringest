package fedora.services.diringest.sip2fox;

import java.io.*;

public interface DataResolver {

    /** 
     * Get the stream indicated by the locator.
     *
     * @throws IOException if the data is not found or retrievable, or the locator is bad.
     */
    public InputStream getData(String locatorType, String locator) throws IOException;

    public void checkResolvability(String locatorType, String locator) throws Exception;

}