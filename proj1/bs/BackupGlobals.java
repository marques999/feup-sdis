package bs;

public class BackupGlobals
{
	public static final int maximumBackoffTime = 400;
	public static final int maximumAttempts = 5;
	public static final int initialWaitingTime = 1000;
	public static final int defaultControlPort = 8080;
	public static final int defaultBackupPort = 8081;
	public static final int defaultRestorePort = 8082;
	public static final long maximumChunkSize = 64000;
	public static final int maximumPacketLength = 65000;
	
	protected static final int minimumPeerArguments = 2;
	protected static final int minimumInitArguments = 3;
	protected static final int maximumPeerArguments = 5;
	protected static final int maximumInitArguments = 6;
	
	protected static boolean checkPeerArguments(int argc)
	{
		return argc >= minimumPeerArguments && argc <= maximumPeerArguments;
	}
	
	protected static boolean checkInitiatorArguments(int argc)
	{
		return argc >= minimumInitArguments && argc <= maximumInitArguments;
	}
}