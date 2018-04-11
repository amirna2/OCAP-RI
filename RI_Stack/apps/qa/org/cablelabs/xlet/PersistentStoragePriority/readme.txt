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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

*********************************************************************************
				Description
*********************************************************************************
The Persistent Storage Priority test is designed to test out the High Water Mark 
and the purging priority of the Persistent Files in the stack. The test is 
comprised of the following:

PSDeadXlet 
This xlet shall write out files each with a unique app priority. The files generated
each will have LOW, MEDIUM, and HIGH priority assigned. This xlet will be terminated
by the TestRunnerXlet upon writing completion. The xlet will be extended 3 times
for various app prioirties

PSAliveXlet
This xlet shall write out files each with a unique app priority. The files
generated each will have LOW, MEDIUM, and HIGH priority assigned along with
one file that has no app priority assigned. This xlet will be not be terminated
by the TestRunnerXlet upon writing completion. The xlet will be extended 3 times
for various app prioirties

PSGobblerXlet
This xlet shall write out to a file that induces deletion of other application
files.  This xlet shall contain a list of the files that are to be checked for
deletion as chunks of data are written out to the “gobbler” file. All files will
be checked once the chunk of data has been written to be checked of their status.
The xlet shall check files that are not deleted if the Gobbler Xlet has a lower
priority than the app that has created the file. The xlet shall have the ability
to clear all the previous files if necessary on starup. The test xlet will be 
instanced multple times to test the response of deletion when app priority is set to 
different values. The interface is as followed: 

<1> - Clean All : Deletes all files from ITFS partition within the usr diectory

<2> - Write Files : Goes through and kicks off file writing applications. The high
				water mark should be signaled, sending out a passed message.

<3> - Kill Apps : Destroys the PSDeadApp*Xlet file writning applications. 

<4> - Start Gobbler : Starts the gobbler file writer. This application as stated
				above will cause impicit deletion of the files in the usr dir. 

<5> - Print List of Files : Prints a list of the files to be created in the usr directory
				NOTE: this is not a listing of the files in the current directory

<6> - Print List of Dirs : Prints a list of the directory structure to be created
				NOTE: this is not a listing of the current dircetory structure

<7> - Kill All Apps : Kills all xlet writer applications 

<8> - Runs all the tests in a row
	Kill All Apps
	Clean All
	Write Files
	Kill Apps
	Start Gobbler

<INFO> - Prints out all options

This Test Xlet is designed to replace the existing Persistent Storage Test. 

*********************************************************************************
				Configuration
*********************************************************************************
Do the following:

In the mpeenv.ini
# PersistentStorageTest settings. 
# The following directory is cleared of files at the start and at the end of
# the PersistentStorageTest. 
OCAP.persistent.dvbroot=/itfs/usr
# Set quota on this directory to 1M
OCAP.filesys.persistent=/itfs/usr=1M

In the hostapp.properties file, make sure the following entries are there
#################################################
## Application 76 – PSLiveApp3Xlet
#################################################
app.76.application_identifier=0x000000017244
app.76.application_control_code=PRESENT
app.76.visibility=INVISIBLE
app.76.priority=210
app.76.application_name= PSLiveApp3Xlet
app.76.base_directory=/snfs/qa/xlet
app.76.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSLiveApp3Xlet
app.76.service=0x12357
app.76.args.0=config_file=config.properties

#################################################
## Application 77 – PSDeadApp3Xlet
#################################################
app.77.application_identifier=0x000000017245
app.77.application_control_code=PRESENT
app.77.visibility=INVISIBLE
app.77.priority=210
app.77.application_name= PSDeadApp3Xlet
app.77.base_directory=/snfs/qa/xlet
app.77.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSDeadApp3Xlet
app.77.service=0x12357
app.77.args.0=config_file=config.properties

