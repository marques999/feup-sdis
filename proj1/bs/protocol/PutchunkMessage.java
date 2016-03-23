package bs.protocol;

import bs.filesystem.Chunk;

public class PutchunkMessage extends PayloadMessage
{
	public PutchunkMessage(final Chunk paramChunk)
	{
		super(paramChunk, paramChunk.getReplicationDegree());
	}

	public PutchunkMessage(final String[] paramHeader, final byte[] paramBuffer)
	{
		super(paramHeader, paramBuffer);
	}

	@Override
	public final String getType()
	{
		return "PUTCHUNK";
	}
}