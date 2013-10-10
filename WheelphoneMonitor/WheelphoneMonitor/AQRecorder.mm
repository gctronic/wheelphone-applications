/*

    File: AQRecorder.mm
Abstract: n/a
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

#include "AQRecorder.h"

// ____________________________________________________________________________________
// Determine the size, in bytes, of a buffer necessary to represent the supplied number
// of seconds of audio data.
int AQRecorder::ComputeRecordBufferSize(const AudioStreamBasicDescription *format, float seconds)
{
	int packets, frames, bytes = 0;
	try {
		frames = (int)ceil(seconds * format->mSampleRate);
		
		if (format->mBytesPerFrame > 0)
			bytes = frames * format->mBytesPerFrame;
		else {
			UInt32 maxPacketSize;
			if (format->mBytesPerPacket > 0)
				maxPacketSize = format->mBytesPerPacket;	// constant packet size
			else {
				UInt32 propertySize = sizeof(maxPacketSize);
				XThrowIfError(AudioQueueGetProperty(mQueue, kAudioQueueProperty_MaximumOutputPacketSize, &maxPacketSize,
												 &propertySize), "couldn't get queue's maximum output packet size");
			}
			if (format->mFramesPerPacket > 0)
				packets = frames / format->mFramesPerPacket;
			else
				packets = frames;	// worst-case scenario: 1 frame in a packet
			if (packets == 0)		// sanity check
				packets = 1;
			bytes = packets * maxPacketSize;
		}
	} catch (CAXException e) {
		char buf[256];
		fprintf(stderr, "Error: %s (%s)\n", e.mOperation, e.FormatError(buf));
		return 0;
	}	
	return bytes;
}

// ____________________________________________________________________________________
// AudioQueue callback function, called when an input buffers has been filled.
void AQRecorder::MyInputBufferHandler(	void *								inUserData,
										AudioQueueRef						inAQ,
										AudioQueueBufferRef					inBuffer,
										const AudioTimeStamp *				inStartTime,
										UInt32								inNumPackets,
										const AudioStreamPacketDescription*	inPacketDesc)
{
	AQRecorder *aqr = (AQRecorder *)inUserData;

	try {
        
        short *coreAudioBuffer;
        coreAudioBuffer = (short*) inBuffer->mAudioData;        
        unsigned int k=0, i=0;
        short currentValue=0, prevValue=0;
        signed int leftSpeedTemp=0;
        signed int rightSpeedTemp=0;
        
//      printf("sizeof short = %ld bytes\n", sizeof(short));
//      printf("valid data size = %ld bytes\n", inBuffer->mAudioDataByteSize);
//      printf("buffer size = %ld bytes\n", inBuffer->mAudioDataBytesCapacity);
//      //if(inBuffer->mPacketDescriptionCapacity != NULL) {
//          printf("desc capacity = %ld\n", inBuffer->mPacketDescriptionCapacity);
//      //}
        
        printf("max signal value = %d\n", aqr->maxSigValue);
        printf("min signal value = %d\n", aqr->minSigValue);
        printf("starting processing data...\n");
       
        // every audio sample is 2 bytes
        for(i=0; i<inBuffer->mAudioDataByteSize/2; i++) {
            
            if(aqr->isInitiating) {
                if(i==0) {
                    aqr->tempValues[0] = coreAudioBuffer[i];
                } else if (i==1) {
                    aqr->tempValues[1] = coreAudioBuffer[i];
                } else if (i==2) {
                    aqr->tempValues[2] = coreAudioBuffer[i];
                } else if (i==3) {
                    aqr->tempValues[3] = coreAudioBuffer[i];
                } else {
                    aqr->isInitiating = false;
                }
                continue;
            } else {
                aqr->tempValues[0] = aqr->tempValues[1];
                aqr->tempValues[1] = aqr->tempValues[2];
                aqr->tempValues[2] = aqr->tempValues[3];
                aqr->tempValues[3] = coreAudioBuffer[i];
            }
            
            if(aqr->maxSigValue < coreAudioBuffer[i]) {
                aqr->maxSigValue = coreAudioBuffer[i];
                aqr->peakThreshold = aqr->maxSigValue/100*35;   // about 35% of max value
            }
            if(aqr->minSigValue > coreAudioBuffer[i]) {
                aqr->minSigValue = coreAudioBuffer[i];
            }            
            
            prevValue = aqr->tempValues[0];
            currentValue = aqr->tempValues[3];
            
            if(aqr->lookForPacketSync) {
                
                if(abs(currentValue) < SYNC_THRESHOLD) {
                    aqr->syncCounter++;
                } else {
                    aqr->syncCounter = 0;
                }
                if(aqr->syncCounter > SYNC_SAMPLES) {
                    printf("packet sync found!\n");
                    aqr->end = [NSDate date];
                    NSTimeInterval executionTime = [aqr->end timeIntervalSinceDate:aqr->start];
                    printf("Packet sync to packet sync time: %f seconds (%f Hz)\n", executionTime, 1.0/executionTime);
                    aqr->start = [NSDate date];
                    
                    aqr->audioDataIndex = 0;
                    aqr->syncCounter = 0;
                    aqr->iChange = 0;
                    aqr->iStart = 0;
                    aqr->bitValue = 0;
                    aqr->startDetected = false;
                    aqr->currentByte = 0;
                    aqr->lookForPacketSync = false;

                }
            } else {
            
                if((i > aqr->iChange+MIN_PEAK_DISTANCE) && (aqr->bitValue == 0) && ((currentValue - prevValue) > aqr->peakThreshold)) { // found a max peak
                    aqr->bitValue = 1;
                    aqr->iChange = i;
                    if (!aqr->startDetected) {
                        aqr->startDetected = true;
                        aqr->iStart = i;
                    }
                }
                
                if((i > aqr->iChange+MIN_PEAK_DISTANCE) && (aqr->bitValue == 1) && ((currentValue - prevValue) < -aqr->peakThreshold)) {    // found a min peak
                    aqr->bitValue = 0;
                    aqr->iChange = i;
                }
                
                if(aqr->startDetected) {
                    if(i == aqr->iStart+33) {
                        aqr->currentByte = aqr->bitValue*128;
                    }
                    if(i == aqr->iStart+55) {
                        aqr->currentByte += aqr->bitValue*64;
                    }
                    if(i == aqr->iStart+77) {
                        aqr->currentByte += aqr->bitValue*32;
                    }
                    if(i == aqr->iStart+99) {
                        aqr->currentByte += aqr->bitValue*16;
                    }
                    if(i == aqr->iStart+121) {
                        aqr->currentByte += aqr->bitValue*8;
                    }
                    if(i == aqr->iStart+143) {
                        aqr->currentByte += aqr->bitValue*4;
                    }
                    if(i == aqr->iStart+165) {
                        aqr->currentByte += aqr->bitValue*2;
                    }
                    if(i == aqr->iStart+187) {
                        aqr->currentByte += aqr->bitValue*1;
                    }
                    if(i == aqr->iStart+209) {
                        aqr->startDetected = false;
                        
                        if(aqr->bitValue == 0) {
                            
                        } else {    // stop bit not well detected
                            printf("Error in interpretation!!!\n");
                        }
                        
                        aqr->audioData[aqr->audioDataIndex] = aqr->currentByte;
                        printf("%d) %d\n", aqr->audioDataIndex, aqr->currentByte);
                        
                        if(aqr->audioDataIndex == 0) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"prox0"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 1) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"prox1"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                            //[sensorsDict setObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"prox1"];
                            //[[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:sensorsDict];
                        } else if(aqr->audioDataIndex == 2) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"prox2"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 3) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"prox3"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 4) {   // prox ambient 0
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"proxAmb0"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 5) {   // prox ambient 1
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"proxAmb1"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];                            
                        } else if(aqr->audioDataIndex == 6) {   // prox ambient 2
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"proxAmb2"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 7) {   // prox ambient 3
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"proxAmb3"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];                            
                        } else if(aqr->audioDataIndex == 8) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"ground0"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 9) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"ground1"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 10) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"ground2"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 11) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"ground3"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 12) {   // ground ambient 0
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"groundAmb0"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 13) {   // ground ambient 1
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"groundAmb1"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];                            
                        } else if(aqr->audioDataIndex == 14) {   // ground ambient 2
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"groundAmb2"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];                            
                        } else if(aqr->audioDataIndex == 15) {   // ground ambient 3
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"groundAmb3"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 16) {
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"battery"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 17) {  // flag status robot to phone
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:aqr->currentByte] forKey:@"flagsRobotToPhone"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                            
                        } else if(aqr->audioDataIndex == 18) {  // speed left LSB
                            leftSpeedTemp = aqr->currentByte;
                            
                        } else if(aqr->audioDataIndex == 19) {  // speed left MSB
                            leftSpeedTemp += aqr->currentByte*256;
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:leftSpeedTemp] forKey:@"leftSpeed"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        } else if(aqr->audioDataIndex == 20) {  // speed right LSB
                            rightSpeedTemp = aqr->currentByte;
                            
                        } else if(aqr->audioDataIndex == 21) {  // speed right MSB
                            rightSpeedTemp += aqr->currentByte*256;
                            NSDictionary *userInfo = [NSDictionary dictionaryWithObject:[NSNumber numberWithInt:rightSpeedTemp] forKey:@"rightSpeed"];
                            [[NSNotificationCenter defaultCenter] postNotificationName:@"sensorsNotification" object:nil userInfo:userInfo];
                        }
                        
                        
                        // testing purpose
                        /*
                        if(aqr->waitZero == 1) {    // wait data=0 to start from a known expected value
                            if(aqr->audioDataIndex == 0) {
                                if(aqr->currentByte == 0) {
                                    aqr->waitZero = 0;
                                    aqr->expectedByte = 1;
                                }
                            }
                        } else {
                            if(aqr->audioDataIndex == 0) {  // only first packet byte is used for testing
//                                printf("%d) %d\n", aqr->audioDataIndex, aqr->currentByte);
//                                printf("expected = %d\n", aqr->expectedByte);
                                if(aqr->currentByte != aqr->expectedByte) {
                                    if(aqr->bitValue == 1) {
                                        aqr->numBytesWrong++;
                                    } else {
                                        aqr->numBytesWrongNotDetected++;
                                    }
                                }
                                aqr->numBytesReceived++;
                                if(aqr->expectedByte < 255) {
                                    aqr->expectedByte++;
                                } else {
                                    aqr->expectedByte = 0;
                                }
                            }
                        }
                        */
                        
                        aqr->currentByte = 0;
                        
                        aqr->audioDataIndex++;
                        if(aqr->audioDataIndex == AUDIO_PACKET_SIZE) {
                            aqr->lookForPacketSync = true;
                            // statistics
                            /*
                            aqr->numPacketsReceived++;
//                            printf("bytes wrong = %lld\n", aqr->numBytesWrong);
//                            printf("bytes wrong not detected = %lld\n", aqr->numBytesWrongNotDetected);
//                            printf("total bytes received = %lld\n", aqr->numBytesReceived);
                            printf("corrected bytes received = %f\n", (1.0 - (float)(aqr->numBytesWrong+aqr->numBytesWrongNotDetected)/(float)aqr->numBytesReceived)*100.0);
                             */
                        }
                        
                        
                    }
                    
                }
                
            }
            
        } // end for
        
        
