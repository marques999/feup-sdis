package bs;

import java.util.Date;

public class BadChunkException extends Exception
{
	private static final long serialVersionUID = 790445000889429821L;

	public BadChunkException(final FileChunk paramChunk)
	{
		m_message = String.format("(%s) chunk %s from file %s has invalid data!\n", 
			(new Date()).toString(), paramChunk.getChunkId(), paramChunk.getFileId());
	}

	private final String m_message;

	@Override
	public String getMessage()
	{
		return m_message;
	}
}