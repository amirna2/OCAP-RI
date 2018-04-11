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
 * The MPE OS Sockets API. This API provides a consistent interface to BSD style sockets
 * regardless of the underlying operating system.
 *
 * @author Todd Earles - Vidiom Systems Corporation
 */

#ifndef _MPEOS_SOCKET_H_
#define _MPEOS_SOCKET_H_
#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_error.h>
#include <mpeos_event.h>
#include <mpeos_time.h>
#include <os_socket.h>

/****************************************************************************************
 *
 * PROTOCOL INDEPENDENT DEFINITIONS
 *
 ***************************************************************************************/

/**
 * The maximum number of file descriptors represented in the mpe_SocketFDSet datatype.
 * @see POSIX definition of FD_SETSIZE
 */
#define MPE_SOCKET_FD_SETSIZE OS_SOCKET_FD_SETSIZE

/**
 * The maximum length of a host name in bytes (including the terminating NULL byte).
 * @see POSIX definition of HOST_NAME_MAX
 */
#define MPE_SOCKET_MAXHOSTNAMELEN OS_SOCKET_MAXHOSTNAMELEN

/**
 * Used to send or receive out-of-band data on sockets that support out-of-band data. The
 * significance and semantics of out-of-band data are protocol-specific.
 * @see POSIX definition of MSG_OOB
 */
#define MPE_SOCKET_MSG_OOB OS_SOCKET_MSG_OOB

/**
 * Peeks at an incoming message. The data is treated as unread and the next receive
 * operation shall still return this data.
 * @see POSIX definition of MSG_PEEK
 */
#define MPE_SOCKET_MSG_PEEK OS_SOCKET_MSG_PEEK

/**
 * Used with mpeos_socketShutdown() to disables further receive operations.
 * @see POSIX definition of SHUT_RD
 */
#define MPE_SOCKET_SHUT_RD OS_SOCKET_SHUTDOWN_RD

/**
 * Used with mpeos_socketShutdown() to disables further send and receive operations.
 * @see POSIX definition of SHUT_RDWR
 */
#define MPE_SOCKET_SHUT_RDWR OS_SOCKET_SHUTDOWN_RDWR

/**
 * Used with mpeos_socketShutdown() to disables further send operations.
 * @see POSIX definition of SHUT_WR
 */
#define MPE_SOCKET_SHUT_WR OS_SOCKET_SHUTDOWN_WR

/**
 * A datagram based socket
 * @see POSIX definition of SOCK_DGRAM
 */
#define MPE_SOCKET_DATAGRAM OS_SOCKET_DATAGRAM

/**
 * A stream based socket
 * @see POSIX definition of SOCK_STREAM
 */
#define MPE_SOCKET_STREAM OS_SOCKET_STREAM

/**
 * A value which indicates that an mpe_Socket descriptor is not valid.
 * @see POSIX definition of INVALID_SOCKET
 */
#define MPE_SOCKET_INVALID_SOCKET OS_SOCKET_INVALID_SOCKET

/**
 * Used with mpeos_socketIoctl() to clear or turn on the non-blocking flag for the socket.
 * @see POSIX definition of FIONBIO
 */
#define MPE_SOCKET_FIONBIO OS_SOCKET_FIONBIO

/**
 * Used with mpeos_socketIoctl() to return the number of bytes currently in the receive
 * buffer for the socket.
 * @see POSIX definition of FIONREAD
 */
#define MPE_SOCKET_FIONREAD OS_SOCKET_FIONREAD

/**
 * This is a numeric type used to reference an open socket file descriptor
 * @see POSIX defines a socket FD as an int
 */
typedef os_Socket mpe_Socket;

/**
 * This type defines a length of a socket structure
 * @see POSIX definition of socklen_t
 */
typedef os_SocketSockLen mpe_SocketSockLen;

/**
 * This is an opaque structure used by mpeos_socketSelect() to define a set of file
 * descriptors
 * @see POSIX definition of fd_set
 */
typedef os_SocketFDSet mpe_SocketFDSet;

/**
 * The host entry returned by calls to mpeos_socketGetHostByAddr() and mpeos_socketGetHostByName().
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     char *h_name - official (canonical) name of host
 * <li>     char **h_aliases - pointer to array of pointers to alias names
 * <li>     int h_addrtype - host address type
 * <li>     int h_length - length of address
 * <li>     char **h_addr_list - pointer to array of pointers with IPv4 or IPv6 addresses
 * </ul>
 * @see POSIX definition of hostent
 */
typedef os_SocketHostEntry mpe_SocketHostEntry;

/**
 * The addr info returned in calls to mpeos_socketGetAddrInfo()
 * The following members may be accessed directly and must be present in the
 * underlying platform dependent structure:
 * <ul>
 * <li>     int              ai_flags;
 * <li>     int              ai_family;
 * <li>     int              ai_socktype;
 * <li>     int              ai_protocol;
 * <li>     size_t           ai_addrlen;
 * <li>     struct sockaddr *ai_addr;
 * <li>     char            *ai_canonname;
 * <li>     struct addrinfo *ai_next;
 * </ul>
 * @see POSIX definition of addrinfo
 */
typedef os_SocketAddrInfo mpe_SocketAddrInfo;

/**
 * Linger structure.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     int l_onoff - 0=off, nonzero=on
 * <li>     int l_linger - linger time in seconds
 * </ul>
 * @see POSIX definition of linger
 */
typedef os_SocketLinger mpe_SocketLinger;

/**
 * Socket address family (MPE_SOCKET_AF_INET4 or MPE_SOCKET_AF_INET6)
 * @see POSIX definition of sa_family_t
 */
typedef os_SocketSaFamily mpe_SocketSaFamily;

/**
 * Protocol independent socket address structure.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     mpe_SocketSaFamily sa_family - address family
 * </ul>
 * @see POSIX definition of sockaddr
 */
typedef os_SocketSockAddr mpe_SocketSockAddr;

/**
 * Platform independent structure definitions used to return a list of network
 * interfaces of the system.  The <i>mpe_SocketNetAddr<i/> structure represents a
 * single network interface address in the linked list of addresses for an interface.
 * The <i>mpe_SocketNetIfList<i/> structure represents a single network interface
 * with its list of associated addresses.
 */
#define MPE_SOCKET_IFNAMSIZ (64)

typedef struct _mpe_SocketNetAddr
{
    int if_family; /* Inet family. */
    mpe_SocketSockAddr *if_addr; /* IPv4 or IPv4 address. */
    struct _mpe_SocketNetAddr *if_next; /* Pointer to next address. */
} mpe_SocketNetAddr;

typedef struct _mpe_SocketNetIfList
{
    char if_name[MPE_SOCKET_IFNAMSIZ]; /* Interface name */
    uint32_t if_index; /* Interface index. */
    mpe_SocketNetAddr *if_addresses; /* List of addresses. */
    struct _mpe_SocketNetIfList *if_next; /* Next interface in list. */
} mpe_SocketNetIfList;

typedef struct _mpeos_LpeDlnaNetworkInterfaceInfo
{
    char* wakeOnPattern;
    char* wakeSupportedTransport;
    int maxWakeOnDelay;
    int dozeDuration;
} mpeos_LpeDlnaNetworkInterfaceInfo;

typedef enum _mpe_SocketNetIfEvent
{
    /**
     * Socket Events start at 500.
     */
    MPE_SOCKET_EVENT_BASE = 500,

    /**
    <pre>
     * This event indicates the platform detected that an IP address
     * was added to an interface
     *
     * optionalEventData1 - N/A
     * optionalEventData2 - IP address that was added
     * optionalEventData3 - N/A
    </pre>
     **/
    MPE_SOCKET_EVT_IP_ADDED = MPE_SOCKET_EVENT_BASE + 1,

    /**
    <pre>
     * This event indicates the platform detected that an IP address
     * was removed from an interface
     *
     * optionalEventData1 - N/A
     * optionalEventData2 - IP address that was removed
     * optionalEventData3 - N/A
    </pre>
     **/
    MPE_SOCKET_EVT_IP_REMOVED = MPE_SOCKET_EVENT_BASE + 2,
}
mpe_SocketNetIfEvent;
 

#define MPE_SOCKET_AF_UNSPEC OS_SOCKET_AF_UNSPEC

/****************************************************************************************
 *
 * IPv4 SPECIFIC DEFINITIONS
 *
 ***************************************************************************************/

/**
 * The address family for IPv4 sockets
 * @see POSIX definition of AF_INET
 */
#define MPE_SOCKET_AF_INET4 OS_SOCKET_AF_INET4

/**
 * The IPv4 protocol
 * @see POSIX definition of IPPROTO_IP
 */
#define MPE_SOCKET_IPPROTO_IPV4 OS_SOCKET_IPPROTO_IPV4

/**
 * The TCP/IP protocol
 * @see POSIX definition of IPPROTO_TCP
 */
#define MPE_SOCKET_IPPROTO_TCP OS_SOCKET_IPPROTO_TCP

/**
 * The UDP/IP protocol
 * @see POSIX definition of IPPROTO_UDP
 */
#define MPE_SOCKET_IPPROTO_UDP OS_SOCKET_IPPROTO_UDP

/**
 * Wildcard IPv4 address (matches any address)
 * @see POSIX definition of INADDR_ANY
 */
#define MPE_SOCKET_IN4ADDR_ANY OS_SOCKET_IN4ADDR_ANY

/**
 * IPv4 loopback address
 * @see POSIX definition of INADDR_LOOPBACK
 */
#define MPE_SOCKET_IN4ADDR_LOOPBACK OS_SOCKET_IN4ADDR_LOOPBACK

/**
 * Maximum length of an IPv4 address string (includes the null terminator)
 * @see POSIX definition of INET_ADDRSTRLEN
 */
#define MPE_SOCKET_INET4_ADDRSTRLEN OS_SOCKET_INET4_ADDRSTRLEN

/**
 * IPv4 address.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     uint32_t s_addr - 32 bit IPv4 address in network byte order
 * </ul>
 * @see POSIX definition of in_addr
 */
typedef os_SocketIPv4Addr mpe_SocketIPv4Addr;

/**
 * IPv4 multicast request structure.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     mpe_SocketIPv4Addr imr_multiaddr - IPv4 class D multicast address
 * <li>     mpe_SocketIPv4Addr imr_interface - IPv4 address of local interface
 * </ul>
 * @see POSIX definition of ip_mreq
 */
typedef os_SocketIPv4McastReq mpe_SocketIPv4McastReq;

/**
 * IPv4 socket address structure.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     mpe_SocketSaFamily sin_family - address family (MPE_SOCKET_AF_INET4)
 * <li>     uint16_t sin_port - 16 bit TCP or UDP port number
 * <li>     mpe_SocketIPv4Addr sin_addr - IPv4 address
 * </ul>
 * @see POSIX definition of sockaddr_in
 */
typedef os_SocketIPv4SockAddr mpe_SocketIPv4SockAddr;

/****************************************************************************************
 *
 * IPv6 SPECIFIC DEFINITIONS
 *
 ***************************************************************************************/

/**
 * The address family for IPv6 sockets.
 * @see POSIX definition of AF_INET6
 */
#define MPE_SOCKET_AF_INET6 OS_SOCKET_AF_INET6

/**
 * The IPv6 protocol
 * @see POSIX definition of IPPROTO_IPV6
 */
#define MPE_SOCKET_IPPROTO_IPV6 OS_SOCKET_IPPROTO_IPV6

/**
 * Wildcard IPv6 address (matches any address)
 * @see POSIX definition of IN6ADDR_ANY_INIT
 */
#define MPE_SOCKET_IN6ADDR_ANY_INIT OS_SOCKET_IN6ADDR_ANY_INIT

/**
 * IPv6 loopback address
 * @see POSIX definition of IN6ADDR_LOOPBACK_INIT
 */
#define MPE_SOCKET_IN6ADDR_LOOPBACK_INIT OS_SOCKET_IN6ADDR_LOOPBACK_INIT

/**
 * Maximum length of an IPv6 address string
 * @see POSIX definition of IN6ADDR_ADDRSTRLEN
 */
#define MPE_SOCKET_INET6_ADDRSTRLEN OS_SOCKET_INET6_ADDRSTRLEN

/**
 * IPv6 address.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     uint8_t s6_addr[16] - 128 bit IPv6 address in network byte order
 * </ul>
 * @see POSIX definition of in6_addr
 */
typedef os_SocketIPv6Addr mpe_SocketIPv6Addr;

