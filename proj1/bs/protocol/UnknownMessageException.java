package bs.protocol;

import java.util.Date;

import bs.logging.ProgramException;

public class UnknownMessageException extends ProgramException
{
	private static final long serialVersionUID = 3089063073971233427L;

	public UnknownMessageException(final String messageType)
	{
		super(String.format("(%s) received wrong message, was expecting %s!",
			(new Date()).toString(), messageType));
	}
}