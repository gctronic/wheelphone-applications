/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/



#import <UIKit/UIKit.h>

#import <QCAR/UIGLViewProtocol.h>

#import "Texture.h"
#import "SampleApplicationSession.h"
#import "WordlistView.h"

#define NUM_AUGMENTATION_TEXTURES 1


// EAGLView is a subclass of UIView and conforms to the informal protocol
// UIGLViewProtocol
@interface TextRecoEAGLView : UIView <UIGLViewProtocol> {
@private
    // OpenGL ES context
    EAGLContext *context;
    
    // The OpenGL ES names for the framebuffer and renderbuffers used to render
    // to this view
    GLuint defaultFramebuffer;
    GLuint colorRenderbuffer;
    GLuint depthRenderbuffer;

    // Shader handles
    GLuint lineShaderProgramID;
    GLint mvpMatrixHandle;
    GLint lineOpacityHandle;
    GLint lineColorHandle;
    GLint vertexHandle;
    
    int ROICenterX;
    int ROICenterY;
    int ROIWidth;
    int ROIHeight;
    
    // Texture used when rendering augmentation
    Texture* augmentationTexture[NUM_AUGMENTATION_TEXTURES];
    
    // View containing the list of detected words
    WordlistView *wordlistView;

    SampleApplicationSession * vapp;
}


- (id)initWithFrame:(CGRect)frame appSession:(SampleApplicationSession *) app;
- (void)finishOpenGLESCommands;
- (void)freeOpenGLESResources;
- (void) setRoiWidth:(int) width height:(int)height centerX:(int)centerX centerY:(int)centerY;

@end
