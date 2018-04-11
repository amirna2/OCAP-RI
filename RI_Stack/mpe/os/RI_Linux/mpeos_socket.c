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

#include <stdio.h>
#include <netdb.h>
#include <sys/types.h>  /* getsockopt(2) */

/* gethostbyaddr(3), getpeername(2), getsockname(3), getsockopt(2) */
#include <sys/socket.h>

#include <sys/ioctl.h>
#include <sys/time.h>

#include <unistd.h>     /* close(2), fcntl(2), gethostname(2) */
#include <ctype.h>
#include <fcntl.h>
#include <stdlib.h>
#include <memory.h>
#include <string.h>

#include <net/if.h>
#include <netinet/in.h>
#include <net/if_arp.h>

#include <mpe_types.h>
#include <mpeos_socket.h>
#include <mpeos_thread.h>
#include <mpeos_mem.h>
#include <mpeos_dbg.h>
#include <mpeos_util.h>

#include <signal.h>

/* BYTEORDER(3): htonl(), htons(), ntohl(), ntohs() */
#include <netinet/in.h>

#include <ri_cablecard.h>

#define MPE_MEM_DEFAULT MPE_MEM_NET

// IP Change eventing
static mpe_EventQueue g_ipChangeQueueId = (mpe_EventQueue) -1;
static void *g_ipChangeEdHandle = NULL;
static char g_interface[32];

mpe_Mutex threadListMutex;
ThreadDesc* gBlockedThreadList = NULL;

void threadSignalHandler(int sig) { }

void addBlockedThread(ThreadDesc* t, int fd)
{
    t->td_blocked_fd = fd;

    mpe_mutexAcquire(threadListMutex);
    if (gBlockedThreadList != NULL)
    {
        t->td_next = gBlockedThreadList;
        gBlockedThreadList->td_prev = t;
    }
    gBlockedThreadList = t;

    mpe_mutexRelease(threadListMutex);
}

void removeBlockedThread(ThreadDesc* t)
{
    mpe_mutexAcquire(threadListMutex);

    if (t->td_prev != NULL)
    {
        t->td_prev->td_next = t->td_next;
    }
    else
    {
        gBlockedThreadList = t->td_next;
    }
   
    if (t->td_next != NULL)
    {
        t->td_next->td_prev = t->td_prev;
    }
    t->td_next = NULL;
    t->td_prev = NULL;
    
    mpe_mutexRelease(threadListMutex);

    t->td_blocked_fd = -1;
}

void interruptBlockedThreads(int fd)
{
    ThreadDesc* walker;
    
    mpe_mutexAcquire(threadListMutex);

    walker = gBlockedThreadList;
    while (walker != NULL)
    {
        if (walker->td_blocked_fd == fd)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET,
                      "%s: Interrupting blocked thread - %d.  Blocked on socket %d\n", __FUNCTION__, walker->td_id, fd);
            pthread_kill(walker->td_id, SIGUSR2);
        }
        walker = walker->td_next;
    }

    mpe_mutexRelease(threadListMutex);
}

#define MAX_CANCELLED_TESTS 10
int gCancelledTests[MAX_CANCELLED_TESTS]={0};
mpe_Mutex cancelledTestMutex;

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
int os_getMacAddress(char* displayName, char* macAddress)
{
    /* Get Ethernet interface information from the OS
     * (since we don't have a real RF interface on the Simulator)
     * Note: this string will be in the format "12:34:56:78:9A:BC" */

    struct ifreq ifr;
    struct ifreq *IFR;
    struct ifconf ifc;
    char buf[1024];
    int s, i;
    int ok = 0;
    char *NO_MAC = "00:00:00:00:00:00";

    s = socket(AF_INET, SOCK_DGRAM, 0);

    if (s != -1)
    {
        ifc.ifc_len = sizeof(buf);
        ifc.ifc_buf = buf;
        ioctl(s, SIOCGIFCONF, &ifc);
        IFR = ifc.ifc_req;

        for (i = ifc.ifc_len / sizeof(struct ifreq); --i >= 0; IFR++)
        {
            strcpy(ifr.ifr_name, IFR->ifr_name);

            if (ioctl(s, SIOCGIFFLAGS, &ifr) == 0)
            {
                if (((!(ifr.ifr_flags & IFF_LOOPBACK)) && (displayName == NULL)) ||
                		((displayName != NULL) && (strcmp(displayName, ifr.ifr_name) == 0)))
                {
                    if (ioctl(s, SIOCGIFHWADDR, &ifr) == 0)
                    {
                        ok = 1;
                        break;
                    }
                }
            }
        }

        close(s);

        if (ok)
        {
            // Get ethernet information.
            sprintf(buf, "%02x:%02x:%02x:%02x:%02x:%02x",
                    (unsigned char) ifr.ifr_hwaddr.sa_data[0],
                    (unsigned char) ifr.ifr_hwaddr.sa_data[1],
                    (unsigned char) ifr.ifr_hwaddr.sa_data[2],
                    (unsigned char) ifr.ifr_hwaddr.sa_data[3],
                    (unsigned char) ifr.ifr_hwaddr.sa_data[4],
                    (unsigned char) ifr.ifr_hwaddr.sa_data[5]);
            strcpy(macAddress, buf);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                    "os_getMacAddress: Unable to get mac\n");
            strcpy(macAddress, NO_MAC);
        }
    }

    return 0;
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
    char buf[1024] =
    { 0 };
    struct ifconf ifc =
    { 0 };
    struct ifreq *ifr = NULL;
    int sock = 0;
    int nInterfaces = 0;
    int i = 0;

    /* Map OS network interface type to OCAP HN type */
    int WIRED_ETHERNET = 2;
    int WIRELESS_ETHERNET = 3;
    int UNKNOWN = 0;

    /* Get a socket handle. */
    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
            "socket(AF_INET, SOCK_DGRAM, 0) returned %d!?\n", sock);
        return -1;
    }

    /* Query available interfaces. */
    // GORP: what if buffer is too small??
    ifc.ifc_len = sizeof(buf);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, &ifc) < 0)
    {
        close(sock);
    	MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
    	"Problems getting interface info from socket\n");
        return -1;
    }

    /* Iterate through the list of interfaces. */
    ifr = ifc.ifc_req;
    nInterfaces = ifc.ifc_len / sizeof(struct ifreq);
    for (i = 0; i < nInterfaces; i++)
    {
        struct ifreq *item = &ifr[i];

        if (strcmp(item->ifr_name, displayName) == 0)
        {
            if (ioctl(sock, SIOCGIFHWADDR, item) < 0)
            {
                close(sock);
            	MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
            	"Problems getting hardware address info from socket\n");
                return -1;
            }

        	// Determine this network interface type
        	switch (item->ifr_hwaddr.sa_family)
        	{
        	case ARPHRD_ETHER:
        	case ARPHRD_EETHER:
        		*type = WIRED_ETHERNET;
        		break;
        	case ARPHRD_IEEE802:
        		*type = WIRELESS_ETHERNET;
        		break;
        	default:
        		*type = UNKNOWN;
        		MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        			"os_getNetworkInterfaceType() - unrecognized interface type = %d\n",
        				item->ifr_hwaddr.sa_family);
        	}
        	break;
        }
    }

    close(sock);

    return 0;
}


