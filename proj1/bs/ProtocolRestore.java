package bs;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class ProtocolRestore extends Protocol
{
	public ProtocolRestore(final InetAddress myAddress, int myPort)
	{
		super(myAddress, myPort);
	}

	public boolean sendMessage(final FileChunk paramChunk)
	{
		return send(new CHUNKMessage(paramChunk));
	}

	@Override
	protected Message processMessage(final DatagramPacket paramPacket) throws VersionMismatchException
	{
		return processPayloadMessage(paramPacket, "GETCHUNK");
	}

	@Override
	public String getName()
	{
		return "restore";
	}

	@Override
	public void sendResponse(String fileId, int chunkId)
	{
	}
}