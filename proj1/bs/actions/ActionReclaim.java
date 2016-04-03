package bs.actions;

import bs.PeerGlobals;

import java.util.Set;

import bs.BackupService;
import bs.Peer;
import bs.ControlService;
import bs.filesystem.Chunk;
import bs.logging.Logger;

public class ActionReclaim extends Action
{
	private final static String messageMaxAttempts = "reclaim operation did not complete after five attempts!";
	private final static String messageNoChunks = "this peer isn't currently storing any more chunks!";
	private final static String messageStartingBackup = "starting chunk backup subprotocol...";
	private final static String messageWaiting = "waiting for peer confirmations, attempt %d of 5...";
	private final static String messageNoPutchunk = "no putchunk message received, peers are not trying to fix replication degree?";
	private final static String messageBackupTimeout = "couldn't reach desired replication degree, trying again...";
	private final static String messageReclaimFailed = "peers in this network have not backed up this chunk!";
	private final static String messageConfirmations = "received %d confirmations (desired repliation degree is %d)";

	public ActionReclaim(long reclaimAmount)
	{
		numberBytes = reclaimAmount;
	}

	private long numberBytes;

	private boolean runEnhancement(final Chunk mostReplicated)
	{
		final ControlService controlService = Peer.getControlService();
		final BackupService backupService = Peer.getBackupService();
		final String fileId = mostReplicated.getFileId();

		//-----------------------------------------------------------------------
		// START LISTENING FOR PUTCHUNK MESSAGES (INITIAL WAITING TIME: 1 SECOND)
		//-----------------------------------------------------------------------

		int chunkId = mostReplicated.getChunkId();
		boolean taskResult = false;

		try
		{
			backupService.subscribePutchunk(fileId, chunkId);
			Thread.sleep(PeerGlobals.initialWaitingTime);
		}
		catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}

		//----------------------------------------------------------------
		// STOP LISTENING FOR PUTCHUNK MESSAGES AND RETRIEVE MESSAGE COUNT
		//----------------------------------------------------------------

		int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);

		//----------------------------------------------------------------------
		// NO PUTCHUNK RECEIVED, PEERS ARE NOT TRYING TO FIX REPLICATION DEGREE?
		//----------------------------------------------------------------------

		if (numberPutchunkMessages > 0)
		{
			Logger.logDebug("received " + numberPutchunkMessages + " putchunk messages, moving on...");
			taskResult = listenConfirmations(mostReplicated, controlService);
			
			if (!taskResult)
			{
				Logger.logError(messageMaxAttempts);
			}
		}
		else
		{
			Logger.logWarning(messageNoPutchunk);
			taskResult = forceBackup(mostReplicated);
		}
		
		return taskResult;
	}

	private boolean listenConfirmations(final Chunk myChunk, final ControlService controlService)
	{
		int waitingTime = PeerGlobals.initialWaitingTime;
		boolean taskResult = false;
		
		Set<Integer> peerConfirmations = bsdbInstance.getPeers(myChunk.getFileId(), myChunk.getChunkId());
		controlService.subscribeConfirmations(myChunk);
		controlService.setPeerConfirmations(myChunk, peerConfirmations);

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

			taskResult = (numberConfirmations >= myChunk.getReplicationDegree());

			if (taskResult)
			{
				break;
			}

			//--------------------------------------------------------
			// START CHUNK BACKUP PROTOCOL IF MAXIMUM ATTEMPTS REACHED
			//--------------------------------------------------------

			if (i == PeerGlobals.maximumAttempts)
			{
				taskResult = forceBackup(myChunk);
			}
			else
			{
				Logger.logWarning(messageBackupTimeout);
				waitingTime *= 2;
			}
		}

		controlService.unsubscribeConfirmations(myChunk);

		return taskResult;
	}

	private boolean forceBackup(final Chunk myChunk)
	{
		Logger.logWarning(messageReclaimFailed);
		Logger.logWarning(messageStartingBackup);

		final BackupHelper backupHelper = new BackupHelper(myChunk, false);

		backupHelper.start();

		try
		{
			backupHelper.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		return backupHelper.getResult();
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
				final int peerCount = bsdbInstance.getPeerCount(fileId, chunkId);

				//------------------------------------
				// SEND REMOVED MESSAGE TO OTHER PEERS
				//------------------------------------

				bsdbInstance.registerReclaim(mostReplicated);
				Peer.sendREMOVED(fileId, chunkId);

				//-----------------------------------------------
				// CHECK IF OTHER PEERS HAVE BACKED UP THIS CHUNK
				//-----------------------------------------------
				
				if (peerCount == 1)
				{
					try
					{
						Thread.sleep(generateBackoff());
					}
					catch (InterruptedException ex)
					{
						ex.printStackTrace();
					}

					actionResult = forceBackup(mostReplicated);				
				}
				else
				{
					//----------------------------------------------------------------------
					// IF PEER ENHANCEMENTS ARE ENABLED, RUN RECLAIM SUBPROTOCOL ENHANCEMENT
					//----------------------------------------------------------------------

					if (Peer.enhancementsEnabled())
					{
						actionResult = runEnhancement(mostReplicated);
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
						
						actionResult = true;
					}
				}
				
				//-------------------------------------------------------------
				// NOW IT'S SAFE TO DELETE THIS CHUNK FROM THE LOCAL FILESYSTEM
				//-------------------------------------------------------------
				
				if (actionResult)
				{
					if (fmInstance.deleteChunk(fileId, chunkId))
					{
						bytesFreed += bsdbInstance.unregisterChunk(fileId, chunkId);
						Logger.logInformation(bytesFreed + " bytes removed, requested amount: " + numberBytes + " bytes...");
					}
					else
					{
						bsdbInstance.unregisterReclaim();
						Logger.logError("could not delete chunk " + chunkId + " from the filesystem!");
					}
				}
			}
			else
			{
				Logger.logError(messageNoChunks);
				actionResult = false;
			}
		}
	}
}