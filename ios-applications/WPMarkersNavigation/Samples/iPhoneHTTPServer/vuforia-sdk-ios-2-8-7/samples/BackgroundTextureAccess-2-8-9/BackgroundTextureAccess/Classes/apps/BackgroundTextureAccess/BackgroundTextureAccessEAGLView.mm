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
#import <QCAR/TrackableResult.h>
#import <QCAR/VideoBackgroundConfig.h>
#import <QCAR/VideoBackgroundTextureInfo.h>

#import "BackgroundTextureAccessEAGLView.h"
#import "Texture.h"
#import "SampleApplicationUtils.h"
#import "SampleApplicationShaderUtils.h"
#import "Teapot.h"


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
    // Teapot texture filenames
    const char* textureFilenames[NUM_AUGMENTATION_TEXTURES] = {
        "TextureTeapotRed.png",
    };
    
    // Model scale factor
    const float kObjectScale = 3.0f;
    
    // These values indicate how many rows and columns we want for our video background texture polygon
    const int vbNumVertexCols = 10;
    const int vbNumVertexRows = 10;
    
    // These are the variables for the vertices, coords and inidices
    const int vbNumVertexValues=vbNumVertexCols*vbNumVertexRows*3;      // Each vertex has three values: X, Y, Z
    const int vbNumTexCoord=vbNumVertexCols*vbNumVertexRows*2;          // Each texture coordinate has 2 values: U and V
    const int vbNumIndices=(vbNumVertexCols-1)*(vbNumVertexRows-1)*6;   // Each square is composed of 2 triangles which in turn
    // have 3 vertices each, so we need 6 indices
    
    // These are the data containers for the vertices, texcoords and indices in the CPU
    float   vbOrthoQuadVertices     [vbNumVertexValues];
    float   vbOrthoQuadTexCoords    [vbNumTexCoord];
    GLbyte  vbOrthoQuadIndices      [vbNumIndices];
    
    // This will hold the data for the projection matrix passed to the vertex shader
    float   vbOrthoProjMatrix[16];
}


@interface BackgroundTextureAccessEAGLView (PrivateMethods)

- (void)initShaders;
- (void)createFramebuffer;
- (void)deleteFramebuffer;
- (void)setFramebuffer;
- (BOOL)presentFramebuffer;

@end


@implementation BackgroundTextureAccessEAGLView

