package bs.logging;

import bs.protocol.Message;

public class Logger
{
	private static final String fmtPayload = "[%s()] :: dumping payload contents...\n";
	private static final String fmtHeader = "[%s()] :: dumping message header...\n";
	private static final String msgIssueChunkCommand = "--[DEBUG]-- sending %s for chunk with id=%d\n";
	private static final String msgIssueFileCommand = "--[DEBUG]-- sending %s...\n";

	public static void dumpPayload(final String paramFunction, final Message paramMessage)
	{
		if (DEBUG)
		{
			System.out.print(String.format(fmtPayload, paramFunction));
			paramMessage.dump();
		}
	}

	public static void logChunkCommand(final String paramType, final String fileId, int chunkId)
	{
		if (DEBUG)
		{
			System.out.print(String.format(msgIssueChunkCommand, paramType, chunkId));
		}
	}

	public static void logFileCommand(final String paramType, final String fileId)
	{
		if (DEBUG)
		{
			System.out.print(String.format(msgIssueFileCommand, paramType));
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

	public static void logDebug(final String paramMessage)
	{
		if (DEBUG)
		{
			System.out.println("--[DEBUG]-- " + paramMessage);
		}
	}

	public static void logWarning(final String paramMessage)
	{
		System.out.println("--[WARNING]-- " + paramMessage);
	}

	public static void logInformation(final String paramMessage)
	{
		System.out.println("--[INFORMATION]-- " + paramMessage);
	}

	public static void logError(final String paramMessage)
	{
		System.out.println("--[ERROR]-- " + paramMessage);
	}

	public static void abort(final String paramMessage)
	{
		System.out.println("--[ERROR]-- " + paramMessage);
		System.exit(1);
	}

	private static final boolean DEBUG = true;
}