
/**
 * Module dependencies.
 */

var express = require('express')
  , http = require('http')
  , Mongolian = require('mongolian')
  , path = require('path');

var app = express();
var server = new Mongolian;
var db = server.db('baseballGame');
var talk = db.collection('talk');
var correct_list = db.collection('correct');

"use strict";

Array.prototype.remove = function(from, to) {
  var rest = this.slice((to || from) + 1 || this.length);
  this.length = from < 0 ? this.length + from : from;
  return this.push.apply(this, rest);
};

// all environments
app.set('port', process.env.PORT || 7000);
app.use(express.favicon());
app.use(express.bodyParser());
app.use(express.logger());
app.use(express.cookieParser('BaseBallGame'));
app.use(express.session());
app.use(app.router);

app.use(function(err, req, res, next) {
	console.error(err.stack);
	res.send({'message' : 'fail'});
});
var reg = /\ /gmi;
var names = [];

function checkValid(req) {
	return (req.session.name && req.param('time'));
} 

function clearSpace(msg) {
	var ret = '';
	for(var i = 0; i < msg.length; i++) {
		if(msg[i] == '') 
			continue;
		else
			ret += msg[i];
	}

	return ret;
}
var c_array = [];

function makeNewAnswer(){
	var answer = Math.floor((Math.random()*1000));
	if(answer < 100) {
		return makeNewAnswer();
	}
	var c_f = parseInt(answer / 100);
	var c_s = parseInt((answer % 100) / 10);
	var c_t = parseInt((answer % 100) % 10);
	if(c_f == c_s || c_t == c_s || c_t == c_f) {
		c_f = null;
		c_s = null;
		c_t = null;
		answer = null;
		c_array = null;

		return makeNewAnswer();
	}

	console.log('new Answer : ' + answer);
	c_array = null;
	c_array = [c_f, c_s ,c_t];

	c_f = null;
	c_s = null;
	c_t = null;
	answer = null;
}

makeNewAnswer();

var success_msg = {message : 'success'};
var fail_msg = {message : 'fail'};

function getTime() {
	return Math.floor(Date.now() / 10);
}

app.get('/new', function(req, res) {
	talk.find({}, {'_id' : 0}).sort({time : -1}).limit(30).toArray(function(err, array){
		if(err) {
			res.send(fail_msg);		
		}
		else {
			res.send({message : 'success', msg : array, time : getTime()});
		}
		
	});
});

app.get('/', function(req, res){
	req.send('');
})

app.get('/setName', function(req, res){
	if(!req.param('name')) {
		res.send(fail_msg);
		return;
	}
	var name = req.param('name').toString();

	if(name.length > 0) {
		if(names.indexOf(name) == -1) {
			req.session.name = clearSpace(name);
			names.push(name);

			res.send(success_msg);
		}
		else {
			req.send({'message' : 'fail', 'reason' : 'name exist'});
		}
	}
	else {
		req.send(fail_msg);
	}
});

app.get('/leave', function(req, res) {
	var idx = names.indexOf(req.session.name);

	if(idx > 0) {
		names.remove(idx);
		res.send(success_msg);
	}
	else {
		res.send(fail_msg);
	}

	req.session.destroy();
});

app.get('/write', function(req, res) {
	if(!checkValid(req)) {
		res.send(fail_msg);
		return;
	}
	var message = req.param('message').toString();
	var msg = clearSpace(message);
	var time = parseInt(req.param('time'));
	message = null;

	if(msg.length >= 256) {
		res.send(fail_msg);
	}
	else if(msg.length == 0) {
		res.send(fail_msg)
	}
	else {
		if(req.session.name && req.session.name.length > 0) {
			talk.insert({
				name: req.session.name,
				message : msg,
				"time" : time
			});

			res.send(success_msg);
		}
		else {
			res.send(fail_msg);
		}
	}
	time = null
	msg = null;
});

app.get('/answer', function(req, res){
	if(!checkValid(req)) {
		res.send(fail_msg);
		return;
	}

	var ans = parseInt(req.param('message'));
	var time = parseInt(req.param('time'));

	if(ans == NaN) {
		res.send(fail_msg);
	}
	else if(ans <= 999) {
		var a_f = parseInt(ans / 100);
		var a_s = parseInt((ans % 100) / 10);
		var a_t = parseInt((ans % 100) % 10);

		var ball = 0;
		var strike = 0;

		var a_array = [a_f, a_s, a_t];

		a_f = null;
		a_s = null;
		a_t = null;

		for(var i = 0; i < a_array.length; i++) {
			var index = c_array.indexOf(a_array[i]);
			if(index >= 0) {
				if(index == i) {
					strike += 1;
				}
				else {
					ball += 1;
				}
			}
		}

		var correct = false;
		if(strike == 3) {
			correct_list.insert({
				name : req.session.name,
				'time' : time,
				answer : ans
			});
			talk.insert({
				name : req.session.name,
				message : "맞았습니다! 정답은 : " + ans,
				"time" : time
			});

			makeNewAnswer();
			correct = true;
		}
		else {
			talk.insert({
				name: req.session.name,
				message : ans + "를 답하셨습니다.\n" + strike + " Strike " + ball + " Ball!",
				"time" : time
			});
		}
		a_s = null;
			
		//resn.send({message : 'success', 'strike' : strike, 'ball' : ball, 'correct' : correct});
		res.send({message : 'success', correct : correct});

		correct = null;
	}
	ans = null;
	time = null;
})

app.get('/readNew', function(req, res){
	if(!checkValid(req)) {
		res.send({"message" : "fail", "reason" : "can't not find cookie"});
		return;
	}

	var time = parseInt(req.param('time'));
	if(time == NaN || time <= 0) {
		res.send({'message' : 'fail'});
	}
	else {
		talk.find({'time' : {'$gt' : (time - 0.1)}}, {'_id' : 0}).toArray(function(err, arr){
			if(err)
			{
				res.send({'message' : 'fail'});
			}
			else {
				res.send({'message' : 'success', 'msg' : arr, 'time' : getTime()});
					
			}
		});	
	}
	time = null;
});

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});
