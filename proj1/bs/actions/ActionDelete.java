package bs.actions;

import java.util.Set;

import bs.Logger;
import bs.Peer;
import bs.PeerGlobals;
import bs.filesystem.FileInformation;

public class ActionDelete extends Action
{
	private final static String messageDeleting = "deleted %s from files/ directory...";
	private final static String messageDeleteFailed = "something bad happened while trying to delete %s!";
	private final static String messageFileOnline = "file was previously backed up by some peers in this network!";
	private final static String messageFileOffline = "peers in this network have not backed up this file.";
	private final static String messageFileNotFound = "requested file doesn't exist in the filesystem!";
	private final static String messagePeerHasChunks = "current peer was also backing up some chunks!";
	private final static String messageSendingDelete = "sending \"delete\" command... (%d/%d)";
	private final static String messagePendingConfirmation = "file was not deleted, some peers have not confirmed its deletion!";
	private final static String messageWaitingDeleted = "waiting for \"deleted\" confirmations from %d peers... (%d/%d)";
	private final String fileName;

	public ActionDelete(final String paramFile)
	{
		fileName = paramFile;
	}

	private long waitingTime = PeerGlobals.initialWaitingTime;

	private boolean simpleDelete(final String fileId)
	{
		for (int i = 1; i <= PeerGlobals.maximumAttempts; i++)
		{
			Peer.sendDELETE(fileId);
			Logger.logDebug(String.format(messageSendingDelete, i, PeerGlobals.maximumAttempts));

			try
			{
				Thread.sleep(generateBackoff());
			}
			catch (InterruptedException ex)
			{
				return false;
			}
		}

		return true;
	}

	private boolean enhancedDelete(final String fileId)
	{
		Set<Integer> myPeers = bsdbInstance.getRemotePeers(fileId);

		for (int i = 1; i <= PeerGlobals.maximumAttempts; i++)
		{
			Peer.sendDELETE(fileId);
			Logger.logInformation(String.format(messageWaitingDeleted, myPeers.size(), i, PeerGlobals.maximumAttempts));

			try
			{
				Thread.sleep(waitingTime);
			}
			catch (InterruptedException ex)
			{
				return false;
			}

			myPeers = bsdbInstance.getRemotePeers(fileId);

			if (myPeers.isEmpty())
			{
				return true;
			}

			waitingTime *= 2;
		}

		return false;
	}

	@Override
	public void run()
	{
		if (fmInstance.fileExists(fileName))
		{
			final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(fileName);

			if (restoreInformation == null)
			{
				Logger.logInformation(messageFileOffline);
				actionResult = true;
			}
			else
			{
				Logger.logInformation(messageFileOnline);

				if (Peer.enhancementsEnabled())
				{
					actionResult = enhancedDelete(restoreInformation.getFileId());
				}
				else
				{
					actionResult = simpleDelete(restoreInformation.getFileId());
				}
			}

			if (actionResult)
			{
				if (fmInstance.deleteFile(fileName))
				{
					if (bsdbInstance.removeChunks(fileName))
					{
						Logger.logWarning(messagePeerHasChunks);
					}

					Logger.logInformation(String.format(messageDeleting, fileName));
					bsdbInstance.unregisterRestore(fileName);
				}
				else
				{
					Logger.logError(String.format(messageDeleteFailed, fileName));
					actionResult = false;
				}
			}
			else
			{
				Logger.logError(messagePendingConfirmation);
				Peer.writeStorage();
			}
		}
		else
		{
			Logger.logError(messageFileNotFound);
		}
	}
}