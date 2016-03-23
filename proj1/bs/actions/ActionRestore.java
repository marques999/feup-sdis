package bs.actions;

import bs.BackupSystem;
import bs.filesystem.BackupStorage;
import bs.filesystem.BadChunkException;
import bs.filesystem.Chunk;
import bs.filesystem.ChunkRestore;
import bs.filesystem.FileInformation;
import bs.filesystem.FileManager;
import bs.server.ProtocolCommand;
import bs.server.ProtocolRestore;

public class ActionRestore extends Thread
{
	private final String m_fileName;
	private final ProtocolRestore m_restoreChannel;
	private final ProtocolCommand m_commandChannel;
	private final BackupStorage bsdbInstance;
	private final FileManager fmInstance;
	private final String msgFileNotFound;

	public ActionRestore(final String fileName, final ProtocolCommand mc, final ProtocolRestore mdr)
	{
		m_fileName = fileName;
		m_result = false;
		m_restoreChannel = mdr;
		m_commandChannel = mc;
		fmInstance = BackupSystem.getFiles();
		bsdbInstance = BackupSystem.getStorage();
		msgFileNotFound = String.format("[ERROR] The requested file was not found in the network and cannot be restored.\n", m_fileName);
	}
	
	private boolean m_result;
	
	public boolean getResult()
	{
		return m_result;
	}

	@Override
	public void run()
	{
		final FileInformation restoreInformation = bsdbInstance.getRestoreInformation(m_fileName);

		if (restoreInformation != null)
		{
			final ChunkRestore recoveredChunks = new ChunkRestore(restoreInformation);
			final String fileId = restoreInformation.getFileId();

			m_restoreChannel.subscribeChunks(fileId);

			for (int i = 0; i < restoreInformation.getCount(); i++)
			{
				m_commandChannel.sendGETCHUNK(fileId, i);
				recoveredChunks.put(m_restoreChannel.getChunk(fileId, i));
			}

			m_restoreChannel.unsubscribeChunks(fileId);

			try
			{
				m_result = fmInstance.writeFile(m_fileName, recoveredChunks.join());
			}
			catch (BadChunkException ex)
			{
				ex.printMessage();
				m_result = false;
			}
		}
		else
		{
			System.out.print(msgFileNotFound);
			m_result = false;
		}
	}
}