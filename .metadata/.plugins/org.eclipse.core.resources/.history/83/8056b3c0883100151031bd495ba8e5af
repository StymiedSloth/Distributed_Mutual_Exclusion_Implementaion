package com.aos.server;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.*;
import java.util.concurrent.PriorityBlockingQueue;

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
	public void MessagePass(int sender, String message) throws RemoteException
	{
		System.out.println("Message got from:" + sender);
		System.out.println("Message is " + message);
	}

	@Override
	public void sendRequest(int timestamp, int sender) throws RemoteException 
	{
		System.out.println("Request Sent from " + sender);
		this.timestamp = timestamp + 1;
		
		for (boolean b:requestMessageSent)
			b = false;
		
		QueueObject queueObject = new QueueObject(this.timestamp, myNodeID);
		
		if(!queue.contains(queueObject))
			queue.add(queueObject);
		
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		QueueObject obj = queue.peek();
		
		if(obj.getSender() == myNodeID && token == true)
		{
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
			
			System.out.println("Release Critical Section " + myNodeID);
			
			for(QueueObject q : queue)
			{
				System.out.println(q.getTimestamp() + " " + q.getSender());
			}
			
			criticalSection = false;	
			
			return;

		}
		
		for(int QuorumMember : quorum)
		{
			System.out.println("Qurum Member " + QuorumMember);
			if(QuorumMember != myNodeID)
			{
				MessagePassing stub;
				try 
				{
					System.out.println("Request Message send to " + QuorumMember);
					
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
		System.out.println("Exited from sendRequest");
	}
	
	@Override
	public void receiveRequest(int timestamp, int sender)
			throws RemoteException
	{
		System.out.println("Message Recieved to " + myNodeID + " from " + sender);
		
		if(!queue.contains(new QueueObject(timestamp, sender)))
			queue.add(new QueueObject(timestamp, sender));
		
		requestMessageReceived[sender] = true;
		
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
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5001/mutex");
				stub.receiveToken(myNodeID);
				System.out.println("Token send to" + sender);
				
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
		System.out.println("Exited from recieve Request from " + sender);
	}

	@Override
	public void askToken(int timestamp, int sender) throws RemoteException
	{
		System.out.println("Ask Token Recieved to " + myNodeID + " from " + sender);
		
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
		System.out.println("My id is "+ myNodeID +" I have the token from " + sender);
		
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
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",TokenRequestor)+".utdallas.edu:5001/mutex");
				stub.receiveToken(myNodeID);
				
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}
		
		System.out.println("Exited from Recieve Token " + sender);
	}

	@Override
	public void receiveReleaseMessage(int timestamp, int sender)
			throws RemoteException {
		
		while(requestMessageReceived[sender] == false);
		
		requestMessageReceived[sender] = false;
		
		//queue.remove(new QueueObject(this.timestamp, sender));
		
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestamp && q.getSender()==sender)
				queue.remove(q);
		}
		
		System.out.println("My id is "+ myNodeID +" I have received a release msg from " + sender);
		
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
		
		this.timestamp = Math.max(this.timestamp , timestamp);

		
		QueueObject queueObject = new QueueObject(timestamp, sender);
		int tokenHolder = queueObject.getSender();
		
		if(queue.size() > 0)
		{
			QueueObject nextRequestorInQueue = queue.peek();
			MessagePassing stub;
			try 
			{
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",tokenHolder)+".utdallas.edu:5001/mutex");
				stub.askToken( nextRequestorInQueue.getTimestamp() , nextRequestorInQueue.getSender());
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}
		
		System.out.println("Exited from Release from " + sender);
	}
	
	@Override
	public void releaseCriticalSection() throws RemoteException 
	{
		System.out.println("Critical Section release of " + myNodeID);

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
		
		//TODO: check for the remove object.
		//Dequeue my own request from my queue.
		//queue.remove(new QueueObject(timestamp, myNodeID));
		
		for(QueueObject q : queue)
		{
			if(q.getTimestamp()== timestamp && q.getSender()==myNodeID)
				queue.remove(q);
		}
		
		criticalSection = false;		
	
		for(QueueObject q : queue)
		{
			System.out.println(q.getTimestamp() + " " + q.getSender());
		}
	}
}
