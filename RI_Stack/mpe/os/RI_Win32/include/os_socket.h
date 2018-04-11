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
 * Map definitions in the MPE OS Sockets API to the CableLabs RI Simulator platform.
 * This is typically just a direct mapping to the cooresponding Windows Socket definition.
 *
 * For ease of maintenance the complete description of all socket types are commented in
 * mpeos_socket.h and not in each of the platform specific headers such as this one.
 *
 */

#if !defined(_OS_SOCKET_H)
#define _OS_SOCKET_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <winsock2.h>
#include <ws2tcpip.h>

/****************************************************************************************
 *
 * PROTOCOL INDEPENDENT DEFINITIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_FD_SETSIZE                FD_SETSIZE
#define OS_SOCKET_MAXHOSTNAMELEN            MAXGETHOSTSTRUCT
#define OS_SOCKET_MSG_OOB                   MSG_OOB
#define OS_SOCKET_MSG_PEEK                  MSG_PEEK
#define OS_SOCKET_SHUTDOWN_RD               SD_RECEIVE
#define OS_SOCKET_SHUTDOWN_RDWR             SD_BOTH
#define OS_SOCKET_SHUTDOWN_WR               SD_SEND
#define OS_SOCKET_DATAGRAM                  SOCK_DGRAM
#define OS_SOCKET_STREAM                    SOCK_STREAM
#define OS_SOCKET_INVALID_SOCKET            INVALID_SOCKET
#define OS_SOCKET_FIONBIO                   FIONBIO
#define OS_SOCKET_FIONREAD                  FIONREAD
typedef SOCKET os_Socket;
typedef int os_SocketSockLen;
typedef fd_set os_SocketFDSet;
typedef struct addrinfo os_SocketAddrInfo;
typedef struct hostent os_SocketHostEntry;
typedef struct linger os_SocketLinger;
typedef unsigned short os_SocketSaFamily;
typedef struct sockaddr os_SocketSockAddr;

#define OS_SOCKET_AF_UNSPEC                 AF_UNSPEC

/****************************************************************************************
 *
 * IPv4 SPECIFIC DEFINITIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_AF_INET4                  AF_INET
#define OS_SOCKET_IN4ADDR_ANY               INADDR_ANY

#define OS_SOCKET_IN4ADDR_LOOPBACK          INADDR_LOOPBACK

#define OS_SOCKET_INET4_ADDRSTRLEN          INET_ADDRSTRLEN
typedef struct in_addr os_SocketIPv4Addr;
typedef struct ip_mreq os_SocketIPv4McastReq;
typedef struct sockaddr_in os_SocketIPv4SockAddr;

/****************************************************************************************
 *
 * IPv6 SPECIFIC DEFINITIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_AF_INET6                  AF_INET6
#define OS_SOCKET_IN6ADDR_ANY_INIT          IN6ADDR_ANY_INIT
#define OS_SOCKET_IN6ADDR_LOOPBACK_INIT     IN6ADDR_LOOPBACK_INIT
#define OS_SOCKET_INET6_ADDRSTRLEN          INET6_ADDRSTRLEN
typedef struct in6_addr os_SocketIPv6Addr;
typedef struct ipv6_mreq os_SocketIPv6McastReq;
typedef struct sockaddr_in6 os_SocketIPv6SockAddr;


/****************************************************************************************
 *
 * SOCKET LEVEL OPTIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_SOL_SOCKET                SOL_SOCKET
#define OS_SOCKET_SO_BROADCAST              SO_BROADCAST
#define OS_SOCKET_SO_DEBUG                  SO_DEBUG
#define OS_SOCKET_SO_DONTROUTE              SO_DONTROUTE
#define OS_SOCKET_SO_ERROR                  SO_ERROR
#define OS_SOCKET_SO_KEEPALIVE              SO_KEEPALIVE
#define OS_SOCKET_SO_LINGER                 SO_LINGER
#define OS_SOCKET_SO_OOBINLINE              SO_OOBINLINE
#define OS_SOCKET_SO_RCVBUF                 SO_RCVBUF
#define OS_SOCKET_SO_RCVLOWAT               SO_RCVLOWAT
#define OS_SOCKET_SO_RCVTIMEO               SO_RCVTIMEO
#define OS_SOCKET_SO_REUSEADDR              SO_REUSEADDR
#define OS_SOCKET_SO_SNDBUF                 SO_SNDBUF
#define OS_SOCKET_SO_SNDLOWAT               SO_SNDLOWAT
#define OS_SOCKET_SO_SNDTIMEO               SO_SNDTIMEO
#define OS_SOCKET_SO_TYPE                   SO_TYPE

/****************************************************************************************
 *
 * IPv4 LEVEL OPTIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_IPPROTO_IPV4              IPPROTO_IP
#define OS_SOCKET_IPPROTO_TCP               IPPROTO_TCP
#define OS_SOCKET_IPPROTO_UDP               IPPROTO_UDP
#define OS_SOCKET_IPV4_ADD_MEMBERSHIP       IP_ADD_MEMBERSHIP
#define OS_SOCKET_IPV4_DROP_MEMBERSHIP      IP_DROP_MEMBERSHIP
#define OS_SOCKET_IPV4_MULTICAST_IF         IP_MULTICAST_IF
#define OS_SOCKET_IPV4_MULTICAST_LOOP       IP_MULTICAST_LOOP
#define OS_SOCKET_IPV4_MULTICAST_TTL        IP_MULTICAST_TTL

/****************************************************************************************
 *
 * IPv6 LEVEL OPTIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_IPPROTO_IPV6              IPPROTO_IPV6
#define OS_SOCKET_IPV6_ADD_MEMBERSHIP       IPV6_ADD_MEMBERSHIP
#define OS_SOCKET_IPV6_DROP_MEMBERSHIP      IPV6_DROP_MEMBERSHIP
#define OS_SOCKET_IPV6_MULTICAST_IF         IPV6_MULTICAST_IF
#define OS_SOCKET_IPV6_MULTICAST_HOPS       IPV6_MULTICAST_HOPS
#define OS_SOCKET_IPV6_MULTICAST_LOOP       IPV6_MULTICAST_LOOP


/****************************************************************************************
 *
 * TCP LEVEL OPTIONS (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_SOCKET_TCP_NODELAY               TCP_NODELAY

/****************************************************************************************
 *
 * SOCKET ERROR CODES (See mpeos_socket.h for details)
 *
 ***************************************************************************************/

