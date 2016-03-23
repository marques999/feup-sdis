package bs.filesystem;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ChunkInformation implements Serializable {	
	
	private static final long serialVersionUID = 2317793689994719337L;
	
	//-----------------------------------------------------
	
	public ChunkInformation(final Chunk paramChunk) {
		m_degree = paramChunk.getReplicationDegree();
		m_size = paramChunk.getLength();
		m_peers = new HashSet<Integer>();
	}
	
	public String toString(int chunkId) {
		return String.format("| %8d | %10d bytes | %2d / %2d |\n", chunkId, m_size, getCount(), m_degree);
	}
	
	//-----------------------------------------------------
	
	private final int m_degree;
	
	public final int getReplicationDegree() {
		return m_degree;
	}
	
	//-----------------------------------------------------
	
	private final long m_size;
	
	public final long getLength() {
		return m_size;
	}
	
	//-----------------------------------------------------
	
	private Set<Integer> m_peers;
	
	public final int getCount() {
		return m_peers.size();
	}

	public void removePeer(int peerId) {
		m_peers.remove(peerId);
	}
	
	public void registerPeer(int peerId) {	
		m_peers.add(peerId);
	}
}