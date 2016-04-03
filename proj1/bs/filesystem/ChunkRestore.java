package bs.filesystem;

import java.util.HashMap;

import bs.logging.BadChunkException;
import bs.logging.MissingChunkException;

public class ChunkRestore
{
	private final String fileId;
	private final HashMap<Integer, Chunk> receivedChunks;

	public ChunkRestore(final FileInformation restoreInformation)
	{
		fileId = restoreInformation.getFileId();
		fileSize = restoreInformation.getFileSize();
		numberChunks = restoreInformation.getCount();
		receivedChunks = new HashMap<Integer, Chunk>();
	}

	private int numberChunks;
	private long fileSize;

	public final boolean put(final Chunk paramChunk)
	{
		int chunkId = paramChunk.getChunkId();

		if (receivedChunks.containsKey(chunkId) || !paramChunk.getFileId().equals(fileId))
		{
			return false;
		}

		receivedChunks.put(chunkId, paramChunk);

		return true;
	}

	public byte[] join() throws MissingChunkException, BadChunkException
	{
		final Chunk lastChunk = receivedChunks.get(numberChunks - 1);

		if (lastChunk == null || !lastChunk.isLast())
		{
			throw new MissingChunkException(fileId);
		}

		final Chunk[] fileChunks = new Chunk[numberChunks];

		for (int chunkId = 0; chunkId < numberChunks; chunkId++)
		{
			if (!receivedChunks.containsKey(chunkId))
			{
				throw new MissingChunkException(fileId);
			}

			final Chunk currentChunk = receivedChunks.get(chunkId);

			if (currentChunk.isLast() && chunkId != (numberChunks - 1))
			{
				throw new BadChunkException(chunkId);
			}

			fileChunks[chunkId] = currentChunk;
		}

		byte[] m_buffer = new byte[(int) fileSize];
		int bytesWritten = 0;

		for (int id = 0; id < numberChunks; id++)
		{
			int bytesToWrite = (int) fileChunks[id].getLength();

			if (bytesToWrite > 0)
			{
				System.arraycopy(fileChunks[id].getData(), 0, m_buffer, bytesWritten, bytesToWrite);
				bytesWritten += bytesToWrite;
			}
		}

		if (bytesWritten != fileSize)
		{
			throw new MissingChunkException(fileId);
		}

		return m_buffer;
	}
}