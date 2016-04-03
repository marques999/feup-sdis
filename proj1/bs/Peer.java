package bs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.filesystem.FileManager;
import bs.protocol.ChunkMessage;
import bs.protocol.ChunkMessage2_0;
import bs.protocol.DeleteMessage;
import bs.protocol.DeletedMessage2_0;
import bs.protocol.GetchunkMessage;
import bs.protocol.GetchunkMessage2_0;
import bs.protocol.Message;
import bs.protocol.PutchunkMessage;
import bs.protocol.RemovedMessage;
import bs.protocol.StoredMessage;

public class Peer
{
	private static int myPeerId = -1;

	public static int getPeerId()
	{
		return myPeerId;
	}

	//----------------------------------------------------

	private static String myVersion = "1.0";

	public static String getVersion()
	{
		return myVersion;
	}

	//----------------------------------------------------

	private static BackupService svcBackup;

	public static BackupService getBackupService()
	{
		return svcBackup;
	}

	//----------------------------------------------------

	private static RestoreService svcRestore;

	public static RestoreService getRestoreService()
	{
		return svcRestore;
	}

	//----------------------------------------------------

	private static ControlService svcControl;

	public static ControlService getControlService()
	{
		return svcControl;
	}

	//----------------------------------------------------

	private static boolean enableEnhancements = false;

	public static boolean enhancementsEnabled()
	{
		return enableEnhancements;
	}

	public static void setEnhancements(boolean enhancementsEnabled)
	{
		enableEnhancements = enhancementsEnabled;

		if (enableEnhancements)
		{
			Logger.logInformation("protocol enhancements enabled on this session!");
			myVersion = "2.0";
		}
		else
		{
			myVersion = "1.0";
		}
	}

	//----------------------------------------------------

	private static BackupStorage bsdbInstance;

	public static BackupStorage getStorage()
	{
		return bsdbInstance;
	}

	//----------------------------------------------------

	private static Connection MDB;

	private static boolean issueBackupCommand(final Message paramMessage)
	{
		return MDB.send(paramMessage.getMessage());
	}

	//----------------------------------------------------

	private static Connection MC;

	private static boolean issueControlCommand(final Message paramMessage)
	{
		return MC.send(paramMessage.getMessage());
	}

	//----------------------------------------------------

	private static Connection MDR;

	private static boolean issueRestoreCommand(final Message paramMessage)
	{
		return MDR.send(paramMessage.getMessage());
	}

	//----------------------------------------------------

	private static UnicastAdapter MDR_enhanced;

	private static boolean issueUnicastCommand(final Message paramMessage, final InetAddress paramAddress, int paramPort)
	{
		return MDR_enhanced.send(paramMessage.getMessage(), paramAddress, paramPort);
	}

	public static int getUnicastPort()
	{
		return MDR_enhanced == null ? -1 : MDR_enhanced.getPort();
	}

	//----------------------------------------------------

	private static File bsdbFilename;
	private static FileManager fmInstance;

	public static FileManager getFiles()
	{
		return fmInstance;
	}

	//----------------------------------------------------

	public static boolean writeStorage()
	{
		if (!bsdbFilename.exists())
		{
			try
			{
				bsdbFilename.createNewFile();
			}
			catch (IOException ex)
			{
				return false;
			}
		}

		try (final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(bsdbFilename)))
		{
			objectOutputStream.writeObject(bsdbInstance);
		}
		catch (IOException ex)
		{
			return false;
		}

