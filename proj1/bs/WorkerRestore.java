package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import bs.filesystem.BackupStorage;
import bs.logging.Logger;
import bs.protocol.Message;
import bs.protocol.PayloadMessage;
import bs.server.ProtocolRestore;

public class WorkerRestore extends Thread
{
	private final ProtocolRestore MDR;
	private final BackupStorage myStorage = BackupSystem.getStorage();
	private final String thisThread = "RestoreWorkerThread";

	private ArrayList<ResponseThread> myQueue = new ArrayList<ResponseThread>();
	
	private MulticastSocket mySocket;
	private InetAddress myHost;
	
	private volatile int queueSize;
	private volatile boolean interruptSend = false;
	
	private void checkDuplicates(final Message receivedMessage)
	{	
		for (int i = 0; i < myQueue.size(); i++)
		{
			final ResponseThread thisThread = myQueue.get(i);
			
			if (thisThread.isAlive())
			{
				final Message thisMessage = thisThread.getMessage();
				
				if (thisMessage.equals(receivedMessage))
				{
					thisThread.interrupt();
					myQueue.remove(i);
					i--;
				}
			}
			else
			{
				myQueue.remove(i);
				i--;
			}
		}
	}
	
	public WorkerRestore(final ProtocolRestore mdr)
	{
		MDR = mdr;
	}
	
	public void registerThread(final ResponseThread paramThread)
	{
		myQueue.add(paramThread);
		paramThread.start();
	}
	
	private void receiveMessage()
	{
		/**
		 * 1) RECEIVE "CHUNK" MESSAGE
		 */
		final PayloadMessage receivedMessage = (PayloadMessage) MDR.receive();
		
		/**
		 * 1) CHECKS IF RECEIVED VALID MESSAGE
		 */
		if (receivedMessage == null)
		{
			return;
		}
		
		/**
		 * 2) PRINTS MESSAGE INFORMATION
		 */
		Logger.logCommand(thisThread, receivedMessage.getType());
		Logger.dumpPayload(thisThread, receivedMessage);

		/**
		 * 3) CHECKS FOR DUPLICATED CHUNKS
		 */	
		checkDuplicates(receivedMessage);
	
		/**
		 * 4) SAVES CHUNK ON LOCAL STORAGE
		 */
		myStorage.putChunk(receivedMessage.generateChunk());
	}
	
	@Override
	public void run()
	{
		for(;;)
		{
			receiveMessage();
		}
	}
}
