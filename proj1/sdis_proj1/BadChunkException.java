package sdis_proj1;

public class BadChunkException extends Exception
{
	private static final long serialVersionUID = 790445000889429821L;

	public BadChunkException(String fileId, int chunkId)
	{
		m_message = "chunk " + chunkId + " from file "+ fileId + " has invalid data!";
	}
	
	private String m_message;
	
	@Override
	public String getMessage()
	{
		return m_message;
	}
}