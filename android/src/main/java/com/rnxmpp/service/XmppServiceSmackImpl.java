package com.rnxmpp.service;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterLoadedListener;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateListener;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jivesoftware.smack.MessageListener;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
// import java.util.concurrent.ScheduledThreadPoolExecutor;
// import java.util.concurrent.TimeUnit;
// import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */

public class XmppServiceSmackImpl implements XmppService, ChatMessageListener, ChatManagerListener, StanzaListener, ConnectionListener, ChatStateListener, RosterLoadedListener, ReceiptReceivedListener {
    XmppServiceListener xmppServiceListener;
    MessageListener groupMessageListner = new MessageListener() {
        @Override
        public void processMessage(Message message) {
            xmppServiceListener.onGroupMessageReceived(message);
        }
    };
    Logger logger = Logger.getLogger(XmppServiceSmackImpl.class.getName());

    XMPPTCPConnection connection;
    Roster roster;
    List<String> trustedHosts = new ArrayList<>();
    String password;

    public XmppServiceSmackImpl(XmppServiceListener xmppServiceListener) {
        this.xmppServiceListener = xmppServiceListener;
    }

    @Override
    public void trustHosts(ReadableArray trustedHosts) {
        for(int i = 0; i < trustedHosts.size(); i++){
            this.trustedHosts.add(trustedHosts.getString(i));
        }
    }

    public static InetAddress getInetAddressByName(String name) {
        AsyncTask<String, Void, InetAddress> task = new AsyncTask<String, Void, InetAddress>() {

            @Override
            protected InetAddress doInBackground(String... params) {
                try {
                    return InetAddress.getByName(params[0]);
                } catch (UnknownHostException e) {
                    return null;
                }
            }
        };

        try {
            return task.execute(name).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }

    }

    String username ="", password1="";
    @Override
    public void connect(String jid, String password, String authMethod, String hostname, Integer port, Promise promise) {
        final String[] jidParts = jid.split("@");
        String[] serviceNameParts = jidParts[1].split("/");
        String serviceName = serviceNameParts[0];
        DomainBareJid serverName = null;

        try {
            serverName = JidCreate.domainBareFrom(serviceName);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        XMPPTCPConnectionConfiguration.Builder confBuilder = null;
        try {

            InetAddress inetAddress = getInetAddressByName(hostname);
            HostnameVerifier verifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            username = jidParts[0];
            this.password1 = password;
            confBuilder = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword(jidParts[0], password)
                    .setXmppDomain(serverName)
                    .setConnectTimeout(20000)
                    .setHostnameVerifier(verifier)
                    .setHostAddress(inetAddress)
                    .setSecurityMode(SecurityMode.disabled);

            if (serviceNameParts.length > 1) {
                confBuilder.setResource(serviceNameParts[1]);
            } else {
                confBuilder.setResource(Long.toHexString(Double.doubleToLongBits(Math.random())));
            }

        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

//       if (hostname != null) {
//            confBuilder.setHost(hostname);
//        }

        if (port != null) {
            confBuilder.setPort(port);
        }

        if (trustedHosts.contains(hostname) || (hostname == null && trustedHosts.contains(serviceName))){
            confBuilder.setSecurityMode(SecurityMode.disabled);
            TLSUtils.disableHostnameVerificationForTlsCertificates(confBuilder);

            try {
                TLSUtils.acceptAllCertificates(confBuilder);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                e.printStackTrace();
            }
        }


        XMPPTCPConnectionConfiguration connectionConfiguration = confBuilder.build();
        try {
            connection = new XMPPTCPConnection(connectionConfiguration);
        } catch (Exception e) {
            e.printStackTrace();
        }

        connection.setReplyTimeout(10000);
        connection.setUseStreamManagement(true);
        connection.addAsyncStanzaListener(this, new OrFilter(new StanzaTypeFilter(IQ.class), new StanzaTypeFilter(Presence.class)));
        connection.addConnectionListener(this);

        DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(this);
        DeliveryReceiptManager.getInstanceFor(connection).autoAddDeliveryReceiptRequests();

        ChatManager.getInstanceFor(connection).addChatListener(this);
        roster = Roster.getInstanceFor(connection);
        roster.addRosterLoadedListener(this);
        new ReconnectionTask().execute(promise);
    }

    public void joinRoom(String roomJid, String userNickname, String lastMessage, Promise promise) {
        try {
            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
            MultiUserChat muc = manager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));

            logger.log(Level.INFO, "last message: " + lastMessage);
            DiscussionHistory history = new DiscussionHistory();
            Calendar calendar = Calendar.getInstance();
            long val = Long.parseLong(lastMessage);
            calendar.setTimeInMillis(val);
            calendar.add(Calendar.SECOND, 1);
            history.setSince(calendar.getTime());
            logger.log(Level.INFO, "Date is" + calendar.getTime());
            // history.setMaxStanzas(0);

            if (muc.isJoined()) {
                sendOnlinePresence(muc, roomJid, userNickname, calendar.getTime());
            } else {
                muc.join(Resourcepart.fromOrNull(userNickname), "", history, connection.getReplyTimeout());
                muc.addMessageListener(this.groupMessageListner);
            }
            promise.resolve(null);
        } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not join chat room", e);
            promise.reject(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not join chat room", e);
            promise.reject(e.toString());
        }
    }

