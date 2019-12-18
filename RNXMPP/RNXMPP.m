//
//  XMPP.m
//  RNXMPP
//
//  Created by Pavlo Aksonov on 23.09.15.
//  Copyright © 2015 Pavlo Aksonov. All rights reserved.
//

#import "RNXMPP.h"
#import "RNXMPPConstants.h"

const NSString *PLAIN_AUTH = @"PLAIN";
const NSString *SCRAMSHA1_AUTH = @"SCRAMSHA1";
const NSString *DigestMD5_AUTH = @"DigestMD5";

@implementation RCTConvert (AuthMethod)
RCT_ENUM_CONVERTER(AuthMethod, (@{ PLAIN_AUTH : @(Plain),
                                             SCRAMSHA1_AUTH : @(SCRAM),
                                             DigestMD5_AUTH : @(MD5)}),
                                          SCRAM, integerValue)
@end

@implementation RNXMPP {
    RCTResponseSenderBlock onError;
    RCTResponseSenderBlock onConnect;
    RCTResponseSenderBlock onMessage;
    RCTResponseSenderBlock onIQ;
    RCTResponseSenderBlock onPresence;
    // Fuad
    RCTResponseSenderBlock onMessageCreated;
    RCTResponseSenderBlock onMessageDelivered;
    
    //Surendra
    RCTResponseSenderBlock onMessageSent;
    RCTResponseSenderBlock onMessageIdGenerated;
}

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE();


-(void)onError:(NSError *)error {
    NSString *message = [error localizedDescription];
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPError" body:message];
}

-(void)onLoginError:(NSError *)error {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPLoginError" body:[error localizedDescription]];
}

-(id)contentOf:(XMPPElement *)element{
    NSMutableDictionary *res = [NSMutableDictionary dictionary];
    if ([element respondsToSelector:@selector(attributesAsDictionary)]){
        res = [element attributesAsDictionary];
    }
    if (element.children){
        for (XMPPElement *child in element.children){
            if (res[child.name] && ![res[child.name] isKindOfClass:[NSArray class]]){
                res[child.name] = [NSMutableArray arrayWithObjects:res[child.name], nil];
            }
            if (res[child.name]){
                [res[child.name] addObject:[self contentOf:child]];
            } else {
                if ([child.name isEqualToString:@"text"]){
                    if ([res count]){
                        res[@"#text"] = [self contentOf:child];
                    } else {
                        return [self contentOf:child];
                    }
                } else {
                    res[child.name] = [self contentOf:child];
                }
            }
        }
    }
    if ([res count]){
        return res;
    } else {
        return [element stringValue];
    }
}

-(void)onMessage:(XMPPMessage *)message {
    if ([message isMessageWithBody]) {
        NSDictionary *res = [self contentOf:message];
        [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPMessage" body:res];
    } else {
        // Fuad
        for (NSXMLElement *child in [message children]) {
            if ([[child namespaceStringValueForPrefix:@""] isEqualToString:@"http://jabber.org/protocol/chatstates"]) {
                NSMutableDictionary *res = @{
                                             @"from": [message attributeStringValueForName:@"from"],
                                             @"status": child.name
                                             };
                [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPTypingStatus" body:res];
                return;
            }
        }
    }
}

-(void)onRosterReceived:(NSArray *)list {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPRoster" body:list];
}

-(void)onIQ:(XMPPIQ *)iq {
    NSDictionary *res = [self contentOf:iq];
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPIQ" body:res];
}

-(void)onPresence:(XMPPPresence *)presence {
    NSMutableDictionary *res = [self contentOf:presence];
    // Fuad
    if ([res objectForKey:@"type"] == nil) {
        [res setValue:@"available" forKey:@"type"];
    }
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPPresence" body:res];
}

-(void)onConnnect:(NSString *)username password:(NSString *)password {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPConnect" body:@{@"username":username, @"password":password}];
}

-(void)onDisconnect:(NSError *)error {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPDisconnect" body:[error localizedDescription]];
    if ([error localizedDescription]){
        [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPLoginError" body:[error localizedDescription]];
    }
}

-(void)onLogin:(NSString *)username password:(NSString *)password {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPLogin" body:@{@"username":username, @"password":password}];
}

RCT_EXPORT_METHOD(trustHosts:(NSArray *)hosts){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] trustHosts:hosts];
}

RCT_EXPORT_METHOD(connect:(NSString *)jid password:(NSString *)password auth:(AuthMethod) auth hostname:(NSString *)hostname port:(int)port){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] connect:jid withPassword:password auth:auth hostname:hostname port:port];
}

