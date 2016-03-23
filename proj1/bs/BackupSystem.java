package bs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import bs.actions.ActionBackup;
import bs.actions.ActionDelete;
import bs.actions.ActionRestore;
import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.filesystem.ChunkBackup;
import bs.filesystem.FileManager;
import bs.server.ProtocolBackup;
import bs.server.ProtocolCommand;
import bs.server.ProtocolRestore;
import bs.test.TestStub;

public class BackupSystem implements TestStub
{
	private static final int MINIMUM_ARGUMENTS = 2;
	private static final int NUMBER_ARGUMENTS = 5;
	private static final int DEFAULT_CONTROL_PORT = 8080;
	private static final int DEFAULT_BACKUP_PORT = 8081;
	private static final int DEFAULT_RESTORE_PORT = 8082;
	
	//----------------------------------------------------
	
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
	
	private static ProtocolCommand MC;
	private static ProtocolBackup MDB;
	private static ProtocolRestore MDR;
	private static WorkerBackup backupThread;
	private static WorkerRestore restoreThread;
	private static WorkerCommand commandThread;
	private static File bsdbFilename;
	
	//----------------------------------------------------
	
	private static BackupStorage bsdbInstance;
	
	public static BackupStorage getStorage()
	{
		return bsdbInstance;
	}
	
	//----------------------------------------------------
	
	private static FileManager fmInstance;
	
	public static FileManager getFiles()
	{
		return fmInstance;
	}
	
	//----------------------------------------------------
	
	public static boolean readStorage()
	{
		try (final ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(bsdbFilename)))
		{
			bsdbInstance = (BackupStorage) objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		catch (IOException ex)
		{
			return false;
		}

		return true;
	}
	
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
	
	// ----------------------------------------------------

	public static void main(final String[] args) throws IOException
	{
		if (args.length < MINIMUM_ARGUMENTS || args.length > NUMBER_ARGUMENTS)
		{
			System.out.println("usage: BackupSystem <Host> <PeerId> [<McPort> <MdbPort> <MdrPort>]");
			System.exit(1);
		}

		InetAddress myHost = null;
		System.out.println("[main()]::attempting to parse multicast group address...");

		try
		{
			myHost = InetAddress.getByName(args[0]);
		}
		catch (UnknownHostException ex)
		{
			System.out.println("[main()]::invalid multicast group address!");
			System.exit(1);
		}

		int multicastControlPort = DEFAULT_CONTROL_PORT;
		int multicastBackupPort = DEFAULT_BACKUP_PORT;
		int multicastRestorePort = DEFAULT_RESTORE_PORT;

		System.out.println("[main()]::attempting to parse peer identifier...");

		try
		{
			myPeerId = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex)
		{
			System.out.println("[main()]::invalid peer identifier, please enter a positive integer!");
			System.exit(1);
		}

		System.out.println(
				"[main()]::attempting to parse control channel port...");

		try
		{
			multicastControlPort = Integer.parseInt(args[2]);
		}
		catch (ArrayIndexOutOfBoundsException | NumberFormatException ex)
		{
			System.out.println("[main()]::invalid or missing control channel port, assuming default...");
		}

		System.out.println("[main()]::attempting to parse backup channel port...");

		try
		{
			multicastBackupPort = Integer.parseInt(args[3]);
		}
		catch (IndexOutOfBoundsException | NumberFormatException ex)
		{
			System.out.println("[main()]::invalid or missing backup channel port, assuming default...");
		}

		System.out.println("[main()]::atempting to parse restore channel port...");

		try
		{
			multicastRestorePort = Integer.parseInt(args[4]);
		} catch (IndexOutOfBoundsException | NumberFormatException ex)
		{
			System.out.println(
					"[main()]::invalid or missing restore channel port, assuming default...");
		}
		
		//----------------------------------------------------
		
		final InetAddress serverAddress = myHost;
		
		MC = new ProtocolCommand(serverAddress, multicastControlPort);
		MDB = new ProtocolBackup(serverAddress, multicastBackupPort);
		MDR = new ProtocolRestore(serverAddress, multicastRestorePort);
		
		//----------------------------------------------------
		
		bsdbFilename = new File("storage$"+ myPeerId + ".bsdb");
	
		if (bsdbFilename.exists())
		{
			if (!readStorage())
			{
				System.err.print("error reading database state!");
				System.exit(1);
			}
		}
		else
		{
			bsdbInstance = new BackupStorage(bsdbFilename);
			
			if (!writeStorage())
			{
				System.err.print("error saving database state!");
				System.exit(1);
			}
		}
		
		//----------------------------------------------------
		
		fmInstance = new FileManager(myPeerId);
		
		//----------------------------------------------------

		// wb = new WorkerBackup(MC, MDB);
		// wr = new WorkerRestore(MDR);
		// wc = new WorkerCommand(MC, wb, wr);
		// wb.start();
		// wr.start();
		// wc.start();
		
		staticBackupFile("example.bin", 4);
		writeStorage();
		bsdbInstance.dumpStorage();
		bsdbInstance.dumpRestore();
		
		byte[] testBuffer = {1, 2, 3};
		Chunk testChunk = new Chunk(testBuffer, "abc", 3, 3);
		MC.subscribeNotifications(testChunk);
		MC.registerConfirmation("abc", 3, 1);
		MC.registerConfirmation("abc", 3, 2);
		MC.registerConfirmation("abc", 3, 1);
		MC.registerConfirmation("abc", 3, 1);
		MC.registerConfirmation("abc", 3, 2);
		System.out.println(MC.getConfirmations(testChunk));
		
		final BackupSystem bsInstance = new BackupSystem();

		try
		{
			TestStub rmiService = (TestStub) UnicastRemoteObject.exportObject(bsInstance, 0);
			LocateRegistry.getRegistry().rebind("1234", rmiService);
		}
		catch (RemoteException e)
		{
			System.out.println("Could not bind to rmiregistry");
		}
	}

	@Override
	public boolean backupFile(final String fileId, int replicationDegree) throws RemoteException
	{
		final ActionBackup actionBackup = new ActionBackup(fileId, replicationDegree);
		
		actionBackup.start();
		
		try
		{
			actionBackup.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		return actionBackup.getResult();
	}

	@Override
	public boolean restoreFile(final String fileId) throws RemoteException
	{
		final ActionRestore actionRestore = new ActionRestore(fileId, MC, MDR);
		
		actionRestore.start();
		
		try
		{
			actionRestore.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		return actionRestore.getResult();
	}
	
	@Override
	public boolean deleteFile(String fileId) throws RemoteException
	{
		final ActionDelete actionDelete = new ActionDelete(fileId, MC);
		
		actionDelete.start();
		
		try
		{
			actionDelete.join();
		}
		catch (InterruptedException ex)
		{
			return false;
		}
		
		return actionDelete.getResult();
	}
	
	public static boolean staticBackupFile(final String fileId, int replicationDegree)
	{
		try
		{
		    final ChunkBackup myChunks = new ChunkBackup(fileId, replicationDegree);
		    final Chunk[] myChunksArray = myChunks.getChunks();

		    for (int i = 0; i < myChunksArray.length; i++)
		    { 
				fmInstance.writeChunk(myChunksArray[i]);
		    }
		}
		catch (IOException ex)
		{
			return false;
		}

		return true;
	}
	
	@Override
	public boolean reclaimSpace() throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean backupEnhanced(String fileId, int replicationDegree) throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean restoreEnhanced(String fileId) throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean deleteEnhanced(String fileId) throws RemoteException
	{
		return false;
	}
	
	@Override
	public boolean reclaimEnhanced() throws RemoteException
	{
		return false;
	}
}