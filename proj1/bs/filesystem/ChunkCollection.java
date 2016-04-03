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
		fileChunks = new HashMap<Integer, ChunkInformation>();
		fileSize = 0;
	}

	//-----------------------------------------------------

	private long fileSize;

	public final long getSize()
	{
		return fileSize;
	}

	//-----------------------------------------------------

	private final HashMap<Integer, ChunkInformation> fileChunks;

	public final HashMap<Integer, ChunkInformation> getLocalChunks()
	{
		return fileChunks;
	}

	public final Integer[] getChunkIds()
	{
		if (fileChunks.isEmpty())
		{
			return new Integer[] {};
		}

		int i = 0;

		final Integer[] chunkIds = new Integer[fileChunks.size()];
		final Set<Integer> chunkSet = fileChunks.keySet();

		for (final Integer chunkId : chunkSet)
		{
			if (!fileChunks.get(chunkId).isTemporary())
			{
				chunkIds[i++] = chunkId;
			}
		}

		return chunkIds;
	}

	public final int getNumberChunks()
	{
		return fileChunks.size();
	}

	public final boolean isEmpty()
	{
		return fileChunks.isEmpty();
	}

	//-----------------------------------------------------

	public final ChunkInformation getChunkInformation(int chunkId)
	{
		if (fileChunks.containsKey(chunkId))
		{
			return fileChunks.get(chunkId);
		}

		return null;
	}

	public final long removeLocalChunk(int chunkId)
	{
		long chunkSize = 0;

		if (fileChunks.containsKey(chunkId))
		{
			final ChunkInformation chunkInformation = fileChunks.get(chunkId);

			if (chunkInformation.isTemporary())
			{
				fileChunks.remove(chunkId);
			}
			else
			{
				chunkSize = fileChunks.remove(chunkId).getLength();
				fileSize -= chunkSize;
			}
		}

		return chunkSize;
	}

	public final boolean removePeer(int chunkId, int peerId)
	{
		if (!fileChunks.containsKey(chunkId))
		{
			return false;
		}
		
		fileChunks.get(chunkId).removePeer(peerId);
		
		return true;
	}

	public final boolean registerPeer(int chunkId, int peerId)
	{
		if (!fileChunks.containsKey(chunkId))
		{
			return false;
		}
		
		fileChunks.get(chunkId).registerPeer(peerId);
		
		return true;
	}

	public final boolean localChunkExists(int chunkId)
	{
		return fileChunks.containsKey(chunkId) && !fileChunks.get(chunkId).isTemporary();
	}

	public final long placeChunk(final Chunk chunk, final boolean localChunk)
	{
		int chunkId = chunk.getChunkId();
		long deltaSpace = 0;

		if (fileChunks.containsKey(chunkId))
		{
			final ChunkInformation chunkInformation = fileChunks.get(chunkId);

			if (chunkInformation.isTemporary() && localChunk)
			{
				chunkInformation.setLocal();
				deltaSpace = chunkInformation.getLength();
				registerPeer(chunkId, Peer.getPeerId());
				fileSize += deltaSpace;
			}
			else
			{
				return -1;
			}
		}
		else
		{
			fileChunks.put(chunkId, new ChunkInformation(chunk, localChunk));

			if (localChunk)
			{
				deltaSpace = chunk.getLength();
				registerPeer(chunkId, Peer.getPeerId());
				fileSize += chunk.getLength();
			}
		}

		return deltaSpace;
	}

	public final String toString(final String fileId)
	{
		final StringBuilder sb = new StringBuilder();
		
		if (fileChunks.size() > 0)
		{
			sb.append("+------------------------------------------------------------------+\n| ");
			sb.append(fileId);
			sb.append(" |\n+----------+------------------+------------------------------------+\n");
			sb.append("| ChunkId  | Length           | Degree  |\n");
			sb.append("+----------+------------------+---------+\n");
			fileChunks.forEach((chunkId, chunkInformation) -> {
				sb.append(chunkInformation.toString(chunkId));
			});
			sb.append("+----------+------------------+---------+\n");
		}

		return sb.toString();
	}
}