#################################################
## Application 78 – PSLiveApp2Xlet
#################################################
app.78.application_identifier=0x000000017246
app.78.application_control_code=PRESENT
app.78.visibility=INVISIBLE
app.78.priority=200
app.78.application_name= PSLiveApp2Xlet
app.78.base_directory=/snfs/qa/xlet
app.78.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSLiveApp2Xlet
app.78.service=0x12357
app.78.args.0=config_file=config.properties

#################################################
## Application 79 - PSDeadApp2Xlet
#################################################
app.79.application_identifier=0x000000017247
app.79.application_control_code=PRESENT
app.79.visibility=INVISIBLE
app.79.priority=200
app.79.application_name= PSDeadApp2Xlet
app.79.base_directory=/snfs/qa/xlet
app.79.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSDeadApp2Xlet
app.79.service=0x12357
app.79.args.0=config_file=config.properties

#################################################
## Application 80 – PSLiveApp1Xlet
#################################################
app.80.application_identifier=0x000000017248
app.80.application_control_code=PRESENT
app.80.visibility=INVISIBLE
app.80.priority=190
app.80.application_name=PSLiveApp1Xlet
app.80.base_directory=/snfs/qa/xlet
app.80.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSLiveApp1Xlet
app.80.service=0x12357
app.80.args.0=config_file=config.properties

#################################################
## Application 81 – PSDeadApp1Xlet
#################################################
app.81.application_identifier=0x000000017249
app.81.application_control_code=PRESENT
app.81.visibility=INVISIBLE
app.81.priority=190
app.81.application_name=PSDeadApp1Xlet
app.81.base_directory=/snfs/qa/xlet
app.81.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSDeadApp1Xlet
app.81.service=0x12357
app.81.args.0=config_file=config.properties

#################################################
## Application 82 – PSTest1Xlet
#################################################
app.82.application_identifier=0x00000001724A
app.82.application_control_code=PRESENT
app.82.visibility=VISIBLE
app.82.priority=220
app.82.application_name= PSTest1Xlet
app.82.base_directory=/snfs/qa/xlet
app.82.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSTest1Xlet
app.82.service=0x12357
app.82.args.0=config_file=config.properties

#################################################
## Application 83 – PSTest2Xlet
#################################################
app.83.application_identifier=0x00000001724B
app.83.application_control_code=PRESENT
app.83.visibility=VISIBLE
app.83.priority=205
app.83.application_name= PSTest2Xlet
app.83.base_directory=/snfs/qa/xlet
app.83.initial_class_name=org.cablelabs.xlet.PersistentStoragePriority.PSTest2Xlet
app.83.service=0x12357
app.83.args.0=config_file=config.properties

In the config file
# Persistent Storage Properties
# Xlet Writers
Kbytes=55
AppName7244=PSLiveApp3Xlet
AppName7245=PSDeadApp3Xlet
AppName7246=PSLiveApp2Xlet
AppName7247=PSDeadApp2Xlet
AppName7248=PSLiveApp1Xlet
AppName7249=PSDeadApp1Xlet

# Xlet Gobblers
MaxDeleted724a=18
ChunksWritten724a=18
Filename724a=usefile1.dat
AppName724a=PSTest1Xlet

MaxDeleted724b=12
ChunksWritten724b=13
Filename724b=usefile2.dat
AppName724b=PSTest2Xlet

*******************************************************************************************
							Procedure
*******************************************************************************************
To run this test manually, the following configurations must be in place.

1) Start PSTest1Xlet

2) Press 7 to make sure none of the write xlets have been executed

3) Press 1 to makes sure no usr files are present.

4) Press 2 to generate out the files from the writer xlets

5) Press 3 to destroy the dead xlets

6) Press 4 to start the gobbler xlet. The necessary files will be first checked before
	purging and then purging file by file will occur. Check for FAILED messages on the 
	screen and in the logging output. Passing should return no

To run this test in automation, use the script in the XML file in the source dir. 
Use the items in configuration for testing and include in those items for AutoXlet.
