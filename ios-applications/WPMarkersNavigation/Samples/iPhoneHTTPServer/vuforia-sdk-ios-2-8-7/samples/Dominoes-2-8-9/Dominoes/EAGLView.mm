/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

// Subclassed from AR_EAGLView
#import "EAGLView.h"
#import "Dominoes.h"
#import "Texture.h"
#import <QCAR/Renderer.h>
#import <QCAR/VirtualButton.h>
#import <QCAR/UpdateCallback.h>

#import "QCARutils.h"
#import "ShaderUtils.h"


namespace {
    // Model scale factor
    const float kObjectScale = 3.0f;
    
    // Texture filenames
    const char* textureFilenames[] = {
        "texture_domino.png",
        "green_glow.png",
        "blue_glow.png"
    };
    
    class VirtualButton_UpdateCallback : public QCAR::UpdateCallback {
        virtual void QCAR_onUpdate(QCAR::State& state);
    } qcarUpdate;
    
}




@implementation EAGLView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    
	if (self)
    {
        // create list of textures we want loading - ARViewController will do this for us
        int nTextures = sizeof(textureFilenames) / sizeof(textureFilenames[0]);
        for (int i = 0; i < nTextures; ++i)
            [textureList addObject: [NSString stringWithUTF8String:textureFilenames[i]]];
    }
    return self;
}


// Pass touch events through to the Dominoes module
- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch* touch = [touches anyObject];
    CGPoint location = [touch locationInView:self];
    dominoesTouchEvent(ACTION_DOWN, 0, location.x, location.y);
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch* touch = [touches anyObject];
    CGPoint location = [touch locationInView:self];
    dominoesTouchEvent(ACTION_CANCEL, 0, location.x, location.y);
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch* touch = [touches anyObject];
    CGPoint location = [touch locationInView:self];
    dominoesTouchEvent(ACTION_UP, 0, location.x, location.y);
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch* touch = [touches anyObject];
    CGPoint location = [touch locationInView:self];
    dominoesTouchEvent(ACTION_MOVE, 0, location.x, location.y);
}

////////////////////////////////////////////////////////////////////////////////
// Initialise the application
- (void)initApplication
{
    initializeDominoes();
}


- (void) setup3dObjects
{
    dominoesSetTextures(textures);
}

- (void)initShaders
{
    [super initShaders];
    
    dominoesSetShaderProgramID(shaderProgramID);
    dominoesSetVertexHandle(vertexHandle);
    dominoesSetNormalHandle(normalHandle);
    dominoesSetTextureCoordHandle(textureCoordHandle);
    dominoesSetMvpMatrixHandle(mvpMatrixHandle);
    dominoesSetTexSampler2DHandle(texSampler2DHandle);
}

////////////////////////////////////////////////////////////////////////////////
// Do the things that need doing after initialisation
// called after QCAR is initialised but before the camera starts
- (void)postInitQCAR
{
    // Here we could make a QCAR::setHint call to set the maximum
    // number of simultaneous targets                
    // QCAR::setHint(QCAR::HINT_MAX_SIMULTANEOUS_IMAGE_TARGETS, 2);
    
    // register for our call back after tracker processing is done
    QCAR::registerCallback(&qcarUpdate);
}


////////////////////////////////////////////////////////////////////////////////
// Draw the current frame using OpenGL
//
// This method is called by QCAR when it wishes to render the current frame to
// the screen.
//
// *** QCAR will call this method on a single background thread ***
- (void)renderFrameQCAR
{
    if (APPSTATUS_CAMERA_RUNNING == qUtils.appStatus) {
        [self setFramebuffer];
        renderDominoes();
        [self presentFramebuffer];
    }
}


////////////////////////////////////////////////////////////////////////////////
// Callback function called by the tracker when each tracking cycle has finished
void VirtualButton_UpdateCallback::QCAR_onUpdate(QCAR::State& state)
{
    // Process the virtual button
    virtualButtonOnUpdate(state);
}

@end
