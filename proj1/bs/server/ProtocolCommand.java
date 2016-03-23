package bs.server;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import bs.ResponseThread;
import bs.filesystem.Chunk;
import bs.protocol.DeleteMessage;
import bs.protocol.GetchunkMessage;
import bs.protocol.Message;
import bs.protocol.RemovedMessage;
import bs.protocol.StoredMessage;

public class ProtocolCommand extends Protocol
{
	public ProtocolCommand(final InetAddress myAddress, int myPort)
	{
		super(myAddress, myPort);
	}

	@Override
	public final String getName()
	{
		return "command";
	}
	
	@Override
	protected final Message processMessage(final DatagramPacket paramPacket) throws VersionMismatchException
	{
		return processSimpleMessage(paramPacket, null);
	}
	
	//-----------------------------------------------------
	
	private ConcurrentHashMap<Integer, Set<Integer>> m_notifications = new ConcurrentHashMap<>();
	
	public synchronized void subscribeNotifications(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		if (!m_notifications.containsKey(generatedHash))
		{
			m_notifications.put(generatedHash, new HashSet<Integer>());
		}
	}

	public synchronized void clearNotifications(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		if (m_notifications.containsKey(generatedHash))
		{
			m_notifications.get(generatedHash).clear();
		}
	}

	public synchronized int getConfirmations(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());
		
		if (m_notifications.containsKey(generatedHash))
		{
			return m_notifications.get(generatedHash).size();
		}
		
		return 0;
	}

	public synchronized void unsubscribeNotifications(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		if (m_notifications.containsKey(generatedHash))
		{
			m_notifications.remove(generatedHash);
		}
	}
	
	public synchronized void registerConfirmation(final String fileId, int chunkId, int peerId)
	{
		int generatedHash = calculateHash(fileId, chunkId);
	
		if (m_notifications.containsKey(generatedHash))
		{
			m_notifications.get(generatedHash).add(peerId);
		}
	}
	
	// -----------------------------------------------------
	
	private Message generateDELETE(final String fileId)
	{
		return new DeleteMessage(fileId);
	}
	
	private Message generateGETCHUNK(final String fileId, int chunkId)
	{
		return new GetchunkMessage(fileId, chunkId);
	}
	
	private Message generateREMOVED(final Chunk cd)
	{
		return new RemovedMessage(cd.getFileId(), cd.getChunkId());
	}
	
	private Message generateSTORED(final Chunk cd)
	{
		return new StoredMessage(cd.getFileId(), cd.getChunkId());
	}
	
	//-----------------------------------------------------
	
	public boolean sendDELETE(final String fileId)
	{
		return sendMessage(generateDELETE(fileId));
	}
	
	public ResponseThread sendGETCHUNK(final String fileId, int chunkId)
	{
		return sendResponse(generateGETCHUNK(fileId, chunkId));
	}
	
	public ResponseThread sendREMOVED(final Chunk cd)
	{
		return sendResponse(generateREMOVED(cd));
	}

	public ResponseThread sendSTORED(final Chunk cd)
	{
		return sendResponse(generateSTORED(cd));
	}
}