package com.rnxmpp.service;

import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Promise;

/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */

public interface XmppService {

    @ReactMethod
    void trustHosts(ReadableArray trustedHosts);

    @ReactMethod
    void connect(String jid, String password, String authMethod, String hostname, Integer port, Promise promise);

    @ReactMethod
    void sendMessage(String text, String to, String thread, Promise promise);

    @ReactMethod
    void sendMessageUpdated(String text, String to, String thread,String messageId, Promise promise);

    @ReactMethod
    void presence(String to, String type, Promise promise);

    @ReactMethod
    void removeRoster(String to, Promise promise);

    @ReactMethod
    void disconnect();

    @ReactMethod
    void fetchRoster(Promise promise);

    @ReactMethod
    void sendStanza(String stanza, Promise promise);

    @ReactMethod
    void createRoasterEntry(String jabberId, String name, Promise promise);

    @ReactMethod
    void sendComposingState(String to, String thread, String state, Promise promise);

    @ReactMethod
    void requestMessageId();

    @ReactMethod
    void joinRoom(String mucJid, String userNickname, String lastMessage, Promise promise);

    @ReactMethod
    void leaveRoom(String mucJid, Promise promise);

    @ReactMethod
    void sendRoomMessage(String roomJid, String text, Promise promise);

    @ReactMethod
    void sendRoomMessageUpdated(String roomJid, String text, String messageId, Promise promise);

}
