package bs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class BackupSystem {
	//----------------------------------------------------
	private static final int MINIMUM_ARGUMENTS = 2;
	private static final int NUMBER_ARGUMENTS = 5;
	private static final int DEFAULT_CONTROL_PORT = 8080;
	private static final int DEFAULT_BACKUP_PORT = 8081;
	private static final int DEFAULT_RESTORE_PORT = 8082;
	//----------------------------------------------------
	private static int myPeerId = -1;
	//----------------------------------------------------
	public static int getPeerId() {
		return myPeerId;
	}
	//----------------------------------------------------	
	public static String getVersion() {
		return "1.0";
	}
	//----------------------------------------------------	
	public static void main(final String[] args) throws IOException {
		
		if (args.length < MINIMUM_ARGUMENTS || args.length > NUMBER_ARGUMENTS) {
			System.out.println("usage: BackupSystem <Host> <PeerId> [<McPort> <MdbPort> <MdrPort>]");
			System.exit(1);
		}

		InetAddress myHost = null;
		System.out.println("[main()]::attempting to parse multicast group address...");
		
		try {
			myHost = InetAddress.getByName(args[0]);
		} 
		catch (UnknownHostException ex) {
			System.out.println("[main()]::invalid multicast group address!");
			System.exit(1);
		}
	
		int multicastControlPort = DEFAULT_CONTROL_PORT;
		int multicastBackupPort = DEFAULT_BACKUP_PORT;
		int multicastRestorePort = DEFAULT_RESTORE_PORT;
		
		System.out.println("[main()]::attempting to parse peer identifier...");

		try {
			myPeerId = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex) {
			System.out.println("[main()]::invalid peer identifier, please enter a positive integer!");
			System.exit(1);
		}

		System.out.println("[main()]::attempting to parse control channel port...");
		
		try {		
			multicastControlPort = Integer.parseInt(args[2]);
		}
		catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
			System.out.println("[main()]::invalid or missing control channel port, assuming default...");
		}
		
		System.out.println("[main()]::attempting to parse backup channel port...");
		
		try {		
			multicastBackupPort = Integer.parseInt(args[3]);
		}
		catch (IndexOutOfBoundsException | NumberFormatException ex) {
			System.out.println("[main()]::invalid or missing backup channel port, assuming default...");
		}
		
		System.out.println("[main()]::atempting to parse restore channel port...");
		
		try {
			multicastRestorePort = Integer.parseInt(args[4]);
		}
		catch (IndexOutOfBoundsException | NumberFormatException ex) {
			System.out.println("[main()]::invalid or missing restore channel port, assuming default...");
		}
		//
		final InetAddress serverAddress = myHost;
		final ProtocolCommand MC = new ProtocolCommand(serverAddress, multicastControlPort);
		final ProtocolBackup MDB = new ProtocolBackup(serverAddress, multicastBackupPort);
		final ProtocolRestore MDR = new ProtocolRestore(serverAddress, multicastRestorePort);
		//
		new WorkerBackup(MC, MDB).start();
		new WorkerRestore(MC, MDR).start();
		new WorkerCommand(MC).start();
	}
}