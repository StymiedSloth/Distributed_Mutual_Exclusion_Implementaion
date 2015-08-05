package com.aos.threads;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.utd.aos.LoginView;
import com.utd.aos.TestBed;

public class TerminateThread implements Runnable {
	private Thread t;
	private HashMap<Integer, ArrayList<String> > hashMap;
	
	public TerminateThread()
	{
		hashMap = new HashMap<Integer, ArrayList<String>>();
	}
	
	@Override
	public void run()
	{
		Scanner s = new Scanner(System.in);
		if(s.hasNext())
		{
			if(s.next().equals("x"))
				TestBed.stopExecution();
			
		}
		s.close();
		
		try
		{
			String files = "";
			for(int i=1;i<= TestBed.TOTAL_NUMBER_OF_NODES;i++)
		      {
				files +=  i +".txt "; 
		      }
			JSch jsch=new JSch();    
			String[] cred = LoginView.getCredentials();
			
			Session session = jsch.getSession(cred[0], "net01.utdallas.edu", 22);
			
			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(cred[1]);
			session.setConfig("PreferredAuthentications","password");
			session.connect();
			
			Channel channel=session.openChannel("exec");
			
			((ChannelExec)channel).setCommand("head -c -1 -q "  + files +"> combined.txt");
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			InputStream ins = channel.getInputStream();

			channel.connect();
			
			
			ChannelSftp sftpchannel = (ChannelSftp) session.openChannel("sftp");
			sftpchannel.connect();
			sftpchannel.cd("/home/004/v/vd/vdr140330/");

			InputStream in = sftpchannel.get("combined.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line;
			int key = 1;
            while ((line = br.readLine()) != null || !channel.isClosed()) {
                if (line != null && line.length() >= 1) {
                	if(line.length() >= 1 && line.length() < 3 && !hashMap.containsKey(Integer.parseInt(line)))
                	{
                		key = Integer.parseInt(line);
                		hashMap.put(Integer.parseInt(line), new ArrayList<String>());
                	}
                	else if(line.length() > 3)
                	{
                		ArrayList<String> arrayList = hashMap.get(key);
                		arrayList.add(line);
                	}
            		
                }
            }
			in.close();
            br.close();
            channel.disconnect();
            sftpchannel.disconnect();
			session.disconnect();
			
			SortedSet<Integer> keys = new TreeSet<Integer>(hashMap.keySet());
			for (int mapKey : keys) { 
				ArrayList<String> arrayList  = hashMap.get(mapKey);
			    System.out.print("T" + String.format("%02d",mapKey) + ": ");
			    for(String item: arrayList)
			    	System.out.println(item);
			}
			
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	

	public void start()
	{
		 t = new Thread(this);
		 t.start();
	}
}
