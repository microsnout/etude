
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
  this.updateState(event);
};


AudioPlayer.prototype._bindEvents = function() {
  var _ref = this.audioPlayerEvents = 
      [ "abort", "error", "play", "playing", "seeked", "pause", "ended", 
        "canplay", "loadstart", "loadeddata", "canplaythrough", "seeking", 
        "stalled", "waiting", "progress"];

  for ( var i = 0 ; i < _ref.length ; i++ ) {
    this.el.addEventListener( _ref[i], this );
  }
};


AudioPlayer.prototype._unbindEvents = function() {
  var _ref = this.audioPlayerEvents;

  for ( i = 0 ; i < _ref.length ; i++ ) {
    this.el.removeEventListener(_ref[i], this);
  }
};


AudioPlayer.prototype.destroy = function() {
  this.ui = null;
  return this._unbindEvents();
};


// ***************************************************************************************
// ************* AudioPlayerUI Class 
// ***************************************************************************************

const scoreGreen = 1;
const scoreRed   = 0x10000;
const greenMask  = 0x0ffff;
const redShift   = 16;


this.AudioPlayerUI = (function() {

    function AudioPlayerUI(options) {

      // Set options
      for ( var key in options ) {
        this[key] = options[key];
      }

      this.audioPlayer = new AudioPlayer( {ui: this} );
      this.setUI( options.ui );

      // Initialise jScrollPane
      var pane = $('.scroll-pane');
      pane.jScrollPane();
      this.apiScrollpane = pane.data('jsp');

      //  statistics
      this.score = 0;
      this.total = 0;
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

  // Navigate back to control 
  window.location.href = "/control";
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


AudioPlayerUI.prototype.recordWord = function( text, word ) {
  this.score += ((text == word) ? scoreGreen : scoreRed);

  var txt = 
    "Score: " + 
    "<span style=color:green>" + (this.score & greenMask) + "</span>" + 
    "-" +
    "<span style=color:red>" + (this.score >>> redShift) + "</span>" +
    "  Total: " + 
    "<span style=color:green>" + (this.total & greenMask) + "</span>" + 
    "-" +
    "<span style=color:red>" + (this.total >>> redShift) + "</span>";

  $('#play-score').html(txt);
};


AudioPlayerUI.prototype.loadText = function( url ) {
  console.log("loadText:" + url);

  // Save context of this obj for the callback func
  var _this = this;

  $.get("/play-get-text", { url: url}, function(data) {
      _this.apiScrollpane.getContentPane().html(data);
      _this.apiScrollpane.reinitialise();

      // Focus the first input field and make return same as tab
      $("#textbox").mousedown (
          // Prevent mouse click in box from defocusing current input field
          function(e) { e.preventDefault(); }
      ).find("input.cloze").mousedown( 
          // Stop mouse events within input fields
          function(e) { e.preventDefault(); }
      ).pressEnter( 
          // Make enter act the same as tab
          function() { $(this).next().focus(); }
      ).focusout( function() {
          var text = $(this).val();
          var word = $(this).attr("data-word");
          _this.recordWord(text, word);
          $(this).css({ 'font-weight': 'bold' }).css( 
              "color", 
              word == text ? "green" : "red");
      }).eq(0).focus();
   });
}


AudioPlayerUI.prototype.loadAudio = function( url ) {
  console.log("loadAudio:" + url);

  this.$progressBar.css( {width: 0} );
  this.audioPlayer.setAudioSrc( url );
}


AudioPlayerUI.prototype.setInfoLine = function(s) {
  $('#play-title').html( s );
}


AudioPlayerUI.prototype.handleServerPost = function( event ) {
  console.log("handleServerPost: " + event.target.id);

  // Save context of this obj for the callback func
  var _this = this;

  $.post("/play-post-user-event", { id: event.target.id }, function(data) {
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
  if ( event.type == "ended" ) {
    this.handleServerPost( {target: {id: "ended"}} );
  }
  else if ( event.type == "timeupdate") {
    this.AudioPlayerTimeUpdated( this.audioPlayer.percentComplete() );
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
  this.audioPlayer.el.addEventListener("timeupdate", this);
};


AudioPlayerUI.prototype._unbindEvents = function() {
  this.$playbutton.off("click");
  this.$stopButton.off("click");
  this.$loopButton.off("click");
  this.$replayButton.off("click");
  this.$progressContainer.off("mouseup");
  
  $(this.el).find(".server").off("click", this.handleServerPost); 

  this.audioPlayer.el.removeEventListener("ended", this);
  this.audioPlayer.el.removeEventListener("timeupdate", this);

  return (_ref3 = this.$progressContainer) != null ? _ref3.off("mouseup", this.seek) : void 0;
};


// ***************************************************************************************
// ************* Document Ready
// ***************************************************************************************

$(document).ready(function(){

    console.log("Player ready");

    // Create AudioPlayer 
    var audioPlayer = new AudioPlayerUI( { ui: document.getElementById("controls") } );

    // Start up 
    audioPlayer.handleServerPost( { target: { id: "startup" }} );

    // Re-initialise the jScrollPane when the window is resized
    $('.scroll-pane').each(
      function() {
        var api = $(this).data('jsp');
        var timeout;
        $(window).bind( 'resize',
          function() {
            if ( !timeout ) {
              timeout = setTimeout(
                function() {
                  api.reinitialise();
                  timeout = null;
                }, 
                50);
            }
          }
        )
      }
    )

});


$.fn.pressEnter = function(fn) {  

    return this.each(function() {  
        $(this).bind('enterPress', fn);
        $(this).keyup(function(e){
            if(e.keyCode == 13)
            {
              $(this).trigger("enterPress");
            }
        })
    });  
 }; 




















