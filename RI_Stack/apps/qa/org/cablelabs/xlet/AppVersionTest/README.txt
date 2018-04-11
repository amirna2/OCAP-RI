to run the AppVersionTest xlet, be sure to set the singalling manager
correctly in final.properties:
    OCAP.mgrmgr.manager.Signalling=org.cablelabs.impl.manager.signalling.TestSignallingMgr

Additionally, be sure OCAP.xait.ignore=true is not set in final.properties.


Xlet Usage/Control:
1.  Copy xait.properties_1 to $OCAPROOT/bin/$OCAPTC/env/xait.properties
2.  Start RI and see Version 1 of the app is started.

3.  Copy xait.properties_2 to $OCAPROOT/bin/$OCAPTC/env/xait.properties; 
    this tests the scenario of an "emergency" replace of the app, where the 
    previous version (1) is signaled to be destroyed and a new version (2) 
    is to start.
4.  Verify Version 2 is started; Press VK_INFO and verify both "hasNewVers" 
    and "isNewVersionSignaled" should be set to false.

5.  Copy xait.properties_3 to $OCAPROOT/bin/$OCAPTC/env/xait.properties; 
    this tests the scenario where two versions of the same app are signaled 
    to autostart in the same service;  The version with the higher 
    launch_order (version 2) should be the version actually shown on the UI 
    as started.
6.  Verify Version 2 is started; press VK_INFO and confirm both "hasNewVers" 
    and "isNewVersionSignaled" should be set to true because Version 3 has been
    signaled and stored.

7.  Copy xait.properties_4 to $OCAPROOT/bin/$OCAPTC/env/xait.properties; 
    this tests the scenario where two versions of the same app are signaled 
    to autostart in different service.  Again, the version with the higher 
    launch order (version 3) is the one that gets started and shown on the UI.
8.  Verify Version 3 is started; Press VK_INFO and verify both "hasNewVers" 
    and "isNewVersionSignaled" should be set to true because Version 4 has 
    been signaled and stored.

9.  Copy xait.properties_5 to $OCAPROOT/bin/$OCAPTC/env/xait.properties; 
    this tests another flavor of the "emergency" replace scenario with the 
    difference being that the new version to be autostarted is in a different 
    service.
10. Verify Version 4 is started; Press VK_INFO and verify both "hasNewVers" 
    and "isNewVersionSignaled" should be set to false since no other versions
    of the app is available.
