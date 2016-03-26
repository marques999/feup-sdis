package bs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import bs.filesystem.Chunk;
import bs.logging.Logger;
import bs.protocol.GenericMessage;

public class BackupService extends BaseService
{
	/**
	 * stores PUTCHUNK messages received for the files being reclaimed, key = (fileId, chunkId)
	 */
	private HashMap<Integer, Set<Integer>> putchunkMessages = new HashMap<>();
	
	/**
	 * mutex for dealing with concurrent accesses to the putchunk messages hashmap
	 */
	private Object putchunkLock = new Object();

	/**
	 * @brief default constructor for 'BackupService' class
	 * @param paramAddress address of the multicast backup channel
	 * @param paramPort port of the multicast backup channel
	 */
	public BackupService(final InetAddress paramAddress, int paramPort)
	{
		super("backup service", paramAddress, paramPort);
	}

	/**
	 * @brief verifies and processes a message sent to the backup service
	 * @param paramMessage contents of the received message
	 * @param hasPayload 'true' if this message should contain binary data attached
	 */
	@Override
	protected final void processMessage(final GenericMessage paramMessage, boolean hasPayload)
	{
		if (paramMessage.getType().equals("PUTCHUNK"))
		{
			if (hasPayload)
			{
				processPUTCHUNK(paramMessage);
			}
			else
			{
				Logger.logError("backup service received empty PUTCHUNK message!");
			}
		}
		else
		{
			Logger.logError("backup service received an invalid message!");
		}
	}

	/**
	 * @brief processes a PUTCHUNK message sent to the control service
	 * @param paramMessage contents of the received message
	 */
	public final void processPUTCHUNK(final GenericMessage paramMessage)
	{
		final String fileId = paramMessage.getFileId();

	/*	if (!bsdbInstance.hasLocalChunks(fileId))
		{
			return;
		}*/

		int chunkId = paramMessage.getChunkId();
		int replicationDegree = paramMessage.getDegree();
		byte[] messageBody = paramMessage.getBody();
		boolean chunkExists = false;
		
		if (fmInstance.chunkExists(fileId, chunkId))
		{	
			chunkExists = true;
			
			if (!bsdbInstance.hasChunk(fileId, chunkId))
			{
				Logger.logDebug("chunk with id=" + chunkId + " was already backed up, but missing in the database!");
				
				try
				{
					Logger.logDebug("reading chunk with id=" + chunkId + " from existing archive...");
					bsdbInstance.putChunk(fmInstance.readChunk(fileId, chunkId));
				}
				catch (IOException ex)
				{
					Logger.logError("could not read chunk with id=" + chunkId + " from existing archive!");
					chunkExists = false;
				}
			}
			else
			{
				Logger.logDebug("chunk with id=" + chunkId + " already backed up by this peer!");
			}
		}
		else
		{
			/*
			 * REJECT CHUNKS IF THERE'S NO FREE SPACE LEFT ON THIS PEER
			 */
			if (messageBody.length > bsdbInstance.getFreeSpace())
			{
				return;
			}
		}
		
		final Chunk myChunk = paramMessage.generateChunk();
		final ControlService svcControl = BackupSystem.getControlService();

		/*
		 * IF PEER ALREADY HAS THIS CHUNK, SEND "STORED" CONFIRMATION TO REMAINING PEERS
		 */
		if (chunkExists)
		{
			BackupSystem.sendSTORED(myChunk);
		}
		else
		{
			/*
			 * SEND CHUNK TO OTHER PEERS TO ACHIEVE DESIRED REPLICATION DEGREE
			 */
			try
			{
				// START LISTENING FOR "STORED" CONFIRMATIONS FOR THIS CHUNK
				svcControl.subscribeConfirmations(myChunk);
				Thread.sleep(generateBackoff());
				
				int numberConfirmations = svcControl.getPeerConfirmations(myChunk);
				
				// WAIT FOR PEER CONFIRMATIONS // TODO: ENHANCEMENT
				if (numberConfirmations < replicationDegree)
				{
					Logger.logDebug("received " + numberConfirmations + " confirmations for chunk with id=" + chunkId);
					BackupSystem.sendSTORED(myChunk);
					Logger.logDebug("saving chunk with id=" + chunkId + " to storage...");
					fmInstance.writeChunk(myChunk);
				}
				
				// STOP LISTENING FOR "STORED" CONFIRMATIONS FOR THIS CHUNK
				svcControl.unsubscribeConfirmations(myChunk);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * @brief starts listening for PUTCHUNK messages for the current chunk
	 * @param fileId unique identifier of the file being backed up
	 * @param chunkId unique identifier of the file being backed up
	 */
	public final void subscribePutchunk(final String fileId, int chunkId)
	{
		int generatedHash = calculateHash(fileId, chunkId);

		synchronized (putchunkLock)
		{
			if (!putchunkMessages.containsKey(generatedHash))
			{
				putchunkMessages.put(generatedHash, new HashSet<Integer>());
			}
		}
	}

	/**
	 * @brief stops listening for PUTCHUNK messages received for this chunk
	 * @param fileId unique identifier of the file being backed up
	 * @param chunkId unique identifier of the chunk being backed up
	 */
	public final int unsubscribePutchunk(final String fileId, int chunkId)
	{
		int generatedHash = calculateHash(fileId, chunkId);

		synchronized (putchunkLock)
		{
			if (putchunkMessages.containsKey(generatedHash))
			{
				return putchunkMessages.remove(generatedHash).size();
			}
		}
		
		return 0;
	}

	/**
	 * @brief registers a PUTCHUNK message sent to the backup service
	 * @param paramChunk binary chunk of the file being backed up
	 * @param peerId unique identifier of the peer sending this chunk
	 */
	public final void registerPutchunk(final Chunk paramChunk, int peerId)
	{
		int generatedHash = calculateHash(paramChunk.getFileId(), paramChunk.getChunkId());

		synchronized (putchunkLock)
		{
			if (putchunkMessages.containsKey(generatedHash))
			{
				putchunkMessages.get(generatedHash).add(peerId);
			}
		}
	}
}