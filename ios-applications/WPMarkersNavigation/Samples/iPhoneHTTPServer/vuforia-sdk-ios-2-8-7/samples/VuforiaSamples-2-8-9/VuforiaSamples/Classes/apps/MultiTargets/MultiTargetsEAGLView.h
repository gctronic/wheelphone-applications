/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <UIKit/UIKit.h>

#import <QCAR/UIGLViewProtocol.h>

#import "Texture.h"
#import "SampleApplicationSession.h"

#define NUM_AUGMENTATION_TEXTURES 2

// MultiTargets is a subclass of UIView and conforms to the informal protocol
// UIGLViewProtocol
@interface MultiTargetsEAGLView : UIView <UIGLViewProtocol> {
@private
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

    BOOL offTargetTrackingEnabled;

    SampleApplicationSession * vapp;
}

- (id)initWithFrame:(CGRect)frame appSession:(SampleApplicationSession *) app;

- (void)finishOpenGLESCommands;
- (void)freeOpenGLESResources;
- (void) setOffTargetTrackingMode:(BOOL) enabled;

@end

