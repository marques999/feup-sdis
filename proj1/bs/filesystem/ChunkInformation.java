package bs.filesystem;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ChunkInformation implements Serializable
{
	private static final long serialVersionUID = 2317793689994719337L;

	public ChunkInformation(final Chunk paramChunk, final boolean paramLocal)
	{
		m_degree = paramChunk.getReplicationDegree();
		m_size = paramChunk.getLength();
		m_peers = new HashSet<Integer>();
		m_local = paramLocal;
	}

	public final String toString(int chunkId)
	{
		return String.format("| %8d | %10d bytes | %2d / %2d |\n", chunkId, m_size, getCount(), m_degree);
	}

	//-----------------------------------------------------

	private final int m_degree;

	public final int getReplicationDegree()
	{
		return m_degree;
	}

	//-----------------------------------------------------

	private final long m_size;

	public final long getLength()
	{
		return m_size;
	}

	//-----------------------------------------------------

	private final Set<Integer> m_peers;

	public final int getCount()
	{
		return m_peers.size();
	}

	public final void removePeer(int peerId)
	{
		m_peers.remove(peerId);
	}

	public final void registerPeer(int peerId)
	{
		m_peers.add(peerId);
	}
	
	private boolean m_local;
	
	public final void setLocal()
	{
		m_local = true;
	}
	
	public final void setRemote()
	{
		m_local = false;
	}
	public final boolean isRemote()
	{
		return !m_local;
	}
}