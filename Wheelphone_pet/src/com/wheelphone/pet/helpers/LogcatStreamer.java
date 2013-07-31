package com.wheelphone.pet.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import android.R;
import android.util.Log;

public class LogcatStreamer implements Runnable{
	private static final String TAG = LogcatStreamer.class.getName();

	private ServerSocket mServerSocket; //Network server socket
	private Socket mSocket; //Network socket
	private PrintWriter mPrintWriter;  //Network socket stream writer (output)
	private Process mProcess; //Logcat process
	private BufferedReader mBufferedReader; //Logcat stream reader (input)

	private AtomicBoolean mActive = new AtomicBoolean(true);
	
	@Override
	public void run() {
		try {
			//Network stuff (socket and output)
			mServerSocket = new ServerSocket(4444);
			Log.d(TAG, "trying to print1");
			mSocket = mServerSocket.accept();
			mPrintWriter = new PrintWriter(mSocket.getOutputStream(), true);

			//Logcat stuff (logcat excecuter and reader)
			mProcess = Runtime.getRuntime().exec("logcat ");
			mBufferedReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));

			Log.d(TAG, "trying to print");
			//Print line by line the logcat
			String line;
			while (mActive.get() && (line = mBufferedReader.readLine()) != null){ 
				//push a logcat line:
				mPrintWriter.println(line);
			}
			Log.d(TAG, "finished printing");

			mProcess.destroy();
			mBufferedReader.close();
			mBufferedReader = null;
			mPrintWriter.close();
			mSocket.close();
			mServerSocket.close();
			Log.d(TAG, "Clean exit.");
		} 
		catch (IOException e) {
			Log.e(TAG, "Problem with logcat streamer");
			e.printStackTrace();
		}
	}
	
	public void stop(){
		Log.d(TAG, "State: " + mServerSocket);
//		mServerSocket.close();
		mActive.set(false);
	}
}
