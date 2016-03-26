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
import bs.logging.Logger;
import bs.protocol.ChunkMessage;
import bs.protocol.DeleteMessage;
import bs.protocol.GetchunkMessage;
import bs.protocol.Message;
import bs.protocol.PutchunkMessage;
import bs.protocol.RemovedMessage;
import bs.protocol.StoredMessage;

public class BackupSystem
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
	
	private static File bsdbFilename;
	private static FileManager fmInstance;
	
	public static FileManager getFiles()
	{
		return fmInstance;
	}
	
	//----------------------------------------------------
	
	public static void initializeStorage(final File bsdbFilename)
	{
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
	}

	//----------------------------------------------------

	public static boolean sendCHUNK(final Chunk paramChunk)
	{
		Logger.logChunkCommand("CHUNK", paramChunk.getFileId(), paramChunk.getChunkId());	
		return issueRestoreCommand( new ChunkMessage(paramChunk));
	}
	
	public static boolean sendDELETE(final String fileId)
	{
		return issueControlCommand(new DeleteMessage(fileId));
	}
	
	public static boolean sendGETCHUNK(final String fileId, int chunkId)
	{
		Logger.logChunkCommand("GETCHUNK", fileId, chunkId);	
		return issueControlCommand(new GetchunkMessage(fileId, chunkId));
	}
	
	public static boolean sendPUTCHUNK(final Chunk paramChunk)
	{
		Logger.logChunkCommand("PUTCHUNK", paramChunk.getFileId(), paramChunk.getChunkId());	
		return issueBackupCommand(new PutchunkMessage(paramChunk));
	}

	public static boolean sendREMOVED(final String fileId, int chunkId)
	{
		Logger.logChunkCommand("REMOVED", fileId, chunkId);	
		return issueControlCommand(new RemovedMessage(fileId, chunkId));
	}

	public static boolean sendSTORED(final Chunk paramChunk)
	{
		Logger.logChunkCommand("STORED", paramChunk.getFileId(), paramChunk.getChunkId());	
		return issueControlCommand(new StoredMessage(paramChunk.getFileId(), paramChunk.getChunkId()));
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
	
	//----------------------------------------------------

	public static void main(final String[] args) throws IOException
	{
		if (BackupGlobals.checkPeerArguments(args.length))
		{
			initializePeer(args);
		}
		else
		{
			System.out.println("usage: BackupSystem <Host> <PeerId> [<McPort> <MdbPort> <MdrPort>]");		
		}	
	}

	protected static void initializePeer(final String[] args)
	{
		InetAddress myHost = null;
		Logger.logDebug("attempting to parse multicast group address...");

		try
		{
			myHost = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException ex)
		{
			Logger.abort("invalid multicast group address!");
		}
		
		Logger.logDebug("attempting to parse peer identifier...");

		try
		{
			myPeerId = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex)
		{
			Logger.abort("invalid peer identifier, please enter a positive integer!");
		}

		int multicastBackupPort = BackupGlobals.defaultBackupPort;
		int multicastControlPort = BackupGlobals.defaultControlPort;
		int multicastRestorePort = BackupGlobals.defaultRestorePort;

		Logger.logDebug("attempting to parse control channel port...");

		try
		{
			multicastControlPort = Integer.parseInt(args[2]);
		}
		catch (ArrayIndexOutOfBoundsException | NumberFormatException ex)
		{
			Logger.logError("invalid or missing control channel port, assuming default...");
		}

		Logger.logDebug("attempting to parse backup channel port...");

		try
		{
			multicastBackupPort = Integer.parseInt(args[3]);
		}
		catch (IndexOutOfBoundsException | NumberFormatException ex)
		{
			Logger.logError("invalid or missing backup channel port, assuming default...");
		}

		Logger.logDebug("atempting to parse restore channel port...");

		try
		{
			multicastRestorePort = Integer.parseInt(args[4]);
		}
		catch (IndexOutOfBoundsException | NumberFormatException ex)
		{
			Logger.logError("invalid or missing restore channel port, assuming default...");
		}
		
		//----------------------------------------------------
		
		MDB = new Connection("backup channel", myHost, multicastBackupPort, false);
		MDR = new Connection("restore channel", myHost, multicastRestorePort, false);
		MC = new Connection("control channel", myHost, multicastControlPort, false);
	
		//----------------------------------------------------
		
		bsdbFilename = new File("storage$"+ myPeerId + ".bsdb");
		initializeStorage(bsdbFilename);
		bsdbInstance.dumpStorage();
		bsdbInstance.dumpRestore();
		
		//----------------------------------------------------

		svcBackup = new BackupService(myHost, multicastBackupPort);
		svcControl = new ControlService(myHost, multicastControlPort);
		svcRestore = new RestoreService(myHost, multicastRestorePort);	
		svcBackup.start();
		svcControl.start();
		svcRestore.start();
	}
}