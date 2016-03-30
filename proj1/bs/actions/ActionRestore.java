package bs.actions;

import bs.Peer;
import bs.PeerGlobals;
import bs.RestoreService;
import bs.filesystem.Chunk;
import bs.filesystem.ChunkRestore;
import bs.filesystem.FileInformation;
import bs.logging.Logger;
import bs.logging.ProgramException;

public class ActionRestore extends Action
{
	private static final String messageChunkTimeout = "chunk reception timed out, trying again...";
	private static final String messageWriteFailed = "could not write received chunks to output file!";
	private static final String messageRestoreFailed = "reached maximum attempts to restore the requested file!";
	private static final String messageFileNotFound = "requested file was not found in the network, cannot restore!";

	public ActionRestore(final String paramName)
	{
		fileName = paramName;
	}

	private final String fileName;

	@Override
	public void run()
	{
		final RestoreService restoreService = Peer.getRestoreService();
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(fileName);

		if (restoreInformation != null)
		{
			int numberChunks = restoreInformation.getCount();
			final ChunkRestore recoveredChunks = new ChunkRestore(restoreInformation);
			final String fileId = restoreInformation.getFileId();

			restoreService.startReceivingChunks(fileId);

			for (int i = 0; i < numberChunks; i++)
			{
				Peer.sendGETCHUNK(fileId, i);

				try
				{
					Thread.sleep(generateBackoff());
				}
				catch (InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}

			int currentChunk = 0;
			int currentAttempt = 1;
			boolean continueRestore = true;

			while (currentChunk != numberChunks && continueRestore)
			{
				final Chunk myChunk = restoreService.retrieveChunk(fileId, currentChunk);

				if (myChunk != null)
				{
					if (recoveredChunks.put(myChunk))
					{
						Logger.logDebug("received \"chunk\" for chunk with id=" + myChunk.getChunkId());
						currentAttempt = 1;
						currentChunk++;
					}
				}
				else
				{
					Logger.logWarning(messageChunkTimeout);
					currentAttempt++;
				}

				if (currentAttempt > PeerGlobals.maximumAttempts)
				{
					continueRestore = false;
				}
			}

			restoreService.stopReceivingChunks(fileId);

			if (continueRestore)
			{
				try
				{
					actionResult = fmInstance.writeFile(fileName, recoveredChunks.join());

					if (!actionResult)
					{
						Logger.logError(messageWriteFailed);
					}
				}
				catch (ProgramException ex)
				{
					ex.printMessage();
					actionResult = false;
				}
			}
			else
			{
				Logger.logError(messageRestoreFailed);
			}
		}
		else
		{
			Logger.logError(messageFileNotFound);
		}
	}
}