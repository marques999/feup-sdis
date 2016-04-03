package bs.filesystem;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RemoteCollection implements Serializable
{
	private static final long serialVersionUID = -9031448440110346390L;

	public RemoteCollection(int paramDegree)
	{
		replicationDegree = paramDegree;
		remotePeers = new HashMap<Integer, Set<Integer>>();
	}
	
	private final HashMap<Integer, Set<Integer>> remotePeers;
	
	public final Set<Integer> getPeers()
	{
		final Set<Integer> myPeers = new HashSet<Integer>();

		remotePeers.forEach((chunkId, peerSet) ->
		{
			myPeers.addAll(peerSet);
		});
		
		return myPeers;
	}
	
	private final int replicationDegree;
	
	public final int getCount()
	{
		return remotePeers.size();
	}

	public final boolean acceptsChunks()
	{
		for (Set<Integer> myConfirmations : remotePeers.values())
		{
			if (myConfirmations.size() < replicationDegree)
			{
				return true;
			}
		}
		
		return false;
	}

	public final void registerPeers(int chunkId, final Set<Integer> peerSet)
	{
		if (!remotePeers.containsKey(chunkId))
		{
			remotePeers.put(chunkId, new HashSet<>());
		}

		remotePeers.get(chunkId).addAll(peerSet);
	}
	
	public final void removePeer(int chunkId, int peerId)
	{
		if (remotePeers.containsKey(chunkId))
		{
			remotePeers.get(chunkId).remove(peerId);
		}
	}
	
	public final void removePeer(int peerId)
	{
		for (Set<Integer> thisChunk : remotePeers.values())
		{
			thisChunk.remove(peerId);
		}
	}
}