package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

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

	private final String[] processHeader(final String paramHeader)
	{
		return paramHeader.trim().split(" ");
	}
	
	protected Message processSimpleMessage(final DatagramPacket paramPacket, final String paramType) throws VersionMismatchException
	{
		final String convertedMessage = new String(paramPacket.getData(), paramPacket.getOffset(), paramPacket.getLength());
		final String thisMethod = "processSimpleMessage";
		
		Message unmarshalledMessage = null;
		
		if (paramType == null || convertedMessage.startsWith(paramType))
		{
			String[] messageHeader = processHeader(convertedMessage);
			
			if (messageHeader[0].equals("GETCHUNK"))
			{
				unmarshalledMessage = new GETCHUNKMessage(messageHeader);
			}
			else if (messageHeader[0].equals("DELETE"))
			{
				unmarshalledMessage = new DELETEMessage(messageHeader);
			}
			else if (messageHeader[0].equals("STORED"))
			{
				unmarshalledMessage = new STOREDMessage(messageHeader);
			}
			else if (messageHeader[0].equals("REMOVED"))
			{		
				unmarshalledMessage = new REMOVEDMessage(messageHeader);
			}
		}
		else
		{
			Logger.logCommand(thisMethod, "WRONG TYPE");
		}
		
		if (unmarshalledMessage.getSender() == BackupSystem.getPeerId())
		{
			System.out.println("PING PONG! Duplicate message received...");
			return null;
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
			int payloadSeparatorStart = convertedMessage.indexOf("\r\n\r\n");
			int payloadSeparatorEnd = payloadSeparatorStart + "\r\n\r\n".length();
			int payloadLength = paramPacket.getLength();
			
			Logger.logGeneric("processPayloadMessage", "message contains payload data!");
			
			final String[] messageHeader = processHeader(convertedMessage.substring(0, payloadSeparatorStart));	
			final byte[] messageBody = Arrays.copyOfRange(paramPacket.getData(), payloadSeparatorEnd, payloadLength);

			if (messageHeader[0].equals("PUTCHUNK"))
			{
				unmarshalledMessage = new PUTCHUNKMessage(messageHeader, messageBody);
			}
			else if (messageHeader[0].equals("CHUNK"))
			{
				unmarshalledMessage = new CHUNKMessage(messageHeader, messageBody);
			}
		}
		else
		{
			Logger.logCommand(thisMethod, "WRONG TYPE");
		}

		return unmarshalledMessage;
	}

	protected boolean send(final Message paramMessage)
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
			return false;
		}

		return true;
	}
	
	private class ReceiveThread extends Thread
	{
		private byte[] myBuffer = new byte[0xffff];

		public ReceiveThread(final String paramName)
		{
			System.out.println("starting " + paramName + " thread");
		}
		
		public Message getMessage()
		{
			return myMessage;
		}
		
		private Message myMessage = null;

		@Override
		public void run()
		{		
			for (;;)
			{
				DatagramPacket packet = new DatagramPacket(myBuffer, myBuffer.length);
		
				try
				{
					m_socket.receive(packet);
					myMessage = processMessage(packet);
				}
				catch (IOException | VersionMismatchException ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}
	
	private class ResponseThread extends Thread
	{
		private byte[] myBuffer;
		
		public ResponseThread(final byte[] msg)
		{
			myBuffer = msg;
			myRandom = new Random();
		}
		
		private Random myRandom;
	
		@Override
		public void run()
		{
			try
			{
				long sleepTime = Math.abs(myRandom.nextLong()) % 400;
				System.out.println("thread going to sleep for " + sleepTime + "milliseconds!");
				Thread.sleep(sleepTime);
				System.out.println("thread is awake!");
				m_socket.send(new DatagramPacket(myBuffer, myBuffer.length, m_host, m_port));
			}
			catch (InterruptedException | IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public abstract void sendResponse(final String fileId, int chunkId);

	protected void sendResponse(final Message paramMessage)
	{
		new ResponseThread(paramMessage.getMessage()).start();
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