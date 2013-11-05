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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView;
import android.os.Message;
import android.util.Log;

import com.qualcomm.QCAR.QCAR;


/** The renderer class for the FrameMarkers sample. */
public class FrameMarkersRenderer implements GLSurfaceView.Renderer
{
	public WheelphoneTargetDocking wheelphoneActivity;
    public boolean mIsActive = false;

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
    }
    
    void setReference(WheelphoneTargetDocking wtd) {
    	wheelphoneActivity = wtd;
    }
    
    public void updateMarkersInfo(int markerId, boolean detected, int i1, int i2, float dist, float z) {
    	wheelphoneActivity.updateMarkersInfo(markerId, detected, i1, i2, dist, z);
    }
    
}
