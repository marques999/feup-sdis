package bs.filesystem;

import java.util.Date;
import bs.logging.ProgramException;

public class MissingChunkException extends ProgramException
{
	private static final long serialVersionUID = 7534263449573927317L;

	public MissingChunkException(final String fileId)
	{
		super(String.format("(%s) file %s not received entirely, some chunks are missing!\n", (new Date()).toString(), fileId));
	}
}