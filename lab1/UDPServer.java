import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;

public class UDPServer
{
	private static void printUsage()
	{
		System.out.println("usage: java UDPServer <port_number>");
	}

	private static void printConnection(final InetAddress host, int port)
	{
		System.out.print(String.format("(%s) server running at %s:%d...\n",
			(new Date()).toString(), host.getHostAddress(), port));
	}

	private static void printReply(final String replyMessage)
	{
		System.out.print(String.format("(%s) sent reply: %s\n",
			(new Date()).toString(), replyMessage));
	}

	private static void printRequest(final String requestMessage, final InetAddress host, int port)
	{
		System.out.print(String.format("(%s) client connected from %s:%d\n",
			(new Date()).toString(), host.getHostAddress(), port));
		System.out.print(String.format("(%s) received command: %s\n",
			(new Date()).toString(), requestMessage));
	}

	private static HashMap<String, String> lookupTable = new HashMap<>();

	private static String registerPlate(final String paramPlate, final String paramOwner)
	{
		if (lookupTable.containsKey(paramPlate))
		{
			return "";
		}

		lookupTable.put(paramPlate, paramOwner);

		return paramOwner;
	}

	private static String lookupPlate(final String paramPlate)
	{
		if (!lookupTable.containsKey(paramPlate))
		{
			return "";
		}

		return lookupTable.get(paramPlate);
	}

	private static byte[] generateResponse(final String command)
	{
		String[] args = command.split(" ");
		String response = "";
		String replace = "";

		int returnCode = -1;

		if (args[0].equals("LOOKUP") && args.length >= 2)
		{
			replace = String.format("%s ", args[0]);
			response = lookupPlate(command.replaceFirst(replace, ""));
		}
		else if (args[0].equals("REGISTER") && args.length >= 3)
		{
			replace = String.format("%s %s ", args[0], args[1]);
			response = registerPlate(args[1], command.replaceFirst(replace, ""));
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

		return (returnCode + "\n" + response).getBytes();
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length != 1)
		{
			printUsage();
			System.exit(1);
		}

		int serverPort = 0;

		try
		{
			serverPort = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		DatagramSocket socket = new DatagramSocket(serverPort);
		InetAddress serverAddress = InetAddress.getLocalHost();
		printConnection(serverAddress, serverPort);

		for (;;)
		{
			byte[] rbuf = new byte[1024];

			try
			{
				// get request
				DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
				socket.receive(packet);
				// display request
				String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
				printRequest(received, packet.getAddress(), packet.getPort());
				// generate response
				byte[] sbuf = generateResponse(received);
				// send response
				socket.send(new DatagramPacket(sbuf, sbuf.length, packet.getAddress(), packet.getPort()));
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
				socket.close();
			}
		}
	}
}