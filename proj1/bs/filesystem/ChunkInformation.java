package bs.filesystem;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ChunkInformation implements Serializable
{
	private static final long serialVersionUID = 2317793689994719337L;

	public ChunkInformation(final Chunk paramChunk, final boolean paramLocal)
	{
		localFile = paramLocal;
		chunkSize = paramChunk.getLength();
		remotePeers = new HashSet<Integer>();
		replicationDegree = paramChunk.getReplicationDegree();
	}

	public final String toString(int chunkId)
	{
		return String.format("| %8d | %10d bytes | %2d / %2d |\n", chunkId, chunkSize, getCount(), replicationDegree);
	}

	//-----------------------------------------------------

	private final int replicationDegree;

	public final int getReplicationDegree()
	{
		return replicationDegree;
	}

	//-----------------------------------------------------

	private final long chunkSize;

	public final long getLength()
	{
		return chunkSize;
	}

	//-----------------------------------------------------

	private final Set<Integer> remotePeers;

	public final int getCount()
	{
		return remotePeers.size();
	}

	public final Set<Integer> getPeers()
	{
		return remotePeers;
	}

	public final void removePeer(int peerId)
	{
		remotePeers.remove(peerId);
	}

	public final void registerPeer(int peerId)
	{
		remotePeers.add(peerId);
	}

	//-----------------------------------------------------

	private boolean localFile;

	public final void setLocal()
	{
		localFile = true;
	}

	public final boolean isTemporary()
	{
		return !localFile;
	}
}