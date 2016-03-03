package sdis_proj1;

public class BadChunkException extends Exception
{
	private static final long serialVersionUID = 790445000889429821L;

	public BadChunkException(final FileChunk paramChunk)
	{
		m_message = "chunk " + paramChunk.getChunkId() + " from file " + paramChunk.getFileId() + " has invalid data!";
	}

	private final String m_message;

	@Override
	public String getMessage()
	{
		return m_message;
	}
}