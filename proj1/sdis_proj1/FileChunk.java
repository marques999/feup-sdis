package sdis_proj1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileChunk
{
	private static final int MAXIMUM_CHUNK_SIZE = 64000;

	private int m_chunkId;
	private int m_size;
	private byte[] m_data;

	public FileChunk(BufferedInputStream paramStream, String paramFile,	int paramChunk) throws IOException
	{
		m_data = new byte[MAXIMUM_CHUNK_SIZE];

		final int bytesRead = paramStream.read(m_data);
		final int chunkSize = bytesRead < 0 ? 0 : bytesRead;
		
		m_data = Arrays.copyOf(m_data, chunkSize);		
		setParameters(paramFile, paramChunk, chunkSize);
	}
	
	public FileChunk(String[] paramHeader, byte[] paramBuffer)
	{
		m_data = paramBuffer;
		setParameters(paramHeader[MessageHeader.FileId],
			Integer.parseInt(paramHeader[MessageHeader.ChunkId]), 
			m_data.length);
	}
	
	private String m_fileId;
	
	private void setParameters(final String fileId, int chunkId, int chunkSize)
	{
		m_fileId = fileId;
		m_chunkId = chunkId;
		m_size = m_data.length;
	}

	public final byte[] getData()
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