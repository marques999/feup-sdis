package sdis_proj1;

public abstract class SimpleMessage extends Message
{
	/*
	 * This field together with the FileId specifies a chunk in the file.
	 * The chunk numbers are integers and should be assigned sequentially
	 * starting at 0. It is encoded as a sequence of ASCII characters
	 * corresponding to the decimal representation of that number, with the
	 * most significant digit first. The length of this field is variable,
	 * but should not be larger than 6 chars. Therefore, each file can have
	 * at most one million chunks. Given that each chunk is 64 KByte, this
	 * limits the size of the files to backup to 64 GByte.
	 */
	private int m_chunkId;

	protected SimpleMessage(FileChunk paramChunk)
	{
		super(5, paramChunk.getFileId());
		m_chunkId = paramChunk.getChunkId();
	}

	protected SimpleMessage(String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
		m_chunkId = Integer.parseInt(paramHeader[MessageHeader.ChunkId]);
	}

	protected void dump()
	{
		super.dump();
		System.out.println("ChunkNo:\t" + m_chunkId);
	}

	public final byte[] getMessage()
	{
		final String[] m_header = generateHeader();
		m_header[MessageHeader.ChunkId] = Integer.toString(m_chunkId);
		return (String.join(" ", m_header) + "\r\n\r\n").getBytes();
	}
}

class STOREDMessage extends SimpleMessage
{
	public STOREDMessage(final FileChunk paramChunk)
	{
		super(paramChunk);
	}

	protected STOREDMessage(String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

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

	protected GETCHUNKMessage(String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

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

	protected REMOVEDMessage(String[] paramHeader) throws VersionMismatchException
	{
		super(paramHeader);
	}

	public final String getType()
	{
		return "REMOVED";
	}
}