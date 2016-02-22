import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;

public class UDPServer
{
	private static HashMap<String, String> m_database = new HashMap<>();

	private static void printUsage()
	{
		System.out.println("ERROR: invalid arguments, usage: java UDPServer <port_number>");
	}

	private static void logMessage(final String message)
	{
		System.out.println("(" + (new Date()).toString() + ") " + message);
	}

	private static String registerPlate(final String paramPlate, final String paramOwner)
	{
		if (m_database.containsKey(paramPlate))
		{
			return null;
		}

		m_database.put(paramPlate, paramOwner);

		return paramOwner;
	}

	private static String lookupPlate(final String paramPlate)
	{
		if (!m_database.containsKey(paramPlate))
		{
			return null;
		}

		return m_database.get(paramPlate);
	}

	private static String generateResponse(final String command)
	{
		final String[] args = command.split(" ");

		if (args[0].equals("LOOKUP") && args.length == 2)
		{
			final String output = Integer.toString(m_database.size()) + "\n" + args[1];
			final String result = lookupPlate(args[1]);

			if (result != null)
			{
				return output + " - " + result;
			}
		}
		else if (args[0].equals("REGISTER") && args.length == 3)
		{
			final String output = Integer.toString(m_database.size()) + "\n" + args[1];
			final String result = registerPlate(args[1], args[2]);

			if (result != null)
			{
				return output + " - " + result;
			}
		}

		return "-1\n";
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length != 1)
		{
			printUsage();
			System.exit(0);
		}

		int hostPort = 0;

		try
		{
			hostPort = Integer.parseInt(args[0]);
		}
		catch (NumberFormatException e)
		{
			printUsage();
			System.exit(0);
		}

		DatagramSocket socket = new DatagramSocket(hostPort);
		InetAddress hostAddress = InetAddress.getLocalHost();
		logMessage(String.format("Server running @ %s:%s...", hostAddress.getHostAddress(), args[0]));

		for (;;)
		{
			byte[] rbuf = new byte[1024];

			try
			{
				// get request
				DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
				socket.receive(packet);

				// display request
				String received = new String(packet.getData()).trim();
				logMessage("REQUEST: " + received);

				// generate response
				String response = generateResponse(received);
				byte[] sbuf = response.getBytes();

				// send response
				packet = new DatagramPacket(sbuf, sbuf.length, packet.getAddress(), packet.getPort());
				socket.send(packet);
			}
			catch (IOException e)
			{
				System.out.println(e.getMessage());
				socket.close();
			}
		}
	}
}