public class Test 
{
	public static void main(String args[])
	{		
		System.out.println("Initiating Sequence");
		TestServer ts = new TestServer();
		TestClient tc = new TestClient();
		
		ts.start();
	
		tc.start();
	}
}
