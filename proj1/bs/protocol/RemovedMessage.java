package bs.protocol;

public class RemovedMessage extends SimpleMessage
{
	public RemovedMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId, "1.0");
	}

	@Override
	public final String getType()
	{
		return "REMOVED";
	}
}