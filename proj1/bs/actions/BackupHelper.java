package bs.actions;

import bs.BackupGlobals;
import bs.BackupSystem;
import bs.ControlService;
import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.logging.Logger;

public class BackupHelper extends Thread
{
	private final static String messageWaiting = "waiting for peer confirmations for chunk...";
	private final static String msgBackupTimeout = "couldn't reach desired replication degree, trying again...";
	private final static String msgBackupFailed = "reached maximum attempts to backup chunk with desired replication degree!";
	private final static String msgBackupInformation = "%d peers have backed up chunk %d (desired replication degree is %d)";
	private final Chunk myChunk;
	
	public BackupHelper(final Chunk paramChunk, boolean paramMode)
	{
		myChunk = paramChunk;
		reclaimMode = paramMode;
	}
	
	private boolean reclaimMode;

	@Override
	public void run()
	{
		final ControlService controlService = BackupSystem.getControlService();
		final BackupStorage bsdbInstance = BackupSystem.getStorage();		
		boolean actionResult = false;
		int currentAttempt = 0;
		int chunkId = myChunk.getChunkId();
		int replicationDegree = myChunk.getReplicationDegree();
		int waitingTime = BackupGlobals.initialWaitingTime;

		controlService.subscribeConfirmations(myChunk);

		while (!actionResult)
		{
			controlService.resetPeerConfirmations(myChunk);
			BackupSystem.sendPUTCHUNK(myChunk);

			try
			{
				Logger.logDebug(messageWaiting);
				Thread.sleep(waitingTime);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			if (reclaimMode)
			{
				BackupSystem.sendSTORED(myChunk);
			}

			int existingConfirmations = bsdbInstance.getPeerCount(myChunk.getFileId(), chunkId);
			int numberConfirmations = existingConfirmations + controlService.getPeerConfirmations(myChunk);

			Logger.logDebug(String.format(msgBackupInformation, numberConfirmations, chunkId, replicationDegree));

			if (numberConfirmations < replicationDegree)
			{
				currentAttempt++;

				if (currentAttempt > BackupGlobals.maximumAttempts)
				{
					Logger.logError(msgBackupFailed);
					actionResult = true;
				}
				else
				{
					Logger.logWarning(msgBackupTimeout);
					waitingTime *= 2;
				}
			}
			else
			{
				Logger.logInformation("chunk " + chunkId + " was backed up sucessfully");
				actionResult = true;
			}
		}

		controlService.unsubscribeConfirmations(myChunk);
	}
}