package bs.protocol;

import bs.filesystem.Chunk;

public class ChunkMessage extends PayloadMessage
{
	public ChunkMessage(final Chunk paramChunk)
	{
		super(paramChunk, 0);
	}

	public ChunkMessage(final String[] paramHeader, final byte[] paramBuffer)
	{
		super(paramHeader, paramBuffer);
	}

	@Override
	public final String getType()
	{
		return "CHUNK";
	}
}