//        printf("data size = %ld\n", inBuffer->mAudioDataByteSize/2);
//        printf("iChange=%lld, iStart=%lld\n", aqr->iChange, aqr->iStart);        
        aqr->iChange -= inBuffer->mAudioDataByteSize/2;
        aqr->iStart -= inBuffer->mAudioDataByteSize/2;
//        printf("iChange=%lld, iStart=%lld\n", aqr->iChange, aqr->iStart);
        
        if(aqr->numPacketsReceived >= 100) {
            aqr->end = [NSDate date];
            NSTimeInterval executionTime = [aqr->end timeIntervalSinceDate:aqr->start];
            printf("Average packet time: %f seconds (%f Hz)\n", executionTime/aqr->numPacketsReceived, 1.0/(executionTime/aqr->numPacketsReceived));
            aqr->start = [NSDate date];
            aqr->numPacketsReceived = 0;
        }
        
        
//        for(i=0; i<inBuffer->mAudioDataByteSize/2; i++) {
//            //printf("%d ",coreAudioBuffer[i]);
//            //printf("\n\n\n");
//            
//            currentValue = coreAudioBuffer[i];
//            
//            if(aqr->lookFor == PACKET_SYNC) {
//                if(abs(currentValue) < SYNC_THRESHOLD) {
//                    aqr->syncCounter++;
//                } else {
//                    aqr->syncCounter = 0;
//                }
//                if(aqr->syncCounter > SYNC_SAMPLES) {
////                    printf("packet sync found!\n");
//                    aqr->lookFor = PEAK_MAX;
//                    aqr->audioDataIndex = 0;
//                    aqr->syncCounter = 0;
//                    aqr->localPeakVal = 0;
//                    aqr->localPeakPos = 0;
//                    aqr->prevDerivativeSign = 0;
//                    aqr->derivativeSign = 0;
//                    aqr->signalState = DETECT_START;
//                    memset(aqr->tempPeaks, 0, SAMPLES_PER_BIT*BIT_PER_BYTE);
//                    aqr->tempPeaksIndex = 0;
//                    aqr->startAdjust = 0;
//                    aqr->startToStart = 0;
//                    aqr->nextBitCounter = 0;
//                    aqr->bitIndex = 0;
//                    aqr->interpretationState = READ_CONTENT;
//                    aqr->peakFound = 0;
//                    aqr->audioDataIndex = 0;
//                    aqr->tempData = 0;
//                }
//            } else {
//            
//                if(aqr->lookFor == PEAK_MAX) {
//                    if(currentValue < PEAK_THRESHOLD && currentValue > 0) {
//                        currentValue = 0;
//                        coreAudioBuffer[i] = 0;
//                    }
//                    if(aqr->localPeakVal < currentValue) {
//                        aqr->localPeakVal = currentValue;
//                        aqr->localPeakPos = i;
//                    }
//                } else {
//                    if(currentValue > -PEAK_THRESHOLD && currentValue < 0) {
//                        currentValue = 0;
//                        coreAudioBuffer[i] = 0;
//                    }
//                    if(aqr->localPeakVal > currentValue) {
//                        aqr->localPeakVal = currentValue;
//                        aqr->localPeakPos = i;
//                    }
//                }
//
//                if(abs(currentValue - aqr->localPeakVal) >= PEAK_THRESHOLD) {
//                    //printf("found a peak\n");
//                    
//                    aqr->prevDerivativeSign = aqr->derivativeSign;
//                    aqr->derivativeSign = currentValue - aqr->localPeakVal;
//                    
//                    if(aqr->lookFor == PEAK_MAX && aqr->derivativeSign < 0) { // top peak found
////                        printf("found a peak max, looking for peak min...\n");
//                        aqr->lookFor = PEAK_MIN;
//                        if(aqr->signalState == DETECT_START) {
//                            aqr->tempPeaksIndex = 0;
//                            aqr->tempPeaks[aqr->tempPeaksIndex] = START_EVENT;
//                            aqr->startAdjust = i - aqr->localPeakPos;
//                            aqr->startToStart = (SAMPLES_PER_BIT*BIT_PER_BYTE)-(SAMPLES_PER_BIT/2)-aqr->startAdjust;
//                            aqr->signalState = DETECT_PEAKS;
////                            printf("i = %d\n", i);
////                            printf("localPeakPos = %d\n", aqr->localPeakPos);
////                            printf("startAdjust = %d\n", aqr->startAdjust);
////                            printf("startToStart = %d\n", aqr->startToStart);
//                        } else if (aqr->signalState == DETECT_PEAKS) {
////                            printf("tempPeaksIndex = %d\n", aqr->tempPeaksIndex);
////                            printf("i = %d\n", i);
////                            printf("localPeakPos = %d\n", aqr->localPeakPos);
////                            printf("startAdjust = %d\n", aqr->startAdjust);
//                            aqr->tempPeaks[aqr->tempPeaksIndex-(i-aqr->localPeakPos)+aqr->startAdjust] = MAX_PEAK_EVENT;
//                        }
//                    } else if(aqr->lookFor == PEAK_MIN && aqr->derivativeSign > 0) {
////                        printf("found a peak min, looking for peak max...\n");
//                        aqr->lookFor = PEAK_MAX;
//                        if(aqr->signalState == DETECT_PEAKS) {
////                            printf("tempPeaksIndex = %d\n", aqr->tempPeaksIndex);
////                            printf("i = %d\n", i);
////                            printf("localPeakPos = %d\n", aqr->localPeakPos);
////                            printf("startAdjust = %d\n", aqr->startAdjust);
//                            aqr->tempPeaks[aqr->tempPeaksIndex-(i-aqr->localPeakPos)+aqr->startAdjust] = MIN_PEAK_EVENT;
//                        }
//                    }
//                }
//                
//                aqr->tempPeaksIndex++;
//                aqr->startToStart--;
//                
//                if(aqr->startToStart == 0) {    // one byte received
////                    printf("one byte received, start interpretation!\n");
//                    
//                    aqr->nextBitCounter = SAMPLES_PER_BIT/2 + SAMPLES_PER_BIT;
//                    aqr->bitIndex = 7;
//                    aqr->currentBit = 1;
//                    aqr->interpretationState = READ_CONTENT;
//                    
//                    for(k=0; k<SAMPLES_PER_BIT*BIT_PER_BYTE; k++) {
////                        printf("k=%d\n",k);
//                        if(aqr->tempPeaks[k] > 0) {
//                            aqr->peakFound = 1;
//                        }
//                        aqr->nextBitCounter--;
//                        if(aqr->interpretationState == READ_CONTENT) {                            
//                            if(aqr->nextBitCounter == 0) {   // bit is read
////                                printf("bit %d is read...\n", aqr->bitIndex);
//                                if(aqr->peakFound == 1) {    // when a peak is found the bit is toggled
//                                    aqr->peakFound = 0;
//                                    aqr->currentBit = 1 - aqr->currentBit;
//                                }
////                                printf("current bit = %d\n", aqr->currentBit);
//                                aqr->tempData = aqr->tempData | (aqr->currentBit<<aqr->bitIndex);
//                                aqr->nextBitCounter = SAMPLES_PER_BIT;
//                                aqr->bitIndex--;
//                                if(aqr->bitIndex < 0) {
//                                    aqr->interpretationState = READ_STOP_BIT;
////                                    printf("waiting stop bit...\n");
//                                }
//                            }
//                            
//                        } else if(aqr->interpretationState == READ_STOP_BIT) {
//                            if(aqr->nextBitCounter == 0) {
//                                if(aqr->peakFound == 1) {
//                                    aqr->peakFound = 0;
//                                    aqr->currentBit = 1 - aqr->currentBit;
//                                }
//                                if(aqr->currentBit == 1) {
////                                    printf("Error in interpretation!!!\n");
//                                }
//                                aqr->signalState = DETECT_START;
//                                memset(aqr->tempPeaks, 0, SAMPLES_PER_BIT*BIT_PER_BYTE);
//                                aqr->audioData[aqr->audioDataIndex] = aqr->tempData;
////                                printf("%d) %d\n", aqr->audioDataIndex, aqr->tempData);
//                                
//                                if(aqr->waitZero == 1) {
//                                    if(aqr->audioDataIndex == 0) {
//                                        if(aqr->tempData == 0) {
//                                            aqr->waitZero = 0;
//                                            aqr->expectedData = 1;
//                                        }
//                                    }
//                                } else {
//                                    if(aqr->audioDataIndex == 0) {  // only first packet byte is used for testing
//                                        printf("%d) %d\n", aqr->audioDataIndex, aqr->tempData);
//                                        printf("expected = %d\n", aqr->expectedData);
//                                        if(aqr->tempData != aqr->expectedData) {
//                                            if(aqr->currentBit == 1) {
//                                                aqr->numBytesWrong++;
//                                            } else {
//                                                aqr->numBytesWrongNotDetected++;
//                                            }
//                                        }
//                                        aqr->numBytesReceived++;
//                                        if(aqr->expectedData < 255) {
//                                            aqr->expectedData++;
//                                        } else {
//                                            aqr->expectedData = 0;
//                                        }
//                                    }
//                                }
//                                aqr->audioDataIndex++;
//                                if(aqr->audioDataIndex == AUDIO_PACKET_SIZE) {
//                                    aqr->lookFor = PACKET_SYNC;
//                                    printf("bytes wrong = %lld\n", aqr->numBytesWrong);
//                                    printf("bytes wrong not detected = %lld\n", aqr->numBytesWrongNotDetected);
//                                    printf("total bytes received = %lld\n", aqr->numBytesReceived);
//                                    printf("corrected bytes received = %f\n", 100.0 - (float)(aqr->numBytesWrong+aqr->numBytesWrongNotDetected)/(float)aqr->numBytesReceived);
//                                }
//                                aqr->tempData = 0;
//                            }
//                        }
//                    }
//                    
//                }
//                
//            }
//
//        }
        
