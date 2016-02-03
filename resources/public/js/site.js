
// ********** AudioPlayer Class ***********

this.AudioPlayer = (function() {

    AudioPlayer.States = {
      Ready: 0,
      Playing: 1,
      Loading: 2,
      Error: 3
    };

    function AudioPlayer(options) {
      this.setOptions(options);
    }

    AudioPlayer.prototype.setOptions = function(options) {
      var key, value;
      if (options == null) {
        options = {};
      }
      for (key in options) {
        value = options[key];
        this[key] = value;
      }
      if (options.el) {
        return this.setEl(options.el);
      }
    };

    AudioPlayer.prototype.setEl = function(el) {
      if (this.el) {
        this._unbindEvents();
      }
      this.el = el;
      return this._bindEvents();
    };

  return AudioPlayer;
})();


AudioPlayer.prototype.updateState = function(e) {
  var state;

  if(this.isErrored()) {
    state = AudioPlayer.States.Error;
  } else if(this.isLoading()) {
    state = AudioPlayer.States.Loading;
  } else if(this.isPlaying()) {
    state = AudioPlayer.States.Playing;
  } else {
    state = AudioPlayer.States.Ready;
  }

  if (this.state !== state) {
    this.state = state;
    if(this.ui != null) {
      this.ui.AudioPlayerUpdateState(state);
    }
  }
};


AudioPlayer.prototype.isPlaying = function() {
  return this.el && !this.el.paused;
};

AudioPlayer.prototype.isPaused = function() {
  return this.el && this.el.paused;
};

AudioPlayer.prototype.isLoading = function() {
  if (!this.state && this.isEmpty()) {
    return false;
  }
  return this.el.networkState === this.el.NETWORK_LOADING && this.el.readyState < this.el.HAVE_FUTURE_DATA;
};

AudioPlayer.prototype.isErrored = function() {
  return this.el.error || this.el.networkState === this.el.NETWORK_NO_SOURCE;
};

AudioPlayer.prototype.isEmpty = function() {
  return this.el.readyState === this.el.HAVE_NOTHING;
};

AudioPlayer.prototype.duration = function() {
  return this.el.duration;
};

AudioPlayer.prototype.percentComplete = function() {
  var number;
  number = ~~((this.el.currentTime / this.el.duration) * 10000);
  return number / 10000;
};


AudioPlayer.prototype.play = function() {
  var _ref;
  if (this.isEmpty()) {
    if ((_ref = this.ui) != null) {
      _ref.AudioPlayerUpdateState(AudioPlayer.States.Loading);
    }
  }
  return this.el.play();
};

AudioPlayer.prototype.pause = function() {
  return this.el.pause();
};

AudioPlayer.prototype.load = function() {
  console.log("AudioPlayer.load");
  console.log(this);
  return this.el.load();
};

AudioPlayer.prototype.seekTo = function(time) {
  return this.el.currentTime = parseInt(time, 10);
};


AudioPlayer.prototype.handleEvent = function(event) {
  var __indexOf = [].indexOf || function(item) { for (var i = 0, l = this.length; i < l; i++) { if (i in this && this[i] === item) return i; } return -1; };

  var _ref;
  if (_ref = event.type, __indexOf.call(this.audioPlayerEvents, _ref) >= 0) {
    return this.updateState(event);
  } else if (event.type === "timeupdate") {
    return this._timeUpdate(event);
  }
};

AudioPlayer.prototype._bindEvents = function() {
  var eventName, _i, _len, _ref;
  this.audioPlayerEvents || (this.audioPlayerEvents = ["abort", "error", "play", "playing", "seeked", "pause", "ended", "canplay", "loadstart", "loadeddata", "canplaythrough", "seeking", "stalled", "waiting", "progress"]);
  _ref = this.audioPlayerEvents;
  for (_i = 0, _len = _ref.length; _i < _len; _i++) {
    eventName = _ref[_i];
    this.el.addEventListener(eventName, this);
  }
  return this.el.addEventListener("timeupdate", this);
};