#define OS_MPE_SOCKET_EACCES                WSAEACCES
#define OS_MPE_SOCKET_EADDRINUSE            WSAEADDRINUSE
#define OS_MPE_SOCKET_EADDRNOTAVAIL         WSAEADDRNOTAVAIL
#define OS_MPE_SOCKET_EAFNOSUPPORT          WSAEAFNOSUPPORT
#define OS_MPE_SOCKET_EAGAIN                WSAEWOULDBLOCK
#define OS_MPE_SOCKET_EALREADY              WSAEALREADY
#define OS_MPE_SOCKET_EBADF                 WSAEBADF
#define OS_MPE_SOCKET_ECONNABORTED          WSAECONNABORTED
#define OS_MPE_SOCKET_ECONNREFUSED          WSAECONNREFUSED
#define OS_MPE_SOCKET_ECONNRESET            WSAECONNRESET
#define OS_MPE_SOCKET_EDESTADDRREQ          WSAEDESTADDRREQ
#define OS_MPE_SOCKET_EDOM                  0 // never returned by Windows
#define OS_MPE_SOCKET_EHOSTNOTFOUND         WSAHOST_NOT_FOUND
#define OS_MPE_SOCKET_EHOSTUNREACH          WSAEHOSTUNREACH
#define OS_MPE_SOCKET_EINTR                 WSAEINTR
#define OS_MPE_SOCKET_EINPROGRESS           WSAEINPROGRESS
#define OS_MPE_SOCKET_EIO                   0 // never returned by Windows
#define OS_MPE_SOCKET_EISCONN               WSAEISCONN
#define OS_MPE_SOCKET_ELOOP                 WSAELOOP
#define OS_MPE_SOCKET_EMFILE                WSAEMFILE
#define OS_MPE_SOCKET_EMSGSIZE              WSAEMSGSIZE
#define OS_MPE_SOCKET_ENAMETOOLONG          WSAENAMETOOLONG
#define OS_MPE_SOCKET_ENFILE                0 // never returned by Windows
#define OS_MPE_SOCKET_ENETDOWN              WSAENETDOWN
#define OS_MPE_SOCKET_ENETUNREACH           WSAENETUNREACH
#define OS_MPE_SOCKET_ENOBUFS               WSAENOBUFS
#define OS_MPE_SOCKET_ENOPROTOOPT           WSAENOPROTOOPT
#define OS_MPE_SOCKET_ENORECOVERY           WSANO_RECOVERY
#define OS_MPE_SOCKET_ENOSPC                0 // never returned by Windows
#define OS_MPE_SOCKET_ENOTCONN              WSAENOTCONN
#define OS_MPE_SOCKET_ENOTSOCK              WSAENOTSOCK
#define OS_MPE_SOCKET_EOPNOTSUPP            WSAEOPNOTSUPP
#define OS_MPE_SOCKET_EPIPE                 0 // never returned by Windows
#define OS_MPE_SOCKET_EPROTO                0 // never returned by Windows
#define OS_MPE_SOCKET_EPROTONOSUPPORT       WSAEPROTONOSUPPORT
#define OS_MPE_SOCKET_EPROTOTYPE            WSAEPROTOTYPE
#define OS_MPE_SOCKET_ETIMEDOUT             WSAETIMEDOUT
#define OS_MPE_SOCKET_ETRYAGAIN             WSATRY_AGAIN
#define OS_MPE_SOCKET_EWOULDBLOCK           WSAEWOULDBLOCK

