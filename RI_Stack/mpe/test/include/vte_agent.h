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

#ifndef VTE_AGENT_H
#define VTE_AGENT_H 1

#include <mpe_types.h>
#include <mpe_socket.h>

/**
 * Preprocessor definitions for vate.h.
 *
 * @preproc VTE_MAX_FILE_NAME_LEN Maximum length a filename can be.
 * @preproc VTE_MAX_PATH_LEN Maximum length a path can be.
 * @preproc VTE_MAX_TEST_NUM Maximum number of tests (suggested).
 *
 * See VATE Command.java file for any changes to the following values.
 * Commands that can be sent to the VTE agent.
 * @preproc QUIT No data, quit.
 * @preproc RUN 2-byte length, Run the test, follow the TestCase URL.
 * @preproc ABORT Abort the previous RUN command.
 *
 * Responses that come from the agent.
 * @preproc ACK Previous command was received.
 * @preproc NACK Previous command was not received (retransmitt).
 * @preproc DONE Done command from the agent.
 * @preproc ABORTED The agent aborted.
 * @preproc LOGMSG Loged message.
 * @preproc CAPTURE Capture the data from the client.
 * @preproc SLEEP Client must sleep.
 */
#define VTE_MAX_FILE_NAME_LEN  256
#define VTE_MAX_PATH_LEN       1024
#define VTE_MAX_TEST_NUM       10240
#define VTE_MAX_BUFFER_SIZE    2048
#define VTE_MAX_STRLEN         5120

#define VTE_QUIT      100
#define VTE_RUN       110
#define VTE_ABORT     120

#define VTE_ACK       130
#define VTE_NACK      140
#define VTE_DONE      150
#define VTE_ABORTED   160
#define VTE_LOGMSG    170
#define VTE_CAPTURE   180
#define VTE_SLEEP     190

/* Prototypes created in Vte_Agent.cpp.
 */

mpe_Bool vte_agent_Log(const char* format, ...);

#endif /* VTE_AGENT_H */
