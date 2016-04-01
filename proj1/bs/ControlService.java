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
		if (paramMessage.hasEnhancements() && !Peer.enhancementsEnabled())
		{
			return;
		}

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
			else if (paramMessage.getType().equals("GETCHUNK"))
			{
				processGETCHUNK(paramMessage, paramPacket);
			}
			else if (paramMessage.getType().equals("REMOVED"))
			{
				processREMOVED(paramMessage);
			}
			else if (paramMessage.getType().equals("DELETE"))
			{
				processDELETE(paramMessage);
			}
			else if (paramMessage.getType().equals("DELETED"))
			{
				processDELETED(paramMessage);
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
		registerConfirmation(paramMessage.getFileId(), paramMessage.getChunkId(), paramMessage.getPeerId());
		bsdbInstance.registerPeer(paramMessage.getFileId(), paramMessage.getChunkId(), paramMessage.getPeerId());
	}
	
	/**
	 * @brief processes a DELETED message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	private void processDELETED(final GenericMessage paramMessage)
	{
		bsdbInstance.removeRemotePeer(paramMessage.getFileId(), paramMessage.getPeerId());
	}

	/**
	 * @brief processes a REMOVED message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	public void processREMOVED(final GenericMessage paramMessage)
	{
		final String fileId = paramMessage.getFileId();
		final int chunkId = paramMessage.getChunkId();
		final int peerId = paramMessage.getPeerId();

		//-------------------------------------------------
		// CHECK IF PEER IS CURRENTLY BACKING UP THIS CHUNK
		//-------------------------------------------------

		bsdbInstance.removeRemotePeer(fileId, chunkId, peerId);
		
		if (!bsdbInstance.hasLocalChunk(fileId, chunkId))
		{
			return;
		}

		//--------------------------------------------------
		// UPDATE LOCAL COUNT OF PEERS BACKING UP THIS CHUNK
		//--------------------------------------------------

		bsdbInstance.removePeer(fileId, chunkId, peerId);
	
		int currentReplicationDegree = bsdbInstance.getPeerCount(fileId, chunkId);
		int desiredReplicationDegree = bsdbInstance.getReplicationDegree(fileId, chunkId);

		final BackupService backupService = Peer.getBackupService();

		//---------------------------------------------------------
		// IF PEER COUNT DROPS BELOW THE DESIRED REPLICATION DEGREE
		//---------------------------------------------------------

		if (currentReplicationDegree < desiredReplicationDegree)
		{
			//-----------------------------------------------------
			// START LISTENING FOR PUTCHUNK MESSAGES FOR THIS CHUNK
			//-----------------------------------------------------

			Logger.logWarning("replication degree is lower than desired, attempting to fix...");
			backupService.subscribePutchunk(fileId, chunkId);

			//---------------------------------------------------------------------
			// WAIT FOR A RANDOM INTERVAL UNIFORMLY DISTRIBUTED BETWEEN 0 AND 400MS
			//---------------------------------------------------------------------

			try
			{
				Thread.sleep(generateBackoff());
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			//-----------------------------------------------------------------------
			// STOP LISTENING FOR PUTCHUNK MESSAGES FOR THIS CHUNK AND RETRIEVE COUNT
			//-----------------------------------------------------------------------

			int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);

			//------------------------------------------------------
			// IF A PUTCHUNK MESSAGE HAS NOT BEEN RECEIVED MEANWHILE
			//------------------------------------------------------

			if (numberPutchunkMessages == 0)
			{
				final Chunk myChunk = fmInstance.readChunk(fileId, chunkId);
				
				Logger.logWarning("no putchunk messages received, starting chunk backup!");

				//-------------------------------
				// START CHUNK BACKUP SUBPROTOCOL
				//-------------------------------

				if (myChunk != null)
				{
					new BackupHelper(myChunk, true).start();
				}
				else
				{
					Logger.logError("could not read chunk " + chunkId + " from existing archive!");
				}
			}
			else
			{
				Logger.logDebug("received " + numberPutchunkMessages + " \"putchunk\" messages, moving on...");
			}
		}
	}

	/**
	 * @brief processes a GETCHUNK message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	public void processGETCHUNK(final GenericMessage paramMessage, final DatagramPacket paramPacket)
	{
		final RestoreService restoreService = Peer.getRestoreService();
		final String fileId = paramMessage.getFileId();

		int chunkId = paramMessage.getChunkId();

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
				Logger.logDebug("another peer has already sent chunk " + chunkId);
			}
			else
			{
				final Chunk myChunk = fmInstance.readChunk(fileId, chunkId);

				if (myChunk != null)
				{
					if (paramMessage.hasEnhancements())
					{
						Peer.startUnicast();
						Peer.sendMulticastCHUNK2_0(myChunk);
						Peer.sendUnicastCHUNK2_0(myChunk, paramPacket.getAddress(), paramMessage.getPort());
					}
					else
					{
						Peer.sendCHUNK(myChunk);
					}
				}
				else
				{
					Logger.logDebug("could not read chunk " + chunkId + " from existing archive!");
				}
			}
		}
		else
		{
			Logger.logDebug("requested chunk " + chunkId + " not backed up by this peer!");
		}
	}

	/**
	 * @brief processes a DELETE message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	public void processDELETE(final GenericMessage paramMessage)
	{
		if (bsdbInstance.removeChunks(paramMessage.getFileId()))
		{
			if (paramMessage.hasEnhancements())
			{
				Peer.sendDELETED2_0(paramMessage.getFileId());
			}
		}
	}

	/**
	 * stores received STORED messages from other peers, key = (fileId, chunkId)
	 */
	private final HashMap<Integer, Set<Integer>> confirmationsArray = new HashMap<>();

	/**
	 * mutex for dealing with concurrent accesses to the confirmations array
	 */
	private final Object confirmationsLock = new Object();

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

	public void setPeerConfirmations(final Chunk paramChunk, final Set<Integer> paramPeers)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (confirmationsLock)
		{
			confirmationsArray.put(generatedHash,paramPeers);
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
	public Set<Integer> getPeerConfirmations(final Chunk paramChunk)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (confirmationsLock)
		{
			if (confirmationsArray.containsKey(generatedHash))
			{
				return confirmationsArray.get(generatedHash);
			}
		}

		return null;
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