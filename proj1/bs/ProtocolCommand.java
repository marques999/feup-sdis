package bs;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ProtocolCommand extends Protocol
{
	public ProtocolCommand(final InetAddress myAddress, int myPort)
	{
		super(myAddress, myPort);
	}

	protected boolean validResponse(final String paramCommand)
	{
		return paramCommand.equals("REMOVED");
	}

	protected boolean validRequest(final String paramCommand)
	{
		return paramCommand.equals("DELETE");
	}

	@Override
	public String getName()
	{
		return "command";
	}

	@Override
	protected Message processMessage(final DatagramPacket paramPacket) throws VersionMismatchException
	{
		return processSimpleMessage(paramPacket, null);
	}

	public void sendSTOREDResponse(final PayloadMessage gm)
	{
		super.sendResponse(new STOREDMessage(gm.getFileId(), gm.getChunkId()));
	}
	
	public void sendREMOVEDResponse(final SimpleMessage gm)
	{
		super.sendResponse(new REMOVEDMessage(gm.getFileId(), gm.getChunkId()));
	}

	@Override
	public void sendResponse(String fileId, int chunkId)
	{
		// TODO Auto-generated method stub
		
	}
}