// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

Description: 
This test xlet verifies various aspects of EAS.  It registers an EASListener
so user can verify the EASEvent accurately reflects the current state of 
EAS.
The test xlet can also be used to test the functionalities provided by 
EASModuleRegistrar.  User can use the test xlet to change the color, font, 
size... of an EAS_TEXT_DISPLAY.
The test xlet only knows about 3 services: 
    0x44c:
    0x45a: The same service as that used by the EAS_DETAILS_CHANNEL
    651MHZ,1,16: 

Configuration Parameters:
Example hostapp.properties entry can be found in 
$OCAPROOT/apps/qa/org/cablelabws/xlet/EASTest/hostapp.properties.eas


Evaluate by viewing the TV screen and RI log.
Control Keys:
  VK_CHANNEL_UP/DOWN: toggles between the two services that are not the 
                      channel locator of the EAS_DETAILS_CHANNEL 
  VK_VOLUME_UP/DOWN: same as VK_CHANNEL_UP/DOWN except a new ServiceContext
                     is created before calling select() on the ServiceContext
  VK_ENTER: selects service that is the same service used by the 
            EAS_DETAILS_CHANNEL
  VK_0: calls destroy() on the ServiceContext.
  VK_8: calls stop() on the ServiceContext.

  VK_1: change the color of the Text display of the EAS based on the 
        "font_color" and "back_color" set in hostapp.properties
  VK_2: change the font of the Text display of the EAS based on the 
        "font_face" and "font_style" set in hostapp.properties
  VK_3/6: change the font size of the Text display of EAS
          3 - makes it larger
          6 - makes it smaller
  VK_4: change the opacity of the Text display of the EAS based on the 
        "font_opacity" and "back_opacity" set in hostapp.properties
  VK_INFO: prints out the EAS capabilities
  VK_POWER: prints out the attributes of the EAS

For app developers running the RI on the PC Platform, see 
    $RICOMMONROOT/resources/fdcdata/eas-test-files/README 
for information on how to simulate the delivery of EAS messages

For set-top developers, you can run this application on your device as a 
hostapp using the above signaling.  
Then, trigger an EAS message on your headend.  

Test Cases:
1: Verify channel tuning is disabled while EAS broadcast by reusing an
   existing ServiceContext is in progress
     VK_ENTER to select the EAS service
     VK_GUIDE to toggle between the 2 other services 10 times
     VK_POWER to go to FULL_POWER (do this withint 10 seconds of previous
              key press of VK_GUIDE

   If EAS is EAS_DETAILS_CHANNEL: 
       selection of the two non-EAS services should fail while EAS is in 
       progress due to SelectFailedEvent (INSUFFICIENT_RESOURCES)
   else
       selection of the two non-EAS services should still work but
       EAS is shown on each new service presentation

   In either case, TV Screen should be completely taken over by the EAS 
   with nothing overlapping to to keep any part of the EAS from being viewed

2: Verify channel tuning is disabled while EAS broadcast by creating a new
   ServiceContext is in progress
       VK_GUIDE to toggle between the 2 other services 10 times
       VK_POWER to go to FULL_POWER (do this withint 10 seconds of pressing

3, 4: Same as the two tests above except use VK_MENU instead of VK_GUIDE
      to toggle channel with a new ServiceContext being created before 
      each select() call.


5: Try to destroy ServiceContext while EAS (broadcast by reusing svcCtxt) 
   is in progress 
       VK_ENTER to select the EAS service
       VK_0 call destroy() on ServiceContext
       VK_POWER to go to FULL_POWER (do this withint 10 seconds of pressing
                VK_GUIDE.
    If EAS is EAS_DETAILS_CHANNEL: 
      Destroy should fail due to an IllegalStateException.  
    else
      Destroy should be successful since the EAS does not use a SvcCtxt
    In either case, EAS continues to be presented without interruption 
    until complete

6: Try to destroy ServiceContext while EAS (broadcast by new svcCtxt) is 
   in progress 
       VK_CHANNEL_UP to select some service
       VK_0 call destroy() on ServiceContext
       VK_POWER to go to FULL_POWER (do this withint 10 seconds of pressing
                VK_GUIDE.
    Destroy is successful since the it was destroying a different 
    ServiceContext then the one used for the current EAS broadcast.  
    EAS continues to be presented without interruption until complete

7, 8: Try to stop ServiceCOntext while EAS is in progress
      Same test procedure and result as 5/6 except use VK_8 instead of VK_0
      to stop serviceContext rather than destroy
