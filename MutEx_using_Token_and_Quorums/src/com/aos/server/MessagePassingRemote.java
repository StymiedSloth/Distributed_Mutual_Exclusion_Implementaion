package com.aos.server;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.PriorityBlockingQueue;

import com.aos.client.TestClient;
import com.aos.common.MessagePassing;
import com.aos.common.QueueObject;

public class MessagePassingRemote extends UnicastRemoteObject implements MessagePassing
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PriorityBlockingQueue<QueueObject> queue;
	private int myNodeID;
	private int[] quorum;
	private Boolean token;
	private Boolean criticalSection;
	
	//TODO Discuss with team about timestamp's position
	private int timestamp;
	
	private boolean[] requestMessageReceived;
	private boolean[] requestMessageSent;
	
	MessagePassingRemote(int myNodeID,PriorityBlockingQueue<QueueObject> queue, int[] quorum, Boolean token) throws RemoteException 
	{
		super();
		this.myNodeID = myNodeID;
		this.queue = queue;
		this.quorum  = quorum;
		this.token = token;
		this.criticalSection = false;
		
		this.requestMessageReceived = new boolean[16];
		this.requestMessageSent = new boolean[16];
		
		for (boolean b:requestMessageReceived)
			b = false;

		for (boolean b:requestMessageSent)
			b = false;
	}

	@Override
	public void sendRequest(int timestamp, int sender) throws RemoteException 
	{
		this.timestamp = timestamp + 1;
		
		for (boolean b:requestMessageSent)
			b = false;
		
		QueueObject queueObject = new QueueObject(this.timestamp, myNodeID);
		
		if(!queue.contains(queueObject))
			queue.add(queueObject);
		
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Queue at timestamp  " +timestamp + "is ");
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		QueueObject obj = queue.peek();
		
		if(obj.getSender() == myNodeID && token == true)
		{
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Enter Critical Section " + myNodeID);
			criticalSection = true;
			token = true;
			try
			{
				Thread.sleep(1000);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			//queue.remove(new QueueObject(this.timestamp, myNodeID));
			
			for(QueueObject q : queue)
			{
				if(q.getTimestamp()==this.timestamp && q.getSender()==myNodeID)
					queue.remove(q);
			}
			
			System.out.println(""+ sdf.format(cal.getTime()) + ":: Release Critical Section " + myNodeID);			
			System.out.println(""+ sdf.format(cal.getTime()) + ":: My queue after release message sent");

			for(QueueObject q : queue)
			{
				System.out.println(q.getTimestamp() + " " + q.getSender());
			}
			
			releaseCriticalSection();

			return;

		}
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				MessagePassing stub;
				try 
				{
					System.out.println(""+ sdf.format(cal.getTime()) + ":: I " + sender  +" am sending a request to my quorum member " + QuorumMember);
					if(requestMessageSent[QuorumMember] == false)
					{
						requestMessageSent[QuorumMember] = true;
						stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5001/mutex");
						stub.receiveRequest(this.timestamp, myNodeID);
					}
				} 
				catch (MalformedURLException e) 
				{
					e.printStackTrace();
				}
				catch (NotBoundException e) 
				{
					e.printStackTrace();
				}
			}
		}		
	}
	
	@Override
	public void receiveRequest(int timestamp, int sender)
			throws RemoteException
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: I have Recieved a request from " + sender);
		
		if(!queue.contains(new QueueObject(timestamp, sender)))
			queue.add(new QueueObject(timestamp, sender));
		
		requestMessageReceived[sender] = true;
		System.out.println(""+ sdf.format(cal.getTime()) + ":: My Queue after receive request");
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		if(token && !criticalSection)
		{
			
			QueueObject queueObject = queue.peek();
			 
			int TokenRequestor = queueObject.getSender();
			 
			MessagePassing stub;
			try 
			{
				//TODO DIsucss with team - This might be an issue, since any asker will get the token, we 
				//have to dequeue the first request from queue and only then send it out. 
				token = false;
				System.out.println(""+ sdf.format(cal.getTime()) + ":: I have token and I am not in CS => Token sent to " + sender);
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5001/mutex");
				stub.receiveToken(myNodeID);
				
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
			
		}
		else if(!token)
		{
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID && QuorumMember != sender)
				{
					MessagePassing stub;
					try 
					{
						System.out.println(""+ sdf.format(cal.getTime()) + ":: I don't have token for receive request by "+ sender
								+" => I ask my quorum members using AskToken");
						stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5001/mutex");
						stub.askToken(timestamp, myNodeID);
					} 
					catch (MalformedURLException e) 
					{
						e.printStackTrace();
					}
					catch (NotBoundException e) 
					{
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Exited from recieve Request from " + sender);
	}

	@Override
	public void askToken(int timestamp, int sender) throws RemoteException
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: I have been asked for the Token by" + sender
				+ " my token state is " + token);
		
		if(token && !criticalSection)
		{			
			MessagePassing stub;
			try 
			{
				token = false;
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5001/mutex");
				stub.receiveToken(myNodeID);
				
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}		
	}

	@Override
	public void receiveToken(int sender) throws RemoteException 
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: My id is "+ myNodeID +" I have received token from " + sender
				+ " and below is my queue");
		
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		if(!queue.isEmpty())
		{
			QueueObject queueObject = queue.peek();
			 
			int TokenRequestor = queueObject.getSender();
			 
			if(TokenRequestor == myNodeID)
			{
				System.out.println(""+ sdf.format(cal.getTime()) + ":: I will now enter my Critical section since I am the"
						+ "first requestor in the queue");
				criticalSection = true;
				token = true;
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
				
				releaseCriticalSection();
				
				return;
			}
			
			MessagePassing stub;
			try 
			{
				token = false;
				System.out.println(""+ sdf.format(cal.getTime()) + ":: I will pass the token to the next requestor who is "+ TokenRequestor);
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",TokenRequestor)+".utdallas.edu:5001/mutex");
				stub.receiveToken(myNodeID);
				
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Exited from Recieve Token " + sender);
	}

	@Override
	public void receiveReleaseMessage(int timestampReceived, int sender)
			throws RemoteException {
		
		while(requestMessageReceived[sender] == false);
		
		requestMessageReceived[sender] = false;
		
		//queue.remove(new QueueObject(this.timestamp, sender));
		
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestampReceived && q.getSender()==sender)
				queue.remove(q);
		}
		
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: I have received a release msg from " + sender
				+ " and below is my queue");
		
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		this.timestamp = Math.max(this.timestamp , timestampReceived);
		TestClient.setTimestamp(this.timestamp);
		System.out.println( "**Timestamp maxxed " +TestClient.getTimeStamp());
