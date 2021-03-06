import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class TCPClient
{
	private static void printUsage()
	{
		System.out.println("usage: java TCPClient <hostname> <port_number> <command> <arguments>*");
	}

	private static void printConnection(final InetAddress paramAddress, int paramPort)
	{
		System.out.print(String.format("(%s) connected to %s:%d\n",
			(new Date()).toString(), paramAddress.getHostAddress(), paramPort));
	}

	private static void printCommand(final String commandMessage)
	{
		System.out.print(String.format("(%s) sent command: %s\n",
			(new Date()).toString(), commandMessage));
	}

	private static void printResponse(final String responseMessage)
	{
		String[] responseLines = responseMessage.split("\n");
		String responseLine = responseLines[0].equals("-1") ? "ERROR" : responseLines[1];
		System.out.print(String.format("(%s) received response: %s\n",
			(new Date()).toString(), responseLine));
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

		int bytesRead = 0;
		int hostPort = 0;

		try
		{
			hostPort = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		if (!validateCommand(args[2], args.length))
		{
			printUsage();
			System.exit(1);
		}

		String fullCommand = args[2];
		InetAddress hostAddress = null;

		for (int i = 3; i < args.length; i++)
		{
			fullCommand += " " + args[i];
		}

		try
		{
			hostAddress = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		final char[] cbuf = new char[1024];

		try (
			final Socket serverSocket = new Socket(hostAddress, hostPort);
			final PrintWriter socketOutput = new PrintWriter(serverSocket.getOutputStream(), true);
			final InputStreamReader socketStream = new InputStreamReader(serverSocket.getInputStream());
			final BufferedReader socketInput = new BufferedReader(socketStream)
			)
		{
			printConnection(hostAddress, hostPort);
			socketOutput.println(fullCommand);
			printCommand(fullCommand);
			bytesRead = socketInput.read(cbuf);
			printResponse(new String(cbuf, 0, bytesRead));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}
}