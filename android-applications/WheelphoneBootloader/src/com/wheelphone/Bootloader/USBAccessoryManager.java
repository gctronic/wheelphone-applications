package com.wheelphone.Bootloader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public abstract class USBAccessoryManager {
	
	public Context mContext;
	
	/**
	 * Enumeration of possible return values for the enable function
	 */
	public enum RETURN_CODES {
		DEVICE_MANAGER_IS_NULL, ACCESSORIES_LIST_IS_EMPTY, FILE_DESCRIPTOR_WOULD_NOT_OPEN, PERMISSION_PENDING, SUCCESS
	}
	
	public RETURN_CODES enable(Context context) {
		return RETURN_CODES.SUCCESS;
	}
	
	public void disable(Context context) {}
	
	public boolean isConnected() {
		return true;
	}
	
	void ignore(int num) {}
	
	int peek(byte[] array) {
		return 0;
	}
	
	int available() {
		return 0;
	}
	
	int read(byte[] array) {
		return 0;
	}
	
	public void write(byte[] data) {}
	
	public boolean isClosed() {
		return true;
	}
	
	void appendLog(String fileName, String text, boolean clearFile)
	{       
	   File logFile = new File("sdcard/" + fileName);
	   if (!logFile.exists()) {
	      try
	      {
	         logFile.createNewFile();
	      } 
	      catch (IOException e)
	      {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }
	   } else {
		   if(clearFile) {
			   logFile.delete();
			   try {
				   logFile.createNewFile();
			   } catch (IOException e) {
				   // TODO Auto-generated catch block
				   e.printStackTrace();
			   }
		   }
	   }
	   try
	   {
	      //BufferedWriter for performance, true to set append to file flag
	      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
	      buf.append(text);
	      buf.newLine(); 
	      buf.close();
	   }
	   catch (IOException e)
	   {
	      // TODO Auto-generated catch block
	      e.printStackTrace();
	   }
	}
	
	public String getVersion() {
		return "";
	}
	
	public String getModel() {
		return "";
	}

	public String getDescription() {
		return "";
	}
	
}