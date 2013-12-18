/*==============================================================================
            Copyright (c) 2010-2012 QUALCOMM Austria Research Center GmbH.
            All Rights Reserved.
            Qualcomm Confidential and Proprietary

@file
    FrameMarkersRenderer.java

@brief
    Sample for FrameMarkers

==============================================================================*/


package com.wheelphone.targetDocking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;

import com.qualcomm.QCAR.QCAR;


/** The renderer class for the FrameMarkers sample. */
public class FrameMarkersRenderer implements GLSurfaceView.Renderer
{
	public WheelphoneTargetDocking wheelphoneActivity;
    public boolean mIsActive = false;
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    
    /** Native function for initializing the renderer. */
    public native void initRendering();


    /** Native function to update the renderer. */
    public native void updateRendering(int width, int height);


    /** Called when the surface is created or recreated. */
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        DebugLog.LOGD("GLRenderer::onSurfaceCreated");

        // Call native function to initialize rendering:
        initRendering();

        // Call QCAR function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        QCAR.onSurfaceCreated();
    }


    /** Called when the surface changed size. */
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        DebugLog.LOGD("GLRenderer::onSurfaceChanged");

        // Call native function to update rendering when render surface
        // parameters have changed:
        updateRendering(width, height);

        mViewWidth = width;
        mViewHeight = height;
        
        // Call QCAR function to handle render surface size changes:
        QCAR.onSurfaceChanged(width, height);
    }


    /** The native render function. */
    public native void renderFrame();


    /** Called to draw the current frame. */
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our native function to render content
        renderFrame();
        
        // make sure the OpenGL rendering is finalized
        GLES20.glFinish();
        
        if (wheelphoneActivity.takeScreenshot()) {        	
            saveScreenShot(0, 0, mViewWidth, mViewHeight, "screenshot.png");
            wheelphoneActivity.takeScreenshot(false);
        }
    }
    
    void setReference(WheelphoneTargetDocking wtd) {
    	wheelphoneActivity = wtd;
    }
    
    public void updateMarkersInfo(int markerId, boolean detected, int i1, int i2, float dist, float z, float tpx, float tpz) {
    	wheelphoneActivity.updateMarkersInfo(markerId, detected, i1, i2, dist, z, tpx, tpz);
    }
    
    public void saveScreenShot() {
    	saveScreenShot(0, 0, mViewWidth, mViewHeight, "screenshot.png");
    }
    
    public void rotataImages() {
    	int k=0;
    	File sd=Environment.getExternalStorageDirectory();
    	String sourcePath="";
    	String destPath="";
    	File file;

    	// delete oldest images
		//for(k=0; k<4; k++) {
		//	sourcePath="/www/images/tour3_target"+Integer.toString(k+1);
		//	file = new File(sd,sourcePath);
		//	file.delete();    			
		//}
    	
		// rename prev to prev prev
    	for(k=0; k<4; k++) {
    		sourcePath="/www/images/tour2_target"+Integer.toString(k+1)+".png";
    		destPath="/www/images/tour3_target"+Integer.toString(k+1)+".png";
    		file = new File(sd, destPath);
    		file.delete();
    		file = new File(sd,sourcePath);
    		//if(file.exists()) {
    			file.renameTo(new File(sd, destPath));
    		//}
    		sourcePath="/www/tour2_target"+Integer.toString(k+1)+".html";
    		destPath="/www/tour3_target"+Integer.toString(k+1)+".html";
    		file = new File(sd, destPath);
    		file.delete();
    		file = new File(sd,sourcePath);
    		//if(file.exists()) {
    			file.renameTo(new File(sd, destPath));
    		//}        		
    	}
    	
		// rename current to prev
    	for(k=0; k<4; k++) {
    		sourcePath="/www/images/tour1_target"+Integer.toString(k+1)+".png";
    		destPath="/www/images/tour2_target"+Integer.toString(k+1)+".png";
    		file = new File(sd, destPath);
    		file.delete();    		
    		file = new File(sd,sourcePath);
    		//if(file.exists()) {
    			file.renameTo(new File(sd, destPath));
    		//}
    		sourcePath="/www/tour1_target"+Integer.toString(k+1)+".html";
    		destPath="/www/tour2_target"+Integer.toString(k+1)+".html";
    		file = new File(sd, destPath);
    		file.delete();    		
    		file = new File(sd,sourcePath);
    		//if(file.exists()) {
    			file.renameTo(new File(sd, destPath));
    		//}        		
    	}    	
    }
    
    private void saveScreenShot(int x, int y, int w, int h, String filename) {
        Bitmap bmp = grabPixels(x, y, w, h);
        int screenshot = wheelphoneActivity.getCurrentScreenshot();
        
        //if(screenshot == 2) {	// copy prev tour to prev-prev tour and last tour to prev tour (rotation)
        //	rotataImages();
        //}
        
        try {
            String path = Environment.getExternalStorageDirectory() + "/www/images/screenshot.png";   
            DebugLog.LOGD(path);             
            File file = new File(path);
            file.createNewFile();             
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(CompressFormat.PNG, 100, fos);
            fos.flush();             
            fos.close();        	
        	
            path = Environment.getExternalStorageDirectory() + "/www/images/tour1_target" + Integer.toString(screenshot) + ".png";   
            DebugLog.LOGD(path);             
            file = new File(path);
            file.createNewFile();             
            fos = new FileOutputStream(file);
            bmp.compress(CompressFormat.PNG, 100, fos);
            fos.flush();             
            fos.close();
             
    		File htmlFile = new File(Environment.getExternalStorageDirectory() + "/www/tour1_target" + Integer.toString(screenshot) + ".html");
   			htmlFile.delete();
    		htmlFile.createNewFile();
    		BufferedWriter buf = new BufferedWriter(new FileWriter(htmlFile, false));
    		Time today = new Time(Time.getCurrentTimezone());
    		today.setToNow();
    		buf.append(today.format("%k:%M:%S"));
    		buf.close();
    		
        } catch (Exception e) {
            DebugLog.LOGD(e.getStackTrace().toString());
        }
    }
  
    private Bitmap grabPixels(int x, int y, int w, int h) {
        int b[] = new int[w * (y + h)];
        int bt[] = new int[w * h];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
         
        GLES20.glReadPixels(x, 0, w, y + h,
                   GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
  
        for (int i = 0, k = 0; i < h; i++, k++) {
            for (int j = 0; j < w; j++) {
                int pix = b[i * w + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(h - k - 1) * w + j] = pix1;
            }
        }
  
        Bitmap sb = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
        return sb;
    }
    
}
