package bs.actions;

import java.util.Random;

import bs.BackupGlobals;
import bs.BackupSystem;
import bs.RestoreService;
import bs.filesystem.BackupStorage;
import bs.filesystem.ChunkRestore;
import bs.filesystem.FileInformation;
import bs.filesystem.FileManager;
import bs.logging.Logger;
import bs.logging.ProgramException;

public class ActionRestore extends Thread
{
	private final String m_fileName;
	private final Random m_random = new Random();
	private final FileManager fmInstance = BackupSystem.getFiles();
	private final BackupStorage bsdbInstance = BackupSystem.getStorage();

	public ActionRestore(final String fileName)
	{
		m_fileName = fileName;
	}
	
	private boolean m_result = false;
	
	public boolean getResult()
	{
		return m_result;
	}

	@Override
	public void run()
	{
		final RestoreService restoreService = BackupSystem.getRestoreService();
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

		if (restoreInformation != null)
		{
			final ChunkRestore recoveredChunks = new ChunkRestore(restoreInformation);
			final String fileId = restoreInformation.getFileId();

			restoreService.startReceivingChunks(fileId);
			
			int currentChunk = 0;
			int numberChunks = restoreInformation.getCount();

			for (int i = 0; i < numberChunks; i++)
			{
				BackupSystem.sendGETCHUNK(fileId, i);
				
				try
				{
					Thread.sleep(m_random.nextInt(BackupGlobals.maximumBackoffTime));
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
				if (fmInstance.writeFile(m_fileName, recoveredChunks.join()))
				{
					bsdbInstance.unregisterRestore(m_fileName);
					m_result = true;
				}
			}
			catch (ProgramException ex)
			{
				ex.printMessage();
				m_result = false;
			}
		}
		else
		{
			Logger.logError("requested file was not found in the network, cannot restore!");
		}
	}
}