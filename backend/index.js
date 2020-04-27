const express = require('express');
const app = express();
const http = require('http');
const server = http.createServer(app);
const io = require('socket.io')(server);
const mongodb = require('mongodb');
const shortid = require('shortid');

const triviaApi = require('./trivia-api');

require('dotenv').config();
shortid.characters('0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@$');

const client = new mongodb.MongoClient(
  `mongodb://${process.env.DB_HOST}:${process.env.DB_PORT}`, 
  { useUnifiedTopology: true }
);

client.connect(function (err) {
  console.log('Connected successfully to database server');
  const db = client.db(process.env.DB_NAME);
  main(db);
});

function main(db) {
  app.get('/generate', async function (req, res) {
    const questions = await triviaApi(50);
    console.log('questions', questions);
    db.collection('questions').insertMany(questions);
    res.send(questions);
  });

  io.on('connection', (socket) => {
    setupSocketListeners(db, socket);
  });

  server.listen(process.env.APP_PORT, () => {
    console.log(`Socket server listening on *:${process.env.APP_PORT}`);
  });
}

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

    // join this socket to all games this user is already a part of
    const games = await findUserGames(db, user._id);
    games.forEach((game) => {
      const key = `game:${game._id}`;
      console.log(`User ${user._id} joining channel ${key}`);
      socket.join(key);
    });
  });

  socket.on('game:join', async (code, callback) => {
    function joinError(err) {
      console.log('game:join error: ' + err);
      callback(false, err);
    }
    
    const currentUser = getCurrentUser();
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
    if (game.status !== 'waiting') {
      joinError('This game has already started!');
      return;
    }
    if (game.players.length === 4) {
      joinError('Max of 4 players already in this game');
      return;
    }

    if (!game.players[currentUser._id]) {
      game.players[currentUser._id] = currentUser;
      updateGame(db, game);
    }

    socket.join('game:' + code);

    // Notify everyone in the game that this player joined
    io.sockets.in('game:' + code).emit('game:player.join', game._id,
      JSON.stringify(currentUser));

    callback(true, null);
  });

  socket.on('game:create', async (name, callback) => {
    console.log('game:create', name);
    name = name.trim();
    if (name == "") {
      return;
    }
    const game = await createGame(db, name, sockets[socket.id].user);
    socket.join('game:' + game._id);
    callback(game._id);
  });

  socket.on('game:fetch', async (id, callback) => {
    console.log('game:fetch', id);
    const game = await findGame(db, id);
    callback(JSON.stringify(game));
  });

  socket.on('game:submitAnswer', async (gameId, answerIndex) => {
    console.log('game:submitAnswer', gameId, answerIndex);
    const game = await findGame(db, gameId);
    const currentQuestion = game.questions[game.questions.length - 1];
    const currentUser = getCurrentUser();
    if (!game) {
      return;
    }
    if (!game.players[currentUser._id]) {
      return;
    }
    if (answerIndex > 3) {
      return;
    }

    const responses = game.playerResponses;
    const currentPlayerResponses = responses[currentUser._id] || [];

    if (currentPlayerResponses.length < game.questions.length) {
      currentPlayerResponses.push(answerIndex);
      game.playerResponses[currentUser._id] = currentPlayerResponses;
      updateGame(db, game);
    }

    if(typeof callback === 'function') {
      callback(answerIndex === currentQuestion.correctIndex);
    }

    io.sockets.in('game:' + game._id).emit('game:playerAnswer', {
      gameId: game._id,
      userId: currentUser._id,
      responses: currentPlayerResponses
    });

    if (canAskAnotherQuestion(game)) {
      await askNextQuestion(db, game);
    }
    checkAndUpdateGameStatus(db, game);
  });

  socket.on('game:start', async (id) => {
    console.log('game:start', id);
    const game = await findGame(db, id);

    if (!game) {
      console.log('game:start error: game doesnt exist', id);
      return;
    }

    if (game.status == 'inplay') {
      console.log('game:start error: game already in play', id);
      return;
    }

    // only allow game owner to start game
    if (getCurrentUser()._id != game.ownerId) {
      console.log('game:start error: non-owner tried to start');
      return;
    }

    game.status = 'inplay';
    updateGame(db, game);
    io.sockets.in('game:' + id).emit('game:starting', game._id);
    await askNextQuestion(db, game);
  });

  socket.on('game:list', async (callback) => {
    console.log('game:list', getCurrentUser());
    const user = getCurrentUser();
    if (!user) {
      return;
    }
    const games = await findUserGames(db, user._id);
    callback(JSON.stringify(games));
  });

  async function askNextQuestion(db, game) {
    if (isGameOver(game)) {
      return;
    }
    const question = await findRandomQuestion(db, game);
    console.log('askQuestion', question);
    game.questions.push(question);
    await updateGame(db, game);
    io.sockets.in('game:' + game._id).emit('game:question', game._id, JSON.stringify(question));
    return game;
  }

  async function endGame(db, game) {
    if (game.status === 'done') {
      return;
    }
    game.status = 'done';
    updateGame(db, game);
    io.sockets.in('game:' + game._id).emit('game:done', game._id);
  }

  function checkAndUpdateGameStatus(db, game) {
    if (isGameOver(game)) {
      endGame(db, game);
    }
  }

  function hasEveryPlayerAnswered(game) {
    if (game.questions.length === 0) {
      return true;
    }
    const playerHasAnswered = (userId) => game.playerResponses[userId] &&
      game.playerResponses[userId].length == game.questions.length;
    return Object.keys(game.players).every(playerHasAnswered);
  }

  function canAskAnotherQuestion(game) {
    return hasEveryPlayerAnswered(game) && !isGameOver(game);
  }

  function isGameOver(game) {
    return game.status === 'done' || game.questions.length >= 10 && hasEveryPlayerAnswered(game);
  }

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
    status: 'waiting',
    ownerId: user._id,
    players: { [user._id]: user },
    playerResponses: {},
    questions: [],
    createdAt: new Date(),
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
    [`players.${userId}`]: {
      $exists: true
    }
  }).toArray();
  console.log('findUserGames userId=', userId, 'games=', games.map(g => ({ id: g._id, name: g.name })));
  return games;
}

function findRandomQuestion(db, game) {
  const questionIdsUsed = game.questions.map(q => q._id);
  const cursor = db.collection('questions').aggregate([
    { $match: { _id: { $nin: questionIdsUsed } } },
    { $sample: { size: 1 } }
  ]);
  return cursor.hasNext() ? cursor.next() : null;
}