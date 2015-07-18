package com.aos.common;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.PriorityBlockingQueue;

public interface MessagePassing extends Remote
{	
	public void MessagePass(int sender,String message)throws RemoteException;
	
	public void sendRequest(int timestamp, int sender)throws RemoteException;
	
	public void receiveRequest(int timestamp, int sender)throws RemoteException;
	
	public void askToken(int timestamp, int sender)throws RemoteException;
	
	public void receiveToken(int sender) throws RemoteException;
	
	public void releaseCriticalSection() throws RemoteException;
	
	public void receiveReleaseMessage(int timestamp, int sender) throws RemoteException;
}
