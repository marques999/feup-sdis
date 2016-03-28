package bs.actions;

import bs.BackupGlobals;
import bs.BackupService;
import bs.BackupSystem;
import bs.ControlService;
import bs.RestoreService;
import bs.filesystem.Chunk;
import bs.logging.Logger;

public class ActionReclaim extends Action
{
	public ActionReclaim(long reclaimAmount)
	{
		numberBytes = reclaimAmount;
	}
	
	private long numberBytes;

	private void reclaimEnhancement(final Chunk myChunk, final ControlService controlService)
	{
		Logger.logDebug("replication degree is less than desired, attempting to fix it...");
		controlService.subscribeConfirmations(myChunk);

		int waitingTime = BackupGlobals.initialWaitingTime;
		int currentAttempt = 0;

		while (!actionResult)
		{
			try
			{
				Logger.logDebug("waiting for STORED confirmations...");
				Thread.sleep(waitingTime);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}
			
			int numberConfirmations = controlService.getPeerConfirmations(myChunk);

			Logger.logDebug("received " + numberConfirmations + " confirmations for chunk with id=" + myChunk.getChunkId());
			
			if (numberConfirmations == 0)
			{
				currentAttempt++;

				if (currentAttempt > BackupGlobals.maximumAttempts)
				{
					Logger.logWarning("peers in this network have not stored this chunk!");
					Logger.logWarning("starting chunk backup before removing this chunk...");
		
					final BackupHelper backupHelper = new BackupHelper(myChunk);
				
					backupHelper.start();
					
					try
					{
						backupHelper.join();
						actionResult = true;
					}
					catch (InterruptedException ex)
					{
						ex.printStackTrace();
						actionResult = false;
					}	
				}
				else
				{
					waitingTime *= 2;
				}
			}
			else
			{
				actionResult = true;
			}
		}

		controlService.unsubscribeConfirmations(myChunk);	
	}

	@Override
	public void run()
	{
		if (numberBytes > bsdbInstance.getUsedSpace())
		{
			numberBytes = bsdbInstance.getUsedSpace();
		}
		
		int bytesFreed = 0;
		
		while (bytesFreed < numberBytes)
		{
			/*
			 * SELECT MOST REPLICATED CHUNK FROM THE DATABASE
			 */
			final Chunk mostReplicated = bsdbInstance.getMostReplicated();
			final ControlService controlService = BackupSystem.getControlService();
			final BackupService backupService = BackupSystem.getBackupService();
			final RestoreService restoreService = BackupSystem.getRestoreService();

			if (mostReplicated != null)
			{
				final String fileId = mostReplicated.getFileId();
				final int chunkId = mostReplicated.getChunkId();

				/*
				 * SEND REMOVED MESSAGE TO OTHER PEERS		
				 */		
				BackupSystem.sendREMOVED(fileId, chunkId);
				
				/*
				 * START LISTENING FOR PUTCHUNK MESSAGES (INITIAL WAITING TIME: 1 SECOND)
				 */	
				try
				{
					backupService.subscribePutchunk(fileId, chunkId);
					Thread.sleep(BackupGlobals.initialWaitingTime);
				}
				catch (InterruptedException ex)
				{
					ex.printStackTrace();
				}
				
				/* 
				 * STOP LISTENING AND RETRIEVE NUMBER OF RECEIVED PUTCHNK MESSAGES
				 */
				boolean peersHaveChunk = true;
				int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);
							
				/*
				 * IF NO PUTCHUNK WAS RECEIVED, it might mean that no peers are backing up this chunk
				 */
				if (numberPutchunkMessages == 0)
				{
					restoreService.startReceivingChunks(fileId);
					BackupSystem.sendGETCHUNK(fileId, chunkId);

					try
					{
						Thread.sleep(BackupGlobals.initialWaitingTime);
					}
					catch (InterruptedException ex)
					{
						ex.printStackTrace();
					}

					peersHaveChunk = restoreService.hasChunk(mostReplicated);
					restoreService.stopReceivingChunks(fileId);
				}

				/*
				 * If no PUTCHUNKs are received after 1 second, that means the
				 * replication degree of the chunk this peer is removing is
				 * still greater or equal to the desired; else, start listening
				 * for STOREDs and only delete the chunk after a STORED has been
				 * confirmed, or maximum attempt has been reached.
				 */
				if (numberPutchunkMessages > 0 || !peersHaveChunk)
				{
					reclaimEnhancement(mostReplicated, controlService);
				}

				if (fmInstance.deleteChunk(fileId, chunkId))
				{
					bsdbInstance.removeChunk(fileId, chunkId);
				}
				else
				{
					
				}
			}
			else
			{
				Logger.logError("no chunks have been stored on this peer!");
			}
		}
	}
}