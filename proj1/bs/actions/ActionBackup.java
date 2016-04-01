package bs.actions;

import java.io.IOException;

import bs.filesystem.Chunk;
import bs.filesystem.ChunkBackup;
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
		if (bsdbInstance.canBackup(fileName))
		{		
			try
			{
				final ChunkBackup chunkBackup = new ChunkBackup(fileName, replicationDegree);

				if (sendChunks(chunkBackup))
				{
					bsdbInstance.registerRestore(chunkBackup);
					actionResult = true;
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