/**
 * Initialize the MPE OS socket API
 */
mpe_Bool mpeos_socketInit(void)
{
    mpe_Bool ret = TRUE;
    struct sigaction mySigAction;

    mpeos_mutexNew(&threadListMutex);
    mpeos_mutexNew(&cancelledTestMutex);

    mySigAction.sa_handler = threadSignalHandler;
    sigemptyset(&mySigAction.sa_mask);
    mySigAction.sa_flags = 0;
    sigaction(SIGUSR2, &mySigAction, NULL);

    // TODO - verify Linux impl does NOT need to use mpeenv.ini NAMESERVERS value
    //     I.e., ARES artifact
    return (ret);
}

/**
 * Terminate the MPE OS socket API
 */
void mpeos_socketTerm(void)
{
    mpeos_mutexDelete(threadListMutex);
}

/**
 * Return status of last socket function that failed
 */
int mpeos_socketGetLastError(void)
{
    return errno;
}

/**
 * Accept a connection
 */
mpe_Socket mpeos_socketAccept(mpe_Socket socket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len)
{
    mpe_Socket s;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    s = accept(socket, (struct sockaddr*)address, address_len);
    removeBlockedThread(t);

    return s;
}

/**
 * Bind a name to a socket
 */
int mpeos_socketBind(mpe_Socket socket, const mpe_SocketSockAddr *address,
        mpe_SocketSockLen address_len)
{
    return bind(socket, (struct sockaddr*) address, address_len);
}

/**
 * Close a socket
 */
int mpeos_socketClose(mpe_Socket socket)
{
    int retVal = close((int) socket);
    interruptBlockedThreads(socket);
    return retVal;
}

/**
 * Make a connection on a socket
 */
int mpeos_socketConnect(mpe_Socket socket, const mpe_SocketSockAddr *address,
        mpe_SocketSockLen address_len)
{
    int retVal;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    retVal = connect(socket, (struct sockaddr*) address, address_len);
    removeBlockedThread(t);

    return retVal;
}

/**
 * Create a socket
 */
mpe_Socket mpeos_socketCreate(int domain, int type, int protocol)
{
    int sock;

    /*
     * Initiate fast boot if not in 2-way mode yet,
     * to insure that our network connection to the head-end is set-up
     */
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "CALLING mpeos_stbBoot() ...\n");
    mpeos_stbBoot( MPE_BOOTMODE_FAST);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "CALLING socket() ...\n");

    if ((sock = socket(domain, type, protocol)) != -1)
    {
        /* Always set this flag so that child processes automatically
           close this file descriptor */
        if (fcntl(sock, F_SETFD, FD_CLOEXEC) == -1)
        {
            return -1;
        }
    }
    return sock;
}

/**
 * Clear an FD from an FD set
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
 * Determine if an FD is a member of an FD set
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
 * Clear all entries from an FD set
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
 * Lookup a host by address
 */
mpe_SocketHostEntry *mpeos_socketGetHostByAddr(const void *addr,
        mpe_SocketSockLen len, int type)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "****** Looking up host by address\n");

    /***
     *** TODO - does Linux impl need to perform a deep-copy of "struct hostent"
     ***        to mpe_SocketHostEntry?
     ***/
    return (mpe_SocketHostEntry *) gethostbyaddr((const char *) addr,
            (int) len, type);
}

