package bs.filesystem;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

import bs.Peer;

public class ChunkCollection implements Serializable
{
	private static final long serialVersionUID = -108274861401132235L;

	public ChunkCollection()
	{
		m_fileChunks = new HashMap<Integer, ChunkInformation>();
		m_size = 0;
	}

	// -----------------------------------------------------

	private long m_size;

	public final long getSize()
	{
		return m_size;
	}

	// -----------------------------------------------------

	private final HashMap<Integer, ChunkInformation> m_fileChunks;

	public final HashMap<Integer, ChunkInformation> getChunks()
	{
		return m_fileChunks;
	}

	public final Integer[] getChunkIds()
	{
		if (m_fileChunks.isEmpty())
		{
			return new Integer[] {};
		}

		int i = 0;

		final Integer[] chunkIds = new Integer[m_fileChunks.size()];
		final Set<Integer> chunkSet = m_fileChunks.keySet();

		for (final Integer chunkId : chunkSet)
		{
			if (!m_fileChunks.get(chunkId).isRemote())
			{
				chunkIds[i++] = chunkId;
			}
		}

		return chunkIds;
	}

	public final int getNumberChunks()
	{
		return m_fileChunks.size();
	}

	public final boolean isEmpty()
	{
		return m_fileChunks.isEmpty();
	}

	// -----------------------------------------------------

	public final ChunkInformation getChunkInformation(int chunkId)
	{
		if (m_fileChunks.containsKey(chunkId))
		{
			return m_fileChunks.get(chunkId);
		}

		return null;
	}

	/**
	 * @brief removes a chunk from this collection
	 * @param chunkId chunk identifier number
	 */
	public final long removeChunk(int chunkId)
	{
		long chunkSize = 0;

		if (m_fileChunks.containsKey(chunkId))
		{
			final ChunkInformation chunkInformation = m_fileChunks.get(chunkId);

			if (chunkInformation.isRemote())
			{
				m_fileChunks.remove(chunkId);
			}
			else
			{
				chunkSize = m_fileChunks.remove(chunkId).getLength();
				m_size -= chunkSize;
			}
		}

		return chunkSize;
	}

	/**
	 * @brief decreases the replication count of a chunk
	 * @param chunkId chunk identifier number
	 */
	public final boolean removePeer(int chunkId, int peerId)
	{
		if (!m_fileChunks.containsKey(chunkId))
		{
			return false;
		}
		
		m_fileChunks.get(chunkId).removePeer(peerId);
		
		return true;
	}

	/**
	 * @brief increases the replication count of a chunk
	 * @param chunkId chunk identifier number
	 */
	public final boolean registerPeer(int chunkId, int peerId)
	{
		if (!m_fileChunks.containsKey(chunkId))
		{
			return false;
		}
		
		m_fileChunks.get(chunkId).registerPeer(peerId);
		
		return true;
	}

	/**
	 * @brief checks if a given chunk exists
	 * @param chunkId chunk identifier number
	 */
	public final boolean localChunkExists(int chunkId)
	{
		return m_fileChunks.containsKey(chunkId) && !m_fileChunks.get(chunkId).isRemote();
	}

	/**
	 * @brief inserts a given chunk into this collection
	 * @param chunkId chunk identifier number
	 */
	public final long placeChunk(final Chunk chunk, final boolean localChunk)
	{
		int chunkId = chunk.getChunkId();
		long deltaSpace = 0;

		if (m_fileChunks.containsKey(chunkId))
		{
			final ChunkInformation chunkInformation = m_fileChunks.get(chunkId);

			if (chunkInformation.isRemote() && localChunk)
			{
				chunkInformation.setLocal();
				deltaSpace = chunkInformation.getLength();
				registerPeer(chunkId, Peer.getPeerId());
				m_size += deltaSpace;
			}
			else
			{
				return -1;
			}
		}
		else
		{
			m_fileChunks.put(chunkId, new ChunkInformation(chunk, localChunk));

			if (localChunk)
			{
				deltaSpace = chunk.getLength();
				registerPeer(chunkId, Peer.getPeerId());
				m_size += chunk.getLength();
			}
		}

		return deltaSpace;
	}

	public final String toString(final String fileId)
	{
		final StringBuilder sb = new StringBuilder();

		sb.append("+------------------------------------------------------------------+\n| ");
		sb.append(fileId);
		sb.append(" |\n+----------+------------------+------------------------------------+\n");
		sb.append("| ChunkId  | Length           | Degree  |\n");
		sb.append("+----------+------------------+---------+\n");
		m_fileChunks.forEach((chunkId, chunkInformation) -> {
			sb.append(chunkInformation.toString(chunkId));
		});
		sb.append("+----------+------------------+---------+\n");

		return sb.toString();
	}
}