/**
 * IPv6 multicast request structure.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     mpe_SocketIPv6Addr ipv6mr_multiaddr - IPv6 multicast address
 * <li>     mpe_SocketIPv6Addr ipv6mr_interface - Interface index, or 0
 * </ul>
 * @see POSIX definition of ip6_mreq
 */
typedef os_SocketIPv6McastReq mpe_SocketIPv6McastReq;

/**
 * IPv6 socket address structure.
 * The following members may be accessed directly and must be present in the underlying
 * platform dependent structure:
 * <ul>
 * <li>     mpe_SocketSaFamily sin6_family - address family (MPE_SOCKET_AF_INET6)
 * <li>     uint16_t sin6_port - transport layer port number in network byte order
 * <li>     uint32_t sin6_flowinfo - priority & flow label in network byte order
 * <li>     mpe_SocketIPv6Addr sin6_addr - IPv6 address
 * </ul>
 * @see POSIX definition of sockaddr_in6
 */
typedef os_SocketIPv6SockAddr mpe_SocketIPv6SockAddr;


/**
 * This data type enumerates the allowable DLNA QOS DSCP values set by the  
 * mpe_socketSetQOS API. Refer to DLNA Guidelines Part 1 for DLNA QOS 
 * recommendations.
 */
typedef enum
{
    MPE_SOCKET_DLNA_QOS_0 = 0x08,
    MPE_SOCKET_DLNA_QOS_1 = 0x00,
    MPE_SOCKET_DLNA_QOS_2 = 0x28,
    MPE_SOCKET_DLNA_QOS_3 = 0x38
} mpe_SocketDLNAQOS;

/****************************************************************************************
 *
 * SOCKET LEVEL OPTIONS
 *
 ***************************************************************************************/

/**
 * Used with mpeos_socketSetOpt() and mpeos_socketGetOpt() to specify a socket level
 * option.
 * @see POSIX definition of SOL_SOCKET
 */
#define MPE_SOCKET_SOL_SOCKET OS_SOCKET_SOL_SOCKET

/**
 * Controls whether transmission of broadcast messages is supported, if this is supported
 * by the protocol. This boolean option shall store an int value.
 * @see POSIX definition of SO_BROADCAST
 */
#define MPE_SOCKET_SO_BROADCAST OS_SOCKET_SO_BROADCAST

/**
 * Controls whether debugging information is being recorded. This boolean option shall store
 * an int value.
 * @see POSIX definition of SO_DEBUG
 */
#define MPE_SOCKET_SO_DEBUG OS_SOCKET_SO_DEBUG

/**
 * Controls whether outgoing messages bypass the standard routing facilities. The
 * destination shall be on a directly-connected network, and messages are directed
 * to the appropriate network interface according to the destination address. The
 * effect, if any, of this option depends on what protocol is in use. This boolean
 * option shall store an int value.
 * @see POSIX definition of SO_DONTROUTE
 */
#define MPE_SOCKET_SO_DONTROUTE OS_SOCKET_SO_DONTROUTE

/**
 * Reports information about error status and clears it when used with mpeos_socketGetOpt().
 * This option cannot be set. This option shall store an int value.
 * @see POSIX definition of SO_ERROR
 */
#define MPE_SOCKET_SO_ERROR OS_SOCKET_SO_ERROR

/**
 * Controls whether connections are kept active with periodic transmission of messages, if
 * this is supported by the protocol. If the connected socket fails to respond to these
 * messages, the connection shall be broken and threads writing to that socket shall be
 * notified with a MPE_SIGPIPE signal. This boolean option shall store an int value.
 * @see POSIX definition of SO_KEEPALIVE
 */
#define MPE_SOCKET_SO_KEEPALIVE OS_SOCKET_SO_KEEPALIVE

/**
 * Controls whether the socket lingers on mpeos_socketClose() if data is present. If this
 * option is set, the system blocks the process during mpeos_socketClose() until it can
 * transmit the data or until the end of the interval indicated by mpe_SocketLinger, whichever
 * comes first. If this option is not specified, and mpeos_socketClose() is issued, the
 * system handles the call in a way that allows the process to continue as quickly as
 * possible. This option shall store a mpe_SocketLinger structure.
 * @see POSIX definition of SO_LINGER
 */
#define MPE_SOCKET_SO_LINGER OS_SOCKET_SO_LINGER

/**
 * Controls whether the socket leaves received out-of-band data (data marked urgent)
 * inline. This boolean option shall store an int value.
 * @see POSIX definition of SO_OOBINLINE
 */
#define MPE_SOCKET_SO_OOBINLINE OS_SOCKET_SO_OOBINLINE

/**
 * Controls the receive buffer size. This option shall store an int value.
 * @see POSIX definition of SO_RCVBUF
 */
#define MPE_SOCKET_SO_RCVBUF OS_SOCKET_SO_RCVBUF

/**
 * Controls the minimum number of bytes to process for socket input operations. The default
 * value for this option is 1. If this option is set to a larger value, blocking receive
 * calls normally wait until they have received the smaller of the low water mark value or
 * the requested amount. (They may return less than the low water mark if an error occurs,
 * a signal is caught, or the type of data next in the receive queue is different from that
 * returned; for example, out-of-band data.) This option shall store an int value.
 * @see POSIX definition of SO_RCVLOWAT
 */
#define MPE_SOCKET_SO_RCVLOWAT OS_SOCKET_SO_RCVLOWAT

/**
 * Controls the timeout value for input operations. This option shall store a mpe_TimeVal
 * structure with the number of seconds and microseconds specifying the limit on how long
 * to wait for an input operation to complete. If a receive operation has blocked for this
 * much time without receiving additional data, it shall return with a partial count or
 * an error indication if no data was received. In the latter case the error code can be
 * retrieved with mpeos_socketGetLastError(). The default for this option is zero, which
 * indicates that a receive operation shall not time out. The option shall store a
 * mpe_TimeVal structure.
 * @see POSIX definition of SO_RCVTIMEO
 */
#define MPE_SOCKET_SO_RCVTIMEO OS_SOCKET_SO_RCVTIMEO

/**
 * Controls whether the rules used in validating addresses supplied to mpeos_socketBind()
 * should allow reuse of local addresses, if this is supported by the protocol. This boolean
 * option shall store an int value.
 * @see POSIX definition of SO_REUSEADDR
 */
#define MPE_SOCKET_SO_REUSEADDR OS_SOCKET_SO_REUSEADDR

/**
 * Controls the send buffer size. This option shall store an int value.
 * @see POSIX definition of SO_SNDBUF
 */
#define MPE_SOCKET_SO_SNDBUF OS_SOCKET_SO_SNDBUF

/**
 * Controls the minimum number of bytes to process for socket output operations. Non-blocking
 * output operations shall process no data if flow control does not allow the smaller of
 * the send low water mark value or the entire request to be processed. This option shall
 * store an int value.
 * @see POSIX definition of SO_SNDLOWAT
 */
#define MPE_SOCKET_SO_SNDLOWAT OS_SOCKET_SO_SNDLOWAT

/**
 * Controls the timeout value specifying the amount of time that an output function blocks
 * because flow control prevents data from being sent. If a send operation has blocked for
 * this time, it shall return with a partial count or an error indication if no data was
 * sent. In the latter case the error code can be retrieved with mpeos_socketGetLastError().
 * The default for this option is zero, which indicates that a send operation shall not time
 * out. The option shall store a mpe_TimeVal structure.
 * @see POSIX definition of SO_SNDTIMEO
 */
#define MPE_SOCKET_SO_SNDTIMEO OS_SOCKET_SO_SNDTIMEO

/**
 * Reports the socket type when used with mpeos_socketGetOpt(). This option cannot be set.
 * This option shall store an int value.
 * @see POSIX definition of SO_TYPE
 */
#define MPE_SOCKET_SO_TYPE OS_SOCKET_SO_TYPE

/****************************************************************************************
 *
 * IPv4 LEVEL OPTIONS
 *
 ***************************************************************************************/

/**
 * Join a multicast group on a specified local interface. Argument is a mpe_SocketIPv4McastReq
 * structure. <i>imr_multiaddr</i> contains the address of the multicast group the
 * caller wants to join or leave. It must be a valid multicast address.
 * <i>imr_interface</i> is the address of the local interface with which the system
 * should join the multicast group; if it is equal to MPE_SOCKET_IN4ADDR_ANY an appropriate
 * interface is chosen by the system.
 * <p>
 * More than one join is allowed on a given socket but each join must be for a different
 * multicast address, or for the same multicast address but on a different interface from
 * previous joins for that address on this socket. This can be used on a multihomed host
 * where, for example, one socket is created and then for each interface a join is
 * performed for a given multicast address.
 * @see POSIX definition of IP_ADD_MEMBERSHIP
 */
#define MPE_SOCKET_IPV4_ADD_MEMBERSHIP OS_SOCKET_IPV4_ADD_MEMBERSHIP

/**
 * Leave a multicast group. Argument is a mpe_SocketIPv4McastReq structure similar to
 * MPE_SOCKET_IPV4_ADD_MEMBERSHIP. If the local interface is not specified (that is, the value is
 * INADDR_ANY), the first matching multicasting group membership is dropped.
 * <p>
 * If a process joins a group but never explicitly leaves the group, when the socket is
 * closed (either explicitly or on process termination), the membership is dropped
 * automatically. It is possible for multiple processes on a host to each join the same
 * group, in which case the host remains a member of that group until the last process
 * leaves the group.
 * @see POSIX definition of IP_DROP_MEMBERSHIP
 */
#define MPE_SOCKET_IPV4_DROP_MEMBERSHIP OS_SOCKET_IPV4_DROP_MEMBERSHIP

/**
 * Specify the interface for outgoing multicast datagrams sent on this socket. This
 * interface is specified as a mpe_SocketIPv4Addr structure. If the value specified is
 * INADDR_ANY, this removes any interface previously assigned by this socket option,
 * and the system will choose the interface each time a datagram is sent.
 * <p>
 * Be careful to distinguish between the local interface specified (or chosen) when a
 * process joins a group (the interface on which arriving multicast datagrams will be
 * received), and the local interface specified (or chosen) when a multicast datagram is
 * output.
 * @see POSIX definition of IP_MULTICAST_IF
 */
#define MPE_SOCKET_IPV4_MULTICAST_IF OS_SOCKET_IPV4_MULTICAST_IF

/**
 * Enable or disable local loopback of multicast datagrams. By default loopback is enabled:
 * a copy of each multicast datagram sent by a process on the host will also be looped back
 * and processed as a received datagram by that host, if the host belongs to that multicast
 * group on the outgoing interface. This option shall store an unsigned char value.
 * @see POSIX definition of IP_MULTICAST_LOOP
 */
#define MPE_SOCKET_IPV4_MULTICAST_LOOP OS_SOCKET_IPV4_MULTICAST_LOOP

/**
 * Sets or reads the time-to-live value of outgoing multicast datagrams for this socket. It
 * is very important for multicast packets to set the smallest TTL possible. The default
 * is 1 which means that multicast packets don't leave the local network unless the
 * user program explicitly requests it. This option shall store an unsigned char value.
 * @see POSIX definition of IP_MULTICAST_TTL
 */
#define MPE_SOCKET_IPV4_MULTICAST_TTL OS_SOCKET_IPV4_MULTICAST_TTL

/****************************************************************************************
 *
 * IPv6 LEVEL OPTIONS
 *
 ***************************************************************************************/


/**
 * Join a multicast group on a specified local interface. Argument is a mpe_SocketIPv6McastReq
 * structure. <i>ipv6mr_multiaddr</i> contains the address of the multicast group the
 * caller wants to join or leave. It must be a valid multicast address.
 * <i>ipv6mr_interface</i> is the address of the local interface with which the system
 * should join the multicast group; if it is equal to MPE_SOCKET_IN6ADDR_ANY_INIT an appropriate
 * interface is chosen by the system.
 * <p>
 * More than one join is allowed on a given socket but each join must be for a different
 * multicast address, or for the same multicast address but on a different interface from
 * previous joins for that address on this socket. This can be used on a multihomed host
 * where, for example, one socket is created and then for each interface a join is
 * performed for a given multicast address.
 * @see POSIX definition of IPV6_ADD_MEMBERSHIP
 */
#define MPE_SOCKET_IPV6_ADD_MEMBERSHIP OS_SOCKET_IPV6_ADD_MEMBERSHIP

