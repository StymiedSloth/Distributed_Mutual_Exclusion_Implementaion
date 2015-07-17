import java.rmi.Naming;

public class TestServer implements Runnable
{
	private Thread t;
	@Override
	public void run()
	{
		try
		{
			MessagePassing stub = new MessagePassingRemote();
			Naming.rebind("rmi://localhost:5000/sonoo",stub);
		}
		catch(Exception ex)
		{
			System.out.println("Error in server");
			System.out.println(ex);
		}	
	}
	
	public void start()
	{
		 t = new Thread(this);
		 t.start();
	}
}
