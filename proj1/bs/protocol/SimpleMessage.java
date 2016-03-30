package bs.protocol;

public abstract class SimpleMessage extends Message
{
	public SimpleMessage(final String fileId, int chunkId, final String msgVersion)
	{
		super(5, fileId, msgVersion);
		m_chunkId = chunkId;
	}

	private int m_chunkId;

	@Override
	public void dump()
	{
		super.dump();
		System.out.println("\tChunkNo: " + m_chunkId);
	}

	protected String getHeader()
	{
		final String[] m_header = generateHeader();
		m_header[Message.ChunkId] = Integer.toString(m_chunkId);
		return String.join(" ", m_header);
	}

	@Override
	public byte[] getMessage()
	{
		return (getHeader() + Message.CRLF).getBytes();
	}
}