/**
 * Leave a multicast group. Argument is a mpe_SocketIPv6McastReq structure similar to
 * MPE_SOCKET_IPV6_ADD_MEMBERSHIP. If the local interface is not specified (that is, the value is
 * IN6ADDR_ANY_INIT), the first matching multicasting group membership is dropped.
 * <p>
 * If a process joins a group but never explicitly leaves the group, when the socket is
 * closed (either explicitly or on process termination), the membership is dropped
 * automatically. It is possible for multiple processes on a host to each join the same
 * group, in which case the host remains a member of that group until the last process
 * leaves the group.
 * @see POSIX definition of IPV6_DROP_MEMBERSHIP
 */
#define MPE_SOCKET_IPV6_DROP_MEMBERSHIP OS_SOCKET_IPV6_DROP_MEMBERSHIP

/**
 * Specify the interface for outgoing multicast datagrams sent on this socket. This
 * interface is specified as a mpe_SocketIPv6Addr structure. If the value specified is
 * IN6ADDR_ANY_INIT, this removes any interface previously assigned by this socket option,
 * and the system will choose the interface each time a datagram is sent.
 * <p>
 * Be careful to distinguish between the local interface specified (or chosen) when a
 * process joins a group (the interface on which arriving multicast datagrams will be
 * received), and the local interface specified (or chosen) when a multicast datagram is
 * output.
 * @see POSIX definition of IPV6_MULTICAST_IF
 */
#define MPE_SOCKET_IPV6_MULTICAST_IF OS_SOCKET_IPV6_MULTICAST_IF

/**
 * Sets or reads the hop limit of outgoing multicast datagrams for this socket. It
 * is very important for multicast packets to set the smallest hop limit possible. The default
 * is 1 which means that multicast packets don't leave the local network unless the
 * user program explicitly requests it. This option shall store an unsigned int value.
 * @see POSIX definition of IPV6_MULTICAST_HOPS
 */
#define MPE_SOCKET_IPV6_MULTICAST_HOPS OS_SOCKET_IPV6_MULTICAST_HOPS

/**
 * Enable or disable local loopback of multicast datagrams. By default loopback is enabled:
 * a copy of each multicast datagram sent by a process on the host will also be looped back
 * and processed as a received datagram by that host, if the host belongs to that multicast
 * group on the outgoing interface. This option shall store an unsigned int value.
 * @see POSIX definition of IPV6_MULTICAST_LOOP
 */
#define MPE_SOCKET_IPV6_MULTICAST_LOOP OS_SOCKET_IPV6_MULTICAST_LOOP


/****************************************************************************************
 *
 * TCP LEVEL OPTIONS
 *
 ***************************************************************************************/

/**
 * If set, this option disables TCP's <i>Nagle algorithm</i>. By default this algorithm is
 * enabled. This option shall store an int value.
 * @see POSIX definition of TCP_NODELAY
 */
#define MPE_SOCKET_TCP_NODELAY OS_SOCKET_TCP_NODELAY

/****************************************************************************************
 *
 * SOCKET ERROR CODES (See function headers below for function-specific definitions of
 * these error codes)
 *
 * The following error codes are used by functions below but are defined in mpe_error.h
 * so they do not need to be mapped here.
 *
 * MPE_EINVAL
 * MPE_ENODATA
 * MPE_ENOMEM
 * MPE_ETHREADDEATH
 *
 ***************************************************************************************/

#define MPE_SOCKET_EACCES OS_MPE_SOCKET_EACCES
#define MPE_SOCKET_EADDRINUSE OS_MPE_SOCKET_EADDRINUSE
#define MPE_SOCKET_EADDRNOTAVAIL OS_MPE_SOCKET_EADDRNOTAVAIL
#define MPE_SOCKET_EAFNOSUPPORT OS_MPE_SOCKET_EAFNOSUPPORT
#define MPE_SOCKET_EAGAIN OS_MPE_SOCKET_EAGAIN
#define MPE_SOCKET_EALREADY OS_MPE_SOCKET_EALREADY
#define MPE_SOCKET_EBADF OS_MPE_SOCKET_EBADF
#define MPE_SOCKET_ECONNABORTED OS_MPE_SOCKET_ECONNABORTED
#define MPE_SOCKET_ECONNREFUSED OS_MPE_SOCKET_ECONNREFUSED
#define MPE_SOCKET_ECONNRESET OS_MPE_SOCKET_ECONNRESET
#define MPE_SOCKET_EDESTADDRREQ OS_MPE_SOCKET_EDESTADDRREQ
#define MPE_SOCKET_EDOM OS_MPE_SOCKET_EDOM
#define MPE_SOCKET_EHOSTNOTFOUND OS_MPE_SOCKET_EHOSTNOTFOUND
#define MPE_SOCKET_EHOSTUNREACH OS_MPE_SOCKET_EHOSTUNREACH
#define MPE_SOCKET_EINTR OS_MPE_SOCKET_EINTR
#define MPE_SOCKET_EIO OS_MPE_SOCKET_EIO
#define MPE_SOCKET_EISCONN OS_MPE_SOCKET_EISCONN
#define MPE_SOCKET_EINPROGRESS OS_MPE_SOCKET_EINPROGRESS
#define MPE_SOCKET_ELOOP OS_MPE_SOCKET_ELOOP
#define MPE_SOCKET_EMFILE OS_MPE_SOCKET_EMFILE
#define MPE_SOCKET_EMSGSIZE OS_MPE_SOCKET_EMSGSIZE
#define MPE_SOCKET_ENAMETOOLONG OS_MPE_SOCKET_ENAMETOOLONG
#define MPE_SOCKET_ENFILE OS_MPE_SOCKET_ENFILE
#define MPE_SOCKET_ENETDOWN OS_MPE_SOCKET_ENETDOWN
#define MPE_SOCKET_ENETUNREACH OS_MPE_SOCKET_ENETUNREACH
#define MPE_SOCKET_ENOBUFS OS_MPE_SOCKET_ENOBUFS
#define MPE_SOCKET_ENOPROTOOPT OS_MPE_SOCKET_ENOPROTOOPT
#define MPE_SOCKET_ENORECOVERY OS_MPE_SOCKET_ENORECOVERY
#define MPE_SOCKET_ENOSPC OS_MPE_SOCKET_ENOSPC
#define MPE_SOCKET_ENOTCONN OS_MPE_SOCKET_ENOTCONN
#define MPE_SOCKET_ENOTSOCK OS_MPE_SOCKET_ENOTSOCK
#define MPE_SOCKET_EOPNOTSUPP OS_MPE_SOCKET_EOPNOTSUPP
#define MPE_SOCKET_EPIPE OS_MPE_SOCKET_EPIPE
#define MPE_SOCKET_EPROTO OS_MPE_SOCKET_EPROTO
#define MPE_SOCKET_EPROTONOSUPPORT OS_MPE_SOCKET_EPROTONOSUPPORT
#define MPE_SOCKET_EPROTOTYPE OS_MPE_SOCKET_EPROTOTYPE
#define MPE_SOCKET_ETIMEDOUT OS_MPE_SOCKET_ETIMEDOUT
#define MPE_SOCKET_ETRYAGAIN OS_MPE_SOCKET_ETRYAGAIN
#define MPE_SOCKET_EWOULDBLOCK OS_MPE_SOCKET_EWOULDBLOCK

/****************************************************************************************
 *
 * Minimum/Maximum values used in mpeos_hnPing/os_Ping/mpeos_hnTraceroute/os_Traceroute
 * and mpeos_hnNSLookup/os_NSLookup 
 *
 ***************************************************************************************/
/**
 * Additional Info string size
 * See mpeos_hnPing() for more details.
 **/
#define    MPE_SOCKET_ADDITIONAL_INFO_SIZE    1024
/**
 * Status string size
 * See mpeos_hnPing() for more details.
 **/
#define    MPE_SOCKET_STATUS_SIZE    32
/**
 * Maximum Ping result array size
 * See mpeos_hnPing() for more details.
 **/
#define    MPE_SOCKET_MAX_PING_RESULT_SIZE    7

/**
 * Minimum/Maximum Ping repetition count
 * See mpeos_hnPing() for more details.
 **/
#define    MPE_SOCKET_PING_MIN_COUNT    1
#define    MPE_SOCKET_PING_MAX_COUNT    100

/**
 * Minimum/Maximum request timeout(msecs)
 * See mpeos_hnPing() for more details.
 **/
#define    MPE_SOCKET_MIN_TIMEOUT    1000
#define    MPE_SOCKET_MAX_TIMEOUT    30000

// Default for Ping since not provided by user
#define    MPE_SOCKET_PING_DEFAULT_TIMEOUT  10000 

/**
 * Minimum/Maximum Ping request interval(msecs)
 * See mpeos_hnPing() for more details.
 **/

#define    MPE_SOCKET_PING_MIN_INTERVAL    1000
#define    MPE_SOCKET_PING_MAX_INTERVAL    30000

/**
 * Minimum/Maximum data block size(bytes) 
 * See mpeos_hnPing() for more details.
 **/

#define    MPE_SOCKET_MIN_BLOCK_SIZE    20 
#define    MPE_SOCKET_MAX_BLOCK_SIZE    2048
 
/**
 * Minimum/Maximum Differential Service Value 
 * See mpeos_hnPing(),mpe_hnTraceroute for more details.
 **/

#define    MPE_SOCKET_MIN_DSCP    0 
#define    MPE_SOCKET_MAX_DSCP    64
 
/**
 * Maximum Traceroute result size
 * See mpeos_hnTraceroute() for more details.
 **/
#define    MPE_SOCKET_TRACEROUTE_RESULT_SIZE    4
#define    MPE_SOCKET_MAX_TRACEROUTE_HOSTS    2048

/**
 * Minimum/Maximum Traceroute hops 
 * See mpeos_hnTraceroute() for more details.
 **/

#define    MPE_SOCKET_TRACEROUTE_MIN_HOPS    1 
#define    MPE_SOCKET_TRACEROUTE_MAX_HOPS    64

/**
 * Maximum NSLookup results size
 * See mpeos_hnNSLookup() for more details.
 **/
#define    MPE_SOCKET_MAX_NSLOOKUP_ANSWER_RESULT_SIZE   32 
#define    MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE    256 
#define    MPE_SOCKET_MAX_NSLOOKUP_IPS_RESULT_SIZE    1024 
#define    MPE_SOCKET_MAX_NSLOOKUP_SERVER_RESULT_SIZE    128 
#define    MPE_SOCKET_MAX_NSLOOKUP_RESULT_ARRAY_SIZE    7 


/****************************************************************************************
 *
 * SOCKET FUNCTIONS
 *
 ***************************************************************************************/

/**
 * Initialize the socket API. This function must be called before any other function
 * with a prefix of <i>mpeos_socket</i>.
 *
 * @return Upon successful completion, this function shall return TRUE. Otherwise, FALSE
 *          is returned to indicate that the socket subsystem could not be initialized.
 */
mpe_Bool mpeos_socketInit(void);

/**
 * Terminate the socket API. This function should be called when use of the socket API
 * (functions beginning with <i>mpeos_socket</i>) is no longer required.
 */
void mpeos_socketTerm(void);

/**
 * Return the error status of the last socket function that failed.
 *
 * @return Error status of last socket function that failed
 */
int mpeos_socketGetLastError(void);

/**
 * The mpeos_socketAccept() function shall
 * extract the first connection on the queue of pending connections, create a new socket
 * with the same socket type protocol and address family as the specified <i>socket</i>, and
 * allocate a new file descriptor for that socket.
 * <p>
 * If <i>address</i> is not a null pointer, the address of the peer for the accepted
 * connection shall be stored in the mpe_SocketSockAddr structure pointed to by
 * <i>address</i>, and the length of this address shall be stored in the object pointed
 * to by <i>address_len</i>.
 * <p>
 * If the actual length of the address is greater than the length of the supplied
 * mpe_SocketSockAddr structure, the stored address shall be truncated.
 * <p>
 * If the protocol permits connections by unbound clients, and the peer is not bound, then
 * the value stored in the object pointed to by <i>address</i> is unspecified.
 * <p>
 * If the listen queue is empty of connection requests this function shall block until a
 * connection is present.
 * <p>
 * The accepted socket cannot itself accept more connections. The original socket remains
 * open and can accept more connections.
 *
 * @param socket specifies a socket that was created with mpeos_socketCreate(), has
 *          been bound to an address with mpeos_socketBind(), and has issued a
 *          successful call to mpeos_socketListen().
 * @param address either a null pointer, or a pointer to a mpe_SocketSockAddr structure
 *          where the address of the connecting socket shall be returned.
 * @param address_len points to a length parameter which on input specifies the length of the
 *          supplied mpe_SocketSockAddr structure, and on output specifies the length
 *          of the stored address.
 * @return Upon successful completion, this function shall return the file descriptor of the
 *          accepted socket. Otherwise, MPE_SOCKET_INVALID_SOCKET shall be returned and one
 *          of the following error codes can be retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_ECONNABORTED - A connection has been aborted.
 * <li>     MPE_SOCKET_EINTR - This function was interrupted by a signal that was caught before a valid
 *          connection arrived.
 * <li>     MPE_EINVAL - The socket is not accepting connections.
 * <li>     MPE_SOCKET_EMFILE - No more file descriptors are available for this process.
 * <li>     MPE_SOCKET_ENFILE - No more file descriptors are available for the system.
 * <li>     MPE_SOCKET_ENOBUFS - No buffer space is available.
 * <li>     MPE_ENOMEM - There was insufficient memory available to complete the operation.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The socket type of the specified socket does not support accepting
 *          connections.
 * <li>     MPE_SOCKET_EPROTO - A protocol error has occurred.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * </ul>
 * @see POSIX function accept()
 */
