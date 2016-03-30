package bs.protocol;

import bs.filesystem.Chunk;

public class ChunkMessage extends PayloadMessage
{
	public ChunkMessage(final Chunk paramChunk)
	{
		super(paramChunk, 0, "1.0");
	}

	@Override
	public final String getType()
	{
		return "CHUNK";
	}
}