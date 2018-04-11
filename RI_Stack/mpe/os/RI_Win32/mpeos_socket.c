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

/*
 *
 * Implementation of MPE OS Sockets API for the CableLabs Reference Implementation
 * (RI) platform.
 *
 * For ease of maintenance the complete description of all socket functions are commented
 * in mpeos_socket.h and not in each of the platform specific implementation files such
 * as this one.
 *
 */

/* Header Files */
#include "winsock2.h"
#include <IPHlpApi.h>
#include <windows.h>

#include <mpe_types.h>
#include <mpeos_socket.h>
#include <mpeos_dbg.h>
#include <mpeos_util.h>
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <sys/time.h>

#include <ri_cablecard.h>

// The Microsoft recommended size of the address array to begin with.
#define WORKING_BUFFER_SIZE 15000
// The number of tries to obtain the address array.
#define MAX_TRIES 3

#define MAX_CANCELLED_TESTS 10
int gCancelledTests[MAX_CANCELLED_TESTS]={0};
mpe_Mutex cancelledTestMutex;

// IP change eventing
static mpe_EventQueue g_ipChangeQueueId = (mpe_EventQueue) -1;
static void *g_ipChangeEdHandle = NULL;
static char g_interface[32];

void addCancelledTest(int id)
{
   mpe_mutexAcquire(cancelledTestMutex);
   int i = 0;
   for (i = 0; i < MAX_CANCELLED_TESTS; i++)
   {
       if (gCancelledTests[i] == 0)
       {
           gCancelledTests[i]=id;
       }
   }
   mpe_mutexRelease(cancelledTestMutex);

}
void removeCancelledTest(int id)
{
   mpe_mutexAcquire(cancelledTestMutex);
   int i = 0;
   for (i = 0; i < MAX_CANCELLED_TESTS; i++)
   {
       if (gCancelledTests[i] == id)
       {
           gCancelledTests[i]=0;
       }
   }
   mpe_mutexRelease(cancelledTestMutex);

}
int isTestCancelled(int id)
{
   int retVal = 0;
   int i = 0;
   mpe_mutexAcquire(cancelledTestMutex);
   for (i = 0; i < MAX_CANCELLED_TESTS; i++)
   {
       if (gCancelledTests[i] == id)
       {
           retVal = 1;
       }
   }
   mpe_mutexRelease(cancelledTestMutex);
   return retVal;
}

/**
 * Get the MAC address of the current machine if no display name is supplied.
 * If a network interface display name is supplied, find that network interface
 * and return the physical address associated with that interface.
 *
 * @param displayName   name of specific network interface, if null, look for
 *                      first interface which has an associated physical address.
 * @param buf the buffer into which the MAC address should be written (returned).
 *             maybe null if no physical address is associated with specified interface
 *             name.
 *
 * @return 0 if successful, -1 if problems encountered
 */
int os_getMacAddress(char* displayName, char* buf)
{
    int retCode = -1;

    /* Get Ethernet interface information from the OS (since we don't have a real RF interface on the Simulator) */
    /* Note: this string will be in the format "12:34:56:78:9A:BC" */

    /* NOTE: Netbios is no longer supported. The implementation below should be forward compatible */
    /* with Windows XP, Vista and Windows 7.                                                       */

    /* Declare and initialize variables. */
    DWORD dwRetVal = 0;

    // Set the flags to pass to GetAdaptersAddresses.
    ULONG flags = GAA_FLAG_INCLUDE_PREFIX;

    // Default to unspecified address family (both).
    ULONG family = AF_INET;

    LPVOID lpMsgBuf = NULL;

    PIP_ADAPTER_ADDRESSES pAddresses = NULL;
    ULONG outBufLen = 0;
    ULONG iterations = 0;

    PIP_ADAPTER_ADDRESSES pCurrAddresses = NULL;
    //PIP_ADAPTER_UNICAST_ADDRESS pUnicast = NULL;
    //PIP_ADAPTER_ANYCAST_ADDRESS pAnycast = NULL;
    //PIP_ADAPTER_MULTICAST_ADDRESS pMulticast = NULL;
    //IP_ADAPTER_DNS_SERVER_ADDRESS *pDnServer = NULL;
    //IP_ADAPTER_PREFIX *pPrefix = NULL;

    // Allocate a 15 KB buffer to start with.
    outBufLen = WORKING_BUFFER_SIZE;

    char *NO_MAC = "00:00:00:00:00:00";

    // Get the list of interfaces
    mpe_SocketNetIfList *netIfList = NULL;
    int index = -1;
    if (displayName != NULL)
    {
        if (mpeos_socketGetInterfaces((mpe_SocketNetIfList**)&netIfList) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                    "os_getMacAddress: Unable to get interfaces\n");
            return -1;
        }

        //  Look for a matching display name, starting with the first
        mpe_SocketNetIfList* curNetIfList = netIfList;
        do
        {
            if (strcmp(displayName, curNetIfList->if_name) == 0)
            {
                // Found matching network interface, get index
                index = curNetIfList->if_index;
                break;
            }
        }
        while ((curNetIfList = curNetIfList->if_next) != NULL);

        mpeos_socketFreeInterfaces(netIfList);
    }

    // Attempt to retrieve the IP Adapter addresses. Try at least MAX_TRIES times before
    // giving up.
    do
    {
        pAddresses = (IP_ADAPTER_ADDRESSES *) malloc(outBufLen);
        if (pAddresses == NULL)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                    "os_getMacAddress: Memory allocation failed for IP_ADAPTER_ADDRESSES struct\n");
            return -1;
        }

        dwRetVal = GetAdaptersAddresses(family, flags, NULL, pAddresses,
                &outBufLen);

        if (dwRetVal == ERROR_BUFFER_OVERFLOW)
        {
            free(pAddresses);
            pAddresses = NULL;
        }
        else
        {
            break;
        }

        iterations++;

    } while ((dwRetVal == ERROR_BUFFER_OVERFLOW) && (iterations < MAX_TRIES));

    if (dwRetVal == NO_ERROR)
    {
        // Walk through all found IP adapters. The last one found will be the
        // MAC address that is returned. TODO: provide a mechanism for returning
        // a specific entry.
        pCurrAddresses = pAddresses;
        while (pCurrAddresses)
        {
            if ((index == -1) || (index == pCurrAddresses->IfIndex))
            {
                if (pCurrAddresses->PhysicalAddressLength != 0)
                {
                    // Get Ethernet information.
                    sprintf(buf, "%02x:%02x:%02x:%02x:%02x:%02x",
                            pCurrAddresses->PhysicalAddress[0],
                            pCurrAddresses->PhysicalAddress[1],
                            pCurrAddresses->PhysicalAddress[2],
                            pCurrAddresses->PhysicalAddress[3],
                            pCurrAddresses->PhysicalAddress[4],
                            pCurrAddresses->PhysicalAddress[5]);

                    // If successful, output some info from the data we received
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                        "os_getMacAddress: Physical address found: %s\n", buf);
                    retCode = 0;
                    break;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                             "os_getMacAddress: physical address length for index = %d was 0\n", index);
                    retCode = 0;
                    strcpy(buf, NO_MAC); 
                    break;
                }
            }
            pCurrAddresses = pCurrAddresses->Next;
        }
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_NET,
                "os_getMacAddress: Call to GetAdaptersAddresses failed with error: %d\n",
                dwRetVal);
        if (dwRetVal == ERROR_NO_DATA)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                    "\tNo adapter addresses were found for the requested parameters.\n");
        }
        else
        {
            if (FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER
                    | FORMAT_MESSAGE_FROM_SYSTEM
                    | FORMAT_MESSAGE_IGNORE_INSERTS, NULL, dwRetVal,
                    MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                    // Default language
                    (LPTSTR) & lpMsgBuf, 0, NULL))
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET, "\tError %s", lpMsgBuf);
                (void) LocalFree(lpMsgBuf);
                if (pAddresses)
                    free(pAddresses);
                return retCode;
            }
        }
    }

    // Clean up.
    if (pAddresses)
    {
        free(pAddresses);
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "os_getMacAddress: Returning MAC address %s, ret code %d\n", buf, retCode);

    return retCode;
}

/**
 * Initialize the MPE OS socket API
 *
 * @return If the Networking API is successfully initialized, this routine will
 * return <b>TRUE</b>. If the socket subsystem could not be initialized, then
 * <b>FALSE</b> will be returned.
 */
mpe_Bool mpeos_socketInit(void)
{
    WORD wVersionRequested = MAKEWORD(2, 0);
    WSADATA wsaData;
    int err;

    mpeos_mutexNew(&cancelledTestMutex);
    err = WSAStartup(wVersionRequested, &wsaData);
    if (err != 0)
    {
        // Could not find a usable WinSock DLL
        return FALSE;
    }

    // Confirm that the WinSock DLL supports 2.0. Note that if the DLL supports
    // versions greater than 2.0 in addition to 2.0, it will still return 2.0
    // in wVersion since that is the version we requested.
    if (LOBYTE(wsaData.wVersion) != 2 || HIBYTE(wsaData.wVersion) != 0)
    {
        // Could not find a usable WinSock DLL
        WSACleanup();
        return FALSE;
    }
    return TRUE;
}

/**
 * Terminate the MPE OS socket API.
 */
void mpeos_socketTerm(void)
{
    // FINISH - deal with errors from WSACleanup()
    WSACleanup();
}

/**
 * Return status of last socket function that failed.
 */
int mpeos_socketGetLastError(void)
{
    return WSAGetLastError();
}

