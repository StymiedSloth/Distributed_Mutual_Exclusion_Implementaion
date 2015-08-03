package com.aos.threads;

import java.util.Scanner;

import com.utd.aos.LoginView;

public class ExecuteThread implements Runnable {
	private Thread t;
	
	public ExecuteThread()
	{

	}
	
	@Override
	public void run()
	{
		LoginView.getView();
	}
	

	public void start()
	{
		 t = new Thread(this);
		 t.start();
	}
}
