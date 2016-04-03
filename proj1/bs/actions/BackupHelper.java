package bs.actions;

import java.util.Set;

import bs.ControlService;
import bs.Logger;
import bs.Peer;
import bs.PeerGlobals;
import bs.filesystem.Chunk;

public class BackupHelper extends Action
{
	private final static String messageWaiting = "waiting for \"stored\" confirmations... (%d/%d)";
	private final static String messageAbortBackup = "peers have not stored this chunk, aborting backup...";
	private final static String messageStoredTimeout = "couldn't reach desired replication degree, trying again...";
	private final static String messageConfirmations = "received %d confirmations (desired replication degree is %d)";
	private final static String messageBackupFailed = "reached maximum attempts to backup chunk with desired replication degree!";
	private final Chunk myChunk;

	public BackupHelper(final Chunk paramChunk, boolean paramMode)
	{
		myChunk = paramChunk;
		reclaimMode = paramMode;
	}

	private final boolean reclaimMode;

	@Override
	public void run()
	{
		final ControlService controlService = Peer.getControlService();
		int currentAttempt = 1;
		int chunkId = myChunk.getChunkId();
		int replicationDegree = myChunk.getReplicationDegree();
		int waitingTime = PeerGlobals.initialWaitingTime;

		controlService.subscribeConfirmations(myChunk);

		while (!actionResult)
		{
			if (reclaimMode)
			{
				Set<Integer> existingConfirmations = bsdbInstance.getPeers(myChunk.getFileId(), chunkId);
				controlService.setPeerConfirmations(myChunk, existingConfirmations);
			}
			else
			{
				controlService.resetPeerConfirmations(myChunk);
			}

			Peer.sendPUTCHUNK(myChunk);

			try
			{
				Logger.logDebug(String.format(messageWaiting, currentAttempt, PeerGlobals.maximumAttempts));
				Thread.sleep(waitingTime);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
			}

			if (reclaimMode)
			{
				Peer.sendSTORED(myChunk);
			}

			final Set<Integer> peerConfirmations = controlService.getPeerConfirmations(myChunk);

			Logger.logDebug(String.format(messageConfirmations, peerConfirmations.size(), replicationDegree));

			if (peerConfirmations.size() < replicationDegree)
			{
				currentAttempt++;

				if (currentAttempt > PeerGlobals.maximumAttempts)
				{
					Logger.logError(messageBackupFailed);

					if (peerConfirmations.size() > 0)
					{
						actionResult = true;
					}
					else
					{
						Logger.logError(messageAbortBackup);
					}

					break;
				}

				Logger.logWarning(messageStoredTimeout);
				waitingTime *= 2;
			}
			else
			{
				actionResult = true;
			}

			bsdbInstance.registerRemotePeers(myChunk, peerConfirmations);
		}

		controlService.unsubscribeConfirmations(myChunk);
	}
}