/****************************************************************************************
 *
 * MAC ADDRESS FUNCTION
 *
 ***************************************************************************************/

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
int os_getMacAddress(char* displayName, char* buf);


/****************************************************************************************
 *
 * HN SUPPORT FUNCTIONS
 *
 ***************************************************************************************/

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
int os_getNetworkInterfaceType(char* displayName, int* type);

/**
 * Issues ICMP echo requests packets to the target host and returns results.
 * This api will block until the ping succeeds or fails.
 *
 * @param   testID     id associated with this test.
 * @param   host       name or address of host to ping. Must not be empty string.
 *                     If value is null or empty "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer( ie "null host string").
 * @param   reps       number of requests to send.
 *                     Value must be between MPE_SOCKET_PING_MIN_COUNT and
 *                     MPE_SOCKET_PING_MAX_COUNT. If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer( ie "reps out of range").
 * @param   interval   length of time in msec to wait between sending each request.
 *                     Value must be between MPE_SOCKET_PING_MIN_INTERVAL and
 *                     MPE_SOCKET_PING_MAX_INTERVAL. If value is not valid Error_other is
 *                     returned in status and additional detail may be put in
 *                     the info buffer.
 * @param   timeout    length of time in msec to wait for response to request.
 *                     Value must be between MPE_SOCKET_MIN_TIMEOUT and
 *                     MPE_SOCKET_MAX_TIMEOUT. If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
*                     the info buffer.
 * @param   timeout    length of time in msec to wait for response to request.
 *                     Value must be between MPE_SOCKET_MIN_TIMEOUT and
 *                     MPE_SOCKET_MAX_TIMEOUT. If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer.
* @param   blocksize  size of each packets data block in bytes. Value must be between
 *                     MPE_SOCKET_MIN_BLOCK_SIZE and
 *                     MPE_SOCKET_MAX_BLOCK_SIZE. If value is not valid
 *                     Error_other is returned in status and additional detail
 *                     may be put in the info buffer.
 * @param   dscp       Diff Serv Point Code value in IP header. Value must be between
 *                     MPE_SOCKET_MIN_DSCP and MPE_SOCKET_MAX_DSCP.
 *                     If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer.
 * @param   status     returned null terminated string copied into this
 *                     caller-allocated string array of length MPE_SOCKET_STATUS_SIZE
 *                     indicating overall success of test
 *                     must return allowed value for ping action
 *                     "Success","Error_CannotResolveHostName" and Error_Other
 *                     must be supported.
 *                     String is truncated if greater than MPE_SOCKET_STATUS_SIZE.
 * @param   info       returned null terminated free formatted string of length
 *                     MPE_SOCKET_ADDITIONAL_INFO_SIZE copied into
 *                     this caller-allocated string array that can contain
 *                     additional information about the test.
 *                     String is truncated if greater than
 *                     MPE_SOCKET_ADDITIONAL_INFO_SIZE.
 * @param   success    returned number of successful pings
 * @param   fails      returned number of failed pings
 * @param   avg        returned average response time in msec of successful pings
 *                     or 0 if there were none.
 * @param   min        returned minimum response time in msec of successful pings
 *                     or 0 if there were none.
 * @param   max        returned maximum response time in msec of successful pings
 *                     or 0 if there were none.
 *
 * @return 0 if successful, -1 if problems encountered
**/
int os_Ping(int testID, char* host, int reps, int interval, int timeout, int blocksize, int dscp, char *status, char *info, int *success, int *fails, int *avg, int *min, int *max);

