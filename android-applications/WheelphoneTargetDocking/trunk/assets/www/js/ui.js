(function () {

    //var host = "192.168.0.105",
    var host = /(.+):/.exec(window.location.host)[1],

    generateURI = function (h) {
		var audioEncoder, videoEncoder, cache, rotation, flash, res;
	
		// Audio conf
		if ($('#audioEnabled').attr('checked')) {
		    audioEncoder = $('#audioEncoder').val()=='AMR-NB'?'amr':'aac';
		} else {
		    audioEncoder = "nosound";
		}
		
		// Resolution
		res = /([0-9]+)x([0-9]+)/.exec($('#resolution').val());
	
		// Video conf
		if ($('#videoEnabled').attr('checked')) {
		    videoEncoder = ($('#videoEncoder').val()=='H.263'?'h263':'h264')+'='+
			/[0-9]+/.exec($('#bitrate').val())[0]+'-'+
			/[0-9]+/.exec($('#framerate').val())[0]+'-';
		    videoEncoder += res[1]+'-'+res[2];
		} else {
		    videoEncoder = "novideo";
		}
		
		// Flash
		if ($('#flashEnabled').val()==='1') flash = 'on'; else flash = 'off';
	
		// Params
		cache = /[0-9]+/.exec($('#cache').val())[0];
		
		$.get('config.json?set&'+videoEncoder+'&'+audioEncoder);
	
		return {
		    uria:"rtsp://"+h+":"+8086+"?"+audioEncoder,
		    uriv:"rtsp://"+h+":"+8086+"?"+videoEncoder+'&flash='+flash,
		    params:[':network-caching='+cache]
		    //params:[':rotate-angle=90']
		    //params:['--video-filter=adjust --brightness=2.0']
		    //params:[':fullscreen']
		    //params:[':video-filter=rotate :rotation-angle=90']
		}
    },

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
	    if (e.keyCode == 13 || e.which == 13 || e.keyCode == 12 || e.which == 12) {
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

	$.getJSON('config.json?get',function (config) {
	    $('#resolution,#framerate,#bitrate,#audioEncoder,#videoEncoder').children().removeAttr('selected').each(function (c) {
		if ($(this).val()===config.videoResX+'x'+config.videoResY || 
		    $(this).val()===config.videoFramerate+" fps" || 
		    $(this).val()===config.videoBitrate+" bps" || 
		    $(this).val()===config.audioEncoder ||
		    $(this).val()===config.videoEncoder ) {
		    $(this).attr('selected','true');
		}
	    });	    
	    if (config.streamAudio===false) $('#audioEnabled').removeAttr('checked');
	    if (config.streamVideo===false) $('#videoEnabled').removeAttr('checked');
	});

    };

    $(document).ready(function () {
	loadSpydroidUI();
    });

}());
