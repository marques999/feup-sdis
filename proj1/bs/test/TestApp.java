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
	private static final String messageProgramUsage = "TestApp <[host:]rmi_object> <sub_protocol> <opnd_1> [<opnd_2>]";
	private static final String messageInvalidRemote = "invalid remote object name, value must be greater than zero!";
	private final static String messageInvalidDegree = "invalid replication degree, value must be greater than zero!";
	private final static String messageInvalidReclaim = "invalid reclaim amount, value must be greater than zero!";

	private static String[] validCommands = {
		"BACKUP", "BACKUPENH", "RESTORE", "RESTOREENH",
		"DELETE", "DELETEENH", "RECLAIM", "RECLAIMENH"
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
		if (args.length < 2)
		{
			Logger.abort(messageProgramUsage);
		}

		final String rmiServer = args[0];
		final String paramType = args[1];

		if (!checkCommand(paramType))
		{
			Logger.abort("command type (BACKUP|RESTORE|DELETE|RECLAIM) not recognized!");
		}

		int separatorPosition = rmiServer.indexOf(':');
		int reclaimAmount = 0;
		int replicationDegree = 0;

		final String rmiObject = rmiServer.substring(separatorPosition + 1);

		try
		{
			Integer.parseInt(rmiObject);
		}
		catch (NumberFormatException ex)
		{
			Logger.abort(messageInvalidRemote);
		}

		if (paramType.startsWith("BACKUP"))
		{
			if (args.length != 4)
			{
				Logger.abort(messageProgramUsage);
			}

			try
			{
				replicationDegree = Integer.parseInt(args[3]);

				if (replicationDegree <= 0)
				{
					Logger.abort(messageInvalidDegree);
				}
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(messageInvalidDegree);
			}	
		}
		else if (paramType.startsWith("RECLAIM"))
		{
			if (args.length != 3)
			{
				Logger.abort(messageProgramUsage);
			}

			try
			{
				reclaimAmount = Integer.parseInt(args[2]);

				if (reclaimAmount <= 0)
				{
					Logger.abort(messageInvalidReclaim);
				}
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(messageInvalidReclaim);
			}
		}
		else
		{
			if (args.length != 3)
			{
				Logger.abort(messageProgramUsage);
			}
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

		try
		{
			registry = LocateRegistry.getRegistry(rmiAddress.getHostAddress());
			Logger.logInformation("establishing connection with remote object \"" + rmiObject + "\"...");
			stub = (TestStub) registry.lookup(rmiObject);
			Logger.logInformation("connected to initiator peer, sending user request...");
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
			boolean enhancedCommand = paramType.endsWith("ENH");

			Logger.logInformation("message sent, waiting for peer response...");

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
			Logger.abort("could not forward the request to the initiator peer!");
		}
	}
}