const app = require('express')();
const http = require('http').createServer(app);
const io = require('socket.io')(http);

app.get('/', (req, res) => {
  res.send('<h1>Hello world</h1>');
});

io.on('connection', (socket) => {
  console.log('a user connected');
  socket.emit('test');
  socket.on('disconnect', () => {
    console.log('user disconnected');
  });
});

io.on('test', () => {
  console.log('hi')
});

http.listen(3000, () => {
  console.log('listening on *:3000');
});

