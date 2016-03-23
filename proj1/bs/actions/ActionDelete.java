package bs.actions;

import bs.BackupSystem;
import bs.filesystem.BackupStorage;
import bs.filesystem.FileInformation;
import bs.filesystem.FileManager;
import bs.server.ProtocolCommand;

public class ActionDelete extends Thread
{
	private final String m_fileName;
	private final ProtocolCommand m_commandChannel;
	private final BackupStorage bsdbInstance;
	private final FileManager fmInstance;
	private final String msgPeersHaveChunks;
	private final String msgNoChunksFound;

	public ActionDelete(final String fileName, final ProtocolCommand mc)
	{
		m_fileName = fileName;
		m_commandChannel = mc;
		m_result = false;
		fmInstance = BackupSystem.getFiles();
		bsdbInstance = BackupSystem.getStorage();
		msgPeersHaveChunks = String.format("[INFORMATION] file %s was previously backed up by some peers in this network.\n", m_fileName);
		msgNoChunksFound = String.format("[INFORMATION] no peers in this network seem to have backed up the file %s.\n",	m_fileName);
	}
	
	private boolean m_result;
	
	public boolean getResult()
	{
		return m_result;
	}

	@Override
	public void run()
	{
		if (fmInstance.fileExists(m_fileName))
		{
			fmInstance.deleteFile(m_fileName);
		}

		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

		if (restoreInformation != null)
		{
			System.out.print(msgPeersHaveChunks);
			m_commandChannel.sendDELETE(restoreInformation.getFileId());
			bsdbInstance.unregisterRestore(m_fileName);
			m_result = true;
		}
		else
		{
			m_result = false;
			System.out.print(msgNoChunksFound);
		}
	}
}