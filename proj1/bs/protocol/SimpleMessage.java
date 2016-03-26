package bs.protocol;

public abstract class SimpleMessage extends Message
{
	public SimpleMessage(final String fileId, int chunkId)
	{
		super(5, fileId);
		m_chunkId = chunkId;
	}

	public SimpleMessage(final String[] paramHeader)
	{
		super(paramHeader);
		m_chunkId = Integer.parseInt(paramHeader[Message.ChunkId]);
	}
	
	private int m_chunkId;

	public int getChunkId()
	{
		return m_chunkId;
	}

	@Override
	public final void dump()
	{
		super.dump();
		System.out.println("\tChunkNo: " + m_chunkId);
	}

	@Override
	public final byte[] getMessage()
	{
		final String[] m_header = generateHeader();
		m_header[Message.ChunkId] = Integer.toString(m_chunkId);
		return (String.join(" ", m_header) + Message.CRLF).getBytes();
	}
}