//
//  RNXMPPService.h
//  RNXMPP
//
//  Created by Pavlo Aksonov on 24.09.15.
//  Copyright © 2015 Pavlo Aksonov. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "XMPP.h"
#import "XMPPReconnect.h"
#import "XMPPDateTimeProfiles.h"
#import "NSDate+XMPPDateTimeProfiles.h"
#import "XMPPMUC.h"
#import "XMPPRoom.h"
#import "XMPPRoster.h"
#import "XMPPRosterMemoryStorage.h"
#import "RNXMPPConstants.h"
#import "XMPPStreamManagement.h"
#import "XMPPStreamManagementMemoryStorage.h"
// Fuad
#import "XMPPAutoPing.h"
#import "XMPPMessageDeliveryReceipts.h"

@protocol RNXMPPServiceDelegate <NSObject>

-(void)onError:(NSError *)error;
-(void)onMessage:(XMPPMessage *)message;
-(void)onPresence:(XMPPPresence *)presence;
-(void)onIQ:(XMPPIQ *)iq;
-(void)onRosterReceived:(NSArray *)list;
-(void)onDisconnect:(NSError *)error;
-(void)onConnnect:(NSString *)username password:(NSString *)password;
-(void)onLogin:(NSString *)username password:(NSString *)password;
-(void)onLoginError:(NSError *)error;
// Fuad
-(void)onMessageCreated:(XMPPMessage *)message;
-(void)onMessageDelivered:(NSString *)message;

-(void)onMessageSent:(NSString *)message;
-(void)onMessageIdGenerated:(NSString *)messageId;

@end

@interface RNXMPPService : NSObject <XMPPStreamDelegate>
{
    XMPPStream *xmppStream;
    XMPPRoom *xmppRoom;
    XMPPRoster *xmppRoster;
    XMPPRosterMemoryStorage *xmppRosterStorage;
    XMPPReconnect *xmppReconnect;
    XMPPStreamManagement *xmppStreamManagement;
    id<XMPPStreamManagementStorage> xmppStreamManagentStorage;
    XMPPMUC *xmppMUC;
    NSArray *trustedHosts;
    NSString *username;
    NSString *password;
    AuthMethod authMethod;
    BOOL customCertEvaluation;
    BOOL isXmppConnected;

    // Fuad
    XMPPAutoPing *xmppAutoPing;
    XMPPMessageDeliveryReceipts *deliveryReciepts;
}

@property (nonatomic, strong, readonly) XMPPStream *xmppStream;
@property (nonatomic, strong, readonly) XMPPReconnect *xmppReconnect;
@property (nonatomic, weak) id<RNXMPPServiceDelegate> delegate;
@property (nonatomic) XMPPRoom *xmppRoom;
@property (nonatomic) NSMutableDictionary *xmppRooms;
// Fuad
@property (nonatomic, strong, readonly) XMPPAutoPing *xmppAutoPing;
@property (nonatomic, strong, readonly) XMPPMessageDeliveryReceipts *deliveryReciepts;

+(RNXMPPService *) sharedInstance;
- (void)trustHosts:(NSArray *)hosts;
- (BOOL)connect:(NSString *)myJID withPassword:(NSString *)myPassword auth:(AuthMethod)auth hostname:(NSString *)hostname port:(int)port;
- (void)disconnect;
- (void)disconnectAfterSending;


- (void)sendMessage:(NSString *)text to:(NSString *)username thread:(NSString *)thread;
- (void)sendPresence:(NSString *)to type:(NSString *)type;

- (void)removeRoster:(NSString *)to;
- (void)fetchRoster;
- (void)sendStanza:(NSString *)stanza;
- (void)joinRoom:(NSString *)roomJID nickName:(NSString *)nickname;
- (void)sendRoomMessage:(NSString *)roomJID message:(NSString *)message;
- (void)leaveRoom:(NSString *)roomJID;
// Fuad
- (void)createRoasterEntry:(NSString *)to name:(NSString *)name;
- (void)sendComposingState:(NSString *)to thread:(NSString *)thread state:(NSString *)state;
- (void)joinRoom:(NSString *)roomJID nickName:(NSString *)nickname since:(NSString *)since;

//Surendra
-(void)sendMessageUpdated:(NSString *)text to:(NSString *)to thread:(NSString *)thread messageId:(NSString*)messageId;
- (void)sendRoomMessageUpdated:(NSString *)roomJID message:(NSString *)message messageId:(NSString*)messageId;
-(void)requestMessageId;

@end

