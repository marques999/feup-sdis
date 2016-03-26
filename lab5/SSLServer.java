import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class SSLServer
{
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

	private static String generateResponse(final String command)
	{
		String[] args = command.split(" ");
		String response = "";

		int returnCode = -1;

		if (args[0].equals("LOOKUP") && args.length >= 2)
		{
			response = lookupPlate(command.replaceFirst(
				String.format("%s ", args[0]), ""));
		}
		else if (args[0].equals("REGISTER") && args.length >= 3)
		{
			response = registerPlate(args[1], command.replaceFirst(
				String.format("%s %s ", args[0], args[1]), ""));
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
			System.out.println("usage: java SSLServer <port_number> <cypher_suite>*");
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

		final InetAddress serverAddress = InetAddress.getLocalHost();
		final SSLServerSocketFactory serverFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		final SSLServerSocket serverSocket = (SSLServerSocket) serverFactory.createServerSocket(serverPort);

		printConnection(serverAddress, serverPort);

		for (;;)
		{
			try (
				final SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
				final InputStreamReader socketStream = new InputStreamReader(clientSocket.getInputStream());
				final PrintWriter socketOutput = new PrintWriter(clientSocket.getOutputStream(), false);
				final BufferedReader socketInput = new BufferedReader(socketStream)
				)
			{
				String request = socketInput.readLine();
				printRequest(request, clientSocket.getInetAddress(), clientSocket.getPort());
				socketOutput.print(generateResponse(request));
				socketOutput.flush();
				socketOutput.close();
				socketInput.close();
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
				serverSocket.close();
			}
		}
	}
}