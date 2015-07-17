import java.rmi.Naming;


public class TestClient implements Runnable
{
	Thread t;
	@Override
	public void run() 
	{
		while(true)
		{
			try
			{
				MessagePassing stub = (MessagePassing) Naming.lookup("rmi://localhost:5000/sonoo");
				System.out.println("I am client passing the parameter");
				stub.MessagePass(1, "This is test");
			}
			catch(Exception ex)
			{
				System.out.println(ex);
			}
		}
	}
	
	public void start()
	{
		t = new Thread(this);
		t.start();
	}
}
