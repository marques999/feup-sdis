package bs.protocol;

import bs.Peer;

public abstract class ExtraMessage extends SimpleMessage
{
	private int m_port = Peer.getUnicastPort();

	public int getPort()
	{
		return m_port;
	}

	protected ExtraMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId, "2.0");
	}

	@Override
	public void dump()
	{
		super.dump();
		System.out.println("\tPort: " + m_port);
	}

	@Override
	public final byte[] getMessage()
	{
		return (getHeader() + "\r\n" + Integer.toString(m_port) + "\r\n\r\n").getBytes();
	}
}