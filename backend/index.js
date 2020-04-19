const express = require('express');
const app = express();
const http = require('http');
const server = http.createServer(app);
const io = require('socket.io')(server);
const mongodb = require('mongodb');
const shortid = require('shortid');

const triviaApi = require('./trivia-api');


shortid.characters('0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@$');

const client = new mongodb.MongoClient('mongodb://127.0.0.1:27017', { useUnifiedTopology: true });
const DB_NAME = 'trivia';

client.connect(function (err) {
  console.log('Connected successfully to database server');
  const db = client.db(DB_NAME);
  main(db);
});

const games = [];
const users = {};
const sockets = {};

function setupSocketListeners(db, socket) {
  sockets[socket.id] = { socket };

  function getCurrentUser() {
    return sockets[socket.id].user;
  }

  console.log('user connected', socket.id);
  socket.on('disconnect', () => {
    console.log('user disconnected, socketid=', socket.id);
    delete sockets[socket.id];
  });

  socket.on('user:enter', async (user) => {
    console.log('user:enter, user=', user);
    if (!user || !user._id) {
      return;
    }
    let dbUser = await findUserById(db, user._id);
    if (!dbUser) {
      await createUser(db, user);
      dbUser = await findUserById(db, user._id);
    }
    console.log('user:enter, dbuser=', dbUser);
    sockets[socket.id].user = dbUser;
    users[user._id] = dbUser;

    // join this socket to all games this user is already a part of
    const games = await findUserGames(db, user._id);
    games.forEach((game) => {
      const key = `game:${game._id}`;
      console.log(`User ${user._id} joining channel ${key}`);
      socket.join(key);
    });
  });

  socket.on('game:join', async (code) => {
    const currentUser = getCurrentUser();
    function joinError(err) {
      console.log('game:join.error: ' + err);
      socket.emit('game:join.error', err);
    }
    if (!currentUser) {
      joinError('Not logged in');
      return;
    }
    console.log('game:join', code);
    const game = await findGame(db, code);
    if (!game) {
      joinError('Game not found');
      return;
    }
    if (game.players.length === 4) {
      joinError('Max of 4 players already in this game');
      return;
    }
    if (!game.players.some(p => p._id == currentUser._id)) {
      game.players.push(currentUser);
      updateGame(db, game);
      return;
    }
    socket.join('game:' + code);
    socket.emit('game:joined', game._id);
    io.sockets.in('game:' + code).emit('game:player.join', game._id, currentUser);
  });

  socket.on('game:create', async (name) => {
    console.log('game:create', name);
    name = name.trim();
    if (name == "") {
      return;
    }
    const game = await createGame(db, name, sockets[socket.id].user);
    socket.emit('game:created', game._id);
  });

  socket.on('game:fetch', async (id) => {
    console.log('game:fetch', id);
    const game = await findGame(db, id);
    socket.emit('game:fetch.response', JSON.stringify(game));
  });

  socket.on('game:list', async () => {
    const games = await findUserGames(db, getCurrentUser()._id);
    console.log('game list',games);
    socket.emit('game:list.response', JSON.stringify(games));
  });
}

function main(db) {
  app.get('/generate', async function (req, res) {
    const questions = await triviaApi();
    console.log('questions', questions);
    db.collection('questions').insertMany(questions);
    res.send(questions);
  });

  io.on('connection', (socket) => {
    setupSocketListeners(db, socket);
  });

  server.listen(3000, () => {
    console.log('listening on *:3000');
  });
}


function findUserById(db, id) {
  return db.collection('users').findOne({ _id: id });
}

function createUser(db, user) {
  return db.collection('users').insertOne(user);
}

async function createGame(db, name, user) {
  const game = {
    _id: shortid.generate(),
    name,
    ownerId: user._id,
    players: [user],
    status: 'waiting',
  };
  await db.collection('games').insertOne(game);
  return game;
}

async function findGame(db, id) {
  const game = await db.collection('games').findOne({ _id: id });
  return game;
}

function updateGame(db, game) {
  db.collection('games').replaceOne({ _id: game._id }, game);
}

async function findUserGames(db, userId) {
  const games = await db.collection('games').find({ 
    players: {
      $elemMatch: { _id: userId } 
    }
  }).toArray();
  console.log('user games', userId, games);
  return games;
}