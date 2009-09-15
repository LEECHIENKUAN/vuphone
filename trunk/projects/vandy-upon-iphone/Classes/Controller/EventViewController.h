//
//  EventViewController.h
//  VandyUpon
//
//  Created by Aaron Thompson on 9/8/09.
//  Copyright 2009 Iostudio, LLC. All rights reserved.
//

#import <UIKit/UIKit.h>

#import "Event.h"
#import "Location.h"

#import "VUTableViewController.h"

@interface EventViewController : VUTableViewController
{
	IBOutlet UITableView *myTableView;
	IBOutlet UIBarButtonItem *saveButton;
	IBOutlet UIBarButtonItem *cancelButton;
	IBOutlet UIBarButtonItem *cancelAddButton;
	IBOutlet UIBarButtonItem *editButton;

	Event *event;
	NSManagedObjectContext *context;
	NSDateFormatter *dateFormatter;
}

- (IBAction)save:(id)sender;
- (IBAction)cancelAdd:(id)sender;
- (IBAction)beginEditing:(id)sender;
- (IBAction)cancelEditing:(id)sender;
- (id)valueForVUEditableTableViewRow:(NSUInteger)row inSection:(NSUInteger)section;

@property (nonatomic, retain, setter=setEvent) Event *event;
@property (nonatomic, retain) NSManagedObjectContext *context;

@end