@synthesize touchLocation_X, touchLocation_Y;

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
        
        touchLocation_X = -100.0;
        touchLocation_Y = -100.0;
        
        self.userInteractionEnabled = YES;
        

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
    
    // Clear color and depth buffer
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    
    // Get the state from QCAR and mark the beginning of a rendering section
    QCAR::State state = QCAR::Renderer::getInstance().begin();
    
    ////////////////////////////////////////////////////////////////////////////
    // This section renders the video background with a
    // custom shader defined in Shaders.h
    if (!QCAR::Renderer::getInstance().bindVideoBackground(0))
    {
        // No video frame available, take no further action
        QCAR::Renderer::getInstance().end();
        return;
    }
    
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    
    // Load the shader and upload the vertex/texcoord/index data
    glViewport(vapp.viewport.posX, vapp.viewport.posY, vapp.viewport.sizeX, vapp.viewport.sizeY);
    
    // We need a finer mesh for this background
    // We have to create it here because it will request the texture info of the video background
    if (!videoBackgroundShader.vbMeshInitialized)
    {
        [self CreateVideoBackgroundMesh];
    }
    
    glUseProgram(videoBackgroundShader.vbShaderProgramID);
    glVertexAttribPointer(videoBackgroundShader.vbVertexPositionHandle, 3, GL_FLOAT, GL_FALSE, 0, vbOrthoQuadVertices);
    glVertexAttribPointer(videoBackgroundShader.vbVertexTexCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, vbOrthoQuadTexCoords);
    glUniform1i(videoBackgroundShader.vbTexSampler2DHandle, 0);
    glUniformMatrix4fv(videoBackgroundShader.vbProjectionMatrixHandle, 1, GL_FALSE, &vbOrthoProjMatrix[0]);
    glUniform1f(videoBackgroundShader.vbTouchLocationXHandle, ([self touchLocation_X]*2.0)-1.0);
    glUniform1f(videoBackgroundShader.vbTouchLocationYHandle, (2.0-([self touchLocation_Y]*2.0))-1.0);
    
    // Render the video background with the custom shader
    glEnableVertexAttribArray(videoBackgroundShader.vbVertexPositionHandle);
    glEnableVertexAttribArray(videoBackgroundShader.vbVertexTexCoordHandle);
    // TODO: it might be more efficient to use Vertex Buffer Objects here
    glDrawElements(GL_TRIANGLES, vbNumIndices, GL_UNSIGNED_BYTE, vbOrthoQuadIndices);
    glDisableVertexAttribArray(videoBackgroundShader.vbVertexPositionHandle);
    glDisableVertexAttribArray(videoBackgroundShader.vbVertexTexCoordHandle);
    
    // Wrap up this rendering
    glUseProgram(0);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, 0);
    
    SampleApplicationUtils::checkGlError("Rendering of the background failed");
    
    ////////////////////////////////////////////////////////////////////////////
    // The following section is similar to image targets
    // we still render the teapot on top of the targets
    glEnable(GL_DEPTH_TEST);
    // We must detect if background reflection is active and adjust the culling direction.
    // If the reflection is active, this means the pose matrix has been reflected as well,
    // therefore standard counter clockwise face culling will result in "inside out" models.
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    if(QCAR::Renderer::getInstance().getVideoBackgroundConfig().mReflection == QCAR::VIDEO_BACKGROUND_REFLECTION_ON)
        glFrontFace(GL_CW);  //Front camera
    else
        glFrontFace(GL_CCW);   //Back camera
    
    // Did we find any trackables this frame?
    for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++)
    {
        // Get the trackable:
        const QCAR::TrackableResult* trackableResult = state.getTrackableResult(tIdx);
        QCAR::Matrix44F modelViewMatrix =
        QCAR::Tool::convertPose2GLMatrix(trackableResult->getPose());
        
        QCAR::Matrix44F modelViewProjection;
        
        SampleApplicationUtils::translatePoseMatrix(0.0f, 0.0f, kObjectScale,
                                         &modelViewMatrix.data[0]);
        SampleApplicationUtils::scalePoseMatrix(kObjectScale, kObjectScale, kObjectScale,
                                     &modelViewMatrix.data[0]);
        SampleApplicationUtils::multiplyMatrix(&vapp.projectionMatrix.data[0],
                                    &modelViewMatrix.data[0] ,
                                    &modelViewProjection.data[0]);
        
        glUseProgram(shaderProgramID);
        
        glVertexAttribPointer(vertexHandle, 3, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &teapotVertices[0]);
        glVertexAttribPointer(normalHandle, 3, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &teapotNormals[0]);
        glVertexAttribPointer(textureCoordHandle, 2, GL_FLOAT, GL_FALSE, 0,
                              (const GLvoid*) &teapotTexCoords[0]);
        
        glEnableVertexAttribArray(vertexHandle);
        glEnableVertexAttribArray(normalHandle);
        glEnableVertexAttribArray(textureCoordHandle);
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, augmentationTexture[0].textureID);
        glUniformMatrix4fv(mvpMatrixHandle, 1, GL_FALSE,
                           (GLfloat*)&modelViewProjection.data[0] );
        glUniform1i(texSampler2DHandle, 0 /*GL_TEXTURE0*/);
        glDrawElements(GL_TRIANGLES, NUM_TEAPOT_OBJECT_INDEX, GL_UNSIGNED_SHORT,
                       (const GLvoid*) &teapotIndices[0]);
        
        glDisableVertexAttribArray(vertexHandle);
        glDisableVertexAttribArray(normalHandle);
        glDisableVertexAttribArray(textureCoordHandle);
        
        SampleApplicationUtils::checkGlError("BackgroundTextureAccess renderFrame");
        
    }
    
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);
    
    ////////////////////////////////////////////////////////////////////////////
    // It is always important to tell the QCAR Renderer that we are finished
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
    
    // Define clear color
    glClearColor(0.0f, 0.0f, 0.0f, QCAR::requiresAlpha() ? 0.0f : 1.0f);
    
    // Initialise video background shader data
    videoBackgroundShader.vbShaderProgramID = [SampleApplicationShaderUtils createProgramWithVertexShaderFileName:@"BGShader.vertsh"
                                                                           fragmentShaderFileName:@"BGShader.fragsh"];
    
    if (0 < videoBackgroundShader.vbShaderProgramID) {
        // Retrieve handler for vertex position shader attribute variable
        videoBackgroundShader.vbVertexPositionHandle = glGetAttribLocation(videoBackgroundShader.vbShaderProgramID, "vertexPosition");
        
        // Retrieve handler for texture coordinate shader attribute variable
        videoBackgroundShader.vbVertexTexCoordHandle = glGetAttribLocation(videoBackgroundShader.vbShaderProgramID, "vertexTexCoord");
        
        // Retrieve handler for texture sampler shader uniform variable
        videoBackgroundShader.vbTexSampler2DHandle = glGetUniformLocation(videoBackgroundShader.vbShaderProgramID, "texSampler2D");
        
        // Retrieve handler for projection matrix shader uniform variable
        videoBackgroundShader.vbProjectionMatrixHandle = glGetUniformLocation(videoBackgroundShader.vbShaderProgramID, "projectionMatrix");
        
        // Retrieve handler for projection matrix shader uniform variable
        videoBackgroundShader.vbTouchLocationXHandle = glGetUniformLocation(videoBackgroundShader.vbShaderProgramID, "touchLocation_x");
        
        // Retrieve handler for projection matrix shader uniform variable
        videoBackgroundShader.vbTouchLocationYHandle = glGetUniformLocation(videoBackgroundShader.vbShaderProgramID, "touchLocation_y");
        
        SampleApplicationUtils::checkGlError("Getting the handles to the shader variables");
        
        // Set the orthographic matrix
        SampleApplicationUtils::setOrthoMatrix(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0, vbOrthoProjMatrix);
    }
    else {
        NSLog(@"Could not initialise video background shader");
    }
    

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

