package bs.protocol;

public class StoredMessage extends SimpleMessage
{
	public StoredMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId);
	}

	public StoredMessage(final String[] paramHeader)
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "STORED";
	}
}