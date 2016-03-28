package bs.actions;

import java.util.Random;

import bs.BackupGlobals;
import bs.BackupSystem;
import bs.filesystem.BackupStorage;
import bs.filesystem.FileManager;

public abstract class Action extends Thread
{
	protected final BackupStorage bsdbInstance = BackupSystem.getStorage();
	protected final FileManager fmInstance = BackupSystem.getFiles();
	private final Random myRandom = new Random();
	
	protected final int generateBackoff()
	{
		return myRandom.nextInt(BackupGlobals.maximumBackoffTime);
	}
	
	protected boolean actionResult = false;
	
	public boolean getResult()
	{
		return actionResult;
	}
}