/**
 * Issues an IP layer traceroute and returns results.
 * Traceroute issues a sequence of ICMP echo request packets to a target host
 * and determines the intermediate transversed.
 * This api blocks until the traceroute succeeds or fails.
 *
 * @param   testID     id associated with this test.
 * @param   host       name or address of host to probe. Must not be empty string.
 *                     If value is null or empty "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer( ie "null host string").
 * @param   hops       max number of hops(ie maximum time-to_live). Value must be between
 *                     MPE_SOCKET_TRACEROUTE_MIN_HOPS and MPE_SOCKET_TRACEROUTE_MAX_HOPS.
 *                     If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer.
 * @param   timeout    length of time in msec to wait for response to probe.
 *                     Value must be between MPE_SOCKET_MIN_TIMEOUT and
 *                     MPE_SOCKET_MAX_TIMEOUT.
 *                     If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer.
 * @param   blocksize  size of each packets data block in bytes. Value must be between
 *                     MPE_SOCKET_MIN_BLOCK_SIZE and
 *                     MPE_SOCKET_MAX_BLOCK_SIZE. If value is not valid
 *                     Error_other is returned in status and additional detail
 *                     may be put in the info buffer.
 * @param   dscp       Diff Serv Point Code value in IP header. Value must be between
 *                     MPE_SOCKET_MIN_DSCP and MPE_SOCKET_MAX_DSCP.
 *                     If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer.
 * @param   status     returned null terminated string copied into this
 *                     caller-allocated string array of length MPE_SOCKET_STATUS_SIZE
 *                     indicating overall success of test
 *                     must return allowed value for traceroute action
 *                     "Success","Error_CannotResolveHostName" and Error_Other
 *                     must be supported.
 *                     String is truncated if greater than MPE_SOCKET_STATUS_SIZE.
 * @param   info       returned null terminated free formatted string of length
 *                     MPE_SOCKET_ADDITIONAL_INFO_SIZE copied into
 *                     this caller-allocated string array that can contain
 *                     additional information about the test.
 *                     String is truncated if greater than
 *                     MPE_SOCKET_ADDITIONAL_INFO_SIZE.
 * @param   avgresp    returned average response time of probes in msec
 * @param   hophosts   null terminated string array copied into this caller-allocated
 *                     string array of length MPE_SOCKET_MAX_TRACEROUTE_HOSTS
 *                     Comma-separated list of host IP addresses
 *                     along the discovered route. If a host could not be contacted,
 *                     the corresponding entry in the list is empty, i.e.
 *                     there will be two consecutive
 *                     commas in the list, as in host1,,host3.
 *                     String is truncated if greater than
 *                     MPE_SOCKET_MAX_TRACEROUTE_HOSTS.
 *
 *
 * @return 0 if successful, -1 if problems encountered
 */
