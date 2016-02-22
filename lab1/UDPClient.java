import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class UDPClient
{
	private static void printUsage()
	{
		System.out.println("ERROR: invalid arguments, usage: java UDPClient <hostname> <port_number> <command> <arguments>*");
	}

	private static boolean validateCommand(final String command, int argc)
	{
		return (command.equals("REGISTER") && argc == 5) || (command.equals("LOOKUP") && argc == 4);
	}

	private static void logMessage(final String message)
	{
		System.out.println("(" + (new Date()).toString() + ") " + message);
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length < 4)
		{
			printUsage();
			System.exit(0);
		}

		int hostPort = 0;

		try
		{
			hostPort = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException e)
		{
			printUsage();
			System.exit(0);
		}

		String fullCommand = args[2];

		if (!validateCommand(args[2], args.length))
		{
			printUsage();
			System.exit(0);
		}

		for (int i = 3; i < args.length; i++)
		{
			fullCommand = fullCommand.concat(" " + args[i]);
		}

		DatagramSocket socket = new DatagramSocket();
		InetAddress hostAddress = InetAddress.getLocalHost();

		try
		{
			hostAddress = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException e)
		{
			System.out.println(e.getMessage());
			socket.close();
		}

		logMessage(String.format("Connected to %s:%s...", hostAddress.getHostAddress(), args[1]));
		logMessage("REQUEST: " + fullCommand);

		byte[] sbuf = fullCommand.getBytes();
		byte[] rbuf = new byte[1024];

		try
		{
			DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, hostAddress, hostPort);
			socket.send(packet);
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
			socket.close();
		}

		try
		{
			DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
			socket.receive(packet);
			logMessage("RESPONSE: " + new String(packet.getData()).trim());
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
		finally
		{
			socket.close();
		}
	}
}