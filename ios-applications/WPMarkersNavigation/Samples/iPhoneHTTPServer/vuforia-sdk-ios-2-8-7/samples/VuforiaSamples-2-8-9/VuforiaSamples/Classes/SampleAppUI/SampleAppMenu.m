/*==============================================================================
 Copyright (c) 2012-2013 Qualcomm Connected Experiences, Inc.
 All Rights Reserved.
 ==============================================================================*/

#import "SampleAppMenu.h"

#define kMaxMenuGroups 5
#define kMaxMenuItemsPerGroup 12

typedef enum {
    TYPE_TEXT,
    TYPE_CHECKBOX,
    TYPE_RADIOBUTTON
} MenuType;

@interface SampleAppMenuItem()
@property (nonatomic) int itemType;
@property (nonatomic) bool selectionValue;
@end

@implementation SampleAppMenuItem
- (bool) isON {
    return self.selectionValue;
}

- (bool) isRadioButton {
    return self.itemType == TYPE_RADIOBUTTON;
}

- (bool) isCheckBox{
    return self.itemType == TYPE_CHECKBOX;
}

- (bool) isTextItem {
    return self.itemType == TYPE_TEXT;
}

- (bool) swapSelection {
    self.selectionValue = ! self.selectionValue;
    return self.selectionValue;
}
@end

@interface SampleAppMenuGroup()
@property (nonatomic, retain) NSString * title;
@property (nonatomic) bool isSelectionGroup;
@property (retain) NSMutableArray * menuItems;

@end

@implementation SampleAppMenuGroup

- (id)initWithTitle:(NSString *) title;
{
    self = [super init];
    if (self) {
        self.title = title;
        self.isSelectionGroup = NO;
        self.menuItems = [NSMutableArray arrayWithCapacity:kMaxMenuItemsPerGroup];
    }
    return self;
}

- (void) addTextItem:(NSString *)text command:(int) command {
    SampleAppMenuItem * mi = [[[SampleAppMenuItem alloc]init] autorelease];
    mi.text = text;
    mi.command = command;
    mi.itemType = TYPE_TEXT;
    mi.selectionValue = NO;
    [self.menuItems addObject:mi];
}

- (void) addSelectionItem:(NSString *)text command:(int) command isSelected:(bool)isSelected {
    SampleAppMenuItem * mi = [[[SampleAppMenuItem alloc]init] autorelease];
    mi.text = text;
    mi.command = command;
    mi.itemType = self.isSelectionGroup ? TYPE_RADIOBUTTON : TYPE_CHECKBOX;
    mi.selectionValue = isSelected;
    [self.menuItems addObject:mi];
}

- (SampleAppMenuItem *) itemAtIndex:(int) index {
    return [self.menuItems objectAtIndex:index];
}

- (void) setActiveItem:(int) indexItem {
    int index = -1;
    for (SampleAppMenuItem * item in self.menuItems) {
        index++;
        item.selectionValue = (index == indexItem) ;
    }
}

- (bool)setSelectionValueForCommand:(int) command value:(bool) value {
    for (SampleAppMenuItem * item in self.menuItems) {
        if (item.command == command) {
            item.selectionValue = value;
            return true;
        }
    }
    return false;
}

- (NSString *) titleOrNil {
    if ((self.title == nil) || ([self.title length] == 0)) {
        return nil;
    }
    return self.title;
}

- (int) nbItems {
    return [self.menuItems count];
}

@end

@interface SampleAppMenu ()
@property (retain) NSMutableArray * menuGroups;
@end

@implementation SampleAppMenu

+(SampleAppMenu *)instance {
    static dispatch_once_t pred;
    static SampleAppMenu *shared = nil;
    
    dispatch_once(&pred, ^{
        shared = [[SampleAppMenu alloc] init];
    });
    return shared;
}

+ (SampleAppMenu *) prepareWithCommandProtocol:(id<SampleAppMenuCommandProtocol>) commandProtocol title:(NSString *) title {
    SampleAppMenu * menu = [SampleAppMenu instance];
    menu.commandProtocol = commandProtocol;
    menu.title = title;
    [menu.menuGroups removeAllObjects];
    return menu;
}

- (id)init
{
    self = [super init];
    if (self) {
        self.menuGroups = [NSMutableArray arrayWithCapacity:kMaxMenuGroups];
    }
    return self;
}

- (SampleAppMenuGroup *) addGroup:(NSString *) title {
    SampleAppMenuGroup * group = [[SampleAppMenuGroup alloc] initWithTitle:title];
    [self.menuGroups addObject:group];
    [group release];
    return group;
}

- (SampleAppMenuGroup *) addSelectionGroup:(NSString *) title {
    SampleAppMenuGroup * group = [self addGroup:title];
    group.isSelectionGroup = true;
    return group;
}

- (int) nbGroups {
    return [self.menuGroups count];
}

- (int) nbItemsInGroupIndex:(int) index {
    SampleAppMenuGroup * group = [self.menuGroups objectAtIndex:index];
    return [group nbItems];
}

- (NSString *) titleForGroupIndex:(int) index {
    SampleAppMenuGroup * group = [self.menuGroups objectAtIndex:index];
    return [group titleOrNil];
}


- (bool)setSelectionValueForCommand:(int) command value:(bool) value {
    for (SampleAppMenuGroup * group in self.menuGroups) {
        if ([group setSelectionValueForCommand:command value:value]) {
            [[NSNotificationCenter defaultCenter] postNotificationName:@"kMenuChange" object:nil];
            return true;
        }
    }
    return false;
}
- (SampleAppMenuGroup *) groupAtIndex:(int) indexGroup {
    return [self.menuGroups objectAtIndex:indexGroup];
}

- (SampleAppMenuItem *) itemInGroup:(int) indexGroup atIndex:(int)indexItem {
    SampleAppMenuGroup * group = [self.menuGroups objectAtIndex:indexGroup];
    return [group itemAtIndex:indexItem];
}

- (bool) notifyCommand:(int)command  value:(bool) value{
    return [self.commandProtocol menuProcess:self command:command value:value];
}

- (void) clear {
    [self.menuGroups removeAllObjects];
    self.title = nil;
}
@end
