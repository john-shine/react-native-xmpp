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

    public static final String RNXMPP_MESSAGE_RECEIVED   = "RNXMPPMessageReceived";
    public static final String RNXMPP_MESSAGE_DELIVERED  = "RNXMPPMessageDelivered";
    public static final String RNXMPP_ROSTER             = "RNXMPPRoster";
    public static final String RNXMPP_IQ                 = "RNXMPPIQ";
    public static final String RNXMPP_PRESENCED          = "RNXMPPPresenced";
    public static final String RNXMPP_CONNECTED          = "RNXMPPConnected";
    public static final String RNXMPP_DISCONNECTED       = "RNXMPPDisconnected";
    public static final String RNXMPP_AUTHENTICATED      = "RNXMPPAuthenticated";
    public static final String RNXMPP_TYPINGSTATUS       = "RNXMPPTypingStatus";

    ReactContext reactContext;

    public RNXMPPCommunicationBridge(ReactContext reactContext) {
        this.reactContext = reactContext;
    }

    @Override
    public void onMessageReceived(Message message) {
        WritableMap params = Arguments.createMap();

        params.putString("_id", message.getStanzaId());
        params.putString("thread", message.getThread());
        params.putString("subject", message.getSubject());
        params.putString("from", message.getFrom().toString());
        params.putString("src", message.toXML("").toString());
        if (message.getBody() != null) {
            params.putString("body", message.getBody());
            sendEvent(reactContext, RNXMPP_MESSAGE_RECEIVED, params);
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
    public void onGroupMessageReceived(Message message) {
        WritableMap params = Arguments.createMap();

        params.putString("_id", message.getStanzaId());
        params.putString("thread", message.getThread());
        params.putString("subject", message.getSubject());
        params.putString("from", message.getFrom().toString());
        params.putString("src", message.toXML("").toString());
        if (message.getBody() != null) {
            params.putString("body", message.getBody());
            sendEvent(reactContext, RNXMPP_MESSAGE_RECEIVED, params);
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
    public void onPresenced(Presence presence) {
        WritableMap presenceMap = Arguments.createMap();
        presenceMap.putString("type", presence.getType().toString());
        presenceMap.putString("from", presence.getFrom().toString());
        presenceMap.putString("status", presence.getStatus());
        presenceMap.putString("mode", presence.getMode().toString());
        sendEvent(reactContext, RNXMPP_PRESENCED, presenceMap);
    }

    @Override
    public void onConnnected(String username, String password) {
        WritableMap params = Arguments.createMap();
        params.putString("username", username);
        params.putString("password", password);
        sendEvent(reactContext, RNXMPP_CONNECTED, params);
    }

    @Override
    public void onDisconnected(Exception e) {
        Log.e("Connection", "Disconnect called from here");
        if (e != null) {
            sendEvent(reactContext, RNXMPP_DISCONNECTED, e.getLocalizedMessage());
        } else {
            sendEvent(reactContext, RNXMPP_DISCONNECTED, "random");
        }
    }

    @Override
    public void onAuthenticated(String username, String password) {
        WritableMap params = Arguments.createMap();
        params.putString("username", username);
        params.putString("password", password);
        sendEvent(reactContext, RNXMPP_AUTHENTICATED, params);
    }

    void sendEvent(ReactContext reactContext, String eventName, @Nullable Object params) {
        reactContext
            .getJSModule(RCTNativeAppEventEmitter.class)
            .emit(eventName, params);
    }
}
