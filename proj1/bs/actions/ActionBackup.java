package bs.actions;

import java.io.IOException;

import bs.filesystem.Chunk;
import bs.filesystem.ChunkBackup;
import bs.filesystem.FileInformation;
import bs.logging.Logger;

public class ActionBackup extends Action
{
	private static final String messageInterrupted = "backup thread was interrupted before it could complete!";
	private static final String messageFileOnline = "requested file has already been backed up on the network!";
	private static final String messageFileNotFound = "requested file doesn't exist on the filesystem!";
	private final String fileName;

	public ActionBackup(final String paramName, int paramDegree)
	{
		fileName = paramName;
		replicationDegree = paramDegree;
	}

	private int replicationDegree;

	private boolean sendChunks(final ChunkBackup chunkBackup)
	{
		final Chunk[] chunkArray = chunkBackup.getChunks();

		for (int i = 0; i < chunkArray.length; i++)
		{
			final BackupHelper currentThread = new BackupHelper(chunkArray[i], false);

			currentThread.start();

			try
			{
				currentThread.join();
			}
			catch (InterruptedException ex)
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public void run()
	{
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(fileName);

		if (restoreInformation == null)
		{
			try
			{
				final ChunkBackup chunkBackup = new ChunkBackup(fileName, replicationDegree);

				if (sendChunks(chunkBackup))
				{
					actionResult = bsdbInstance.registerRestore(chunkBackup);
				}
				else
				{
					Logger.logError(messageInterrupted);
				}
			}
			catch (IOException ex)
			{
				Logger.logError(messageFileNotFound);
			}
		}
		else
		{
			Logger.logError(messageFileOnline);
		}
	}
}