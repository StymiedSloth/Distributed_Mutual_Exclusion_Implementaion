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
	
	MessagePassingRemote(int myNodeID,PriorityBlockingQueue<QueueObject> queue, int[] quorum, Boolean token) throws RemoteException 
	{
		super();
		this.myNodeID = myNodeID;
		this.queue = queue;
		this.quorum  = quorum;
		this.token = token;
		this.criticalSection = false;
	}

	@Override
	public void MessagePass(int sender, String message) throws RemoteException
	{
		System.out.println("Message got from:" + sender);
		System.out.println("Message is " + message);
	}

	@Override
	public void sendRequest(int timestamp, int sender) throws RemoteException {
		this.timestamp++;
		
		QueueObject queueObject = new QueueObject(timestamp, myNodeID);
		
		queue.add(queueObject);
		
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				MessagePassing stub;
				try 
				{
					stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5000/mutex");
					stub.receiveRequest(timestamp, myNodeID);
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
		System.out.println("Message Recieved to " + myNodeID + " from " + sender);
		queue.add(new QueueObject(timestamp, sender));
		
		if(token && !criticalSection)
		{
			
			 QueueObject queueObject = queue.peek();
			 
			 int TokenRequestor = queueObject.getSender();
			 
			 MessagePassing stub;
			try 
			{
				//TODO DIsucss with team - This might be an issue, since any asker will get the token, we 
				//have to dequeue the first request from queue and only then send it out. 
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5000/mutex");
				stub.receiveToken(myNodeID);
				token = false;
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
						stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5000/mutex");
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
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5000/mutex");
				stub.receiveToken(myNodeID);
				token = false;
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
		
		if(!queue.isEmpty())
		{
			QueueObject queueObject = queue.peek();
			 
			int TokenRequestor = queueObject.getSender();
			 
			if(TokenRequestor == myNodeID)
			{
				criticalSection = true;
				return;
			}
			
			MessagePassing stub;
			try 
			{
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",TokenRequestor)+".utdallas.edu:5000/mutex");
				stub.receiveToken(myNodeID);
				token = false;
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void receiveReleaseMessage(int timestamp, int sender)
			throws RemoteException {
		System.out.println("My id is "+ myNodeID +" I have received a release msg from " + sender);
		
		this.timestamp = Math.max(this.timestamp , timestamp);
		
		QueueObject queueObject = queue.poll();
		
		int tokenHolder = queueObject.getSender();
		
		if(queue.size() > 0)
		{
			QueueObject nextRequestorInQueue = queue.peek();
			MessagePassing stub;
			try 
			{
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",tokenHolder)+".utdallas.edu:5000/mutex");
				stub.askToken( nextRequestorInQueue.getTimestamp() , nextRequestorInQueue.getSender());
			}
			catch (MalformedURLException | NotBoundException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void releaseCriticalSection() throws RemoteException {
		for(int QuorumMember : quorum)
		{
			if(QuorumMember != myNodeID)
			{
				MessagePassing stub;
				try 
				{
					stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5000/mutex");
					//TODO Discuss with team if the below needs to be a Queue.peek() since a node can make multiple requests,
					//the first item in it's queue would be the first request which has been handled
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
		
		//Dequeue my own request from my queue.
		queue.poll();
		
	}
}