/**
 * Accept a connection.
 *
 * @param socket Specifies the socket that was created with <code>mpeos_socketCreate()</code>.
 * @param address An output pointer to the <code>mpe_SocketSockAddr</code> where the address of the
 * connecting socket should be returned. May be <b>NULL</b>.
 * @param address_len An output pointer to a length parameter specifying the length of the
 * supplied <code>mpe_SocketSockAddr</code>.
 *
 * @return If successful, the non-negative file descriptor of the accepted socket will be returned.
 * Otherwise, this routine will return <code>MPE_SOCKET_INVALID_SOCKET</code>.
 */
mpe_Socket mpeos_socketAccept(mpe_Socket mpeSocket,
        mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len)
{
#ifdef MPE_FEATURE_DEBUG
    mpe_Socket s = accept(mpeSocket, address, address_len);
    if ( s == MPE_SOCKET_INVALID_SOCKET )
    {
        int err = WSAGetLastError();
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketAccept: error occurred, result = %d, error = %d\n", s, err);
        WSASetLastError(err);
    }
    else
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketAccept: socket = %x, new socket = %x\n", mpeSocket, s);

    return s;
#else
    return accept(mpeSocket, address, address_len);
#endif
}

/**
 * Bind a name to a socket.
 *
 * @param socket Specifies the file descriptor of the socket to be bound.
 * @param address An input pointer to the <code>mpe_SocketSockAddr</code> containing
 * the address to be bound to the socket.
 * @param address_len Specifies the length of the supplied <code>mpe_SocketSockAddr</code>.
 *
 * @return If successful, 0 shall be returned.
 * Otherwise, this routine will return -1.
 */
int mpeos_socketBind(mpe_Socket mpeSocket, const mpe_SocketSockAddr *address,
        mpe_SocketSockLen address_len)
{
#ifdef MPE_FEATURE_DEBUG
    int i = bind(mpeSocket, address, address_len);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketBind: socket = %0x, result = %d\n", mpeSocket, i);
    return i;
#else
    return bind(mpeSocket, address, address_len);
#endif
}

/**
 * Close a socket.
 *
 * @param socket Specifies the file descriptor associated with the socket to be closed.
 *
 * @return If the call is successful, this routine will return 0. Otherwise, -1 shall
 * be returned.
 */
int mpeos_socketClose(mpe_Socket mpeSocket)
{
    return closesocket(mpeSocket);
}

/**
 * Make a connection on a socket.
 *
 * @param socket Specifies the file descriptor of the socket in which to make the connection.
 * @param address An input pointer to the <code>mpe_SocketSockAddr</code> containing
 * the peer address.
 * @param address_len Specifies the length of the supplied <code>mpe_SocketSockAddr</code>.
 *
 * @return If successful, 0 shall be returned.
 * Otherwise, this routine will return -1.
 */
int mpeos_socketConnect(mpe_Socket mpeSocket,
        const mpe_SocketSockAddr *address, mpe_SocketSockLen address_len)
{
#ifdef MPE_FEATURE_DEBUG
    {
        int c = connect(mpeSocket, address, address_len);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketConnect: result = %x, socket = %x, address = %x\n", c, mpeSocket, *(int*)address);
        return c;
    }
#else
    return connect(mpeSocket, address, address_len);
#endif
}

/**
 * Create a socket.
 *
 * @param domain Specifies the address family used in the communications domain in which
 * a socket is to be created.
 * @param type Specifies the type of socket to be created, which determine the semantics
 * of communication over the socket.
 * @param protocol Specifies the protocol to be used with the socket.
 *
 * @return Of successful, then a valid socket descriptor is returned. Otherwise, this
 * routine will return <code>MPE_SOCKET_INVALID_SOCKET</code>.
 */
mpe_Socket mpeos_socketCreate(int domain, int type, int protocol)
{
#ifdef MPE_FEATURE_DEBUG
    {
        mpe_Socket s = socket(domain, type, protocol);
        if ( s == MPE_SOCKET_INVALID_SOCKET )
        {
            int err = WSAGetLastError();
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketCreate: error = %d, domain = %d, type = %d, protocol = %d\n"
                    , err, domain, type, protocol);
            WSASetLastError(err);
        }
        else
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketCreate: socket = %x\n", s);
        return s;
    }
#else
    return socket(domain, type, protocol);
#endif
}

/**
 * Clear an FD from an FD set.
 *
 * @param fd Specifies the file descriptor to be removed from <i>fdset</i>.
 * @param fdset A pointer to the file descriptor set to be modified.
 */
void mpeos_socketFDClear(mpe_Socket fd, mpe_SocketFDSet *fdset)
{
    if (fdset == NULL)
    {
        return;
    }

    FD_CLR(fd, fdset);
}

/**
 * Determine if an FD is a member of an FD set.
 *
 * @param fd Specifies the file descriptor within <i>fdset</i> to be checked.
 * @param fdset A pointer to the file descriptor set to be checked.
 *
 * @return <b>TRUE</B> will be returned if <i>fd</i> is a member of the <i>fdset</i>.
 * Otherwise <b>FALSE</b> will be returned if <i>fd</i> is not a member of the <i>fdset</i>.
 */
int mpeos_socketFDIsSet(mpe_Socket fd, mpe_SocketFDSet *fdset)
{
    if (fdset == NULL)
    {
        return 0;
    }

    return FD_ISSET(fd, fdset);
}

/**
 * Add an FD to an FD set
 *
 * @param fd  Specifies the file descriptor to be added to the <i>fdset</i>.
 * @param fdset A pointer to the file descriptor set to be modified.
 */
void mpeos_socketFDSet(mpe_Socket fd, mpe_SocketFDSet *fdset)
{
    if (fdset == NULL)
    {
        return;
    }

    FD_SET(fd, fdset);
}

/**
 * Clear all entries from an FD set.
 *
 * @param fdset A pointer to a file descriptor set to initialize to 0.
 */
void mpeos_socketFDZero(mpe_SocketFDSet *fdset)
{
    if (fdset == NULL)
    {
        return;
    }

    FD_ZERO(fdset);
}

/**
 * Obtain one or more mpe_SocketAddrInfo structures, each of which
 * contains an informative mpe_SocketSockAddr structure.
 *
 * @param addr specifies either a numerical network address (v4 or v6), or a
 *        network hostname, whose network addresses are looked up and resolved.
 *        If hints.ai_flags contains the AI_NUMERICHOST flag then addr must be
 *        a numerical network address.
 *
 * @param port sets the port in each returned address structure.  If this
 *        argument is a service name, it is translated to the corresponding
 *        port number. If port is NULL, then the port number of the returned
 *        socket addresses will be left uninitialized. If AI_NUMERICSERV is
 *        specified in hints.ai_flags and port is not NULL, then port must
 *        point to a string containing a numeric port number.
 *
 *        Either addr or port, but not both, may be NULL.
 *
 * @param hints (if not NULL) points to a mpe_SocketAddrInfo structure whose
 *        ai_family, ai_socktype, and ai_protocol specify criteria that limit
 *        the set of socket addresses returned by mpeos_socketGetAddrInfo().
 *        Specifying hints as NULL is equivalent to setting ai_socktype and
 *        ai_protocol to 0; ai_family to AF_UNSPEC; and ai_flags to
 *        (AI_V4MAPPED | AI_ADDRCONFIG).
 *
 * @param result a pointer to a linked list of mpe_SocketAddrInfo structures,
 *        one for each network address that matches addr and port, subject to
 *        any restrictions imposed by hints. The items in the linked list are
 *        linked by the ai_next field.  mpeos_socketFreeAddrInfo() must be
 *        used to release the memory allocated and returned in result
 *
 * @return 0 on succes, or one of the following nonzero error codes:
 *         EAI_ADDRFAMILY The specified network host does not have any
 *         network addresses in the requested address family.
 *
 *         EAI_AGAIN The name server returned a temporary failure indication.
 *         Try again later.
 *
 *         EAI_BADFLAGS hints.ai_flags contains invalid flags; or,
 *         hints.ai_flags included AI_CANONNAME and name was NULL.
 *
 *         EAI_FAIL The name server returned a permanent failure indication.
 *         
 *         EAI_FAMILY The requested address family is not supported.
 *
 *         EAI_MEMORY Out of memory.
 *
 *         EAI_NODATA The specified network host exists, but does not have
 *         any network addresses defined.
 *
 *         EAI_NONAME The addr or port is not known; or both addr and port
 *         are NULL; or AI_NUMERICSERV was specified in hints.ai_flags
 *         and port was not a numeric port-number string.
 *
 *         EAI_SERVICE The requested port is not available for the requested
 *         socket type.
 *
 *         EAI_SOCKTYPE The requested socket type is not supported. This could
 *         occur if hints.ai_socktype and hints.ai_protocol are inconsistent
 *         (e.g., SOCK_DGRAM and IPPROTO_TCP, respectively).
 *
 *         EAI_SYSTEM Other system error, check errno for details.
 */
int mpeos_socketGetAddrInfo(const char* addr, const char* port,
                            mpe_SocketAddrInfo* hints,
                            mpe_SocketAddrInfo** result) 
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "mpeos_socketGetAddrInfo addr = %s, port = %s, hint = %p result = %p\n",
        addr, port, hints, result);
    return getaddrinfo(addr, port, hints, result);
}

/**
 * Release one or more mpe_SocketAddrInfo structures returned from
 * mpe_SocketGetAddrInfo().
 */
void mpeos_socketFreeAddrInfo(mpe_SocketAddrInfo* ai)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "mpeos_socketFreeAddrInfo ai = %p\n", ai);
    freeaddrinfo(ai);
}

/**
 * Lookup a host by address.
 *
 * @param addr A pointer to the address, which is specified in network byte order.
 * @param len Specifies the length of the address.
 * @param type Specifies the type of the address.
 *
 * @return If the requested entry is found, a pointer to an <code>mpe_SocketHostEntry</code>
 * structure is returned. Otherwise <b>NULL</b> will be returned.
 */