mpe_Socket mpeos_socketAccept(mpe_Socket socket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len);

/**
 * The mpeos_socketBind() function shall assign a local socket address to a
 * socket identified by descriptor <i>socket</i> that has no local socket address assigned.
 * Sockets created with the mpe_socketCreate() function are initially unnamed; they are
 * identified only by their address family.
 *
 * @param socket specifies the file descriptor of the socket to be bound.
 * @param address points to a mpe_SocketSockAddr structure containing the address to be
 *          bound to the socket. The length and format of the address depend on the address
 *          family of the socket.
 * @param address_len specifies the length of the mpe_SocketSockAddr structure pointed
 *          to by the <i>address</i> argument.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall be
 *          returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - The specified <i>address</i> is protected and the current user does
 *          not have permission to bind to it.
 * <li>     MPE_SOCKET_EADDRINUSE - The specified <i>address</i> is already in use.
 * <li>     MPE_SOCKET_EADDRNOTAVAIL - The specified <i>address</i> is not available from the local
 *          machine.
 * <li>     MPE_SOCKET_EAFNOSUPPORT - The specified <i>address</i> is not a valid address for the address
 *          family of the specified socket.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_EINVAL - The socket is already bound to an address, and the protocol does not
 *          support binding to a new address; or the socket has been shut down.
 * <li>     MPE_EINVAL - The <i>address_len</i> argument is not a valid length for the address
 *          family.
 * <li>     MPE_SOCKET_EISCONN - The socket is already connected.
 * <li>     MPE_SOCKET_ELOOP - More than the maximum number of symbolic links were encountered during
 *          resolution of the pathname in <i>address</i>.
 * <li>     MPE_SOCKET_ENAMETOOLONG - Pathname resolution of a symbolic link produced an intermediate
 *          result whose length exceeds the maximum allowed for a pathname.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available to complete the call.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The socket type of the specified socket does not support binding
 *          to an address.
 * </ul>
 * @see POSIX function bind()
 */
int mpeos_socketBind(mpe_Socket socket, const mpe_SocketSockAddr *address,
        mpe_SocketSockLen address_len);

/**
 * The mpeos_socketClose() function shall deallocate the file descriptor indicated by
 * <i>socket</i>. To deallocate means to make the file descriptor available for return by
 * subsequent calls to mpeos_socketCreate() or other functions that allocate file
 * descriptors.
 * <p>
 * If this function is interrupted by a signal that is to be caught, it shall return -1
 * and the state of <i>socket</i> is unspecified. In this case mpeos_socketGetLastError()
 * returns MPE_SOCKET_EINTR.
 * <p>
 * If an I/O error occurred while reading from or writing to the socket during
 * mpeos_socketClose(), it may return -1 and the state of <i>socket</i> is unspecified.
 * In this case mpeos_socketGetLastError() returns MPE_SOCKET_EIO.
 * <p>
 * If the socket is in connection-mode, and the MPE_SOCKET_SO_LINGER option is set for the
 * socket with non-zero linger time, and the socket has untransmitted data, then this
 * function shall block for up to the current linger interval until all data is transmitted.
 *
 * @param socket specifies the file descriptor associated with the socket to be closed.
 * @return Upon successful completion, 0 shall be returned; otherwise, -1 shall be returned
 *          and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_EINTR - This function was interrupted by a signal.
 * <li>     MPE_SOCKET_EIO - An I/O error occurred while reading from or writing to the socket.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * </ul>
 * @see POSIX function close()
 */
int mpeos_socketClose(mpe_Socket socket);

/**
 * The mpeos_socketConnect() function shall attempt to make a connection on a socket.
 * If the socket has not already been bound to a local address, this function shall bind it
 * to an unused local address.
 * <p>
 * If the initiating socket is not connection-mode, then this function shall set the socket's
 * peer address, and no connection is made. For MPE_SOCKET_DATAGRAM sockets, the peer
 * address identifies where all datagrams are sent on subsequent mpeos_socketSend()
 * functions, and limits the remote sender for subsequent mpeos_socketRecv() functions. If
 * <i>address</i> is a null address for the protocol, the socket's peer address shall be reset.
 * <p>
 * If the initiating socket is connection-mode, then this function shall attempt to establish
 * a connection to the address specified by the <i>address</i> argument. If the connection
 * cannot be established immediately this function shall block for up to an unspecified
 * timeout interval until the connection is established. If the timeout interval expires
 * before the connection is established, this function shall fail and the connection attempt
 * shall be aborted. If this function is interrupted by a signal that is caught while blocked
 * waiting to establish a connection, this function shall fail,
 * but the connection request shall not be aborted, and the connection shall be established
 * asynchronously. In this case mpeos_socketGetLastError() returns MPE_SOCKET_EINTR.
 * <p>
 * When the connection has been established asynchronously, <i>mpeos_socketSelect()</i> shall
 * indicate that the file descriptor for the socket is ready for writing.
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param address points to a mpe_SocketSockAddr structure containing the peer address.
 *          The length and format of the address depend on the address family of the socket.
 * @param address_len specifies the length of the mpe_SocketSockAddr structure pointed
 *          to by the <i>address</i> argument.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - Search permission is denied for a component of the path prefix;
 *          or write access to the named socket is denied.
 * <li>     MPE_SOCKET_EADDRINUSE - Attempt to establish a connection that uses addresses that are
 *          already in use.
 * <li>     MPE_SOCKET_EADDRNOTAVAIL - The specified <i>address</i> is not available from the local
 *          machine.
 * <li>     MPE_SOCKET_EAFNOSUPPORT - The specified <i>address</i> is not a valid address for the
 *          address family of the specified socket.
 * <li>     MPE_SOCKET_EALREADY - A connection request is already in progress for the specified
 *          socket.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_ECONNREFUSED - The target address was not listening for connections or
 *          refused the connection request.
 * <li>     MPE_SOCKET_ECONNRESET - Remote host reset the connection request.
 * <li>     MPE_SOCKET_EHOSTUNREACH - The destination host cannot be reached (probably because the
 *          host is down or a remote router cannot reach it).
 * <li>     MPE_SOCKET_EINTR - The attempt to establish a connection was interrupted by delivery of
 *          a signal that was caught; the connection shall be established asynchronously.
 * <li>     MPE_EINVAL - The <i>address_len</i> argument is not a valid length for the address
 *          family; or invalid address family in the mpe_SocketSockAddr structure.
 * <li>     MPE_SOCKET_EISCONN - The specified socket is connection-mode and is already connected.
 * <li>     MPE_SOCKET_ELOOP - More than the maximum number of symbolic links were encountered
 *          during resolution of the pathname in <i>address</i>.
 * <li>     MPE_SOCKET_ENAMETOOLONG - Pathname resolution of a symbolic link produced an intermediate
 *          result whose length exceeds the maximum allowed for a pathname.
 * <li>     MPE_SOCKET_ENETDOWN - The local network interface used to reach the destination is down.
 * <li>     MPE_SOCKET_ENETUNREACH - No route to the network is present.
 * <li>     MPE_SOCKET_ENOBUFS - No buffer space is available.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The socket is listening and cannot be connected.
 * <li>     MPE_SOCKET_EPROTOTYPE - The specified <i>address</i> has a different type than the
 *          socket bound to the specified peer address.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * <li>     MPE_SOCKET_ETIMEDOUT - The attempt to connect timed out before a connection was made.
 * </ul>
 * @see POSIX function connect()
 */
int mpeos_socketConnect(mpe_Socket socket, const mpe_SocketSockAddr *address,
        mpe_SocketSockLen address_len);

/**
 * The mpeos_socketCreate() function shall create an unbound socket in a communications
 * domain, and return a file descriptor that can be used in later function calls that
 * operate on sockets.
 * <p>
 * The <i>domain</i> argument specifies the address family used in the communications domain.
 * The only address families currently supported are MPE_SOCKET_AF_INET4 and MPE_SOCKET_AF_INET6.
 * <p>
 * The <i>type</i> argument specifies the socket type, which determines the semantics of
 * communication over the socket. The following socket types are currently supported:
 * <ul>
 * <li> MPE_SOCKET_STREAM - provides sequenced, reliable, bidirectional, connection-mode
 *      byte streams.
 * <li> MPE_SOCKET_DATAGRAM - provides datagrams, which are connectionless-mode, unreliable
 *      messages of fixed maximum length.
 * </ul>
 * <p>
 * If the <i>protocol</i> argument is non-zero, it shall specify a protocol that is supported
 * by the address family. If the <i>protocol</i> argument is zero, the default protocol for this
 * address family and type shall be used.
 *
 * @param domain specifies the communications domain in which a socket is to be created.
 * @param type specifies the type of socket to be created.
 * @param protocol specifies a particular protocol to be used with the socket. Specifying
 *          a protocol of 0 causes this function to use an unspecified default protocol
 *          appropriate for the requested socket type.
 * @return Upon successful completion, this function shall return a valid socket descriptor.
 *          Otherwise, a value of MPE_SOCKET_INVALID_SOCKET shall be returned and
 *          one of the following error codes can be retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - The process does not have appropriate privileges.
 * <li>     MPE_SOCKET_EAFNOSUPPORT - The implementation does not support the specified address
 *          family.
 * <li>     MPE_SOCKET_EMFILE - No more file descriptors are available for this process.
 * <li>     MPE_SOCKET_ENFILE - No more file descriptors are available for the system.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to perform
 *          the operation.
 * <li>     MPE_ENOMEM - Insufficient memory was available to fulfill the request.
 * <li>     MPE_SOCKET_EPROTONOSUPPORT - The <i>protocol</i> is not supported by the address
 *          family, or the <i>protocol</i> is not supported by the implementation.
 * <li>     MPE_SOCKET_EPROTOTYPE - The socket <i>type</i> is not supported by the <i>protocol</i>.
 * </ul>
 * @see POSIX function socket()
 */
mpe_Socket mpeos_socketCreate(int domain, int type, int protocol);

/**
 * The mpeos_socketFDClear() function shall
 * remove the file descriptor <i>fd</i> from the set pointed to by <i>fdset</i>.
 * If <i>fd</i> is not a member of this set, there shall be no effect on the set,
 * nor will an error be returned.
 * The behavior of this function is undefined if the <i>fd</i> argument is less than 0
 * or greater than or equal to MPE_SOCKET_FD_SETSIZE, or if <i>fd</i> is not a valid file
 * descriptor.
 *
 * @param fd file descriptor to be removed from <i>fdset</i>
 * @param fdset file descriptor set to be modified
 * @see POSIX macro FD_CLR()
 */
void mpeos_socketFDClear(mpe_Socket fd, mpe_SocketFDSet *fdset);

/**
 * The mpeos_socketFDIsSet() function shall
 * determine whether the file descriptor <i>fd</i> is a member of the set pointed to by
 * <i>fdset</i>.
 * The behavior of this function is undefined if the <i>fd</i> argument is less than 0
 * or greater than or equal to MPE_SOCKET_FD_SETSIZE, or if <i>fd</i> is not a valid file
 * descriptor.
 *
 * @param fd file descriptor within fdset to be checked
 * @param fdset file descriptor set to be checked
 * @return A non-zero value if the bit for the file descriptor <i>fd</i> is set in
 *          the file descriptor set pointed to by <i>fdset</i>, and 0 otherwise.
 * @see POSIX macro FD_ISSET()
 */
