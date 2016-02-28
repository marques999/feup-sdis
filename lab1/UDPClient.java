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
		System.out.println("usage: java UDPClient <hostname> <port_number> <command> <arguments>*");
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
		String responseLine = responseLines[0].equals("-1") ? responseLines[0] : responseLines[1];
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

		String fullCommand = args[2];
		InetAddress hostAddress = null;
		DatagramPacket packet = null;

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
			hostAddress = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		byte[] sbuf = fullCommand.getBytes();
		byte[] rbuf = new byte[1024];

		try (final DatagramSocket socket = new DatagramSocket())
		{
			packet = new DatagramPacket(sbuf, sbuf.length, hostAddress, hostPort);
			printConnection(hostAddress, hostPort);
			socket.send(packet);
			printCommand(fullCommand);
			packet = new DatagramPacket(rbuf, rbuf.length);
			socket.receive(packet);
			printResponse(new String(packet.getData(), packet.getOffset(), packet.getLength()));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}
}