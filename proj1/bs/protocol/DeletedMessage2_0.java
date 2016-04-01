package bs.protocol;

public class DeletedMessage2_0 extends Message
{
	public DeletedMessage2_0(final String fileId)
	{
		super(4, fileId, "2.0");
	}

	@Override
	public final String getType()
	{
		return "DELETED";
	}
}