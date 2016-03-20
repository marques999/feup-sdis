package bs;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ProtocolBackup extends Protocol
{
	public ProtocolBackup(final InetAddress myAddress, int myPort)
	{
		super(myAddress, myPort);
	}

	public boolean sendRequest(final FileChunk paramChunk)
	{
		return super.send(new PUTCHUNKMessage(paramChunk, paramChunk.getReplicationDegree()));
	}
	
	public void sendResponse(final String fileId, int chunkId)
	{
		super.sendResponse(new STOREDMessage(fileId, chunkId));
	}
	
	@Override
	protected Message processMessage(final DatagramPacket paramPacket) throws VersionMismatchException
	{
		return super.processPayloadMessage(paramPacket, "PUTCHUNK");
	}

	@Override
	public String getName()
	{
		return "backup";
	}
}