package bs.actions;

import bs.BackupGlobals;
import bs.BackupSystem;
import bs.ControlService;
import bs.filesystem.Chunk;
import bs.logging.Logger;

public class BackupHelper extends Thread
{
	private final Chunk myChunk;
	private final String msgBackupTimeout = "couldn't reach desired replication degree, trying again...";
	private final String msgBackupFailed = "reached maximum attempts to backup chunk with desired replication degree!";
	private final String msgBackupSuccessful = "chunk with id=%d was backed up sucessfully!";
	private final String msgBackupInformation = "%d peers have backed up chunk with id=%d (desired replication degree is %d)\n";

	public BackupHelper(final Chunk paramChunk)
	{
		myChunk = paramChunk;
	}
	
	@Override
	public void run()
	{
		int currentAttempt = 0;
		int chunkId = myChunk.getChunkId();
		int replicationDegree = myChunk.getReplicationDegree();
		int waitingTime = BackupGlobals.initialWaitingTime;
		boolean m_result = false;
		
		ControlService controlService = BackupSystem.getControlService();
		controlService.subscribeConfirmations(myChunk);
		
		while (!m_result)
		{
			controlService.resetPeerConfirmations(myChunk);
			BackupSystem.sendPUTCHUNK(myChunk);

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

			System.out.print(String.format(msgBackupInformation, numberConfirmations, chunkId, replicationDegree));

			if (numberConfirmations < replicationDegree)
			{
				currentAttempt++;

				if (currentAttempt > BackupGlobals.maximumAttempts)
				{
					Logger.logError(msgBackupFailed);
					m_result = true;
				}
				else
				{
					Logger.logWarning(msgBackupTimeout);
					waitingTime *= 2;
				}
			}
			else
			{
				Logger.logDebug(String.format(msgBackupSuccessful, chunkId));
				m_result = true;
			}
		}

		controlService.unsubscribeConfirmations(myChunk);
	}
}