/**
 * Lookup a host by name
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
        /***
         *** TODO - does Linux impl need to perform a deep-copy of
         ***        "struct hostent" to mpe_SocketHostEntry?
         ***/
        hostEntry = (mpe_SocketHostEntry *) gethostbyname(name);
    }

    return hostEntry;
}

/**
 * Get the name of the current machine
 */
int mpeos_socketGetHostName(char *name, size_t namelen)
{
    int ret = gethostname(name, namelen);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%d = %s(%s)\n", ret, __func__, name);
    return ret;
}

/**
 * Get the locally-bound name of a socket
 */
int mpeos_socketGetSockName(mpe_Socket socket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len)
{
    return getsockname((int) socket, (struct sockaddr*)address, (socklen_t *) address_len);
}

/**
 * Get the value of a socket option
 */
int mpeos_socketGetOpt(mpe_Socket socket, int level, int option_name,
        void *option_value, mpe_SocketSockLen *option_len)
{
    return getsockopt((int) socket, level, option_name, option_value,
            (socklen_t *) option_len);
}

/**
 * Get the name of a sockets' peer
 */
int mpeos_socketGetPeerName(mpe_Socket socket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len)
{
    return getpeername((int) socket, (struct sockaddr*)address, (socklen_t *) address_len);
}

/**
 * Convert host long to network byte order
 */
uint32_t mpeos_socketHtoNL(uint32_t hostlong)
{
    return htonl(hostlong);
}

/**
 * Convert host short to network byte order
 */
uint16_t mpeos_socketHtoNS(uint16_t hostshort)
{
    return htons(hostshort);
}

/**
 * Convert network long to host byte order
 */
uint32_t mpeos_socketNtoHL(uint32_t netlong)
{
    return ntohl(netlong);
}

/**
 * Convert network short to host byte order
 */
uint16_t mpeos_socketNtoHS(uint16_t netshort)
{
    return ntohs(netshort);
}

/**
 * Perform a control function on a socket
 */
int mpeos_socketIoctl(mpe_Socket socket, int request, ...)
{
    int result, *iargp;
    unsigned long longarg;
    va_list ap;
    va_start(ap, request);

    // Processing depends on request code
    switch (request)
    {
    // Set or clear non-blocking flag
    case MPE_SOCKET_FIONBIO:
        iargp = va_arg(ap, int*);
        longarg = *iargp;
        result = fcntl((int) socket, F_SETFL, (longarg == 0) ? 0 : O_NONBLOCK);
        break;

        // Get number of bytes ready
    case MPE_SOCKET_FIONREAD:
        // Linux: FIONREAD == TIOCINQ
        result = ioctl((int) socket, TIOCINQ, (caddr_t) & longarg);
        iargp = va_arg(ap, int*);
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
 * Listen for a connection on a socket
 */
int mpeos_socketListen(mpe_Socket socket, int backlog)
{
    int retVal;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    retVal = listen(socket, backlog);
    removeBlockedThread(t);

    return retVal;

}

/**
 * Convert an ascii formatted address (in dot notation) to its numeric format.
 *
 * Copyright (c) 1983, 1990, 1993
 *      The Regents of the University of California.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *      This product includes software developed by the University of
 *      California, Berkeley and its contributors.
 * 4. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
int mpeos_socketAtoN(const char *strptr, mpe_SocketIPv4Addr *addrptr)
{
    uint32_t val;
    int32_t base, n;
    char c;
    uint32_t parts[4];
    uint32_t *pp = parts;

    for (;;)
    {
        /*
         * Collect number up to ".". Values are specified as for C:
         * 0x=hex, 0=octal, other=decimal.
         */
        val = 0;
        base = 10;
        if (*strptr == '0')
        {
            if (*++strptr == 'x' || *strptr == 'X')
                base = 16, strptr++;
            else
                base = 8;
        }
        while ((c = *strptr) != '\0')
        {
            if (isdigit((unsigned char) c))
            {
                val = (val * base) + (c - '0');
                strptr++;
                continue;
            }
            if (base == 16 && isxdigit((unsigned char) c))
            {
                val = (val << 4) + (c + 10 - (islower((unsigned char) c) ? 'a'
                        : 'A'));
                strptr++;
                continue;
            }
            break;
        }
        if (*strptr == '.')
        {
            /*
             * Internet format:
             *    a.b.c.d (with d treaded as 8 bits)
             *    a.b.c   (with c treated as 16 bits)
             *    a.b     (with b treated as 24 bits)
             *    a       (with a treated as 32 bits)
             */
            if (pp >= parts + 3 || val > 0xff)
                return 0;
            *pp++ = val, strptr++;
        }
        else
            break;
    }

    /*
     * Check for trailing junk.
     */
    while (*strptr)
        if (!isspace((unsigned char) *strptr++))
            return 0;

    /*
     * Concoct the address according to the number of parts specified.
     */
    n = pp - parts + 1;
    switch (n)
    {

    case 1: /* a -- 32 bits */
        break;

    case 2: /* a.b -- 8.24 bits */
        if (val > 0xffffff)
            return 0;
        val |= parts[0] << 24;
        break;

    case 3: /* a.b.c -- 8.8.16 bits */
        if (val > 0xffff)
            return 0;
        val |= (parts[0] << 24) | (parts[1] << 16);
        break;

    case 4: /* a.b.c.d -- 8.8.8.8 bits */
        if (val > 0xff)
            return 0;
        val |= (parts[0] << 24) | (parts[1] << 16) | (parts[2] << 8);
        break;
    }
    if (addrptr)
        addrptr->s_addr = mpeos_socketHtoNL(val);
    return 1;
}

