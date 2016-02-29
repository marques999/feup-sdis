package sdis_proj1;

public class VersionMismatchException extends Exception
{
	private static final long serialVersionUID = -2585014697472478945L;

	public VersionMismatchException(String actualVersion, String expectedVersion)
	{
		m_message = "message was sent by a different version ("
				+ expectedVersion
				+ ") of this service, you are running version "
				+ actualVersion;
	}

	private String m_message;

	@Override
	public String getMessage()
	{
		return m_message;
	}
}