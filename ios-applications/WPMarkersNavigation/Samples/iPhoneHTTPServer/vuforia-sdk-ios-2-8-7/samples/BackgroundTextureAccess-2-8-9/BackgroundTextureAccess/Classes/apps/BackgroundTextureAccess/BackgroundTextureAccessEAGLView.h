/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

#import <QCAR/UIGLViewProtocol.h>

#import "Texture.h"
#import "SampleApplicationSession.h"


#define NUM_AUGMENTATION_TEXTURES 1


// BackgroundTextureAccess is a subclass of UIView and conforms to the informal protocol
// UIGLViewProtocol
@interface BackgroundTextureAccessEAGLView : UIView <UIGLViewProtocol> {
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
    
    // Coordinates of user touch
    float touchLocation_X;
    float touchLocation_Y;
    
    // ----- Video background OpenGL data -----
    struct tagVideoBackgroundShader {
        // These handles are required to pass the values to the video background
        // shaders
        GLuint vbShaderProgramID;
        GLuint vbVertexPositionHandle;
        GLuint vbVertexTexCoordHandle;
        GLuint vbTexSampler2DHandle;
        GLuint vbProjectionMatrixHandle;
        GLuint vbTouchLocationXHandle;
        GLuint vbTouchLocationYHandle;
        
        // This flag indicates whether the mesh values have been initialized
        bool vbMeshInitialized;
    } videoBackgroundShader;


    SampleApplicationSession * vapp;
}

@property (readwrite) float touchLocation_X;
@property (readwrite) float touchLocation_Y;

- (id)initWithFrame:(CGRect)frame appSession:(SampleApplicationSession *) app;

- (void)finishOpenGLESCommands;
- (void)freeOpenGLESResources;

- (void) setOrientationTransform:(CGAffineTransform)transform withLayerPosition:(CGPoint)pos;
- (void) cameraDidStart;
@end

