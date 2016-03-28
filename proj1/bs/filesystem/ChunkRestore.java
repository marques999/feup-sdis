package bs.filesystem;

import java.util.HashMap;

public class ChunkRestore
{
	private final String m_fileId;
	private final HashMap<Integer, Chunk> m_chunksmap;

	public ChunkRestore(final FileInformation restoreInformation)
	{
		m_fileId = restoreInformation.getFileId();
		m_fileSize = restoreInformation.getFileSize();
		m_count = restoreInformation.getCount();
		m_chunksmap = new HashMap<Integer, Chunk>();
	}

	private int m_count;
	private long m_fileSize;

	public final boolean put(final Chunk paramChunk)
	{
		int chunkId = paramChunk.getChunkId();

		if (m_chunksmap.containsKey(chunkId) || !paramChunk.getFileId().equals(m_fileId))
		{
			return false;
		}

		m_chunksmap.put(chunkId, paramChunk);

		return true;
	}

	public byte[] join() throws MissingChunkException, BadChunkException
	{
		final Chunk lastChunk = m_chunksmap.get(m_count - 1);

		if (lastChunk == null || !lastChunk.isLast())
		{
			throw new MissingChunkException(m_fileId);
		}

		final Chunk[] fileChunks = new Chunk[m_count];

		for (int chunkId = 0; chunkId < m_count; chunkId++)
		{
			if (!m_chunksmap.containsKey(chunkId))
			{
				throw new MissingChunkException(m_fileId);
			}

			final Chunk currentChunk = m_chunksmap.get(chunkId);

			if (currentChunk.isLast() && chunkId != (m_count - 1))
			{
				throw new BadChunkException(currentChunk);
			}

			fileChunks[chunkId] = currentChunk;
		}

		byte[] m_buffer = new byte[(int) m_fileSize];
		int bytesWritten = 0;

		for (int id = 0; id < m_count; id++)
		{
			int bytesToWrite = (int) fileChunks[id].getLength();

			if (bytesToWrite > 0)
			{
				System.arraycopy(fileChunks[id].getData(), 0, m_buffer, bytesWritten, bytesToWrite);
				bytesWritten += bytesToWrite;
			}
		}

		if (bytesWritten != m_fileSize)
		{
			throw new MissingChunkException(m_fileId);
		}

		return m_buffer;
	}
}