    public void leaveRoom(String roomJid, Promise promise) {
        MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);

        try {
            MultiUserChat muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));
            muc.leave();
            muc.removeMessageListener(this.groupMessageListner);
            promise.resolve(null);
        } catch (SmackException | InterruptedException | XmppStringprepException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not leave chat room: " + roomJid, e);
            promise.reject(e.toString());
        }
    }

    private void sendOnlinePresence(MultiUserChat muc, String room, String nickname, Date date) {
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setTo(room + "/" + nickname);
        MUCInitialPresence mucInitialPresence = new MUCInitialPresence();
        MUCInitialPresence.History history = new MUCInitialPresence.History();
        history.setSince(date);
        mucInitialPresence.setHistory(history);
        joinPresence.addExtension(mucInitialPresence);
        try {
            connection.sendStanza(joinPresence);
        } catch (Exception e) {}
    }

    public void sendRoomMessage(String roomJid, String text, Promise promise) {
        MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);

        try {
            MultiUserChat muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));
            muc.sendMessage(text);
            promise.resolve(null);
        } catch (SmackException | InterruptedException | XmppStringprepException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not send group message", e);
            promise.reject(e.toString());
        }
    }

    public void sendRoomMessageUpdated(String roomJid, String text, final String messageId, final Promise promise) {
        MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);

        try {
            MultiUserChat muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));
            Message message = muc.createMessage();
            message.setBody(text);
            message.setStanzaId(messageId);

            muc.sendMessage(message);
            connection.addStanzaIdAcknowledgedListener(messageId, new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
                   promise.resolve(null);
                }
            });
        } catch (SmackException | InterruptedException | XmppStringprepException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not send group message", e);
            promise.reject(e.toString());
        }
    }

    @Override
    public void sendMessage(String text, String to, String thread, final Promise promise) {
        String chatIdentifier = (thread == null ? to : thread);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = chatManager.getThreadChat(chatIdentifier);

        try {
            if (chat == null) {
                if (thread == null) {
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), this);
                } else {
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), thread, this);
                }
            }

            Message message = new Message();
            message.setBody(text);
            message.setFrom(connection.getUser());

            chat.sendMessage(message);
            message.setBody(text);
            message.setFrom(connection.getUser());

            connection.addStanzaIdAcknowledgedListener(message.getStanzaId(), new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {
                    promise.resolve(null);
                }
            });

        } catch (SmackException | InterruptedException | XmppStringprepException e) {
            e.printStackTrace();
            promise.reject(e.getMessage());
        }
    }

    @Override
    public void sendMessageUpdated(String text, String to, String thread, final String messageId, final Promise promise) {
        String chatIdentifier = (thread == null ? to : thread);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = chatManager.getThreadChat(chatIdentifier);

        try {
            if (chat == null) {
                if (thread == null){
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), this);
                } else {
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), thread, this);
                }
            }

            Message message = new Message();
            message.setStanzaId(messageId);
            message.setBody(text);
            message.setFrom(connection.getUser());

            chat.sendMessage(message);
            connection.addStanzaIdAcknowledgedListener(messageId, new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
                    promise.resolve(null);
                }
            });

        } catch (SmackException | InterruptedException | XmppStringprepException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not send message", e);
            promise.reject(e.toString());
        }
    }

    private String generateMessageId(){
         Message message = new Message();
         return message.getStanzaId();
    }

    @Override
    public void presence(String to, String type, Promise promise) {
        try {
            connection.sendStanza(new Presence(Presence.Type.fromString(type), type, 1, Presence.Mode.available));
            promise.resolve(null);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not send presence", e);
            promise.reject(e.toString());
        }
    }

    @Override
    public void removeRoster(String to, Promise promise) {
        Roster roster = Roster.getInstanceFor(connection);
        RosterEntry rosterEntry = null;
        try {
            rosterEntry = roster.getEntry(JidCreate.entityBareFrom(to));
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not get roster entry: " + to);
            promise.reject(e.toString());
        }

        if (rosterEntry != null){
            try {
                roster.removeEntry(rosterEntry);
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException | InterruptedException e) {
                e.printStackTrace();
                logger.log(Level.WARNING, "Could not remove roster entry: " + to);
                promise.reject(e.toString());
            }
        }

        promise.resolve(null);
    }

    @Override
    public void createRoasterEntry(String jabberId, String name, Promise promise) {
        Roster roster = Roster.getInstanceFor(connection);
        RosterEntry rosterEntry = null;
        try {
            rosterEntry = roster.getEntry(JidCreate.entityBareFrom(jabberId));
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not get roster entry: " + jabberId);
            promise.reject(e.toString());
        }

        if (rosterEntry == null) {
            try {
               roster.createEntry(JidCreate.entityBareFrom(jabberId), name,null);
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException | InterruptedException | XmppStringprepException e) {
                e.printStackTrace();
                logger.log(Level.WARNING, "Could not remove roster entry: " + jabberId);
                promise.reject(e.toString());
            }
        }
        promise.resolve(null);
    }

    @Override
    public void sendComposingState(String to, String thread, String state, Promise promise) {
        String chatIdentifier = (thread == null ? to : thread);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = chatManager.getThreadChat(chatIdentifier);

        try {
            if (chat == null) {
                if (thread == null){
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), this);
                } else {
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), thread, this);
                }
            }

            Message message = new Message();
            message.setFrom(connection.getUser());
            if (state.equalsIgnoreCase("composing")) {
                ChatStateExtension extension = new ChatStateExtension(ChatState.composing);
                message.addExtension(extension);
            } else {
                ChatStateExtension extension=new ChatStateExtension(ChatState.paused);
                message.addExtension(extension);
            }

            chat.sendMessage(message);

            promise.resolve(null);
        } catch (SmackException | XmppStringprepException | InterruptedException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not send message", e);
            promise.reject(e.toString());
        }
    }

    @Override
    public void requestMessageId() {
        xmppServiceListener.onMessageIdGenerated(generateMessageId());
    }

    @Override
    public void disconnect() {
        connection.disconnect();
        xmppServiceListener.onDisconnected(null);
    }

    @Override
    public void fetchRoster(Promise promise) {
        try {
            roster.reload();
            promise.resolve(null);
        } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not fetch roster", e);
            promise.reject(e.toString());
        }
    }

    @Override
    public void stateChanged(org.jivesoftware.smack.chat2.Chat chat, ChatState state, Message message) {
        String name = chat.getXmppAddressOfChatPartner().asEntityBareJidString();
        String stateData = "";
        if (state == ChatState.composing) {
            stateData = name + " is typing";
        } else {
            stateData = name + " stopped typing";
        }
        logger.log(Level.INFO, "State: " + stateData);
    }

    @Override
    public void onReceiptReceived(Jid fromJid, Jid toJid, String receiptId, Stanza receipt) {
        this.xmppServiceListener.onMessageDelivered(receiptId);
    }

    public class StanzaPacket extends org.jivesoftware.smack.packet.Stanza {
        private String xmlString;

        public StanzaPacket(String xmlString) {
            super();
            this.xmlString = xmlString;
        }

        @Override
        public String toString() {
            return this.xmlString;
        }

        @Override
        public CharSequence toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.append(this.xmlString);
            return xml;
        }
    }

    @Override
    public void sendStanza(String stanza, Promise promise) {
        StanzaPacket packet = new StanzaPacket(stanza);
        try {
            connection.sendStanza(packet);
            promise.resolve(null);
        } catch (SmackException | InterruptedException e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Could not send stanza", e);
            promise.reject(e.toString());
        }
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        chat.addMessageListener(this);
    }

    @Override
    public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
        Log.e("Received stanza is", packet.toString());
        if (packet instanceof IQ) {
            this.xmppServiceListener.onIQ((IQ) packet);
        } else if (packet instanceof Presence) {
            this.xmppServiceListener.onPresenced((Presence) packet);
        } else {
            logger.log(Level.WARNING, "Got a Stanza, of unknown subclass", packet.toXML("").toString());
        }
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        this.xmppServiceListener.onMessageReceived(message);
        logger.log(Level.INFO, "Received a new message", message.toString());
    }

    @Override
    public void onRosterLoaded(Roster roster) {
        this.xmppServiceListener.onRosterReceived(roster);
    }

    @Override
    public void onRosterLoadingFailed(Exception exception) {

    }

    @Override
    public void connected(XMPPConnection connection) {
        this.xmppServiceListener.onConnnected(username, password1);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        this.xmppServiceListener.onAuthenticated(connection.getUser().toString(), password);
    }

    @Override
    public void connectionClosed() {
        logger.log(Level.INFO, "Connection was closed.");
        xmppServiceListener.onDisconnected( null);
        // new ReconnectionTask().execute();
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        logger.log(Level.WARNING, "Connection closed with error", e);
        this.xmppServiceListener.onDisconnected(e);
    }

    class ReconnectionTask extends AsyncTask<Promise,Void,Void> {

        @Override
        protected Void doInBackground(Promise... promises) {
            try {
                ReconnectionManager manager = ReconnectionManager.getInstanceFor(connection);
                manager.setFixedDelay(5);
                manager.enableAutomaticReconnection();
                ReconnectionManager.setEnabledPerDefault(true);
                connection.connect().login();
            } catch (XMPPException | SmackException | IOException | InterruptedException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Could not login for current user", e);
                promises[0].reject(e.toString());
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "Could not login for current user", e);
                if (e instanceof SASLErrorException) {
                    promises[0].reject(((SASLErrorException) e).getSASLFailure().toString());
                } else {
                    promises[0].reject(e.toString());
                }
            }

            promises[0].resolve(null);

            return null;
        }
    }
}