/**
 * Convert a numeric address to its ascii format in dot notation
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
    return inet_ntop(af, src, dst, size);
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
    return inet_pton(af, src, dst);
}

/**
 * Receive a message on a socket
 */
size_t mpeos_socketRecv(mpe_Socket socket, void *buffer, size_t length,
        int flags)
{
    size_t retVal;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    retVal = recv(socket, buffer, length, flags);
    removeBlockedThread(t);

    return retVal;
}

/**
 * Receive a message on a socket and determine its sender
 */
size_t mpeos_socketRecvFrom(mpe_Socket socket, void *buffer, size_t length,
        int flags, mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len)
{
    size_t retVal;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    retVal = recvfrom(socket, buffer, length, flags, (struct sockaddr*)address, address_len);
    removeBlockedThread(t);

    return retVal;
}

/**
 * Block waiting for one or more FDs to become ready for I/O
 */
int mpeos_socketSelect(int numfds, mpe_SocketFDSet *readfds,
        mpe_SocketFDSet *writefds, mpe_SocketFDSet *errorfds,
        const mpe_TimeVal *timeout)
{
    return select(numfds, readfds, writefds, errorfds, (mpe_TimeVal *) timeout);
}

/**
 * Send a message
 */
size_t mpeos_socketSend(mpe_Socket socket, const void *buffer, size_t length,
        int flags)
{
    size_t retVal;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    /*
     * MSG_NOSIGNAL is OR'd into flags to insure that a client dropping a connection
     * does not raise a signal which is currently not handled and would cause the
     * ri to exit.
     */
    retVal = send(socket, buffer, length, flags | MSG_NOSIGNAL);
    removeBlockedThread(t);

    return retVal;
}

/**
 * Send a message to the specified address
 */
size_t mpeos_socketSendTo(mpe_Socket socket, const void *message,
        size_t length, int flags, const mpe_SocketSockAddr *dest_addr,
        mpe_SocketSockLen dest_len)
{
    size_t retVal;

    ThreadDesc* t;
    mpeos_threadGetCurrent(&t);

    addBlockedThread(t, socket);
    /*
     * MSG_NOSIGNAL is OR'd into flags to insure that a client dropping a connection
     * does not raise a signal which is currently not handled and would cause the
     * ri to exit.
     */
    retVal = sendto(socket, message, length, flags | MSG_NOSIGNAL, (struct sockaddr*)dest_addr, dest_len);
    removeBlockedThread(t);

    return retVal;
}

/**
 * Set the value of a socket option
 */
int mpeos_socketSetOpt(mpe_Socket socket, int level, int option_name,
        const void *option_value, mpe_SocketSockLen option_len)
{
    return setsockopt(socket, level, option_name, (char *) option_value,
            option_len);
}

/**
 * Shut down a socket connection
 */
int mpeos_socketShutdown(mpe_Socket socket, int how)
{
    return shutdown(socket, how);
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
 * Add an interface to the list. If known interface just link
 * a new address onto the list. If new interface create new
 * mpe_SocketNetIf structure.
 */
mpe_SocketNetIfList *addNetIf(mpe_SocketNetIfList *ifs, char *if_name,
        int if_index, int family, mpe_SocketSockAddr *addr, int addrlen)
{
    mpe_SocketNetIfList *currif = ifs;
    mpe_SocketNetAddr *addrP;
    char name[MPE_SOCKET_IFNAMSIZ];
    char *unit;

    /*
     * If the interface name is longer than we can support,
     * return an un-updated list.
     */
    if (strlen(if_name) >= MPE_SOCKET_IFNAMSIZ)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
                "addNetIf: Interface name '%s' is too long\n", if_name);
        return ifs;
    }

    /*
     * If the interface name is a logical interface then we
     * remove the unit number so that we have the physical
     * interface (eg: hme0:1 -> hme0). NetworkInterface
     * currently doesn't have any concept of physical vs.
     * logical interfaces.
     */
    strcpy(name, if_name);
    unit = strchr(name, ':');
    if (unit != NULL)
        *unit = '\0';

    /*
     * Create and populate the netaddr node. If allocation fails
     * return an un-updated list.
     */
    if ((addrP = (mpe_SocketNetAddr *) malloc(sizeof(mpe_SocketNetAddr)))
            != NULL)
    {
        if ((addrP->if_addr = (mpe_SocketSockAddr *) malloc(addrlen)) == NULL)
        {
            free(addrP);
            return ifs;
        }
    }
    else
    {
        return ifs;
    }
    memcpy(addrP->if_addr, addr, addrlen);
    addrP->if_family = family;

    /*
     * Make sure this is a new interface.
     */
    while (currif != NULL)
    {
        /* Compare interface names. */
        if (strcmp(name, currif->if_name) == 0)
            break;
        currif = currif->if_next;
    }

    /*
     * If not found, create a new one.
     */
    if (currif == NULL)
    {
        /* Allocate a new interface structure. */
        if ((currif = (mpe_SocketNetIfList *) malloc(
                sizeof(mpe_SocketNetIfList))) != NULL)
        {
            /* Copy the interface name. */
            strcpy(currif->if_name, name);
        }
        else
        {
            /* Free address structure on interface allocation error. */
            free(addrP->if_addr);
            free(addrP);
            return ifs;
        }
        /* Link new interface into the list. */
        currif->if_index = if_index;
        currif->if_addresses = NULL;
        currif->if_next = ifs;
        ifs = currif;
    }

    /*
     * Insert the address structure onto the interface list.
     */
    addrP->if_next = currif->if_addresses;
    currif->if_addresses = addrP;

    return ifs;
}

