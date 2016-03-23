package bs.logging;

import bs.protocol.Message;

public class Logger
{
	private static final String fmtGeneric = "[%s()] :: %s\n";
	private static final String fmtCommand = "[%s()] :: received %s command!\n";
	private static final String fmtPayload = "[%s()] :: dumping payload contents...\n";
	private static final String fmtHeader =  "[%s()] :: dumping message header...\n";
	private static final String fmtState = "[parseCommand()] :: parsing %s command...\n";
	//
	public static final boolean DEBUG = true;
	
	public static void dumpPayload(final String paramFunction, final Message paramMessage)
	{
		if (DEBUG)
		{
			System.out.print(String.format(fmtPayload, paramFunction));
			paramMessage.dump();
		}
	}
	
	public static void logState(final String paramState)
	{
		if (DEBUG)
		{
			System.out.print(String.format(fmtState, paramState));
		}
	}
	
	public static void dumpHeader(final String paramFunction, final Message paramMessage)
	{
		if (DEBUG)
		{
			System.out.print(String.format(fmtHeader, paramFunction));
			paramMessage.dump();
		}
	}
	
	public static void logCommand(final String paramFunction, final String paramCommand)
	{
		if (DEBUG)
		{
			System.out.print(String.format(fmtCommand, paramFunction, paramCommand));
		}
	}

	public static void logGeneric(final String paramFunction, final String paramMessage)
	{
		if (DEBUG)
		{
			System.out.print(String.format(fmtGeneric, paramFunction, paramMessage));
		}
	}
}