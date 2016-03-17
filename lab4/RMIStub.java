import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIStub extends Remote
{
	public String generateResponse(final String command) throws RemoteException;
}