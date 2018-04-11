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
 * socket_appender.c
 *
 *  Created on: Feb 9, 2009
 *      Author: Mark Millard
 */

// Include system header files.
#include <stdio.h>
#include <string.h>

#include <log4c.h>

// Include Windows socket header files.
#ifdef WIN32
#define RI_WIN32_SOCKETS
#include <winsock2.h>
#else
#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>
typedef struct sockaddr SOCKADDR_IN;
typedef int SOCKET;
typedef int BOOL;
#define SO_MAX_MSG_SIZE (64*1024)
#define MAX_COMPUTERNAME_LENGTH 80
#define INVALID_SOCKET -1
#define FALSE 0
#endif

// Include RI Platform header files.
#include "socket_client.h"

/* Define SOCKET_CONF_DIR, the default location where socket.host should exist. */
#ifndef SOCKET_CONF_DIR
static const char *g_socket_conf_dir = ".";
#else
static const char *g_socket_conf_dir = SOCKET_CONF_DIR;
#endif
#define SOCKET_DGRAM_SIZE 4096

static BOOL g_initialized = FALSE;
//static const char *g_socket_ident;
static struct sockaddr_in g_sa_logger;

static SOCKET g_socket;

static char g_local_hostname[MAX_COMPUTERNAME_LENGTH + 1];
static char g_datagramm[SOCKET_DGRAM_SIZE];
static size_t g_datagramm_size;

/******************************************************************************
 * set_socket_conf_dir
 *
 * Set the configuration directory for the socket.host file.
 */
const char* set_socket_conf_dir(const char* dir)
{
    const char *ret = g_socket_conf_dir;
    g_socket_conf_dir = dir;
    return ret;
}

/******************************************************************************
 * init_logger_addr
 *
 * Read configuration file socket.host. This file should contain host address
 * and, optionally, port. Initialize sa_logger. If the configuration file does
 * not exist, use localhost:51400.
 * Returns: 0 - ok, -1 - error.
 */
static void init_logger_addr()
{
#ifdef WIN32
    char pathname[FILENAME_MAX];
    char *p;
    FILE *fd;
    char host[256];
    struct hostent * phe;

    memset(&g_sa_logger, 0, sizeof(SOCKADDR_IN));
    g_sa_logger.sin_family = AF_INET;

    if ('\\' == g_socket_conf_dir[0] || '/' == g_socket_conf_dir[0] || ':' == g_socket_conf_dir[1])
    {
        /* Absolute path. */
        strcpy(pathname, g_socket_conf_dir);
    }
    else
    {
        /* Relative path. */
        char *q;

        strcpy(pathname, __argv[0]);
        if (NULL != (p = strrchr(pathname, '\\')))
        p++;
        else
        goto use_default;

        if (NULL != (q = strrchr(pathname, '/')))
        q++;
        else
        goto use_default;

        if (p < q)
        *q = 0;
        else if (p > q)
        *p = 0;
        else
        pathname[0] = 0;
        strcat(pathname, g_socket_conf_dir);
    }
    p = &pathname[strlen(pathname) - 1];
    if ('\\' != *p && '/' != *p)
    {
        p++; *p = '/';
    }
    strcpy(++p, "socket.host");

    /* Read destination host name. */
    fd = fopen(pathname, "r");
    if (! fd)
    goto use_default;

    if (NULL == fgets(host, sizeof(host), fd))
    host[0] = 0;
    else
    {
        p = strchr(host, '\n');
        if (p)
        *p = 0;
        p = strchr(host, '\r');
        if (p)
        *p = 0;
    }
    fclose(fd);

    p = strchr(host, ':');
    if (p)
    *p++ = 0;

    phe = gethostbyname(host);
    if (! phe)
    goto use_default;

    memcpy(&g_sa_logger.sin_addr.s_addr, phe->h_addr, phe->h_length);

    if (p)
    g_sa_logger.sin_port = htons((unsigned short) strtoul(p, NULL, 0));
    else
    g_sa_logger.sin_port = htons(SOCKET_PORT);
    return;

    use_default:
    g_sa_logger.sin_addr.S_un.S_addr = htonl(0x7F000001);
#else
    g_sa_logger.sin_family = AF_INET;
    g_sa_logger.sin_addr.s_addr = inet_addr("127.0.0.1");
#endif
    g_sa_logger.sin_port = htons(SOCKET_PORT);
}

