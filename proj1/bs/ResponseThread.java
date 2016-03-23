package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Random;

import bs.protocol.Message;
import bs.server.Protocol;

public class ResponseThread extends Thread
{
	private byte[] m_buffer;
	private long m_delay;
	
	public ResponseThread(final Protocol paramProtocol, final Message msg)
	{
		m_buffer = msg.getMessage();
		m_delay = Math.abs(new Random().nextLong()) % 400;
		m_message = msg;
		m_protocol = paramProtocol;
	}
	
	private Message m_message;
	private Protocol m_protocol;

	public final Message getMessage()
	{
		return m_message;		
	}
	
	@Override
	public void run()
	{
		final MulticastSocket thisSocket = m_protocol.getSocket();

		try
		{
			System.out.println("thread going to sleep for " + m_delay + "milliseconds!");
			Thread.sleep(m_delay);
			System.out.println("thread is awake, going to send!");
			thisSocket.send(new DatagramPacket(m_buffer, m_buffer.length, m_protocol.getHost(), m_protocol.getPort()));
		}
		catch (InterruptedException | IOException e)
		{
			System.out.println("thread sending cancelled!");
		}
	}
}