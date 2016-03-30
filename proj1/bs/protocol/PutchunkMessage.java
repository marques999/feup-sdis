package bs.protocol;

import bs.Peer;
import bs.filesystem.Chunk;

public class PutchunkMessage extends PayloadMessage
{
	public PutchunkMessage(final Chunk paramChunk)
	{
		super(paramChunk, paramChunk.getReplicationDegree(), Peer.getVersion());
	}

	@Override
	public final String getType()
	{
		return "PUTCHUNK";
	}
}