package bs;

import java.util.Date;

public class MissingChunksException extends Exception
{
	private static final long serialVersionUID = 7534263449573927317L;

	public MissingChunksException(final String fileId)
	{
		m_message = String.format("(%s) file %s not received entirely, some chunks are missing!\n",
			(new Date()).toString(), fileId);
	}

	private final String m_message;

	@Override
	public String getMessage()
	{
		return m_message;
	}
}