package bs;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import bs.actions.ActionBackup;
import bs.actions.ActionDelete;
import bs.actions.ActionReclaim;
import bs.actions.ActionRestore;
import bs.test.TestStub;

public class InitiatorPeer implements TestStub
{
	public static void main(final String[] args)
	{
		if (PeerGlobals.checkArguments(args.length))
		{
			if (Peer.initializePeer(args, true))
			{
				initializeInitiator(args);
			}
		}
		else
		{
			System.out.println("--[ERROR]-- invalid number of arguments given, please enter the following:");
			System.out.println("    (1) InitiatorPeer <PeerId> <Host>");
			System.out.println("    (2) InitiatorPeer <PeerId> <Host> <McPort> <MdbPort> <MdrPort>");
			System.out.println("    (3) InitiatorPeer <PeerId> <McHost> <McPort> <MdbHost> <MdbPort> <MdrHost> <MdrPort>");
			System.exit(1);
		}
	}

	private static void initializeInitiator(final String[] args)
	{
		Registry registry = null;
		String objectName = Integer.toString(Peer.getPeerId());
		TestStub stub = null;

		try
		{
			registry = LocateRegistry.getRegistry();
			stub = (TestStub) UnicastRemoteObject.exportObject(new InitiatorPeer(), 0);
			Logger.logInformation(PeerStrings.messageConnecting);
			registry.bind(objectName, stub);
			Logger.logInformation(PeerStrings.messageConnected);
		}
		catch (AlreadyBoundException ex)
		{
			try
			{
				Logger.logWarning(PeerStrings.messageObjectExists);
				registry.rebind(objectName, stub);
				Logger.logInformation(String.format(PeerStrings.messageConnected, objectName));
			}
			catch (RemoteException exr)
			{
				Logger.abort(PeerStrings.messageRemoteException);
			}
		}
		catch (RemoteException ex)
		{
			Logger.abort(PeerStrings.messageRemoteException);
		}
	}

	@Override
	public boolean backupFile(final String fileId, int replicationDegree) throws RemoteException
	{
		Peer.setEnhancements(false);

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
			Logger.logInformation(PeerStrings.messageBackupDone);
		}

		return threadResult;
	}

	@Override
	public boolean restoreFile(final String fileId, boolean enableEnhancements) throws RemoteException
	{
		Peer.setEnhancements(enableEnhancements);

		if (enableEnhancements)
		{
			Peer.startUnicast();
		}

		final ActionRestore actionRestore = new ActionRestore(fileId);

		actionRestore.start();

		try
		{
			actionRestore.join();
			Peer.stopUnicast();
		}
		catch (InterruptedException ex)
		{
			return false;
		}

		boolean threadResult = actionRestore.getResult();

		if (threadResult)
		{
			Logger.logInformation(PeerStrings.messageRestoreDone);
		}

		return threadResult;
	}

	@Override
	public boolean deleteFile(String fileId, boolean enableEnhancements) throws RemoteException
	{
		Peer.setEnhancements(enableEnhancements);

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
			Logger.logInformation(PeerStrings.messageDeleteDone);
		}

		return threadResult;
	}

	@Override
	public boolean reclaimSpace(int reclaimAmount, boolean enableEnhancements) throws RemoteException
	{
		Peer.setEnhancements(enableEnhancements);

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
			Logger.logInformation(PeerStrings.messageReclaimDone);
		}

		return threadResult;
	}
}