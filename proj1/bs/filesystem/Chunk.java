package bs.filesystem;

import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import bs.BackupGlobals;

public class Chunk implements Serializable
{
	private static final long serialVersionUID = 6234395837590681732L;

	public Chunk(final InputStream fileStream, final String fileId, int chunkId, int replicationDegree) throws IOException
	{
		m_data = new byte[(int) BackupGlobals.maximumChunkSize];

		int bytesRead = fileStream.read(m_data);

		if (bytesRead < 0)
		{
			bytesRead = 0;
		}

		m_data = Arrays.copyOf(m_data, bytesRead);
		m_degree = replicationDegree;
		m_fileId = fileId;
		m_chunkId = chunkId;
		m_size = bytesRead;
	}

	public Chunk(final byte[] fileBuffer, final String fileId, int chunkId, int replicationDegree)
	{
		m_data = fileBuffer;
		m_degree = replicationDegree;
		m_fileId = fileId;
		m_chunkId = chunkId;
		m_size = fileBuffer.length;
	}

	//-----------------------------------------------------

	public final boolean equals(final Object other)
	{
		if (other instanceof Chunk)
		{
			final Chunk rhs = (Chunk) other;
			return (m_fileId.equals(rhs.m_fileId) && m_chunkId == rhs.m_chunkId);
		}

		return false;
	}

	//-----------------------------------------------------

	private final String m_fileId;

	public final String getFileId()
	{
		return m_fileId;
	}

	//-----------------------------------------------------

	private final int m_degree;

	public final int getReplicationDegree()
	{
		return m_degree;
	}

	//-----------------------------------------------------

	private byte[] m_data;

	public final byte[] getData()
	{
		return m_data;
	}

	//-----------------------------------------------------

	private final long m_size;

	public final long getLength()
	{
		return m_size;
	}

	//-----------------------------------------------------

	private final int m_chunkId;

	public final int getChunkId()
	{
		return m_chunkId;
	}

	public final boolean isFirst()
	{
		return m_chunkId == 0;
	}

	public final boolean isLast()
	{
		return m_size < BackupGlobals.maximumChunkSize;
	}
}