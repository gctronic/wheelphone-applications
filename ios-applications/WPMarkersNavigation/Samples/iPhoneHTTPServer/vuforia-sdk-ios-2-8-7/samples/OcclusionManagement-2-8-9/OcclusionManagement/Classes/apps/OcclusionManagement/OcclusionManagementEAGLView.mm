/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <QuartzCore/QuartzCore.h>
#import <OpenGLES/ES2/gl.h>
#import <OpenGLES/ES2/glext.h>
#import <sys/time.h>

#import <QCAR/QCAR.h>
#import <QCAR/State.h>
#import <QCAR/Tool.h>
#import <QCAR/Renderer.h>
#import <QCAR/MultiTargetResult.h>
#import <QCAR/TrackableResult.h>
#import <QCAR/VideoBackgroundConfig.h>
#import <QCAR/VideoBackgroundTextureInfo.h>

#import "OcclusionManagementEAGLView.h"
#import "Texture.h"
#import "SampleApplicationUtils.h"
#import "SampleApplicationShaderUtils.h"
#import "Teapot.h"
#import "Cube.h"


//******************************************************************************
// *** OpenGL ES thread safety ***
//
// OpenGL ES on iOS is not thread safe.  We ensure thread safety by following
// this procedure:
// 1) Create the OpenGL ES context on the main thread.
// 2) Start the QCAR camera, which causes QCAR to locate our EAGLView and start
//    the render thread.
// 3) QCAR calls our renderFrameQCAR method periodically on the render thread.
//    The first time this happens, the defaultFramebuffer does not exist, so it
//    is created with a call to createFramebuffer.  createFramebuffer is called
//    on the main thread in order to safely allocate the OpenGL ES storage,
//    which is shared with the drawable layer.  The render (background) thread
//    is blocked during the call to createFramebuffer, thus ensuring no
//    concurrent use of the OpenGL ES context.
//
//******************************************************************************


namespace {
    // --- Data private to this unit ---

    
    // Texture filenames
    const char* textureFilenames[] = {
        "background.png", // 0
        "teapot.png",  // 1
        "mask.png", // 2
    };
    
    float vbOrthoQuadVertices[] =
    {
        -1.0f, -1.0f, 0.0f,
        1.0f, -1.0f, 0.0f,
        1.0f,  1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f
    };
    
    float vbOrthoQuadTexCoords[] =
    {
        0.0,    1.0,
        1.0,    1.0,
        1.0,    0.0,
        0.0,    0.0
    };
    
    GLbyte vbOrthoQuadIndices[]=
    {
        0, 1, 2, 2, 3, 0
    };
    
    float   vbOrthoProjMatrix[16];
    
    unsigned int vbShaderProgramOcclusionID     = 0;
    GLint vbVertexPositionOcclusionHandle      = 0;
    GLint vbVertexTexCoordOcclusionHandle      = 0;
    GLint vbTexSamplerVideoOcclusionHandle     = 0;
    GLint vbProjectionMatrixOcclusionHandle    = 0;
    GLint vbTexSamplerMaskOcclusionHandle      = 0;
    GLint vbViewportOriginHandle               = 0;
    GLint vbViewportSizeHandle                 = 0;
    GLint vbTextureRatioHandle                 = 0;
    
    unsigned int vbShaderProgramOcclusionReflectID  = 0;
    GLint vbVertexPositionOcclusionReflectHandle   = 0;
    GLint vbVertexTexCoordOcclusionReflectHandle   = 0;
    GLint vbTexSamplerVideoOcclusionReflectHandle  = 0;
    GLint vbProjectionMatrixOcclusionReflectHandle = 0;
    GLint vbTexSamplerMaskOcclusionReflectHandle   = 0;
    GLint vbViewportOriginReflectHandle            = 0;
    GLint vbViewportSizeReflectHandle              = 0;
    GLint vbTextureRatioReflectHandle              = 0;
    
