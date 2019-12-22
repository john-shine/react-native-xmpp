'use strict';
var React = require('react-native');
var {NativeAppEventEmitter, NativeModules} = React;
var RNXMPP = NativeModules.RNXMPP;

var map = {
    'connected': 'RNXMPPConnected',
    'authenticated': 'RNXMPPAuthenticated',
    'disconnected': 'RNXMPPDisconnected',
    'IQ': 'RNXMPPIQ',
    'roster': 'RNXMPPRoster',
    'presence': 'RNXMPPPresenced',
    'messageIDGenerated': 'RNXMPPMessageIdGenerated',
    'messageReceived' : 'RNXMPPMessageReceived',
    'messageDelivered':'RNXMPPMessageDelivered',
    'typingStatus':'RNXMPPTypingStatus'
}

const LOG = (message) => {
  if (__DEV__) {
    console.log('react-native-xmpp: ' + message);
  }
}

class XMPP {
    PLAIN = RNXMPP.PLAIN;
    SCRAM = RNXMPP.SCRAMSHA1;
    MD5 = RNXMPP.DigestMD5;
    
    constructor() {
        this.isConnected = false;
        this.isLogged = false;
        this.listeners = [];
        for (let type in map) {
            if (type === 'connected') {
                var callback = (username, password) => {
                    LOG("Connected");
                    this.isConnected = true;
                }
            } else if (type == 'disconnected') {
                var callback = () => {
                    LOG("Disconnected, error: " + error);
                    this.isConnected = false;
                    this.isLogged = false;
                }
            } else {
                var callback = () => {
                    LOG(type);
                };
            }
            let listener = NativeAppEventEmitter.addListener(map[type], callback.bind(this));
            this.listeners.push(listener)
        }
    }

    on(type, callback) {
        if (map[type]) {
            const listener = NativeAppEventEmitter.addListener(map[type], callback);
            this.listeners.push(listener);
            return listener;
        } else {
            throw "No registered type: " + type;
        }
    }

    removeListener(type) {
        if (map[type]) {
            for (let i = 0; i < this.listeners.length; i++) {
                let listener = this.listeners[i];
                if (listener.eventType === map[type]) {
                    listener.remove();
                    let index = this.listeners.indexOf(listener);
                    if (index > -1) {
                        this.listeners.splice(index, 1);
                    }
                    LOG(`Event listener of type "${type}" removed`);
                }
            }
        }
    }

    removeListeners() {
        for (let i = 0; i < this.listeners.length; i++) {
            this.listeners[i].remove();
        }

        this.listeners = [];
        
        LOG('All event listeners removed');
    }

    trustHosts(hosts) {
        return React.NativeModules.RNXMPP.trustHosts(hosts);
    }

    connect(username, password, auth = RNXMPP.SCRAMSHA1, hostname = null, port = 5222) {
        if (!hostname) {
            hostname = (username + '@/').split('@')[1].split('/')[0];
        }
        return React.NativeModules.RNXMPP.connect(username, password, auth, hostname, port);
    }

    sendMessage(text, user, thread = null) {
        LOG(`Message: "${text}" being sent to user: ${user}`);
        return React.NativeModules.RNXMPP.sendMessage(text, user, thread);
    }

    sendMessageUpdated(text, user, messageId, thread=null) {
        LOG(`Message: "${text}" being sent to user: ${user}`);
        return React.NativeModules.RNXMPP.sendMessageUpdated(text, user, thread,messageId);
    }

    requestMessageid() {
        return React.NativeModules.RNXMPP.requestMessageId();
    }

    sendStanza(stanza) {
        return RNXMPP.sendStanza(stanza);
    }

    fetchRoster() {
        return RNXMPP.fetchRoster();
    }

    presence(to, type) {
        return React.NativeModules.RNXMPP.presence(to, type);
    }

    removeFromRoster(to) {
        return React.NativeModules.RNXMPP.removeRoster(to);
    }

    createRosterEntry(to, name) {
        return React.NativeModules.RNXMPP.createRoasterEntry(to,name);
    }

    disconnect() {
        if (this.isConnected) {
            return React.NativeModules.RNXMPP.disconnect();
        }
    }
    disconnectAfterSending() {
        if (this.isConnected) {
            return React.NativeModules.RNXMPP.disconnectAfterSending();
        }
    }

    joinRoom(roomJID, nickname, lastMessageTimeStamp) {
        return React.NativeModules.RNXMPP.joinRoom(roomJID, nickname,lastMessageTimeStamp);
    }

    sendRoomMessage(message, roomJID) {
        return React.NativeModules.RNXMPP.sendRoomMessage(message, roomJID);
    }

    sendRoomMessageUpdated(message, roomJID, messageId) {
        return React.NativeModules.RNXMPP.sendRoomMessageUpdated(message, roomJID,messageId);
    }

    leaveRoom(roomJID) {
        return React.NativeModules.RNXMPP.leaveRoom(roomJID);
    }

    sendComposingState(user, thread = null, state) {
        return React.NativeModules.RNXMPP.sendComposingState(user,thread,state);
    }
}

module.exports = new XMPP();
