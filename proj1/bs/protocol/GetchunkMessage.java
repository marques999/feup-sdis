package bs.protocol;

public class GetchunkMessage extends SimpleMessage
{
	public GetchunkMessage(final String fileId, int chunkId)
	{
		super(fileId, chunkId, "1.0");
	}

	@Override
	public final String getType()
	{
		return "GETCHUNK";
	}
}