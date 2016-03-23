package bs.actions;

import java.io.IOException;

import bs.BackupSystem;
import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.filesystem.ChunkBackup;
import bs.filesystem.FileInformation;
import bs.filesystem.FileManager;

public class ActionBackup extends Thread
{
	private final String m_fileName;
	private final BackupStorage bsdbInstance;
	private final FileManager fmInstance;

	public ActionBackup(final String fileName, int replicationDegree)
	{
		m_fileName = fileName;
		m_result = false;
		m_degree = replicationDegree;
		fmInstance = BackupSystem.getFiles();
		bsdbInstance = BackupSystem.getStorage();
	}

	private int m_degree;
	private boolean m_result;
	
	public boolean getResult()
	{
		return m_result;
	}
	
	private void sendChunks() throws IOException
	{
		final ChunkBackup chunkBackup = new ChunkBackup(m_fileName, m_degree);
		final String fileId = chunkBackup.getFileId();
		final Chunk[] chunkArray = chunkBackup.getChunks();
		
		for (int i = 0; i < chunkArray.length; i++)
		{
			BackupHelper currentThread = new BackupHelper(chunkArray[i]);
			currentThread.start();

			/*
			 * TODO can chunks be backed up in parallel? not working for big files...
			 */
			try
			{
				currentThread.join();
			}
			catch (InterruptedException ex)
			{
				m_result = false;
				ex.printStackTrace();
			}
		}
		
		bsdbInstance.registerRestore(chunkBackup);
		m_result = true;
	}

	@Override
	public void run()
	{
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

		if (restoreInformation == null)
		{
			try
			{
				sendChunks();
			}
			catch (IOException ex)
			{
				m_result = false;
				System.out.println(ex.getMessage());
			}
		}
		else
		{
			System.out.println("[INFORMATION] The requested file has already been backed up on the network.");
			m_result = false;
		}
	}
}