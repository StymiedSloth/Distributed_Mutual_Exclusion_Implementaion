package com.aos.common;

public class HandlerQueueObject implements Comparable<HandlerQueueObject>{

	private String requestFrom;
	private String method;
	private int timestamp;
	private int sender;
	private int receiver;
	
	public HandlerQueueObject(String requestFrom,String method, int timestamp, int sender, int receiver){
		this.requestFrom = requestFrom;
		this.method = method;
		this.sender = sender;
		this.receiver = receiver;
		this.timestamp = timestamp;
	}
	
	public int getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}

	public int getSender() {
		return sender;
	}

	public void setSender(int sender) {
		this.sender = sender;
	}

    public int compareTo(HandlerQueueObject other){
    	/*if(other.timestamp > this.timestamp)
    		return -1;
    	else if(other.timestamp == this.timestamp && other.sender < this.sender)
    			return 1;
		else 
			return 0;*/
    	
    	if(other.timestamp == this.timestamp && other.sender == this.sender)
    		return 0;
    	
    	if(other.timestamp > this.timestamp)
    		return -1;
    	
    	if(other.timestamp == this.timestamp && other.sender > this.sender)
    		return -1;
    	
    	return 1;
    }
    public String getRequestFrom() {
		return requestFrom;
	}

	public void setRequestFrom(String requestFrom) {
		this.requestFrom = requestFrom;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public int getReceiver() {
		return receiver;
	}

	public void setReceiver(int receiver) {
		this.receiver = receiver;
	}

	@Override
    public boolean equals(Object other)
	{	
		if (this == other)
			return true;
		if(((HandlerQueueObject)other).timestamp == this.timestamp && ((HandlerQueueObject)other).sender == this.sender)
			return true;	
		else 
			return false;
	}
}
