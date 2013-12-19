//
//  WheelphoneRobot.h
//  ios-wheelphone-library
//
//  Created by Stefano Morgani on 17.10.13.
//  Copyright (c) 2013 Stefano Morgani. All rights reserved.
//

#import <UIKit/UIKit.h>
#include <AVFoundation/AVFoundation.h>
#import <AudioToolbox/AudioToolbox.h>
#import <OpenAl/al.h>
#import <OpenAl/alc.h>

@interface WheelphoneRobot : NSObject {
    
    // COMMUNICATION
    AVAudioPlayer *testAudioPlayer;
    NSString *audioFilePath[12];
    NSURL *audioFileURL[12];
    int commTimeout;
    int commTimeoutLimit;		// based on "handleCommandsToRobot" task (repeatedly scheduled at about 50 ms)
    BOOL isConnected;					// flag indicating if the robot is connected (and exchanging packets) with the phone
    
	// ROBOT STATE (robot => phone)
	int proxValues[4];                  // front proximity values (higher value means nearer object)
	int proxAmbientValues[4];           // front proximity ambient values
	int proxValuesCalibration[4];       // front proximity values of the calibration
	int groundValues[4];				// ground proximity values (darker surface/cliff => lower values)
    int groundAmbientValues[4];         // ground proximity ambient values
	int groundValuesCalibration[4];     // ground proximity values of the calibration
	int maxBatteryValue;
	int battery;
	int16_t leftMeasuredSpeed, rightMeasuredSpeed;	// they contain the current measured motors speed in mm/s (speed measured using back EMF)
	char flagRobotToPhone;				// bit 5: 1 => robot is charging, 0 => robot not charging
                                        // bit 6: 1 => robot completely charged, 0 => robot not completely charged
                                        // others bits not used
    int chargeState;
	BOOL isCalibrating;
	int isCalibratingCounter;           // counter used to wait for the completion of the calibration
	int firmwareVersion;				// robot firmware version
	BOOL odomCalibFinish;
	BOOL obstacleAvoidanceEnabled;
    BOOL cliffAvoidanceEnabled;
    
	// ROBOT CONTROL (phone => robot)
	int lSpeed, rSpeed;                 // desired left and right speed
    int avgSpeed, avgSpeedPrev;         // average speed => (left+right)/2
    int rotSpeed;                       // rotational speed => (left-right)/2
    int currentLeftSpeed, currentRightSpeed;    // current left and right speed; these are the speed that are currently set on the robot and are updated until they reach the desired speed
	char flagPhoneToRobot;              // bit 0 => controller On/Off
                                        // bit 1 => soft acceleration On/Off
                                        // bit 2 => obstacle avoidance On/Off
                                        // bit 3 => cliff avoidance On/Off
                                        // others bits not used
    char currFlagPhoneToRobot;
    bool stopSent;
    int noStopCount;
    
	// VARIOUS
	BOOL debug;
	NSString * logString;
	
	// ODOMETRY
    double leftDiamCoeff;
	double rightDiamCoeff;
	double wheelBase;					// meters
	double odometry[4];                 // x (mm), y (mm), theta (radians) respectively
	double leftDist, rightDist;
	double leftDistPrev, rightDistPrev;
	double deltaDist;
    NSDate *startTime, *finalTime;
    NSTimeInterval totalTime;
	BOOL logEnabled;
    
    // TESTING
    //SystemSoundID _calibrateSound;
    
}

- (void)registerForBackgroundNotifications;

/**
 * \brief Set the new left and right speeds for the robot. The new data
 *  will be actually sent to the robot when "sendCommandsToRobot" is
 * called the next time within the timer communication task (50 ms cadence). This means that the robot speed will be updated
 * after at most 50 ms (if the task isn't delayed by the system).
 * \param l left speed given in mm/s
 * \param r right speed given in mm/s
 * \return none
 */
- (void) setSpeedleft: (int) l right: (int) r;	// speed given in mm/s

/**
 * \brief Set the new left speed for the robot. For more details refer to "setSpeed".
 * \param l left speed given in mm/s
 * \return none
 */
- (void) setLeftSpeed: (int) l;	// speed given in mm/s

/**
 * \brief Set the new right speed for the robot. For more details refer to "setSpeed".
 * \param r right speed given in mm/s
 * \return none
 */
- (void) setRightSpeed: (int) r;	// speed given in mm/s

