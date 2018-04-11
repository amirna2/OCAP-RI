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


/**
 *
 * MPE / MPEOS function names are re-defined here using macros, in order to
 * support MPE or MPEOS tests using the same test code.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */

#ifndef _MPETEST_NET_H_
#define _MPETEST_NET_H_ 1

#include <mpetest_dbg.h>
#include <netmgr.h>
#include <mpe_socket.h>

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
# include <mpeos_socket.h>
# define MPETEST_NET(x)  mpeos_ ## x
#else
# include "mpe_sys.h"
# define MPETEST_NET(x)  mpe_ ## x
#endif /* TEST_MPEOS */

#define socketInit             MPETEST_NET(socketInit)
#define socketTerm             MPETEST_NET(socketTerm)
#define socketGetLastError     MPETEST_NET(socketGetLastError)
#define socketAccept           MPETEST_NET(socketAccept)
#define socketBind             MPETEST_NET(socketBind)
#define socketClose            MPETEST_NET(socketClose)
#define socketConnect          MPETEST_NET(socketConnect)
#define socketCreate           MPETEST_NET(socketCreate)
#define socketFDClear          MPETEST_NET(socketFDClear)
#define socketFDIsSet          MPETEST_NET(socketFDIsSet)
#define socketFDSet            MPETEST_NET(socketFDSet)
#define socketFDZero           MPETEST_NET(socketFDZero)
#define socketGetHostByAddr    MPETEST_NET(socketGetHostByAddr)
#define socketGetHostByName    MPETEST_NET(socketGetHostByName)
#define socketGetHostName      MPETEST_NET(socketGetHostName)
#define socketGetSockName      MPETEST_NET(socketGetSockName)
#define socketGetOpt           MPETEST_NET(socketGetOpt)
#define socketGetPeerName      MPETEST_NET(socketGetPeerName)
#define socketHtoNL            MPETEST_NET(socketHtoNL)
#define socketHtoNS            MPETEST_NET(socketHtoNS)
#define socketNtoHL            MPETEST_NET(socketNtoHL)
#define socketNtoHS            MPETEST_NET(socketNtoHS)
#define socketIoctl            MPETEST_NET(socketIoctl)
#define socketListen           MPETEST_NET(socketListen)
#define socketAtoN             MPETEST_NET(socketAtoN)
#define socketNtoA             MPETEST_NET(socketNtoA)
#define socketRecv             MPETEST_NET(socketRecv)
#define socketRecvFrom         MPETEST_NET(socketRecvFrom)
#define socketSelect           MPETEST_NET(socketSelect)
#define socketSend             MPETEST_NET(socketSend)
#define socketSendTo           MPETEST_NET(socketSendTo)
#define socketSetOpt           MPETEST_NET(socketSetOpt)
#define socketShutdown         MPETEST_NET(socketShutdown)
#define socketNtoP             MPETEST_NET(socketNtoP)
#define socketPtoN             MPETEST_NET(socketPtoN)

#endif /* _MPETEST_NET_H_ */ 
