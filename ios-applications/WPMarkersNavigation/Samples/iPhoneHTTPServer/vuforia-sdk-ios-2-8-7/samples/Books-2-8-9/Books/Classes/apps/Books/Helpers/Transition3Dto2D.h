/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#ifndef _QCAR_TRANSITION_3D_TO_2D_H_
#define _QCAR_TRANSITION_3D_TO_2D_H_


#include <QCAR/QCAR.h>
#include <QCAR/Renderer.h>
#include <QCAR/Image.h>

class Transition3Dto2D
{
public:
    
    Transition3Dto2D(int screenWidth, int screenHeight, bool isPortraitMode);
    ~Transition3Dto2D();
    
    // Call this from the GL thread
    void initializeGL(unsigned int sProgramID);
    
    // Center of the screen is (0, 0)
    // centerX and centerY are pixel offsets from this point
    // width and height are also in pixels
    void setScreenRect(int centerX, int centerY, int width, int height);
    
    // Call this once to set up the transition
    // Note: inReverse and keepRendering are not currently used
    void startTransition(float duration, bool inReverse, bool keepRendering);
    
    // Transitions between textures 1 and 2
    // Transitions between target space and screen space
    void render(QCAR::Matrix44F projectionMatrix, QCAR::Matrix34F targetPose, QCAR::Vec2F trackableSize, GLuint texture1);
    
    // Returns true if transition has finished animating
    bool transitionFinished();
    
private:
    

    bool isActivityPortraitMode;
    int screenWidth;
    int screenHeight;
    QCAR::Vec4F screenRect;
    QCAR::Matrix44F identityMatrix;
    QCAR::Matrix44F orthoMatrix;
    
    unsigned int shaderProgramID;
    GLint normalHandle;
    GLint vertexHandle;
    GLint textureCoordHandle;
    GLint mvpMatrixHandle;

    
    float animationLength;
    int animationDirection;
    bool renderAfterCompletion;
    
    unsigned long animationStartTime;
    bool animationFinished;
    
    float stepTransition();
    QCAR::Matrix44F getFinalPositionMatrix();
    float deccelerate(float val);
    float accelerate(float val);
    void linearInterpolate(QCAR::Matrix44F* start, QCAR::Matrix44F* end, QCAR::Matrix44F* current, float elapsed);
    unsigned long getCurrentTimeMS();
    void updateScreenProperties(int screenWidth, int screenHeight, bool isPortraitMode);    
};

#endif //_QCAR_TRANSITION_3D_TO_2D_H_
