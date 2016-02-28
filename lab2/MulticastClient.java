import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;

public class MulticastClient
{
	private static int multicastPort;
	private static int serverPort;

	private static void printCommand(String commandMessage, String responseMessage)
	{
		String[] responseLines = responseMessage.split("\n");
		String responseLine = responseLines[0].equals("-1") ? responseLines[0] : responseLines[1];
		System.out.print(String.format("(%s) sent command: %s\n",
			(new Date()).toString(), commandMessage));
		System.out.print(String.format("(%s) received reply: %s\n",
			(new Date()).toString(), responseLine));
	}

	private static void printMessage(InetAddress serviceAddress, int servicePort)
	{
		String outMessage = String.format("(%s) multicast: %s:%d, %s:%d",
			(new Date()).toString(), multicastAddress.getHostAddress(),
			multicastPort, serviceAddress.getHostAddress(), servicePort);
		System.out.println(outMessage);
	}

	private static void printUsage()
	{
		System.out.println("usage: java MulticastClient <mcast_addr> <mcast_port> <command> <arguments>*");
	}

	private static InetAddress multicastAddress;
	private static InetAddress serverAddress;

	private static boolean validateCommand(String command, int argc)
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

		try
		{
			multicastPort = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		String commandMessage = args[2];

		if (!validateCommand(commandMessage, args.length))
		{
			printUsage();
			System.exit(1);
		}

		for (int i = 3; i < args.length; i++)
		{
			commandMessage += " " + args[i];
		}

		try
		{
			multicastAddress = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		byte[] sbuf = commandMessage.getBytes();
		byte[] rbuf = new byte[1024];

		try (final MulticastSocket socket = new MulticastSocket(multicastPort))
		{
			socket.setTimeToLive(1);
			socket.joinGroup(multicastAddress);
			DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
			socket.receive(packet);
			serverAddress = packet.getAddress();
			serverPort = Integer.parseInt(new String(packet.getData(), packet.getOffset(), packet.getLength()));
			printMessage(serverAddress, serverPort);
			socket.leaveGroup(multicastAddress);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		try (final DatagramSocket socket = new DatagramSocket())
		{
			DatagramPacket packet = new DatagramPacket(sbuf, sbuf.length, serverAddress, serverPort);
			socket.send(packet);
			packet = new DatagramPacket(rbuf, rbuf.length);
			socket.receive(packet);
			printCommand(commandMessage, new String(packet.getData(), packet.getOffset(), packet.getLength()));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}
}