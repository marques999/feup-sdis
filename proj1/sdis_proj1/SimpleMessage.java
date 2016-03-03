package sdis_proj1;

public abstract class SimpleMessage extends Message
{
	/*
	 * This attribute together with the FileId specifies a chunk in the file.
	 * The chunk numbers are integers and should be assigned sequentially
	 * starting at 0. It is encoded as a sequence of ASCII characters
	 * corresponding to the decimal representation of that number, with the
	 * most significant digit first. The length of this field is variable,
	 * but should not be larger than 6 chars. Therefore, each file can have
	 * at most one million chunks. Given that each chunk is 64 KByte, this
	 * limits the size of the files to backup to 64 GByte.
	 */
	private final int m_chunkId;

	protected SimpleMessage(final FileChunk paramChunk)
	{
		super(5, paramChunk.getFileId());
		m_chunkId = paramChunk.getChunkId();
	}

	protected SimpleMessage(final String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
		m_chunkId = Integer.parseInt(paramHeader[MessageFields.ChunkId]);
	}

	@Override
	protected void dump()
	{
		super.dump();
		System.out.println("\tChunkNo: " + m_chunkId);
	}

	@Override
	public final byte[] getMessage()
	{
		final String[] m_header = generateHeader();
		m_header[MessageFields.ChunkId] = Integer.toString(m_chunkId);
		return (String.join(" ", m_header) + "\r\n\r\n").getBytes();
	}
}

class STOREDMessage extends SimpleMessage
{
	public STOREDMessage(final FileChunk paramChunk)
	{
		super(paramChunk);
	}

	protected STOREDMessage(final String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "STORED";
	}
}

class GETCHUNKMessage extends SimpleMessage
{
	public GETCHUNKMessage(final FileChunk paramChunk)
	{
		super(paramChunk);
	}

	protected GETCHUNKMessage(final String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "GETCHUNK";
	}
}

class REMOVEDMessage extends SimpleMessage
{
	public REMOVEDMessage(final FileChunk paramChunk)
	{
		super(paramChunk);
	}

	protected REMOVEDMessage(final String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "REMOVED";
	}
}