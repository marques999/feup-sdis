package bs.protocol;

import bs.Peer;

public class DeleteMessage extends Message
{
	public DeleteMessage(final String fileId)
	{
		super(4, fileId, Peer.getVersion());
	}

	@Override
	public final String getType()
	{
		return "DELETE";
	}
}