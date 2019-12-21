package com.rnxmpp.service;

import androidx.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import com.rnxmpp.utils.Parser;



/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */

public class RNXMPPCommunicationBridge implements XmppServiceListener {

    public static final String RNXMPP_ERROR =       "RNXMPPError";
    public static final String RNXMPP_LOGIN_ERROR = "RNXMPPLoginError";
    public static final String RNXMPP_MESSAGE =     "RNXMPPMessage";
    public static final String RNXMPP_MESSAGE_DELIVERED = "RNXMPPMessageDelivered";
    public static final String RNXMPP_ROSTER =      "RNXMPPRoster";
    public static final String RNXMPP_IQ =          "RNXMPPIQ";
    public static final String RNXMPP_PRESENCE =    "RNXMPPPresence";
    public static final String RNXMPP_CONNECT =     "RNXMPPConnect";
    public static final String RNXMPP_DISCONNECT =  "RNXMPPDisconnect";
    public static final String RNXMPP_LOGIN =       "RNXMPPLogin";
    public static final String RNXMPP_TYPINGSTATUS =       "RNXMPPTypingStatus";
    public static final String RNXMPP_MESSAGE_ID_CREATED =       "RNXMPPMessageIdCreated";
    public static final String RNXMPP_MESSAGE_SENT =       "RNXMPPMessageSent";

    ReactContext reactContext;

    public RNXMPPCommunicationBridge(ReactContext reactContext) {
        this.reactContext = reactContext;
    }

    @Override
    public void onError(Exception e) {
        sendEvent(reactContext, RNXMPP_ERROR, e.getLocalizedMessage());
    }

    @Override
    public void onLoginError(String errorMessage) {
        sendEvent(reactContext, RNXMPP_LOGIN_ERROR, errorMessage);
    }

    @Override
    public void onLoginError(Exception e) {
        this.onLoginError(e.getLocalizedMessage());
    }

    @Override
    public void onMessage(Message message) {
        WritableMap params = Arguments.createMap();

        params.putString("_id", message.getStanzaId());
        params.putString("thread", message.getThread());
        params.putString("subject", message.getSubject());
        params.putString("from", message.getFrom().toString());
        params.putString("src", message.toXML("").toString());
        if (message.getBody() != null) {
            params.putString("body", message.getBody());
            sendEvent(reactContext, RNXMPP_MESSAGE, params);
        } else {
            boolean isChatState = message.hasExtension(ChatStateExtension.NAMESPACE);
            if (isChatState) {
                ChatStateExtension extension = (ChatStateExtension) message.getExtension(ChatStateExtension.NAMESPACE);
                ChatState state = extension.getChatState();
                WritableMap paramStatus = Arguments.createMap();
                paramStatus.putString("from", message.getFrom().toString());
                paramStatus.putString("status", state.name());

                sendEvent(reactContext, RNXMPP_TYPINGSTATUS, paramStatus);
            }
        }
    }

    @Override
    public void onMessageIdGenerated(String messageId) {
        Log.e("Message id is", messageId);
        sendEvent(reactContext, RNXMPP_MESSAGE_ID_CREATED, messageId);
    }

    @Override
    public void onMessageSent(String messageId) {
        sendEvent(reactContext, RNXMPP_MESSAGE_SENT, messageId);
    }

    @Override
    public void onMessageDelivered(String messageId) {
        sendEvent(reactContext, RNXMPP_MESSAGE_DELIVERED, messageId);
    }

    @Override
    public void onRosterReceived(Roster roster) {
        WritableArray rosterResponse = Arguments.createArray();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            WritableMap rosterProps = Arguments.createMap();
            rosterProps.putString("username", rosterEntry.getUser());
            rosterProps.putString("displayName", rosterEntry.getName());

            Presence availability = roster.getPresence(rosterEntry.getJid());
            rosterProps.putString("presence",availability.getType().name());

            WritableArray groupArray = Arguments.createArray();
            for (RosterGroup rosterGroup : rosterEntry.getGroups()) {
                groupArray.pushString(rosterGroup.getName());
            }
            rosterProps.putArray("groups", groupArray);
            rosterProps.putString("subscription", rosterEntry.getType().toString());
            rosterResponse.pushMap(rosterProps);
        }
        sendEvent(reactContext, RNXMPP_ROSTER, rosterResponse);
    }

    @Override
    public void onIQ(IQ iq) {
        sendEvent(reactContext, RNXMPP_IQ, Parser.parse(iq.toString()));
    }

    @Override
    public void onPresence(Presence presence) {
        WritableMap presenceMap = Arguments.createMap();
        presenceMap.putString("type", presence.getType().toString());
        presenceMap.putString("from", presence.getFrom().toString());
        presenceMap.putString("status", presence.getStatus());
        presenceMap.putString("mode", presence.getMode().toString());
        sendEvent(reactContext, RNXMPP_PRESENCE, presenceMap);
    }

    @Override
    public void onConnnect(String username, String password) {
        WritableMap params = Arguments.createMap();
        params.putString("username", username);
        params.putString("password", password);
        sendEvent(reactContext, RNXMPP_CONNECT, params);
    }

    @Override
    public void onDisconnect(Exception e) {

         Log.e("Connection", "Disconnect called from here");
        if (e != null) {
            sendEvent(reactContext, RNXMPP_DISCONNECT, e.getLocalizedMessage());
        } else {
            sendEvent(reactContext, RNXMPP_DISCONNECT, "random");
        }
    }

    @Override
    public void onLogin(String username, String password) {
        WritableMap params = Arguments.createMap();
        params.putString("username", username);
        params.putString("password", password);
        sendEvent(reactContext, RNXMPP_LOGIN, params);
    }

    void sendEvent(ReactContext reactContext, String eventName, @Nullable Object params) {
        reactContext
                .getJSModule(RCTNativeAppEventEmitter.class)
                .emit(eventName, params);
    }
}
