package sdis_proj1;

import java.util.HashMap;

public class ChunkRestore
{
	private final String m_fileId;
	private final HashMap<Integer, FileChunk> m_chunksmap;

	public ChunkRestore(final String paramFile)
	{
		m_fileId = paramFile;
		m_chunksmap = new HashMap<Integer, FileChunk>();
	}

	/*
	 * This method returns a chunk from the hash map given the key
	 */
	public final FileChunk get(int chunkId)
	{
		if (m_chunksmap.containsKey(chunkId))
		{
			return m_chunksmap.get(chunkId);
		}

		return null;
	}

	/*
	 * This method adds a file chunk to the hash map
	 * @throws BadChunkException
	 */
	public final void put(final FileChunk chunk) throws BadChunkException
	{
		// -----------------------------------
		// 1) check if received a valid chunk
		// -----------------------------------

		if (chunk.getChunkId() < 0 || !chunk.getFileId().equals(m_fileId))
		{
			throw new BadChunkException(chunk);
		}

		// ------------------------------------------
		// 2) verify if there are no duplicate chunks
		// ------------------------------------------

		if (!m_chunksmap.containsKey(chunk.getChunkId()))
		{
			m_chunksmap.put(chunk.getChunkId(), chunk);
		}
	}

	/*
	 * This method returns the received file chunks as an array of bytes
	 * @throws MissingChunksException, BadChunkException
	 */
	public byte[] join() throws MissingChunksException, BadChunkException
	{
		// -----------------------------------
		// 1) obtain number of received chunks
		// -----------------------------------

		int numberChunks = m_chunksmap.size();
		int fileSize = 0;

		// -------------------------------------
		// 2) verify if all chunks were received
		// -------------------------------------

		final FileChunk lastChunk = m_chunksmap.get(numberChunks - 1);

		if (lastChunk == null || !lastChunk.isLast())
		{
			throw new MissingChunksException(m_fileId);
		}

		// -------------------------------------------------
		// 3) sort all chunks by id and calculate total size
		// -------------------------------------------------

		final FileChunk[] fileChunks = new FileChunk[numberChunks];

		for (int id = 0; id < numberChunks; id++)
		{
			if (!m_chunksmap.containsKey(id))
			{
				throw new MissingChunksException(m_fileId);
			}

			FileChunk currentChunk = m_chunksmap.get(id);

			if (currentChunk.isLast() && id != numberChunks - 1)
			{
				throw new BadChunkException(currentChunk);
			}

			fileChunks[id] = currentChunk;
			fileSize += fileChunks[id].getLength();
		}

		// ---------------------------------
		// 4) initialize and fill data array
		// ---------------------------------

		byte[] data = new byte[fileSize];
		int bytesWritten = 0;

		for (int id = 0; id < numberChunks; id++)
		{
			int bytesToWrite = fileChunks[id].getLength();

			if (bytesToWrite > 0)
			{
				System.arraycopy(fileChunks[id].getData(), 0, data, bytesWritten, bytesToWrite);
				bytesWritten += bytesToWrite;
			}
		}

		// ------------------------------------------
		// 5) check file size after joining chunks
		// ------------------------------------------

		if (bytesWritten != fileSize)
		{
			throw new MissingChunksException(m_fileId);
		}

		return data;
	}
}