# SecureTime (Android Roughtime library)

Library for easy usage of [Roughtime protocol](https://roughtime.googlesource.com/roughtime) on Android (with Rx). This is based on Java library [Nearenough by int08h](https://github.com/int08h/nearenough) - kudos!

# Why Roughtime, instead of NTP?

* Secured - responses are signed by server, contains nonce send by client.
* Scalable - much better handling on backend (signing and batches), compact payload of network packages.
* Doesn't allow amplification of DDOS attacts.

# Why not?

* It is not millisecond accurate - accuracy is around 1 second.

[Much more details on Roughtime](https://int08h.com/post/to-catch-a-lying-timeserver/)

# Android App

<p align="center">
  <img src="https://github.com/tajchert/securetime/blob/master/assets/android_app_screen.png?raw=true" width="350" title="Android App">
  <img src="https://github.com/tajchert/securetime/blob/master/assets/roughtime.png?raw=true" alt="Command line">
</p>
Known issue: some requests fail to get response in 5 sec. timeframe. Extending it doesn't seem to solve problem, might be a server issue.

# Roughtime servers

App is hooked to `roughtime.cloudflare.com:2002`, second known running server is `roughtime.int08h.com:2002`.

* roughtime.cloudflare.com:2002
  * `Base64 = gD63hSj3ScS+wuOeGrubXlq35N1c5Lby/S+T7MNTjxo=`
  * `hex = 803eb78528f749c4bec2e39e1abb9b5e5ab7e4dd5ce4b6f2fd2f93ecc3538f1a`
* roughtime.int08h.com:2002
  * `Base64 = AW5uAoTSTDfG5NfY1bTh08GUnOqlRb+HVhbJ3ODJvsE=`
  * `hex = 016e6e0284d24c37c6e4d7d8d5b4e1d3c1949ceaa545bf875616c9dce0c9bec1`

# Roadmap for this library (help wanted)
* Separate Rx extension to separate library module
* Retry mechanism
* Allow using multiple servers on one `getTime()`
* Utilize same interface as [TrueTime](https://github.com/instacart/truetime-android) library to allow for easy switch.
* In sample app allow for server picker (Cloudflare/int08h) or both servers
* Debug issue with requests that are not served in 5-10 seconds window.
