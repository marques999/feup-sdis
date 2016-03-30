package bs.protocol;

public class GetchunkMessage2_0 extends ExtraMessage
{
	public GetchunkMessage2_0(final String fileId, int chunkId)
	{
		super(fileId, chunkId);
	}

	@Override
	public final String getType()
	{
		return "GETCHUNK";
	}
}