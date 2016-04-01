package bs.protocol;

import bs.filesystem.Chunk;

public class PutchunkMessage extends PayloadMessage
{
	public PutchunkMessage(final Chunk paramChunk)
	{
		super(paramChunk, paramChunk.getReplicationDegree(), "1.0");
	}

	@Override
	public final String getType()
	{
		return "PUTCHUNK";
	}
}