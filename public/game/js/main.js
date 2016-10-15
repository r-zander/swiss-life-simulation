"use strict";

var game;
var questions;
var $main = $('#main');
var $death = $('#death');

function startGame(event) {
    $death.hide();

    $.ajax("/game/start", {
        success: function (data) {
            game = data;

            $('#start').hide();
            $main.children('.name').text("");
            $main.show();

            updateMainScreen();
        }
    });

    event.preventDefault();
}
$('#splash').click(startGame);

$death.children('.reincarnate').click(startGame);

function updateMainScreen() {
    if (game.name !== undefined) {
        $main.find('.name').text(game.name);
    }
    $main.find('.age > .value').text(game.age);
    $main.find('.satisfaction').attr('src', 'img/satisfaction/' + game.satisfaction + '.svg');
    $main.find('.money').attr('src', 'img/money/' + game.money + '.svg');

    $.ajax('/game/' + game.gameId + "/questions", {
        success: function (data) {
            questions = data;

            $main.find('.topicInstructions').show();

            var $options = $main.find('.options');
            $options.addClass('questions');
            $options.empty();

            questions.forEach(function (question, index) {
                var link = $('<a href="#">' + question.preview + '</a>');
                link.click(function (event) {
                    $main.find('.topicInstructions').hide();
                    $main.find('.question').text(question.text).show();
                    $main.addClass('question' + (index + 1));

                    $options.removeClass('questions');
                    $options.empty();

                    question.answers.forEach(function (answer) {
                        var link = $('<a href="#">' + answer.text + '</a>');
                        link.click(function () {
                            $main.removeClass('question1');
                            $main.removeClass('question2');
                            $main.removeClass('question3');

                            $main.find('.topicInstructions').show();
                            $main.find('.question').hide();

                            $.ajax('/game/' + game.gameId + "/answers/" + answer.id, {
                                success: function (data) {
                                    game = data;

                                    if (game.endOfGame !== undefined){
                                        $main.hide();
                                        $death.show();
                                        $death.find('.age').text(game.age);
                                        $death.find('.score > .value').text(game.endOfGame.highScore);
                                        $death.find('.causeOfDeath').text(game.endOfGame.causeOfDeath);
                                        $death.find('h1 .value').text(game.endOfGame.probability);
                                    }
                                    updateMainScreen();
                                }
                            });
                        });
                        var item = $('<li>');
                        item.append(link);
                        $options.append(item)
                    });

                    event.preventDefault();

                });
                var item = $('<li>');
                item.append(link);
                $options.append(item)
            });
        }
    });
}
