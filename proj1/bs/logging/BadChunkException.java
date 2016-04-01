package bs.logging;

import java.util.Date;

import bs.filesystem.Chunk;

public class BadChunkException extends ProgramException
{
	private static final long serialVersionUID = 790445000889429821L;

	public BadChunkException(final Chunk paramChunk)
	{
		super(String.format("(%s) chunk %s from file %s has invalid data!\n", (new Date()).toString(), paramChunk.getChunkId(), paramChunk.getFileId()));
	}
}