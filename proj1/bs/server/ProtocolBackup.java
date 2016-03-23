package bs.server;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import bs.ResponseThread;
import bs.filesystem.Chunk;
import bs.protocol.Message;
import bs.protocol.PutchunkMessage;
import bs.protocol.StoredMessage;

public class ProtocolBackup extends Protocol
{

	public ProtocolBackup(final InetAddress myAddress, int myPort)
	{
		super(myAddress, myPort);
	}

	public boolean sendPUTCHUNK(final Chunk cd)
	{
		return sendMessage(new PutchunkMessage(cd));
	}
	
	public ResponseThread sendSTORED(final String fileId, int chunkId)
	{
		return sendResponse(new StoredMessage(fileId, chunkId));
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