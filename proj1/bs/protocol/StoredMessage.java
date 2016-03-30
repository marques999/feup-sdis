package bs.protocol;

public class StoredMessage extends SimpleMessage
{
	public StoredMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId, "1.0");
	}

	@Override
	public final String getType()
	{
		return "STORED";
	}
}