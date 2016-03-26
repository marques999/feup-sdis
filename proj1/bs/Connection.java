package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import bs.logging.Logger;

public class Connection
{	
	private static final String msgConnected = "%s connected to %s:%d";
	private static final String msgListening = "%s listening at %s:%d";
	private static final String msgConnectionError = "%s: connection error!";

	public Connection(final String paramName, final InetAddress paramAddress, int paramPort, boolean paramListen)
	{
		m_host = paramAddress;
		m_port = paramPort;
		m_name = paramName;
		m_available = false;
		m_mutex = new Object();

		try
		{
			if (paramListen)
			{
				m_socket = new MulticastSocket(paramPort);	
				m_socket.setTimeToLive(1);
				m_socket.joinGroup(paramAddress);
			}
			else
			{
				m_socket = new MulticastSocket();
				m_socket.setTimeToLive(1);
			}
			
			m_available = true;
		}
		catch (IOException ex)
		{
			m_available = false;
		}

		if (m_available)
		{
			final String ipAddress = m_host.getHostAddress();
		
			if (paramListen)
			{
				Logger.logDebug(String.format(msgListening, m_name, ipAddress, m_port));
			}
			else
			{
				Logger.logDebug(String.format(msgConnected, m_name, ipAddress, m_port));
			}
		}
		else
		{
			Logger.logDebug(String.format(msgConnectionError, m_name));	
		}
	}

	private final Object m_mutex;
	private final InetAddress m_host;
	
	public final InetAddress getHost()
	{
		return m_host;
	}
	
	private MulticastSocket m_socket;
	
	public final MulticastSocket getSocket()
	{
		return m_socket;
	}
	
	private final int m_port;
	
	public final int getPort()
	{
		return m_port;
	}
	
	private boolean m_available;

	public final boolean available()
	{
		return m_available;
	}
	
	private final String m_name;
	
	public final String getName()
	{
		return m_name;
	}

	public boolean send(final byte[] paramBuffer)
	{
		if (!m_available)
		{
			return false;
		}
		
		synchronized (m_mutex)
		{
			try
			{
				m_socket.send(new DatagramPacket(paramBuffer, paramBuffer.length, m_host, m_port));
			}
			catch (IOException ex)
			{
				return false;
			}
		}
		
		return true;
	}
}