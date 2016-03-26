package bs.test;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TestStub extends Remote
{
	public boolean backupFile(final String fileId, int replicationDegree) throws RemoteException;
	public boolean restoreFile(final String fileId) throws RemoteException;
	public boolean deleteFile(final String fileId) throws RemoteException;
	public boolean reclaimSpace() throws RemoteException;
	public boolean backupEnhanced(final String fileId, int replicationDegree) throws RemoteException;
	public boolean restoreEnhanced(final String fileId) throws RemoteException;
	public boolean deleteEnhanced(final String fileId) throws RemoteException;
	public boolean reclaimEnhanced() throws RemoteException;
}