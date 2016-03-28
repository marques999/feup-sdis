package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Random;

import bs.filesystem.BackupStorage;
import bs.filesystem.FileManager;
import bs.protocol.GenericMessage;
import bs.protocol.Message;

public abstract class BaseService extends Thread
{
	private final Random m_random;
	private final MulticastConnection m_connection;
	private final MulticastSocket m_socket;
	protected final FileManager fmInstance;
	protected final BackupStorage bsdbInstance;

	/**
	 * @brief default constructor for 'BackupService' class
	 * @param paramAddress address of the multicast channel
	 * @param paramPort port of the multicast channel
	 */
	public BaseService(final String paramName, final InetAddress paramAddress, int paramPort)
	{
		m_connection = new MulticastConnection(paramName, paramAddress, paramPort, true);
		m_random = new Random();
		m_socket = m_connection.getSocket();
		fmInstance = BackupSystem.getFiles();
		bsdbInstance = BackupSystem.getStorage();
	}

	private final String[] processHeader(final String paramHeader)
	{
		return paramHeader.trim().split(" ");
	}

	public final MulticastConnection getConnection()
	{
		return m_connection;
	}

	private final boolean checkPayload(final String[] messageHeader)
	{
		if (messageHeader.length < 2)
		{
			return false;
		}

		final String messageType = messageHeader[Message.Type];
		return messageType.equals("CHUNK") || messageType.equals("PUTCHUNK");
	}

	private final boolean checkPeerId(final String[] messageHeader)
	{
		return messageHeader.length > 2 && Integer.parseInt(messageHeader[Message.SenderId]) != BackupSystem.getPeerId();
	}

	protected abstract void processMessage(final GenericMessage paramMessage, final DatagramPacket paramPacket, boolean hasPayload);

	protected final void umarshallMessage(final DatagramPacket paramPacket)
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
				processMessage(new GenericMessage(messageHeader, messageBody), paramPacket, true);
			}
			else
			{
				processMessage(new GenericMessage(messageHeader), paramPacket, false);
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
			final DatagramPacket packet = new DatagramPacket(buf, buf.length);

			try
			{
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