/******************************************************************************
 * socket_open_log
 *
 * Open connection to system logger.
 */
void socket_open_log(const char* ident, int option)
{
    int failed = 0;
#ifdef RI_WIN32_SOCKETS
    int wsa_initialized = 0;
    WSADATA wsd;
    int size;
#endif
    struct sockaddr_in sa_local;
    int n;

    if (g_initialized)
        return;

    // Parse options here if there are any.

#ifdef RI_WIN32_SOCKETS
    // Initialize windows sockets
    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    goto done;
    wsa_initialized = 1;
#endif

    // Get local host name
    n = sizeof(g_local_hostname);
    if (gethostname(g_local_hostname, n) == -1)
        goto done;

    g_socket = INVALID_SOCKET;

    init_logger_addr();

    for (n = 0;; n++)
    {
        g_socket = socket(AF_INET, SOCK_DGRAM, 0);
        if (INVALID_SOCKET == g_socket)
            goto done;

        memset(&sa_local, 0, sizeof(struct sockaddr_in));
        sa_local.sin_family = AF_INET;
        if (bind(g_socket, (struct sockaddr*) &sa_local,
                sizeof(struct sockaddr)) == 0)
            break;
#ifdef RI_WIN32_SOCKETS
        (void)closesocket(g_socket);
#else
        (void) close(g_socket);
#endif
        g_socket = INVALID_SOCKET;
        if (n == 100)
            goto done;
    }

    /* Get size of datagramm. */
#ifdef RI_WIN32_SOCKETS
    size = sizeof(g_datagramm_size);
    if (getsockopt(g_socket, SOL_SOCKET, SO_MAX_MSG_SIZE, (char*) &g_datagramm_size, &size))
    goto done;
    if (g_datagramm_size - strlen(g_local_hostname) - (ident ? strlen(ident) : 0) < 64)
    goto done;
    if (g_datagramm_size > sizeof(g_datagramm))
    g_datagramm_size = sizeof(g_datagramm);
#else
    g_datagramm_size = sizeof(g_datagramm);
#endif

    //g_socket_ident = ident;
    failed = 0;

    done: if (failed)
    {
#ifdef RI_WIN32_SOCKETS
        if (g_socket != INVALID_SOCKET) (void)closesocket(g_socket);
        if (wsa_initialized) WSACleanup();
#else
        if (g_socket != INVALID_SOCKET)
            (void) close(g_socket);
#endif
    }
    g_initialized = !failed;
}

/******************************************************************************
 * socket_close_log
 *
 * Close descriptor used to write to system logger.
 */
void socket_close_log()
{
    if (!g_initialized)
        return;
#ifdef RI_WIN32_SOCKETS
    (void)closesocket(g_socket);
    WSACleanup();
#else
    (void) close(g_socket);
#endif
    g_initialized = FALSE;
}

/******************************************************************************
 * socket_append_msg
 *
 * Generate a log message using FMT string and option arguments.
 */
void socket_append_msg(char* fmt, ...)
{
    va_list ap = NULL;

    va_start(ap, fmt);
    vsocket_append_msg(fmt, ap);
    va_end(ap);
}

/******************************************************************************
 * vsocket_append_msg
 *
 * Generate a log message using FMT and using arguments pointed to by AP.
 */
void vsocket_append_msg(char* fmt, va_list ap)
{
    //char *p;
    int num_chars;
    ssize_t numSent;

    if (!g_initialized)
        return;

    num_chars = vsnprintf(g_datagramm, g_datagramm_size, fmt, ap);
    g_datagramm[num_chars + 1] = '\0';
#if 0
    p = strchr(g_datagramm, '\n');
    if (p)
    *p = 0;
    p = strchr(g_datagramm, '\r');
    if (p)
    *p = 0;
#endif

    numSent = sendto(g_socket, g_datagramm, strlen(g_datagramm), 0,
            (struct sockaddr*) &g_sa_logger, sizeof(struct sockaddr));
    if (numSent < 0)
    {
        printf("***** Error occurred while sending log message. *****\n");
    }
}