mpe_SocketHostEntry *mpeos_socketGetHostByAddr(const void *addr,
        mpe_SocketSockLen len, int type)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "mpeos_socketGetHostByAddr: addr = %x, len = %d\n", *(int*) addr,
            len);
    return gethostbyaddr((char *) addr, len, type);
}

/**
 * Lookup a host by name.
 *
 * @param name The name of the host to get.
 *
 * @return If the requested entry is found, a pointer to an <code>mpe_SocketHostEntry</code>
 * structure is returned. Otherwise <b>NULL</b> will be returned.
 */
mpe_SocketHostEntry *mpeos_socketGetHostByName(const char *name)
{
    char hostName[128];
    int ret = mpeos_socketGetHostName(hostName, 127);
    mpe_SocketHostEntry *hostEntry = NULL;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%s(%s)\n", __func__, name);

    if ((0 == ret) && (0 == strcmp(name, hostName)))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET,
                "getting host with ri_cablecard_gethostbyname(%s)'\n", name);
        hostEntry = (mpe_SocketHostEntry *) ri_cablecard_gethostbyname(name);
    }
    else
    {
        hostEntry = (mpe_SocketHostEntry *) gethostbyname(name);

#ifdef MPE_FEATURE_DEBUG
        if ( hostEntry != NULL )
        {
            int i;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketGetHostByName: name = %s, h_length = %d\n", name, hostEntry->h_length);
            for (i = 0; hostEntry->h_addr_list[i] != NULL; i++)
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketGetHostByName: addr %d = %x\n", i+1, *(int*)hostEntry->h_addr_list[i]);
        }
        else MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketGetHostByName: error occured for %s\n", name);
#endif
    }

    return hostEntry;
}

/**
 * Get the name of the current machine.
 *
 * @param name An output pointer to the buffer into which the host name
 * is written.
 * @param namelen Specifies the size in character of the buffer pointed
 * to by <i>name</i>.
 *
 * @return If the call is successful, this routine will returnn 0.
 * Otherwise it will return -1.
 */
int mpeos_socketGetHostName(char *name, size_t namelen)
{
#ifdef MPE_FEATURE_DEBUG
    int i = gethostname(name, namelen);
    if ( i == 0 )
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketGetHostName: name = %s\n", name);
    return i;
#else
    return gethostname(name, namelen);
#endif
}

/**
 * Get the locally-bound name of a socket.
 *
 * @param socket Specifies the file descriptor associated with the socket in which to
 * get the locally-bound name.
 * @param address An output pointer to an <code>mpe_SocketSockAddr</code> structure where
 * the name should be returned.
 * @param address_len A pointer to the length of the <code>mpeSocketSockAddr</code>
 * structer pointed to by <i>address</i>.
 *
 * @return
 */
int mpeos_socketGetSockName(mpe_Socket mpeSocket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len)
{
    return getsockname(mpeSocket, address, address_len);
}

/**
 * Get the value of a socket option.
 *
 * @param socket Specifies the file descriptor associated with the socket.
 * @param level Specifes the protocol level at which the option resides.
 * @param option_name Specifies a single option to be retrieved.
 * @param option_value An output pointer to storage sufficient to hold the value
 * of the option.
 * @param option_len A pointer to the length of the buffer pointed to by
 * <i>option_value</i>
 *
 * @return If the call is successful, then 0 will be returned. Otherwise
 * -1 will be returned to indicate an error.
 */
int mpeos_socketGetOpt(mpe_Socket mpeSocket, int level, int option_name,
        void *option_value, mpe_SocketSockLen *option_len)
{
    // Handle special cases for Windows
    switch (option_name)
    {
    // Handle MPE_SOCKET_SO_LINGER as a special case because Windows defines non-standard types
    // for the mpe_SocketLinger members.
    case MPE_SOCKET_SO_LINGER:
    {
        mpe_SocketLinger *linger = (mpe_SocketLinger *) option_value;
        LINGER l;
        mpe_SocketSockLen optlen = sizeof(l);
        int result = getsockopt(mpeSocket, level, option_name, (char *) (&l),
                &optlen);
        linger->l_onoff = l.l_onoff;
        linger->l_linger = l.l_linger;
        return result;
    }

        // Handle MPE_SOCKET_SO_RCVTIMEO & MPE_SOCKET_SO_SNDTIMEO as
        // special cases because Windows uses an int specifying the
        // number of milliseconds instead of the struct timeval
        // specifying seconds and microseconds used by POSIX.
    case MPE_SOCKET_SO_RCVTIMEO:
    case MPE_SOCKET_SO_SNDTIMEO:
    {
        mpe_TimeVal *mtv = (mpe_TimeVal *) option_value;
        int val;
        mpe_SocketSockLen optlen = sizeof(val);
        int result = getsockopt(mpeSocket, level, option_name, (char *) (&val),
                &optlen);
        mtv->tv_sec = val / 1000; /* get seconds             */
        mtv->tv_usec = (val - (mtv->tv_sec * 1000)) * 1000; /* whatever's left -> usec */
        return result;
    }

        // Handle all other options in a generic manner
    default:
    {
        return getsockopt(mpeSocket, level, option_name, (char *) option_value,
                option_len);
    }
    }
}

/**
 * Get the name of a sockets' peer.
 *
 * @param socket Specifies the file descriptor associated with the socket
 * in which to get the per address.
 * @param address An output pointer to an <code>mpde_SocketSockAddr</code> structure
 * containing the peer address.
 * @param adddress_len A pointer to the length of the <code>mpe_SocketSockAddr</code>
 * structure pointed to by <i>address</i>.
 *
 * @return If the call is successful, then this routine will return 0.
 * Otherwise -1 shall be returned.
 */
int mpeos_socketGetPeerName(mpe_Socket mpeSocket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len)
{
    return getpeername(mpeSocket, address, address_len);
}

/**
 * Convert host long to network byte order.
 *
 * @param hostlong Specifies the 32-bit value in host byte order.
 *
 * @return <i>hostlong</i> converted to network byte order is returned.
 */
uint32_t mpeos_socketHtoNL(uint32_t hostlong)
{
    return htonl(hostlong);
}

/**
 * Convert host short to network byte order.
 *
 * @param hostshort Specifies the 16-bit value in host byte order.
 *
 * @return <i>hostshort</i> converted to network byte order is returned.
 */
uint16_t mpeos_socketHtoNS(uint16_t hostshort)
{
    return htons(hostshort);
}

/**
 * Convert network long to host byte order.
 *
 * @param netlong Specifies the 32-bit value in network byte order.
 *
 * @return The value of <i>netlong</i> converted to host byte order
 * is returned.
 */
uint32_t mpeos_socketNtoHL(uint32_t netlong)
{
    return ntohl(netlong);
}

/**
 * Convert network short to host byte order.
 *
 * @param netshort Specifies the 16-bit value in network byte order.
 *
 * @return The value of <i>netshort</i> converted to host byte order
 * is returned.
 */
uint16_t mpeos_socketNtoHS(uint16_t netshort)
{
    return ntohs(netshort);
}

/**
 * Perform a control function on a socket
 *
 * @param socket Specifies the file descriptor associated with the socket.
 * @param request Specifies the request for the control function to perform.
 *
 * @return If the call is successful, then 0 will be returned.
 * Otherwise -1 is returned indicating an error has occurred.
 */
int mpeos_socketIoctl(mpe_Socket mpeSocket, int request, ...)
{
    int result, *iargp;
    unsigned long longarg;
    va_list ap;
    va_start(ap, request);

    // Processing depends on request code
    switch ((unsigned) request)
    {
    // Set or clear non-blocking flag
    case MPE_SOCKET_FIONBIO:
        iargp = (int*)va_arg(ap, int*);
        longarg = *iargp;
        result = ioctlsocket(mpeSocket, request, &longarg);
        break;

        // Get number of bytes ready
    case MPE_SOCKET_FIONREAD:
        result = ioctlsocket(mpeSocket, request, &longarg);
        iargp = (int*)va_arg(ap, int*);
        *iargp = longarg;
        break;

        // Unsupported or unknown request codes
    default:
        result = MPE_EINVAL;
    }

    va_end(ap);
    return result;
}

/**
 * Listen for a connection on a socket.
 *
 * @param socket Specifies the file descriptor associated with the socket.
 * @param backlog Specifies the requested maximum number of connections to
 * allow in the socket's listen queue.
 *
 * @return If the call is successful, then 0 shall be returned.
 * Otherwise, -1 will be returned.
 */
int mpeos_socketListen(mpe_Socket mpeSocket, int backlog)
{
#ifdef MPE_FEATURE_DEBUG
    int i = listen(mpeSocket, backlog);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketListen: socket = %x, backlog = %d, result = %d\n", mpeSocket, backlog, i);
    return i;
#else
    return listen(mpeSocket, backlog);
#endif
}

/**
 * Convert an ascii formatted address (in dot notation) to its numeric format.
 *
 * @param strptr An input pointer to an Internet address in dot notation.
 * @param addrptr An input pointer to the structure to hold the address in
 * numeric form.
 *
 * @return If the string is successfully interpreted, then 1 will be returned.
 * Otherwise 0 will be returned if the string is invalid.
 */
int mpeos_socketAtoN(const char *strptr, mpe_SocketIPv4Addr *addrptr)
{
    // Windows does not have inet_aton() so use inet_addr() instead.
    unsigned long addr = inet_addr(strptr);
    if (addr == INADDR_NONE)
    {
        return 0;
    }
    else
    {
        addrptr->s_addr = addr;
        return 1;
    }
}

