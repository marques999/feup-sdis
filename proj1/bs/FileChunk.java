package bs;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileChunk
{
	private static final int MAXIMUM_CHUNK_SIZE = 64000;

	public FileChunk(final BufferedInputStream myStream, final String myFile, int myChunkId, int myDegree) throws IOException
	{
		m_data = new byte[MAXIMUM_CHUNK_SIZE];

		final int bytesRead = myStream.read(m_data);
		final int chunkSize = bytesRead < 0 ? 0 : bytesRead;

		m_data = Arrays.copyOf(m_data, chunkSize);
		m_count = 0;
		m_degree = myDegree;
		m_fileId = myFile;
		m_chunkId = myChunkId;
		m_size = chunkSize;
	}

	private int m_chunkId;
	private int m_count;
	private int m_degree;
	private int m_size;
	private byte[] m_data;

	public FileChunk(final String myFile, int myChunkId, final byte[] myData, int myDegree)
	{
		m_data = myData;
		m_count = 0;
		m_degree = myDegree;
		m_fileId = myFile;
		m_chunkId = myChunkId;
		m_size = m_data.length;
	}

	private String m_fileId;

	public final byte[] getData()
	{
		return m_data;
	}

	public final int getCount()
	{
		return m_count;
	}
	
	public final int getReplicationDegree()
	{
		return m_degree;
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
	
	public void decreaseCount()
	{
		m_count--;
	}
	
	public void increaseCount()
	{
		m_count++;
	}
}