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

#include <test_interface.h>

// 
// ri_test_RegisterMenu  registers new menus with the RI test interface
//                       infrastructure.
//
// pMenuItem             pointer to the MenuItem struct to register with the
//                       RI test interface
// returns               success/fail of registration operation
//
ri_bool ri_test_RegisterMenu(MenuItem *pMenuItem)
{
    return test_RegisterMenu(pMenuItem);
}

// 
// ri_test_SetNextMenu   selects the provided menu for continued operation;
//                       i.e. used to traverse up or down in the menu tree
//
// sock                  the socket of the input stream
// index                 the index (returned from FindMenu) of the menu to
//                       select
// returns               success/fail of selection operation
//
ri_bool ri_test_SetNextMenu(int sock, int index)
{
    return test_SetNextMenu(sock, index);
}

// 
// ri_test_FindMenu      finds a menu within the RI test interface
//                       infrastructure.
//
// title                 title of the menu to find
// returns               the index of the menu (or MAX_MENUS if not found)
//
int ri_test_FindMenu(char *title)
{
    return test_FindMenu(title);
}

// 
// ri_test_GetString     gets 'size' bytes from the input stream if open
//
// sock                  the socket of the input stream
// buf                   the char buffer to place the received bytes into
// size                  the size of the output char buffer (described above)
// prompt                an optional string to display before getting input
// returns               the number of bytes read
//
int ri_test_GetString(int sock, char *buf, int size, char *prompt)
{
    return test_GetString(sock, buf, size, prompt);
}

// 
// ri_test_GetNumber     gets an integer from the input stream if open
//
// sock                  the socket of the input stream
// buf                   the char buffer to place the received bytes into
// size                  the size of the output char buffer (described above)
// prompt                an optional string to display before getting input
// dfault                the default value to return if obtaining a number fails
// returns               the number obtained or dfault (described above)
//
int ri_test_GetNumber(int sock, char *buf, int size, char *prompt, int dfault)
{
    return test_GetNumber(sock, buf, size, prompt, dfault);
}

// 
// ri_test_SendString    sends a string to the output stream if open
//
// sock                  the socket of the output stream
// string                the char buffer to send
//
void ri_test_SendString(int sock, char *string)
{
    return test_SendString(sock, string);
}