/*
 * Acquire the list of IPv4 interfaces and add each
 * interface and its addresses to the list of interfaces.
 */
mpe_SocketNetIfList *getIPv4Interfaces(mpe_SocketNetIfList *ifs)
{
    char buf[1024] =
    { 0 };
    struct ifconf ifc =
    { 0 };
    struct ifreq *ifr = NULL;
    int sock = 0;
    int nInterfaces = 0;
    int i = 0;

    /* Get a socket handle. */
    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock < 0)
    {
        return NULL;
    }

    /* Query available interfaces. */
    // GORP: what if buffer is too small??
    ifc.ifc_len = sizeof(buf);
    ifc.ifc_buf = buf;
    if (ioctl(sock, SIOCGIFCONF, &ifc) < 0)
    {
        close(sock);
        return NULL;
    }

    /* Iterate through the list of interfaces. */
    ifr = ifc.ifc_req;
    nInterfaces = ifc.ifc_len / sizeof(struct ifreq);
    for (i = 0; i < nInterfaces; i++)
    {
        struct ifreq *item = &ifr[i];

        struct sockaddr_storage *addr = (struct sockaddr_storage*)&(item->ifr_addr);
        ifs = addNetIf(ifs, item->ifr_name, item->ifr_ifindex, AF_INET, addr,
                sizeof(struct sockaddr_storage));
    }

    close(sock);
    return ifs;
}

#define PROCNET_IF_INET6_PATH "/proc/net/if_inet6"

/*
 * Acquire the list of IPv4 interfaces and add each
 * interface and its addresses to the list of interfaces.
 */
mpe_SocketNetIfList *getIPv6Interfaces(mpe_SocketNetIfList *ifs)
{
    FILE *ifinet6FILE;

    if ((ifinet6FILE = fopen(PROCNET_IF_INET6_PATH, "r")) != NULL)
    {
        char ifName[30];
        char addrSegments[8][5];
        int plen, scope, status, idx;

        // Read the interface information from /proc/net/if_inet6
        while (fscanf(ifinet6FILE, "%4s%4s%4s%4s%4s%4s%4s%4s %02x %02x %02x %02x %30s\n",
                      addrSegments[0], addrSegments[1], addrSegments[2], addrSegments[3],
                      addrSegments[4], addrSegments[5], addrSegments[6], addrSegments[7],
                      &idx, &plen, &scope, &status, ifName) != EOF)
        {
            struct sockaddr_storage addr;
            char addrStr[40];     // 32 address chars + 7 ':' + null char

            memset(&addr, 0, sizeof(struct sockaddr_in6));
            addr.ss_family = AF_INET6;

            // Write the address out to a format parseable by inet_pton()
            sprintf(addrStr, "%s:%s:%s:%s:%s:%s:%s:%s",
                    addrSegments[0], addrSegments[1], addrSegments[2], addrSegments[3],
                    addrSegments[4], addrSegments[5], addrSegments[6], addrSegments[7]);
            inet_pton(AF_INET6, addrStr, (void*)((struct sockaddr_in6*)&addr)->sin6_addr.s6_addr);

            ifs = addNetIf(ifs, ifName, idx, AF_INET6,
                           (struct sockaddr_storage *)&addr,
                           sizeof(struct sockaddr_storage));
        }
        fclose(ifinet6FILE);
    }
    return ifs;
}

/**
 * Get the network interfaces.
 */
mpe_Error mpeos_socketGetInterfaces(mpe_SocketNetIfList **netIfList)
{
    /* Try IPv4 interfaces first... */
    *netIfList = getIPv4Interfaces(NULL);

    /* Try IPv6 interfaces if we have no IPv4 ifs */
    if (*netIfList == NULL)
    {
        *netIfList = getIPv6Interfaces(*netIfList);
    }

    /* Get all interfaces - this should have worked, but it doesn't! */
    //*netIfList = getInterfaces( NULL, AF_UNSPEC );

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

    // check value passed in
    if (value != MPE_SOCKET_DLNA_QOS_0 && value != MPE_SOCKET_DLNA_QOS_1 &&
        value != MPE_SOCKET_DLNA_QOS_2 && value != MPE_SOCKET_DLNA_QOS_3)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "%s: invalid value = 0x%x\n", __FUNCTION__,value);
        return MPE_EINVAL;
    }

    // Set the IP_TOS option and SO_PRIORITY field to map to DSCP
    int tosOpt = (int) value << 2;
    int result = mpeos_socketSetOpt(sock, SOL_IP, IP_TOS, &tosOpt, sizeof(tosOpt));
    if (result == -1)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "%s: failed setting IP_TOS err = %d\n", __FUNCTION__, errno);
        return MPE_ENODATA;
    }
    int soPriOpt = (int) value >> 3;
    result = mpeos_socketSetOpt(sock, SOL_SOCKET, SO_PRIORITY, &soPriOpt,
        sizeof(soPriOpt));
    if (result == -1)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "%s: failed setting SO_PRIORITY err = %d\n", __FUNCTION__, errno);
        return MPE_ENODATA;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%s: DSCP value set to 0x%x\n", __FUNCTION__, value);
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
        dlnaNetworkIfInfo->wakeOnPattern = "FFFFFFFFFFFF"; // temporary
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
 * @param   testID     id of associated test 
 * @param   host       host to ping
 * @param   reps       number of requests to send
 * @param   interval   time in msec between sending requests 
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
 * Refer to os_socket.h for full description
 *
 * @return 0 if successful, -1 if problems encountered
 */