/**
 * Convert a numeric address to its ascii format in dot notation.
 *
 * @param inaddr Specifies the Internet host address to convert.
 *
 * @return An ASCII string representing the address in dot notation
 * is returned.
 */
char *mpeos_socketNtoA(mpe_SocketIPv4Addr inaddr)
{
    return inet_ntoa(inaddr);
}

/**
 * The mpeos_socketNtoP() function shall convert a numeric address into a text string
 * suitable for presentation. The <i>af</i> argument shall specify the family of the
 * address. This can be MPE_SOCKET_AF_INET4 or MPE_SOCKET_AF_INET6. The <i>src</i> argument points to a buffer
 * holding an IPv4 address if the <i>af</i> argument is MPE_SOCKET_AF_INET4, or an IPv6 address if
 * the <i>af</i> argument is MPE_SOCKET_AF_INET6. The <i>dst</i> argument points to a buffer where
 * the function stores the resulting text string; it shall not be NULL. The <i>size</i>
 * argument specifies the size of this buffer, which shall be large enough to hold the
 * text string (MPE_SOCKET_INET4_ADDRSTRLEN characters for IPv4, MPE_SOCKET_INET6_ADDRSTRLEN characters
 * for IPv6).
 *
 * @param af address family
 * @param src the source form of the address
 * @param dst the destination form of the address
 * @param size the size of the buffer pointed to by <i>dst</i>
 * @return This function shall return a pointer to the buffer containing the text string
 *          if the conversion succeeds, and NULL otherwise. In the latter case one of the
 *          following error codes can be retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EAFNOSUPPORT - The <i>af</i> argument is invalid.
 * <li>     MPE_SOCKET_ENOSPC - The size of the result buffer is inadequate.
 * </ul>
 * @see POSIX function inet_ntop()
 */
const char *mpeos_socketNtoP(int af, const void *src, char *dst, size_t size)
{
    int ret = 0;
    DWORD dw_length = size;

    if (0 == (ret = WSAAddressToStringA((void*)src,
                                        sizeof(struct sockaddr_storage),
                                        NULL, dst, &dw_length)))
    {
        return (const char*)dst;
    }
    else
    {
        ret = WSAGetLastError();

        if (WSAEFAULT == ret)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                      "%s buffer length %d is insufficient, need %ld\n",
                       __FUNCTION__, size, dw_length);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                      "%s %d = WSAAddressToStringA()\n", __FUNCTION__, ret);
        }

        return (const char*)NULL;
    }
}

/**
 * The mpeos_socketPtoN() function shall convert an address in its standard text presentation
 * form into its numeric binary form. The <i>af</i> argument shall specify the family of the
 * address. This can be MPE_SOCKET_AF_INET4 or MPE_SOCKET_AF_INET6. The <i>src</i> argument points to the
 * string being passed in. The <i>dst</i> argument points to a buffer into which the function
 * stores the numeric address; this shall be large enough to hold the numeric address (32
 * bits for MPE_SOCKET_AF_INET4, 128 bits for MPE_SOCKET_AF_INET6).
 * <p>
 * If the <i>af</i> argument is MPE_SOCKET_AF_INET4, the <i>src</i> string shall be in the standard
 * IPv4 dotted-decimal form:
 * <p>
 * ddd.ddd.ddd.ddd
 * <p>
 * where "ddd" is a one to three digit decimal number between 0 and 255 inclusive. This function
 * does not accept other formats (such as the octal numbers, hexadecimal numbers, or fewer than
 * four numbers).
 * <p>
 * If the <i>af</i> argument MPE_SOCKET_AF_INET6, the <i>src</i> string shall be in one of the
 * following standard IPv6 text forms:
 * <ul>
 * <li>     The preferred form is "x:x:x:x:x:x:x:x" , where the 'x' s are the hexadecimal
 *          values of the eight 16-bit pieces of the address. Leading zeros in individual
 *          fields can be omitted, but there shall be at least one numeral in every field.
 * <li>     A string of contiguous zero fields in the preferred form can be shown as "::".
 *          The "::" can only appear once in an address. Unspecified addresses
 *          ("0:0:0:0:0:0:0:0") may be represented simply as "::".
 * <li>     A third form that is sometimes more convenient when dealing with a mixed
 *          environment of IPv4 and IPv6 nodes is "x:x:x:x:x:x:d.d.d.d" , where the 'x's
 *          are the hexadecimal values of the six high-order 16-bit pieces of the address,
 *          and the 'd's are the decimal values of the four low-order 8-bit pieces of the
 *          address (standard IPv4 representation).
 * </ul>
 *
 * @param af address family
 * @param src the source form of the address
 * @param dst the destination form of the address
 * @return This function shall return 1 if the conversion succeeds, with the address pointed
 *          to by <i>dst</i> in network byte order. It shall return 0 if the input is not a
 *          valid IPv4 dotted-decimal string or a valid IPv6 address string, or -1 on error.
 *          In the latter case one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EAFNOSUPPORT - The <i>af</i> argument is invalid.
 * </ul>
 * @see POSIX function inet_pton()
 * @see A more extensive description of the standard representations of IPv6 addresses can
 *          be found in RFC 2373.
 */
int mpeos_socketPtoN(int af, const char *src, void *dst)
{
    int length = (af == AF_INET?  sizeof(struct sockaddr_in) :
                                  sizeof(struct sockaddr_in6));
    return WSAStringToAddressA((char*)src, af, 0, dst, &length);
}

/**
 * Receive a message on a socket.
 *
 * @param socket Specifies the file descriptor associated with the socket.
 * @param buffer An ouput pointer to a buffer where the message should be stored.
 * @param length Specifies the length in bytes of the buffer pointed to by <i>buffer</i>.
 * @param flags Specifies the type of message rectption.
 *
 * @return If the call is successul, then this routine will return the length of the
 * message in bytes. Otherwise, -1 will be returned.
 */
size_t mpeos_socketRecv(mpe_Socket mpeSocket, void *buffer, size_t length,
        int flags)
{
    size_t s = recv(mpeSocket, (char *) buffer, length, flags);

#ifdef MPE_FEATURE_DEBUG
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketRecv: called, socket = %0x, flags = %x\n", mpeSocket, flags);
    {
        if ( s == (size_t)-1 )
        {
            int err = WSAGetLastError();
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketRecv: socket = %0x, error = %d\n", mpeSocket, err);
            WSASetLastError(err);
        }
        else
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketRecv: result = %d, socket = %0x, flags = %x\n", s, mpeSocket, flags);
    }
#endif

    /* If the buffer is not large enough to hold the message, it is simply
     truncated -- this is not an error, UDP sockets are supposed to truncate */
    if ((s == (size_t) SOCKET_ERROR) && (WSAGetLastError() == WSAEMSGSIZE))
    {
        return length;
    }

    return s;
}

/**
 * Receive a message on a socket and determine its sender.
 */
size_t mpeos_socketRecvFrom(mpe_Socket mpeSocket, void *buffer, size_t length,
        int flags, mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len)
{
    size_t s = recvfrom(mpeSocket, (char *) buffer, length, flags, address,
            address_len);

#ifdef MPE_FEATURE_DEBUG
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketRecvFrom: called, socket = %0x, flags = %x, address = %x\n", mpeSocket, flags, *(int*)address);
    {
        if (s == (size_t)-1)
        {
            int err = WSAGetLastError();
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketRecvFrom: socket = %0x, error = %d\n", mpeSocket, err);
            WSASetLastError(err);
        }
        else
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketRecvFrom: result = %d, socket = %0x, flags = %x, address = %x\n", s, mpeSocket, flags, *(int*)address);
    }
#endif

    /* If the buffer is not large enough to hold the message, it is simply
     truncated -- this is not an error, UDP sockets are supposed to truncate */
    if ((s == (size_t) SOCKET_ERROR) && (WSAGetLastError() == WSAEMSGSIZE))
    {
        return length;
    }

    return s;
}

/**
 * Block waiting for one or more FDs to become ready for I/O
 */
int mpeos_socketSelect(int numfds, mpe_SocketFDSet *readfds,
        mpe_SocketFDSet *writefds, mpe_SocketFDSet *errorfds,
        const mpe_TimeVal *timeout)
{
#ifdef MPE_FEATURE_DEBUG
    {
        int i = select(numfds, readfds, writefds, errorfds, timeout);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketSelect: result = %d, numfds = %d\n", i, numfds);
        return i;
    }
#else
    return select(numfds, readfds, writefds, errorfds, timeout);
#endif
}

/**
 * Send a message
 */
size_t mpeos_socketSend(mpe_Socket mpeSocket, const void *buffer,
        size_t length, int flags)
{
#ifdef MPE_FEATURE_DEBUG
    {
        int s = send(mpeSocket, (const char *)buffer, length, flags);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketSend: result = %d, len = %d\n", s, length );
        return s;
    }
#else
    return send(mpeSocket, (const char *) buffer, length, flags);
#endif
}

/**
 * Send a message to the specified address
 */
size_t mpeos_socketSendTo(mpe_Socket mpeSocket, const void *message,
        size_t length, int flags, const mpe_SocketSockAddr *dest_addr,
        mpe_SocketSockLen dest_len)
{
#ifdef MPE_FEATURE_DEBUG
    {
        int s = sendto(mpeSocket, (const char *)message, length, flags, dest_addr, dest_len);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "mpeos_socketSendTo: result = %d, len = %d, addr = %x\n", s, length, (dest_addr == NULL) ? 0 : *(int*)dest_addr );
        return s;
    }
#else
    return sendto(mpeSocket, (const char *) message, length, flags, dest_addr,
            dest_len);
#endif
}

/**
 * Set the value of a socket option
 */