AudioPlayer.prototype._unbindEvents = function() {
  var eventName, _i, _len, _ref;
  _ref = this.audioPlayerEvents;
  for (_i = 0, _len = _ref.length; _i < _len; _i++) {
    eventName = _ref[_i];
    this.el.removeEventListener(eventName, this);
  }
  return this.el.removeEventListener("timeupdate", this);
};

AudioPlayer.prototype._timeUpdate = function(e) {
  var _ref;
  if (!this.isLoading()) {
    return (_ref = this.ui) != null ? typeof _ref.AudioPlayerTimeUpdated === "function" ? _ref.AudioPlayerTimeUpdated(this.percentComplete()) : void 0 : void 0;
  }
};

AudioPlayer.prototype.destroy = function() {
  this.ui = null;
  return this._unbindEvents();
};


// ************* AudioPlayerUI Class ********

this.AudioPlayerUI = (function() {

    function AudioPlayerUI(options) {
      this.setOptions( options == null ? {} : options );
      this.audioPlayer = new AudioPlayer( {ui: this} );
      this.audioEl = document.createElement("audio");
      this.audioPlayer.setEl(this.audioEl);
      this.setEl(options.elParent);
      this.goToTrack(0);
    }

    AudioPlayerUI.prototype.setOptions = function(options) {
      var key, value, _results;
      _results = [];
      for (key in options) {
        value = options[key];
        _results.push(this[key] = value);
      }
      return _results;
    };

    AudioPlayerUI.prototype.setEl = function(el) {
      this._unbindEvents();
      this.el = el;
      this.$el = $(this.el);
      this.$el.append(this.audioEl);
      this.$progressContainer = this.$el.find(".audio-player-progress");
      this.$progressBar = this.$el.find(".audio-player-progress-bar");
      this.$playbutton = this.$el.find("#play-pause");
      this.$backButton = this.$el.find("#back");
      this.$nextButton = this.$el.find("#next");
      this.$stopButton = this.$el.find("#stop");
      this.$loopButton = this.$el.find("#loop");
      this.$replayButton = this.$el.find("#replay");
      this._bindEvents();
    };

    return AudioPlayerUI;

})();


AudioPlayerUI.prototype.togglePlayPause = function() {
  if (this.audioPlayer.isPlaying()) {
    this.audioPlayer.pause();
  } else {
    this.audioPlayer.play();
  }
};


AudioPlayerUI.prototype.goToTrack = function(index) {
  console.log("goToTrack: " + index)

  var wasPlaying = this.audioPlayer.isPlaying();

  this.currentTrack = index;

  this.$progressBar.css({
    width: 0
  });

  this.audioEl.src = this.tracks[this.currentTrack].url;
  this.audioPlayer.load();
  if (wasPlaying) {
    return this.audioPlayer.play();
  }
};


AudioPlayerUI.prototype.nextTrack = function() {
  this.goToTrack( this.currentTrack === this.tracks.length - 1 ? 
    0 : this.currentTrack + 1);
};


AudioPlayerUI.prototype.prevTrack = function() {
  this.goToTrack( this.currentTrack === 0 ?
    this.tracks.length - 1  : this.currentTrack - 1 );
};


AudioPlayerUI.prototype.seek = function(e) {
  var duration, offset, percent, seekTo, _ref;
  if (offset = e.offsetX || ((_ref = e.originalEvent) != null ? _ref.layerX : void 0)) {
    percent = offset / this.$progressContainer.width();
    duration = this.audioPlayer.duration();
    seekTo = duration * percent;
    return this.audioPlayer.seekTo(seekTo);
  }
};

