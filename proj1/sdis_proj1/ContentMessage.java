package sdis_proj1;

import java.util.Arrays;

public abstract class ContentMessage
{
	// -------------------------------------------------
	private static final int TYPE = 0;
	private static final int VERSION = 1;
	private static final int SENDER_ID = 2;
	private static final int FILE_ID = 3;
	private static final int CHUNK_NO = 4;
	// -------------------------------------------------
	private static final String HEADER_EOF = "\r\n\r\n";
	// -------------------------------------------------
	protected String[] m_header;
	// -------------------------------------------------
	private byte[] m_body;

	protected ContentMessage(int headerSize, FileChunk paramChunk)
	{
		m_header = new String[headerSize];
		m_body = paramChunk.getContents();
		m_header[TYPE] = getType();
		m_header[SENDER_ID] = Integer.toString(Server.getInstance().getServerId());
		m_header[VERSION] = Server.getInstance().getVersion();
		m_header[FILE_ID] = paramChunk.getFileId();
		m_header[CHUNK_NO] = Integer.toString(paramChunk.getChunkId());
	}

	protected abstract String getType();

	public byte[] getHeader()
	{
		return (String.join(" ", m_header) + HEADER_EOF).getBytes();
	}

	public byte[] getBody()
	{
		return m_body;
	}
	
	public byte[] getMessage()
	{
		byte[] messageHeader = (String.join(" ", m_header) + HEADER_EOF).getBytes();
		byte[] result = Arrays.copyOf(messageHeader, messageHeader.length + m_body.length);
		System.arraycopy(m_body, 0, result, messageHeader.length, m_body.length);
		return result;
	}
}

class PUTCHUNKMessage extends ContentMessage
{
	private static final int REPLICATION_DEGREE = 5;
	
	public PUTCHUNKMessage(FileChunk paramChunk, int replicationDegree)
	{
		super(6, paramChunk);
		m_header[REPLICATION_DEGREE] = Integer.toString(replicationDegree);
	}

	protected String getType()
	{
		return "PUTCHUNK";
	}
}

class CHUNKMessage extends ContentMessage
{
	public CHUNKMessage(FileChunk paramChunk)
	{
		super(5, paramChunk);
	}
	
	protected String getType()
	{
		return "CHUNK";
	}
}