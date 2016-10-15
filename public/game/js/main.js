"use strict";

var game;
var questions;

$('#splash').click(function (event) {
    $.ajax("/game/start", {
        success: function (data) {
            game = data;

            $('#start').hide();
            var $main = $('#main');
            $main.children('.name').text("Game Id: " + game.gameId);
            $main.find('.age > .value').text(game.age);
            $main.show();


            $.ajax('/game/' + game.gameId + "/questions", {
                success: function (data) {
                    questions = data;

                    $main.find('.topicInstructions').show();

                    var $options = $main.find('.options');
                    $options.empty();

                    questions.forEach(function (question) {
                        $options.append('<li><a href="#"><img scr="" />' + question.text + '</a></li>')
                    });
                }
            });
        }
    });

    event.preventDefault();
});