//        printf("processing end...\n");
		
		// if we're not stopping, re-enqueue the buffer so that it gets filled again
		if (aqr->IsRunning())
			XThrowIfError(AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, NULL), "AudioQueueEnqueueBuffer failed");
	} catch (CAXException e) {
		char buf[256];
		fprintf(stderr, "Error: %s (%s)\n", e.mOperation, e.FormatError(buf));
	}
}

AQRecorder::AQRecorder()
{
	mIsRunning = false;
	mRecordPacket = 0;
    
//    lookFor = PACKET_SYNC;
//    localPeakVal = 0;
//    localPeakPos = 0;
//    prevDerivativeSign = 0;
//    derivativeSign = 0;
//    signalState = DETECT_START;
//    memset(tempPeaks, 0, SAMPLES_PER_BIT*BIT_PER_BYTE);
//    tempPeaksIndex = 0;
//    startAdjust = 0;
//    startToStart = 0;
//    nextBitCounter = 0;
//    bitIndex = 0;
//    interpretationState = READ_CONTENT;
//    peakFound = 0;
//    tempData = 0;
//    expectedData = 0;
    
    iChange = 0;
    iStart = 0;
    bitValue = 0;
    startDetected = false;
    currentByte = 0;
    expectedByte = 0;
    numBytesReceived = 0;
    numBytesWrong = 0;
    numBytesWrongNotDetected = 0;
    audioDataIndex = 0;
    memset(audioData, 0, AUDIO_PACKET_SIZE);
    waitZero = 1;
    lookForPacketSync = true;
    syncCounter = 0;
    isInitiating = true;
    start = [NSDate date];
    end = [NSDate date];
    numPacketsReceived = 0;
    
    firstView = [FirstViewController new];
    
    maxSigValue=0;
    minSigValue=0;
    peakThreshold=0;
    
}

