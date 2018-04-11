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
 * The networking manager definitions
 * 
 * @author Todd Earles - Vidiom Systems Corporation
 *
 ***************************************************************************************/

#ifndef _NETMGR_H_
#define _NETMGR_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpeos_socket.h>

void mpe_netSetup(void);
void mpe_netInit(void);

typedef struct
{
    void (*mpe_net_init_ptr)(void);

    mpe_Bool (*mpe_socketInit_ptr)(void);
    void (*mpe_socketTerm_ptr)(void);
    int (*mpe_socketGetLastError_ptr)(void);
    mpe_Socket (*mpe_socketAccept_ptr)(mpe_Socket socket,
            mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len);
    int (*mpe_socketBind_ptr)(mpe_Socket socket,
            const mpe_SocketSockAddr *address, mpe_SocketSockLen address_len);
    int (*mpe_socketClose_ptr)(mpe_Socket socket);
    int (*mpe_socketConnect_ptr)(mpe_Socket socket,
            const mpe_SocketSockAddr *address, mpe_SocketSockLen address_len);
    mpe_Socket (*mpe_socketCreate_ptr)(int domain, int type, int protocol);
    void (*mpe_socketFDClear_ptr)(mpe_Socket fd, mpe_SocketFDSet *fdset);
    int (*mpe_socketFDIsSet_ptr)(mpe_Socket fd, mpe_SocketFDSet *fdset);
    void (*mpe_socketFDSet_ptr)(mpe_Socket fd, mpe_SocketFDSet *fdset);
    void (*mpe_socketFDZero_ptr)(mpe_SocketFDSet *fdset);
    mpe_SocketHostEntry* (*mpe_socketGetHostByAddr_ptr)(const void *addr,
            mpe_SocketSockLen len, int type);
    mpe_SocketHostEntry* (*mpe_socketGetHostByName_ptr)(const char *name);
    int (*mpe_socketGetHostName_ptr)(char *name, size_t namelen);
    int (*mpe_socketGetSockName_ptr)(mpe_Socket socket,
            mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len);
    int (*mpe_socketGetOpt_ptr)(mpe_Socket socket, int level, int option_name,
            void *option_value, mpe_SocketSockLen *option_len);
    int (*mpe_socketGetPeerName_ptr)(mpe_Socket socket,
            mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len);
    uint32_t (*mpe_socketHtoNL_ptr)(uint32_t hostlong);
    uint16_t (*mpe_socketHtoNS_ptr)(uint16_t hostshort);
    uint32_t (*mpe_socketNtoHL_ptr)(uint32_t netlong);
    uint16_t (*mpe_socketNtoHS_ptr)(uint16_t netshort);
    int (*mpe_socketIoctl_ptr)(mpe_Socket socket, int request, ...);
    int (*mpe_socketListen_ptr)(mpe_Socket socket, int backlog);
    int (*mpe_socketAtoN_ptr)(const char *strptr, mpe_SocketIPv4Addr *addrptr);
    char* (*mpe_socketNtoA_ptr)(mpe_SocketIPv4Addr inaddr);
    size_t (*mpe_socketRecv_ptr)(mpe_Socket socket, void *buffer,
            size_t length, int flags);
    size_t (*mpe_socketRecvFrom_ptr)(mpe_Socket socket, void *buffer,
            size_t length, int flags, mpe_SocketSockAddr *address,
            mpe_SocketSockLen *address_len);
    int (*mpe_socketSelect_ptr)(int numfds, mpe_SocketFDSet *readfds,
            mpe_SocketFDSet *writefds, mpe_SocketFDSet *errorfds,
            const mpe_TimeVal *timeout);
    size_t (*mpe_socketSend_ptr)(mpe_Socket socket, const void *buffer,
            size_t length, int flags);
    size_t (*mpe_socketSendTo_ptr)(mpe_Socket socket, const void *message,
            size_t length, int flags, const mpe_SocketSockAddr *dest_addr,
            mpe_SocketSockLen dest_len);
    int (*mpe_socketSetOpt_ptr)(mpe_Socket socket, int level, int option_name,
            const void *option_value, mpe_SocketSockLen option_len);
    int (*mpe_socketShutdown_ptr)(mpe_Socket socket, int how);
    mpe_Error (*mpe_socketGetInterfaces_ptr)(mpe_SocketNetIfList **netIfList);
    void (*mpe_socketFreeInterfaces_ptr)(mpe_SocketNetIfList *netIfList);
    const char* (*mpe_socketNtoP_ptr)(int af, const void *src, char *dst, size_t size);
    int (*mpe_socketPtoN_ptr)(int af, const char *src, void *dst);
    int (*mpe_socketGetAddrInfo_ptr)(const char* addr, const char* port,
                                     mpe_SocketAddrInfo* hints,
                                     mpe_SocketAddrInfo** result);
    void (*mpe_socketFreeAddrInfo_ptr)(mpe_SocketAddrInfo* ai);
    mpe_Error (*mpe_getDLNANetworkInterfaceInfo_ptr)(char* interfaceName, 
                                                     mpeos_LpeDlnaNetworkInterfaceInfo* dlnaNetworkIfInfo);
    mpe_Error (*mpe_getDLNANetworkInterfaceMode_ptr)(char* interfaceName, char** lpeDlnaNetworkInterfaceModeInfo);
    mpe_Error (*mpe_socketSetLinkLocalAddress_ptr)(char *);
    mpe_Error (*mpe_socketRegisterForIPChanges_ptr)(char *, mpe_EventQueue, void *);


} mpe_net_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* _NETMGR_H_ */
