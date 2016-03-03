package sdis_proj1;

public class DBS
{
	private static DBS m_instance;

	public static DBS getInstance()
	{
		if (m_instance == null)
		{
			m_instance = new DBS();
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