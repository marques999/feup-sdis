package bs.protocol;

public class DeleteMessage extends Message
{
	public DeleteMessage(final String fileId)
	{
		super(4, fileId, "1.0");
	}

	@Override
	public final String getType()
	{
		return "DELETE";
	}
}