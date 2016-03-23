package bs.protocol;

public class GetchunkMessage extends SimpleMessage
{
	public GetchunkMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId);
	}

	public GetchunkMessage(final String[] paramHeader)
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "GETCHUNK";
	}
}