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
The FileAccessPermission Test xlet includes five applications that are 
presented in the Test Launcher window. These five applications test the 
permissions associated with files that are held in persistent storage.

The five applications are configured through the hostapp.properties file to 
report whether operations pass or fail as expected. The results are presented 
to the screen. Results presented in GREEN indicate that the test has passed 
as expected. Results presented in ORANGE indicate that the test has failed.

The "FAP Create Files" application creates a set of files and then sets 
different FileAccessPermissions on them. It sets one with only owner rights 
(app1.txt), one with same-org rights (org1.txt), one with world rights 
(world1.txt), and one with other-org rights (other1.txt). If the permissions 
given to the files don't come back as expected, then the operation will 
produce a failure.

There are two applications within the same organization that test whether or 
not they can access those files. These two applications are the "FAP Read 
Files (same org)" and "FAP Write Files (same org)". There are also two 
application in a different organization that tests whether they can access 
those files as well.  These applications are the "FAP Read (other org)" and 
"FAP Write Files (diff org)"

The Xlet arguments in the hostapp.properties file describe what should be 
tested and what the expected outcome should be (success or failure).  The 
output on the screen is GREEN (SUCCESS) if the operations end up as expected 
otherwise the output is ORANGE (FAILURE).

The CREATE app creates and sets permissions on files.  The other apps try 
to read/write those files, and test whether the operations succeed/fail as 
expected.

How to run FileAccessPermission Test
------------------
Evaluate by viewing the test results sent to TV screen.

The "FAP Create Files" application must be run first. This will create four 
files and place them in persistent storage. The application will then assign 
different permissions to each one of the files.

After "FAP Create Files" application has been run, the "FAP Read Files 
(same org)", "FAP Write Files (same org)", and "FAP Read Files (other org)" 
applications can be run in any order. These three applications will output 
information to the screen. If all of the output is in GREEN then the 
application is successful. If any of the output is in ORANGE then that 
application failed. 

The "FAP Remove Files (diff org)" application can be run to to verify it
can not delete any of the four files created.  In other words, it verifies
it does not have write access to the directory under which the 
four files are created.  
The "FAP Remove Files (same org)" application can then be run after all the 
other applications have been executed to delete all four files.  This
application verifies it has write access to the directory under which the 
four files are created and hence can delete the four files.

Running any of the "FAP Read Files" or "FAP Write Files" after running 
"FAP Remove Files (same org)" will display output to the screen that is 
not entirely successful. Some of the output will be in ORANGE since the files 
have been removed from the persistent storage.

User Interface
--------------
The five applications are all executed from the Test Launcher. 


Sample Test Case
----------------
1. Select "Start FAP Create Files" and make sure that all output on the screen 
is in GREEN.  Now the files have been successfully created.

2. Press "EXIT" followed by "MENU" to return to the Test Launcher.

3. Select "Start FAP Read Files (same org)" and make sure that all output on 
the screen is in GREEN. 

4. Press "EXIT" followed by "MENU" to return to the Test Launcher.

5. Repeat steps 3 and 4 by selecting "Start FAP Read Files (other org)" and 
"Start FAP Write Files (same org)" making sure the all output to the screen 
is in GREEN.

6. Select "Start FAP Remove Files" to delete the files from persistent storage.


Xlet Control:
initXlet
	Setup scene.
startXlet
	Manage user control and display image.
pauseXlet
	Clear video.
distroyXlet
	Clear video.
