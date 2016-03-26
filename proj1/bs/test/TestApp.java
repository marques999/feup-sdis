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
	private static String[] validCommands = {
		"BACKUP", "BACKUPENH", 
		"RESTORE", "RESTORENH",
		"DELETE", "DELETEENH", 
		"RECLAIM", "RECLAIMENH"
	};
	
	public static boolean checkCommand(final String paramType)
	{
		for (int i = 0; i < validCommands.length; i++)
		{
			if (validCommands[i].equals(paramType))
			{
				return true;
			}
		}
		
		return false;
	}

	public static void main(String[] args)
	{
		if (args.length < 3 || args.length > 4)
		{
			System.out.println("usage: TestApp <[host:]rmi_object> <sub_protocol> <opnd_1> [<opnd_2>]");
			System.exit(1);
		}

		final String rmiServer = args[0];
		final String fileId = args[2];

		int separatorPosition = rmiServer.indexOf(':');
		int replicationDegree = 0;

		InetAddress rmiAddress = null;

		if (separatorPosition == -1 || separatorPosition == 0)
		{
			try
			{
				rmiAddress = InetAddress.getLocalHost();
			}
			catch (UnknownHostException e)
			{
				Logger.abort("could not connect to localhost!");
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
				Logger.abort("you have entered an invalid IP address!");
			}
		}

		final String rmiObject = rmiServer.substring(separatorPosition + 1);
		final String paramType = args[1];
		
		if (!checkCommand(paramType))
		{
			Logger.abort("user has entered an invalid request!");
		}

		if (paramType.equals("BACKUP") || paramType.equals("BACKUPENH"))
		{
			if (args.length != 4)
			{
				Logger.abort("could not parse replication degree!");
			}

			try
			{
				replicationDegree = Integer.parseInt(args[3]);
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(ex.getMessage());
			}

			if (replicationDegree <= 0)
			{
				Logger.abort("invalid replication degree, value must be greater than zero!");
			}
		}

		Registry registry = null;
		TestStub stub = null;

		try
		{
			registry = LocateRegistry.getRegistry(rmiAddress.getHostAddress());
			Logger.logDebug("establishing connection with remote object \"" + rmiObject + "\"...");
			stub = (TestStub) registry.lookup(rmiObject);
			Logger.logDebug("connected to initiator peer, sending user request...");
		}
		catch (NotBoundException ex)
		{
			Logger.abort("remote object not registered on the target machine!");
		}
		catch (IOException ex)
		{
			Logger.abort("could not connect to target machine, is rmiregistry running?");
		}

		try
		{
			boolean remoteResult = false;
			
			Logger.logDebug("message sent, waiting for peer response...");
			
			if (paramType.equals("BACKUP"))
			{
				remoteResult = stub.backupFile(fileId, replicationDegree);
			}
			else if (paramType.equals("BACKUPENH"))
			{
				remoteResult = stub.backupEnhanced(fileId, replicationDegree);
			}
			else if (paramType.equals("DELETE"))
			{
				remoteResult = stub.deleteFile(fileId);
			}
			else if (paramType.equals("DELETEENH"))
			{
				remoteResult = stub.deleteEnhanced(fileId);
			}
			else if (paramType.equals("RESTORE"))
			{
				remoteResult = stub.restoreFile(fileId);
			}
			else if (paramType.equals("RESTOREENH"))
			{
				remoteResult = stub.restoreEnhanced(fileId);
			}
			else if (paramType.equals("RECLAIM"))
			{
				remoteResult = stub.reclaimSpace();
			}
			else if (paramType.equals("RECLAIMENH"))
			{
				remoteResult = stub.reclaimEnhanced();
			}
					
			Logger.logDebug("command returned \"" + remoteResult + "\"");
		}
		catch (RemoteException ex)
		{
			Logger.abort("could not forward the request to the initiator peer!");
		}
	}
}