int mpeos_socketFDIsSet(mpe_Socket fd, mpe_SocketFDSet *fdset);

/**
 * The mpeos_socketFDSet() function shall
 * add the file descriptor <i>fd</i> to the set pointed to by <i>fdset</i>. If the file
 * descriptor <i>fd</i> is already in this set, there shall be no effect on the set, nor
 * will an error be returned.
 * The behavior of this function is undefined if the <i>fd</i> argument is less than 0
 * or greater than or equal to MPE_SOCKET_FD_SETSIZE, or if <i>fd</i> is not a valid file
 * descriptor.
 *
 * @param fd file descriptor to be added to <i>fdset</i>
 * @param fdset file descriptor set to be modified
 * @see POSIX macro FD_SET()
 */
void mpeos_socketFDSet(mpe_Socket fd, mpe_SocketFDSet *fdset);

/**
 * The mpeos_socketFDZero() function shall
 * initialize the descriptor set pointed to by <i>fdset</i> to the null set. No error
 * is returned if the set is not empty at the time this function is invoked.
 * The behavior of this function is undefined if the <i>fd</i> argument is less than 0
 * or greater than or equal to MPE_SOCKET_FD_SETSIZE, or if <i>fd</i> is not a valid file
 * descriptor.
 *
 * @param fdset file descriptor set to be initialized
 * @see POSIX macro FD_ZERO()
 */
void mpeos_socketFDZero(mpe_SocketFDSet *fdset);

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
 * @param res a pointer to a linked list of mpe_SocketAddrInfo structures, one
 *        for each network address that matches addr and port, subject to any
 *        restrictions imposed by hints. The items in the linked list are
 *        linked by the ai_next field.
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
 *
 * @see POSIX function getaddrinfo()
 */
int mpeos_socketGetAddrInfo(const char* addr, const char* port,
                            mpe_SocketAddrInfo* hints,
                            mpe_SocketAddrInfo** result);

/**
 * Release one or more mpe_SocketAddrInfo structures returned from
 * mpe_SocketGetAddrInfo().
 *
 * @see POSIX function freeaddrinfo()
 */
void mpeos_socketFreeAddrInfo(mpe_SocketAddrInfo* ai);


/**
 * The mpeos_socketGetHostByAddr() function shall return an entry containing addresses
 * of address family type for the host with address <i>addr</i>. The <i>len</i> argument
 * contains the length of the address pointed to by <i>addr</i>.
 * This information is considered to be stored in a database that can be accessed
 * sequentially or randomly. Implementation of this database is unspecified.
 * <p>
 * The <i>addr</i> argument shall be an mpe_SocketIPv4Addr structure when type is MPE_SOCKET_AF_INET4 or
 * an mpe_SocketIPv6Addr structure when type is MPE_SOCKET_AF_INET6. It
 * contains a binary format (that is, not null-terminated) address in network byte order.
 * This function is not guaranteed to return addresses of address families other than
 * MPE_SOCKET_AF_INET4 or MPE_SOCKET_AF_INET6, even when such addresses exist in the database.
 * <p>
 * If this function returns successfully, then the h_addrtype field in the result shall
 * be the same as the <i>type</i> argument that was passed to the function, and the
 * h_addr_list field shall list a single address that is a copy of the <i>addr</i> argument
 * that was passed to the function.
 *
 * @param addr pointer to the address in network byte order
 * @param len length of the address
 * @param type type of the address
 * @return Upon successful completion, this function shall return a pointer to a
 *          mpe_SocketHostEntry structure if the requested entry was found. The caller must never
 *          attempt to modify this structure or to free any of its components. Furthermore,
 *          only one copy of this structure is allocated, so the caller should copy
 *          any information it needs before calling any other socket function. On error,
 *          a null pointer is returned and one of the following error codes can be retrieved
 *          with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EHOSTNOTFOUND - No such host is known.
 * <li>     MPE_ENODATA - The server recognized the request and the <i>name</i>, but no
 *          address is available. Another type of request to the name server for the
 *          domain might return an answer.
 * <li>     MPE_SOCKET_ENORECOVERY - An unexpected server failure occurred which cannot be recovered.
 * <li>     MPE_SOCKET_ETRYAGAIN - A temporary and possibly transient error occurred, such as a
 *          failure of a server to respond.
 * </ul>
 * @see POSIX function gethostbyaddr()
 */
mpe_SocketHostEntry *mpeos_socketGetHostByAddr(const void *addr,
        mpe_SocketSockLen len, int type);

/**
 * The mpeos_socketGetHostByName() function shall return an entry containing addresses
 * of address family MPE_SOCKET_AF_INET4 or MPE_SOCKET_AF_INET6 for the host with name <i>name</i>.
 * This information is considered to be stored in a database that can be accessed
 * sequentially or randomly. Implementation of this database is unspecified.
 * <p>
 * The <i>name</i> argument shall be a node name; the behavior of this function when
 * passed a numeric address string is unspecified. For IPv4, a numeric address string
 * shall be in dotted-decimal notation.
 * <p>
 * If <i>name</i> is not a numeric address string and is an alias for a valid host name,
 * then this function shall return information about the host name to which the alias
 * refers, and name shall be included in the list of aliases returned.
 * <p>
 * If <i>name</i> is identical to the local host name as returned by <i>mpeos_socketGetHostName() </i>,
 * the returned <i>mpe_SocketHostEntry</i> structure shall contain a single address only.  That
 * address must identify the OCAP1.0 return channel or DOCSIS interface.
 *
 * @param name the host name
 * @return Upon successful completion, this function shall return a pointer to a
 *          mpe_SocketHostEntry structure if the requested entry was found. The caller must never
 *          attempt to modify this structure or to free any of its components. Furthermore,
 *          only one copy of this structure is allocated, so the caller should copy
 *          any information it needs before calling any other socket function. On error,
 *          a null pointer is returned and one of the following error codes can be retrieved
 *          with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EHOSTNOTFOUND - No such host is known.
 * <li>     MPE_ENODATA - The server recognized the request and the <i>name</i>, but no
 *          address is available. Another type of request to the name server for the
 *          domain might return an answer.
 * <li>     MPE_SOCKET_ENORECOVERY - An unexpected server failure occurred which cannot be recovered.
 * <li>     MPE_SOCKET_ETRYAGAIN - A temporary and possibly transient error occurred, such as a
 *          failure of a server to respond.
 * </ul>
 * @see POSIX function gethostbyname()
 */
mpe_SocketHostEntry *mpeos_socketGetHostByName(const char *name);

/**
 * The mpeos_socketGetHostName() function shall return the standard host name for the
 * current machine. The <i>namelen</i> argument shall specify the size of the array pointed
 * to by the <i>name</i> argument. The returned name is null-terminated and truncated if
 * insufficient space is provided.
 * <p>
 * Host names are limited to MPE_SOCKET_MAXHOSTNAMELEN bytes.
 *
 * @param name the buffer into which the host name should be written (returned).
 * @param namelen the size of the buffer pointed to by <i>name</i>
 * @return Upon successful completion, 0 shall be returned; otherwise, -1 shall be returned.
 * @see POSIX function gethostname()
 */
int mpeos_socketGetHostName(char *name, size_t namelen);

/**
 * The mpeos_socketGetSockName() function shall retrieve the locally-bound name of the
 * specified socket, store this address in the mpe_SocketSockAddr structure pointed to by
 * the <i>address</i> argument, and store the length of this address in the object pointed
 * to by the <i>address_len</i> argument.
 * <p>
 * If the actual length of the address is greater than the length of the supplied
 * mpe_SocketSockAddr structure, the stored address shall be truncated.
 * <p>
 * If the socket has not been bound to a local name, the value stored in the object pointed
 * to by <i>address</i> is unspecified.
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param address points to a mpe_SocketSockAddr structure where the name should be returned.
 * @param address_len specifies the length of the mpe_SocketSockAddr structure pointed
 *          to by the <i>address</i> argument. This is updated to reflect the length of the
 *          name actually copied to <i>address</i>.
 * @return Upon successful completion, 0 shall be returned, the <i>address</i> argument shall
 *          point to the address of the socket, and the <i>address_len</i> argument shall
 *          point to the length of the address. Otherwise, -1 shall be returned and one of
 *          the following error codes can be retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_EINVAL - The socket has been shut down.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to complete
 *          the function.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The operation is not supported for this socket's protocol.
 * </ul>
 * @see POSIX function getsockname()
 */
int mpeos_socketGetSockName(mpe_Socket socket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len);

/**
 * The mpeos_socketGetOpt() function manipulates options associated with a socket.
 * <p>
 * The mpeos_socketGetOpt() function shall retrieve the value for the option specified
 * by the <i>option_name</i> argument for the socket specified by the <i>socket</i> argument.
 * If the size of the option value is greater than <i>option_len</i>, the value stored in the
 * object pointed to by the <i>option_value</i> argument shall be silently truncated.
 * Otherwise, the object pointed to by the <i>option_len</i> argument shall be modified to
 * indicate the actual length of the value.
 * <p>
 * The <i>level</i> argument specifies the protocol level at which the option resides. To
 * retrieve options at the socket level, specify the <i>level</i> argument as MPE_SOCKET_SOL_SOCKET. To
 * retrieve options at other levels, supply the appropriate level identifier for the protocol
 * controlling the option. For example, to indicate that an option is interpreted by the TCP
 * (Transmission Control Protocol), set <i>level</i> to MPE_SOCKET_IPPROTO_TCP.
 * <p>
 * Valid values for <i>option_name</i> include the following where the datatype of the
 * option is in parenthesis:
 * <ul>
 * <li> MPE_SOCKET_SO_BROADCAST - permit sending of broadcast datagrams (int)
 * <li> MPE_SOCKET_SO_DEBUG - enable debug tracing (int)
 * <li> MPE_SOCKET_SO_DONTROUTE - bypass routing table lookup (int)
 * <li> MPE_SOCKET_SO_ERROR - get pending error and clear (int)
 * <li> MPE_SOCKET_SO_KEEPALIVE - periodically test if connection still alive (int)
 * <li> MPE_SOCKET_SO_LINGER - linger on close if data to send (mpe_SocketLinger)
 * <li> MPE_SOCKET_SO_OOBINLINE - leave received out-of-band data inline (int)
 * <li> MPE_SOCKET_SO_RCVBUF - receive buffer size (int)
 * <li> MPE_SOCKET_SO_SNDBUF - send buffer size (int)
 * <li> MPE_SOCKET_SO_RCVLOWAT - receive buffer low-water mark (int)
 * <li> MPE_SOCKET_SO_SNDLOWAT - send buffer low-water mark (int)
 * <li> MPE_SOCKET_SO_RCVTIMEO - receive timeout (mpe_TimeVal)
 * <li> MPE_SOCKET_SO_SNDTIMEO - send timeout (mpe_TimeVal)
 * <li> MPE_SOCKET_SO_REUSEADDR - allow local address reuse (int)
 * <li> MPE_SOCKET_SO_TYPE - get socket type (int)
 * <li> MPE_SOCKET_IPV4_MULTICAST_IF - specify outgoing interface (mpe_SocketIPv4Addr)
 * <li> MPE_SOCKET_IPV4_MULTICAST_LOOP - specify loopback (unsigned char)
 * <li> MPE_SOCKET_IPV4_MULTICAST_TTL - specify outgoing TTL (unsigned char)
 * <li> MPE_SOCKET_IPV6_MULTICAST_IF - specify outgoing interface (mpe_SocketIPv6Addr)
 * <li> MPE_SOCKET_IPV6_MULTICAST_HOPS - specify outgoing hop limit (unsigned int)
 * <li> MPE_SOCKET_IPV6_MULTICAST_LOOP - specify loopback (unsigned int)
 * <li> MPE_SOCKET_TCP_NODELAY - disable Nagle algorithm (int)
 * </ul>
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param level is the protocol level at which the option resides
 * @param option_name specifies a single option to be retrieved.
 * @param option_value points to storage sufficient to hold the value of the option. For
 *          boolean options, a zero value indicates that the option is disabled and a
 *          non-zero value indicates that the option is enabled.
 * @param option_len specifies the length of the buffer pointed to by <i>option_value</i>. It
 *          is updated by this function to indicate the number of bytes actually copied
 *          to <i>option_value</i>.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - The calling process does not have the appropriate privileges.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_EINVAL - The specified option is invalid at the specified socket level, or
 *          the socket has been shut down.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources are available in the system to complete
 *          the function.
 * <li>     MPE_SOCKET_ENOPROTOOPT - The option is not supported by the protocol.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * </ul>
 * @see POSIX function getsockopt()
 */
