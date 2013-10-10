(function () {

    //var host = "192.168.0.105",
    var host = /(.+):/.exec(window.location.host)[1],

    testScreenState = function () {
	if (screenState==0) {
	    $('#error-screenoff').fadeIn(1000);
	    $('#glass').fadeIn(1000);
	}
    },

    loadSpydroidUI = function () {
	wait = false;

	$('h1,h2,span,p').translate();

	testScreenState();

	$('.sound').click(function () {
	    $.get('/sound.htm?name='+$(this).attr('id'));
	});

	$(document).on('keypress',function(e) {
	    if (e.keyCode == 38 || e.which == 38) {
	    	$.get('/sound.htm?name=forward');
	    }
	    if (e.keyCode == 40 || e.which == 40) {
	    	$.get('/sound.htm?name=backward');
	    }
	    if (e.keyCode == 37 || e.which == 37) {
	    	$.get('/sound.htm?name=left');
	    }
	    if (e.keyCode == 39 || e.which == 39) {
	    	$.get('/sound.htm?name=right');
	    }
	    if (e.keyCode == 32 || e.which == 32 || e.keyCode == 12 || e.which == 12) {
	    	$.get('/sound.htm?name=stop');
	    }
	    if (e.keyCode == 43 || e.which == 43) {
	    	$.get('/sound.htm?name=incspeed');
	    }
	    if (e.keyCode == 45 || e.which == 45) {
	    	$.get('/sound.htm?name=decspeed');
	    }	    	    	    	    	    
	});
	
	$('#forward').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});	
	
	$('#backward').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});
	
	$('#left').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});
	
	$('#right').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});
	
	$('#stop').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});
	
	$('#incspeed').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});
	
	$('#decspeed').click(function () {
		$.get('/sound.htm?name='+$(this).attr('id'));
	});

	$('.behaviors input').change(function () {
		if ($('#obstacleAvoidance').attr('checked')) { 
			$.get('/sound.htm?name=obstacleOn');
		} else {
			$.get('/sound.htm?name=obstacleOff');
		}
		
		if ($('#cliffAvoidance').attr('checked')) { 
			$.get('/sound.htm?name=cliffOn');
		} else {
			$.get('/sound.htm?name=cliffOff');
		}
	
	});	

    };

    $(document).ready(function () {
	loadSpydroidUI();
    });

}());
