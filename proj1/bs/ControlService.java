package bs;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import bs.filesystem.Chunk;
import bs.actions.BackupHelper;
import bs.logging.Logger;
import bs.protocol.GenericMessage;

public class ControlService extends BaseService
{
	/**
	 * stores received STORED messages from other peers, key = (fileId, chunkId)
	 */
	private final HashMap<Integer, Set<Integer>> confirmationsArray = new HashMap<>();
	
	/**
	 * mutex for dealing with concurrent accesses to the confirmations array
	 */
	private final Object confirmationsLock = new Object();

	/**
	 * @brief default constructor for 'ControlService' class
	 * @param paramAddress address of the multicast control channel
	 * @param paramPort port of the multicast control channel
	 */
	public ControlService(final InetAddress paramAddress, int paramPort)
	{
		super("control service", paramAddress, paramPort);
	}
	
	/**
	 * @brief verifies and processes a message sent to the control service
	 * @param paramMessage contents of the received message
	 * @param hasPayload 'true' if this message should contain binary data attached
	 */
	@Override
	protected void processMessage(final GenericMessage paramMessage, final DatagramPacket paramPacket, boolean hasPayload)
	{
		if (hasPayload)
		{
			Logger.logError("control service received an invalid message!");
		}	
		else
		{
			if (paramMessage.getType().equals("STORED"))
			{
				processSTORED(paramMessage);
			}
			else if (paramMessage.getType().equals("DELETE"))
			{
				processDELETE(paramMessage);
			}
			else if (paramMessage.getType().equals("GETCHUNK"))
			{
				processGETCHUNK(paramMessage, paramPacket);		
			}
			else if (paramMessage.getType().equals("REMOVED"))
			{
				processREMOVED(paramMessage);
			}
			else
			{
				Logger.logError("control service received an invalid message!");
			}
		}
	}

	/**
	 * @brief processes a STORED message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	private void processSTORED(final GenericMessage paramMessage)
	{
		int peerId = paramMessage.getPeerId();
		int chunkId = paramMessage.getChunkId();

		/*
		 * if this peer requested the backup, update the number of received STOREDs (replication degree).
		 */
		registerConfirmation(paramMessage.getFileId(), chunkId, peerId);
		
		/*
		 * if this peer is backing up this chunk, save the other peers which are also backing it up.
		 */
		bsdbInstance.registerPeer(paramMessage.getFileId(), chunkId, peerId);
	}
	
	public void processREMOVED(final GenericMessage paramMessage)
	{	
		final String fileId = paramMessage.getFileId();
		final int chunkId = paramMessage.getChunkId();

		bsdbInstance.removePeer(fileId, chunkId, paramMessage.getPeerId());

		if (!bsdbInstance.hasLocalChunk(fileId, chunkId))
		{
			return;
		}
		
		int currentReplicationDegree = bsdbInstance.getPeerCount(fileId, chunkId);
		int desiredReplicationDegree = bsdbInstance.getReplicationDegree(fileId, chunkId);
		
		final BackupService backupService = BackupSystem.getBackupService();

		if (currentReplicationDegree < desiredReplicationDegree)
		{
			backupService.subscribePutchunk(fileId, chunkId);

			try
			{
				Thread.sleep(generateBackoff());
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);

			if (numberPutchunkMessages == 0)
			{
				final Chunk myChunk = fmInstance.readChunk(fileId, chunkId);
				
				if (myChunk != null)
				{
					new BackupHelper(myChunk).start();
				}
				else
				{
					Logger.logDebug("could not read chunk with id=" + chunkId + " from existing archive!");
				}
			}
		}
	}
	
	/**
	 * @brief processes a GETCHUNK message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	public void processGETCHUNK(final GenericMessage paramMessage, final DatagramPacket paramPacket)
	{
		final RestoreService restoreService = BackupSystem.getRestoreService();
		final String fileId = paramMessage.getFileId();
		
		int chunkId = paramMessage.getChunkId();
		boolean enableEnhancement = BackupSystem.enhancementsEnabled();
		
		if (bsdbInstance.hasLocalChunk(fileId, chunkId))
		{
			restoreService.subscribeMessages(fileId, chunkId);

			try
			{
				Thread.sleep(generateBackoff());
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			if (restoreService.unsubscribeMessages(fileId, chunkId))
			{
				Logger.logDebug("another peer has already sent chunk with id=" + chunkId);
			}
			else
			{
				final Chunk myChunk = fmInstance.readChunk(fileId, chunkId);
				
				if (myChunk != null)
				{
					if (enableEnhancement)
					{
						BackupSystem.sendEnhancedCHUNK(myChunk, paramPacket.getAddress(), 20000 + paramMessage.getPeerId());
					}
					else
					{
						BackupSystem.sendCHUNK(myChunk);
					}
				}
				else
				{
					Logger.logDebug("could not read chunk with id=" + chunkId + " from existing archive!");
				}
			}
		}
		else
		{
			Logger.logDebug("requested chunk with id=" + chunkId + " not backed up by this peer!");
		}
	}
	
	/**
	 * @brief processes a DELETE message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	public void processDELETE(final GenericMessage paramMessage)
	{
		bsdbInstance.removeFile(paramMessage.getFileId());
	}
	
	/**
	 * @brief starts listening for peer confirmations for the current chunk
	 * @param paramChunk binary chunk of the file being backed up
	 */
	public void subscribeConfirmations(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (confirmationsLock)
		{
			if (!confirmationsArray.containsKey(generatedHash))
			{
				confirmationsArray.put(generatedHash, new HashSet<Integer>());
			}
		}
	}

	/**
	 * @brief resets number of received confirmations for the current chunk
	 * @param paramChunk binary chunk of the file being backed up
	 */
	public void resetPeerConfirmations(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (confirmationsLock)
		{
			if (confirmationsArray.containsKey(generatedHash))
			{
				confirmationsArray.get(generatedHash).clear();
			}
		}
	}

	/**
	 * @brief retrieves number of received confirmations for the current chunk
	 * @param paramChunk binary chunk of the file being backed up
	 */
	public int getPeerConfirmations(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (confirmationsLock)
		{
			if (confirmationsArray.containsKey(generatedHash))
			{
				return confirmationsArray.get(generatedHash).size();
			}
		}

		return 0;
	}

	/**
	 * @brief stops listening for peer confirmations for the current chunk
	 * @param paramChunk binary chunk of the file being backed up
	 */
	public void unsubscribeConfirmations(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (confirmationsLock)
		{
			if (confirmationsArray.containsKey(generatedHash))
			{
				confirmationsArray.remove(generatedHash);
			}
		}
	}

	/**
	 * @brief registers a STORED message sent to the control service
	 * @param fileId unique identifier of the file being backed up
	 * @param chunkId unique identifier of the chunk being backed up
	 * @param peerId unique identifier of the peer sending the message
	 */
	public void registerConfirmation(final String fileId, int chunkId, int peerId)
	{
		int generatedHash = calculateHash(fileId, chunkId);

		synchronized (confirmationsLock)
		{
			if (confirmationsArray.containsKey(generatedHash))
			{
				confirmationsArray.get(generatedHash).add(peerId);
			}
		}
	}
}