package bs.protocol;

public abstract class SimpleMessage extends Message
{
	/*
	 * This field together with the FileId specifies a chunk in the file. The
	 * chunk numbers are integers and should be assigned sequentially starting
	 * at 0. It is encoded as a sequence of ASCII characters corresponding to
	 * the decimal representation of that number, with the most significant
	 * digit first. The length of this field is variable, but should not be
	 * larger than 6 chars. Therefore, each file can have at most one million
	 * chunks. Given that each chunk is 64 KByte, this limits the size of the
	 * files to backup to 64 GByte.
	 */
	private int m_chunkId;

	public SimpleMessage(final String fileId, int chunkId, final String msgVersion)
	{
		super(5, fileId, msgVersion);
		m_chunkId = chunkId;
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