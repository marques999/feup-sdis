package bs;

import java.util.Date;

public class UnknownMessageException extends Exception
{
	private static final long serialVersionUID = 3089063073971233427L;

	public UnknownMessageException(final String messageType)
	{
		m_message = String.format("(%s) received wrong message, was expecting %s!",
			(new Date()).toString(), messageType);
	}

	private final String m_message;

	@Override
	public String getMessage()
	{
		return m_message;
	}
}