AudioPlayerUI.prototype.AudioPlayerUpdateState = function() {
//  this.$el.toggleClass("error", this.audioPlayer.isErrored());
  this.$progressContainer.toggleClass("loading", this.audioPlayer.isLoading());

  if (this.audioPlayer.isPlaying()) {
    this.$playbutton.html("2");
  } else {
    this.$playbutton.html("1");
  }
};


AudioPlayerUI.prototype.AudioPlayerTimeUpdated = function(percentComplete) {
  return this.$progressBar.css({
    width: "" + (percentComplete * 100) + "%"
  });
};


AudioPlayerUI.prototype.stopPlayer = function() {
  this.audioPlayer.pause();
  this.audioPlayer.seekTo(0);
};

AudioPlayerUI.prototype.toggleLoopMode = function() {
  if ( this.audioPlayer.el.loop ) {
    this.$loopButton.removeClass( "buttonPushed" );
    this.audioPlayer.el.loop = false;
  }
  else {
    this.$loopButton.addClass( "buttonPushed" );
    this.audioPlayer.el.loop = true;
  }
};

AudioPlayerUI.prototype.replayTrack = function() {
  this.audioPlayer.seekTo(0);
};


AudioPlayerUI.prototype._bindEvents = function() {
  this.$playbutton.on("click", $.proxy(this, "togglePlayPause"));
  this.$backButton.on("click", $.proxy(this, "prevTrack"));
  this.$nextButton.on("click", $.proxy(this, "nextTrack"));
  this.$stopButton.on("click", $.proxy(this, "stopPlayer"));
  this.$loopButton.on("click", $.proxy(this, "toggleLoopMode"));
  this.$replayButton.on("click", $.proxy(this, "replayTrack"));
  return this.$progressContainer.on("mouseup", $.proxy(this, "seek"));
};


AudioPlayerUI.prototype._unbindEvents = function() {
  var _ref, _ref1, _ref2, _ref3;
  if ((_ref = this.$button) != null) {
    _ref.off("click", this.togglePlayPause);
  }
  if ((_ref1 = this.$backButton) != null) {
    _ref1.off("click", this.previousSong);
  }
  if ((_ref2 = this.$nextButton) != null) {
    _ref2.off("click", this.nextSong);
  }
  return (_ref3 = this.$progressContainer) != null ? _ref3.off("mouseup", this.seek) : void 0;
};


//***********

$(document).ready(function(){

    console.log("Doc ready");

    $.get("/ctl-get-text", 'test=1', function(data) {
      $('#textbox').html(data);
    });

    audioPlayer = new AudioPlayerUI({
        elParent: document.getElementById("controls"),
        tracks: [
          {
            title: "AA",
            url: "data/crim/audio/01_01.m4a"
          },
          {
            title: "BB",
            url: "data/crim/audio/01_02.m4a"
          }
        ]
    } );

//    play = $('#play-pause');
//    replay = $('#replay');

//    ctl_audio = new Audio();

//    addEventHandlers();

//    ctl_audio.src = "data/crim/audio/01_01.m4a";
//    ctl_audio.play();

});




function addEventHandlers(){
    $("a#stop").click(stopAudio);
    $("a#back").click(backAudio);
    play.click(playAudio);
    replay.click(replayAudio);
    $("a#next").click(nextAudio);
    $("a#loop").click(loopAudio);
}


function stopAudio() {
}

function backAudio() {
}

function playAudio() {
  ctl_audio.play();

//  $("a#play").attr("disabled", "disabled");
//  $("a#pause").removeAttr("disabled");

  $("a#play").html("");
  $("a#pause").html("");
}

function pauseAudio() {
  ctl_audio.pause();

//  $("a#play").removeAttr("disabled");
//  $("a#pause").attr("disabled", "disabled");

  $("a#play").html("");
  $("a#pause").html("1");
}

function replayAudio() {
  ctl_audio.currentTime = 0;
}

function nextAudio() {
}

function loopAudio() {
  ctl_audio.loop = !ctl_audio.loop;
}
