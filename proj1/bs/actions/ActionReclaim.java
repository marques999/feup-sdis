package bs.actions;

import java.io.FileNotFoundException;

import bs.BackupGlobals;
import bs.BackupService;
import bs.BackupSystem;
import bs.BaseService;
import bs.ControlService;
import bs.RestoreService;
import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.filesystem.FileManager;
import bs.logging.Logger;
import bs.misc.Pair;

public class ActionReclaim extends Thread
{
	private final FileManager fmInstance;
	private final BackupStorage bsdbInstance;
	
	public ActionReclaim(int amount)
	{
		m_result = false;
		fmInstance = BackupSystem.getFiles();
		bsdbInstance = BackupSystem.getStorage();
	}
		
	private boolean m_result;
	
	public boolean getResult()
	{
		return m_result;
	}

	@Override
	public void run()
	{
		final ControlService controlService = BackupSystem.getControlService();
		final BackupService backupService = BackupSystem.getBackupService();
		final RestoreService restoreService = BackupSystem.getRestoreService();
		// Peer.getDisk().setCapacity(m_amount);

		while (bsdbInstance.getFreeSpace() < 0)
		{
			/*
			 * select most replicated chunk from the database
			 */
			final Pair<String, Integer> mostReplicated = bsdbInstance.getMostReplicated();

			if (mostReplicated != null)
			{
				int chunkId = mostReplicated.second();
				final String fileId = mostReplicated.first();
				final Chunk myChunk = fmInstance.readChunk(fileId, chunkId);	
				
				/*
				 * send REMOVED and start listening for PUTCHUNK messages
				 */
				BackupSystem.sendREMOVED(fileId, chunkId);
				backupService.subscribePutchunk(fileId, chunkId);

				/*
				 * wait for PUTCHUNK messages (initial waiting time: 1 second)
				 */
				try
				{
					Thread.sleep(BackupGlobals.initialWaitingTime);
				}
				catch (InterruptedException ex)
				{
					ex.printStackTrace();
				}
				
				// STOP LISTENING AND RETRIEVE NUMBER OF RECEIVED PUTCHNK MESSAGES
				int numberPutchunkMessages = backupService.unsubscribePutchunk(fileId, chunkId);

				/*
				 * if no PUTCHUNK was received, it might mean that no peers are backing up this chunk
				 */
				boolean peersHaveChunk = true;

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

					peersHaveChunk = restoreService.hasReceivedChunk(myChunk);
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
					Logger.logDebug("replication degree is less than desired. Trying to fix it.");
					controlService.subscribeConfirmations(myChunk);

					long waitingTime = BackupGlobals.initialWaitingTime;
					int currentAtempt = 0;

					while (!m_result)
					{
						try
						{
							System.out.println("Waiting for STOREDs for " + waitingTime + "ms");
							Thread.sleep(waitingTime);
						}
						catch (InterruptedException ex)
						{
							ex.printStackTrace();
						}
						
						int numberStoredMessages = controlService.getPeerConfirmations(myChunk);

						if (numberStoredMessages == 0)
						{
							currentAtempt++;

							if (currentAtempt > BackupGlobals.maximumAttempts)
							{
								Logger.logWarning("None of the peers has stored this chunk.");
								Logger.logWarning("Starting chunk backup initiator before deleting this chunk.");

								try
								{
									new BackupHelper(myChunk).start();
								}
								catch (FileNotFoundException ex)
								{
									ex.printStackTrace();
								}

								m_result = true;
							}
							else
							{
								waitingTime *= 2;
							}
						}
						else
						{
							m_result = true;
						}
					}

					controlService.unsubscribeConfirmations(myChunk);
				}

				Logger.logDebug("Deleting chunk no. " + chunkId);
				m_result = fmInstance.deleteChunk(fileId, chunkId);
			}
			else
			{
				Logger.logError("There are no chunks stored. Unexpected error.");
			}
		}
	}
}