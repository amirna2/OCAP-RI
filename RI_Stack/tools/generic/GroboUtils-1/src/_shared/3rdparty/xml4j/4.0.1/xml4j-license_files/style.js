<!--
if ((navigator.appName == "Microsoft Internet Explorer") && (parseInt(navigator.appVersion) >= 4 ))
	if((navigator.appVersion.indexOf("Macintosh"))!= -1)
		document.write('<link rel="stylesheet" href="/css/r1.css" type="text/css"/>')
	else document.write('<link rel="stylesheet" href="/css/ie1.css" type="text/css"/>')
else if ((navigator.appName == "Netscape") && (parseInt(navigator.appVersion) >= 4))
	if((navigator.appVersion.indexOf("Macintosh"))!= -1)
		document.write('<link rel="stylesheet" href="/css/r1.css" type="text/css"/>')
	else if ((navigator.appVersion.indexOf("X11"))!= -1)
		document.write('<link rel="stylesheet" href="/css/ln1.css" type="text/css"/>')
	else document.write('<link rel="stylesheet" href="/css/ns1.css" type="text/css"/>')
else document.write('<link rel="stylesheet" href="/css/r1.css" type="text/css"/>') // -->