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

/****************************************************************************************
 *
 * The MPE Sockets API. This API provides a consistent interface to BSD style sockets
 * regardless of the underlying operating system.
 *
 * @author Todd Earles - Vidiom Systems Corporation
 *
 ***************************************************************************************/

#ifndef _MPE_SOCKET_H_
#define _MPE_SOCKET_H_

#include "mpe_sys.h"
#include "../mgr/include/mgrdef.h"
#include "../mgr/include/netmgr.h"

#define mpe_netmgr_ftable ((mpe_net_ftable_t*)(FTABLE[MPE_MGR_TYPE_NET]))

#define mpe_netInit (mpe_netmgr_ftable->mpe_net_init_ptr)

#define mpe_socketInit (mpe_netmgr_ftable->mpe_socketInit_ptr)
#define mpe_socketTerm (mpe_netmgr_ftable->mpe_socketTerm_ptr)
#define mpe_socketGetLastError (mpe_netmgr_ftable->mpe_socketGetLastError_ptr)
#define mpe_socketAccept (mpe_netmgr_ftable->mpe_socketAccept_ptr)
#define mpe_socketBind (mpe_netmgr_ftable->mpe_socketBind_ptr)
#define mpe_socketClose (mpe_netmgr_ftable->mpe_socketClose_ptr)
#define mpe_socketConnect (mpe_netmgr_ftable->mpe_socketConnect_ptr)
#define mpe_socketCreate (mpe_netmgr_ftable->mpe_socketCreate_ptr)
#define mpe_socketFDClear (mpe_netmgr_ftable->mpe_socketFDClear_ptr)
#define mpe_socketFDIsSet (mpe_netmgr_ftable->mpe_socketFDIsSet_ptr)
#define mpe_socketFDSet (mpe_netmgr_ftable->mpe_socketFDSet_ptr)
#define mpe_socketFDZero (mpe_netmgr_ftable->mpe_socketFDZero_ptr)
#define mpe_socketGetHostByAddr (mpe_netmgr_ftable->mpe_socketGetHostByAddr_ptr)
#define mpe_socketGetHostByName (mpe_netmgr_ftable->mpe_socketGetHostByName_ptr)
#define mpe_socketGetHostName (mpe_netmgr_ftable->mpe_socketGetHostName_ptr)
#define mpe_socketGetSockName (mpe_netmgr_ftable->mpe_socketGetSockName_ptr)
#define mpe_socketGetOpt (mpe_netmgr_ftable->mpe_socketGetOpt_ptr)
#define mpe_socketGetPeerName (mpe_netmgr_ftable->mpe_socketGetPeerName_ptr)
#define mpe_socketHtoNL (mpe_netmgr_ftable->mpe_socketHtoNL_ptr)
#define mpe_socketHtoNS (mpe_netmgr_ftable->mpe_socketHtoNS_ptr)
#define mpe_socketNtoHL (mpe_netmgr_ftable->mpe_socketNtoHL_ptr)
#define mpe_socketNtoHS (mpe_netmgr_ftable->mpe_socketNtoHS_ptr)
#define mpe_socketIoctl (mpe_netmgr_ftable->mpe_socketIoctl_ptr)
#define mpe_socketListen (mpe_netmgr_ftable->mpe_socketListen_ptr)
#define mpe_socketAtoN (mpe_netmgr_ftable->mpe_socketAtoN_ptr)
#define mpe_socketNtoA (mpe_netmgr_ftable->mpe_socketNtoA_ptr)
#define mpe_socketRecv (mpe_netmgr_ftable->mpe_socketRecv_ptr)
#define mpe_socketRecvFrom (mpe_netmgr_ftable->mpe_socketRecvFrom_ptr)
#define mpe_socketSelect (mpe_netmgr_ftable->mpe_socketSelect_ptr)
#define mpe_socketSend (mpe_netmgr_ftable->mpe_socketSend_ptr)
#define mpe_socketSendTo (mpe_netmgr_ftable->mpe_socketSendTo_ptr)
#define mpe_socketSetOpt (mpe_netmgr_ftable->mpe_socketSetOpt_ptr)
#define mpe_socketShutdown (mpe_netmgr_ftable->mpe_socketShutdown_ptr)
#define mpe_socketGetInterfaces (mpe_netmgr_ftable->mpe_socketGetInterfaces_ptr)
#define mpe_socketFreeInterfaces (mpe_netmgr_ftable->mpe_socketFreeInterfaces_ptr)
#define mpe_socketNtoP (mpe_netmgr_ftable->mpe_socketNtoP_ptr)
#define mpe_socketPtoN (mpe_netmgr_ftable->mpe_socketPtoN_ptr)
#define mpe_socketGetAddrInfo (mpe_netmgr_ftable->mpe_socketGetAddrInfo_ptr)
#define mpe_socketFreeAddrInfo (mpe_netmgr_ftable->mpe_socketFreeAddrInfo_ptr)
#define mpe_getDLNANetworkInterfaceInfo (mpe_netmgr_ftable->mpe_getDLNANetworkInterfaceInfo_ptr)
#define mpe_getDLNANetworkInterfaceMode (mpe_netmgr_ftable->mpe_getDLNANetworkInterfaceMode_ptr)
#define mpe_socketSetLinkLocalAddress (mpe_netmgr_ftable->mpe_socketSetLinkLocalAddress_ptr)
#define mpe_socketRegisterForIPChanges (mpe_netmgr_ftable->mpe_socketRegisterForIPChanges_ptr)
#endif