// Mark the video background mesh as uninitialised, so it is reinitialised the
// next time it is used; this ensures correct behaviour when swapping between
// front and back cameras
- (void)cameraDidStart
{
    videoBackgroundShader.vbMeshInitialized = false;
}

// This function adds the values to the vertex, coord and indices variables.
// Essentially it defines a mesh from -1 to 1 in X and Y with
// vbNumVertexRows rows and vbNumVertexCols columns. Thus, if we were to assign
// vbNumVertexRows=10 and vbNumVertexCols=10 we would have a mesh composed of
// 100 little squares (notice, however, that we work with triangles so it is
// actually not composed of 100 squares but of 200 triangles). The example
// below shows 4 triangles composing 2 squares.
//      D---E---F
//      | \ | \ |
//      A---B---C
- (void)CreateVideoBackgroundMesh
{
    // Get the texture and image dimensions from QCAR
    const QCAR::VideoBackgroundTextureInfo texInfo=QCAR::Renderer::getInstance().getVideoBackgroundTextureInfo();
    
    // Detect if the renderer is reporting reflected pose info, possibly due to usage of the front camera.
    // If so, we need to reflect the image of the video background to match the pose.
    const QCAR::VIDEO_BACKGROUND_REFLECTION reflection = QCAR::Renderer::getInstance().getVideoBackgroundConfig().mReflection;
    
    // If there is no image data yet then return;
    if ((texInfo.mImageSize.data[0]==0)||(texInfo.mImageSize.data[1]==0)) return;
    
    // These calculate a slope for the texture coords
    float uRatio=((float)texInfo.mImageSize.data[0]/(float)texInfo.mTextureSize.data[0]);
    float vRatio=((float)texInfo.mImageSize.data[1]/(float)texInfo.mTextureSize.data[1]);
    float uSlope=uRatio/(vbNumVertexCols-1);
    float vSlope=vRatio/(vbNumVertexRows-1);
    
    // These calculate a slope for the vertex values in this case we have a span of 2, from -1 to 1
    float totalSpan=2.0f;
    float colSlope=totalSpan/(vbNumVertexCols-1);
    float rowSlope=totalSpan/(vbNumVertexRows-1);
    
    // Some helper variables
    int currentIndexPosition=0;
    int currentVertexPosition=0;
    int currentCoordPosition=0;
    int currentVertexIndex=0;
    
    for (int j=0; j<vbNumVertexRows; j++)
    {
        for (int i=0; i<vbNumVertexCols; i++)
        {
            // We populate the mesh with a regular grid
            vbOrthoQuadVertices[currentVertexPosition   /*X*/] = ((colSlope*i)-(totalSpan/2.0f));   // We subtract this because the values range from -totalSpan/2 to totalSpan/2
            vbOrthoQuadVertices[currentVertexPosition+1 /*Y*/] = ((rowSlope*j)-(totalSpan/2.0f));
            vbOrthoQuadVertices[currentVertexPosition+2 /*Z*/] = 0.0f;                              // It is all a flat polygon orthogonal to the view vector
            
            // We also populate its associated texture coordinate
            vbOrthoQuadTexCoords[currentCoordPosition   /*U*/] = uSlope*i;
            vbOrthoQuadTexCoords[currentCoordPosition+1 /*V*/] = (reflection == QCAR::VIDEO_BACKGROUND_REFLECTION_ON) ? (1 - (vRatio - (vSlope*j))): vRatio - (vSlope*j);
            
            // Now we populate the triangles that compose the mesh
            // First triangle is the upper right of the vertex
            if (j<vbNumVertexRows-1)
            {
                if (i<vbNumVertexCols-1) // In the example above this would make triangles ABD and BCE
                {
                    vbOrthoQuadIndices[currentIndexPosition  ]=currentVertexIndex;
                    vbOrthoQuadIndices[currentIndexPosition+1]=currentVertexIndex+1;
                    vbOrthoQuadIndices[currentIndexPosition+2]=currentVertexIndex+vbNumVertexCols;
                    currentIndexPosition+=3;
                }
                if (i>0) // In the example above this would make triangles BED and CFE
                {
                    vbOrthoQuadIndices[currentIndexPosition  ]=currentVertexIndex;
                    vbOrthoQuadIndices[currentIndexPosition+1]=currentVertexIndex+vbNumVertexCols;
                    vbOrthoQuadIndices[currentIndexPosition+2]=currentVertexIndex+vbNumVertexCols-1;
                    currentIndexPosition+=3;
                }
            }
            currentVertexPosition+=3;
            currentCoordPosition+=2;
            currentVertexIndex+=1;
        }
    }
    
    videoBackgroundShader.vbMeshInitialized=true;
}

