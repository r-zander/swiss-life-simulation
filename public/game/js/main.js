"use strict";

var game;
var questions;
var $main = $('#main');

$('#splash').click(function (event) {
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
});

function updateMainScreen() {
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
                var link = $('<a href="#"><img scr="" />' + question.preview + '</a>');
                link.click(function () {
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

                                    // TODO check for Death
                                    updateMainScreen();
                                }
                            });
                        });
                        var item = $('<li>');
                        item.append(link);
                        $options.append(item)
                    });

                });
                var item = $('<li>');
                item.append(link);
                $options.append(item)
            });
        }
    });
}
