package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.IOException;

public class FileChunk
{
	private static final int MAXIMUM_CHUNK_SIZE = 64000;

	private String m_fileId;

	private int m_chunkId;
	private int m_size;
	private byte[] m_data;

	public FileChunk(BufferedInputStream paramStream, String paramFile,	int paramChunk) throws IOException
	{
		m_data = new byte[MAXIMUM_CHUNK_SIZE];

		int bytesRead = paramStream.read(m_data, 0, MAXIMUM_CHUNK_SIZE);

		if (bytesRead < 0)
		{
			throw new IOException();
		}

		m_fileId = paramFile;
		m_chunkId = paramChunk;
		m_size = bytesRead;
	}

	public final byte[] getContents()
	{
		return m_data;
	}
	
	public final int getLength()
	{
		return m_size;
	}

	public final int getChunkId()
	{
		return m_chunkId;
	}

	public final String getFileId()
	{
		return m_fileId;
	}

	public final boolean isFirst()
	{
		return m_chunkId == 0;
	}

	public final boolean isLast()
	{
		return m_size < MAXIMUM_CHUNK_SIZE;
	}
}