static float saved_x = 0.0, saved_y = 0.0;

- (void)touchAtSavedCoords
{
    [self handleUserTouchEventAtXCoord:saved_x YCoord:saved_y];
}

// The user touched the screen
- (void)touchesBegan:(NSSet*)touches withEvent:(UIEvent*)event
{
    UITouch* touch = [touches anyObject];
    CGPoint point = [touch locationInView:self];
    CGRect rect = [self bounds];
    saved_x = point.x / rect.size.width;
    saved_y = point.y / rect.size.height;
    [self performSelector:@selector(touchAtSavedCoords) withObject:nil afterDelay:.3];
}

- (void)touchesMoved:(NSSet*)touches withEvent:(UIEvent*)event
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(touchAtSavedCoords) object:nil];
    UITouch* touch = [touches anyObject];
    CGPoint point = [touch locationInView:self];
    CGRect rect = [self bounds];
    
    [self handleUserTouchEventAtXCoord:(point.x / rect.size.width) YCoord:(point.y / rect.size.height)];
}

- (void)touchesEnded:(NSSet*)touches withEvent:(UIEvent*)event
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(touchAtSavedCoords) object:nil];
    [self handleUserTouchEventAtXCoord:-100 YCoord:-100];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(touchAtSavedCoords) object:nil];
    // needs implementing even if it does nothing
    [self handleUserTouchEventAtXCoord:-100 YCoord:-100];
}

- (void)handleUserTouchEventAtXCoord:(float)x YCoord:(float)y
{
    // Use touch coordinates for the Loupe effect.  Note: the value -100.0 is
    // simply used as a flag for the shader to ignore the position
    
    // Thread-safe access to touch location data members
    [self setTouchLocation_X:x];
    [self setTouchLocation_Y:y];
}


@end