int mpeos_socketGetOpt(mpe_Socket socket, int level, int option_name,
        void *option_value, mpe_SocketSockLen *option_len);

/**
 * The mpeos_socketGetPeerName() function shall retrieve the peer address of the specified
 * socket, store this address in the mpe_SocketSockAddr structure pointed to by the <i>address</i>
 * argument, and store the length of this address in the object pointed to by the
 * <i>address_len</i> argument.
 * <p>
 * If the actual length of the address is greater than the length of the supplied
 * mpe_SocketSockAddr structure, the stored address shall be truncated.
 * <p>
 * If the protocol permits connections by unbound clients, and the peer is not bound, then
 * the value stored in the object pointed to by <i>address</i> is unspecified.
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param address points to a mpe_SocketSockAddr structure containing the peer address.
 * @param address_len specifies the length of the mpe_SocketSockAddr structure pointed
 *          to by the <i>address</i> argument.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_EINVAL - The socket has been shut down.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to complete
 *          the call.
 * <li>     MPE_SOCKET_ENOTCONN - The socket is not connected or otherwise has not had the peer
 *          pre-specified.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The operation is not supported for the socket protocol.
 * </ul>
 * @see POSIX function getpeername()
 */
int mpeos_socketGetPeerName(mpe_Socket socket, mpe_SocketSockAddr *address,
        mpe_SocketSockLen *address_len);

/**
 * The mpeos_socketHtoNL() function shall convert a 32-bit quantity from host byte order to
 * network byte order.
 *
 * @param hostlong the 32 bit value in host byte order
 * @return This function shall return <i>hostlong</i> converted to network byte order.
 * @see POSIX function htonl()
 */
uint32_t mpeos_socketHtoNL(uint32_t hostlong);

/**
 * The mpeos_socketHtoNS() function shall convert a 16-bit quantity from host byte order to
 * network byte order.
 *
 * @param hostshort the 16 bit value in host byte order
 * @return This function shall return <i>hostshort</i> converted to network byte order.
 * @see POSIX function htons()
 */
uint16_t mpeos_socketHtoNS(uint16_t hostshort);

/**
 * The mpeos_socketNtoHL() function shall convert a 32-bit quantity from network byte order to
 * host byte order.
 *
 * @param netlong the 32 bit value in network byte order
 * @return This function shall return <i>netlong</i> converted to host byte order.
 * @see POSIX function ntohl()
 */
uint32_t mpeos_socketNtoHL(uint32_t netlong);

/**
 * The mpeos_socketNtoHS() function shall convert a 16-bit quantity from network byte order to
 * host byte order.
 *
 * @param netshort the 16 bit value in network byte order
 * @return This function shall return <i>netshort</i> converted to host byte order.
 * @see POSIX function ntohs()
 */
uint16_t mpeos_socketNtoHS(uint16_t netshort);

/**
 * The mpeos_socketIoctl() function shall perform a variety of control functions on a socket.
 * The <i>request</i> argument selects the control function to be performed. The <i>arg</i>
 * argument represents additional information that is needed to perform the requested function.
 * The type of <i>arg</i> depends upon the particular control request.
 * <p>
 * Valid values for <i>request</i> include:
 * <ul>
 * <li> MPE_SOCKET_FIONBIO - The nonblocking flag for the socket is cleared or turned on, depending
 *      on whether the third argument points to a zero or nonzero integer value, respectively.
 * <li> MPE_SOCKET_FIONREAD - Return in the integer pointed to by the third argument, the number of
 *      bytes currently in the socket receive buffer.
 * </ul>
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param request the control function to perform
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_EINTR - This function was interrupted by a signal that was caught, before any
 *          data was available.
 * <li>     MPE_EINVAL - The <i>request</i> or <i>arg</i> argument is not valid for the
 *          specified socket.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * </ul>
 * @see POSIX function ioctl()
 */
int mpeos_socketIoctl(mpe_Socket socket, int request, ...);

/**
 * The mpeos_socketListen() function shall mark a connection-mode socket, specified
 * by the <i>socket</i> argument, as accepting connections.
 * <p>
 * The <i>backlog</i> argument provides a hint to the implementation which the implementation
 * shall use to limit the number of outstanding connections in the socket's listen queue.
 * Implementations may impose a limit on <i>backlog</i> and silently reduce the specified value.
 * Normally, a larger <i>backlog</i> argument value shall result in a larger or equal length of
 * the listen queue.
 * <p>
 * The implementation may include incomplete connections in its listen queue. The limits
 * on the number of incomplete connections and completed connections queued may be different.
 * <p>
 * The implementation may have an upper limit on the length of the listen queue-either
 * global or per accepting socket. If <i>backlog</i> exceeds this limit, the length of the listen
 * queue is set to the limit.
 * <p>
 * If this function is called with a <i>backlog</i> argument value that is less than 0, the
 * function behaves as if it had been called with a <i>backlog</i> argument value of 0.
 * <p>
 * A <i>backlog</i> argument of 0 may allow the socket to accept connections, in which case the
 * length of the listen queue may be set to an implementation-defined minimum value.
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param backlog the requested maximum number of connections to allow in the socket's
 *          listen queue.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - The calling process does not have the appropriate privileges.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_EDESTADDRREQ - The socket is not bound to a local address, and the protocol
 *          does not support listening on an unbound socket.
 * <li>     MPE_EINVAL - The socket is already connected or the socket has been shut down.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources are available in the system to complete the
 *          call.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The socket protocol does not support <i>mpeos_socketListen()</i>.
 * </ul>
 * @see POSIX function listen()
 */
int mpeos_socketListen(mpe_Socket socket, int backlog);

/**
 * The mpeos_socketAtoN() function interprets the specified character string as an Internet
 * address, placing the address into the structure provided. All Internet addresses are
 * returned in network byte order (bytes are ordered from left to right). All network numbers
 * and local address parts are returned as machine-format integer values. Using the dot
 * notation, you can specify addresses in one of the following forms:
 * <ul>
 * <li>     a.b.c.d: When four parts are specified, each shall be interpreted as a byte of
 *          data and assigned, from left to right, to the four bytes of an Internet address.
 * <li>     a.b.c: When a three-part address is specified, the last part shall be interpreted
 *          as a 16-bit quantity and placed in the rightmost two bytes of the network address.
 *          This makes the three-part address format convenient for specifying Class B network
 *          addresses as "128.net.host".
 * <li>     a.b: When a two-part address is supplied, the last part shall be interpreted as a
 *          24-bit quantity and placed in the rightmost three bytes of the network address.
 *          This makes the two-part address format convenient for specifying Class A network
 *          addresses as "net.host".
 * <li>     a: When only one part is given, the value shall be stored directly in the network
 *          address without any byte rearrangement.
 * </ul>
 * All numbers supplied as parts in IPv4 dotted decimal notation may be decimal, octal, or
 * hexadecimal, as specified in the ISO C standard (that is, a leading 0x or 0X implies
 * hexadecimal; otherwise, a leading '0' implies octal; otherwise, the number is interpreted
 * as decimal).
 *
 * @param strptr pointer to Internet address in dot notation
 * @param addrptr pointer to structure to hold the address in numeric form
 * @return This function shall return 1 if the string was successfully interpreted; 0 if the
 *          string is invalid.
 * @see POSIX function inet_aton()
 */
int mpeos_socketAtoN(const char *strptr, mpe_SocketIPv4Addr *addrptr);

/**
 * The mpeos_socketNtoA() function converts the Internet address stored in the <i>inaddr</i>
 * argument into an ASCII string representing the address in dot notation (for
 * example, 127.0.0.1).
 *
 * @param inaddr Internet host address
 * @return This function shall return a pointer to the network address in Internet standard
 *          dot notation.
 * @see POSIX function inet_ntoa()
 */
char *mpeos_socketNtoA(mpe_SocketIPv4Addr inaddr);

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
const char *mpeos_socketNtoP(int af, const void *src, char *dst, size_t size);

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
int mpeos_socketPtoN(int af, const char *src, void *dst);

/**
 * The mpeos_socketRecv() function shall receive a message from a connection-mode or
 * connectionless-mode socket. It is normally used with connected sockets because it does
 * not permit the caller to retrieve the source address of received data.
 * <p>
 * This function shall return the length of the message written to the buffer pointed to
 * by the <i>buffer</i> argument. For message-based sockets, such as MPE_SOCKET_DATAGRAM, the entire
 * message shall be read in a single operation. If a message is too long to fit in the
 * supplied buffer, and MPE_SOCKET_MSG_PEEK is not set in the <i>flags</i> argument, the excess bytes
 * shall be discarded. For stream-based sockets, such as MPE_SOCKET_STREAM, message
 * boundaries shall be ignored. In this case, data shall be returned to the user as
 * soon as it becomes available, and no data shall be discarded.
 * <p>
 * If no messages are available at the socket, this function shall block until a message
 * arrives.
 *
 * @param socket specifies the socket file descriptor
 * @param buffer points to a buffer where the message should be stored
 * @param length specifies the length in bytes of the buffer pointed to by the <i>buffer</i>
 *          argument
 * @param flags specifies the type of message reception. Values of this argument are formed
 *          by logically OR'ing zero or more of the following values:
 *          <ul>
 *          <li> MPE_SOCKET_MSG_PEEK - Peeks at an incoming message. The data is treated as unread and
 *               the next mpeos_socketRecv() or similar function shall still return this
 *               data.
 *          <li> MPE_SOCKET_MSG_OOB - Requests out-of-band data. The significance and semantics of
 *               out-of-band data are protocol-specific.
 *          </ul>
 * @return Upon successful completion, this function shall return the length of the message in
 *          bytes. If no messages are available to be received and the peer has performed an
 *          orderly shutdown, this function shall return 0. Otherwise, -1 shall be returned
 *          and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EAGAIN or MPE_SOCKET_EWOULDBLOCK - The receive operation timed out and no data is waiting
 *          to be received; or MPE_SOCKET_MSG_OOB is set and no out-of-band data is available and the
 *          socket does not support blocking to await out-of-band data.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_ECONNRESET - A connection was forcibly closed by a peer.
 * <li>     MPE_SOCKET_EINTR - This function was interrupted by a signal that was caught, before any
 *          data was available.
 * <li>     MPE_EINVAL - The MPE_SOCKET_MSG_OOB flag is set and no out-of-band data is available.
 * <li>     MPE_SOCKET_EIO - An I/O error occurred while reading from or writing to the file system.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to perform the
 *          operation.
 * <li>     MPE_ENOMEM - Insufficient memory was available to fulfill the request.
 * <li>     MPE_SOCKET_ENOTCONN - A receive is attempted on a connection-mode socket that is not
 *          connected.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The specified <i>flags</i> are not supported for this socket
 *          type or protocol.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * <li>     MPE_SOCKET_ETIMEDOUT - The connection timed out during connection establishment, or
 *          due to a transmission timeout on active connection.
 * </ul>
 * @see POSIX function recv()
 */
size_t mpeos_socketRecv(mpe_Socket socket, void *buffer, size_t length,
        int flags);

