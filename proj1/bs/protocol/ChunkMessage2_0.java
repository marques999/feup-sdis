package bs.protocol;

public class ChunkMessage2_0 extends SimpleMessage
{
	public ChunkMessage2_0(final String fileId, int chunkId)
	{
		super(fileId, chunkId, "2.0");
	}

	@Override
	public final String getType()
	{
		return "CHUNK";
	}
}