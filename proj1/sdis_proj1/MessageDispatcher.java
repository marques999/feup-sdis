package sdis_proj1;

public class MessageDispatcher extends Thread {

	private final String m_type;
	
	public MessageDispatcher(final byte[] datagramPacket, final String messageType) {
		m_type = messageType;
		m_data = datagramPacket;
	}
	
	private final byte[] m_data;

	@Override
	public void run()
	{
	}
}