int mpeos_socketSetOpt(mpe_Socket mpeSocket, int level, int option_name,
        const void *option_value, mpe_SocketSockLen option_len)
{
    int result;

    // Handle special cases for Windows
    switch (option_name)
    {
    // Handle MPE_SOCKET_SO_LINGER as a special case because Windows
    // defines non-standard types for the mpe_SocketLinger members.
    case MPE_SOCKET_SO_LINGER:
    {
        LINGER l;
        l.l_onoff = ((mpe_SocketLinger *) option_value)->l_onoff;
        l.l_linger = ((mpe_SocketLinger *) option_value)->l_linger;
        result = setsockopt(mpeSocket, level, option_name, (const char *) (&l),
                sizeof(l));
        break;
    }

        // Handle MPE_SOCKET_SO_RCVTIMEO & MPE_SOCKET_SO_SNDTIMEO as
        // special cases because Windows uses an int specifying the
        // number of milliseconds instead of the struct timeval
        // specifying seconds and microseconds used by POSIX.
    case MPE_SOCKET_SO_RCVTIMEO:
    case MPE_SOCKET_SO_SNDTIMEO:
    {
        mpe_TimeVal* mtv = (mpe_TimeVal*) option_value;
        int val = mtv->tv_sec * 1000 + (mtv->tv_usec + 500000) / 1000;
        result = setsockopt(mpeSocket, level, option_name,
                (const char *) (&val), sizeof(val));
        break;
    }

        // Handle all other options in a generic manner
    default:
    {
        result = setsockopt(mpeSocket, level, option_name,
                (const char *) option_value, option_len);
        break;
    }
    }
#ifdef MPE_FEATURE_DEBUG
    if ( result == (-1) )
    {
        int err = WSAGetLastError();
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"mpeos_socketSetOpt: cmd = %d, result = %d, error = %d\n", option_name, result, err);
        WSASetLastError(err);
    }
#endif
    return result;
}

/**
 * Shut down a socket connection
 */
int mpeos_socketShutdown(mpe_Socket mpeSocket, int how)
{
    return shutdown(mpeSocket, how);
}

/**
 * Get the network interfaces.
 */
mpe_Error mpeos_socketGetInterfaces(mpe_SocketNetIfList **netIfList)
{
    extern mpe_SocketNetIfList *getInterfaces(int);

    /* Try IPv4 interfaces first... */
    *netIfList = getInterfaces( AF_INET );

    /* Try IPv6 interfaces if we have no IPv4 ifs */
    if (*netIfList == NULL)
    {
        *netIfList = getInterfaces( AF_INET6 );
    }

    /* Get all interfaces - this should have worked, but it doesn't! */
    //*netIfList = getInterfaces( AF_UNSPEC );

    /* Return interface list or error. */
    if (*netIfList != NULL)
    {
        return MPE_SUCCESS;
    }
    else
    {
        return MPE_ENOMEM;
    }
}

/**
 * Free network interfaces previously obtained via mpeos_socketGetInterfaces().
 */
void mpeos_socketFreeInterfaces(mpe_SocketNetIfList *netIfList)
{
    mpe_SocketNetIfList *curIf = netIfList;

    while (curIf != NULL)
    {
        mpe_SocketNetIfList *iface = curIf;
        mpe_SocketNetAddr *curAddr = curIf->if_addresses;
        while (curAddr != NULL)
        {
            mpe_SocketNetAddr *addr = curAddr;
            curAddr = curAddr->if_next;
            free(addr->if_addr);
            addr->if_addr = NULL;
            free(addr);
            addr = NULL;
        }
        curIf = curIf->if_next;
        free(iface);
        iface = NULL;
    }
}

/*
 * Wrapper for a native Win32 system call.
 * Returns a linked list of PIP_ADAPTER_ADDRESSES.
 */
static PIP_ADAPTER_ADDRESSES enumerateWin32NetworkAdapters(int family)
{
    PIP_ADAPTER_ADDRESSES pAddresses = NULL;
    ULONG outBufLen = 0;
    ULONG flags = (GAA_FLAG_SKIP_ANYCAST | GAA_FLAG_SKIP_DNS_SERVER
            | GAA_FLAG_SKIP_FRIENDLY_NAME | GAA_FLAG_SKIP_MULTICAST);

    /*
     * Make an initial call to GetAdaptersAddresses to get the size needed
     * into the outBufLen variable.
     */
    outBufLen = sizeof(IP_ADAPTER_ADDRESSES);
    if ((pAddresses = (IP_ADAPTER_ADDRESSES *) malloc(outBufLen)) == NULL)
        return NULL;

    if (GetAdaptersAddresses(family, flags, NULL, pAddresses, &outBufLen)
            == ERROR_BUFFER_OVERFLOW)
    {
        free(pAddresses); /* Free small buffer. */

        /* Allocate buffer of correct size. */
        pAddresses = (IP_ADAPTER_ADDRESSES *) malloc(outBufLen);
        if (GetAdaptersAddresses(family, flags, NULL, pAddresses, &outBufLen)
                != ERROR_SUCCESS)
        {
            free(pAddresses);
            return NULL;
        }
    }
    return pAddresses;
}

static mpe_SocketNetIfList *createNetworkInterface(const char *if_name, uint32_t if_index)
{
    mpe_SocketNetIfList *nif = NULL;
    /* If the interface name is longer than we can support, return NULL. */
    if (strlen(if_name) >= MPE_SOCKET_IFNAMSIZ)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                  "addNetIf: Interface name '%s' is too long\n", if_name);
    }
    else if ( (nif = (mpe_SocketNetIfList *)malloc(sizeof(mpe_SocketNetIfList))) != NULL )
    {
        /* Copy the interface name. */
        strcpy(nif->if_name, if_name);
        nif->if_index = if_index;
        nif->if_addresses = NULL;
        nif->if_next = NULL;
    }
    return nif;
}

static mpe_SocketNetAddr *createInetAddress(int family, mpe_SocketSockAddr *sockaddr, int addrlen)
{
    mpe_SocketNetAddr *netaddr = NULL;

    if ( (netaddr = (mpe_SocketNetAddr *)malloc(sizeof(mpe_SocketNetAddr))) != NULL )
    {
        netaddr->if_family = family;
        if ( (netaddr->if_addr = (mpe_SocketSockAddr *)malloc(addrlen)) == NULL)
        {
            free(netaddr);
            return NULL;
        }
        else
        {
            memcpy(netaddr->if_addr, sockaddr, addrlen);
        }
        netaddr->if_next = NULL;
    }
    return netaddr;
}

/**
 * On Windows 7 (and potentially Windows XP as well), some interfaces that are returned
 * in the enumeration are invalid as when the corresponding IP addresses are passed to
 * the bind function, it returns an error. The purpose of this function is to filter out
 * those IP addresses from the enumeration returned to the stack.
 *
 * Based on initial observations, the criteria for an IP address failing a bind call are:
 * - Auto-assigned IP in the 169.254.XXX.XXX address range AND
 * - Link status of the underlying network interface being DOWN.
 */
static mpe_Bool wouldBindFail( PIP_ADAPTER_ADDRESSES pIface, PIP_ADAPTER_UNICAST_ADDRESS pAddr )
{
    mpe_Bool retval = FALSE;

    ULONG outBufLen = sizeof (IP_PER_ADAPTER_INFO);
    PIP_PER_ADAPTER_INFO pIpPerAdapterInfo = NULL;

    if ((pIpPerAdapterInfo = (PIP_PER_ADAPTER_INFO) malloc(outBufLen)) == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                  "%s: first malloc failed\n", __FUNCTION__);
    }
    else if (GetPerAdapterInfo(pIface->IfIndex, pIpPerAdapterInfo, &outBufLen)
            == ERROR_BUFFER_OVERFLOW)
    {
        free(pIpPerAdapterInfo);
        if ((pIpPerAdapterInfo = (PIP_PER_ADAPTER_INFO) malloc(outBufLen)) == NULL)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                      "%s: second malloc failed\n", __FUNCTION__);
        }
        else
        {
            unsigned long maxAddrStrLen = INET6_ADDRSTRLEN;
            char asciiBuffer[INET6_ADDRSTRLEN];

            if (WSAAddressToString(pAddr->Address.lpSockaddr, pAddr->Address.iSockaddrLength,
                    NULL, asciiBuffer, &maxAddrStrLen) == 0)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%s: IP %s: AutoconfigActive = %u, "
                        "OperStatus = %u\n", __FUNCTION__, asciiBuffer,
                        pIpPerAdapterInfo->AutoconfigActive, pIface->OperStatus);

                if (pIpPerAdapterInfo->AutoconfigActive && pIface->OperStatus == IfOperStatusDown)
                {
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                          "%s: skipping %s\n", __FUNCTION__, asciiBuffer);
                    retval = TRUE;
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                        "%s: WSAAddressToString failed\n", __FUNCTION__);
            }
            free(pIpPerAdapterInfo);
        }
    }

    return retval;
}

static mpe_SocketNetAddr *getAddresses( int family, PIP_ADAPTER_ADDRESSES pAddresses )
{
    PIP_ADAPTER_UNICAST_ADDRESS ipAddr = NULL;

    mpe_SocketNetAddr  *currAddr = NULL;
    mpe_SocketNetAddr  *prevAddr = NULL;
    mpe_SocketNetAddr *firstAddr = NULL;

    /* Process addresses of the current interface. */
    for ( ipAddr = pAddresses->FirstUnicastAddress; ipAddr != NULL; ipAddr = ipAddr->Next )
    {
        if (wouldBindFail(pAddresses, ipAddr) == TRUE)
        {
            continue;
        }

        currAddr = createInetAddress(family, ipAddr->Address.lpSockaddr, ipAddr->Address.iSockaddrLength);

        /* Adjust the structure pointers. */
        if (firstAddr == NULL)
        {
            /* First iteration only. */
            firstAddr = currAddr;
        }

        if (prevAddr != NULL)
        {
            /* All iterations but the first one. */
            prevAddr->if_next = currAddr;
        }
        prevAddr = currAddr;
    }
    return firstAddr;
}

