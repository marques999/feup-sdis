package bs.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

public class TestApp
{
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
				System.err.println("[error@main:38] could not connect to localhost!");
				System.exit(1);
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
				System.err.println("[error@main:52] user has given an invalid IP address!");
				System.exit(1);
			}
		}

		final String rmiObject = rmiServer.substring(separatorPosition + 1);
		final String paramType = args[1];

		System.out.println("[information@main:60] ADDRESS: [" + rmiAddress.getHostAddress()+ "] OBJECT: [" + rmiObject + "]");

		if (paramType.equals("BACKUP") || paramType.equals("BACKUPENH"))
		{
			if (args.length != 4)
			{
				System.err.println("[error@main:66] invalid number of arguments, no replication degree given!");
				System.exit(1);
			}

			try
			{
				replicationDegree = Integer.parseInt(args[3]);
			}
			catch (NumberFormatException ex)
			{
				System.err.println(ex.getMessage());
				System.exit(1);
			}

			if (replicationDegree <= 0)
			{
				System.err.println("[error@main:82] invalid replication degree, must be greater than 0!");
				System.exit(1);
			}
		}

		Registry registry = null;
		TestStub stub = null;

		try
		{
			registry = LocateRegistry.getRegistry(rmiAddress.getHostAddress());
			System.out.print(String.format("(%s) establishing connection with remote server...\n",(new Date()).toString()));
			stub = (TestStub) registry.lookup(args[1]);
		}
		catch (NotBoundException | IOException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}

		try
		{
			if (paramType.equals("BACKUP"))
			{
				stub.backupFile(fileId, replicationDegree);
			}
			else if (paramType.equals("BACKUPENH"))
			{
				stub.backupEnhanced(fileId, replicationDegree);
			}
			else if (paramType.equals("DELETE"))
			{
				stub.deleteFile(fileId);
			}
			else if (paramType.equals("DELETEENH"))
			{
				stub.deleteEnhanced(fileId);
			}
			else if (paramType.equals("RESTORE"))
			{
				stub.restoreFile(fileId);
			}
			else if (paramType.equals("RESTOREENH"))
			{
				stub.restoreEnhanced(fileId);
			}
			else if (paramType.equals("RECLAIM"))
			{
				stub.reclaimSpace();
			}
			else if (paramType.equals("RECLAIMENH"))
			{
				stub.reclaimEnhanced();
			}
			else
			{
				System.err.println("[error@main:138] user has entered an invalid command!");
				System.exit(1);
			}
		}
		catch (RemoteException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}
}