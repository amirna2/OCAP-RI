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
This app is capable of allocating resources that are intended to trigger
resource reclamation procedures.  

Configuration Parameters:
-none-

Test Control:
-none-

AppID: 
0x000000016099

********************************************************************************
The app presents a very simple UI:

* At the bottom of the screen is a report of Free/Used/Total memory.
  This reports the amount of memory that is free, in-use, or overall
  in the Java Heap.

* Above this are navigable buttons/labels.

======================
Garbage: ?????? bytes
======================

Select this button to allocate ??????? bytes of garbage.
Use the CH+/CH- buttons to adjust the amount ???????.
This allocates from the Java Heap.

=========================
Garbage: ??????x10 Image
=========================

Select this button to create a ??????x10 (x4) Image.  
Use the CH+/CH- buttons to adjust the amount ??????.
This allocates mainly from the native (GFX) heap.

========================
Memory [XXXX]: ??
=======================

Indicates the amount of "leaked" memory (it is XXXX x ?? bytes).
This memory will not be freed by a GC.
This memory will be freed by a ResourceDepletionEvent.
Use the CH+/CH- to adjust ??.
This allocates from the Java Heap.

========================
Images (320x240): ??
=======================

Indicates the amount of "leaked" images.
This memory will not be freed by a GC.
Use the CH+/CH- to adjust ??.
This allocates mainly from the native (GFX) heap.

========================
Threads: ??
=======================

Indicates the amount of running ("leaked") threads.
This memory will not be freed by a GC.
Use the CH+/CH- to adjust ??.
This allocates mainly from the native heap.

=======================
Garbage Collect...
=======================

Invokes System.gc();

=======================
Garbage Collect...
=======================

Invokes System.runFinalization();

=======================
Install Handler
=======================

Install a SystemEventListener to receive ResourceDepletionEvents.
When invoked, will purge "leaked" byte[], Images, and Threads.
Use CH+/CH- to adjust the amount of time spent in this routine.

=======================
Remove Handler
=======================

Remove the SystemEventHandler.


--------------------------------------------------------------------------

Some things to do:

1. Create a lot of Garbage.  Eventually, the asynchronous threshold
   monitor should kick-in and clean up that garbage.
2. Leak a bunch of memory.  Eventually, the asynchronous threshold
   monitor should kick-in.  
   Unless there is a ResourceDepletionEvent handler installed, 
   this will continue to be "leaked".
