package bs;

public class WorkerBackup extends Thread
{	
	private final ProtocolBackup MDB;
	private final ProtocolCommand MC;
	private final String thisThread = "BackupWorkerThread";
	
	public WorkerBackup(final ProtocolCommand mc, final ProtocolBackup mdb)
	{
		MC = mc;
		MDB = mdb;
	}
	
	@Override
	public void run()
	{
		for(;;)
		{
			final PayloadMessage receivedMessage = (PayloadMessage) MDB.receive();
			
			if (receivedMessage != null)
			{
				Logger.logCommand(thisThread, receivedMessage.getType());
				Logger.dumpPayload(thisThread, receivedMessage);
				BackupStorage.placeChunk(receivedMessage.generateChunk());
				MC.sendSTOREDResponse(receivedMessage);
			}
		}
	}
}