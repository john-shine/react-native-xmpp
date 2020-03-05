# react-native-xmpp

An XMPP library for React Native. Android platform are tested.


## Example

view demo on repository: https://github.com/john-shine/react-native-xmpp-demo/

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
