package bs.filesystem;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RemoteCollection implements Serializable
{
	private static final long serialVersionUID = -9031448440110346390L;

	public RemoteCollection(int replicationDegree)
	{
		m_degree = replicationDegree;
		m_peers = new HashMap<Integer, Set<Integer>>();
	}
	
	private final HashMap<Integer, Set<Integer>> m_peers;
	
	public final Set<Integer> getPeers()
	{
		final Set<Integer> remotePeers = new HashSet<Integer>();

		m_peers.forEach((chunkId, peerSet) ->
		{
			remotePeers.addAll(peerSet);
		});
		
		return remotePeers;
	}
	
	public final int getCount()
	{
		return m_peers.size();
	}
	
	private final int m_degree;
	
	public final boolean acceptsChunks()
	{
		for (Set<Integer> myConfirmations : m_peers.values())
		{
			if (myConfirmations.size() < m_degree)
			{
				return true;
			}
		}
		
		return false;
	}

	public final void registerPeers(int chunkId, final Set<Integer> peerSet)
	{
		if (!m_peers.containsKey(chunkId))
		{
			m_peers.put(chunkId, new HashSet<>());
		}

		m_peers.get(chunkId).addAll(peerSet);
	}
	
	public final void removePeer(int chunkId, int peerId)
	{
		if (m_peers.containsKey(chunkId))
		{
			m_peers.get(chunkId).remove(peerId);
		}
	}
	
	public final void removePeer(int peerId)
	{
		for (Set<Integer> thisChunk : m_peers.values())
		{
			thisChunk.remove(peerId);
		}
	}
}