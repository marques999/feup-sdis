package bs;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Stack;

import bs.filesystem.Chunk;
import bs.logging.Logger;
import bs.protocol.GenericMessage;

public class RestoreService extends BaseService
{
	/**
	 * stores binary chunks for the files being restored, key = fileId
	 */
	private final HashMap<String, Stack<Chunk>> m_collection = new HashMap<>();
	
	/**
	 * mutex for dealing with concurrent accesses to the chunk collection hashmap
	 */
	private final Object m_collectionLock = new Object();
	
	/**
	 * stores received CHUNK messages from other peers, key = (fileId, chunkId)
	 */
	private final HashMap<Integer, Boolean> m_received = new HashMap<>();
	
	/**
	 * mutex for dealing with concurrent accesses to the received messages hashmap
	 */
	private final Object m_receivedLock = new Object();

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

		synchronized (m_collectionLock)
		{
			if (m_collection.containsKey(fileId))
			{
				registerChunk(paramMessage.generateChunk());
			}
			else
			{
				registerMessage(fileId, paramMessage.getChunkId());
			}
		}
	}
	
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
	 * @brief starts receiving binary chunks for the current file
	 * @param fileId unique identifier of the file being restored
	 */
	public final void startReceivingChunks(final String fileId)
	{
		synchronized (m_collectionLock)
		{
			if (!m_collection.containsKey(fileId))
			{
				m_collection.put(fileId, new Stack<Chunk>());
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
				m_collection.get(fileId).push(paramChunk);
				m_collectionLock.notifyAll();
			}
		}
	}
	
	public final boolean hasChunk(final Chunk paramChunk)
	{
		final String fileId = paramChunk.getFileId();
	
		synchronized (m_collectionLock)
		{
			if (!m_collection.containsKey(fileId))
			{
				return false;
			}

			final Stack<Chunk> chunkStack = m_collection.get(fileId);
			return !chunkStack.isEmpty() && chunkStack.contains(paramChunk);
		}
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

			final Stack<Chunk> chunkStack = m_collection.get(fileId);

			while (chunkStack.empty())
			{
				try
				{
					m_collectionLock.wait();
				}
				catch (InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}

			return chunkStack.pop();
		}
	}

	/**
	 * @brief stops receiving binary chunks for the current file
	 * @param fileId unique identifier of the file being restored
	 */
	public synchronized void stopReceivingChunks(final String fileId)
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