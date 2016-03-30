package bs.actions;

import java.util.Random;

import bs.PeerGlobals;
import bs.Peer;
import bs.filesystem.BackupStorage;
import bs.filesystem.FileManager;

public abstract class Action extends Thread
{
	protected final BackupStorage bsdbInstance = Peer.getStorage();
	protected final FileManager fmInstance = Peer.getFiles();
	protected final Random myRandom = new Random();

	protected int generateBackoff()
	{
		return myRandom.nextInt(PeerGlobals.maximumBackoffTime);
	}

	protected boolean actionResult = false;

	public boolean getResult()
	{
		return actionResult;
	}
}