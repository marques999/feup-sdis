package bs.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Date;

import bs.BackupSystem;
import bs.ResponseThread;
import bs.logging.Logger;
import bs.protocol.Message;
import bs.protocol.PayloadMessage;

public abstract class Protocol
{
	public Protocol(final InetAddress myAddress, int myPort)
	{
		m_host = myAddress;
		m_port = myPort;
		
		try
		{
			m_socket = new MulticastSocket(myPort);	
			m_socket.setTimeToLive(8);
			m_socket.joinGroup(myAddress);
			m_available = true;
		}
		catch (IOException ex)
		{
			m_available = false;
		}
		
		printStatus();
	}

	private InetAddress m_host = null;
	private MulticastSocket m_socket = null;

	private int m_port = 8080;
	private boolean m_available = false;

	public final boolean isAvailable()
	{
		return m_available;
	}

	public abstract String getName();
	protected abstract Message processMessage(final DatagramPacket paramPacket) throws VersionMismatchException;

	public final void printStatus()
	{
		if (m_available)
		{
			System.out.println(String.format("(%s) %s channel: connected to %s:%d",
			(new Date()).toString(), getName(),
			getHost().getHostAddress(), getPort()));
		}
		else
		{
			System.out.println(String.format("(%s) %s channel: connection error!",
			(new Date()).toString(), getName()));
		}
	}

	public final InetAddress getHost()
	{
		return m_host;
	}

	public final int getPort()
	{
		return m_port;
	}
	
	public final MulticastSocket getSocket()
	{
		return m_socket;
	}

	private final String[] processHeader(final String paramHeader)
	{
		return paramHeader.trim().split(" ");
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
	
	protected Message processSimpleMessage(final DatagramPacket paramPacket, final String paramType) throws VersionMismatchException
	{
		final String convertedMessage = new String(paramPacket.getData(), paramPacket.getOffset(), paramPacket.getLength());
		final String thisMethod = "processSimpleMessage";
		
		Message unmarshalledMessage = null;
		
		if (paramType == null || convertedMessage.startsWith(paramType))
		{
			unmarshalledMessage = Message.createSimpleMessage(processHeader(convertedMessage));
			
			if (unmarshalledMessage.getSender() == BackupSystem.getPeerId())
			{
				System.out.println("PING PONG! Duplicate message received...");
				unmarshalledMessage = null;
			}
		}
		else
		{
			Logger.logCommand(thisMethod, "WRONG TYPE");
		}

		return unmarshalledMessage;
	}

	protected PayloadMessage processPayloadMessage(final DatagramPacket paramPacket, final String paramType) throws VersionMismatchException
	{
		final String convertedMessage = new String(paramPacket.getData(), paramPacket.getOffset(), paramPacket.getLength());
		final String thisMethod = "processPayloadMessage";
		
		PayloadMessage unmarshalledMessage = null;

		if (convertedMessage.startsWith(paramType))
		{
			Logger.logGeneric("processPayloadMessage", "message contains payload data!");
			
			int payloadSeparatorStart = convertedMessage.indexOf("\r\n\r\n");
			int payloadSeparatorEnd = payloadSeparatorStart + "\r\n\r\n".length();
			int payloadLength = paramPacket.getLength();
				
			final String[] messageHeader = processHeader(convertedMessage.substring(0, payloadSeparatorStart));	
			final byte[] messageBody = Arrays.copyOfRange(paramPacket.getData(), payloadSeparatorEnd, payloadLength);

			unmarshalledMessage = Message.createPayloadMessage(messageHeader, messageBody);
			
			if (unmarshalledMessage.getSender() == BackupSystem.getPeerId())
			{
				System.out.println("PING PONG! Duplicate message received...");
				unmarshalledMessage = null;
			}
		}
		else
		{
			Logger.logCommand(thisMethod, "WRONG TYPE");
		}

		return unmarshalledMessage;
	}

	protected synchronized boolean sendMessage(final Message paramMessage)
	{
		if (!m_available)
		{
			return false;
		}

		final byte[] sbuf = paramMessage.getMessage();
		
		try
		{
			m_socket.send(new DatagramPacket(sbuf, sbuf.length, m_host, m_port));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		return true;
	}
	
	protected ResponseThread sendResponse(final Message paramMessage)
	{
		final ResponseThread rt = new ResponseThread(this, paramMessage);
		rt.start();
		return rt;
	}
	
	public final Message receive()
	{
		if (!m_available)
		{
			return null;
		}

		byte[] rbuf = new byte[0xffff];

		Message response = null;
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);

		try
		{
			m_socket.receive(packet);
			response = processMessage(packet);
		}
		catch (IOException | VersionMismatchException ex)
		{
			ex.printStackTrace();
		}

		return response;
	}
}