int os_Ping(int testID, char* host, int reps, int interval, int timeout, int blocksize, int dscp, char *status, char *info, int *successes, int *fails, int *retAvg, int *retMin, int *retMax)
{
    char cmd[128];
    char buffer[2048];
    int success= 0;
    int failed= 0;
    int transmitted = 0;
    int received = 0;
    int min= 0;
    int max= 0;
    int avg= 0;
    status[0] = 0;
    info[0] = 0;


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
    if (interval < MPE_SOCKET_PING_MIN_INTERVAL || interval > MPE_SOCKET_PING_MAX_INTERVAL) 
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
    timeout = timeout/1000;

    sprintf(cmd,"%s %s %d %s %d %s %d %s %d %s %d %s 2>&1",
        "/bin/ping ","-c", reps, "-W", timeout, "-i",interval,"-s",blocksize, "-Q", tos, host);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "os_Ping cmd %s\n",cmd);

    FILE *cmdFile = popen(cmd,"r");
    if (cmdFile == NULL)
    {
       return -1;
    }
    while (fgets(buffer, sizeof(buffer), cmdFile)) {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
            "os_Ping buffer %s\n",buffer);
      if (isTestCancelled(testID))
      {
          removeCancelledTest(testID);
          strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
          strncpy(info, "test canceled" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
          pclose(cmdFile);
          return MPE_SUCCESS;
      }
      if (strstr(buffer, "packet loss"))
      {
          char *result = NULL;
          result = strtok(buffer,",");
          sscanf(result,"%d", &transmitted);
          result = strtok(NULL,",");
          sscanf(result,"%d", &received);
      }
      if (strstr(buffer, "min"))
      {
          char *result = NULL;
          result = strtok(buffer,"=");
          result = strtok(NULL,"=");
          result = strtok(result,"/");
          sscanf(result,"%d", &min);
          result = strtok(NULL,"/");
          sscanf(result,"%d", &avg);
          result = strtok(NULL,"/");
          sscanf(result,"%d", &max);
      }

      // check for unknown host
      if (strstr(buffer, "ping: unknown host"))
      {
          strncpy(status, "Error_CannotResolveHostName", MPE_SOCKET_STATUS_SIZE);
          break;
      }
      // check for other error 
      if (strstr(buffer, "ping:"))
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

    success = received;
    failed = transmitted - received;

    *successes = success;
    *fails = failed;
    *retMin = min;
    *retMax = max;
    *retAvg = avg; 
    
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "os_Ping results %s %d %d %d %d %d\n",status,
            *successes,*fails,*retMin, *retMax, *retAvg);
    return MPE_SUCCESS;
}

/**
 * Issue a traceroute and return results
 *
 * @param   testID     id of associated test 
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
 * Refer to os_socket.h for full description
 *
 * @return 0 if successful, -1 if problems encountered
 */