		return true;
	}

	public static boolean sendCHUNK(final Chunk paramChunk)
	{
		Logger.logDebug("sending chunk " + paramChunk.getChunkId() + "...");
		return issueRestoreCommand(new ChunkMessage(paramChunk));
	}

	public static boolean sendUnicastCHUNK2_0(final Chunk paramChunk, final InetAddress paramAddress, int paramPort)
	{
		Logger.logDebug("sending unicast chunk " + paramChunk.getChunkId() + " to " + paramAddress + ":" + paramPort);
		return issueUnicastCommand(new ChunkMessage(paramChunk), paramAddress, paramPort);
	}

	public static boolean sendMulticastCHUNK2_0(final Chunk paramChunk)
	{
		Logger.logDebug("sending dummy chunk " + paramChunk.getChunkId() + "...");
		return issueRestoreCommand(new ChunkMessage2_0(paramChunk.getFileId(), paramChunk.getChunkId()));
	}

	public static boolean sendDELETE(final String fileId)
	{
		return issueControlCommand(new DeleteMessage(fileId));
	}

	public static boolean sendDELETED2_0(final String fileId)
	{
		return issueControlCommand(new DeletedMessage2_0(fileId));
	}

	public static boolean sendGETCHUNK(final String fileId, int chunkId)
	{
		if (enableEnhancements)
		{
			Logger.logChunkCommand("\"getchunk2_0\"", fileId, chunkId);
			return issueControlCommand(new GetchunkMessage2_0(fileId, chunkId));
		}
		else
		{
			Logger.logChunkCommand("\"getchunk\"", fileId, chunkId);
			return issueControlCommand(new GetchunkMessage(fileId, chunkId));
		}
	}

	public static boolean sendPUTCHUNK(final Chunk paramChunk)
	{
		Logger.logChunkCommand("\"putchunk\"", paramChunk.getFileId(), paramChunk.getChunkId());
		return issueBackupCommand(new PutchunkMessage(paramChunk));
	}

	public static boolean sendREMOVED(final String fileId, int chunkId)
	{
		Logger.logChunkCommand("\"removed\"", fileId, chunkId);
		return issueControlCommand(new RemovedMessage(fileId, chunkId));
	}

	public static boolean sendSTORED(final Chunk paramChunk)
	{
		Logger.logChunkCommand("\"stored\"", paramChunk.getFileId(), paramChunk.getChunkId());
		return issueControlCommand(new StoredMessage(paramChunk.getFileId(), paramChunk.getChunkId()));
	}

	private static int multicastControlPort = PeerGlobals.defaultControlPort;
	private static int multicastBackupPort = PeerGlobals.defaultBackupPort;
	private static int multicastRestorePort = PeerGlobals.defaultRestorePort;

	public static void startUnicast()
	{
		if (MDR_enhanced == null)
		{
			Logger.logInformation(PeerStrings.messageStartingUnicast);
			MDR_enhanced = new UnicastAdapter(svcRestore);
			MDR_enhanced.start();
		}
	}

	public static void stopUnicast()
	{
		if (MDR_enhanced != null)
		{
			Logger.logInformation(PeerStrings.messageTerminatingUnicast);
			MDR_enhanced.interrupt();
			MDR_enhanced = null;
		}
	}

	private static int parsePort(final String args, int defaultPort)
	{
		int currentPort = defaultPort;

		try
		{
			currentPort = Integer.parseInt(args);
		}
		catch (IndexOutOfBoundsException | NumberFormatException ex)
		{
			currentPort = defaultPort;
			Logger.logError(PeerStrings.messageInvalidPort);
		}

		return currentPort;
	}

	private static InetAddress parseAddress(final String args)
	{
		InetAddress multicastBackupHost = null;

		if (args != null)
		{
			try
			{
				multicastBackupHost = InetAddress.getByName(args);
			}
			catch (UnknownHostException ex)
			{
				multicastBackupHost = null;
			}
		}

		if (multicastBackupHost == null)
		{
			Logger.logError(PeerStrings.messageInvalidAddress);

			try
			{
				multicastBackupHost = InetAddress.getLocalHost();
			}
			catch (UnknownHostException e)
			{
				Logger.abort("could not connect to localhost!");
			}
		}

		return multicastBackupHost;
	}

	private static void printUsage()
	{
		Logger.logError("invalid number of arguments given, please enter the following:");
		System.out.println("    (1) Peer <PeerId> <Host>");
		System.out.println("    (2) Peer <PeerId> <Host> <McPort> <MdbPort> <MdrPort>");
		System.out.println("    (3) Peer <PeerId> <McHost> <McPort> <MdbHost> <MdbPort> <MdrHost> <MdrPort>");
		System.exit(1);
	}

	private static InetAddress multicastControlHost = null;
	private static InetAddress multicastBackupHost = null;
	private static InetAddress multicastRestoreHost = null;

	public static void main(final String[] args)
	{
		if (PeerGlobals.checkArguments(args.length))
		{
			initializePeer(args, false);
			setEnhancements(false);
		}
		else
		{
			printUsage();
		}
	}

	public static boolean initializePeer(final String[] args, boolean initiatorPeer)
	{
		if (initiatorPeer)
		{
			try
			{
				myPeerId = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(PeerStrings.messageInvalidRemote);
			}

			if (myPeerId < 0 || myPeerId > Short.MAX_VALUE)
			{
				Logger.abort(PeerStrings.messageInvalidRemote);
			}
		}
		else
		{
			try
			{
				myPeerId = Integer.parseInt(args[0]);
			}
			catch (NumberFormatException ex)
			{
				Logger.abort(PeerStrings.messageInvalidPeer);
			}

			if (myPeerId < 0 || myPeerId > Short.MAX_VALUE)
			{
				Logger.abort(PeerStrings.messageInvalidPeer);
			}
		}

		if (args.length == 7)
		{
			Logger.logInformation("parsing multicast control channel arguments...");
			multicastControlHost = parseAddress(args[1]);
			multicastControlPort = parsePort(args[2], PeerGlobals.defaultControlPort);
			Logger.logInformation("parsing multicast backup channel arguments...");
			multicastBackupHost = parseAddress(args[3]);
			multicastBackupPort = parsePort(args[4], PeerGlobals.defaultBackupPort);
			Logger.logInformation("parsing multicast restore channel arguments...");
			multicastRestoreHost = parseAddress(args[5]);
			multicastRestorePort = parsePort(args[6], PeerGlobals.defaultRestorePort);
		}
		else if (args.length == 5)
		{
			Logger.logInformation("parsing multicast group address...");
			multicastControlHost = parseAddress(args[1]);
			multicastBackupHost = multicastControlHost;
			multicastRestoreHost = multicastBackupHost;
			Logger.logInformation("parsing multicast control channel port...");
			multicastControlPort = parsePort(args[2], PeerGlobals.defaultControlPort);
			Logger.logInformation("parsing multicast backup channel port...");
			multicastBackupPort = parsePort(args[3], PeerGlobals.defaultBackupPort);
			Logger.logInformation("parsing multicast restore channel port...");
			multicastRestorePort = parsePort(args[4], PeerGlobals.defaultRestorePort);
		}
		else if (args.length == 2)
		{
			Logger.logInformation("parsing multicast group address...");
			multicastControlHost = parseAddress(args[1]);
			multicastBackupHost = multicastControlHost;
			multicastRestoreHost = multicastBackupHost;
		}
		else
		{
			printUsage();
		}

		bsdbFilename = new File("storage$" + myPeerId + ".bsdb");

		if (bsdbFilename.exists())
		{
			try (final ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(bsdbFilename)))
			{
				bsdbInstance = (BackupStorage) objectInputStream.readObject();
			}
			catch (Exception ex)
			{
				Logger.logError("could not read database state, but file exists!");
				System.exit(1);
			}
		}
		else
		{
			bsdbInstance = new BackupStorage(bsdbFilename);

			if (!writeStorage())
			{
				Logger.logError("could not write database state to disk!");
				System.exit(1);
			}
		}

		fmInstance = new FileManager(myPeerId);
		bsdbInstance.dumpStorage();
		bsdbInstance.dumpRestore();

		MC = new Connection("control channel", multicastControlHost, multicastControlPort, false);
		MDB = new Connection("backup channel", multicastBackupHost, multicastBackupPort, false);
		MDR = new Connection("restore channel", multicastRestoreHost, multicastRestorePort, false);

		svcControl = new ControlService(multicastControlHost, multicastControlPort);
		svcBackup = new BackupService(multicastBackupHost, multicastBackupPort);
		svcRestore = new RestoreService(multicastRestoreHost, multicastRestorePort);

		boolean peerConnected = svcControl.available() && svcBackup.available() && svcRestore.available();

		if (peerConnected)
		{
			svcBackup.start();
			svcControl.start();
			svcRestore.start();
		}

		return peerConnected;
	}
}