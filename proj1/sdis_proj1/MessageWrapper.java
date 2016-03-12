package sdis_proj1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MessageWrapper
{
	private InetAddress m_host;
	
	public MessageWrapper(InetAddress paramHost, int paramPort) {
		m_host = paramHost;
		m_port = paramPort;
	}
	
	public boolean sendResponse(final Message paramMessage) {
		
		final byte[] request = paramMessage.getMessage();
		
		try (final DatagramSocket socket = new DatagramSocket()) {
			socket.send(new DatagramPacket(request, request.length, m_host, m_port));
		}
		catch (IOException ex) {
			return false;
		}
		
		return true;
	}
	
	public boolean sendRequest(final Message paramMessage) {
		
		final byte[] request = paramMessage.getMessage();
		
		try (final MulticastSocket socket = new MulticastSocket(m_port)) {
			socket.send(new DatagramPacket(request, request.length, m_host, m_port));
		}
		catch (IOException ex) {
			return false;
		}
		
		return true;
	}
	
	private int m_port;
}