/**
 * \brief Set the new left and right speeds for the robot. For more details refer to "setSpeed".
 * \param l left speed (range is from -127 to 127)
 * \param r right speed (range is from -127 to 127)
 * \return none
 */
- (void) setRawSpeedleft: (int) l right: (int) r;

/**
 * \brief Set the new left speed for the robot. For more details refer to "setSpeed".
 * \param l left speed (range is from -127 to 127)
 * \return none
 */
- (void) setRawLeftSpeed: (int) l;

/**
 * \brief Set the new right speed for the robot. For more details refer to "setSpeed".
 * \param r right speed (range is from -127 to 127)
 * \return none
 */
- (void) setRawRightSpeed: (int) r;

/**
 * \brief Flags setting directly with a byte value.
 * \param value representing the flags to enable (1) or disable (0):
 * - bit1: speed control enable/disable
 * - bit2: soft acceleration enable/disable
 * - bit3: obstacle avoidance enable/disable
 * - bit4: cliff avoidance enable/disable
 * - bit5: calibrate sensors
 * - bit6: calibrate odometry
 * - bit7/8: not used
 * \return none
 */
- (void) setFlagsPhoneToRobot: (char) value;

/**
 * \brief Enable speed control on the robot (controller based on speed measure with back-emf).
 * \return none
 */
- (void) enableSpeedControl;

/**
 * \brief Disable speed control on the robot.
 * \return none
 */
- (void) disableSpeedControl;

/**
 * \brief Enable soft acceleration on the robot; this is useful when the robot is started fast (from standstill)
 * because it avoid the robot to wheelie.
 * \return none
 */
- (void) enableSoftAcceleration;

/**
 * \brief Disable soft acceleration on the robot.
 * \return none
 */
- (void) disableSoftAcceleration;

/**
 * \brief Enable obstacle avoidance onboard.
 * \return none
 */
- (void) enableObstacleAvoidance;

/**
 * \brief Disable obstacle avoidance onboard.
 * \return none
 */
- (void) disableObstacleAvoidance;

/**
 * \brief Enable cliff avoidance onboard; when a cliff is detected the robot is stopped until this flag is reset.
 * \return none
 */
- (void) enableCliffAvoidance;

/**
 * \brief Disable cliff avoidance onboard.
 * \return none
 */
- (void) disableCliffAvoidance;

/**
 * \brief Start the calibration of all the sensors. Use "isCalibrating" to know when the calibration is done.
 * \return none
 */
- (void) calibrateSensors;

/**
 * \brief Returns the sampled value of the battery.
 * \return battery level (from 0 to maxBatteryValue=152)
 */
- (int) getBatteryRaw;

/**
 * \brief Returns the current battery voltage.
 * \return battery voltage (from 3.5 to 4.2 volts)
 */
- (float) getBatteryVoltage;

/**
 * \brief Returns the percentage of charge remaining in the battery.
 * \return battery charge (from 0% to 100%)
 */
- (int) getBatteryCharge;

/**
 * \brief Returns the battery charge low status.
 * \return true if low, false otherwise
 */
- (BOOL) batteryIsLow;

/**
 * \brief Returns the flag byte that the robot set/clear itself.
 * \return flag byte:
 * - bit 5: 1 => robot is charging, 0 => robot not charging
 * - bit 6: 1 => robot completely charged, 0 => robot not completely charged
 * - others bits not used
 */
- (char) getFlagStatus;

/**
 * \brief Returns the charging status.
 * \return true if charging, false otherwise
 */
- (BOOL) isCharging;

/**
 * \brief Returns the battery charged status.
 * \return true if battery charged, false otherwise
 */
- (BOOL) isCharged;

/**
 * \brief The value of the left motor speed returned from the robot. This value is the measured speed using back EMF.
 * \return left speed (positive or negative)
 */
- (int) getLeftSpeed;

/**
 * \brief The value of the right motor speed returned from the robot. For more details refer to "getLeftSpeed".
 * \return right speed (positive or negative)
 */
- (int) getRightSpeed;

/**
 * \brief The robot has 4 front proximity sensors positioned as follows:
 * \verbatim
 1	2
 0			3
 \endverbatim
 * The higher the value the nearer the object in front of the sensor.
 * \return array of size 4 containing the sensors values
 */
- (void) getFrontProxs: (int*) arr;

/**
 * \brief Return the corresponding front proximity sensor value. For more details refer to "getFrontProxs".
 * \return sensor value
 */