AQRecorder::~AQRecorder()
{
	AudioQueueDispose(mQueue, TRUE);
	AudioFileClose(mRecordFile);
	if (mFileName) CFRelease(mFileName);
}

// ____________________________________________________________________________________
// Copy a queue's encoder's magic cookie to an audio file.
void AQRecorder::CopyEncoderCookieToFile()
{
	UInt32 propertySize;
	// get the magic cookie, if any, from the converter		
	OSStatus err = AudioQueueGetPropertySize(mQueue, kAudioQueueProperty_MagicCookie, &propertySize);
	
	// we can get a noErr result and also a propertySize == 0
	// -- if the file format does support magic cookies, but this file doesn't have one.
	if (err == noErr && propertySize > 0) {
		Byte *magicCookie = new Byte[propertySize];
		UInt32 magicCookieSize;
		XThrowIfError(AudioQueueGetProperty(mQueue, kAudioQueueProperty_MagicCookie, magicCookie, &propertySize), "get audio converter's magic cookie");
		magicCookieSize = propertySize;	// the converter lies and tell us the wrong size
		
		// now set the magic cookie on the output file
		UInt32 willEatTheCookie = false;
		// the converter wants to give us one; will the file take it?
		err = AudioFileGetPropertyInfo(mRecordFile, kAudioFilePropertyMagicCookieData, NULL, &willEatTheCookie);
		if (err == noErr && willEatTheCookie) {
			err = AudioFileSetProperty(mRecordFile, kAudioFilePropertyMagicCookieData, magicCookieSize, magicCookie);
			XThrowIfError(err, "set audio file's magic cookie");
		}
		delete[] magicCookie;
	}
}

