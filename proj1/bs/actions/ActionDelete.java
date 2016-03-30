package bs.actions;

import bs.PeerGlobals;
import bs.Peer;
import bs.filesystem.FileInformation;
import bs.logging.Logger;

public class ActionDelete extends Action
{
	private final String m_fileName;

	public ActionDelete(final String fileName)
	{
		m_fileName = fileName;
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

					for (int i = 0; i < PeerGlobals.maximumAttempts; i++)
					{
						Peer.sendDELETE(restoreInformation.getFileId());

						try
						{
							Thread.sleep(PeerGlobals.maximumBackoffTime);
						}
						catch (InterruptedException ex)
						{
							ex.printStackTrace();
						}
					}
				}

				actionResult = bsdbInstance.unregisterRestore(m_fileName);
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