package com.wheelphone.pet.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class LogcatStreamer{
	private static final String TAG = LogcatStreamer.class.getName();

	private ServerSocket mServerSocket; //Network server socket
	private Socket mSocket; //Network socket
	private PrintWriter mPrintWriter;  //Network socket stream writer (output)
	private Process mProcess; //Logcat process
	private BufferedReader mBufferedReader; //Logcat stream reader (input)

	//	private AtomicBoolean mActive = new AtomicBoolean(true);

	public LogcatStreamer(){
		mService.start();
	}


	Thread mService = new Thread(){
		@Override
		public void run() {
			Log.d(TAG, "starting LogcatStreamer thread");

			//Network stuff (socket and output)
			try {
				mServerSocket = new ServerSocket(4444);
			} catch (IOException e) {
				Log.e(TAG, "ServerSocket initialization error");
				e.printStackTrace();
			}

			try {
				mSocket = mServerSocket.accept();
			} catch (IOException e) {
				Log.e(TAG, "ServerSocket accept error");
				e.printStackTrace();
			}

			if (mSocket != null){
				try {
					mPrintWriter = new PrintWriter(mSocket.getOutputStream(), true);

					//Logcat stuff (logcat excecuter and reader)
					mProcess = Runtime.getRuntime().exec("logcat ");
					mBufferedReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));

					//Print line by line the logcat
					String line;
//					line = mBufferedReader.readLine();
					while ((line = mBufferedReader.readLine()) != null){
//						Log.d(TAG, "connected: " + mSocket.isConnected());
						
						//push a logcat line:
						mPrintWriter.println(line);
					}

					mProcess.destroy();
					mBufferedReader.close();
					mBufferedReader = null;
					mPrintWriter.close();
					mSocket.close();
					mServerSocket.close();
					Log.d(TAG, "Clean exit.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	};

	Thread mServiceStopper = new Thread(){
		@Override
		public void run() {
			try {
				if (mSocket != null){
					mProcess.destroy();
					mBufferedReader = null;
					mPrintWriter.close();
					mSocket.close();
					Log.d(TAG, "Closed connection on close");
				}
				mServerSocket.close();
			} catch (IOException e) {}
		}
	};

	public void stop(){
		mServiceStopper.start();
	}
}
