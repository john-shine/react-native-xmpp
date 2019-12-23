package com.rnxmpp.service;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;

/**
 * Created by Kristian Frølund on 7/19/16.
 * Copyright (c) 2016. Teletronics. All rights reserved
 */

public interface XmppServiceListener {
    void onMessageReceived(Message message);
    void onMessageDelivered(String messageId);
    void onGroupMessageReceived(Message message);
    void onRosterReceived(Roster roster);
    void onIQ(IQ iq);
    void onPresenced(Presence presence);
    void onConnnected(String username, String password);
    void onAuthenticated(String username, String password);
    void onDisconnected(Exception e);
}
