package bs.logging;

public class BadChunkException extends ProgramException
{
	private static final long serialVersionUID = 790445000889429821L;

	public BadChunkException(int chunkId)
	{
		super(String.format("chunk %s has invalid data!\n", chunkId));
	}
}