package com.aos.client;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;


public class TestClient implements Runnable
{
	Thread t;
	private int myNodeID;
	private PriorityBlockingQueue<QueueObject> queue;
	private int[] quorum;
	private Boolean token;
	private int requestTime;
	private static int timestamp = 0;
	private PriorityBlockingQueue<Integer> requestTimestampQueue; 
	
	public TestClient(int myNodeID,PriorityBlockingQueue<QueueObject> queue,int[] quorum,Boolean token,int requestTime) {
		this.myNodeID = myNodeID;
		this.queue = queue;		
		this.quorum = quorum;
		this.token = token;
		this.requestTime = requestTime;
		requestTimestampQueue = new PriorityBlockingQueue<Integer>();
		requestTimestampQueue.add(requestTime);
	}
	
	public static int getTimeStamp(){
		return timestamp;
	}
	
	public static void setTimestamp(int latestTimestamp){
		timestamp = latestTimestamp;
	}
	
	@Override
	public void run() 
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println("Time: "+ sdf.format(cal.getTime()) + ":: Client Thread start " + myNodeID + " with " + quorum.length);
		while (true) {
			try 
			{			
				if(!requestTimestampQueue.isEmpty() && requestTimestampQueue.peek() <= getTimeStamp())
				{
					System.out.println("Time: "+ sdf.format(cal.getTime()) + ":: Current timestamp is " +  getTimeStamp()
					+ " compared to " + requestTimestampQueue.peek() + " at " + myNodeID );
					requestTimestampQueue.poll();
					MessagePassing stub;
					stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",myNodeID)+".utdallas.edu:5001/mutex");
					stub.sendRequest(requestTime-1, myNodeID);
				}
				Thread.sleep(15000);
				setTimestamp(getTimeStamp() + 1);
				System.out.println("**Timestamp increment " + getTimeStamp());
			}
			catch (RemoteException e) 
			{
				e.printStackTrace();
			} catch (InterruptedException e) {
			
				e.printStackTrace();
			} catch (MalformedURLException e) {
			
				e.printStackTrace();
			} catch (NotBoundException e) {
			
				e.printStackTrace();
			}
		}

	}
	
	public void start()
	{
		t = new Thread(this);
		t.start();
	}
}
