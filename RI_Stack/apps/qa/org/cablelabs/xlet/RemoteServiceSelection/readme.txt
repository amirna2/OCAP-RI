This application is built by build.apps target and is registered in qa/hostapp.properties - use Video Test Service/RemoteService Selection
(ensure qa/hostapp.properties is in the classpath)

NOTE: you must update the app.119.args to point to the server you want and app.119.args.1 to point to the server name
(OCAP Media Server for the ri, Cyber Garage Media Server for Cyber Media Gate)

(left here for example purposes)

app.0.application_identifier=0x000000017000
app.0.application_control_code=AUTOSTART
app.0.visibility=VISIBLE
app.0.priority=220
app.0.application_name=RemoteServiceSelection
app.0.base_directory=/syscwd/qa/xlet
app.0.initial_class_name=org.cablelabs.xlet.RemoteServiceSelection.RemoteServiceSelectionXlet
# Inet Address
app.0.args.0=10.0.1.144
# Media Server name
app.0.args.1=OCAP Media Server