- (int) getFrontProx:(int) ind;

/**
 * \brief The robot has 4 front ambient sensors, actually they are the front proximity sensors that can measure also
 *  the ambient light. The higher the value the lighter the environment.
 * \return array of size 4 containing the sensors values
 */
- (void) getFrontAmbients: (int*) arr;

/**
 * \brief Return the corresponding front ambient sensor value. For more details refer to "getFrontAmbients".
 * \return sensor value
 */
- (int) getFrontAmbient: (int) ind;

/**
 * \brief When a calibration is done with "calibrateSensors" then the calibrated values are internally stored; these values
 *  can be obtained with this function.
 * \return array of size 4 containing the sensors calibration values
 */
- (void) getFrontProxCalibrationValues: (int*) arr;

/**
 * \brief The robot has 4 ground proximity sensors positioned as follows:
 * \verbatim
 1	2
 0			3
 \endverbatim
 * The higher the value the darker the object in front of the sensor.
 * \return array of size 4 containing the sensors values
 */
- (void) getGroundProxs: (int*) arr;

/**
 * \brief Return the corresponding ground proximity sensor value. For more details refer to "getGroundProxs".
 * \return sensor value
 */
- (int) getGroundProx: (int) ind;

/**
 * \brief The robot has 4 ground ambient sensors, actually they are the ground proximity sensors that can measure also
 *  the ambient light. The higher the value the lighter the environment.
 * \return array of size 4 containing the sensors values
 */
- (void) getGroundAmbients: (int*) arr;

/**
 * \brief Return the corresponding ground ambient sensor value. For more details refer to "getGroundAmbients".
 * \return sensor value
 */
- (int) getGroundAmbient: (int) ind;

/**
 * \brief When a calibration is done with "calibrateSensors" then the calibrated values are internally stored; these values
 *  can be obtained with this function.
 * \return array of size 4 containing the sensors calibration values
 */
- (void) getGroundProxCalibrationValues: (int*) arr;

/**
 * \brief Tell whether the calibration is still in progress or not.
 * \return true (calibration in progress), false otherwise
 */
- (BOOL) isCalibrating;

/**
 * \brief Return the odometry information resulting from the measured speeds values received by the robot.
 * The positive x axis is pointing forward and the positive y axis is pointing to the left side of the robot.
 * \return array of length 3 containing sequentially x position (mm), y position (mm), theta (radians).
 */
- (void) getOdometry: (double*) arr;

/**
 * \brief Return the x absolute position in mm. For more information refer to "getOdometry".
 * \return x position (mm)
 */
- (double) getOdometryX;

/**
 * \brief Return the y absolute position in mm. For more information refer to "getOdometry".
 * \return y position (mm)
 */
- (double) getOdometryY;

/**
 * \brief Return the theta absolute angle in radians. For more information refer to "getOdometry".
 * \return theta (radians)
 */
- (double) getOdometryTheta;

/**
 * \brief Set/reset odometry components. For more information refer to "getOdometry".
 * \param x x position (mm)
 * \param y y position (mm)
 * \param t theta angle (radians)
 * \return none
 */
- (void) setOdometryx: (double) x y: (double) y theta: (double) t;

/**
 * \brief Set/reset odometry parameters.
 * \param dl left wheel diameter coefficient
 * \param dr right wheel diameter coefficient
 * \param wb wheels distance (m)
 * \return none
 */
- (void) setOdometryParametersleftDiamCoeff: (double) ldc rightDiamCoeff: (double) rdc wheelBase: (double) wb;

- (void) calibrateOdometry;
- (BOOL) odometryCalibrationTerminated;
- (void) resetOdometry;

/**
 * \brief Enable the logging of the sensors data received from the robot and the computed odometry.
 */
- (void) enableDataLog;

/**
 * \brief Disable the logging.
 */
- (void) disableDataLog;

- (void) appendLog: (NSString*) text;

/**
 * \brief Indicate whether the robot is connected (and exchanging packets) with the phone or not.
 * \return true (if robot connected), false otherwise
 */
- (BOOL) isRobotConnected;

/**
 * \brief This timeout sets how much to wait for a response from the robot before changing to a disconnected state.
 * \param timeout in milliseconds
 * \return none
 */
- (void) setCommunicationTimeout: (int) ms;

- (BOOL) isObstacleAvoidanceEnabled;
- (BOOL) isCliffAvoidanceEnabled;


@end
