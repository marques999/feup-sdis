package bs.actions;

import bs.PeerGlobals;
import bs.BackupService;
import bs.Peer;
import bs.ControlService;
import bs.RestoreService;
import bs.filesystem.Chunk;
import bs.logging.Logger;

public class ActionReclaim extends Action
{
	private static final String messageNoPutchunk = "no putchunk message received, peers are not trying to fix replication degree?";
	private static final String messageStartingBackup = "starting chunk backup before removing this chunk...";
	private final static String messageWaiting = "waiting for peer confirmations, attempt %d of 5...";
	private final static String messageBackupTimeout = "couldn't reach desired replication degree, trying again...";
	private final static String messageReclaimFailed = "peers in this network have not stored this chunk!";
	private final static String messageConfirmations = "received %d confirmations (desired repliation degree is %d)";

	public ActionReclaim(long reclaimAmount)
	{
		numberBytes = reclaimAmount;
	}

	private long numberBytes;

	private void runEnhancement(final Chunk mostReplicated)
	{
		final ControlService controlService = Peer.getControlService();
		final BackupService backupService = Peer.getBackupService();
		final RestoreService restoreService = Peer.getRestoreService();
		final String fileId = mostReplicated.getFileId();

		// -----------------------------------------------------------------------
		// START LISTENING FOR PUTCHUNK MESSAGES (INITIAL WAITING TIME: 1 SECOND)
		// -----------------------------------------------------------------------

		int chunkId = mostReplicated.getChunkId();

		try
		{
			backupService.subscribePutchunk(fileId, chunkId);
			Thread.sleep(PeerGlobals.initialWaitingTime);
		}
		catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}

		// ----------------------------------------------------------------
		// STOP LISTENING FOR PUTCHUNK MESSAGES AND RETRIEVE MESSAGE COUNT
		// ----------------------------------------------------------------

		boolean receivedChunk = true;
		int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);

		// ----------------------------------------------------------------------
		// NO PUTCHUNK RECEIVED, PEERS ARE NOT TRYING TO FIX REPLICATION DEGREE?
		// ----------------------------------------------------------------------

		if (numberPutchunkMessages == 0)
		{
			// -------------------------------------------------------------------
			// REQUEST CHUNK BEING REMOVED AND START LISTENING FOR CHUNK MESSAGES
			// -------------------------------------------------------------------

			Logger.logWarning(messageNoPutchunk);
			restoreService.startReceivingChunks(fileId);
			Peer.sendGETCHUNK(fileId, chunkId);

			// --------------------------------------------------------------------------
			// WAIT FOR CHUNK MESSAGES FROM OTHER PEERS (INITIAL WAITING TIME: 1 SECOND)
			// --------------------------------------------------------------------------

			try
			{
				Thread.sleep(PeerGlobals.initialWaitingTime);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			// --------------------------------------------------------------------
			// STOP LISTENING FOR CHUNK MESSAGES FOR THIS CHUNK AND RETRIEVE COUNT
			// --------------------------------------------------------------------

			receivedChunk = restoreService.hasReceivedChunk(mostReplicated);
			restoreService.stopReceivingChunks(fileId);

			if (!receivedChunk)
			{
				Logger.logWarning("replication degree is still lower than desired, forcing backup...");
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
			if (!forceBackup(mostReplicated, controlService))
			{
				Logger.logError("reclaim attempt failed!");
			}
		}
	}

	private boolean forceBackup(final Chunk myChunk, final ControlService controlService)
	{
		int waitingTime = PeerGlobals.initialWaitingTime;
		boolean actionDone = false;
		
		controlService.subscribeConfirmations(myChunk);

		//--------------------------------------------------------------------
		// REPEAT THIS BLOCK 5 TIMES, DOUBLING THE WAITING TIME ON EVERY RETRY
		//--------------------------------------------------------------------

		for (int i = 1; i <= PeerGlobals.maximumAttempts; i++)
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

			int numberConfirmations = controlService.getPeerConfirmations(myChunk).size();

			Logger.logDebug(String.format(messageConfirmations, numberConfirmations, myChunk.getReplicationDegree()));

			//-------------------------------------------------------------------
			// ONLY DELETE THIS CHUNK AFTER RECEIVING AT LEAST ONE STORED MESSAGE
			//-------------------------------------------------------------------

			actionDone = (numberConfirmations == myChunk.getReplicationDegree());

			if (actionDone)
			{
				break;
			}

			//--------------------------------------------------------
			// START CHUNK BACKUP PROTOCOL IF MAXIMUM ATTEMPTS REACHED
			//--------------------------------------------------------

			if (i == PeerGlobals.maximumAttempts)
			{
				Logger.logWarning(messageReclaimFailed);
				Logger.logWarning(messageStartingBackup);

				final BackupHelper backupHelper = new BackupHelper(myChunk, false);

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
				Peer.sendREMOVED(fileId, chunkId);

				//----------------------------------------------------------------------
				// IF PEER ENHANCEMENTS ARE ENABLED, RUN RECLAIM SUBPROTOCOL ENHANCEMENT
				//----------------------------------------------------------------------

				if (Peer.enhancementsEnabled())
				{
					runEnhancement(mostReplicated);
				}
				else
				{
					try
					{
						Thread.sleep(PeerGlobals.initialWaitingTime);
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
					bytesFreed += bsdbInstance.unregisterChunk(fileId, chunkId);
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