package sdis_proj1;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

public class BackupSystem
{
	//private static HashMap<String, ChunkContainer> myFiles;
	
	private static final int MINIMUM_ARGUMENTS = 3;
	private static final int NUMBER_ARGUMENTS = 7;
	private static final int DEFAULT_PORT = 0;
	
	public static void main(final String[] args) {
		
		if (args.length < MINIMUM_ARGUMENTS || args.length > NUMBER_ARGUMENTS) {
			System.out.println("usage: BackupSystem <MC:ip> <MC:port> <MDB:ip> <MDB:port> <MDR:ip> <MDR:port>");
			System.exit(1);
		}

		int peerId = -1;
		int serverPort = DEFAULT_PORT;
		
		System.out.println("[main()]::attempting to parse multicast command channel arguments...");

		try {
			peerId = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException ex) {
			System.out.println("[main()]::invalid server identifier, should be an integer!");
			System.exit(1);
		}

		try {		
			serverPort = Integer.parseInt(args[2]);
			Protocol.getInstance().connect(peerId, InetAddress.getByName(args[1]), serverPort);
		}
		catch (UnknownHostException | ArrayIndexOutOfBoundsException | NumberFormatException ex) {
			System.out.println("[main()]::invalid or missing multicast command channel arguments!");
			System.exit(1);
		}
		
		System.out.println("[main()]::attempting to parse multicast backup channel arguments...");
		
		try {		
			serverPort = Integer.parseInt(args[4]);
			Protocol.getInstance().connectBackup(InetAddress.getByName(args[3]), serverPort);
		}
		catch (UnknownHostException | IndexOutOfBoundsException | NumberFormatException ex) {
			System.out.println("[main()]::invalid or missing multicast backup channel arguments, skipping...");
		}
		
		System.out.println("[main()]::atempting to parse multicast restore channel arguments...");
		
		try {		
			serverPort = Integer.parseInt(args[6]);
			Protocol.getInstance().connectRestore(InetAddress.getByName(args[5]), serverPort);
		}
		catch (UnknownHostException | IndexOutOfBoundsException | NumberFormatException ex) {
			System.out.println("[main()]::invalid or missing restore backup channel arguments, skipping...");
		}
		
		/*try
		{
			test();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}*/
	}
	
	public static void test() throws IOException {
		
		try {
			
			final File myFile = new File("example.bin");
			final ChunkContainer myChunks = new ChunkContainer(myFile, true);
			final Collection<FileChunk> myChunksArray = myChunks.getCollection();
			////////////////////////////////////////////////////////
			myChunks.removeChunk(0);
			System.out.println(myChunks.getChunk(0));
			myChunks.removeChunk(25);
			////////////////////////////////////////////////////////
			myChunksArray.forEach(chunk -> {
				
				final byte[] command = new CHUNKMessage(chunk).getMessage();
				
				try {
					processMessage(command, "CHUNK");
				}
				catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
			});
			////////////////////////////////////////////////////////
		}
		catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
		}
	}

	private static String[] processHeader(final String paramHeader) {
		return paramHeader.trim().split(" ");
	}

	private static boolean checkType(String paramMessage, String paramPrefix) {
		return paramMessage.startsWith(paramPrefix);
	}
	
	public static Message processSimpleMessage(final String convertedMessage) throws VersionMismatchException {
		
		String[] messageHeader = processHeader(convertedMessage);
		Message unmarshalledMessage = null;
		
		if (messageHeader[0].equals("GETCHUNK")) {
			
			if (Protocol.DEBUG) {
				System.out.println("[processSimpleMessage()]::received GETCHUNK command!");
			}
			
			unmarshalledMessage = new GETCHUNKMessage(messageHeader);
		}	
		else if (messageHeader[0].equals("DELETE")) {
		
			if (Protocol.DEBUG) {
				System.out.println("[processSimpleMessage()]::received DELETE command!");
			}
			
			unmarshalledMessage = new DELETEMessage(messageHeader);
		}		
		else if (messageHeader[0].equals("STORED")) {

			if (Protocol.DEBUG) {
				System.out.println("[processSimpleMessage()]::received STORED response!");
			}
			
			unmarshalledMessage = new STOREDMessage(messageHeader);
		}		
		else if (messageHeader[0].equals("REMOVED")) {

			if (Protocol.DEBUG) {
				System.out.println("[processSimpleMessage()]::received REMOVED response!");
			}
			
			unmarshalledMessage = new REMOVEDMessage(messageHeader);
		}
		else {
			
			if (Protocol.DEBUG) {
				System.out.println("[processSimpleMessage()]::received UNKNOWN message!");
			}
			
			return null;
		}
		
		if (Protocol.DEBUG) {
			System.out.println("[processSimpleMessage()]::dumping message header...");
			unmarshalledMessage.dump();
		}
		
		return unmarshalledMessage;
	}

	public static PayloadMessage processPayloadMessage(final String convertedMessage, final byte[] msg) throws VersionMismatchException
	{
		if (Protocol.DEBUG)
		{
			System.out.println("[processMessage()]::message contains payload data!");
		}
		
		int payloadSeparatorStart = convertedMessage.indexOf("\r\n\r\n");
		int payloadSeparatorEnd = payloadSeparatorStart + "\r\n\r\n".length();
			
		String[] messageHeader = processHeader(convertedMessage.substring(0, payloadSeparatorStart));
		PayloadMessage unmarshalledMessage = null;
		
		byte[] messageBody = Arrays.copyOfRange(msg, payloadSeparatorEnd, msg.length);
		
		if (messageHeader[0].equals("PUTCHUNK")) {
			
			if (Protocol.DEBUG) {
				System.out.println("[processPayloadMessage()]::received PUTCHUNK command!");
			}
			
			unmarshalledMessage = new PUTCHUNKMessage(messageHeader, messageBody);
		}
		else if (messageHeader[0].equals("CHUNK")) {
			
			if (Protocol.DEBUG) {
				System.out.println("[processPayloadMessage()]::received CHUNK command!");
			}
			
			unmarshalledMessage = new CHUNKMessage(messageHeader, messageBody);
		}	
		else {
			
			if (Protocol.DEBUG) {
				System.out.println("[processPayloadMessage()]::received UNKNOWN message!");
			}
			
			return null;
		}
		
		if (Protocol.DEBUG) {
			System.out.println("[processPayloadMessage()]::dumping message header...");
			unmarshalledMessage.dump();
		}

		return unmarshalledMessage;
	}
	
	public static Message processMessage(final byte[] msg, final String messageType) throws VersionMismatchException, BadChunkException, UnknownMessageException {

		final String convertedMessage = new String(msg);

		if (messageType != null && !checkType(convertedMessage, messageType)) {
			
			if (Protocol.DEBUG) {
				System.out.println("[processMessage()]::received UNKNOWN message!");
			}
			
			return null;
		}
	
		if (messageType.equals("PUTCHUNK") || messageType.equals("CHUNK")) {
			return processPayloadMessage(convertedMessage, msg);
		}

		return processSimpleMessage(convertedMessage);
	}
	
	public final Message receiveResponse(int paramPort, final String messageType) {
		
		final byte[] rbuf = new byte[1024];
		
		Message result = null;
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		try (final DatagramSocket socket = new DatagramSocket(paramPort)) {	
			socket.receive(packet);
			result = processMessage(packet.getData(), messageType);
		}
		catch (IOException | VersionMismatchException | BadChunkException | UnknownMessageException ex) {
			return null;
		}
		
		return result;
	}
}