int os_Traceroute(int testID, char* host, int hops, int timeout, int blocksize, int dscp, char *status, char *info, int *avgresp, char *hophosts)
{
    
    char cmd[128];
    char buffer[204800];
    float rtt1 = 0;
    float rtt2 = 0;
    float rtt3 = 0;
    int hopnumber =0;
    char hostname[128], ipname[128], junk[128];
    float favgresp = 0;
    char namelist[2048]="";

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
    timeout = timeout/1000;
    int tos = dscp >> 2;

    sprintf(cmd,"%s %s %d %s %d %s %d %s %d 2>&1\n",
        "export PATH=$PATH:/sbin:/usr/sbin ; traceroute","-t",tos,"-w",timeout,"-m",hops, host, blocksize);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%s", cmd);
    FILE *cmdFile = popen(cmd,"r");
    if (cmdFile == NULL)
    {
       return -1;
    }
    while (fgets(buffer, sizeof(buffer), cmdFile))
    {
      if (isTestCancelled(testID))
      {
          removeCancelledTest(testID);
          strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
          strncpy(info, "test cancelled" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
          pclose(cmdFile);
          return MPE_SUCCESS;
      }
      if (strstr(buffer, "traceroute to"))
      {
          continue;
      }
      if (strstr(buffer, "Name or service not known"))
      {
          strncpy(status, "Error_CannotResolveHostName", MPE_SOCKET_STATUS_SIZE);
          break;
      }
      sscanf(buffer,"%d %s %s %f %s %f %s %f",
          &hopnumber, hostname, ipname, &rtt1, junk, &rtt2, junk, &rtt3);

      if (hopnumber == hops && strstr(buffer, "*"))
      {
          strncpy(status, "Error_MaxHopCountExceeded", MPE_SOCKET_STATUS_SIZE);
          break;
        
      }   

      favgresp = (rtt1+rtt2+rtt3)/3;
      if (strstr(ipname,"*"))
      {
          ipname[0]=',';
      }
      else
      {
          ipname[strlen(ipname)-1]=0;
          strcpy(ipname, (char *)&ipname[1]);
      }
      strcat(namelist,ipname);
      strcat(namelist,",");
    }

    namelist[strlen(namelist)-1] = 0;
    pclose(cmdFile);

    if (strlen(status) == 0)
    {
        strncpy(status, "Success", MPE_SOCKET_STATUS_SIZE);  
        strncpy(hophosts, namelist, MPE_SOCKET_MAX_TRACEROUTE_HOSTS);
        *avgresp = (int) favgresp;
    }
    else
    {
        *avgresp = 0;
        hophosts[0] = 0;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET,
        "os_Traceroute results  status = %s avgresp = %d hophosts = %s\n", status , *avgresp, hophosts);
    return MPE_SUCCESS;
}

/**
 * Issue a nslookup and return results
 *
 * @param   testID     id of associated test 
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
 * Refer to os_socket.h for full description
 *
 * @return 0 if successful, -1 if problems encountered
 */

int os_NSLookup(int testID, char* host, char *server, int timeout, char *status, char *info, char *resultAnswer, char *resultName, char *resultIPS, char *resultServer, int *resultTime)
{
    char cmd[128];
    char buffer[2048];
    char junk[2048];
    char tmp[256]="";
    int successcount = 0;


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

    strncpy(status, "Success", MPE_SOCKET_STATUS_SIZE);
    // initial return values
    info[0]=0;
    timeout = timeout/1000;

    sprintf(cmd,"%s%s%d %s %s 2>&1\n",
        "/usr/bin/nslookup ","-timeout=",timeout, host, server);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%s", cmd);
    strncpy(resultName, host, MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE);
    strncpy(resultAnswer,"None", MPE_SOCKET_MAX_NSLOOKUP_ANSWER_RESULT_SIZE);
    struct timeval tv1, tv2;
    struct timezone tz1, tz2;
    long totalT = 0;;
    int serverfound = 0;
    int dnsserverfound = 0;
    *info = 0;

    // start timer
    gettimeofday(&tv1, &tz1);

    FILE *cmdFile = popen(cmd,"r");
    if (cmdFile == NULL)
    {
        return -1;
    }
    while (fgets(buffer, sizeof(buffer), cmdFile))
    {
       memset(junk, 0, sizeof(junk));
       memset(tmp, 0, sizeof(tmp));
 
       MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_NET, "%s", buffer);
       if (isTestCancelled(testID))
       {
           removeCancelledTest(testID);
           strncpy(status, "Error_Other", MPE_SOCKET_STATUS_SIZE);
           strncpy(info, "test cancelled" , MPE_SOCKET_ADDITIONAL_INFO_SIZE);
           pclose(cmdFile);
           return MPE_SUCCESS;
       }
       if (strstr(buffer,"server can't find"))
       {
           strncpy(status, "Error_HostNameNot-Resolved", MPE_SOCKET_STATUS_SIZE);
       }
       if (strstr(buffer,"Non-authoritative answer:"))
       {
           strcpy(resultAnswer,"NonAuthoritative");
       }
       if (strstr(buffer,"Name:") && strstr(buffer, host))
       {
           sscanf(buffer,"%s %s", tmp, junk);
           strncpy(resultName, junk, MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE);
       }
       if (strstr(buffer,"Server:"))
       {
           successcount++;
           strcpy(resultAnswer, "Authoritative");
           serverfound = 1;
       }
       if (strstr(buffer,"Address:"))
       {
           if (serverfound)
           {
               //this is server address
               sscanf(buffer,"%s %s", tmp, junk);
               char *iponly = strtok(junk,"#");
               strncpy(resultServer, iponly, MPE_SOCKET_MAX_NSLOOKUP_SERVER_RESULT_SIZE);
               serverfound = 0;
               dnsserverfound = 1;
               continue;
           }
           sscanf(buffer,"%s %s", junk, tmp);
           strcat(resultIPS, tmp);
           strcat(resultIPS, ",");
       }
    }
    // Get the end time
    gettimeofday(&tv2, &tz2);
    totalT = (tv2.tv_sec - tv1.tv_sec) * 1000 + ((tv2.tv_usec - tv1.tv_usec) / 1000);
    pclose(cmdFile);

    resultIPS[strlen(resultIPS)-1]=0;
    *resultTime = (int) totalT;
    
    if (!dnsserverfound)
    {
        strncpy(status, "Error_DNSServerNotResolved", MPE_SOCKET_STATUS_SIZE);
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET,
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
 * Check to see if an arp entry exists for an address 
 *
 * @param   addr    point to address 
 *
 * @return  1 if exists 0 or -1 otherwise   
 */
static int checkArp(char *addr, char *interface)
{
    int sock = -1;
    struct arpreq arprequest;
    struct sockaddr_in *sockin_ptr = NULL;
    int found = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET, "checkArp %s %s\n", addr, interface);

    // check arp cache to see if we got arp entry for this address 
    sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock == -1)
    {
        return -1;
    }

    sockin_ptr = (struct sockaddr_in *)&arprequest.arp_pa;
    sockin_ptr->sin_family = AF_INET;
    sockin_ptr->sin_addr.s_addr = inet_addr(addr);
    strcpy(arprequest.arp_dev, interface);

    if (ioctl(sock, SIOCGARP, &arprequest) >= 0)
    {
        // Lookup complete..we found entry
        if (arprequest.arp_flags & ATF_COM)
        {
            found = 1;
        }
    }
 
    close(sock);
    return found;
}

