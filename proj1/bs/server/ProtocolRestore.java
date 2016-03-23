package bs.server;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import bs.filesystem.Chunk;
import bs.protocol.ChunkMessage;
import bs.protocol.Message;

public class ProtocolRestore extends Protocol
{
	public ProtocolRestore(final InetAddress myAddress, int myPort)
	{
		super(myAddress, myPort);
	}

	public boolean sendCHUNK(final Chunk paramChunk)
	{
		return sendMessage(new ChunkMessage(paramChunk));
	}
	
	private ConcurrentHashMap<String, ArrayList<Chunk>> m_notifications = new ConcurrentHashMap<>();
	
	public synchronized void subscribeChunks(final String fileId)
	{
		if (!m_notifications.containsKey(fileId))
		{
			m_notifications.put(fileId, new ArrayList<Chunk>());
		}
	}
	
	public synchronized void unsubscribeChunks(String fileId)
	{
		m_notifications.remove(fileId);
	}

	@Override
	protected Message processMessage(final DatagramPacket paramPacket) throws VersionMismatchException
	{
		return processPayloadMessage(paramPacket, "CHUNK");
	}

	@Override
	public String getName()
	{
		return "restore";
	}
}