    unsigned int vbShaderProgramID              = 0;
    GLint vbVertexPositionHandle               = 0;
    GLint vbVertexTexCoordHandle               = 0;
    GLint vbTexSamplerVideoHandle              = 0;
    GLint vbProjectionMatrixHandle             = 0;
    
    // Constants:
    const float kCubeScaleX = 120.0f * 0.75f / 2.0f;
    const float kCubeScaleY = 120.0f * 1.00f / 2.0f;
    const float kCubeScaleZ = 120.0f * 0.50f / 2.0f;
    
    static const float kTeapotScaleX            = 120.0f * 0.015f;
    static const float kTeapotScaleY            = 120.0f * 0.015f;
    static const float kTeapotScaleZ            = 120.0f * 0.015f;
}


@interface OcclusionManagementEAGLView (PrivateMethods)

- (void)initShaders;
- (void)createFramebuffer;
- (void)deleteFramebuffer;
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;

@end


@implementation OcclusionManagementEAGLView

// You must implement this method, which ensures the view's underlying layer is
// of type CAEAGLLayer
+ (Class)layerClass
{
    return [CAEAGLLayer class];
}


//------------------------------------------------------------------------------
#pragma mark - Lifecycle

- (id)initWithFrame:(CGRect)frame appSession:(SampleApplicationSession *) app
{
    self = [super initWithFrame:frame];
    
    if (self) {
        vapp = app;
        // Enable retina mode if available on this device
        if (YES == [vapp isRetinaDisplay]) {
            [self setContentScaleFactor:2.0f];
        }
        
        CAEAGLLayer *eaglLayer = (CAEAGLLayer *)self.layer;
        
        eaglLayer.opaque = TRUE;
        eaglLayer.drawableProperties = [NSDictionary dictionaryWithObjectsAndKeys:
                                        [NSNumber numberWithBool:FALSE], kEAGLDrawablePropertyRetainedBacking,
                                        kEAGLColorFormatRGBA8, kEAGLDrawablePropertyColorFormat,
                                        nil];
        
        
        
        // Load the augmentation textures
        for (int i = 0; i < NUM_AUGMENTATION_TEXTURES; ++i) {
            augmentationTexture[i] = [[Texture alloc] initWithImageFile:[NSString stringWithCString:textureFilenames[i] encoding:NSASCIIStringEncoding]];
        }

        // Create the OpenGL ES context
        context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
        
        // The EAGLContext must be set for each thread that wishes to use it.
        // Set it the first time this method is called (on the main thread)
        if (context != [EAGLContext currentContext]) {
            [EAGLContext setCurrentContext:context];
        }
        
        // Generate the OpenGL ES texture and upload the texture data for use
        // when rendering the augmentation
        for (int i = 0; i < NUM_AUGMENTATION_TEXTURES; ++i) {
            GLuint textureID;
            glGenTextures(1, &textureID);
            [augmentationTexture[i] setTextureID:textureID];
            glBindTexture(GL_TEXTURE_2D, textureID);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, [augmentationTexture[i] width], [augmentationTexture[i] height], 0, GL_RGBA, GL_UNSIGNED_BYTE, (GLvoid*)[augmentationTexture[i] pngData]);
        }

        [self initShaders];
    }
    
    return self;
}


- (void)dealloc
{
    [self deleteFramebuffer];
    
    // Tear down context
    if ([EAGLContext currentContext] == context) {
        [EAGLContext setCurrentContext:nil];
    }
    
    [context release];

    for (int i = 0; i < NUM_AUGMENTATION_TEXTURES; ++i) {
        [augmentationTexture[i] release];
    }

    [super dealloc];
}

- (void) setOrientationTransform:(CGAffineTransform)transform withLayerPosition:(CGPoint)pos {
    self.layer.position = pos;
    self.transform = transform;
}


