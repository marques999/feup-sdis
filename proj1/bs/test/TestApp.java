package bs.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import bs.logging.Logger;

public class TestApp
{
	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			Logger.abort(TestStrings.messageInvalidArguments);
		}

		final String rmiServer = args[0];
		final String paramType = args[1];

		int separatorPosition = rmiServer.indexOf(':');
		int reclaimAmount = 0;
		int replicationDegree = 0;

		if (paramType.equals("BACKUP") || paramType.equals("BACKUPENH"))
		{
			if (args.length != 4)
			{
				Logger.abort(TestStrings.messageInvalidBackupArguments);
			}

			try
			{
				replicationDegree = Integer.parseInt(args[3]);

				if (replicationDegree <= 0)
				{
					Logger.abort(TestStrings.messageInvalidReplicationDegree);
				}
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(TestStrings.messageInvalidReplicationDegree);
			}
		}
		else if (paramType.equals("RESTORE") || paramType.equals("RESTOREENH"))
		{
			if (args.length != 3)
			{
				Logger.abort(TestStrings.messageInvalidRestoreArguments);
			}
		}
		else if (paramType.equals("DELETE") || paramType.equals("DELETEENH"))
		{
			if (args.length != 3)
			{
				Logger.abort(TestStrings.messageInvalidDeleteArguments);
			}
		}
		else if (paramType.equals("RECLAIM") || paramType.equals("RECLAIMENH"))
		{
			if (args.length != 3)
			{
				Logger.abort(TestStrings.messageInvalidReclaimArguments);
			}

			try
			{
				reclaimAmount = Integer.parseInt(args[2]);

				if (reclaimAmount <= 0)
				{
					Logger.abort(TestStrings.messageInvalidReclaimAmount);
				}
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(TestStrings.messageInvalidReclaimAmount);
			}
		}
		else
		{
			Logger.abort(TestStrings.messageInvalidArguments);
		}

		final String rmiObject = rmiServer.substring(separatorPosition + 1);

		try
		{
			int objectName = Integer.parseInt(rmiObject);

			if (objectName < 0 || objectName > Short.MAX_VALUE)
			{
				Logger.abort(TestStrings.messageInvalidRemoteObject);
			}
		}
		catch (NumberFormatException ex)
		{
			Logger.abort(TestStrings.messageInvalidRemoteObject);
		}

		String firstOperand = args[2];
		InetAddress rmiAddress = null;
		Registry registry = null;
		TestStub stub = null;

		if (separatorPosition == -1 || separatorPosition == 0)
		{
			try
			{
				rmiAddress = InetAddress.getLocalHost();
			}
			catch (UnknownHostException ex)
			{
				Logger.abort(TestStrings.messageLocalhost);
			}
		}
		else
		{
			final String ipAddress = rmiServer.substring(0, separatorPosition);

			try
			{
				rmiAddress = InetAddress.getByName(ipAddress);
			}
			catch (UnknownHostException ex)
			{
				Logger.abort(TestStrings.messageInvalidAddress);
			}
		}

		try
		{
			registry = LocateRegistry.getRegistry(rmiAddress.getHostAddress());
			Logger.logInformation("establishing connection with initiator peer " + rmiObject + "...");
			stub = (TestStub) registry.lookup(rmiObject);
			Logger.logInformation(TestStrings.messageConnected);
		}
		catch (NotBoundException ex)
		{
			Logger.abort(TestStrings.messageNotBoundException);
		}
		catch (IOException ex)
		{
			Logger.abort(TestStrings.messageRemoteException);
		}

		try
		{
			boolean remoteResult = false;
			boolean enhancedCommand = paramType.endsWith("ENH");

			Logger.logInformation(TestStrings.messageSentCommand);

			if (paramType.startsWith("BACKUP"))
			{
				remoteResult = stub.backupFile(firstOperand, replicationDegree, enhancedCommand);
			}
			else if (paramType.startsWith("DELETE"))
			{
				remoteResult = stub.deleteFile(firstOperand, enhancedCommand);
			}
			else if (paramType.startsWith("RESTORE"))
			{
				remoteResult = stub.restoreFile(firstOperand, enhancedCommand);
			}
			else if (paramType.startsWith("RECLAIM"))
			{
				remoteResult = stub.reclaimSpace(reclaimAmount, enhancedCommand);
			}

			Logger.logInformation("command returned \"" + remoteResult + "\"");
		}
		catch (RemoteException ex)
		{
			Logger.abort(TestStrings.messageCommandError);
		}
	}
}