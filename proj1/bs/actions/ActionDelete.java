package bs.actions;

import bs.BackupSystem;
import bs.filesystem.BackupStorage;
import bs.filesystem.FileInformation;
import bs.filesystem.FileManager;
import bs.logging.Logger;

public class ActionDelete extends Thread
{
	private final FileManager fmInstance = BackupSystem.getFiles();
	private final BackupStorage bsdbInstance = BackupSystem.getStorage();
	private final String m_fileName;

	public ActionDelete(final String fileName)
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
		if (fmInstance.fileExists(m_fileName))
		{
			final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

			if (fmInstance.deleteFile(m_fileName))
			{
				Logger.logDebug("deleted " + m_fileName + " from files/ directory...");
				
				if (restoreInformation == null)
				{
					Logger.logDebug("peers in this network have not backed up this file.");
				}
				else
				{
					Logger.logDebug("file was previously backed up by some peers in this network.");
					BackupSystem.sendDELETE(restoreInformation.getFileId());
				}
			
				bsdbInstance.unregisterRestore(m_fileName);
				m_result = true;
			}
			else
			{
				Logger.logError("something bad happened while trying to delete " + m_fileName + "!");
			}
		}
		else
		{
			Logger.logError("requested file doesn't exist in the filesystem!");
		}
	}
}