void AQRecorder::SetupAudioFormat(UInt32 inFormatID)
{
	memset(&mRecordFormat, 0, sizeof(mRecordFormat));

	UInt32 size = sizeof(mRecordFormat.mSampleRate);
	XThrowIfError(AudioSessionGetProperty(	kAudioSessionProperty_CurrentHardwareSampleRate,
										&size, 
										&mRecordFormat.mSampleRate), "couldn't get hardware sample rate");

	size = sizeof(mRecordFormat.mChannelsPerFrame);
	XThrowIfError(AudioSessionGetProperty(	kAudioSessionProperty_CurrentHardwareInputNumberChannels, 
										&size, 
										&mRecordFormat.mChannelsPerFrame), "couldn't get input channel count");
			
	mRecordFormat.mFormatID = inFormatID;
	if (inFormatID == kAudioFormatLinearPCM)
	{
		// if we want pcm, default to signed 16-bit little-endian
		mRecordFormat.mFormatFlags = kLinearPCMFormatFlagIsSignedInteger | kLinearPCMFormatFlagIsPacked;
		mRecordFormat.mBitsPerChannel = 16;
		mRecordFormat.mBytesPerPacket = mRecordFormat.mBytesPerFrame = (mRecordFormat.mBitsPerChannel / 8) * mRecordFormat.mChannelsPerFrame;
		mRecordFormat.mFramesPerPacket = 1;
	}
}

