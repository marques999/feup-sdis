package bs.protocol;

public class RemovedMessage extends SimpleMessage
{
	public RemovedMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId);
	}

	public RemovedMessage(final String[] paramHeader)
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "REMOVED";
	}
}