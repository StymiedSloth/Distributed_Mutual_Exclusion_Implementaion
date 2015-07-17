import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MessagePassing extends Remote
{
	public void MessagePass(int sender,String message)throws RemoteException;
}