int os_Traceroute(int testID, char* host, int reps, int timeout, int blocksize, int dscp, char *status, char *info, int *avgresp, char *hophosts);

 /**
 * Issues an IP layer DNS lookup and returns results.
 * Queries a supplied DNS server for domain name and IP address
 * mappings for a target host.
 *
 * @param   testID     id associated with this test.
 * @param   host       name of host to lookup. Must not be empty string.
 *                     If name is not fully qualified current domain is used for
 *                     lookup.
 *                     If value is not valid(empty string). "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer( ie "null host string").
 * @param   server     name or address of server to use. An empty string denotes
 *                     using the default server. If name or address is not resolvable
 *                     "Error_DNSServerNotAvailable" is returned in the status buffer.
 * @param   timeout    length of time in msec to wait for response to probe.
 *                     Value must be between MPE_SOCKET_MIN_TIMEOUT and
 *                     MPE_SOCKET_MAX_TIMEOUT.
 *                     If value is not valid "Error_Other"
 *                     is returned in status buffer and additional detail is put in
 *                     the info buffer.
 * @param   status     returned null terminated string copied into this
 *                     caller-allocated string array of length MPE_SOCKET_STATUS_SIZE
 *                     indicating overall success of test
*                     must return allowed value for traceroute action
 *                     "Success","Error_CannotResolveHostName" and Error_Other
 *                     must be supported.
 *                     String is truncated if greater than MPE_SOCKET_STATUS_SIZE.
 * @param   info       returned null terminated free formatted string of length
 *                     MPE_SOCKET_ADDITIONAL_INFO_SIZE copied into
 *                     this caller-allocated string array that can contain
 *                     additional information about the test.
 *                     String is truncated if greater than
 *                     MPE_SOCKET_ADDITIONAL_INFO_SIZE.
 *                         MPE_SOCKET_ADDITIONAL_INFO_SIZE.
 * @param   resultAnswer   returned null terminated string of DNS answer type
 *                         copied into this caller-allocated string array of length
 *                         MPE_SOCKET_MAX_NSLOOKUP_ANSWER_RESULT_SIZE.
 *                         Valid values are "None", "Authoritative" or NonAuthoritative".
 * @param   resultName     returned null terminated string of DNS fully qualified name of host
 *                         copied into this caller-allocated string array of length
 *                         MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE.
 *                         String is truncated if greater than MPE_SOCKET_MAX_NSLOOKUP_NAME_RESULT_SIZE.
 * @param   resultIPS      returned null terminated string of DNS IP addresses resolved for host
 *                         copied into this caller-allocated string array of length
 *                         MPE_SOCKET_MAX_NSLOOKUP_IPS_RESULT_SIZE.
 *                         This is a comma-separated list if IP addresses returned by lookup or empty
 *                         string if none found.
 *                         String is truncated if greater than MPE_SOCKET_MAX_NSLOOKUP_IPS_RESULT_SIZE.
 * @param   resultServer   returned null terminated string of DNS Server IP address used 
 *                         copied into this caller-allocated string array of length
 *                         MPE_SOCKET_MAX_NSLOOKUP_SERVER_RESULT_SIZE.
 *                         String is truncated if greater than MPE_SOCKET_MAX_NSLOOKUP_IPS_RESULT_SIZE.
 * @param   resultTime     Response time of lookup in msec
 *
 * @return 0 if successful, -1 if problems encountered
 */
int os_NSLookup(int testID, char* host, char *server, int timeout, char *status, char *info, char *resultAnswer, char *resultName, char *resultIPS, char *resultServer, int *resultTime);

/**
 * Cancels Ping/Traceroute or NSLookup Test 
 *
 * @param   testID     id associated with this test.
 *
 * @return 0 if successful, -1 if problems encountered
 */
int os_CancelTest(int testID);

#ifdef __cplusplus
}
#endif

#endif /* _OS_SOCKET_H */