/*
 * Windows 2000
 * Windows XP
 * Windows .NET Server
 *
 * Use adapter->IfIndex to assign ethX names to each network interface.
 */
mpe_SocketNetIfList *getInterfacesWinMajorVer5( int family, PIP_ADAPTER_ADDRESSES pAddresses )
{
    PIP_ADAPTER_ADDRESSES  pCurr = NULL;
    mpe_SocketNetIfList *firstIf = NULL;
    mpe_SocketNetIfList  *currIf = NULL;
    mpe_SocketNetIfList  *prevIf = NULL;
    uint32_t     physicalIfCount = 0;
    uint32_t         numAdapters = 0;

    uint32_t i = 0;

    /* Figure out the number of all enabled network interfaces. */
    for ( pCurr = pAddresses; pCurr != NULL; pCurr = pCurr->Next )
    {
        numAdapters++;
    }

    /* Iterate through interfaces adding them to the interface list. */
    /* Return the mpe_SocketNetIfList stored by the IfIndex. */
    for ( i = 1; i <= numAdapters; i++)
    {
        for ( pCurr = pAddresses; pCurr != NULL; pCurr = pCurr->Next )
        {
            if (pCurr->IfIndex == i)
            {
                char if_name[MPE_SOCKET_IFNAMSIZ];
                if (pCurr->IfType == IF_TYPE_SOFTWARE_LOOPBACK)
                {
                    snprintf(if_name, MPE_SOCKET_IFNAMSIZ - 1, "%s", "lo");
                }
                else
                {
                    snprintf(if_name, MPE_SOCKET_IFNAMSIZ - 1, "eth%u", physicalIfCount++);
                }

                /* If network interface creation fails, continue to the next interface. */
                if ( (currIf = createNetworkInterface(if_name, pCurr->IfIndex)) == NULL)
                {
                    continue;
                }

                currIf->if_addresses = getAddresses(family, pCurr);

                /* Adjust the structure pointers. */
                if (firstIf == NULL)
                {
                    /* First iteration only. */
                    firstIf = currIf;
                }

                if (prevIf != NULL)
                {
                    /* All iterations but the first one. */
                    prevIf->if_next = currIf;
                }
                prevIf = currIf;
                break; // fall out from the second "for" loop
            }
        }
    }

    return firstIf;
}

/*
 * Windows Vista
 * Windows 2008
 * Windows 7
 * Windows 2008 R2
 *
 * Use the PIP_ADAPTER_ADDRESSES linked list ordering to assign ethX names to each network interface.
 */
mpe_SocketNetIfList *getInterfacesWinMajorVer6( int family, PIP_ADAPTER_ADDRESSES pAddresses )
{
    PIP_ADAPTER_ADDRESSES  pCurr = NULL;
    mpe_SocketNetIfList *firstIf = NULL;
    mpe_SocketNetIfList  *currIf = NULL;
    mpe_SocketNetIfList  *prevIf = NULL;
    uint32_t     physicalIfCount = 0;

    /* Iterate through interfaces adding them to the interface list. */
    for ( pCurr = pAddresses; pCurr != NULL; pCurr = pCurr->Next )
    {
        char if_name[MPE_SOCKET_IFNAMSIZ];
        if (pCurr->IfType == IF_TYPE_SOFTWARE_LOOPBACK)
        {
            snprintf(if_name, MPE_SOCKET_IFNAMSIZ - 1, "%s", "lo");
        }
        else
        {
            snprintf(if_name, MPE_SOCKET_IFNAMSIZ - 1, "eth%u", physicalIfCount++);
        }

        /* If network interface creation fails, continue to the next interface. */
        if ( (currIf = createNetworkInterface(if_name, pCurr->IfIndex)) == NULL)
        {
            continue;
        }

        currIf->if_addresses = getAddresses(family, pCurr);

        /* Adjust the structure pointers. */
        if (firstIf == NULL)
        {
            /* First iteration only. */
            firstIf = currIf;
        }

        if (prevIf != NULL)
        {
            /* All iterations but the first one. */
            prevIf->if_next = currIf;
        }
        prevIf = currIf;
    }
    return firstIf;
}

/*
 * Acquire the list of interfaces for the specified familty and add each
 * interface adn its addresses to the list of interfaces.
 */
mpe_SocketNetIfList *getInterfaces( int family )
{
    OSVERSIONINFO osvi;

    PIP_ADAPTER_ADDRESSES pAddresses = NULL;
    mpe_SocketNetIfList     *firstIf = NULL;

    pAddresses = enumerateWin32NetworkAdapters(family);
    if ( pAddresses == NULL )
    {
        return NULL;
    }

    ZeroMemory(&osvi, sizeof(OSVERSIONINFO));
    osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFO);
    GetVersionEx(&osvi);
    if (osvi.dwMajorVersion == 5)
    {
        firstIf = getInterfacesWinMajorVer5( family, pAddresses );
    }
    else if (osvi.dwMajorVersion == 6)
    {
        firstIf = getInterfacesWinMajorVer6( family, pAddresses );
    }
    else
    {
        MPEOS_LOG(MPE_LOG_FATAL, MPE_MOD_NET,
            "getInterfaces: unsupported Windows major version number (%d)\n",
            osvi.dwMajorVersion);
        firstIf = NULL;
    }

    free( pAddresses );
    return firstIf;
}

/**
 * Get the type of the current machine if no display name is supplied.
 * If a network interface display name is supplied, find that network interface
 * and return the physical address associated with that interface.
 *
 * @param displayName   name of specific network interface, if null, look for
 *                      first interface which has an associated physical address.
 * @param type returns numeric representation of interface type the associated with specified
 *             interface name.  Types are defined in org.ocap.hn.NetworkInterface as follows:
 *                 MOCA = 1;
 *                 WIRED_ETHERNET = 2;
 *                 WIRELESS_ETHERNET = 3;
 *                 UNKNOWN = 0;
 *
 * @return 0 if successful, -1 if problems encountered
 */
int os_getNetworkInterfaceType(char* displayName, int* type)
{
    PIP_ADAPTER_ADDRESSES pAddresses = NULL;
    PIP_ADAPTER_ADDRESSES pCurr = NULL;
    uint32_t numAdapters = 0;
    uint32_t i = 0;
    int retCode = -1;

    // Constants from org.ocap.hn.NetworkInterface
    //int MOCA = 1;
    int WIRED_ETHERNET = 2;
    int WIRELESS_ETHERNET = 3;
    int UNKNOWN = 0;

    // Get the list of interfaces
    mpe_SocketNetIfList *netIfList = NULL;
    if (mpeos_socketGetInterfaces((mpe_SocketNetIfList**)&netIfList) != MPE_SUCCESS)
    {
        return -1;
    }

    //  Look for a matching display name, starting with the first
    mpe_SocketNetIfList* curNetIfList = netIfList;
    do
    {
        if (strcmp(displayName, curNetIfList->if_name) == 0)
        {
            // Found matching network interface, get if type
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                     "os_getNetworkIntfaceType() - Found matching if name = %s\n",
                     curNetIfList->if_name);

            // Get the underlying OS structure for network interfaces
            pAddresses = enumerateWin32NetworkAdapters(AF_INET);
            if ( pAddresses == NULL )
            {
                return -1;
            }

            /* Figure out the number of all enabled network interfaces. */
            for ( pCurr = pAddresses; pCurr != NULL; pCurr = pCurr->Next )
            {
                numAdapters++;
            }

            // Find the address with specified index
            for ( i = 1; i <= numAdapters; i++)
            {
                for ( pCurr = pAddresses; pCurr != NULL; pCurr = pCurr->Next )
                {
                    if (pCurr->IfIndex == curNetIfList->if_index)
                    {
                        // Return the type from underlying os structure
                        switch (pCurr->IfType)
                        {
                        //case IF_TYPE_?: Not supported on RI platform
                        //    type = MOCA;
                        //    break;
                        case IF_TYPE_ETHERNET_CSMACD:
                            *type = WIRED_ETHERNET;
                            break;
                        case IF_TYPE_IEEE80211:
                            *type = WIRELESS_ETHERNET;
                            break;
                         default:
                            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                            "os_getNetworkIntfaceType() - Unexpected IF Type %d for display name = %s\n",
                            pCurr->IfType, curNetIfList->if_name);
                            *type = UNKNOWN;
                        }
                        retCode = 0;
                        break;
                    }
                }
            }
        }
    }
    while (((curNetIfList = curNetIfList->if_next) != NULL) && (retCode == -1));

    mpeos_socketFreeInterfaces(netIfList);

    return retCode;
}

/**
 * Sets the DLNA QOS(Quality of Service) value for a socket
 * This is the DSCP value set in the TOS field of the IP header.
 * This API restricts the allowable values to the values
 * defined by mpe_SocketDLNAQOS. A value other than those defined
 * by mpe_SocketDLNAQOS results in a MPE_EINVAL return code.
 *
 * @param sock is the socket to set QOS on
 * @param value is the value to set
 *
 * @return Upon successful completion, returns MPE_SUCCESS
 * <ul>
 * <li>     MPE_EINVAL - value other than those defined by mpe_SocketDLNAQOS.
 * <li>     MPE_ENODATA - any other error.
 * </ul>
 */
mpe_Error mpeos_socketSetDLNAQOS(mpe_Socket sock, mpe_SocketDLNAQOS value)
{
    // Currently unimplemented.. return success
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "mpeos_socketSetDLNAQOS currently unimplemented");
    return MPE_SUCCESS;
}

