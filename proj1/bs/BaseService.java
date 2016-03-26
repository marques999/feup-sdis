package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import bs.filesystem.BackupStorage;
import bs.filesystem.FileManager;
import bs.protocol.GenericMessage;
import bs.protocol.Message;

public abstract class BaseService extends Thread
{
	private final Random m_random;
	private final Connection m_connection;
	private final MulticastSocket m_socket;
	protected final FileManager fmInstance;
	protected final BackupStorage bsdbInstance;
	
	private static HashMap<String, Boolean> m_payloadMessage = new HashMap<>();
	
	static
	{
		m_payloadMessage.put("PUTCHUNK", true);
		m_payloadMessage.put("CHUNK", true);
		m_payloadMessage.put("GETCHUNK", false);
		m_payloadMessage.put("DELETE", false);
		m_payloadMessage.put("STORED", false);
		m_payloadMessage.put("REMOVED", false);
	}
	
	/**
	 * @brief default constructor for 'BackupService' class
	 * @param paramAddress address of the multicast channel
	 * @param paramPort port of the multicast channel
	 */
	public BaseService(final String paramName, final InetAddress paramAddress, int paramPort)
	{
		m_connection = new Connection(paramName, paramAddress, paramPort, true);
		m_random = new Random();
		m_socket = m_connection.getSocket();
		fmInstance = BackupSystem.getFiles();
		bsdbInstance = BackupSystem.getStorage();
	}
	
	private final String[] processHeader(final String paramHeader)
	{
		return paramHeader.trim().split(" ");
	}
	
	private final boolean checkPayload(final String[] messageHeader)
	{
		return messageHeader.length > 1 && m_payloadMessage.get(messageHeader[Message.Type]);
	}

	private final boolean checkPeerId(final String[] messageHeader)
	{
		return messageHeader.length > 2 && Integer.parseInt(messageHeader[Message.SenderId]) != BackupSystem.getPeerId();
	}
	
	protected abstract void processMessage(final GenericMessage paramMessage, boolean hasPayload);
	
	private void umarshallMessage(final DatagramPacket paramPacket)
	{
		final String convertedMessage = new String(paramPacket.getData(), paramPacket.getOffset(), paramPacket.getLength());
		
		int payloadSeparatorStart = convertedMessage.indexOf("\r\n\r\n");
		int payloadSeparatorEnd = payloadSeparatorStart + "\r\n\r\n".length();
		int payloadLength = paramPacket.getLength();
		byte[] messageBody = null;
		
		final String[] messageHeader = processHeader(convertedMessage.substring(0, payloadSeparatorStart));
		
		if (checkPeerId(messageHeader))
		{
			if (checkPayload(messageHeader))
			{	
				messageBody = Arrays.copyOfRange(paramPacket.getData(), payloadSeparatorEnd, payloadLength);		
				processMessage(new GenericMessage(messageHeader, messageBody), true);
			}
			else
			{
				processMessage(new GenericMessage(messageHeader), false);
			}
		}
	}
	
	protected final int generateBackoff()
	{
		return m_random.nextInt(BackupGlobals.maximumBackoffTime);
	}

	protected final int calculateHash(final String fileId, int chunkId)
	{
		int hash = 7;

		for (int i = 0; i < fileId.length(); i++)
		{
			hash = hash * 13 + fileId.charAt(i);
		}

		return (hash * 13) + chunkId;
	}
	
	@Override
	public void run()
	{
		byte[] buf = new byte[BackupGlobals.maximumPacketLength];

		while (m_connection.available())
		{
			try
			{
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				m_socket.receive(packet);
				umarshallMessage(packet);
			}
			catch (IOException ex)
			{
				System.err.println(ex.getMessage());
			}
		}
	}
}