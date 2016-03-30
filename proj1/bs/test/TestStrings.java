package bs.test;

public class TestStrings
{
	protected static final String messageInvalidRemoteObject = "invalid remote object, value must be between zero and 32767!";
	protected static final String messageInvalidArguments = "TestApp <[host:]rmi_object> <command> <opnd_1> [<opnd_2>]";
	protected static final String messageInvalidCommand = "invalid command: [BACKUP(ENH)|RESTORE(ENH)|DELETE(ENH)|RECLAIM(ENH)]";
	protected static final String messageInvalidReplicationDegree = "invalid replication degree, value must be greater than zero!";
	protected static final String messageInvalidReclaimAmount = "invalid reclaim amount, value must be greater than zero!";
	protected static final String messageInvalidBackupArguments = "invalid arguments: BACKUP[ENH] <filename> <replication_degree>";
	protected static final String messageInvalidRestoreArguments =	"invalid arguments: RESTORE[ENH] <filename>";
	protected static final String messageInvalidDeleteArguments = "invalid arguments: DELETE[ENH] <filename>";
	protected static final String messageInvalidReclaimArguments = "invalid arguments: RECLAIM[ENH] <amount(bytes)>";
	protected static final String messageRemoteException = "could not connect to target machine, is rmiregistry running?";
	protected static final String messageNotBoundException = "remote object not registered on the target machine!";
	protected static final String messageSentCommand = "message sent, waiting for peer response...";
	protected static final String messageLocalhost = "could not connect to localhost!";
	protected static final String messageInvalidAddress = "could not locate address!";
	protected static final String messageConnected = "connected to initiator peer, sending user request...";
	protected final static String messageCommandError = "could not forward the request to the initiator peer!";
}