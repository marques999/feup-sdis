package bs;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

import bs.actions.ActionBackup;
import bs.actions.ActionDelete;
import bs.actions.ActionReclaim;
import bs.actions.ActionRestore;

import bs.logging.Logger;

import bs.test.TestStub;

public class InitiatorPeer implements TestStub
{
	private static final String messageConnecting = "connecting to rmiregistry server...";
	private static final String messageConnected = "connected to initiator service, listening for commands!";
	private static final String messageObjectExists = "remote object already exists, rebinding...";
	private static final String messageProgramUsage = "BackupSystem <Host> <PeerId> [<McPort> <MdbPort> <MdrPort>]";
	private static final String messageRemoteException = "could not bind object, is rmiregistry running?";
	private static final String messageBackupDone = "backup action sucessfully completed!";
	private static final String messageRestoreDone = "restore action sucessfully completed!";
	private static final String messageDeleteDone = "file successfully deleted from the network!";
	private static final String messageReclaimDone = "redundant chunks successfully reclaimed!";
	
	public static void main(final String[] args) throws IOException
	{
		if (!BackupGlobals.checkInitiatorArguments(args.length))
		{
			Logger.abort(messageProgramUsage);
		}
		
		String objectName = "1234";
		Registry registry = null;
		TestStub stub = null;

		if (args.length > BackupGlobals.minimumInitArguments)
		{
			try
			{
				Integer.parseInt(args[0]);
				objectName = args[0];
			}
			catch (NumberFormatException ex)
			{
				
			}
		}

		try
		{
			Logger.logDebug("remote object name -> \"" + objectName + "\"");
			registry = LocateRegistry.getRegistry();
			stub = (TestStub) UnicastRemoteObject.exportObject(new InitiatorPeer(), 0);
			Logger.logDebug(messageConnecting);
			registry.bind(objectName, stub);
			Logger.logError(messageConnected);
		}
		catch (AlreadyBoundException ex)
		{
			try
			{
				Logger.logDebug(messageObjectExists);
				registry.rebind(objectName, stub);
				Logger.logDebug(String.format(messageConnected, objectName));
			}
			catch (RemoteException exr)
			{
				Logger.abort(messageRemoteException);
			}
		}
		catch (RemoteException ex)
		{
			Logger.abort(messageRemoteException);
		}

		BackupSystem.initializePeer(Arrays.copyOfRange(args, 1, args.length));
	}

	@Override
	public boolean backupFile(final String fileId, int replicationDegree, boolean enableEnhancements) throws RemoteException
	{
		BackupSystem.setEnhancements(enableEnhancements);

		final ActionBackup actionBackup = new ActionBackup(fileId, replicationDegree);
		
		actionBackup.start();
		
		try
		{
			actionBackup.join();	
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		boolean threadResult = actionBackup.getResult();
		
		if (threadResult)
		{
			Logger.logDebug(messageBackupDone);
		}
		
		return threadResult;
	}

	@Override
	public boolean restoreFile(final String fileId, boolean enableEnhancements) throws RemoteException
	{
		BackupSystem.setEnhancements(enableEnhancements);

		final ActionRestore actionRestore = new ActionRestore(fileId);
		
		actionRestore.start();
		
		try
		{
			actionRestore.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		boolean threadResult = actionRestore.getResult();
		
		if (threadResult)
		{
			Logger.logDebug(messageRestoreDone);
		}

		return threadResult;
	}

	@Override	
	public boolean deleteFile(String fileId, boolean enableEnhancements) throws RemoteException
	{
		BackupSystem.setEnhancements(enableEnhancements);

		final ActionDelete actionDelete = new ActionDelete(fileId);
		
		actionDelete.start();
		
		try
		{
			actionDelete.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		boolean threadResult = actionDelete.getResult();
	
		if (threadResult)
		{
			Logger.logDebug(messageDeleteDone);
		}
		
		return threadResult;
	}

	@Override
	public boolean reclaimSpace(int reclaimAmount, boolean enableEnhancements) throws RemoteException
	{
		BackupSystem.setEnhancements(enableEnhancements);

		final ActionReclaim actionReclaim = new ActionReclaim(reclaimAmount);
		
		actionReclaim.start();
		
		try
		{
			actionReclaim.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		boolean threadResult = actionReclaim.getResult();
		
		if (threadResult)
		{
			Logger.logDebug(messageReclaimDone);
		}
		
		return threadResult;
	}
}