/**
 * Gets the LPE NetworkInterface information, after providing the interfaceName.
 * interfaceName can be eth0, eth1, em1 etc.
 * @return Upon successful completion, returns MPE_SUCCESS and the structure of 
 * the LPE networkInterface gets populated with correct values.
 * Structure attributes contain values of:
 * WakeOnPattern
 * WakeSupportedTransport
 * MaxWakeOnDelay
 * DozeDuration
 * If error, MPE_ENODATA is returned and the structure of 
 * the LPE networkInterface gets populated with default values.
 */
mpe_Error mpeos_getDLNANetworkInterfaceInfo(char* interfaceName, mpeos_LpeDlnaNetworkInterfaceInfo* dlnaNetworkIfInfo)
{
    if (interfaceName)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "%s: interfaceName provided %s\n", __FUNCTION__, interfaceName);
        dlnaNetworkIfInfo->wakeOnPattern = "GGGGGGGGGGGG"; // temporary
        dlnaNetworkIfInfo->wakeSupportedTransport = "UDP-Broadcast";
        dlnaNetworkIfInfo->maxWakeOnDelay = 30; // temporary
        dlnaNetworkIfInfo->dozeDuration = 30; // temporary
    }
    else
    {
        // Values when there was an error
        dlnaNetworkIfInfo->wakeOnPattern = ""; // temporary
        dlnaNetworkIfInfo->wakeSupportedTransport = "";
        dlnaNetworkIfInfo->maxWakeOnDelay = 0; // means value is NOT set
        dlnaNetworkIfInfo->dozeDuration = 0; // means value is NOT set

        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                "%s: InterfaceName not provided\n", __FUNCTION__);
        return MPE_ENODATA;
    }
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                "Returning from %s\n", __FUNCTION__);
    return MPE_SUCCESS;
}

/**
 * Gets the LPE NetworkInterfaceMode information, after providing the interfaceName.
 * interfaceName can be eth0, eth1, em1 etc.
 * @return Upon successful completion, returns MPE_SUCCESS and
 * populates the string describing the NetworkInterfaceMode with the correct value.
 * NetworkInterfaceMode can be: "Unimplemented", "IP-up", "IP-up-Periodic", "IP-down-no-Wake",
 * "IP-down-with-WakeOn", "IP-down-with-WakeAuto", "IP-down-with-WakeOnAuto".
 * On error: MPE_ENODATA is returned and string describing the NetworkInterfaceMode is
 * set to "Unimplemented".
 */
mpe_Error mpeos_getDLNANetworkInterfaceMode(char* interfaceName, char** lpeDlnaNetworkInterfaceModeInfo)
{
    mpe_PowerStatus currPowerStat = mpeos_stbGetPowerStatus();
    if (currPowerStat == MPE_POWER_FULL)
    {
        *lpeDlnaNetworkInterfaceModeInfo = "IP-up";
    }
    else
    {
        *lpeDlnaNetworkInterfaceModeInfo = "IP-down-with-WakeOn";
    }
        
    if (!interfaceName)
    {
        *lpeDlnaNetworkInterfaceModeInfo = "Unimplemented";
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                "%s: InterfaceName not provided\n", __FUNCTION__);
        return MPE_ENODATA;
    }
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
                "Returning from %s\n", __FUNCTION__);
    return MPE_SUCCESS;
}

/**
 * Issue a ping and return results
 *
 * @param   testID     id associated with test 
 * @param   host       host to ping
 * @param   reps       number of requests to send
 * @param   interval   time in msec between requests 
 * @param   timeout    timeout in msec to wait for reply
 * @param   blocksize  send buffer size
 * @param   dscp       DSCP value
 * @param   status     returned status
 * @param   info       returned additional info
 * @param   successes  returned number of successful pings
 * @param   fails      returned number of failed pings
 * @param   retAvg     returned average time
 * @param   retMin     returned minimum time
 * @param   retMax     returned maximum time
 *
 * @return 0 if successful, -1 if problems encountered
 */

int os_Ping(int testID, char* host, int reps, int interval, int timeout, int blocksize, int dscp, char *status, char *info, int *successes, int *fails, int *retAvg, int *retMin, int *retMax)

{

    char buffer[1024] = {0};
    char cmd[128] = {0};
    int success= 0;
    int failed= 0;
    int min= 0;
    int max= 0;
    int avg= 0;
    status[0] = 0;
    info[0] = 0;
    *successes = 0;
    *fails = 0;
    *retAvg = 0;
    *retMin = 0;
    *retMax = 0;

    //Range checks
    if (host == NULL || strlen(host) == 0)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid host parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (reps < MPE_SOCKET_PING_MIN_COUNT || reps > MPE_SOCKET_PING_MAX_COUNT)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid reps parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (interval < MPE_SOCKET_PING_MIN_INTERVAL || 
        interval > MPE_SOCKET_PING_MAX_INTERVAL)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid interval parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (timeout < MPE_SOCKET_MIN_TIMEOUT || timeout > MPE_SOCKET_MAX_TIMEOUT)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid timeout parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (blocksize < MPE_SOCKET_MIN_BLOCK_SIZE ||
        blocksize > MPE_SOCKET_MAX_BLOCK_SIZE)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid blocksize parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
   
    if (dscp < MPE_SOCKET_MIN_DSCP || dscp > MPE_SOCKET_MAX_DSCP)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid dscp parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }

    // adjust dscp to TOS value
    int tos = dscp >> 2;
    // convert msec to sec
    interval = interval/1000;

    char *pingCmd = "c:\\Windows\\system32\\ping";
    sprintf(cmd,"%s -n %d -w %d -v %d -l %d %s 2>&1", pingCmd, reps,
        timeout, tos, blocksize, host); 
    
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"os_Ping cmd = %s\n", cmd);    
    FILE *cmdFile = popen(cmd, "r");

    if (cmdFile == NULL)
    {
        return -1;
    }
    while (fgets(buffer, sizeof(buffer), cmdFile))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"received %s\n", buffer);
        if (isTestCancelled(testID))
        {
            removeCancelledTest(testID);
            pclose(cmdFile);
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "test %d cancelled\n", testID);
            return MPE_SUCCESS;
        }
        if (strstr(buffer, "Packets:"))
        {
            char *result = NULL;
            result = strtok(buffer,"=");
            result = strtok(NULL,"=");
            result = strtok(NULL,"=");
            sscanf(result,"%d", &success);
            result = strtok(NULL,"=");
            sscanf(result,"%d", &failed);
        }
        if (strstr(buffer, "Minimum"))
        {
            char *result = NULL;
            result = strtok(buffer,"=");
            result = strtok(NULL,"=");
            sscanf(result,"%d", &min);
            result = strtok(NULL,"=");
            sscanf(result,"%d", &max);
            result = strtok(NULL,"=");
            sscanf(result,"%d", &avg);
        }
        // check for unknown host
        if (strstr(buffer, "Ping request could not find host"))
        {
            strncpy(status, "Error_CannotResolveHostName", MPE_SOCKET_STATUS_SIZE);
            break;
        }
        // check for other error
        if (strstr(buffer, "Usage:"))
        {
            strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
            strncpy(info, buffer, MPE_SOCKET_ADDITIONAL_INFO_SIZE);
            break;
        }

    }
    pclose(cmdFile);

    if (strlen(status) == 0)
    {
       strncpy(status, "Success", MPE_SOCKET_STATUS_SIZE);
    }

    *successes = success;
    *fails = failed;
    *retMin = min;
    *retMax = max;
    *retAvg = avg;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "os_Ping results %s %s %d %d %d %d %d\n",status, info, *successes,*fails,
        *retAvg,*retMin,*retMax);
    return MPE_SUCCESS;
}

/**
 * Issue a traceroute and return results
 *
 * @param   testID     id associated with test 
 * @param   host       host
 * @param   hops       max number of hops
 * @param   timeout    timeout in msec to wait for reply
 * @param   blocksize  send buffer size
 * @param   dscp       DSCP value
 * @param   status     returned status
 * @param   info       returned additional info
 * @param   avgresp    returned average response time
 * @param   hophosts   returned hosts found
 *
 * @return 0 if successful, -1 if problems encountered
 */

