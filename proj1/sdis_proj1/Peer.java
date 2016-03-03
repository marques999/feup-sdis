package sdis_proj1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Peer
{
	private static final boolean DEBUG = true;
	
	public static void main(final String[] args) throws IOException
	{
		try
		{
			final ChunkBackup f_aligned = new ChunkBackup("256bytes.hex");
			final int numberChunks = f_aligned.split();
			final FileChunk[] chunks = f_aligned.getChunks();
			////////////////////////////////////////////////////////
			for (int i = 0; i < numberChunks; i++)
			{
				byte[] command = new CHUNKSMessage(chunks[i]).getMessage();
				processMessage(command, "CHUNK");
			}
		}
		catch (NoSuchAlgorithmException | VersionMismatchException ex)
		{
			ex.printStackTrace();
		}
		catch (BadChunkException ex)
		{
			System.out.println(ex.getMessage());
		}
		catch (UnknownMessageException ex)
		{
			ex.printStackTrace();
		}
	}

	private static String[] processHeader(final String paramHeader)
	{
		return paramHeader.trim().split(" ");
	}

	private static boolean checkType(String paramMessage, String paramPrefix)
	{
		return paramMessage.startsWith(paramPrefix);
	}
	
	public static Message processMessage(final byte[] msg, final String messageType) throws VersionMismatchException, BadChunkException, UnknownMessageException
	{
		Message unmarshalledMessage = null;
		String[] messageHeader = null;
		String convertedMessage = new String(msg);

		if (DEBUG)
		{
			System.out.println("[DEBUG] processMessage()::start processing...");
		}
		
		if (messageType != null && !checkType(convertedMessage, messageType))
		{
			if (DEBUG)
			{
				System.out.println("[ERROR] processMessage()::unknown message type!");
			}
			
			return null;
		}
	
		byte[] messageBody = null;
		boolean payloadMessage = messageType.equals("PUTCHUNK") || messageType.equals("CHUNK");
		
		if (payloadMessage)
		{
			int payloadSeparatorStart = convertedMessage.indexOf("\r\n\r\n");
			int payloadSeparatorEnd = payloadSeparatorStart + "\r\n\r\n".length();

			if (DEBUG)
			{
				System.out.println("[DEBUG] processMessage()::message contains payload data!");
			}
			
			messageHeader = processHeader(convertedMessage.substring(0, payloadSeparatorStart));
			messageBody = Arrays.copyOfRange(msg, payloadSeparatorEnd, msg.length);
		}
		else
		{
			messageHeader = processHeader(convertedMessage);
		}

		if (messageHeader[0].equals("PUTCHUNK"))
		{
			if (DEBUG)
			{
				System.out.println("[DEBUG] processMessage()::generating STORED response");
			}
			
			unmarshalledMessage = new PUTCHUNKMessage(messageHeader, messageBody);
		}	
		else if (messageHeader[0].equals("GETCHUNK"))
		{
			if (DEBUG)
			{
				System.out.println("[DEBUG] processMessage()::generating CHUNK response");
			}
			
			unmarshalledMessage = new GETCHUNKMessage(messageHeader);
		}	
		else if (messageHeader[0].equals("DELETE"))
		{
			if (DEBUG)
			{
				System.out.println("[DEBUG] processMessage()::generating REMOVED response");
			}
			
			unmarshalledMessage = new DELETEMessage(messageHeader);
		}		
		else if (messageHeader[0].equals("CHUNK"))
		{
			unmarshalledMessage = new CHUNKSMessage(messageHeader, messageBody);
		}	
		else if (messageHeader[0].equals("STORED"))
		{
			unmarshalledMessage = new STOREDMessage(messageHeader);
		}		
		else if (messageHeader[0].equals("REMOVED"))
		{
			unmarshalledMessage = new REMOVEDMessage(messageHeader);
		}
		else
		{
			throw new UnknownMessageException(messageHeader[0]);
		}

		if (DEBUG && unmarshalledMessage != null)
		{
			System.out.println("[DEBUG] processMessage()::dumping message header...");
			unmarshalledMessage.dump();
		}
		
		return unmarshalledMessage;
	}
	
	private static Message receiveResponse(int paramPort, final String messageType)
	{
		final byte[] rbuf = new byte[1024];
		
		Message result = null;
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		try (final DatagramSocket socket = new DatagramSocket(paramPort))
		{	
			socket.receive(packet);
			result = processMessage(packet.getData(), messageType);
		}
		catch (IOException | VersionMismatchException | BadChunkException | UnknownMessageException ex)
		{
			return null;
		}
		
		return result;
	}

	private static MessageWrapper receiveRequest(InetAddress paramHost, int paramPort)
	{
		final byte[] rbuf = new byte[1024];
		
		MessageWrapper response = null;
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		try (final MulticastSocket socket = new MulticastSocket(paramPort))
		{
			socket.setTimeToLive(1);
			socket.joinGroup(paramHost);
			socket.receive(packet);
			response = new MessageWrapper(packet.getAddress(), Integer.parseInt(new String(packet.getData(), packet.getOffset(), packet.getLength())));
			socket.leaveGroup(paramHost);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		return response;
	}
}