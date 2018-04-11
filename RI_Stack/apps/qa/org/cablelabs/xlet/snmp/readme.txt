SNMPXlet.Java
=============

This is a test applet for using the diagnostics snmp interface under various conditions

MIBManager::MIBDefinitionExt queryMibs(string oid)

To run the App:

1 - Change 
  ri\RI_Stack\bin\CableLabs\simulator\Win32\debug\env\hostapp.properties
  to
app.0.application_identifier=0x000000014001
app.0.application_control_code=AUTOSTART
app.0.visibility=VISIBLE
app.0.priority=220
app.0.base_directory=/syscwd/qa/xlet
app.0.initial_class_name=org.cablelabs.xlet.snmp.SNMPXlet
app.0.application_name=SNMPXlet
app.0.args.0=x=175
app.0.args.1=y=100
app.0.args.2=width=300
app.0.args.3=height=150
  
  
2 - turn off security checking in ri\RI_Stack\java\src\base\base.properties
#OCAP.mgrmgr.manager.OcapSecurity=org.cablelabs.impl.manager.security.OcapSecurityImpl
OCAP.mgrmgr.manager.OcapSecurity=org.cablelabs.impl.manager.security.NoAccessControl





