package bs;

public class PeerGlobals
{
	public static final int maximumBackoffTime = 400;
	public static final int maximumAttempts = 5;
	public static final int initialWaitingTime = 1000;
	public static final int defaultControlPort = 9050;
	public static final int defaultBackupPort = 9051;
	public static final int defaultRestorePort = 9052;
	public static final int maximumPacketLength = 65000;
	public static final long maximumChunkSize = 64000;

	private static final int minimumPeerArguments = 2;
	private static final int maximumPeerArguments = 7;

	protected static boolean checkArguments(int argc)
	{
		return argc >= minimumPeerArguments && argc <= maximumPeerArguments;
	}
}