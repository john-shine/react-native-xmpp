package com.rnxmpp.service;

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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;


/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */

public class XmppServiceSmackImpl implements XmppService,ChatMessageListener, ChatManagerListener, StanzaListener, ConnectionListener, ChatStateListener, RosterLoadedListener,ReceiptReceivedListener {
    XmppServiceListener xmppServiceListener;
    Logger logger = Logger.getLogger(XmppServiceSmackImpl.class.getName());
    XmppGroupMessageListenerImpl groupMessageListner;

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


    public static InetAddress getInetAddressByName(String name)
    {
        AsyncTask<String, Void, InetAddress> task = new AsyncTask<String, Void, InetAddress>()
        {

            @Override
            protected InetAddress doInBackground(String... params)
            {
                try
                {
                    return InetAddress.getByName(params[0]);
                }
                catch (UnknownHostException e)
                {
                    return null;
                }
            }
        };
        try
        {
            return task.execute(name).get();
        }
        catch (InterruptedException e)
        {
            return null;
        }
        catch (ExecutionException e)
        {
            return null;
        }

    }

    String username ="",password1="";
    @Override
    public void connect(String jid, String password, String authMethod, String hostname, Integer port) {
        final String[] jidParts = jid.split("@");
        String[] serviceNameParts = jidParts[1].split("/");
        String serviceName = serviceNameParts[0];
        DomainBareJid serName=null;

        try {
            serName = JidCreate.domainBareFrom(serviceName);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        XMPPTCPConnectionConfiguration.Builder confBuilder = null;
        try {

            InetAddress inetAddress = getInetAddressByName("mntto.com");
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
                    .setXmppDomain(serName)
                    .setConnectTimeout(20000)
                    .setHostnameVerifier(verifier)

                    .setHostAddress(inetAddress)
                    .setSecurityMode(SecurityMode.disabled);

            if (serviceNameParts.length>1){
                confBuilder.setResource(serviceNameParts[1]);
            } else {
                confBuilder.setResource(Long.toHexString(Double.doubleToLongBits(Math.random())));
            }


        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }


//       if (hostname != null){
//            confBuilder.setHost(hostname);
//        }

        if (port != null){
            confBuilder.setPort(port);
        }

//        if (trustedHosts.contains(hostname) || (hostname == null && trustedHosts.contains(serviceName))){
//            confBuilder.setSecurityMode(SecurityMode.disabled);
//              TLSUtils.disableHostnameVerificationForTlsCertificates(confBuilder);
//              //TLSUtils.disableHostnameVerificationForTlsCertificicates(confBuilder);
//            try {
//                TLSUtils.acceptAllCertificates(confBuilder);
//            } catch (NoSuchAlgorithmException | KeyManagementException e) {
//                e.printStackTrace();
//            }
//        }


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
        new ReconnectionTask().execute();
    }




    public void joinRoom(String roomJid, String userNickname,String lastMessage) {

        if (connection != null) {
            MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
            try {
                MultiUserChat muc = manager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));
                try {
                    Log.e("Date is", lastMessage);
                    DiscussionHistory history = new DiscussionHistory();
                    Calendar c = Calendar.getInstance();
                    long val = Long.parseLong(lastMessage);
                    c.setTimeInMillis(val);
                    c.add(Calendar.SECOND, 1);
                    history.setSince(c.getTime());
                    Log.e("Date is", "" + c.getTime());
                    //history.setMaxStanzas(0);

                    if (muc.isJoined()) {
                        sendOnlinePresence(muc, roomJid, userNickname, c.getTime());
                    } else {
                        muc.join(Resourcepart.fromOrNull(userNickname), "", history, connection.getReplyTimeout());
                        groupMessageListner = new XmppGroupMessageListenerImpl(this.xmppServiceListener, logger);
                        muc.addMessageListener(groupMessageListner);
                    }
                } catch (SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                    logger.log(Level.WARNING, "Could not join chat room", e);
                }
            }
            catch (Exception e){}
        }
    }


    private void sendOnlinePresence(MultiUserChat muc,String room,String nickname,Date date ){
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setTo(room + "/" + nickname);
        MUCInitialPresence mucInitialPresence = new MUCInitialPresence();
        MUCInitialPresence.History history = new MUCInitialPresence.History();
        history.setSince(date);
        mucInitialPresence.setHistory(history);
        joinPresence.addExtension(mucInitialPresence);
        try {
            connection.sendStanza(joinPresence);
        }
        catch (Exception e){}
    }


    public void sendRoomMessage(String roomJid, String text) {

        MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);

        try {
            MultiUserChat muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));
            muc.sendMessage(text);
        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not send group message", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    public void sendRoomMessageUpdated(String roomJid, String text,String messageId) {

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
                   xmppServiceListener.onMessageSent(packet.getStanzaId());
                }
            });

        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not send group message", e);
            xmppServiceListener.onDisconnect(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }


    public void leaveRoom(String roomJid) {
        MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(connection);


        try {
            MultiUserChat muc = mucManager.getMultiUserChat(JidCreate.entityBareFrom(roomJid));
            muc.leave();
            muc.removeMessageListener(groupMessageListner);
        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not leave chat room", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void message(String text, String to, String thread) {
        String chatIdentifier = (thread == null ? to : thread);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = chatManager.getThreadChat(chatIdentifier);

        try {

            if (chat == null) {
                if (thread == null){
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), this);

                }else{
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), thread, this);
                }
            }

            Message message=new Message();
            message.setBody(text);
            message.setFrom(connection.getUser());

            chat.sendMessage(message);
            this.xmppServiceListener.onMessageCreated(message);

            connection.addStanzaIdAcknowledgedListener(message.getStanzaId(), new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {

                }
            });

            //chat.sendMessage(text);


        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not send message", e);
            xmppServiceListener.onDisconnect(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageUpdated(String text, String to, String thread, String messageId) {
        String chatIdentifier = (thread == null ? to : thread);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = chatManager.getThreadChat(chatIdentifier);

        try {
            if (chat == null) {
                if (thread == null){
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), this);

                }else{
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), thread, this);
                }
            }

            Message message=new Message();
            message.setStanzaId(messageId);
            message.setBody(text);
            message.setFrom(connection.getUser());

            chat.sendMessage(message);
            connection.addStanzaIdAcknowledgedListener(messageId, new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
                    xmppServiceListener.onMessageSent(packet.getStanzaId());
                }
            });


        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not send message", e);
            xmppServiceListener.onDisconnect(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    private String generateMessage(){
         Message message = new Message();
         return message.getStanzaId();
    }

    @Override
    public void presence(String to, String type) {
        try {
            if(connection!=null)
            connection.sendStanza(new Presence(Presence.Type.fromString(type), type, 1, Presence.Mode.available));
        } catch (SmackException.NotConnectedException e) {
            logger.log(Level.WARNING, "Could not send presence", e);
        //    xmppServiceListener.onDisconnect(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeRoster(String to) {
        Roster roster = Roster.getInstanceFor(connection);
        RosterEntry rosterEntry =null;
        try {
            rosterEntry = roster.getEntry(JidCreate.entityBareFrom(to));
        }
        catch (Exception e){}

        if (rosterEntry != null){
            try {
                roster.removeEntry(rosterEntry);
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                logger.log(Level.WARNING, "Could not remove roster entry: " + to);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void createRoasterEntry(String jabberId, String name) {
        Roster roster = Roster.getInstanceFor(connection);
        RosterEntry rosterEntry =null;
        try {
            rosterEntry = roster.getEntry(JidCreate.entityBareFrom(jabberId));
        }
        catch (Exception e){}

        if (rosterEntry == null){
            try {
               roster.createEntry(JidCreate.entityBareFrom(jabberId),name,null);
            } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
                logger.log(Level.WARNING, "Could not remove roster entry: ");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendComposingState(String to, String thread,String state) {
        String chatIdentifier = (thread == null ? to : thread);

        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = chatManager.getThreadChat(chatIdentifier);

        try {

            if (chat == null) {
                if (thread == null){
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), this);

                }else{
                    chat = chatManager.createChat(JidCreate.entityBareFrom(to), thread, this);
                }
            }

            Message message=new Message();
            message.setFrom(connection.getUser());
            if(state.equalsIgnoreCase("composing")){
                ChatStateExtension extension=new ChatStateExtension(ChatState.composing);
                message.addExtension(extension);
            }
            else {
                ChatStateExtension extension=new ChatStateExtension(ChatState.paused);
                message.addExtension(extension);
            }

            chat.sendMessage(message);

           // this.xmppServiceListener.onMessageCreated(message);
        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not send message", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void requestMessageId() {
      xmppServiceListener.onMessageIdGenerated(generateMessage());
    }


    @Override
    public void disconnect() {
        connection.disconnect();
        xmppServiceListener.onDisconnect(null);
    }

    @Override
    public void fetchRoster() {
        try {
            roster.reload();
        } catch (SmackException.NotLoggedInException | SmackException.NotConnectedException e) {
            logger.log(Level.WARNING, "Could not fetch roster", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void stateChanged(org.jivesoftware.smack.chat2.Chat chat, ChatState state, Message message) {
        String name=chat.getXmppAddressOfChatPartner().asEntityBareJidString();
        String stateData="";
        if(state==ChatState.composing){
            stateData=name+" is typing";
        }
        else {
            stateData=name+" stopped typing";
        }
        Log.e("State",name);
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
    public void sendStanza(String stanza) {
        StanzaPacket packet = new StanzaPacket(stanza);
        try {
            connection.sendStanza(packet);
        } catch (SmackException e) {
            logger.log(Level.WARNING, "Could not send stanza", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        chat.addMessageListener(this);
    }

    @Override
    public void processStanza(Stanza packet) throws SmackException.NotConnectedException {
        Log.e("Received stanza is", packet.toString());
        if (packet instanceof IQ){
            this.xmppServiceListener.onIQ((IQ) packet);
        }else if (packet instanceof Presence){
            this.xmppServiceListener.onPresence((Presence) packet);
        }else{
            logger.log(Level.WARNING, "Got a Stanza, of unknown subclass", packet.toXML("").toString());
        }


    }

    @Override
    public void connected(XMPPConnection connection) {

        this.xmppServiceListener.onConnnect(username, password1);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        this.xmppServiceListener.onLogin(connection.getUser().toString(), password);

    }

    @Override
    public void processMessage(Chat chat, Message message) {
        this.xmppServiceListener.onMessage(message);
        // logger.log(Level.INFO, "Received a new message", message.toString());
    }

    @Override
    public void onRosterLoaded(Roster roster) {
        this.xmppServiceListener.onRosterReceived(roster);
    }

    @Override
    public void onRosterLoadingFailed(Exception exception) {

    }

    @Override
    public void connectionClosedOnError(Exception e) {
        this.xmppServiceListener.onDisconnect(e);

    }

    @Override
    public void connectionClosed() {
        logger.log(Level.INFO, "Connection was closed.");
        xmppServiceListener.onDisconnect( null);
       // new ReconnectionTask().execute();
    }


    class ReconnectionTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                ReconnectionManager manager = ReconnectionManager.getInstanceFor(connection);
                manager.setFixedDelay(12);
                manager.enableAutomaticReconnection();
                ReconnectionManager.setEnabledPerDefault(true);




            } catch (Exception e) {
                // logger.log(Level.SEVERE, "Could not login for user " + jidParts[0], e);
                if (e instanceof SASLErrorException){
                    XmppServiceSmackImpl.this.xmppServiceListener.onLoginError(((SASLErrorException) e).getSASLFailure().toString());
                }else{
                    XmppServiceSmackImpl.this.xmppServiceListener.onError(e);
                }

            }

            try {
                connection.connect().login();
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
