/*
 
    File: AQRecorder.h
Abstract: Helper class for recording audio files via the AudioQueue
 Version: 2.5

Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
Inc. ("Apple") in consideration of your agreement to the following
terms, and your use, installation, modification or redistribution of
this Apple software constitutes acceptance of these terms.  If you do
not agree with these terms, please do not use, install, modify or
redistribute this Apple software.

In consideration of your agreement to abide by the following terms, and
subject to these terms, Apple grants you a personal, non-exclusive
license, under Apple's copyrights in this original Apple software (the
"Apple Software"), to use, reproduce, modify and redistribute the Apple
Software, with or without modifications, in source and/or binary forms;
provided that if you redistribute the Apple Software in its entirety and
without modifications, you must retain this notice and the following
text and disclaimers in all such redistributions of the Apple Software.
Neither the name, trademarks, service marks or logos of Apple Inc. may
be used to endorse or promote products derived from the Apple Software
without specific prior written permission from Apple.  Except as
expressly stated in this notice, no other rights or licenses, express or
implied, are granted by Apple herein, including but not limited to any
patent rights that may be infringed by your derivative works or by other
works in which the Apple Software may be incorporated.

The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.

IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

Copyright (C) 2012 Apple Inc. All Rights Reserved.

 
*/

#include <AudioToolbox/AudioToolbox.h>
#include <Foundation/Foundation.h>
#include <libkern/OSAtomic.h>

#include "CAStreamBasicDescription.h"
#include "CAXException.h"

#define kNumberRecordBuffers	3       // buffers used for audio sampling
#define kBufferDurationSeconds 0.125    // one entire packet (22 bytes) takes 125 ms

#define MIN_PEAK_DISTANCE 13    // distance between peaks measured in samples
#define AUDIO_PACKET_SIZE 18    // complete packet size (containing all sensors data)
#define SYNC_SAMPLES 150        // number of samples used to sync
#define SYNC_THRESHOLD 700      // if the signal is higher than this value then the sync is lost
                                // (for syncing we need the signal to be lower than this value for SYNC_SAMPLES samples)
#define DEBUG_ALGORITHM 0

#define LINE_SEARCH 0
#define LINE_FOLLOW 1
#define INIT_GROUND_THR 180
#define INIT_SPEED 10
#define INIT_LOST_THR 15
#define MAX_SPEED 350

class AQRecorder {
    
	public:
		AQRecorder();
		~AQRecorder();
		
		UInt32 GetNumberChannels() const { return mRecordFormat.NumberChannels(); }
		AudioQueueRef Queue() const	{ return mQueue; }
		CAStreamBasicDescription DataFormat() const	{ return mRecordFormat; }		
		void StartRecord();
		void StopRecord();		
		Boolean IsRunning() const { return mIsRunning; }
        void setFollowingEnabled(BOOL state);
        BOOL isFollowingEnabled();
        BOOL isCliffDetectionEnabled() {return isCliffDetecting;}
        void setCliffDetectionEnabled(BOOL state) {isCliffDetecting=state;}
        int getLeftSpeed() {return lSpeed;}
        int getRightSpeed() {return rSpeed;}
        void setLineFollowThr(int thr) { groundThreshold=thr;}
        void setLineLostThr(int thr) { lineLostThr=thr;}
        void setLineSpeed(int speed) { desiredSpeed=speed;}
    
        int getProx(int index) { return robProxValues[index];}
        int getProxAmb(int index) { return robProxAmbValues[index];}
        int getGround(int index) { return robGroundValues[index];}
        int getBattery() { return robBattery;}
        int getFlagsRobotToPhone() { return robFlagsRobotToPhone;}
        int getMeasLeftSpeed() { return robLeftSpeed;}
        int getMeasRightSpeed() { return robRightSpeed;}
        int getBehaviorStatus() { return (int)globalState;}
    
		UInt64 startTime;
        SInt64 iChange;
        SInt64 iStart;
        UInt8 bitValue;
        Boolean startDetected;
        UInt8 currentByte;
        UInt8 expectedByte;
        UInt64 numBytesReceived;
        UInt64 numBytesWrong;
        UInt64 numBytesWrongNotDetected;
        UInt8 audioData[AUDIO_PACKET_SIZE];
        UInt8 audioDataIndex;
        UInt8 waitZero;
        Boolean lookForPacketSync;
        UInt16 syncCounter;
        Boolean isInitiating;
        short tempValues[4];
        NSDate *start;
        NSDate *end;
        UInt8 numPacketsReceived;
        int maxSigValue;
        int minSigValue;
        int peakThreshold;
    
        BOOL isFollowing;
        BOOL isCliffDetecting;
        char globalState;
        int groundThreshold;
        int lineLostThr;
        int robGroundValues[4];
        int robProxValues[4];
        int robProxAmbValues[4];
        int robBattery;
        int robFlagsRobotToPhone;
        int robLeftSpeed;
        int robRightSpeed;
        int lineFound;
        int outOfLine;
        int lineFollowSpeed;
        int minSpeedLineFollow;
        int tempSpeed;
        int desiredSpeed;    
        int lSpeed;
        int rSpeed;
        int lSpeedPrev;
        int rSpeedPrev;
    
	private:
		AudioQueueRef mQueue;
		AudioQueueBufferRef mBuffers[kNumberRecordBuffers];
		SInt64 mRecordPacket; // current packet number in record file
		CAStreamBasicDescription mRecordFormat;
		Boolean	mIsRunning;
		void SetupAudioFormat(UInt32 inFormatID);
		int ComputeRecordBufferSize(const AudioStreamBasicDescription *format, float seconds);
        static void MyInputBufferHandler(	void *								inUserData,
											AudioQueueRef						inAQ,
											AudioQueueBufferRef					inBuffer,
											const AudioTimeStamp *				inStartTime,
											UInt32								inNumPackets,
											const AudioStreamPacketDescription*	inPacketDesc);

};