//		QueueObject queueObject = new QueueObject(timestamp, sender);
//		int tokenHolder = queueObject.getSender();
		
		if(queue.size() > 0)
		{
			QueueObject nextRequestorInQueue = queue.peek();
			MessagePassing stub;
			try 
			{
				System.out.println(""+ sdf.format(cal.getTime()) + ":: Since there are"
						+ " more requests in queue, I will ask token from " + sender + " and send it to " + nextRequestorInQueue.getSender());
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5001/mutex");
				stub.askToken( nextRequestorInQueue.getTimestamp() , nextRequestorInQueue.getSender());
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}
		
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Exited from Release from " + sender);
	}
	
	@Override
	public void releaseCriticalSection() throws RemoteException 
	{
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: My id is "+ myNodeID +" I am releasing my critical section ");
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Just Before Critical Section release of " + myNodeID);

		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		//TODO: check for the remove object.
		//Dequeue my own request from my queue.
		//queue.remove(new QueueObject(timestamp, myNodeID));
		
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestamp && q.getSender()==myNodeID)
				queue.remove(q);
		}
		
		criticalSection = false;		
		System.out.println(""+ sdf.format(cal.getTime()) + ":: Just After Critical Section release of " + myNodeID);
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				MessagePassing stub;
				try 
				{
					stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5001/mutex");
					
					if(requestMessageSent[QuorumMember] == false)
					{
						stub.receiveRequest(timestamp, myNodeID);
						requestMessageSent[QuorumMember] = true;
					}
					stub.receiveReleaseMessage(timestamp, myNodeID);
				} 
				catch (MalformedURLException e) 
				{
					e.printStackTrace();
				}
				catch (NotBoundException e) 
				{
					e.printStackTrace();
				}
			}
		}

	}
}
