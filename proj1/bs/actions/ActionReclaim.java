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
	private static final String messageStartingBackup = "starting chunk backup before removing this chunk...";
	private final static String messageWaiting = "waiting for peer confirmations, attempt %d of 5...";
	private final static String messageBackupTimeout = "couldn't reach desired replication degree, trying again...";
	private final static String messageReclaimFailed = "peers in this network have not stored this chunk!";
	private final static String messageConfirmations = "%d peers have stored chunk %d";
		
	public ActionReclaim(long reclaimAmount)
	{
		numberBytes = reclaimAmount;
	}

	private long numberBytes;

	private void runEnhancement(final Chunk mostReplicated)
	{
		final ControlService controlService = BackupSystem.getControlService();
		final BackupService backupService = BackupSystem.getBackupService();
		final RestoreService restoreService = BackupSystem.getRestoreService();
		final String fileId = mostReplicated.getFileId();

		// -----------------------------------------------------------------------
		// START LISTENING FOR PUTCHUNK MESSAGES (INITIAL WAITING TIME: 1 SECOND)
		// -----------------------------------------------------------------------

		int chunkId = mostReplicated.getChunkId();

		try
		{
			backupService.subscribePutchunk(fileId, chunkId);
			Thread.sleep(BackupGlobals.initialWaitingTime);
		}
		catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}

		//----------------------------------------------------------------
		// STOP LISTENING FOR PUTCHUNK MESSAGES AND RETRIEVE MESSAGE COUNT
		//----------------------------------------------------------------

		boolean receivedChunk = true;
		int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);

		//----------------------------------------------------------------------
		// NO PUTCHUNK RECEIVED, PEERS ARE NOT TRYING TO FIX REPLICATION DEGREE?
		//----------------------------------------------------------------------

		if (numberPutchunkMessages == 0)
		{
			//-------------------------------------------------------------------
			// REQUEST CHUNK BEING REMOVED AND START LISTENING FOR CHUNK MESSAGES
			//-------------------------------------------------------------------

			Logger.logWarning("no putchunk message received, peers are not trying to fix replication degree?");
			restoreService.startReceivingChunks(fileId);
			BackupSystem.sendGETCHUNK(fileId, chunkId);

			//--------------------------------------------------------------------------
			// WAIT FOR CHUNK MESSAGES FROM OTHER PEERS (INITIAL WAITING TIME: 1 SECOND)
			//--------------------------------------------------------------------------

			try
			{
				Thread.sleep(BackupGlobals.initialWaitingTime);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			//--------------------------------------------------------------------
			// STOP LISTENING FOR CHUNK MESSAGES FOR THIS CHUNK AND RETRIEVE COUNT
			//--------------------------------------------------------------------

			receivedChunk = restoreService.hasReceivedChunk(mostReplicated);
			restoreService.stopReceivingChunks(fileId);

			if (receivedChunk)
			{
				Logger.logDebug("chunk " + chunkId + " was received on the restore protocol!");
			}
		}
		else
		{
			Logger.logDebug("received " + numberPutchunkMessages + " putchunk messages, moving on...");
		}

		//----------------------------------------------------------------------
		// IF AT LEAST ONE PUTCHUNK WAS RECEIVED, THE REPLICATION DEGREE OF THE
		// chunk this peer is removing is still greater or equal to the desired;
		//----------------------------------------------------------------------

		if (numberPutchunkMessages > 0 || !receivedChunk)
		{
			Logger.logWarning("replication degree is still lower than expected, forcing backup...");

			if (!forceBackup(mostReplicated, controlService))
			{
				Logger.logError("reclaim attempt failed!");
			}
		}
	}

	private boolean forceBackup(final Chunk myChunk, final ControlService controlService)
	{
		Logger.logDebug("replication degree is less than desired, attempting to fix it...");
		controlService.subscribeConfirmations(myChunk);

		int waitingTime = BackupGlobals.initialWaitingTime;
		boolean actionDone = false;

		//--------------------------------------------------------------------
		// REPEAT THIS BLOCK 5 TIMES, DOUBLING THE WAITING TIME ON EVERY RETRY
		//--------------------------------------------------------------------

		for (int i = 1; i <= BackupGlobals.maximumAttempts; i++)
		{
			//---------------------------------------------------------------------
			// START LISTENING FOR STORED MESSAGES (INITIAL WAITING TIME: 1 SECOND)
			//---------------------------------------------------------------------

			try
			{
				Logger.logDebug(String.format(messageWaiting, i));
				Thread.sleep(waitingTime);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			//-----------------------------------------------
			// RETRIEVE "STORED" MESSAGE COUNT FOR THIS CHUNK
			//-----------------------------------------------

			int numberConfirmations = controlService.getPeerConfirmations(myChunk);

			Logger.logDebug(String.format(messageConfirmations, numberConfirmations, myChunk.getChunkId()));

			//-------------------------------------------------------------------
			// ONLY DELETE THIS CHUNK AFTER RECEIVING AT LEAST ONE STORED MESSAGE
			//-------------------------------------------------------------------

			actionDone = numberConfirmations > 0;

			if (actionDone)
			{
				break;
			}

			//--------------------------------------------------------
			// START CHUNK BACKUP PROTOCOL IF MAXIMUM ATTEMPTS REACHED
			//--------------------------------------------------------

			if (i == BackupGlobals.maximumAttempts)
			{
				Logger.logWarning(messageReclaimFailed);
				Logger.logWarning(messageStartingBackup);

				final BackupHelper backupHelper = new BackupHelper(myChunk, true);

				backupHelper.start();

				try
				{
					backupHelper.join();
					actionDone = true;
				}
				catch (InterruptedException ex)
				{
					ex.printStackTrace();
					actionDone = false;
				}

				break;
			}
			else
			{
				Logger.logWarning(messageBackupTimeout);
				waitingTime *= 2;
			}
		}

		controlService.unsubscribeConfirmations(myChunk);

		return actionDone;
	}

	@Override
	public void run()
	{
		actionResult = true;

		if (numberBytes > bsdbInstance.getUsedSpace())
		{
			numberBytes = bsdbInstance.getUsedSpace();
		}

		int bytesFreed = 0;
		boolean enhancementsEnabled = BackupSystem.enhancementsEnabled();

		while (bytesFreed < numberBytes && actionResult)
		{
			//-------------------------------------------------
			// RETRIEVE MOST REPLICATED CHUNK FROM THE DATABASE
			//-------------------------------------------------

			final Chunk mostReplicated = bsdbInstance.getMostReplicated();

			if (mostReplicated != null)
			{
				final String fileId = mostReplicated.getFileId();
				final int chunkId = mostReplicated.getChunkId();

				//------------------------------------
				// SEND REMOVED MESSAGE TO OTHER PEERS		
				//------------------------------------	

				bsdbInstance.registerReclaim(mostReplicated);
				BackupSystem.sendREMOVED(fileId, chunkId);

				//----------------------------------------------------------------------
				// IF PEER ENHANCEMENTS ARE ENABLED, RUN RECLAIM SUBPROTOCOL ENHANCEMENT
				//----------------------------------------------------------------------

				if (enhancementsEnabled)
				{
					runEnhancement(mostReplicated);
				}
				else
				{
					try
					{
						Thread.sleep(BackupGlobals.initialWaitingTime);
					}
					catch (InterruptedException ex)
					{
						ex.printStackTrace();
					}
				}

				//-------------------------------------------------------------
				// NOW IT'S SAFE TO DELETE THIS CHUNK FROM THE LOCAL FILESYSTEM
				//-------------------------------------------------------------

				if (fmInstance.deleteChunk(fileId, chunkId))
				{
					bytesFreed += bsdbInstance.removeChunk(fileId, chunkId);
					Logger.logDebug(bytesFreed + " bytes removed, requested amount:" + numberBytes + " bytes...");
				}
				else
				{
					bsdbInstance.unregisterReclaim();
					Logger.logError("could not delete chunk " + chunkId + " from the filesystem!");
				}
			}
			else
			{
				Logger.logError("this peer isn't currently storing any more chunks!");
				actionResult = false;
			}
		}
	}
}