RCT_EXPORT_METHOD(message:(NSString *)text to:(NSString *)to thread:(NSString *)threadId){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendMessage:text to:to thread:threadId];
}

RCT_EXPORT_METHOD(presence:(NSString *)to type:(NSString *)type){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendPresence:to type:type];
}

RCT_EXPORT_METHOD(removeRoster:(NSString *)to){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] removeRoster:to];
}

RCT_EXPORT_METHOD(disconnect){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] disconnect];
}

RCT_EXPORT_METHOD(disconnectAfterSending){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] disconnectAfterSending];
}

RCT_EXPORT_METHOD(fetchRoster){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] fetchRoster];
}

RCT_EXPORT_METHOD(sendStanza:(NSString *)stanza){
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendStanza:stanza];
}

RCT_EXPORT_METHOD(joinRoom:(NSString *)roomJID nickName:(NSString *)nickname)
{
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] joinRoom:roomJID nickName:nickname];
}

// Fuad
RCT_EXPORT_METHOD(joinRoom:(NSString *)roomJID nickName:(NSString *)nickname since:(NSString *)since)
{
    [RNXMPPService sharedInstance].delegate = self;
    NSTimeInterval timeInterval =  ([since doubleValue] / 1000);
    NSLog(@"since %d", since);
    NSDate *date = [NSDate dateWithTimeIntervalSince1970: timeInterval];
    NSDateFormatter *dateformatter = [[NSDateFormatter alloc] init];
    [dateformatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"];
    NSString *dateString=[dateformatter stringFromDate:date];
    NSLog(@"Fetch msgs since: %@", dateString);
    [[RNXMPPService sharedInstance] joinRoom:roomJID nickName:nickname since:dateString];
}

RCT_EXPORT_METHOD(leaveRoom:(NSString *)roomJID)
{
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] leaveRoom:roomJID];
}

RCT_EXPORT_METHOD(sendRoomMessage:(NSString *)roomJID message:(NSString *)message)
{
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendRoomMessage:roomJID message:message];
}



// Surendra - Start
RCT_EXPORT_METHOD(requestMessageId)
{
    NSLog(@"Surnedra.. : calling from RCT requestMessageId");
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] requestMessageId];
}

RCT_EXPORT_METHOD(messageUpdated:(NSString *)text to:(NSString *)to thread:(NSString *)threadId messageId:(NSString*)messageId){
    NSLog(@"Surnedra.. : calling from RCT sendMessageUpdated");
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendMessageUpdated:text to:to thread:threadId messageId:messageId];
}

RCT_EXPORT_METHOD(sendRoomMessageUpdated:(NSString *)roomJID message:(NSString *)message messageId:(NSString*)messageId)
{
    NSLog(@"Surnedra.. : calling from RCT sendRoomMessageUpdated");
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendRoomMessageUpdated:roomJID message:message messageId:messageId];
}

// Surendra - End

- (NSDictionary *)constantsToExport
{
    return @{ PLAIN_AUTH : @(Plain),
              SCRAMSHA1_AUTH: @(SCRAM),
              DigestMD5_AUTH: @(MD5)
              };
};

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}


#pragma mark - Fuad

-(void)onMessageSent:(NSString *)message {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPMessageSent" body:message];
    NSLog(@"Surnedra.. : called RNXMPPMessageDelivered");
}

-(void)onMessageIdGenerated:(NSString *)messageId {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPMessageIdCreated" body:messageId];
    NSLog(@"Surnedra.. : called RNXMPPMessageIdCreated");
}



-(void)onMessageCreated:(XMPPMessage *)message {
    NSDictionary *res = [self contentOf:message];
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPMessageCreated" body:res];
}

-(void)onMessageDelivered:(NSString *)message {
    [self.bridge.eventDispatcher sendAppEventWithName:@"RNXMPPMessageDelivered" body:message];
}

RCT_EXPORT_METHOD(createRoasterEntry:(NSString *)to name:(NSString *)name)
{
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] createRoasterEntry:to name:name];
}

RCT_EXPORT_METHOD(sendComposingState:(NSString *)to thread:(NSString *)thread state:(NSString *)state)
{
    [RNXMPPService sharedInstance].delegate = self;
    [[RNXMPPService sharedInstance] sendComposingState:to thread:thread state:state];

}

@end
