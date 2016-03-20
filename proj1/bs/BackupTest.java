package bs;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BackupTest {

	private static ProtocolBackup MDB = null;

	private static final boolean DEBUG = true;

	// machine states
	private static final int INIT = 0;
	private static final int PEER_PARSE = 1;
	private static final int REGULAR_COMMAND = 2;
	private static final int BACKUP_COMMAND = 3;
	private static final int BACKUP_DEG = 4;
	private static final int FINAL = 5;

	private static final List<String> readCommands = Arrays.asList(
		"RESTORE",
		"RESTORENENH",
		"DELETE",
		"DELETEENH",
		"RECLAIM",
		"RECLAIMENH"
	);
	
	private static int currentState = INIT;
	private static int commandType = INIT;
	private static int myPort;
	private static int myDegree;

	private static InetAddress myAddress;
	private static File myFile;
	
	public static void main(String[] args) {

		// verify if number of arguments passed is valid
		if (args.length < 3 || args.length > 4) {
			System.out.println("[ main()             ] invalid number of arguments!");
			System.exit(1);
		}

		while (currentState != FINAL) {
		
			switch (currentState) {
			case INIT:
				parseAccessPoint(args[0]);
				break;
			case PEER_PARSE:
				parseCommand(args[1]);
				break;
			case REGULAR_COMMAND:
			case BACKUP_COMMAND:
				parseArguments(args[2]);
				break;
			case BACKUP_DEG:
				parseReplicationDegree(args);
				break;
			default:
				System.out.println("[ main()             ] point of no return reached!");
				System.exit(1);
			}
		}

		logDebug("[ main()             ] command parsed with success!");
	
		MDB = new ProtocolBackup(myAddress, myPort);
		
		if (commandType == BACKUP_COMMAND) {
			generateChunks();
		}
		else {
			generateCommand();
		}
	}

	private static GenericMessage generateCommand() {
		return null;
	}
	
	// parse the first operand
	private static void parseArguments(String arg) {
		// TODO: validate argument
		if (currentState == BACKUP_COMMAND) {
			parseFile(arg);
			logDebug("[ parseOperand()     ] entering BACKUP_DEG state...");
			currentState = BACKUP_DEG;
		}
		else {
			currentState = FINAL;
			logDebug("[ parseOperand()     ] entering FINAL state...");
		}
	}
	
	private static final void generateChunks() {
		
		logDebug("[ parseFile()        ] generating chunks...");

		try {
			final ChunkContainer myChunks = new ChunkContainer(myFile, myDegree);
			final Collection<FileChunk> myChunksArray = myChunks.getCollection();
			////////////////////////////////////////////////////////
			myChunksArray.forEach(thisChunk -> {
				MDB.sendRequest(thisChunk);
			});
			////////////////////////////////////////////////////////
		}
		catch (IOException | NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
		
		logDebug("[ parseFile()        ] generated chunks successfully!");
	}

	private static final void parseFile(final String fileName)
	{
		myFile = new File(fileName);

		if (myFile.exists())
		{
			if (myFile.isDirectory())
			{
				System.out.println("[ parseFile()        ] selected path " + fileName + " is a directory!");
				System.exit(1);
			}
			else
			{
				logDebug("[ parseFile()        ] loading file " + fileName	+ " for reading...");
			}
		}
		else
		{
			System.out.println("[ parseFile()        ] selected file " + fileName + " doesn't exist!");
			System.exit(1);
		}
	}

	private static final void parseCommand(final String argument)
	{		
		if (argument.equals("BACKUP") || argument.equals("BACKUPENH"))
		{
			commandType = BACKUP_COMMAND;
			currentState = BACKUP_COMMAND;
		}
		else if (readCommands.contains(argument))
		{
			commandType = REGULAR_COMMAND;
			currentState = REGULAR_COMMAND;
		}
		else
		{
			System.out.println("[ERROR]: Invalid command!");
			System.exit(1);
		}
		
		logState(argument);
	}
	
	private static int parseInteger(final String arg)
	{
		int degree = 0;

		try
		{
			degree = Integer.parseInt(arg);
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}

		return degree;
	}

	private static void parseReplicationDegree(final String[] args)
	{
		if (args.length != 4)
		{
			System.out.println("[ parseReplicationDegree() ] invalid number of arguments given!");
			System.exit(1);
		}

		int replicationDegree = parseInteger(args[3]);

		if (replicationDegree > 0)
		{
			myDegree = replicationDegree;
			currentState = FINAL;
		}
		else
		{
			System.out.println("[ parseReplicationDegree() ] invalid replication degree!");
			System.exit(1);
		}
	}

	private static void logDebug(final String args)
	{
		if (DEBUG)
		{
			System.out.println(args);
		}
	}
	
	private static void logState(final String paramState)
	{
		if (DEBUG)
		{
			System.out.println("[ parseCommand()     ] entering " + paramState
					+ " command state...");
		}
	}

	private static void parseAccessPoint(final String access) {
		
		int separatorPosition = access.indexOf(':');
		
		if (separatorPosition == -1 || separatorPosition == 0) {
			
			try {
				myAddress = InetAddress.getLocalHost();
			}
			catch (UnknownHostException e) {
				System.out.println("[ parseAccessPoint() ] invalid IP address!");
				System.exit(1);
			}	
		}
		else {
			
			final String ipAddress = access.substring(0, separatorPosition);
		
			try {
				myAddress = InetAddress.getByName(ipAddress);
			}
			catch (UnknownHostException ex) {
				System.out.println("[ parseAccessPoint() ] invalid IP address!");
				System.exit(1);
			}
		}

		try {
			myPort = Integer.parseInt(access.substring(separatorPosition + 1));
		}
		catch (NumberFormatException ex) {
			System.out.println("[ parseAccessPoint() ] invalid port number!");
			System.exit(1);
		}
		
		logDebug("[ parseAccessPoint() ] ADDRESS [" + myAddress.getHostAddress() + "] PORT [" + myPort + "]");
		logDebug("[ parseAccessPoint() ] entering PEER_PARSE state...");
		currentState = PEER_PARSE;
	}
}