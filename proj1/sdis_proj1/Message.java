package sdis_proj1;

public class Message
{
	private String m_header;
	private String[] m_field;
	
	public Message(byte[] msg)
	{
		m_header = new String(msg).trim();
		m_field = m_header.split(" ");
		for (String str : m_field)
		{
			System.out.println(str);
		}
	}
}
