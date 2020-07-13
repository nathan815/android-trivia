# android-trivia

Multiplayer trivia game for Android

- Server side: NodeJS, socket.io library, MongoDB 
- Client side: Java, Android, [socket.io java client](https://github.com/socketio/socket.io-client-java),
and [Gson](https://github.com/google/gson)

<img src="https://i.imgur.com/xdE1PMt.png" width="100%" />

## Prerequisites
- Android Studio
- Java 8
- NodeJS 12
- MongoDB

## Installing

1. `git clone git@github.com:nathan815/android-trivia.git && cd android-trivia`
2. Backend
   1. `cd backend`
   2. `cp .env.example .env`
   3. `npm install`
   4. Run the server with `node index.js`. A MongoDB server needs to be running. Specify info for it in .env file.
3. Android
   1. Open `android` directory in Android Studio