/**
 * The mpeos_socketRecvFrom() function shall receive a message from a connection-mode or
 * connectionless-mode socket. It is normally used with connectionless-mode sockets because
 * it permits the caller to retrieve the source address of received data.
 * <p>
 * This function shall return the length of the message written to the buffer pointed to by
 * the <i>buffer</i> argument. For message-based sockets, such as MPE_SOCKET_DATAGRAM, the entire message
 * shall be read in a single operation. If a message is too long to fit in the supplied buffer,
 * and MPE_SOCKET_MSG_PEEK is not set in the <i>flags</i> argument, the excess bytes shall be discarded.
 * For stream-based sockets, such as MPE_SOCKET_STREAM, message boundaries shall be ignored.
 * In this case, data shall be returned to the user as soon as it becomes available, and no
 * data shall be discarded.
 * <p>
 * Not all protocols provide the source address for messages. If the <i>address</i> argument is not
 * a null pointer and the protocol provides the source address of messages, the source
 * address of the received message shall be stored in the mpe_SocketSockAddr structure pointed
 * to by the <i>address</i> argument, and the length of this address shall be stored in the object
 * pointed to by the <i>address_len</i> argument.
 * <p>
 * If the actual length of the address is greater than the length of the supplied mpe_SocketSockAddr
 * structure, the stored address shall be truncated.
 * <p>
 * If the <i>address</i> argument is not a null pointer and the protocol does not provide the source
 * address of messages, the value stored in the object pointed to by <i>address</i> is unspecified.
 * <p>
 * If no messages are available at the socket this function shall block until a message
 * arrives.
 *
 * @param socket specifies the socket file descriptor
 * @param buffer points to the buffer where the message should be stored
 * @param length specifies the length in bytes of the buffer pointed to by the <i>buffer</i>
 *          argument
 * @param flags specifies the type of message reception. Values of this argument are formed
 *          by logically OR'ing zero or more of the following values:
 *          <ul>
 *          <li> MPE_SOCKET_MSG_PEEK - Peeks at an incoming message. The data is treated as unread
 *               and the next mpeos_socketRecvFrom() or similar function shall still return
 *               this data.
 *          <li> MPE_SOCKET_MSG_OOB - Requests out-of-band data. The significance and semantics of
 *               out-of-band data are protocol-specific.
 * @param address a null pointer, or points to a mpe_SocketSockAddr structure in which the
 *          sending address is to be stored. The length and format of the address depend
 *          on the address family of the socket.
 * @param address_len specifies the length of the mpe_SocketSockAddr structure pointed to by
 *          the <i>address</i> argument.
 * @return Upon successful completion, this function shall return the length of the message
 *          in bytes. If no messages are available to be received and the peer has performed
 *          an orderly shutdown, this function shall return 0. Otherwise, the function shall
 *          return -1 and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EAGAIN or MPE_SOCKET_EWOULDBLOCK - The receive operation timed out and no data is waiting
 *          to be received; or MPE_SOCKET_MSG_OOB is set and no out-of-band data is available and the
 *          socket does not support blocking to await out-of-band data.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_ECONNRESET - A connection was forcibly closed by a peer.
 * <li>     MPE_SOCKET_EINTR - This function was interrupted by a signal that was caught, before any
 *          data was available.
 * <li>     MPE_EINVAL - The MPE_SOCKET_MSG_OOB flag is set and no out-of-band data is available.
 * <li>     MPE_SOCKET_EIO - An I/O error occurred while reading from or writing to the file system.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to perform the
 *          operation.
 * <li>     MPE_ENOMEM - Insufficient memory was available to fulfill the request.
 * <li>     MPE_SOCKET_ENOTCONN - A receive is attempted on a connection-mode socket that is not
 *          connected.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The specified <i>flags</i> are not supported for this socket
 *          type.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * <li>     MPE_SOCKET_ETIMEDOUT - The connection timed out during connection establishment, or
 *          due to a transmission timeout on active connection.
 * </ul>
 * @see POSIX function recvfrom()
 */
size_t mpeos_socketRecvFrom(mpe_Socket socket, void *buffer, size_t length,
        int flags, mpe_SocketSockAddr *address, mpe_SocketSockLen *address_len);

/**
 * The mpeos_socketSelect() function shall
 * examine the file descriptor sets whose addresses are passed in the <i>readfds</i>,
 * <i>writefds</i>, and <i>errorfds</i> parameters to see whether some of their descriptors
 * are ready for reading, are ready for writing, or have an exceptional condition pending,
 * respectively.
 * The behavior of this function on non-socket file descriptors is unspecified.
 * <p>
 * Upon successful completion, this function shall modify the objects pointed to by the
 * <i>readfds</i>, <i>writefds</i>, and <i>errorfds</i> arguments to indicate which file
 * descriptors are ready for reading, ready for writing, or have an error condition pending,
 * respectively, and shall return the total number of ready descriptors in all the output sets.
 * For each file descriptor less than <i>numfds</i>, the corresponding bit shall be set on
 * successful completion if it was set on input and the associated condition is true for
 * that file descriptor.
 * <p>
 * If none of the selected descriptors are ready for the requested operation, this function
 * shall block until at least one of the requested operations becomes ready, until the
 * timeout occurs, or until interrupted by a signal. The <i>timeout</i> parameter controls how
 * long this function shall take before timing out. If the <i>timeout</i> parameter is not a null
 * pointer, it specifies a maximum interval to wait for the selection to complete. If the
 * specified time interval expires without any requested operation becoming ready, the
 * function shall return. If the <i>timeout</i> parameter is a null pointer, then the call to this
 * function shall block indefinitely until at least one descriptor meets the specified
 * criteria. To effect a poll, the <i>timeout</i> parameter should not be a null pointer, and
 * should point to a zero-valued mpe_TimeVal structure.
 * <p>
 * Implementations may place limitations on the maximum timeout interval supported. If
 * the <i>timeout</i> argument specifies a timeout interval greater than the implementation-defined
 * maximum value, the maximum value shall be used as the actual timeout value.
 * Implementations may also place limitations on the granularity of timeout intervals. If
 * the requested timeout interval requires a finer granularity than the implementation
 * supports, the actual timeout interval shall be rounded up to the next supported value.
 * <p>
 * If the <i>readfds</i>, <i>writefds</i>, and <i>errorfds</i> arguments are all null pointers
 * and the <i>timeout</i> argument is not a null pointer, this function shall block for the
 * time specified, or until interrupted by a signal. If the <i>readfds</i>, <i>writefds</i>,
 * and <i>errorfds</i> arguments are all null pointers and the <i>timeout</i> argument is a
 * null pointer, this function shall block until interrupted by a signal.
 * <p>
 * On failure, the objects pointed to by the <i>readfds</i>, <i>writefds</i>, and <i>errorfds</i>
 * arguments shall not be modified. If the timeout interval expires without the specified
 * condition being true for any of the specified file descriptors, the objects pointed to
 * by the <i>readfds</i>, <i>writefds</i>, and <i>errorfds</i> arguments shall have all bits
 * set to 0.
 *
 * @param numfds the range of descriptors to be tested. The first <i>numfds</i> descriptors
 *          shall be checked in each set; that is, the descriptors from zero through
 *          <i>numfds</i>-1 in the descriptor sets shall be examined.
 * @param readfds if the <i>readfds</i> argument is not a null pointer, it points to an object
 *          of type mpe_SocketFDSet that on input specifies the file descriptors to be
 *          checked for being ready to read, and on output indicates which file descriptors
 *          are ready to read.
 * @param writefds if the <i>writefds</i> argument is not a null pointer, it points to an object
 *          of type mpe_SocketFDSet that on input specifies the file descriptors to be
 *          checked for being ready to write, and on output indicates which file descriptors
 *          are ready to write.
 * @param errorfds if the <i>errorfds</i> argument is not a null pointer, it points to an object
 *          of type mpe_SocketFDSet that on input specifies the file descriptors to be
 *          checked for error conditions pending, and on output indicates which file descriptors
 *          have error conditions pending.
 * @param timeout the timeout period in seconds and microseconds.
 * @return Upon successful completion, this function shall return the total number of bits
 *          set in the bit masks. Otherwise, -1 shall be returned, and one of the following
 *          error codes can be retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - One or more of the file descriptor sets specified a file descriptor
 *          that is not a valid open file descriptor.
 * <li>     MPE_SOCKET_EINTR - The function was interrupted before any of the selected events
 *          occurred and before the timeout interval expired.
 * <li>     MPE_EINVAL - An invalid timeout interval was specified; or the <i>nfds</i>
 *          argument is less than 0 or greater than MPE_SOCKET_FD_SETSIZE.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * </ul>
 * @see POSIX function select()
 */
int mpeos_socketSelect(int numfds, mpe_SocketFDSet *readfds,
        mpe_SocketFDSet *writefds, mpe_SocketFDSet *errorfds,
        const mpe_TimeVal *timeout);

/**
 * The mpeos_socketSend() function shall initiate transmission of a message from the
 * specified socket to its peer. This function shall send a message only when the socket
 * is connected (including when the peer of a connectionless socket has been set via
 * mpeos_socketConnect()).
 * <p>
 * The length of the message to be sent is specified by the <i>length</i> argument. If the
 * message is too long to pass through the underlying protocol, this function shall fail
 * and no data shall be transmitted.
 * <p>
 * Successful completion of a call to this function does not guarantee delivery of the
 * message. A return value of -1 indicates only locally-detected errors.
 * <p>
 * If space is not available at the sending socket to hold the message to be transmitted,
 * this function shall block until space is available. The mpeos_socketSelect() function
 * can be used to determine when it is possible to send more data.
 *
 * @param socket specifies the socket file descriptor
 * @param buffer points to the buffer containing the message to send
 * @param length specifies the length of the message in bytes
 * @param flags specifies the type of message transmission. Values of this argument are
 *          formed by logically OR'ing zero or more of the following flags:
 *          <ul>
 *          <li> MPE_SOCKET_MSG_OOB - Sends out-of-band data on sockets that support
 *               out-of-band communications. The significance and semantics of
 *               out-of-band data are protocol-specific.
 *          </ul>
 * @return Upon successful completion, this function shall return the number of bytes
 *          sent. Otherwise, -1 shall be returned and one of the following error codes
 *          can be retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - The calling process does not have the appropriate privileges.
 * <li>     MPE_SOCKET_EAGAIN or MPE_SOCKET_EWOULDBLOCK - The send operation timed out and no data was
 *          sent.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_ECONNRESET - A connection was forcibly closed by a peer.
 * <li>     MPE_SOCKET_EDESTADDRREQ - The socket is not connection-mode and no peer address is set.
 * <li>     MPE_SOCKET_EINTR - A signal interrupted this function before any data was transmitted.
 * <li>     MPE_SOCKET_EIO - An I/O error occurred while reading from or writing to the file system.
 * <li>     MPE_SOCKET_EMSGSIZE - The message is too large to be sent all at once, as the socket
 *          requires.
 * <li>     MPE_SOCKET_ENETDOWN - The local network interface used to reach the destination is down.
 * <li>     MPE_SOCKET_ENETUNREACH - No route to the network is present.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to perform the
 *          operation.
 * <li>     MPE_SOCKET_ENOTCONN - The socket is not connected or otherwise has not had the peer
 *          pre-specified.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The <i>socket</i> argument is associated with a socket that does
 *          not support one or more of the values set in <i>flags</i>.
 * <li>     MPE_SOCKET_EPIPE - The socket is shut down for writing, or the socket is connection-mode
 *          and is no longer connected. In the latter case, and if the socket is of type
 *          MPE_SOCKET_STREAM, the MPE_SIGPIPE signal is generated to the calling thread.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * </ul>
 * @see POSIX function send()
 */
size_t mpeos_socketSend(mpe_Socket socket, const void *buffer, size_t length,
        int flags);

