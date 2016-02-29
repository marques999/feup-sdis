package sdis_proj1;

public abstract class SimpleMessage
{
	protected static final int TYPE = 0;
	protected static final int VERSION = 1;
	protected static final int SENDER_ID = 2;
	protected static final int FILE_ID = 3;
	protected static final int CHUNK_NO = 4;

	private static final String HEADER_EOF = "\r\n";
	protected String[] m_header;

	protected SimpleMessage(FileChunk paramChunk, int messageLength)
	{
		m_header = new String[messageLength];
		m_header[TYPE] = getType();
		m_header[SENDER_ID] = Integer.toString(Server.getInstance().getServerId());
		m_header[VERSION] = Server.getInstance().getVersion();
		m_header[FILE_ID] = paramChunk.getFileId();
		m_header[CHUNK_NO] = Integer.toString(paramChunk.getChunkId());
	}
	
	protected void attach(String fileId, int chunkId)
	{
		m_header[FILE_ID] = fileId;
		m_header[CHUNK_NO] = Integer.toString(chunkId);
	}
	
	protected abstract String getType();
	
	public byte[] getMessage()
	{
		return (String.join(" ", m_header) + HEADER_EOF).getBytes();
	}
}

class STOREDMessage extends SimpleMessage
{
	public STOREDMessage(final FileChunk paramChunk)
	{
		super(paramChunk, 5);
	}

	protected String getType()
	{
		return "STORED";
	}
}

class GETCHUNKMessage extends SimpleMessage
{
	public GETCHUNKMessage(final FileChunk paramChunk)
	{
		super(paramChunk, 5);
	}

	protected String getType()
	{
		return "GETCHUNK";
	}
}

class DELETEMessage extends SimpleMessage
{
	public DELETEMessage(final FileChunk paramChunk)
	{
		super(paramChunk, 4);
	}

	protected String getType()
	{
		return "DELETE";
	}
}

class REMOVEDMessage extends SimpleMessage
{
	public REMOVEDMessage(final FileChunk paramChunk)
	{
		super(paramChunk, 5);
	}

	protected String getType()
	{
		return "REMOVED";
	}
}