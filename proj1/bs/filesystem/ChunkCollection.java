package bs.filesystem;

import java.io.Serializable;

import java.util.Collection;
import java.util.HashMap;

import bs.misc.Pair;

public class ChunkCollection implements Serializable {

	private static final long serialVersionUID = -108274861401132235L;
	
	// -----------------------------------------------------
	
	public ChunkCollection() {
		m_fileChunks = new HashMap<Integer, ChunkInformation>();
		m_size = 0;
	}
	
	// -----------------------------------------------------
	
	private long m_size;
	
	public final long getSize() {
		return m_size;
	}
	
	// -----------------------------------------------------
	
	private HashMap<Integer, ChunkInformation> m_fileChunks;
	
	public final Collection<ChunkInformation> getChunks() {
		return m_fileChunks.values();
	}
	
	public final Integer[] getChunkIds() {
		return (Integer[]) m_fileChunks.keySet().toArray();
	}
	
	public final int getNumberChunks() {
		return m_fileChunks.size();
	}

	// -----------------------------------------------------
	
	public final Pair<Integer, Integer> findMostReplicated() {

		int mostReplicatedCount = Integer.MIN_VALUE;
		int mostReplicatedId = 0;

		for (int chunkId : m_fileChunks.keySet()) {
			
			int currentCount = m_fileChunks.get(chunkId).getCount();
		
			if (currentCount > mostReplicatedCount) {
				mostReplicatedCount = currentCount;
				mostReplicatedId = chunkId;
			}
		}
		
		return new Pair<Integer, Integer>(mostReplicatedId, mostReplicatedCount);
	}
	
	// -----------------------------------------------------

	public final ChunkInformation getChunk(int chunkId) {
		
		if (m_fileChunks.containsKey(chunkId)) {
			return m_fileChunks.get(chunkId);
		}

		return null;
	}
	
	/**
	 * @brief removes a chunk from this collection
	 * @param chunkId chunk identifier number
	 */
	public final long removeChunk(int chunkId) {
		
		if (m_fileChunks.containsKey(chunkId)) {
			long chunkSize = m_fileChunks.remove(chunkId).getLength();	
			m_size -= chunkSize;		
			return chunkSize;
		}
		
		return -1;
	}

	/**
	 * @brief decreases the replication count of a chunk
	 * @param chunkId chunk identifier number
	 */
	public final void removePeer(int chunkId, int peerId) {
		
		if (m_fileChunks.containsKey(chunkId)) {
			m_fileChunks.get(chunkId).removePeer(peerId);
		}
	}
	
	/**
	 * @brief increases the replication count of a chunk
	 * @param chunkId chunk identifier number
	 */
	public final void registerPeer(int chunkId, int peerId) {
		
		if (m_fileChunks.containsKey(chunkId)) {
			m_fileChunks.get(chunkId).registerPeer(peerId);	
		}
	}
	
	/**
	 * @brief checks if a given chunk exists
	 * @param chunkId chunk identifier number
	 */
	public final boolean chunkExists(int chunkId) {
		return m_fileChunks.containsKey(chunkId);
	}
	
	/**
	 * @brief inserts a given chunk into this collection
	 * @param chunkId chunk identifier number
	 */
	public final boolean putChunk(final Chunk chunk) {
		
		if (m_fileChunks.containsKey(chunk.getChunkId())) {
			return false;
		}
		
		m_fileChunks.put(chunk.getChunkId(), new ChunkInformation(chunk));
		m_size += chunk.getLength();
		
		return true;
	}


	public final String toString(final String fileId) {
		
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