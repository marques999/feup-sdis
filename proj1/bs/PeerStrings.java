package bs;

public class PeerStrings
{
	protected static final String messageConnecting = "connecting to rmiregistry server...";
	protected static final String messageConnected = "connected to initiator service, listening for commands!";
	protected static final String messageObjectExists = "remote object already exists, rebinding...";	
	protected static final String messageMulticastConnected = "%s forwarding messages to %s:%d";
	protected static final String messageMulticastListening = "%s listening at %s:%d";
	protected static final String messageUnicastListening = "unicast service: listening at port %d";
	protected static final String messageUnicastError = "unicast service: offline (connection problem)!";
	protected static final String messageMulticastError = "%s: offline (connection problem)!";
	protected static final String messageRemoteException = "could not bind object, is rmiregistry running?";
	protected static final String messageBackupDone = "backup action sucessfully completed!";
	protected static final String messageRestoreDone = "restore action sucessfully completed!";
	protected static final String messageDeleteDone = "file successfully deleted from the network!";
	protected static final String messageReclaimDone = "redundant chunks successfully reclaimed!";
	protected static final String messageParsingPeer = "attempting to parse peer identifier...";
	protected static final String messageParsingRemote = "attempting to parse remote object name...";
	protected static final String messageInvalidAddress = "invalid multicast address, reverting to localhost...";
	protected static final String messageInvalidPort = "invalid multicast port, assuming defaults...";
	protected static final String messageTerminatingUnicast = "terminating unicast restore protocol...";
	protected static final String messageStartingUnicast = "starting unicast restore protocol...";
	protected static final String messageInvalidRemote = "invalid remote object name,  value must be between zero and 32767!";
	protected static final String messageInvalidPeer = "invalid peer identifier, value must be between zero and 32767!";
}