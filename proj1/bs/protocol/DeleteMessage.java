package bs.protocol;

public class DeleteMessage extends Message
{
	public DeleteMessage(final String fileId)
	{
		super(4, fileId);
	}

	public DeleteMessage(final String[] paramHeader)
	{
		super(paramHeader);
	}

	@Override
	public final String getType()
	{
		return "DELETE";
	}
}