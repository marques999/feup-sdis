package bs;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import bs.filesystem.Chunk;
import bs.protocol.GenericMessage;

public class RestoreService extends BaseService
{
	/**
	 * @brief default constructor for 'RestoreService' class
	 * @param paramAddress address of the multicast restore channel
	 * @param paramPort port of the multicast restore channel
	 */
	public RestoreService(final InetAddress paramAddress, int paramPort)
	{
		super("restore service", paramAddress, paramPort);
	}

	/**
	 * @brief verifies and processes a message sent to the restore service
	 * @param paramMessage contents of the received message
	 * @param hasPayload 'true' if this message should contain binary data attached
	 */
	@Override
	protected void processMessage(final GenericMessage paramMessage, final DatagramPacket paramPacket, boolean hasPayload)
	{
		if (paramMessage.hasEnhancements() && !Peer.enhancementsEnabled())
		{
			return;
		}

		if (paramMessage.getType().equals("CHUNK"))
		{
			if (hasPayload)
			{
				processCHUNK(paramMessage);
			}
			else
			{
				Logger.logError("restore service received empty CHUNK message!");
			}
		}
		else
		{
			Logger.logError("restore service received an invalid message!");
		}
	}

	/**
	 * @brief processes a CHUNK message sent to the restore service
	 * @param paramMessage contents of the received message
	 */
	private void processCHUNK(final GenericMessage paramMessage)
	{
		final String fileId = paramMessage.getFileId();

		if (paramMessage.hasEnhancements())
		{
			registerMessage(fileId, paramMessage.getChunkId());
		}
		else if (m_collection.containsKey(fileId))
		{
			registerChunk(paramMessage.generateChunk());
		}
		else
		{
			registerMessage(fileId, paramMessage.getChunkId());
		}
	}

	/**
	 * stores received CHUNK messages from other peers, key = (fileId, chunkId)
	 */
	private final HashMap<Integer, Boolean> m_received = new HashMap<>();

	/**
	 * mutex for dealing with concurrent accesses to the received messages hashmap
	 */
	private final Object m_receivedLock = new Object();

	/**
	 * @brief starts listening for CHUNK messages for the current chunk
	 * @param fileId unique identifier of the file being restored
	 * @param chunkId unique identifier of the chunk being restored
	 */
	public final void subscribeMessages(final String fileId, int chunkId)
	{
		int generatedHash = calculateHash(fileId, chunkId);

		synchronized (m_receivedLock)
		{
			m_received.put(generatedHash, false);
		}
	}

	/**
	 * @brief registers a CHUNK message sent to the restore service
	 * @param fileId unique identifier of the file being restored
	 * @param chunkId unique identifier of the chunk being restored
	 */
	public final void registerMessage(final String fileId, int chunkId)
	{
		int generatedHash = calculateHash(fileId, chunkId);

		synchronized (m_receivedLock)
		{
			m_received.put(generatedHash, true);
		}
	}

	/**
	 * @brief stops listening for CHUNK messages for the current chunk
	 * @param fileId unique identifier of the file being restored
	 * @param chunkId unique identifier of the chunk being restored
	 */
	public final boolean unsubscribeMessages(final String fileId, int chunkId)
	{
		int generatedHash = calculateHash(fileId, chunkId);

		synchronized (m_receivedLock)
		{
			if (m_received.containsKey(generatedHash))
			{
				return m_received.remove(generatedHash);
			}
		}

		return false;
	}

	/**
	 * stores binary chunks for the files being restored, key = fileId
	 */
	private final HashMap<String, LinkedBlockingQueue<Chunk>> m_collection = new HashMap<>();

	/**
	 * mutex for dealing with concurrent accesses to the chunk collection hashmap
	 */
	private final Object m_collectionLock = new Object();

	/**
	 * @brief starts receiving binary chunks for the current file
	 * @param fileId unique identifier of the file being restored
	 */
	public final void startReceivingChunks(final String fileId)
	{
		synchronized (m_collectionLock)
		{
			if (!m_collection.containsKey(fileId))
			{
				m_collection.put(fileId, new LinkedBlockingQueue<Chunk>());
			}
		}
	}

	/**
	 * @brief places a binary chunk on the received chunks array
	 * @param paramChunk binary chunk of the file being restored
	 */
	public final void registerChunk(final Chunk paramChunk)
	{
		final String fileId = paramChunk.getFileId();

		synchronized (m_collectionLock)
		{
			if (m_collection.containsKey(fileId))
			{
				m_collection.get(fileId).offer(paramChunk);
			}
		}
	}

	public final boolean hasReceivedChunk(final Chunk paramChunk)
	{
		final String fileId = paramChunk.getFileId();

		synchronized (m_collectionLock)
		{
			if (m_collection.containsKey(fileId))
			{
				return m_collection.get(fileId).contains(paramChunk);
			}
		}

		return false;
	}

	/**
	 * @brief retrieves a binary chunk from the received chunks array
	 * @param fileId unique identifier of the file being restored
	 * @param chunkId unique identifier of the chunk being restored
	 */
	public final Chunk retrieveChunk(final String fileId, int chunkId)
	{
		synchronized (m_collectionLock)
		{
			if (!m_collection.containsKey(fileId))
			{
				return null;
			}
		}

		try
		{
			return m_collection.get(fileId).poll(2 * PeerGlobals.initialWaitingTime, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * @brief stops receiving binary chunks for the current file
	 * @param fileId unique identifier of the file being restored
	 */
	public final void stopReceivingChunks(final String fileId)
	{
		synchronized (m_collectionLock)
		{
			if (m_collection.containsKey(fileId))
			{
				m_collection.remove(fileId);
			}
		}
	}
}