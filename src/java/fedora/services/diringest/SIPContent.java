package fedora.services.diringest;

import java.io.*;

public interface SIPContent {

    public String getID();

    public String getLabel();
    public void setLabel(String label);

    public String getType();
    public void setType(String type);

    public boolean wasInline();

    public String getMIMEType();

    public InputStream getInputStream() throws IOException;

    // do a basic check to see if the input stream can be retrieved
    public void check() throws Exception;

}