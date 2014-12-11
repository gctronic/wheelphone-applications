/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

#import <QCAR/UIGLViewProtocol.h>

#import "Texture.h"
#import "SampleApplicationSession.h"
#import "VideoPlayerHelper.h"


#define NUM_AUGMENTATION_TEXTURES 5
#define NUM_VIDEO_TARGETS 2

// VideoPlayback is a subclass of UIView and conforms to the informal protocol
// UIGLViewProtocol
@interface VideoPlaybackEAGLView : UIView <UIGLViewProtocol> {
@private
    // Instantiate one VideoPlayerHelper per target
    VideoPlayerHelper* videoPlayerHelper[NUM_VIDEO_TARGETS];
    float videoPlaybackTime[NUM_VIDEO_TARGETS];
    
    
    VideoPlaybackViewController * videoPlaybackViewController ;
    
    // Timer to pause on-texture video playback after tracking has been lost.
    // Note: written/read on two threads, but never concurrently
    NSTimer* trackingLostTimer;
    
    // Coordinates of user touch
    float touchLocation_X;
    float touchLocation_Y;
    
    // Lock to synchronise data that is (potentially) accessed concurrently
    NSLock* dataLock;
    
    
    // OpenGL ES context
    EAGLContext *context;
    
    // The OpenGL ES names for the framebuffer and renderbuffers used to render
    // to this view
    GLuint defaultFramebuffer;
    GLuint colorRenderbuffer;
    GLuint depthRenderbuffer;

    // Shader handles
    GLuint shaderProgramID;
    GLint vertexHandle;
    GLint normalHandle;
    GLint textureCoordHandle;
    GLint mvpMatrixHandle;
    GLint texSampler2DHandle;
    
    // Texture used when rendering augmentation
    Texture* augmentationTexture[NUM_AUGMENTATION_TEXTURES];

    SampleApplicationSession * vapp;
}

- (id)initWithFrame:(CGRect)frame rootViewController:(VideoPlaybackViewController *) rootViewController appSession:(SampleApplicationSession *) app;

- (void) prepare;
- (void) dismiss;

- (void)finishOpenGLESCommands;
- (void)freeOpenGLESResources;

- (bool) handleTouchPoint:(CGPoint) touchPoint;
- (bool) handleDoubleTouchPoint:(CGPoint) touchPoint;

- (void) preparePlayers;
- (void) dismissPlayers;

@end

