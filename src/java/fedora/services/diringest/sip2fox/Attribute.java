package fedora.services.diringest.sip2fox;

public class Attribute {

    private String m_name;
    private String m_value;

    public Attribute(String name, String value) {
        m_name = name;
        m_value = value;
    }

    public String getName() { return m_name; }
    public String getValue() { return m_value; }

}