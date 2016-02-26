// ***************************************************************************************
// ************* Controler Class 
// ***************************************************************************************

this.Controler = (function() {

    function Controler(options) {

      // Set options
      for ( var key in options ) {
        this[key] = options[key];
      }

      this.setUI( options.ui );

      // Initialise jScrollPane
      var pane = $('.scroll-pane');
      pane.jScrollPane();
      this.apiScrollpane = pane.data('jsp');
    };


    Controler.prototype.setUI = function(el) {
      //this._unbindEvents();
      this.el = el;
      var $el = $(this.el);
      this.$addButton = $el.find("#add");
      this.$subButton = $el.find("#sub");
      this.$playButton = $el.find("#play");
      this._bindEvents();
    };

    return Controler;
})();


Controler.prototype.loadControlHtml = function() {
  console.log("loadControlHtml:");

  // Save context of this obj for the callback func
  var _this = this;

  $.get("/ctl-get-control-html", {}, function(data) {
      console.log(data);
      _this.apiScrollpane.getContentPane().html(data);
      _this.apiScrollpane.reinitialise();
   });
}


Controler.prototype.playControl = function() {
  console.log("playControl:");
  var parms = "";
  var space = "?";

  $('.table-x').each( function(index) {
    parms += space;
    parms += $(this).attr('id');
    parms += "=";
    parms += $(this).find('input:checked').attr("data-id");
    space = "&";
  });

  window.location.href = "/player" + parms; 
}


Controler.prototype.setInfoLine = function(s) {
  $('#info-line').html( "<i>File: </i>" + s );
}


Controler.prototype.handleServerPost = function( event ) {
  console.log("handleServerPost: " + event.target.id);

  // Save context of this obj for the callback func
  var _this = this;

  $.post("/ctl-post-user-event", { id: event.target.id }, function(data) {
//      console.log("Post ret:");
//      console.log(data);

      for ( i=0 ; i < data.length ; i++ ) {
        var args = data[i];
        var fn = args.shift();
        window["Controler"]["prototype"][fn].apply( _this, args );
      }
  }, 'json');
};


Controler.prototype.handleEvent = function( event ) {
};


Controler.prototype._bindEvents = function() {
  this.$addButton.on("click", $.proxy(this, "addControl"));
  this.$subButton.on("click", $.proxy(this, "subControl"));
  this.$playButton.on("click", $.proxy(this, "playControl"));
    
  // Capture click events for all controls with class "server"
  $(this.el).find(".server").on("click", $.proxy(this.handleServerPost, this))  
};


Controler.prototype._unbindEvents = function() {
  this.$addButton.off("click");
  this.$subButton.off("click");
  this.$playButton.off("click");
  
  $(this.el).find(".server").off("click", this.handleServerPost); 
};

// ***************************************************************************************
// ************* Document Ready
// ***************************************************************************************

$(document).ready(function(){

    console.log("Site ready");


    // Create Controler  
    var controler = new Controler( { ui: document.getElementById("controls") } );

    // Start up 
    controler.handleServerPost( { target: { id: "startup" }} );

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