void AQRecorder::StartRecord(CFStringRef inRecordFile)
{
	int i, bufferByteSize;
	UInt32 size;
	CFURLRef url = nil;
	
	try {		
		mFileName = CFStringCreateCopy(kCFAllocatorDefault, inRecordFile);

		// specify the recording format
		SetupAudioFormat(kAudioFormatLinearPCM);
		
		// create the queue
		XThrowIfError(AudioQueueNewInput(
									  &mRecordFormat,
									  MyInputBufferHandler,
									  this /* userData */,
									  NULL /* run loop */, NULL /* run loop mode */,
									  0 /* flags */, &mQueue), "AudioQueueNewInput failed");
		
		// get the record format back from the queue's audio converter --
		// the file may require a more specific stream description than was necessary to create the encoder.
		mRecordPacket = 0;

		size = sizeof(mRecordFormat);
		XThrowIfError(AudioQueueGetProperty(mQueue, kAudioQueueProperty_StreamDescription,	
										 &mRecordFormat, &size), "couldn't get queue's format");
			
		NSString *recordFile = [NSTemporaryDirectory() stringByAppendingPathComponent: (__bridge NSString*)inRecordFile];	
			
		url = CFURLCreateWithString(kCFAllocatorDefault, (CFStringRef)recordFile, NULL);
		
		// create the audio file
		OSStatus status = AudioFileCreateWithURL(url, kAudioFileCAFType, &mRecordFormat, kAudioFileFlags_EraseFile, &mRecordFile);
		CFRelease(url);
        
        XThrowIfError(status, "AudioFileCreateWithURL failed");
		
		// copy the cookie first to give the file object as much info as we can about the data going in
		// not necessary for pcm, but required for some compressed audio
		CopyEncoderCookieToFile();
		
		// allocate and enqueue buffers
		bufferByteSize = ComputeRecordBufferSize(&mRecordFormat, kBufferDurationSeconds);	// enough bytes for half a second
		for (i = 0; i < kNumberRecordBuffers; ++i) {
			XThrowIfError(AudioQueueAllocateBuffer(mQueue, bufferByteSize, &mBuffers[i]),
					   "AudioQueueAllocateBuffer failed");
			XThrowIfError(AudioQueueEnqueueBuffer(mQueue, mBuffers[i], 0, NULL),
					   "AudioQueueEnqueueBuffer failed");
		}
		// start the queue
		mIsRunning = true;
		XThrowIfError(AudioQueueStart(mQueue, NULL), "AudioQueueStart failed");
	}
	catch (CAXException e) {
		char buf[256];
		fprintf(stderr, "Error: %s (%s)\n", e.mOperation, e.FormatError(buf));
	}
	catch (...) {
		fprintf(stderr, "An unknown error occurred\n");;
	}	

}

void AQRecorder::StopRecord()
{
	// end recording
	mIsRunning = false;
	XThrowIfError(AudioQueueStop(mQueue, true), "AudioQueueStop failed");	
	// a codec may update its cookie at the end of an encoding session, so reapply it to the file now
	CopyEncoderCookieToFile();
	if (mFileName)
	{
		CFRelease(mFileName);
		mFileName = NULL;
	}
	AudioQueueDispose(mQueue, true);
	AudioFileClose(mRecordFile);
}
