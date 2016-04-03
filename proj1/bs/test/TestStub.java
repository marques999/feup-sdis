package bs.test;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TestStub extends Remote
{
	public boolean backupFile(final String fileId, int replicationDegree) throws RemoteException;
	public boolean restoreFile(final String fileId, boolean enableEnhancements) throws RemoteException;
	public boolean deleteFile(final String fileId, boolean enableEnhancements) throws RemoteException;
	public boolean reclaimSpace(int reclaimAmount, boolean enableEnhancements) throws RemoteException;
}