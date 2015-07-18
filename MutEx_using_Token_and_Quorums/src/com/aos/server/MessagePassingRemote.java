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
		//TODO intialize myNodeId
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
				stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",sender)+".utdallas.edu:5000/mutex");
				stub.receiveToken(myNodeID);
				token = false;
			}
			catch (MalformedURLException | NotBoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if(!token)
		{
			for(int QuorumMember : quorum)
			{
				if(QuorumMember != myNodeID && QuorumMember != sender)
				{
					//TODO implement request token function
					MessagePassing stub;
					try 
					{
						stub = (MessagePassing) Naming.lookup("rmi://net"+String.format("%02d",QuorumMember)+".utdallas.edu:5000/mutex");
						stub.askToken(timestamp, myNodeID);
					} 
					catch (MalformedURLException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (NotBoundException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void askToken(int timestamp, int sender) throws RemoteException
	{
		// TODO Auto-generated method stub
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
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
					stub.receiveReleaseMessage(timestamp, myNodeID);
				} 
				catch (MalformedURLException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (NotBoundException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		//Dequeue my own request from my queue.
		queue.poll();
		
	}
}
