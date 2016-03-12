package sdis_proj1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Date;

public class Protocol
{
	private static Protocol m_instance;

	public static Protocol getInstance() {
		
		if (m_instance == null) {
			m_instance = new Protocol();
		}

		return m_instance;
	}
	
	private int m_peerId = -1;

	public void connectBackup(final InetAddress backupAddress, int backupPort) {
	
		m_backupChannel = new Pair<InetAddress, Integer>(backupAddress, backupPort);
		
		try {
			m_backupSocket = new MulticastSocket();
			m_backupSocket.setTimeToLive(1);
			m_backupSocket.joinGroup(m_backupChannel.first());
			m_backupAvailable = true;
		}
		catch (IOException ex) {
			m_backupAvailable = false;
			ex.printStackTrace();
		}
		
		printBackupStatus();
	}
	
	public void connectRestore(final InetAddress restoreAddress, int restorePort) {
		
		m_restoreChannel = new Pair<InetAddress, Integer>(restoreAddress, restorePort);
		
		try {
			m_restoreSocket = new MulticastSocket();
			m_restoreSocket.setTimeToLive(1);
			m_restoreSocket.joinGroup(m_restoreChannel.first());
			m_restoreAvailable = true;
		}
		catch (IOException ex) {
			m_restoreAvailable = false;
			ex.printStackTrace();
		}
		
		printRestoreStatus();
	}
	
	public void connect(int peerId, final InetAddress commandAddress, int commandPort) {
		
		m_peerId = peerId;	
		m_commandChannel = new Pair<InetAddress, Integer>(commandAddress, commandPort);
		
		try {
			m_commandSocket = new MulticastSocket();
			m_commandSocket.setTimeToLive(1);
			m_commandSocket.joinGroup(m_commandChannel.first());
			m_commandAvailable = true;
		}
		catch (IOException ex) {
			m_commandAvailable = false;
			ex.printStackTrace();
		}
		
		printCommandStatus();
	}

	//------------------------------------------------------
	private Pair<InetAddress, Integer> m_commandChannel = null;
	private Pair<InetAddress, Integer> m_backupChannel = null;
	private Pair<InetAddress, Integer> m_restoreChannel = null;
	//------------------------------------------------------
	private MulticastSocket m_commandSocket = null;
	private MulticastSocket m_backupSocket = null;
	private MulticastSocket m_restoreSocket = null;
	//------------------------------------------------------
	private boolean m_commandAvailable = false;
	private boolean m_backupAvailable = false;
	private boolean m_restoreAvailable = false;
	//------------------------------------------------------
	
	public final String getVersion() {
		return "1.0";
	}

	public final int getPeerId() {
		return m_peerId;
	}
	
	public Pair<InetAddress, Integer> getCommandChannel() {
		return m_commandChannel;
	}
	
	public Pair<InetAddress, Integer> getBackupChannel() {
		return m_backupChannel;
	}
	
	public Pair<InetAddress, Integer> getRestoreChannel() {
		return m_restoreChannel;
	}
	
	public boolean isCommandAvailable() {
		return m_commandAvailable;
	}
	
	public boolean isBackupAvailable() {
		return m_backupAvailable;
	}
	
	public boolean isRestoreAvailable() {
		return m_restoreAvailable;
	}

	private final void printCommandStatus() {
		System.out.println(String.format(
			"(%s) connected to command channel: %s:%d",
			(new Date()).toString(),
			m_commandChannel.first().getHostAddress(),
			m_commandChannel.second()));
	}

	private final void printBackupStatus() {
		System.out.println(String.format(
			"(%s) connected to backup channel: %s:%d",
			(new Date()).toString(),
			m_backupChannel.first().getHostAddress(),
			m_backupChannel.second()));
	}

	private final void printRestoreStatus() {
		System.out.println(String.format(
			"(%s) connected to restore channel: %s:%d",
			(new Date()).toString(),
			m_restoreChannel.first().getHostAddress(),
			m_restoreChannel.second()));
	}

	public final MessageWrapper receiveRequest(final InetAddress paramHost, int paramPort) {
		
		final byte[] rbuf = new byte[1024];
		
		MessageWrapper response = null;
		DatagramPacket packet = new DatagramPacket(rbuf, rbuf.length);
		
		try (final MulticastSocket socket = new MulticastSocket(paramPort)) {
			socket.setTimeToLive(1);
			socket.joinGroup(paramHost);
			socket.receive(packet);
			response = new MessageWrapper(packet.getAddress(), Integer.parseInt(new String(packet.getData(), packet.getOffset(), packet.getLength())));
			socket.leaveGroup(paramHost);
		}
		catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

		return response;
	}
	
	public static final boolean DEBUG = true;
}