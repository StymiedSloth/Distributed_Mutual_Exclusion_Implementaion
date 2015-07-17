public class Test 
{
	public static void main(String args[])
	{
		TestServer ts = new TestServer();
		TestClient tc = new TestClient();
		
		ts.start();
	
		tc.start();
	}
}
