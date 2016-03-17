import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;

public class RMIServer implements RMIStub
{
	private HashMap<String, String> lookupTable;

	public RMIServer()
	{
		lookupTable = new HashMap<String, String>();
	}

	private final String registerPlate(final String paramPlate, final String paramOwner)
	{
		if (lookupTable.containsKey(paramPlate))
		{
			return "";
		}

		lookupTable.put(paramPlate, paramOwner);

		return paramOwner;
	}

	private final String lookupPlate(final String paramPlate)
	{
		if (!lookupTable.containsKey(paramPlate))
		{
			return "";
		}

		return lookupTable.get(paramPlate);
	}

	private final void printReply(final String replyMessage)
	{
		System.out.print(String.format("(%s) sent reply: %s\n",
			(new Date()).toString(), replyMessage));
	}

	public String generateResponse(final String requestCommand)
	{
		System.out.print(String.format("(%s) received command: %s\n",
				(new Date()).toString(), requestCommand));

		String[] args = requestCommand.split(" ");
		String response = "";
		String replace = "";

		int returnCode = -1;

		if (args[0].equals("LOOKUP") && args.length >= 2)
		{
			replace = String.format("%s ", args[0]);
			response = lookupPlate(requestCommand.replaceFirst(replace, ""));
		}
		else if (args[0].equals("REGISTER") && args.length >= 3)
		{
			replace = String.format("%s %s ", args[0], args[1]);
			response = registerPlate(args[1], requestCommand.replaceFirst(replace, ""));
		}

		if (response.isEmpty())
		{
			printReply(Integer.toString(returnCode));
		}
		else
		{
			response = String.format("(%s, %s)", args[1], response);
			returnCode = lookupTable.size();
			printReply(response);
		}

		return returnCode + "\n" + response;
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length != 1)
		{
			System.out.println("usage: java RMIServer <remote_object_name>");
			System.exit(1);
		}

		final String remoteObject = args[0];
		final RMIServer obj = new RMIServer();

		try
		{
			RMIStub stub = (RMIStub) UnicastRemoteObject.exportObject(obj, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(remoteObject, stub);
			System.out.print(String.format("(%s) server running at %s...\n",
				(new Date()).toString(), InetAddress.getLocalHost().getHostAddress()));
		}
		catch (IOException | AlreadyBoundException ex)
		{
			ex.printStackTrace();
		}
	}
}