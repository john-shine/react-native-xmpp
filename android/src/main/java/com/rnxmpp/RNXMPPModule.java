package com.rnxmpp;

import android.text.TextUtils;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;

import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;

import java.util.logging.Logger;

import com.rnxmpp.service.RNXMPPCommunicationBridge;
import com.rnxmpp.service.XmppServiceSmackImpl;

/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */
public class RNXMPPModule extends ReactContextBaseJavaModule implements com.rnxmpp.service.XmppService {

    public static final String MODULE_NAME = "RNXMPP";
    Logger logger = Logger.getLogger(RNXMPPModule.class.getName());
    XmppServiceSmackImpl xmppService;

    public RNXMPPModule(ReactApplicationContext reactContext) {
        super(reactContext);
        RNXMPPCommunicationBridge listener = new RNXMPPCommunicationBridge(reactContext);
        xmppService = new XmppServiceSmackImpl(listener);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    @ReactMethod
    public void trustHosts(ReadableArray trustedHosts) {
        this.xmppService.trustHosts(trustedHosts);
    }

    @Override
    @ReactMethod
    public void connect(String jid, String password, String authMethod, String hostname, Integer port, Promise promise){
        this.xmppService.connect(jid, password, authMethod, hostname, port, promise);
    }

    @ReactMethod
    public void joinRoom(String mucJid, String userNickname, String timestamp, Promise promise) {
        if(!TextUtils.isEmpty(userNickname))
        this.xmppService.joinRoom(mucJid, userNickname, timestamp, promise);
    }

    @ReactMethod
    public void leaveRoom(String mucJid, Promise promise) {
        this.xmppService.leaveRoom(mucJid, promise);
    }

    @ReactMethod
    public void sendRoomMessage(String mucJid, String text, Promise promise) {
        this.xmppService.sendRoomMessage(mucJid, text, promise);
    }

    @ReactMethod
    public void sendRoomMessageUpdated(String mucJid, String text, String messageId, Promise promise) {
        this.xmppService.sendRoomMessageUpdated(mucJid, text, messageId, promise);
    }

    @Override
    @ReactMethod
    public void sendMessage(String text, String to, String thread, Promise promise){
        this.xmppService.sendMessage(text, to, thread, promise);
    }

    @Override
    @ReactMethod
    public void sendMessageUpdated(String text, String to, String thread, String messageId, Promise promise) {
        this.xmppService.sendMessageUpdated(text, to, thread, messageId, promise);
    }

    @Override
    @ReactMethod
    public void presence(String to, String type, Promise promise) {
        this.xmppService.presence(to, type, promise);
    }

    @Override
    @ReactMethod
    public void removeRoster(String to, Promise promise) {
        this.xmppService.removeRoster(to, promise);
    }

    @Override
    @ReactMethod
    public void disconnect() {
        this.xmppService.disconnect();
    }

    @Override
    @ReactMethod
    public void fetchRoster(Promise promise) {
        this.xmppService.fetchRoster(promise);
    }

    @Override
    @ReactMethod
    public void sendStanza(String stanza, Promise promise) {
        this.xmppService.sendStanza(stanza, promise);
    }

    @Override
    @ReactMethod
    public void createRoasterEntry(String jabberId, String name, Promise promise) {
        this.xmppService.createRoasterEntry(jabberId, name, promise);
    }

    @Override
    @ReactMethod
    public void sendComposingState(String to, String thread, String state, Promise promise) {
        this.xmppService.sendComposingState(to, thread, state, promise);
    }

    @Override
    @ReactMethod
    public void requestMessageId(Promise promise) {
        this.xmppService.requestMessageId(promise);
    }

}