/**
 * Get's unused link local address 
 *
 * @param   addr    point to address 
 *
 * @return  addr   
 */
static int getLinkLocal(char *interface, char *addr)
{
    char mac[32] = {0};
    int i = 0;
    int network[20] = {0};
    int host[20] = {0};
    char cmd[64] = {0};
    char buffer[256] = {0};
    int foundAddr = -1;


    if ((interface == NULL) || (addr == NULL))
    {
        return -1;
    }

    // get the mac address
    os_getMacAddress(interface, mac);
    // create the seeds
    unsigned int seed1 = (mac[12] << 4 | mac[13]) << 8 | (mac[15] << 4 | mac[16]);
    unsigned int seed2 = (mac[9] << 4 | mac[10]) << 8 | (mac[15] << 4 | mac[16]);
    srand(seed1);
    // get 20 random numbers for last 2 bytes of link local between 1 and 255
    for ( i = 0 ; i < 20; i++)
    {
        network[i] = rand()%255 + 1;
    }
    srand(seed2);
    for ( i = 0 ; i < 20; i++)
    {
        host[i] = rand()%255 + 1;
    }

    for ( i = 0 ; i < 20; i++)
    {
        sprintf(addr, "%s.%d.%d", "169.254", network[i], host[i]);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET, "getLinkLocal %s\n", addr);
        // now ping to force arp
        sprintf(cmd, "/bin/ping -c 1 %s 2>&1\n", addr);
        FILE *cmdFile = popen(cmd,"r");
        if (cmdFile == NULL)
        {
            return -1;
        }
        while (fgets(buffer, sizeof(buffer), cmdFile))
        {
        }
        pclose(cmdFile);
        // now check arp table
        if (checkArp(addr, interface) != 1)
        {
            // didn't find entry.. this address is good to use
            foundAddr = 0;    
            break;
        }
    }
 
    return foundAddr;

}
/**
 * Checks if interface has link local address 
 *
 * @param   interface   interface to check 
 *
 * @return 0 if interface has link local, -1 otherwise  
 */
static int hasLinkLocal(char *interface)
{
    int sock = -1;
    struct ifconf ifconfig;
    struct ifreq ifrequest[50];
    int i = 0;
    char ipaddr[32] = {0};
    struct sockaddr_in *sockin_ptr = NULL;
    int found = -1;

    // Get list of interfaces and associated addresses and 
    // see if the passed in one has an IPV4 link local address
    sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == -1)
    {
        return -1;
    }

    ifconfig.ifc_buf = (char *) ifrequest;
    ifconfig.ifc_len = sizeof ifrequest;

    if (ioctl(sock, SIOCGIFCONF, &ifconfig) == -1)
    {
        close(sock);
        return -1;
    }

    for (i = 0; i < (ifconfig.ifc_len / sizeof(ifrequest[0])); i++)
    {
        sockin_ptr = (struct sockaddr_in *) &ifrequest[i].ifr_addr;
        if (inet_ntop(AF_INET, &sockin_ptr->sin_addr, ipaddr, sizeof(ipaddr)) == NULL)
        {
            continue;
        }
        if (strstr(ipaddr, "169.254"))
        {
            found = 0;
            break;
        }
    }

    close(sock);
    return found;

}

/**
 * Sets a link local address on interface 
 *
 * @param   interface   name of interface 
 * Refer to mpeos_socket.h for full description
 *
 * @return MPE_SUCCESS           if successful
 *         MPE_ENODATA           if problems are encountered
 */
mpe_Error mpeos_socketSetLinkLocalAddress(char *interface)
{

    char cmd[128];
    char buffer[2048];
    int retVal = MPE_SUCCESS;
    char addr[32] = {0}; 
  
    // Does interface already have link local
    if (hasLinkLocal(interface) == 0)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET, "os_SetLinkLocalAddress linklocal already present");
        return retVal;
    } 

    // Get a unused local link address
    if (getLinkLocal(interface, addr) != 0)
    {
        return MPE_ENODATA;
    }

    // Now add the address to the interface 
    sprintf(cmd,"%s %s%s %s %s 2>&1",
        "ip addr add ", addr,"/16", "dev", interface);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET, "os_SetLinkLocalAddress cmd %s\n",cmd);

    FILE *cmdFile = popen(cmd,"r");
    if (cmdFile == NULL)
    {
       return MPE_ENODATA;
    }
    while (fgets(buffer, sizeof(buffer), cmdFile))
    {
        // shouldn't drop into here on success
        retVal = MPE_ENODATA;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_NET,
            "os_SetLinkLocalAddress buffer %s\n",buffer);
    }
    pclose(cmdFile);
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_NET, "os_SetLinkLocalAddress %s retVal = %d\n", 
        interface, retVal);
    if (retVal == MPE_SUCCESS)
    {
        // persist that platform set link local
        sprintf(cmd,"%s%s%s%s%s 2>&1", "/usr/bin/touch /tmp/ri_",addr,"_", interface,
            ".linklocal");   
        system(cmd);
    }
 
    return retVal;

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