- (void)finishOpenGLESCommands
{
    // Called in response to applicationWillResignActive.  The render loop has
    // been stopped, so we now make sure all OpenGL ES commands complete before
    // we (potentially) go into the background
    if (context) {
        [EAGLContext setCurrentContext:context];
        glFinish();
    }
}


- (void)freeOpenGLESResources
{
    // Called in response to applicationDidEnterBackground.  Free easily
    // recreated OpenGL ES resources
    [self deleteFramebuffer];
    glFinish();
}


//------------------------------------------------------------------------------
#pragma mark - UIGLViewProtocol methods

// Draw the current frame using OpenGL
//
// This method is called by QCAR when it wishes to render the current frame to
// the screen.
//
// *** QCAR will call this method periodically on a background thread ***
- (void)renderFrameQCAR
{
    [self setFramebuffer];
    SampleApplicationUtils::checkGlError("Check gl errors prior render Frame");
    
    // Clear color and depth buffer
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    // Render video background:
    QCAR::State state = QCAR::Renderer::getInstance().begin();
    
    const QCAR::VIDEO_BACKGROUND_REFLECTION reflection = QCAR::Renderer::getInstance().getVideoBackgroundConfig().mReflection;
    
    const QCAR::VideoBackgroundTextureInfo texInfo =
    QCAR::Renderer::getInstance().getVideoBackgroundTextureInfo();
    float uRatio =
    ((float)texInfo.mImageSize.data[0]/(float)texInfo.mTextureSize.data[0]);
    float vRatio =
    ((float)texInfo.mImageSize.data[1]/(float)texInfo.mTextureSize.data[1]);
    
    // Y coords for the video texture remain const
    vbOrthoQuadTexCoords[1] = vRatio;
    vbOrthoQuadTexCoords[3] = vRatio;
    
    vbOrthoQuadTexCoords[0] = 0;
    vbOrthoQuadTexCoords[2] = uRatio;
    vbOrthoQuadTexCoords[4] = uRatio;
    vbOrthoQuadTexCoords[6] = 0;
    
    vbOrthoQuadTexCoords[1] = vRatio;
    vbOrthoQuadTexCoords[3] = vRatio;
    vbOrthoQuadTexCoords[5] = 0;
    vbOrthoQuadTexCoords[7] = 0;
    
    
    
    // This section renders the video background with a
    // custom shader defined in Shaders.h
    
    const GLuint vbVideoTextureUnit = 0;
    const GLuint vbMaskTextureUnit = 1;
    if (!QCAR::Renderer::getInstance().bindVideoBackground(vbVideoTextureUnit))
    {
        // No video frame available, take no further action
        QCAR::Renderer::getInstance().end();
        return;
    }
    
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    
    struct tagViewport theViewPort;
    theViewPort.posX = vapp.viewport.posX;
    theViewPort.posY = vapp.viewport.posY;
    theViewPort.sizeX = vapp.viewport.sizeX;
    theViewPort.sizeY = vapp.viewport.sizeY;
    
    
    glViewport(theViewPort.posX, theViewPort.posY,
               theViewPort.sizeX, theViewPort.sizeY);
    
    glUseProgram(vbShaderProgramID);
    glVertexAttribPointer(vbVertexPositionHandle, 3, GL_FLOAT, GL_FALSE, 0,
                          vbOrthoQuadVertices);
    glVertexAttribPointer(vbVertexTexCoordHandle, 2, GL_FLOAT, GL_FALSE, 0,
                          vbOrthoQuadTexCoords);
    glUniform1i(vbTexSamplerVideoHandle, vbVideoTextureUnit);
    glUniformMatrix4fv(vbProjectionMatrixHandle, 1, GL_FALSE,
                       &vbOrthoProjMatrix[0]);
    
    // Render the video background with the custom shader
    glEnableVertexAttribArray(vbVertexPositionHandle);
    glEnableVertexAttribArray(vbVertexTexCoordHandle);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, vbOrthoQuadIndices);
    glDisableVertexAttribArray(vbVertexPositionHandle);
    glDisableVertexAttribArray(vbVertexTexCoordHandle);
    
    // Wrap up this rendering
    glUseProgram(0);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, 0);
    
    SampleApplicationUtils::checkGlError("Rendering of the video background");
    //
    ////////////////////////////////////////////////////////////////////////////
    
    glEnable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    
    // Did we find any trackables this frame?
    if (state.getNumTrackableResults())
    {
        // Get the trackable:
        const QCAR::TrackableResult* result=NULL;
        int numResults=state.getNumTrackableResults();
        
        // Browse results searching for the MultiTarget
        for (int j=0;j<numResults;j++)
        {
            result = state.getTrackableResult(j);
            if (result->getType().isOfType(QCAR::MultiTargetResult::getClassType())) break;
            result=NULL;
        }
        
        // If it was not found exit
        if (result==NULL)
        {
            // Clean up and leave
            glDisable(GL_BLEND);
            glDisable(GL_DEPTH_TEST);
            
            QCAR::Renderer::getInstance().end();
            return;
        }
        
        
        QCAR::Matrix44F modelViewMatrix = QCAR::Tool::convertPose2GLMatrix(result->getPose());
        QCAR::Matrix44F modelViewProjectionCube;
        QCAR::Matrix44F modelViewProjectionTeapot;
        
        SampleApplicationUtils::scalePoseMatrix(kCubeScaleX, kCubeScaleY, kCubeScaleZ,
                                     &modelViewMatrix.data[0]);
        
        SampleApplicationUtils::multiplyMatrix(&vapp.projectionMatrix.data[0],
                                    &modelViewMatrix.data[0],
                                    &modelViewProjectionCube.data[0]);
        
        ////////////////////////////////////////////////////////////////////////
        // First, we render the faces that serve as a "background" to the teapot
        // This helps the user to have a visually constrained space
        // (otherwise the teapot looks floating in space)
        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);
        
        if(reflection == QCAR::VIDEO_BACKGROUND_REFLECTION_ON)
            glFrontFace(GL_CW);  //Front camera
        else
            glFrontFace(GL_CCW);   //Back camera
        
        glUseProgram(shaderProgramID);
        
        glVertexAttribPointer(vertexHandle, 3, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &cubeVertices[0]);
        glVertexAttribPointer(normalHandle, 3, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &cubeNormals[0]);
        glVertexAttribPointer(textureCoordHandle, 2, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &cubeTexCoords[0]);
        glEnableVertexAttribArray(vertexHandle);
        glEnableVertexAttribArray(normalHandle);
        glEnableVertexAttribArray(textureCoordHandle);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, [augmentationTexture[0] textureID]);
        glUniformMatrix4fv(mvpMatrixHandle, 1, GL_FALSE,
                           (GLfloat*)&modelViewProjectionCube.data[0] );
        glDrawElements(GL_TRIANGLES, NUM_CUBE_INDEX, GL_UNSIGNED_SHORT,
                       (const GLvoid*) &cubeIndices[0]);
        
        glCullFace(GL_BACK);
        
        SampleApplicationUtils::checkGlError("Back faces of the box");
        //
        ////////////////////////////////////////////////////////////////////////
        
        
        ////////////////////////////////////////////////////////////////////////
        // Then, we render the actual teapot
        modelViewMatrix = QCAR::Tool::convertPose2GLMatrix(result->getPose());
        SampleApplicationUtils::translatePoseMatrix(0.0f*120.0f, -0.0f*120.0f,
                                         -0.17f*120.0f, &modelViewMatrix.data[0]);
        SampleApplicationUtils::rotatePoseMatrix(90.0f, 0.0f, 0, 1,
                                      &modelViewMatrix.data[0]);
        SampleApplicationUtils::scalePoseMatrix(kTeapotScaleX, kTeapotScaleY, kTeapotScaleZ,
                                     &modelViewMatrix.data[0]);
        SampleApplicationUtils::multiplyMatrix(&vapp.projectionMatrix.data[0],
                                    &modelViewMatrix.data[0],
                                    &modelViewProjectionTeapot.data[0]);
        glUseProgram(shaderProgramID);
        glEnableVertexAttribArray(vertexHandle);
        glEnableVertexAttribArray(normalHandle);
        glEnableVertexAttribArray(textureCoordHandle);
        glVertexAttribPointer(vertexHandle, 3, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &teapotVertices[0]);
        glVertexAttribPointer(normalHandle, 3, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &teapotNormals[0]);
        glVertexAttribPointer(textureCoordHandle, 2, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &teapotTexCoords[0]);
        glBindTexture(GL_TEXTURE_2D, [augmentationTexture[1] textureID]);
        glUniformMatrix4fv(mvpMatrixHandle, 1, GL_FALSE,
                           (GLfloat*)&modelViewProjectionTeapot.data[0] );
        glDrawElements(GL_TRIANGLES, NUM_TEAPOT_OBJECT_INDEX, GL_UNSIGNED_SHORT,
                       (const GLvoid*) &teapotIndices[0]);
        glBindTexture(GL_TEXTURE_2D, 0);
        ////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        // Finally, we render the top layer based on the video image
        // this is the layer that actually gives the "transparent look"
        // notice that we use the mask.png (textures[2]->mTextureID)
        // to define how the transparency looks
        glDepthFunc(GL_LEQUAL);
        glActiveTexture(GL_TEXTURE0);
        QCAR::Renderer::getInstance().bindVideoBackground(vbVideoTextureUnit);
        glActiveTexture(GL_TEXTURE0 + vbMaskTextureUnit);
        glBindTexture(GL_TEXTURE_2D, [augmentationTexture[2] textureID]);
        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
        
        glViewport(theViewPort.posX, theViewPort.posY,
                   theViewPort.sizeX, theViewPort.sizeY);
        
        glUseProgram(vbShaderProgramOcclusionID);
        glVertexAttribPointer(vbVertexPositionOcclusionHandle, 3, GL_FLOAT,
                              GL_FALSE, 0, (const GLvoid*) &cubeVertices[0]);
        glVertexAttribPointer(vbVertexTexCoordOcclusionHandle, 2, GL_FLOAT,
                              GL_FALSE, 0, (const GLvoid*) &cubeTexCoords[0]);
        glEnableVertexAttribArray(vbVertexPositionOcclusionHandle);
        glEnableVertexAttribArray(vbVertexTexCoordOcclusionHandle);
        
        glUniform2f(vbViewportOriginHandle,
                    theViewPort.posX, theViewPort.posY);
        glUniform2f(vbViewportSizeHandle, theViewPort.sizeX, theViewPort.sizeY);
        glUniform2f(vbTextureRatioHandle, uRatio, vRatio);
        
        glUniform1i(vbTexSamplerVideoOcclusionHandle, vbVideoTextureUnit);
        glUniform1i(vbTexSamplerMaskOcclusionHandle, vbMaskTextureUnit);
        glUniformMatrix4fv(vbProjectionMatrixOcclusionHandle, 1, GL_FALSE,
                           (GLfloat*)&modelViewProjectionCube.data[0] );
        glDrawElements(GL_TRIANGLES, NUM_CUBE_INDEX, GL_UNSIGNED_SHORT,
                       (const GLvoid*) &cubeIndices[0]);
        glDisableVertexAttribArray(vbVertexPositionOcclusionHandle);
        glDisableVertexAttribArray(vbVertexTexCoordOcclusionHandle);
        glUseProgram(0);
        glDepthFunc(GL_LESS);
        SampleApplicationUtils::checkGlError("Transparency layer");
        //
        ////////////////////////////////////////////////////////////////////////
    }
    
    glDisable(GL_BLEND);
    glDisable(GL_DEPTH_TEST);
    
    glDisableVertexAttribArray(vertexHandle);
    glDisableVertexAttribArray(normalHandle);
    glDisableVertexAttribArray(textureCoordHandle);
    
    QCAR::Renderer::getInstance().end();
    [self presentFramebuffer];

}

