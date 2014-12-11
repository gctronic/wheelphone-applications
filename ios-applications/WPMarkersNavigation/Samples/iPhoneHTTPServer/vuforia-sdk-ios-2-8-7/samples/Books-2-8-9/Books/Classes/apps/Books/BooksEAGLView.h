/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import <UIKit/UIKit.h>

#import <QCAR/UIGLViewProtocol.h>

#import "Texture.h"
#import "SampleApplicationSession.h"
#import "BooksControllerDelegateProtocol.h"
#import "Transition3Dto2D.h"
#import "BooksManagerDelegateProtocol.h"
#import "ImagesManagerDelegateProtocol.h"

// structure to point to an object to be drawn
@interface Object3D : NSObject {
    unsigned int numVertices;
    const float *vertices;
    const float *normals;
    const float *texCoords;
    
    unsigned int numIndices;
    const unsigned short *indices;
    
    Texture *texture;
}
@property (nonatomic) unsigned int numVertices;
@property (nonatomic) const float *vertices;
@property (nonatomic) const float *normals;
@property (nonatomic) const float *texCoords;

@property (nonatomic) unsigned int numIndices;
@property (nonatomic) const unsigned short *indices;

@property (nonatomic, assign) Texture *texture;

@end



#define NUM_AUGMENTATION_TEXTURES 1


// Books is a subclass of UIView and conforms to the informal protocol
// UIGLViewProtocol
@interface BooksEAGLView : UIView <UIGLViewProtocol, BooksManagerDelegateProtocol, ImagesManagerDelegateProtocol> {
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
    
    // Whether the application is in scanning mode (or in content mode):
    bool scanningMode;
    
    id<BooksControllerDelegateProtocol> booksDelegate;
    
    
    // ----------------------------------------------------------------------------
    // Trackable Data Global Variables
    // ----------------------------------------------------------------------------
    const QCAR::TrackableResult* trackableResult;
    QCAR::Vec2F trackableSize;
    QCAR::Matrix34F pose;
    QCAR::Matrix44F modelViewMatrix;

    
    NSMutableArray *objects3D;  // objects to draw
    
    BOOL trackingTIDSet;
    GLuint tID;
    BOOL trackingTextureAvailable;
    BOOL isViewingTarget;
    BOOL isShowing2DOverlay;
    
    // ----------------------------------------------------------------------------
    // 3D to 2D Transition control variables
    // ----------------------------------------------------------------------------
    Transition3Dto2D* transition3Dto2D;
    Transition3Dto2D* transition2Dto3D;
    
    bool startTransition;
    bool startTransition2Dto3D;
    
    bool reportedFinished;
    bool reportedFinished2Dto3D;
    
    int renderState;
    float transitionDuration;
    
    // Lock to prevent concurrent access of the framebuffer on the main and
    // render threads (layoutSubViews and renderFrameQCAR methods)
    NSLock *framebufferLock;
    
    // Lock to synchronise data that is (potentially) accessed concurrently
    NSLock* dataLock;

    SampleApplicationSession * vapp;
    BOOL mDoLayoutSubviews;
    
}

- (id)initWithFrame:(CGRect)frame  delegate:(id<BooksControllerDelegateProtocol>) delegate appSession:(SampleApplicationSession *) app;

- (void)setOverlayLayer:(CALayer *)overlayLayer;
- (void)enterScanningMode;

- (BOOL)isPointInsideAROverlay:(CGPoint)aPoint;

- (void)finishOpenGLESCommands;
- (void)freeOpenGLESResources;

- (bool) isTouchOnTarget:(CGPoint) touchPoint;

- (void) setOrientationTransform:(CGAffineTransform)transform withLayerPosition:(CGPoint)pos;

@end

