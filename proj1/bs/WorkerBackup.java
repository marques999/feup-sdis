package bs;

import java.util.ArrayList;

import bs.filesystem.BackupStorage;
import bs.filesystem.Chunk;
import bs.logging.Logger;
import bs.protocol.ChunkMessage;
import bs.protocol.Message;
import bs.protocol.PayloadMessage;
import bs.server.ProtocolBackup;
import bs.server.ProtocolCommand;

public class WorkerBackup extends Thread {
	private final ProtocolBackup m_backupChannel;
	private final ProtocolCommand m_controlChannel;
	private final BackupStorage m_storage = BackupSystem.getStorage();
	private final String thisThread = "BackupWorkerThread";

	private ArrayList<ResponseThread> myQueue = new ArrayList<ResponseThread>();

	public WorkerBackup(final ProtocolCommand mc, final ProtocolBackup mdb) {
		m_controlChannel = mc;
		m_backupChannel = mdb;
	}

	private void checkDuplicates(final Message receivedMessage) {
		for (int i = 0; i < myQueue.size(); i++) {
			final ResponseThread thisThread = myQueue.get(i);

			if (thisThread.isAlive()) {
				System.out.println("active thread number " + i);
				final Message thisMessage = thisThread.getMessage();

				// if (thisMessage.equals(receivedMessage))
				// {
				thisThread.interrupt();
				myQueue.remove(i--);
				// }
			} else {
				System.out.println("dead thread number " + i);
				myQueue.remove(i--);
			}
		}
	}

	private PayloadMessage sendChunk(final String fileId, final int chunkId) {
	//	final Chunk myChunk = m_storage.getChunk(fileId, chunkId);

	//	if (myChunk != null) {
	//		return new ChunkMessage(myChunk);
	//	}

		return null;
	}

	@Override
	public void run() {
		for (;;) {
			final PayloadMessage receivedMessage = (PayloadMessage) m_backupChannel.receive();

			if (receivedMessage != null) {
				Logger.logCommand(thisThread, receivedMessage.getType());
				Logger.dumpPayload(thisThread, receivedMessage);
				m_storage.putChunk(receivedMessage.generateChunk());
				BackupSystem.writeStorage();
				// myQueue.add(MC.sendSTOREDResponse(receivedMessage));
			}
		}
	}
}