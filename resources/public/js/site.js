
// ***************************************************************************************
// ************* AudioPlayer Class 
// ***************************************************************************************

this.AudioPlayer = (function() {

    AudioPlayer.States = {
      Ready:    0,
      Playing:  1,
      Loading:  2,
      Error:    3
    };

    function AudioPlayer(options) {
      // Create audio element if none provided
      if ( options.el === undefined ) {
        this.el = document.createElement("audio");
        this.el.autoplay = "true";
      }

      // Set all options as part of this
      this.setOptions(options);

      this._bindEvents();
    }


    AudioPlayer.prototype.setOptions = function(options) {
      options = options || {};

      for ( var key in options ) {
        this[key] = options[key];
      }
    };

    AudioPlayer.prototype.setEl = function(el) {
      if ( this.el ) {
        this._unbindEvents();
      }
      this.el = el;
      this._bindEvents();
    };

  return AudioPlayer;
})();


AudioPlayer.prototype.setAudioSrc = function( url )
{
  var _playing = this.isPlaying();

  this.el.src = url;
  this.load();

  if ( _playing ) this.play();
}


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
  return this.el.load();
};

AudioPlayer.prototype.seekTo = function(time) {
  return this.el.currentTime = parseInt(time, 10);
};


AudioPlayer.prototype.handleEvent = function(event) {
  if (event.type === "timeupdate") {
    this._timeUpdate(event);
  }
  else {
    this.updateState(event);
  }
};


AudioPlayer.prototype._bindEvents = function() {
  var _ref = this.audioPlayerEvents = 
      [ "abort", "error", "play", "playing", "seeked", "pause", "ended", 
        "canplay", "loadstart", "loadeddata", "canplaythrough", "seeking", 
        "stalled", "waiting", "progress"];

  for ( var i = 0 ; i < _ref.length ; i++ ) {
    this.el.addEventListener( _ref[i], this );
  }

  this.el.addEventListener("timeupdate", this);
};


AudioPlayer.prototype._unbindEvents = function() {
  var _ref = this.audioPlayerEvents;

  for ( i = 0 ; i < _ref.length ; i++ ) {
    this.el.removeEventListener(_ref[i], this);
  }
  this.el.removeEventListener("timeupdate", this);
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


// ***************************************************************************************
// ************* AudioPlayerUI Class 
// ***************************************************************************************

this.AudioPlayerUI = (function() {

    function AudioPlayerUI(options) {
      this.tracks = null;

      // Set options
      for ( var key in options ) {
        this[key] = options[key];
      }

      this.audioPlayer = new AudioPlayer( {ui: this} );

      this.setUI( options.ui );
    };


    AudioPlayerUI.prototype.setUI = function(el) {
      //this._unbindEvents();
      this.el = el;
      var $el = $(this.el);
      this.$progressContainer = $el.find(".audio-player-progress");
      this.$progressBar = $el.find(".audio-player-progress-bar");
      this.$playbutton = $el.find("#play-pause");
      this.$backButton = $el.find("#back");
      this.$nextButton = $el.find("#next");
      this.$stopButton = $el.find("#stop");
      this.$loopButton = $el.find("#loop");
      this.$replayButton = $el.find("#replay");
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


AudioPlayerUI.prototype.seek = function(e) {
  var duration, offset, percent, seekTo, _ref;
  if (offset = e.offsetX || ((_ref = e.originalEvent) != null ? _ref.layerX : void 0)) {
    percent = offset / this.$progressContainer.width();
    duration = this.audioPlayer.duration();
    seekTo = duration * percent;
    this.audioPlayer.seekTo(seekTo);
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


AudioPlayerUI.prototype.loadText = function( url ) {
  console.log("loadText:" + url);

  $.get("/ctl-get-text", { url: url}, function(data) {
     $('#textbox').html(data);
   });

  $('#info-line').html( "<i>File: </i>" + url );
}


AudioPlayerUI.prototype.loadAudio = function( url ) {
  console.log("loadAudio:" + url);

  this.$progressBar.css( {width: 0} );
  this.audioPlayer.setAudioSrc( url );
}


AudioPlayerUI.prototype.handleServerPost = function( event ) {
  console.log("handleServerPost: " + event.target.id);

  // Save context of this obj for the callback func
  var _this = this;

  $.post("/ctl-post-user-event", { id: event.target.id }, function(data) {
//      console.log("Post ret:");
//      console.log(data);

      for ( i=0 ; i < data.length ; i++ ) {
        var args = data[i];
        var fn = args.shift();
        window["AudioPlayerUI"]["prototype"][fn].apply( _this, args );
      }
  }, 'json');
};


AudioPlayerUI.prototype.handleEvent = function( event ) {
  console.log("AudioPlayerUI.handleEvent:" + event.type);
  if ( event.type == "ended" ) {
    this.handleServerPost( {target: {id: "ended"}} );
  }
}


AudioPlayerUI.prototype._bindEvents = function() {
  this.$playbutton.on("click", $.proxy(this, "togglePlayPause"));
  this.$stopButton.on("click", $.proxy(this, "stopPlayer"));
  this.$loopButton.on("click", $.proxy(this, "toggleLoopMode"));
  this.$replayButton.on("click", $.proxy(this, "replayTrack"));
  this.$progressContainer.on("mouseup", $.proxy(this, "seek"));

  // Capture click events for all controls with class "server"
  $(this.el).find(".server").on("click", $.proxy(this.handleServerPost, this))  

  // Capture track ended event from audio player
  this.audioPlayer.el.addEventListener("ended", this);
};


AudioPlayerUI.prototype._unbindEvents = function() {
  this.$playbutton.off("click");
  this.$stopButton.off("click");
  this.$loopButton.off("click");
  this.$replayButton.off("click");
  this.$progressContainer.off("mouseup");
  
  $(this.el).find(".server").off("click", this.handleServerPost); 

  this.audioPlayer.el.removeEventListener("ended", this);

  return (_ref3 = this.$progressContainer) != null ? _ref3.off("mouseup", this.seek) : void 0;
};


// ***************************************************************************************
// ************* Document Ready
// ***************************************************************************************

$(document).ready(function(){

    console.log("Doc ready");

    // Create AudioPlayer 
    var audioPlayer = new AudioPlayerUI( { ui: document.getElementById("controls") } );

    // Start up 
    audioPlayer.handleServerPost( { target: { id: "startup" }} );

});
