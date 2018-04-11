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


#include "net_utils.h"

#ifndef STANDALONE
#include "ri_log.h"

// Logging category
log4c_category_t* net_RILogCategory = NULL;

// Use Net category for logs in this file
#define RILOG_CATEGORY net_RILogCategory

#define CHECK_LOGGER()  \
{ \
    if (NULL == net_RILogCategory) \
    { \
        net_RILogCategory = log4c_category_get("RI.NET.UTILS"); \
    } \
}

#else
#define CHECK_LOGGER()
#endif


/**
 * This function returns the appropriate in_addr pointer based on the provided
 * addr_in structure
 *
 * @param struct in_addr or in_addr6 containing a network address based on
 *        the the address family
 * @return the in_addr address upon success; NULL is returned on error
 */
void* net_in_addr(struct sockaddr* sa)
{
    CHECK_LOGGER();

    if (AF_INET == sa->sa_family)
    {
        return &(((struct sockaddr_in*)sa)->sin_addr);
    }
    else if (AF_INET6 == sa->sa_family)
    {
        return &(((struct sockaddr_in6*)sa)->sin6_addr);
    }
    else
    {
        RILOG_ERROR("%s unknown AF_FAMILY (%d)?\n", __func__, sa->sa_family);
        return NULL;
    }
}

/**
 * This function returns the size of the provided addr_in structure
 *
 * @param struct in_addr or in_addr6 containing a network address based on
 *        the the address family
 * @return the size upon success; 0 is returned if there is an error
 */
int net_in_addr_len(struct sockaddr* sa)
{
    CHECK_LOGGER();

    if (AF_INET == sa->sa_family)
    {
        return sizeof(struct sockaddr_in);
    }
    else if (AF_INET6 == sa->sa_family)
    {
        return sizeof(struct sockaddr_in6);
    }
    else
    {
        RILOG_ERROR("%s unknown AF_FAMILY (%d)?\n", __func__, sa->sa_family);
        return 0;
    }
}

/**
 * This function returns the port from the provided addr_in structure
 *
 * @param struct in_addr or in_addr6 containing a network address based on
 *        the the address family
 * @return the port upon success; 0 is returned if there is an error
 */
unsigned short net_in_port(struct sockaddr* sa)
{
    CHECK_LOGGER();

    if (AF_INET == sa->sa_family)
    {
        return (((struct sockaddr_in*)sa)->sin_port);
    }
    else if (AF_INET6 == sa->sa_family)
    {
        return (((struct sockaddr_in6*)sa)->sin6_port);
    }
    else
    {
        RILOG_ERROR("%s unknown AF_FAMILY (%d)?\n", __func__, sa->sa_family);
        return 0;
    }
}

/**
 * This function converts the provided network address structure for the
 * provided address family into a string, then copies the string to the
 * provided destination.
 *
 * @param af address family, must be either AF_INET or AF_INET6
 * @param src points to a struct in_addr or in_addr6 containing a network
 *            address based on the the address family
 * @param dst points to the memory to be filled with a string representation
 *            of the aforementioned network address
 * @param length is the length of the provided buffer dst
 * @return non-NULL pointer to dst upon success; NULL is returned if there is
 *         an error and errno is set to indicate the error
 */
const char* net_ntop(int af, const void* src, char* dest, size_t length)
{
    CHECK_LOGGER();

#ifdef WIN32
    int ret = 0;
    DWORD dw_length = length;
    RILOG_INFO("%s getting result from WSAAddressToStringA()\n", __FUNCTION__);

    if (0 == (ret = WSAAddressToStringA((void*)src,
                                        sizeof(struct sockaddr_storage),
                                        NULL, dest, &dw_length)))
    {
        return (const char*)dest;
    }
    else
    {
        ret = WSAGetLastError();

        if (WSAEFAULT == ret)
        {
            RILOG_ERROR("%s buffer length %d is insufficient, need %ld\n",
                        __FUNCTION__, length, dw_length);
        }
        else
        {
            RILOG_ERROR("%s %d = WSAAddressToStringA()\n", __FUNCTION__, ret);
        }

        return (const char*)NULL;
    }
#else
    RILOG_INFO("%s getting result from inet_ntop()\n", __FUNCTION__);
    return inet_ntop(af, net_in_addr((struct sockaddr*)src), dest, length);
#endif
}

/**
 * This function converts the provided string into a network address structure
 * for the provided address family, then copies the network address structure
 * to the provided destination.
 *
 * @param af address family, must be either AF_INET or AF_INET6
 * @param src points to a character string containing a network address
 *            formatted appropriately for the address family
 * @param dst points to the memory to be filled with a struct in_addr or a
 *            struct in_addr6
 * @return 1 upon success; 0 is returned if src does not contain a string
 * representing a valid network address in the specified address family;
 * -1 is returned if af does not contain a valid address family and errno
 *  is set to EAFNOSUPPORT.
 */
int net_pton(int af, char* src, void* dest)
{
    CHECK_LOGGER();

#ifdef WIN32
    int length = (af == AF_INET?  sizeof(struct sockaddr_in) :
                                  sizeof(struct sockaddr_in6));
    RILOG_INFO("%s getting result from WSAStringToAddressA()\n", __FUNCTION__);
    return WSAStringToAddressA(src, af, 0, dest, &length);
#else
    RILOG_INFO("%s getting result from inet_pton()\n", __FUNCTION__);
    return inet_pton(af, src, dest);
#endif
}

