import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Date;

public class RMIClient
{
	private static void printUsage()
	{
		System.out.println("usage: java RMIClient <hostname> <remote_object_name> <command> <arguments>*");
	}

	private static void printConnection(final InetAddress paramAddress, final String remoteObject)
	{
		System.out.print(String.format("(%s) connected to remote server!\n",
			(new Date()).toString()));
	}

	private static void printCommand(final String commandMessage)
	{
		System.out.print(String.format("(%s) sent command: %s\n",
			(new Date()).toString(), commandMessage));
	}

	private static void printResponse(final String responseMessage)
	{
		final String[] responseLines = responseMessage.split("\n");

		if (responseLines[0].equals("-1"))
		{
			System.out.print(String.format("(%s) response status: ERROR\n",
				(new Date()).toString()));
		}
		else
		{
			System.out.print(String.format("(%s) response status: %s\n",
				(new Date()).toString(), responseLines[0]));
			System.out.print(String.format("(%s) response message: %s\n",
				(new Date()).toString(), responseLines[1]));
		}
	}

	private static boolean validateCommand(final String command, int argc)
	{
		return (command.equals("REGISTER") && argc == 5) || (command.equals("LOOKUP") && argc == 4);
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length < 4)
		{
			printUsage();
			System.exit(1);
		}

		InetAddress hostAddress = null;
		String fullCommand = args[2];

		try
		{
			hostAddress = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		if (!validateCommand(args[2], args.length))
		{
			printUsage();
			System.exit(1);
		}

		for (int i = 3; i < args.length; i++)
		{
			fullCommand += " " + args[i];
		}

		try
		{
			Registry registry = LocateRegistry.getRegistry(hostAddress.getHostAddress());
			printConnection(hostAddress, args[1]);
			RMIStub stub = (RMIStub) registry.lookup(args[1]);
			printCommand(fullCommand);
			printResponse(stub.generateResponse(fullCommand));
		}
		catch (IOException | NotBoundException ex)
		{
			ex.printStackTrace();
		}
	}
}