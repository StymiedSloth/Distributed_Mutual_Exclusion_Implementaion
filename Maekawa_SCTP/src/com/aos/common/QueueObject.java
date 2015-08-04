package com.aos.common;

public class QueueObject implements Comparable<QueueObject>{

	private int timestamp;
	private int sender;
	
	public QueueObject(int timestamp, int sender){
		
		this.sender = sender;
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

    public int compareTo(QueueObject other){
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
    @Override
    public boolean equals(Object other)
	{	
		if (this == other)
			return true;
		if(((QueueObject)other).timestamp == this.timestamp && ((QueueObject)other).sender == this.sender)
			return true;	
		else 
			return false;
	}
}