/**
 * The mpeos_socketSendTo() function shall send a message through a connection-mode or
 * connectionless-mode socket. If the socket is connectionless-mode, the message shall be
 * sent to the address specified by <i>dest_addr</i>. If the socket is connection-mode,
 * <i>dest_addr</i> shall be ignored.
 * <p>
 * If the socket protocol supports broadcast and the specified address is a broadcast
 * address for the socket protocol, this function shall fail if the MPE_SOCKET_SO_BROADCAST
 * option is not set for the socket.
 * <p>
 * The <i>dest_addr</i> argument specifies the address of the target. The <i>length</i> argument
 * specifies the length of the message.
 * <p>
 * Successful completion of a call to this function does not guarantee delivery of the
 * message. A return value of -1 indicates only locally-detected errors.
 * <p>
 * If space is not available at the sending socket to hold the message to be transmitted,
 * this function shall block until space is available.
 *
 * @param socket specifies the socket file descriptor
 * @param message points to a buffer containing the message to be sent
 * @param length specifies the size of the message in bytes
 * @param flags specifies the type of message transmission. Values of this argument are
 *          formed by logically OR'ing zero or more of the following flags:
 *          <ul>
 *          <li> MPE_SOCKET_MSG_OOB - Sends out-of-band data on sockets that support
 *               out-of-band data. The significance and semantics of out-of-band data
 *               are protocol-specific.
 *          </ul>
 * @param dest_addr points to a mpe_SocketSockAddr structure containing the destination
 *          address. The length and format of the address depend on the address
 *          family of the socket.
 * @param dest_len specifies the length of the mpe_SocketSockAddr structure pointed to by
 *          the <i>dest_addr</i> argument.
 * @return Upon successful completion, this function shall return the number of bytes sent.
 *          Otherwise, -1 shall be returned and one of the following error codes can be
 *          retrieved with mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EACCES - Search permission is denied for a component of the path prefix;
 *          or write access to the named socket is denied.
 * <li>     MPE_SOCKET_EAFNOSUPPORT - Addresses in the specified address family cannot be used
 *          with this socket.
 * <li>     MPE_SOCKET_EAGAIN or MPE_SOCKET_EWOULDBLOCK - The send operation timed out and no data was
 *          sent.
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_ECONNRESET - A connection was forcibly closed by a peer.
 * <li>     MPE_SOCKET_EDESTADDRREQ - The socket is not connection-mode and does not have its peer
 *          address set, and no destination address was specified.
 * <li>     MPE_SOCKET_EHOSTUNREACH - The destination host cannot be reached (probably because the
 *          host is down or a remote router cannot reach it).
 * <li>     MPE_SOCKET_EINTR - A signal interrupted this function before any data was transmitted.
 * <li>     MPE_EINVAL - The <i>dest_len</i> argument is not a valid length for the address
 *          family.
 * <li>     MPE_SOCKET_EIO - An I/O error occurred while reading from or writing to the file system.
 * <li>     MPE_SOCKET_EISCONN - A destination address was specified and the socket is already
 *          connected. This error may or may not be returned for connection mode sockets.
 * <li>     MPE_SOCKET_EMSGSIZE - The message is too large to be sent all at once, as the socket
 *          requires.
 * <li>     MPE_SOCKET_ENETDOWN - The local network interface used to reach the destination is down.
 * <li>     MPE_SOCKET_ENETUNREACH - No route to the network is present.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to perform the
 *          operation.
 * <li>     MPE_ENOMEM - Insufficient memory was available to fulfill the request.
 * <li>     MPE_SOCKET_ENOTCONN - The socket is connection-mode but is not connected.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * <li>     MPE_SOCKET_EOPNOTSUPP - The <i>socket</i> argument is associated with a socket that
 *          does not support one or more of the values set in <i>flags</i>.
 * <li>     MPE_SOCKET_EPIPE - The socket is shut down for writing, or the socket is connection-mode
 *          and is no longer connected. In the latter case, and if the socket is of type
 *          MPE_SOCKET_STREAM, the MPE_SIGPIPE signal is generated to the calling thread.
 * <li>     MPE_ETHREADDEATH - The thread executing this function has been marked for death.
 * </ul>
 * @see POSIX function sendto()
 */
size_t mpeos_socketSendTo(mpe_Socket socket, const void *message,
        size_t length, int flags, const mpe_SocketSockAddr *dest_addr,
        mpe_SocketSockLen dest_len);

/**
 * The mpeos_socketSetOpt() function shall set the option specified by the <i>option_name</i>
 * argument, at the protocol level specified by the <i>level</i> argument, to the value pointed
 * to by the <i>option_value</i> argument for the socket associated with the file descriptor
 * specified by the <i>socket</i> argument.
 * <p>
 * The <i>level</i> argument specifies the protocol level at which the option resides. To set
 * options at the socket level, specify the <i>level</i> argument as MPE_SOCKET_SOL_SOCKET. To set
 * options at other levels, supply the appropriate level identifier for the protocol
 * controlling the option. For example, to indicate that an option is interpreted by the
 * TCP (Transport Control Protocol), set <i>level</i> to MPE_SOCKET_IPPROTO_TCP.
 * <p>
 * The <i>option_name</i> argument specifies a single option to set. The <i>option_name</i> argument and
 * any specified options are passed uninterpreted to the appropriate protocol module for
 * interpretations.
 * <p>
 * Valid values for <i>option_name</i> include:
 * <ul>
 * <li> MPE_SOCKET_SO_BROADCAST - permit sending of broadcast datagrams (int)
 * <li> MPE_SOCKET_SO_DEBUG - enable debug tracing (int)
 * <li> MPE_SOCKET_SO_DONTROUTE - bypass routing table lookup (int)
 * <li> MPE_SOCKET_SO_KEEPALIVE - periodically test if connection still alive (int)
 * <li> MPE_SOCKET_SO_LINGER - linger on close if data to send (mpe_SocketLinger)
 * <li> MPE_SOCKET_SO_OOBINLINE - leave received out-of-band data inline (int)
 * <li> MPE_SOCKET_SO_RCVBUF - receive buffer size (int)
 * <li> MPE_SOCKET_SO_SNDBUF - send buffer size (int)
 * <li> MPE_SOCKET_SO_RCVLOWAT - receive buffer low-water mark (int)
 * <li> MPE_SOCKET_SO_SNDLOWAT - send buffer low-water mark (int)
 * <li> MPE_SOCKET_SO_RCVTIMEO - receive timeout (mpe_TimeVal)
 * <li> MPE_SOCKET_SO_SNDTIMEO - send timeout (mpe_TimeVal)
 * <li> MPE_SOCKET_SO_REUSEADDR - allow local address reuse (int)
 * <li> MPE_SOCKET_IPV4_ADD_MEMBERSHIP - join a multicast group (mpe_SocketIPv4McastReq)
 * <li> MPE_SOCKET_IPV4_DROP_MEMBERSHIP - leave a multicast group (mpe_SocketIPv4McastReq)
 * <li> MPE_SOCKET_IPV4_MULTICAST_IF - specify outgoing interface (mpe_SocketIPv4Addr)
 * <li> MPE_SOCKET_IPV4_MULTICAST_LOOP - specify loopback (unsigned char)
 * <li> MPE_SOCKET_IPV4_MULTICAST_TTL - specify outgoing TTL (unsigned char)
 * <li> MPE_SOCKET_IPV6_ADD_MEMBERSHIP - join a multicast group (mpe_SocketIPv6McastReq)
 * <li> MPE_SOCKET_IPV6_DROP_MEMBERSHIP - leave a multicast group (mpe_SocketIPv4McastReq)
 * <li> MPE_SOCKET_IPV6_MULTICAST_IF - specify outgoing interface (mpe_SocketIPv6Addr)
 * <li> MPE_SOCKET_IPV6_MULTICAST_HOPS - specify outgoing hop limit (unsigned int)
 * <li> MPE_SOCKET_IPV6_MULTICAST_LOOP - specify loopback (unsigned int)
 * <li> MPE_SOCKET_TCP_NODELAY - disable Nagle algorithm (int)
 * </ul>
 *
 * @param socket specifies the file descriptor associated with the socket.
 * @param level is the protocol level at which the option resides
 * @param option_name specifies a single option to be set.
 * @param option_value points to the new value for the option
 * @param option_len specifies the length of option pointed to by <i>option_value</i>.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_SOCKET_EDOM - The send and receive timeout values are too big to fit into the
 *          timeout fields in the socket structure.
 * <li>     MPE_EINVAL - The specified option is invalid at the specified socket level or
 *          the socket has been shut down.
 * <li>     MPE_SOCKET_EISCONN - The socket is already connected, and a specified option cannot
 *          be set while the socket is connected.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources are available in the system to complete
 *          the call.
 * <li>     MPE_ENOMEM - There was insufficient memory available for the operation to
 *          complete.
 * <li>     MPE_SOCKET_ENOPROTOOPT - The option is not supported by the protocol.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * </ul>
 * @see POSIX function setsockopt()
 */
int mpeos_socketSetOpt(mpe_Socket socket, int level, int option_name,
        const void *option_value, mpe_SocketSockLen option_len);

/**
 * The mpeos_socketShutdown() function shall cause all or part of a full-duplex connection
 * on the socket associated with the file descriptor <i>socket</i> to be shut down.
 * This function disables subsequent send and/or receive operations on a socket, depending
 * on the value of the <i>how</i> argument.
 *
 * @param socket specifies the file descriptor of the socket
 * @param how specifies the type of shutdown. The values are as follows:
 *          <ul>
 *          <li> MPE_SOCKET_SHUT_RD - Disables further receive operations.
 *          <li> MPE_SOCKET_SHUT_WR - Disables further send operations.
 *          <li> MPE_SOCKET_SHUT_RDWR - Disables further send and receive operations.
 * @return Upon successful completion, this function shall return 0; otherwise, -1 shall
 *          be returned and one of the following error codes can be retrieved with
 *          mpeos_socketGetLastError():
 * <ul>
 * <li>     MPE_SOCKET_EBADF - The <i>socket</i> argument is not a valid file descriptor.
 * <li>     MPE_EINVAL - The <i>how</i> argument is invalid.
 * <li>     MPE_SOCKET_ENOBUFS - Insufficient resources were available in the system to perform
 *          the operation.
 * <li>     MPE_SOCKET_ENOTCONN - The socket is not connected.
 * <li>     MPE_SOCKET_ENOTSOCK - The <i>socket</i> argument does not refer to a socket.
 * </ul>
 * @see POSIX function shutdown()
 */
int mpeos_socketShutdown(mpe_Socket socket, int how);

/**
 * The mpeos_socketGetInterfaces() function acquires a pointer to a linked
 * list structure of the network interfaces and their associated addresses.
 * The addresses are also represented by a linked list structure.
 *
 * The <i>mpe_SocketNetAddr<i/> structure represents a
 * single network interface address in the linked list of addresses for an interface.
 *
 * The <i>mpe_SocketNetIfList<i/> structure returned by this function represents a
 * single network interface with its list of associated addresses.
 *
 * The pointer returned is to an internal buffer that should not be modified by the caller.
 *
 * @param interfaces is a pointer for returning the address of the interface buffer.
 * @return MPE_SUCCESS if the operation succeeded.
 * <ul>
 * <li>     MPE_EINVAL - invalid pointer argument.
 * <li>     MPE_ENODATA - if an error occurred attempting to acquire the list.
 * <li>     MPE_ENOMEM - if a memory allocation error occurred.
 * </ul>
 */
mpe_Error mpeos_socketGetInterfaces(mpe_SocketNetIfList **netIfList);

/**
 * Free the mpe_socketNetIfList structure previously allocated via
 * mpeos_socketGetInterfaces() call.
 *
 * @param netIfList is a pointer to the structure previously allocated
 *                  via mpeos_socketGetInterfaces() call.
 * 
 */
void mpeos_socketFreeInterfaces(mpe_SocketNetIfList *netIfList);

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
mpe_Error mpeos_socketSetDLNAQOS(mpe_Socket sock, mpe_SocketDLNAQOS value);

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
mpe_Error mpeos_getDLNANetworkInterfaceInfo(char* interfaceName, mpeos_LpeDlnaNetworkInterfaceInfo* dlnaNetworkIfInfo);

/**
 * Gets the LPE NetworkInterfaceMode information, after providing the interfaceName.
 * interfaceName can be eth0, eth1, em1 etc.
 * @return Upon successful completion, returns MPE_SUCCESS and
 * populates the string describing the NetworkInterfaceMode with the correct value.
 * NetworkInterfaceMode can be: "Unimplemented", "IP-up", "IP-up-Periodic", "IP-down-no-Wake",
 * "IP-down-with-WakeOn", "IP-down-with-WakeAuto", "IP-down-with-WakeOnAuto".
 * On error, MPE_ENODATA is returned and string describing the NetworkInterfaceMode is
 * set to "Unimplemented".
 */
mpe_Error mpeos_getDLNANetworkInterfaceMode(char* interfaceName, char** lpeDlnaNetworkInterfaceModeInfo);


/**
 * Sets a unique IPV4 link local address on the supplied interface.
 * This is a synchronous call that will block until the address assignment
 * succeeds or fails. 
 * If the supplied interface already is assigned a link local address
 * then this returns MPE_SUCCESS and does nothing.
 *
 *
 * @param interface                    caller allocated null terminated string that
 *                                     contains the interface to assign a unique IPV4
 *                                     link local address to.
 *
 * @return MPE_SUCCESS                 If successful.
 * @return MPE_ENODATA                 If any errors encountered. 
 *                                  
 **/
mpe_Error mpeos_socketSetLinkLocalAddress(char *interface);

/**
 * Registers an object to receive notification of IPV4 address changes
 * for a particular interface. Only IPV4 changes for that interface
 * are signalled.
 *
 *
 * @param interface                    caller allocated null terminated string that
 *                                     contains the interface the caller registers
 *                                     for notifications.
 * @param queueId                      event queue identifier used to post events
 *                                     related to IP address changes. 
 * @param act                          the completion token delivered with events 
 *                                     (optionalEventData2) 
 *
 * @return MPE_SUCCESS                 If successful.
 * @return MPE_ENODATA                 If any errors encountered.
 **/
mpe_Error mpeos_socketRegisterForIPChanges(char *interface, mpe_EventQueue queueId, void *act);

#ifdef __cplusplus
}
#endif
#endif
