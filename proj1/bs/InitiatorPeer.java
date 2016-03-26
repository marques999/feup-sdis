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
	private final static String messageConnected = "connected to initiator service, listening for commands!";
	private final static String messageRemoteException = "could not bind object, is rmiregistry running?";
	private static final String messageBackupDone = "backup action sucessfully completed!";
	private static final String messageRestoreDone = "restore action sucessfully completed!";
	private static final String messageDeleteDone = "file successfully deleted from the network!";
	private static final String messageReclaimDone = "redundant chunks successfully reclaimed!";
	
	public static void main(final String[] args) throws IOException
	{
		if (!BackupGlobals.checkInitiatorArguments(args.length))
		{
			System.out.println("usage: BackupSystem <Host> <PeerId> [<McPort> <MdbPort> <MdrPort>]");
			System.exit(1);
		}
		
		BackupSystem.initializePeer(Arrays.copyOfRange(args, 1, args.length));
		String objectName = "initiator";
		Registry registry = null;
		TestStub stub = null;;

		if (args.length > BackupGlobals.minimumInitArguments)
		{
			objectName = args[0];
		}

		try
		{
			Logger.logDebug("remote object name -> \"" + objectName + "\"");
			registry = LocateRegistry.getRegistry();
			stub = (TestStub) UnicastRemoteObject.exportObject(new InitiatorPeer(), 0);
			Logger.logDebug("connecting to rmiregistry server...");
			registry.bind(objectName, stub);
			Logger.logError(messageConnected);
		}
		catch (AlreadyBoundException ex)
		{
			try
			{
				Logger.logDebug("remote object already exists, rebinding...");
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
	}
	
	@Override
	public boolean backupFile(final String fileId, int replicationDegree) throws RemoteException
	{
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
	public boolean restoreFile(final String fileId) throws RemoteException
	{
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
	public boolean deleteFile(String fileId) throws RemoteException
	{
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
	public boolean reclaimSpace() throws RemoteException
	{
		final ActionReclaim actionReclaim = new ActionReclaim(2048000);
		
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
	
	@Override
	public boolean backupEnhanced(String fileId, int replicationDegree) throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean restoreEnhanced(String fileId) throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean deleteEnhanced(String fileId) throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean reclaimEnhanced() throws RemoteException
	{
		return false;
	}
}