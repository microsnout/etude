
$(document).ready(function(){
    $.get("/ctl-get-text", 'test=1', function(data) {
      $('#textbox').html(data);
    });

});
