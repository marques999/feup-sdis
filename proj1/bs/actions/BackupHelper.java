package bs.actions;

import bs.PeerGlobals;
import bs.Peer;

import java.util.Set;

import bs.ControlService;
import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.logging.Logger;

public class BackupHelper extends Thread
{
	private final static String messageStoredTimeout = "couldn't reach desired replication degree, trying again...";
	private final static String messageWaiting = "waiting for \"stored\" confirmations... (%d/%d)";
	private final static String messageBackupFailed = "reached maximum attempts to backup chunk with desired replication degree!";
	private final static String messageConfirmations = "received %d confirmations (desired replication degree is %d)";
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
		final BackupStorage bsdbInstance = Peer.getStorage();		
		boolean actionResult = false;
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
					actionResult = true;
				}
				else
				{
					Logger.logWarning(messageStoredTimeout);
					waitingTime *= 2;
				}
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