int os_Traceroute(int testID, char* host, int hops, int timeout, int blocksize, int dscp, char *status, char *info, int *avgresp, char *hophosts)
{
    char buffer[1024];
    char cmd[128];
    char ipname[128];
    float rtt1 = 0;
    float rtt2 = 0;
    float rtt3 = 0;
    float favgresp = 0;
    char field1[32],field2[32],field3[32],field4[32];
    char field5[32],field6[32],field7[32],field8[32];
    int hopnumber = 0;


    //Range checks
    if (host == NULL || strlen(host) == 0)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid host parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (hops < MPE_SOCKET_TRACEROUTE_MIN_HOPS || hops > MPE_SOCKET_TRACEROUTE_MAX_HOPS)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid hops parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (timeout < MPE_SOCKET_MIN_TIMEOUT || timeout > MPE_SOCKET_MAX_TIMEOUT)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid timeout parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (blocksize < MPE_SOCKET_MIN_BLOCK_SIZE ||
        blocksize > MPE_SOCKET_MAX_BLOCK_SIZE)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid block size" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }

    if (dscp < MPE_SOCKET_MIN_DSCP || dscp > MPE_SOCKET_MAX_DSCP)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid dscp parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }

    // adjust input args
    *avgresp = 0;
    status[0] = 0;

    char *tracertCmd = "c:\\Windows\\system32\\tracert -d";
    sprintf(cmd,"%s -h %d -w %d %s 2>&1", tracertCmd, hops, timeout, host); 
    
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"cmd = %s\n", cmd);    
    FILE *cmdFile = popen(cmd, "r");
    if (cmdFile == NULL)
    {
        return -1;
    }

    while (fgets(buffer, sizeof(buffer), cmdFile))
    {
         MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"received %s\n", buffer);

         if (isTestCancelled(testID))
         {
             removeCancelledTest(testID);
             pclose(cmdFile);
             return MPE_SUCCESS;
         }
         if (strstr(buffer, "Tracing route to")
             || strstr(buffer,"over a maximum") || strlen(buffer) < 10)
         {
             continue;
         }
         if (strstr(buffer, "Unable to resolve target"))
         {
             strncpy(status, "Error_CannotResolveHostName", MPE_SOCKET_STATUS_SIZE);
             strncpy(info, buffer, MPE_SOCKET_ADDITIONAL_INFO_SIZE); 
             break;
         }

         memset(ipname,0,sizeof(ipname));
         memset(field1,0,sizeof(field1));
         memset(field2,0,sizeof(field2));
         memset(field3,0,sizeof(field3));
         memset(field4,0,sizeof(field4));
         memset(field5,0,sizeof(field5));
         memset(field6,0,sizeof(field6));
         memset(field7,0,sizeof(field7));
         memset(field8,0,sizeof(field8));
         sscanf(buffer,"%s %s %s %s %s %s %s %s", field1, field2, field3, field4,
             field5, field6, field7, field8);
               hopnumber = atoi(field1);
         if (strstr(field2,"<"))
         {
             rtt1 = 0.0;
         }
         else
         {
             rtt1 = (float) atoll(field2);
         }
         if (strstr(field4,"<"))
         {
             rtt2 = 0.0;
         }
         else
         {
             rtt2 = (float) atoll(field4);
         }
         if (strstr(field6,"<"))
         {
             rtt3 = 0.0;
         }
         else
         {
             rtt3 = (float) atoll(field6);
         }

         strncpy(ipname, field8, sizeof(ipname));
         if (hopnumber == hops && strstr(buffer, "*"))
         {
             strncpy(status, "Error_MaxHopCountExceeded", sizeof(status));
             break;
         }

         favgresp = (rtt1+rtt2+rtt3)/3;
         if (strstr(ipname,"*"))
         {
             ipname[0]=',';
         }
         strcat(hophosts ,ipname);
         strcat(hophosts ,",");
    }
    pclose(cmdFile);

    if (strlen(status) == 0)
    {
        strncpy(status, "Success", MPE_SOCKET_STATUS_SIZE);
        *avgresp = (int) favgresp;
        hophosts[strlen(hophosts)-1] = 0;
    }
    else
    {
        // not success zero out other results
        *avgresp = 0;
        hophosts[0] = 0;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "os_Traceroute results %s %s %d %s\n",status, info, *avgresp, hophosts);
    return MPE_SUCCESS;

}

/**
 * Issue a nslookup and return results
 *
 * @param   testID     id associated with test 
 * @param   host       host
 * @param   server     DNS server
 * @param   timeout    timeout in msec to wait for reply
 * @param   status     returned status
 * @param   info       returned additional info
 * @param   resultAnswer   returned DNS answer type
 * @param   resultName     returned fully qualified host name
 * @param   resultIPS      returned IP addresses returned DNS server
 * @param   resultServer   returned DNS Server IP address
 * @param   resultTime     returned time on msec of response
 *
*
 * @return 0 if successful, -1 if problems encountered
 */

int os_NSLookup(int testID, char* host, char *server, int timeout, char *status, char *info, char *resultAnswer, char *resultName, char *resultIPS, char *resultServer, int *resultTime)

{

    char buffer[1024];
    char cmd[128];
    int successcount = 0;
    char junk[2048];
    char tmp[256]="";


    //Range checks
    if (host == NULL || strlen(host) == 0)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid host parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }
    if (timeout < MPE_SOCKET_MIN_TIMEOUT || timeout > MPE_SOCKET_MAX_TIMEOUT)
    {
        strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
        strncpy(info, "invalid timeout parameter" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
        return MPE_SUCCESS;
    }

    // initial return values
    strncpy(status, "Success", MPE_SOCKET_STATUS_SIZE);
    info[0]=0;
    timeout = timeout/1000;

    char *nslookupCmd = "c:\\Windows\\system32\\nslookup";
    sprintf(cmd,"%s -timeout=%d %s %s 2>&1", nslookupCmd, timeout, host, server); 

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"cmd = %s\n", cmd);
    strncpy(resultName, host, MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE);
    strncpy(resultAnswer,"None", MPE_SOCKET_MAX_NSLOOKUP_ANSWER_RESULT_SIZE);
    struct timeval tv1, tv2;
    struct timezone tz1, tz2;
    long totalT = 0;;
    int dnsserverfound = 0;
    int addressesfound = 0;
    *info = 0;

    memset(resultIPS, 0, MPE_SOCKET_MAX_NSLOOKUP_IPS_RESULT_SIZE);
    // start timer
    gettimeofday(&tv1, &tz1);

    FILE *cmdFile = popen(cmd, "r");
    if (cmdFile == NULL)
    {
        return -1;
    }

    while (fgets(buffer, sizeof(buffer), cmdFile))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,"received %s\n", buffer);
        if (isTestCancelled(testID))
        {
            removeCancelledTest(testID);
            pclose(cmdFile);
            return MPE_SUCCESS;
        }
        if (addressesfound)
        {
            // strip white space
            strncpy(tmp, buffer, sizeof(tmp));
            char *ptr = tmp;
            while(isspace(*ptr))
            {
                ptr++;
            }
            char *end = ptr + strlen(ptr) - 1;
            while(end > ptr && isspace(*end))
            {
                end--;
            }
            *(end+1) = 0;

            if (*ptr != 0)
            {
                strcat(resultIPS, ptr);
                strcat(resultIPS, ",");
            }
        }
        if (strstr(buffer,"Can't find server address"))
        {
            // fatal lookup error
            strncpy(status, "Error_DNSServerNotResolved", MPE_SOCKET_STATUS_SIZE);
            strncpy(info, buffer , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
            pclose(cmdFile);
            return MPE_SUCCESS;
        }
        if (strstr(buffer,"can't find"))
        {
            // not fatal continue reps
            strncpy(status, "Error_HostNameNot-Resolved", MPE_SOCKET_STATUS_SIZE);
            break;
        }
        if (strstr(buffer,"DNS request timed out"))
        {
            // not fatal continue reps
            strncpy(status, "Error_Timeout", MPE_SOCKET_STATUS_SIZE);
            strncpy(info, buffer , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
            break;
        }
        if (strstr(buffer,"Non-authoritative answer:"))
        {
            strcpy(resultAnswer,"NonAuthoritative");
            continue;
        }
        if (strstr(buffer,"Name:") && strstr(buffer, host))
        {
            sscanf(buffer,"%s %s", junk, junk);
            strncpy(junk, resultName, MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE);
            continue;
        }
        if (strstr(buffer,"Address:") && !dnsserverfound)
        {
            successcount++;
            sscanf(buffer,"%s %s", junk, junk);
            strncpy(resultServer, junk, MPE_SOCKET_MAX_NSLOOKUP_SERVER_RESULT_SIZE);
            dnsserverfound = 1;
            continue;
        }
        if (strstr(buffer,"Addresses:"))
        {
            sscanf(buffer,"%s %s", junk, tmp);
            strcat(resultIPS, tmp);
            strcat(resultIPS, ",");
            addressesfound = 1;
        }
        // single resolved address found
        if (strstr(buffer,"Address:") && dnsserverfound)
        {
            sscanf(buffer,"%s %s", junk, tmp);
            strcat(resultIPS, tmp);
        }
     }
     // Get the end time
     gettimeofday(&tv2, &tz2);
     totalT = (tv2.tv_sec - tv1.tv_sec) * 1000 + ((tv2.tv_usec - tv1.tv_usec) / 1000);
     pclose(cmdFile);

     resultIPS[strlen(resultIPS)-1]=0;
     *resultTime = (int) totalT;

     MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "os_NSLookup status = %s info = %s resultAnswer = %s resultName = %s resultIPS = %s resultServer = %s resultTime = %d\n", status, info, resultAnswer, resultName, resultIPS, resultServer,*resultTime);
     return MPE_SUCCESS;
}

/**
 * Cancels a Ping/Traceroute or NSLookup Test
 *
 * @param   testID     id of test to cancel
 * Refer to os_socket.h for full description
 *
 * @return 0 if successful, -1 if problems encountered
 */
int os_CancelTest(int testID)
{
    addCancelledTest(testID);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "os_CancelTest test %d cancelled\n", testID);
    return MPE_SUCCESS;

}

/**
 * Sets a link local address on interface
 *
 * @param   interface   name of interface
 * Refer to mpeos_socket.h for full description
 *
 * @return  MPE_SUCCESS           if successful 
 *          MPE_ENODATA           if problems are encountered
 */
mpe_Error mpeos_socketSetLinkLocalAddress(char *interface)
{

    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_NET, "os_SetLinkLocalAddress not implemented\n");
    return -1;
}

/**
 * <i>mpeos_socketRegisterForIPChanges()</i>
 *
 * Registers to receive IP change notification on an interface
 *
 * Refer to mpeos_socket.h for full method description.
 *
 * @return  MPE_SUCCESS           if successful
 *          MPE_ENODATA           if any errors encountered. 
 */
mpe_Error mpeos_socketRegisterForIPChanges(char *interface, mpe_EventQueue queueId, void *act)
{
    g_ipChangeQueueId = queueId;
    g_ipChangeEdHandle = act;
    if (interface == NULL)
    {
        return MPE_ENODATA;
    }

    strncpy(g_interface, interface, sizeof(g_interface));
    return MPE_SUCCESS;
}