//------------------------------------------------------------------------------
#pragma mark - OpenGL ES management

- (void)initShaders
{
    shaderProgramID = [SampleApplicationShaderUtils createProgramWithVertexShaderFileName:@"Simple.vertsh"
                                                   fragmentShaderFileName:@"Simple.fragsh"];

    if (0 < shaderProgramID) {
        vertexHandle = glGetAttribLocation(shaderProgramID, "vertexPosition");
        normalHandle = glGetAttribLocation(shaderProgramID, "vertexNormal");
        textureCoordHandle = glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
        texSampler2DHandle  = glGetUniformLocation(shaderProgramID,"texSampler2D");
    }
    else {
        NSLog(@"Could not initialise augmentation shader");
    }
        
    vbShaderProgramID                   = [SampleApplicationShaderUtils createProgramWithVertexShaderFileName:@"PassThrough.vertsh"
                       fragmentShaderFileName:@"PassThrough.fragsh"];
    vbVertexPositionHandle              =
    glGetAttribLocation(vbShaderProgramID, "vertexPosition");
    vbVertexTexCoordHandle              =
    glGetAttribLocation(vbShaderProgramID, "vertexTexCoord");
    vbProjectionMatrixHandle            =
    glGetUniformLocation(vbShaderProgramID, "modelViewProjectionMatrix");
    vbTexSamplerVideoHandle             =
    glGetUniformLocation(vbShaderProgramID, "texSamplerVideo");
    SampleApplicationUtils::setOrthoMatrix(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0,
                                vbOrthoProjMatrix);
    
    vbShaderProgramOcclusionID          = [SampleApplicationShaderUtils createProgramWithVertexShaderFileName:@"PassThrough.vertsh"
                       fragmentShaderFileName:@"Occlusion.fragsh"];
    vbVertexPositionOcclusionHandle     =
    glGetAttribLocation(vbShaderProgramOcclusionID, "vertexPosition");
    vbVertexTexCoordOcclusionHandle     =
    glGetAttribLocation(vbShaderProgramOcclusionID, "vertexTexCoord");
    vbProjectionMatrixOcclusionHandle   =
    glGetUniformLocation(vbShaderProgramOcclusionID,
                         "modelViewProjectionMatrix");
    vbViewportOriginHandle              =
    glGetUniformLocation(vbShaderProgramOcclusionID, "viewportOrigin");
    vbViewportSizeHandle                =
    glGetUniformLocation(vbShaderProgramOcclusionID, "viewportSize");
    vbTextureRatioHandle                =
    glGetUniformLocation(vbShaderProgramOcclusionID, "textureRatio");
    vbTexSamplerVideoOcclusionHandle       =
    glGetUniformLocation(vbShaderProgramOcclusionID, "texSamplerVideo");
    vbTexSamplerMaskOcclusionHandle     =
    glGetUniformLocation(vbShaderProgramOcclusionID, "texSamplerMask");
    
    
    vbShaderProgramOcclusionReflectID       =  [SampleApplicationShaderUtils createProgramWithVertexShaderFileName:@"PassThrough.vertsh"
                                                                              withVertexShaderDefs:nil                    // No preprocessor defs for vertex shader
                                                                            fragmentShaderFileName:@"Occlusion.fragsh"
                                                                            withFragmentShaderDefs:@"#define REFLECT\n"]; // Define reflection for the fragment shader

    vbVertexPositionOcclusionReflectHandle  =
    glGetAttribLocation(vbShaderProgramOcclusionReflectID, "vertexPosition");
    vbVertexTexCoordOcclusionReflectHandle  =
    glGetAttribLocation(vbShaderProgramOcclusionReflectID, "vertexTexCoord");
    vbTexSamplerVideoOcclusionReflectHandle =
    glGetUniformLocation(vbShaderProgramOcclusionReflectID, "texSamplerVideo");
    vbProjectionMatrixOcclusionReflectHandle=
    glGetUniformLocation(vbShaderProgramOcclusionReflectID,
                         "modelViewProjectionMatrix");
    vbTexSamplerMaskOcclusionReflectHandle  =
    glGetUniformLocation(vbShaderProgramOcclusionReflectID, "texSamplerMask");
    vbViewportOriginReflectHandle           =
    glGetUniformLocation(vbShaderProgramOcclusionReflectID, "viewportOrigin");
    vbViewportSizeReflectHandle             =
    glGetUniformLocation(vbShaderProgramOcclusionReflectID, "viewportSize");
    vbTextureRatioReflectHandle             =
    glGetUniformLocation(vbShaderProgramOcclusionReflectID, "textureRatio");
}


