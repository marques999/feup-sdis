package bs.actions;

import java.io.IOException;

import bs.BackupSystem;
import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.filesystem.ChunkBackup;
import bs.filesystem.FileInformation;
import bs.logging.Logger;

public class ActionBackup extends Thread
{
	private final String m_fileName;
	private final BackupStorage bsdbInstance = BackupSystem.getStorage();

	public ActionBackup(final String fileName, int replicationDegree)
	{
		m_fileName = fileName;
		m_degree = replicationDegree;
	}

	private int m_degree;
	private boolean m_result = false;
	
	public boolean getResult()
	{
		return m_result;
	}
	
	private boolean sendChunks(final ChunkBackup chunkBackup)
	{	
		final Chunk[] chunkArray = chunkBackup.getChunks();
		
		for (int i = 0; i < chunkArray.length; i++)
		{
			final BackupHelper currentThread = new BackupHelper(chunkArray[i]);
			
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
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

		if (restoreInformation == null)
		{
			try
			{
				final ChunkBackup chunkBackup = new ChunkBackup(m_fileName, m_degree);
				
				if (sendChunks(chunkBackup))
				{
					bsdbInstance.registerRestore(chunkBackup);
					m_result = true;
				}
				else
				{
					Logger.logError("backup thread was interrupted before it could complete!");
				}
			}
			catch (IOException ex)
			{
				Logger.logError("requested file doesn't exist on the filesystem!");
			}
		}
		else
		{
			Logger.logError("requested file has already been backed up on the network!");
		}
	}
}