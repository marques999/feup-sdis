package sdis_proj1;

public class Server
{
	private static Server m_instance;

	public static Server getInstance()
	{
		if (m_instance == null)
		{
			m_instance = new Server();
		}
		
		return m_instance;
	}
	
	public final String getVersion()
	{
		return "1.0";
	}
	
	public final int getServerId()
	{
		return 1;
	}
}