- (void)createFramebuffer
{
    if (context) {
        // Create default framebuffer object
        glGenFramebuffers(1, &defaultFramebuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
        
        // Create colour renderbuffer and allocate backing store
        glGenRenderbuffers(1, &colorRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
        
        // Allocate the renderbuffer's storage (shared with the drawable object)
        [context renderbufferStorage:GL_RENDERBUFFER fromDrawable:(CAEAGLLayer*)self.layer];
        GLint framebufferWidth;
        GLint framebufferHeight;
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH, &framebufferWidth);
        glGetRenderbufferParameteriv(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT, &framebufferHeight);
        
        // Create the depth render buffer and allocate storage
        glGenRenderbuffers(1, &depthRenderbuffer);
        glBindRenderbuffer(GL_RENDERBUFFER, depthRenderbuffer);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, framebufferWidth, framebufferHeight);
        
        // Attach colour and depth render buffers to the frame buffer
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, colorRenderbuffer);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRenderbuffer);
        
        // Leave the colour render buffer bound so future rendering operations will act on it
        glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
    }
}


- (void)deleteFramebuffer
{
    if (context) {
        [EAGLContext setCurrentContext:context];
        
        if (defaultFramebuffer) {
            glDeleteFramebuffers(1, &defaultFramebuffer);
            defaultFramebuffer = 0;
        }
        
        if (colorRenderbuffer) {
            glDeleteRenderbuffers(1, &colorRenderbuffer);
            colorRenderbuffer = 0;
        }
        
        if (depthRenderbuffer) {
            glDeleteRenderbuffers(1, &depthRenderbuffer);
            depthRenderbuffer = 0;
        }
    }
}


- (void)setFramebuffer
{
    // The EAGLContext must be set for each thread that wishes to use it.  Set
    // it the first time this method is called (on the render thread)
    if (context != [EAGLContext currentContext]) {
        [EAGLContext setCurrentContext:context];
    }
    
    if (!defaultFramebuffer) {
        // Perform on the main thread to ensure safe memory allocation for the
        // shared buffer.  Block until the operation is complete to prevent
        // simultaneous access to the OpenGL context
        [self performSelectorOnMainThread:@selector(createFramebuffer) withObject:self waitUntilDone:YES];
    }
    
    glBindFramebuffer(GL_FRAMEBUFFER, defaultFramebuffer);
}


- (BOOL)presentFramebuffer
{
    // setFramebuffer must have been called before presentFramebuffer, therefore
    // we know the context is valid and has been set for this (render) thread
    
    // Bind the colour render buffer and present it
    glBindRenderbuffer(GL_RENDERBUFFER, colorRenderbuffer);
    
    return [context presentRenderbuffer:GL_RENDERBUFFER];
}



@end

