package bs.logging;

public class ProgramException extends Exception
{
	private static final long serialVersionUID = 197708902185606179L;
	
	public ProgramException(final String paramMessage)
	{
		m_message = paramMessage;
	}
	
	private final String m_message;
		
	@Override
	public final String getMessage()
	{
		return m_message;
	}

	public final void printMessage()
	{
		System.out.println(m_message);
	}
}