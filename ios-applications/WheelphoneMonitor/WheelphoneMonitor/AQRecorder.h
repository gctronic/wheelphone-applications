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
#import "FirstViewController.h"

#define kNumberRecordBuffers	3
#define kBufferDurationSeconds 0.125    // one entire packet (22 bytes) takes 125 ms

//#define PEAK_MAX 0
//#define PEAK_MIN 1
//#define PACKET_SYNC 2
//#define DETECT_START 0
//#define DETECT_PEAKS 1
//#define SAMPLES_PER_BIT 22
//#define BIT_PER_BYTE 10
//#define START_EVENT 0
//#define MAX_PEAK_EVENT 1
//#define MIN_PEAK_EVENT 2
//#define READ_CONTENT 0
//#define READ_STOP_BIT 1


#define PEAK_THRESHOLD 10000 //15000
#define MIN_PEAK_DISTANCE 13
#define AUDIO_PACKET_SIZE 22
#define SYNC_THRESHOLD 700//600//500//450//350//400
#define SYNC_SAMPLES 150//100//120//150//SAMPLES_PER_BIT*BIT_PER_BYTE   // number of samples used to sync

class AQRecorder
	{
	public:
		AQRecorder();
		~AQRecorder();
		
		UInt32						GetNumberChannels() const	{ return mRecordFormat.NumberChannels(); }
		CFStringRef					GetFileName() const			{ return mFileName; }
		AudioQueueRef				Queue() const				{ return mQueue; }
		CAStreamBasicDescription	DataFormat() const			{ return mRecordFormat; }
		
		void			StartRecord(CFStringRef inRecordFile);
		void			StopRecord();		
		Boolean			IsRunning() const			{ return mIsRunning; }
		
		UInt64			startTime;
        
//        UInt8    lookFor;
//        short    localPeakVal;
//        UInt16   localPeakPos;
//        short    prevDerivativeSign;
//        short    derivativeSign;
//        UInt8    signalState;
//        UInt8    tempPeaks[SAMPLES_PER_BIT*BIT_PER_BYTE];
//        UInt16   tempPeaksIndex;
//        UInt16   startAdjust;
//        UInt16   startToStart;
//        UInt16   nextBitCounter;
//        SInt8    bitIndex;
//        UInt8    currentBit;
//        UInt8    interpretationState;
//        UInt8    peakFound;
//        UInt8    tempData;
//        UInt8   expectedData;
        
        SInt64  iChange;
        SInt64  iStart;
        UInt8   bitValue;
        Boolean startDetected;
        UInt8   currentByte;
        UInt8   expectedByte;
        UInt64  numBytesReceived;
        UInt64  numBytesWrong;
        UInt64  numBytesWrongNotDetected;
        UInt8    audioData[AUDIO_PACKET_SIZE];
        UInt8    audioDataIndex;
        UInt8    waitZero;
        Boolean  lookForPacketSync;
        UInt16   syncCounter;
        Boolean isInitiating;
        short   tempValues[4];
        NSDate *start;
        NSDate *end;
        UInt8   numPacketsReceived;
        FirstViewController *firstView;
        int maxSigValue;
        int minSigValue;
        int peakThreshold;
        
	private:
		CFStringRef					mFileName;
		AudioQueueRef				mQueue;
		AudioQueueBufferRef			mBuffers[kNumberRecordBuffers];
		AudioFileID					mRecordFile;
		SInt64						mRecordPacket; // current packet number in record file
		CAStreamBasicDescription	mRecordFormat;
		Boolean						mIsRunning;
        
		void			CopyEncoderCookieToFile();
		void			SetupAudioFormat(UInt32 inFormatID);
		int				ComputeRecordBufferSize(const AudioStreamBasicDescription *format, float seconds);

        static void MyInputBufferHandler(	void *								inUserData,
											AudioQueueRef						inAQ,
											AudioQueueBufferRef					inBuffer,
											const AudioTimeStamp *				inStartTime,
											UInt32								inNumPackets,
											const AudioStreamPacketDescription*	inPacketDesc);
	};