/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/


#import <Foundation/Foundation.h>

@class SampleAppMenu;

@protocol SampleAppMenuCommandProtocol <NSObject>
// this method is called when a menu item is selected.
// the application can return NO to notify the command failed.
- (bool) menuProcess:(SampleAppMenu *) menu command:(int) command value:(bool) value;
@end

@interface SampleAppMenuItem : NSObject
@property (nonatomic, retain) NSString * text;
@property (nonatomic) int command;

- (bool) isON;
- (bool) isRadioButton;
- (bool) isCheckBox;
- (bool) isTextItem;

- (bool) swapSelection;

@end


@interface SampleAppMenuGroup : NSObject
- (NSString *) title;

// command will be used for the notification, a negative value is internally handled as a "back" command
- (void) addTextItem:(NSString *)text command:(int) command;

- (void) addSelectionItem:(NSString *)text command:(int) command isSelected:(bool)isSelected;

- (void) setActiveItem:(int) indexItem;

- (int) nbItems;

@end

@interface SampleAppMenu : NSObject

@property(nonatomic,retain) NSString * title;

@property(nonatomic,assign) id <SampleAppMenuCommandProtocol> commandProtocol;

+ (SampleAppMenu *)instance;

+ (SampleAppMenu *) prepareWithCommandProtocol:(id<SampleAppMenuCommandProtocol>) commandProtocol title:(NSString *) title;

- (SampleAppMenuGroup *) addGroup:(NSString *) title;

- (SampleAppMenuGroup *) addSelectionGroup:(NSString *) title;

- (int) nbGroups;

- (bool) notifyCommand:(int)command value:(bool) value;

- (void) clear;

- (bool)setSelectionValueForCommand:(int ) command value:(bool) value;

// for rendering purpose

- (int) nbItemsInGroupIndex:(int) index;

- (NSString *) titleForGroupIndex:(int) index;

- (SampleAppMenuItem *) itemInGroup:(int) indexGroup atIndex:(int)indexItem;

- (SampleAppMenuGroup *) groupAtIndex:(int) indexGroup;

@end
