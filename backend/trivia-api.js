const fetch = require('node-fetch');

module.exports = async function (amount = 10, difficulty = 'easy') {
  const url = `https://opentdb.com/api.php?amount=${amount}&difficulty=${difficulty}&type=multiple`;
  console.log('Fetching from', url);
  const data = await fetch(url).then(response => response.json());
  return data.results.map(question => {
    const { 
      category, difficulty, question: questionText, 
      incorrect_answers, correct_answer
    } = question;
    const answers = [...incorrect_answers, correct_answer];
    return {
      category,
      difficulty,
      points: getPoints(difficulty),
      question: questionText,
      answers,
      correctIndex: answers.indexOf(question.correct_answer),
    };
  });
}

function getPoints(difficulty) {
  if(difficulty === 'easy') return 10;
  if(difficulty === 'medium') return 15;
  if(difficulty === 'hard') return 25;
}