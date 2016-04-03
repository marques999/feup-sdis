package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UnicastAdapter extends Thread
{
	private final BaseService m_service;
	private final Object m_mutex;
	private DatagramSocket m_socket;

	public UnicastAdapter(final BaseService paramService)
	{
		m_service = paramService;
		m_available = false;
		m_mutex = new Object();

		try
		{
			m_socket = new DatagramSocket(0);
			m_available = true;
		}
		catch (IOException ex)
		{
			m_available = false;
		}

		if (m_available)
		{
			Logger.logInformation(String.format(PeerStrings.messageUnicastListening, getPort()));
		}
		else
		{
			Logger.logError(String.format(PeerStrings.messageUnicastError));
		}
	}

	private boolean m_available;

	public final int getPort()
	{
		return m_socket.getLocalPort();
	}

	public final boolean available()
	{
		return m_available;
	}

	public boolean send(final byte[] paramBuffer, final InetAddress paramAddress, int paramPort)
	{
		if (!m_available)
		{
			return false;
		}

		synchronized (m_mutex)
		{
			try
			{
				m_socket.send(new DatagramPacket(paramBuffer, paramBuffer.length, paramAddress, paramPort));
			}
			catch (IOException ex)
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void run()
	{
		byte[] buf = new byte[PeerGlobals.maximumPacketLength];

		while (m_available)
		{
			final DatagramPacket packet = new DatagramPacket(buf, buf.length);

			try
			{
				m_socket.receive(packet);
				m_service.umarshallMessage(packet);
			}
			catch (IOException ex)
			{
				System.err.println(ex.getMessage());
			}
		}
	}
}