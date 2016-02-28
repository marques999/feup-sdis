import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;

public class MulticastServer
{
	private static int groupPort = 8888;
	private static int servicePort = 8888;

	private static void printRequest(final String message, final InetAddress host, int port)
	{
		System.out.print(String.format("(%s) client connected from %s:%d\n",
			(new Date()).toString(), host.getHostAddress(), port));
		System.out.print(String.format("(%s) received command: %s\n",
			(new Date()).toString(), message));
	}

	private static void printMessage()
	{
		String outMessage = String.format("(%s) multicast: %s:%d, %s:%d",
			(new Date()).toString(), groupAddress.getHostAddress(),
			groupPort, serviceAddress.getHostAddress(), servicePort);
		System.out.println(outMessage);
	}

	private static void printReply(String replyMessage)
	{
		System.out.print(String.format("(%s) sent reply: %s\n",
			(new Date()).toString(), replyMessage));
	}

	private static void printUsage()
	{
		System.out.println("usage: java MulticastServer <srvc_port> <mcast_addr> <mcast_port>");
	}

	private static InetAddress groupAddress = null;
	private static InetAddress serviceAddress = null;
	private static HashMap<String, String> lookupTable = new HashMap<>();

	private static String registerPlate(String paramPlate, String paramOwner)
	{
		if (lookupTable.containsKey(paramPlate))
		{
			return "";
		}

		lookupTable.put(paramPlate, paramOwner);

		return paramOwner;
	}

	private static String lookupPlate(String string)
	{
		if (!lookupTable.containsKey(string))
		{
			return "";
		}

		return lookupTable.get(string);
	}

	private static class BroadcastThread implements Runnable
	{
		private MulticastSocket m_socket;

		public BroadcastThread(MulticastSocket socket)
		{
			m_socket = socket;
		}

		@Override
		public void run()
		{
			try
			{
				for (;;)
				{
					printMessage();
					byte[] sbuf = Integer.toString(servicePort).getBytes();
					m_socket.send(new DatagramPacket(sbuf, sbuf.length, groupAddress, groupPort));
					Thread.sleep(1000);
				}
			}
			catch (IOException | InterruptedException ex)
			{
				ex.printStackTrace();
				System.exit(1);
			}
		}
	}

	private static byte[] generateResponse(String command)
	{
		String[] args = command.split(" ");
		String response = "";

		int returnCode = -1;

		if (args[0].equals("LOOKUP") && args.length >= 2)
		{
			response = lookupPlate(command.replaceFirst("LOOKUP ", ""));
		}
		else if (args[0].equals("REGISTER") && args.length >= 3)
		{
			response = registerPlate(args[1], command.replaceFirst("REGISTER " + args[1] + " ", ""));
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
		if (args.length != 3)
		{
			printUsage();
			System.exit(1);
		}

		try
		{
			servicePort = Integer.parseInt(args[0]);
			groupPort = Integer.parseInt(args[2]);
		}
		catch (NumberFormatException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		try
		{
			serviceAddress = InetAddress.getLocalHost();
			groupAddress = InetAddress.getByName(args[1]);
		}
		catch (UnknownHostException ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}

		final DatagramSocket udpsocket = new DatagramSocket(servicePort);
		final MulticastSocket socket = new MulticastSocket();

		socket.setTimeToLive(1);
		socket.joinGroup(groupAddress);

		new Thread(new BroadcastThread(socket)).start();

		for (;;)
		{
			byte[] rbuf = new byte[1024];

			try
			{
				// get request
				DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
				udpsocket.receive(packet);
				// display request
				String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
				printRequest(received, packet.getAddress(), packet.getPort());
				// process response
				byte[] sbuf = generateResponse(received);
				// send response
				udpsocket.send(new DatagramPacket(sbuf, sbuf.length, packet.getAddress(), packet.getPort()));
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
				socket.close();
				udpsocket.close();
			}
		}
	}
}