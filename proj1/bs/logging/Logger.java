package bs.logging;

public class Logger
{
	public static void logChunkCommand(final String paramType, final String fileId, int chunkId)
	{
		if (DEBUG)
		{
			System.out.print(String.format("--[DEBUG]-- sending %s for chunk with id=%d\n", paramType, chunkId));
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