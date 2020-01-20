# react-native-xmpp

An XMPP library for React Native. Android platform are tested.


## Example

```js
import XMPP = from 'react-native-xmpp';

// events callbacks
XMPP.on("messageReceived", message =>
  console.log("MESSAGE:" + JSON.stringify(message))
);
XMPP.on("messageDelivered", message =>
  console.log("MESSAGE:" + JSON.stringify(message))
);
XMPP.on("IQ", message => console.log("IQ:" + JSON.stringify(message)));
XMPP.on("roster", roster => console.log("roster received:" + JSON.stringify(roster)));
XMPP.on("presence", message =>
  console.log("PRESENCE:" + JSON.stringify(message))
);
XMPP.on("typingStatus", status =>
  console.log("user is typing:" + JSON.stringify(status))
);

XMPP.on("connected", message => console.log("CONNECTED!"));
XMPP.on("authenticated", message => console.log("LOGGED!"));
XMPP.on("disconnected", message => console.log("DISCONNECTED!"));

// trustHosts (ignore self-signed SSL issues). Warning: Do not use this in production (security will be compromised).
XMPP.trustHosts(["chat.xxxxx.com"]);

// connect && login in xmpp server
XMPP.connect(username, password, auth = RNXMPP.SCRAMSHA1, hostname = null, port = 5222).then(()=> {
  console.log('login success.');
}).catch(err => {
  console.log('login failure: ', err);
});

// send message
XMPP.sendMessage("Hello world!", TO_JID).then(()=> {
  console.log('send message to ' + TO_JID + ' success.');
}).catch(err => {
  console.log('send message to ' + TO_JID + ' failure: ', err);
});

// join room(s)
XMPP.joinRoom(ROOM_JID, ROOM_NICKNAME).then(()=> {
  console.log('join room success.');
}).catch(err => {
  console.log('join room failure: ', err);
});

// send message to room(s)
XMPP.sendRoomMessage(ROOM_JID, "Hello room!").then(()=> {
  console.log('send room message success.');
}).catch(err => {
  console.log('send room message failure: ', err);
});

// leave room(s)
XMPP.leaveRoom(ROOMJID).then(()=> {
   console.log('leave room success.');
 }).catch(err => {
   console.log('leave room failure: ', err);
 });

// disconnect
XMPP.disconnect();

// remove all event listeners registered (recommended on componentWillUnmount)
XMPP.removeListeners();

// remove specific event listener
// EVENT_TYPE can be: 'connected', 'authenticated', 'disconnected', 'IQ', 'roster', 'presence', 'messageReceived', 'messageDelivered', 'typingStatus'
XMPP.removeListener(EVENT_TYPE);
```

## Getting started

1. `yarn add react-native-xmpp`

### iOS (untested)

Please use CocoaPods

2. Install latest XMPPFramework:
   https://github.com/robbiehanson/XMPPFramework
   `pod 'XMPPFramework', :git => 'https://github.com/robbiehanson/XMPPFramework.git', :branch => 'master'`

3. Add this package pod:
   `pod 'RNXMPP', :path => '../node_modules/react-native-xmpp'`

If you have problems with latest 4.0 XMPPFramework and/or XCode 9.3, you may use old one with forked KissXML:
`pod 'XMPPFramework', '~> 3.7.0'`
`pod 'KissXML', :git => "https://github.com/aksonov/KissXML.git", :branch => '5.1.4'`

### Android

`react-native link react-native-xmpp`

If it doesn't link the react-native-xmpp correct:

**android/settings.gradle**

```gradle
include ':react-native-xmpp'
project(':react-native-xmpp').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-xmpp/android')
```

**android/app/build.gradle**

```gradle
dependencies {
   ...
   compile project(':react-native-xmpp')
}
```

**MainApplication.java**

On top, where imports are:

```java
import com.rnxmpp.RNXMPPPackage;
```

Add the `ReactVideoPackage` class to your list of exported packages.

```java
@Override
protected List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new RNXMPPPackage()
    );
}
```
