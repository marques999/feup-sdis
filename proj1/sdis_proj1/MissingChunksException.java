package sdis_proj1;

public class MissingChunksException extends Exception
{
	private static final long serialVersionUID = 1L;

	public MissingChunksException(String fileId)
	{
		m_message = "file " + fileId + " not received entirely, some chunks are missing!";
	}

	private String m_message;

	@Override
	public String getMessage()
	{
		return m_message;
	}
}