package bs;

import java.util.ArrayList;

import bs.filesystem.Chunk;
import bs.logging.Logger;
import bs.protocol.DeleteMessage;
import bs.protocol.GetchunkMessage;
import bs.protocol.Message;
import bs.protocol.RemovedMessage;
import bs.protocol.StoredMessage;
import bs.server.ProtocolCommand;

public class WorkerCommand extends Thread {
    private final ProtocolCommand MC;
    private final WorkerBackup backupThread;
    private final WorkerRestore restoreThread;
    private final String myName = "CommandWorkerThread";

    public WorkerCommand(final ProtocolCommand mc, final WorkerBackup bThread, final WorkerRestore rThread) {
	MC = mc;
	backupThread = bThread;
	restoreThread = rThread;
    }

    private ResponseThread sendResponse(final Message paramMessage) {
	return new ResponseThread(MC, paramMessage);
    }

    public void notifyStored(final String fileId, final int chunkId) {
	sendResponse(new StoredMessage(fileId, chunkId)).start();
    }

    public boolean requestDelete(final String fileId) {
	sendResponse(new DeleteMessage(fileId)).start();

	return true;
    }

    private void notifyReclaim(final String fileId, int chunkId) {
	sendResponse(new RemovedMessage(fileId, chunkId)).start();
    }

    public boolean notifyReclaim(final ArrayList<Chunk> removedChunks) {
	for (final Chunk thisChunk : removedChunks) {
	    notifyReclaim(thisChunk.getFileId(), thisChunk.getChunkId());
	}

	return true;
    }

    public void requestChunk(final String fileId, final int chunkId) {
	restoreThread.registerThread(sendResponse(new GetchunkMessage(fileId, chunkId)));
    }

    @Override
    public void run() {
	for (;;) {
	    final Message receivedMessage = MC.receive();

	    if (receivedMessage != null) {
		Logger.logCommand(myName, receivedMessage.getType());
		Logger.dumpHeader(myName, receivedMessage);
	    }
	}
    }
}