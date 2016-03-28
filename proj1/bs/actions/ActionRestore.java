package bs.actions;

import bs.BackupSystem;
import bs.RestoreService;
import bs.filesystem.ChunkRestore;
import bs.filesystem.FileInformation;
import bs.logging.Logger;
import bs.logging.ProgramException;

public class ActionRestore extends Action
{
	private static final String messageWriteFailed = "could not write received chunks to output file!";
	private static final String messageFileNotFound = "requested file was not found in the network, cannot restore!";
	private final String m_fileName;

	public ActionRestore(final String fileName)
	{
		m_fileName = fileName;
	}

	@Override
	public void run()
	{
		final RestoreService restoreService = BackupSystem.getRestoreService();
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

		if (restoreInformation != null)
		{
			int currentChunk = 0;
			int numberChunks = restoreInformation.getCount();
			final ChunkRestore recoveredChunks = new ChunkRestore(restoreInformation);
			final String fileId = restoreInformation.getFileId();

			restoreService.startReceivingChunks(fileId);

			for (int i = 0; i < numberChunks; i++)
			{
				BackupSystem.sendGETCHUNK(fileId, i);

				try
				{
					Thread.sleep(generateBackoff());
				}
				catch (InterruptedException ex)
				{
					ex.printStackTrace();
				}
			}

			while (currentChunk != numberChunks)
			{
				if (recoveredChunks.put(restoreService.retrieveChunk(fileId, currentChunk)))
				{
					currentChunk++;
				}
			}

			restoreService.stopReceivingChunks(fileId);

			try
			{
				actionResult = fmInstance.writeFile(m_fileName, recoveredChunks.join());

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
			Logger.logError(messageFileNotFound);
		}
	}
}