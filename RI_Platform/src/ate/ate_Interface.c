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

#include <stdio.h>      /* for printf() and fprintf() */
#include <sys/types.h>
#include <stdlib.h>     /* for atoi() and exit() */
#include <string.h>     /* for memset() */
#include <time.h>

#include <net_utils.h>
#include <ri_config.h>

#include "ate_Interface.h"
#include "tuner/vlc_Tuner.h"

#define CHECK_SHUTDOWN() (ate.mShutdownInProgress)

// contain all the module data ate_Interface requires:
static struct Ate
{
    char mBindAddress[INET6_ADDRSTRLEN]; //Local host interface to bind
    char mSrcUrl[512];// ATE's StreamFile URL
    int mTelnetPort; //ATE's telnet listener port
    int mTelnetSocket; //ATE's telnet socket
    int mTelnetSd; //ATE's telnet socket descriptor
    //Telnet proposed options: Command: Will Echo, Command: Will Suppress Go Ahead
    unsigned char mProposeOptions[8];
    //Telnet expected options: Command: Do Echo, Command: Do Suppress Go Ahead
    unsigned char mExpectOptions[8];
    //Prompt string
    char mPrompt[8];
    GThread *telnet;
    // set when shutting down
    ri_bool mShutdownInProgress;
} ate;

static void setTelnetPort(int serverPort)
{
    ate.mTelnetPort = serverPort;
}
static gpointer threadError(const char *func, char *iface, char *addr, int sock)
{
    CLOSESOCK(sock);
    RILOG_ERROR("%s: %s(%s, %d) failed?\n", func, iface, addr, sock);
    return NULL;
}

/**
 * This class provides a dedicated telnet server that emulates a
 * subset of an IpSwitch IPS-400/IPS-400-CE Telnet
 * mode protocol.  Only the dialog currently expressed by the ATE is
 * modeled, since the sole purpose is to accept a virtual "STB Reboot"
 * command from the ATE.
 * @author dburt
 * @since 23 Dec, 08
 */

void ate_InterfaceInit(char *srvrIp, int srvrPort)
{
    char str[INET6_ADDRSTRLEN] = {0};
    char portStr[8] = {0};
    struct addrinfo hints = {0};
    struct addrinfo* srvrInfo = NULL;
    struct addrinfo* pSrvr = NULL;
    int yes = 1;
    int ret = 0;
#ifdef RI_WIN32_SOCKETS
    WSADATA wsd;
#endif

    RILOG_INFO("%s(%s, %d);\n", __FUNCTION__, srvrIp, srvrPort);
    ate.mShutdownInProgress = FALSE;

    if (0 > snprintf(ate.mBindAddress, sizeof(ate.mBindAddress), "%s", srvrIp))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }
    setTelnetPort(srvrPort);
    ate.mTelnetSocket = 0;

    //Load proposed options: Command: Will Echo, Command: Will Suppress Go Ahead
    ate.mProposeOptions[0] = 0xff;
    ate.mProposeOptions[1] = 0xfb;
    ate.mProposeOptions[2] = 0x01;
    ate.mProposeOptions[3] = 0xff;
    ate.mProposeOptions[4] = 0xfb;
    ate.mProposeOptions[5] = 0x03;

    //Load expected options: Command: Do Echo, Command: Do Suppress Go Ahead
    ate.mExpectOptions[0] = 0xff;
    ate.mExpectOptions[1] = 0xfd;
    ate.mExpectOptions[2] = 0x01;
    ate.mExpectOptions[3] = 0xff;
    ate.mExpectOptions[4] = 0xfd;
    ate.mExpectOptions[5] = 0x03;

    //Load prompt
    ate.mPrompt[0] = '\r';
    ate.mPrompt[1] = '\n';
    ate.mPrompt[2] = 'I';
    ate.mPrompt[3] = 'P';
    ate.mPrompt[4] = 'S';
    ate.mPrompt[5] = '>';
    ate.mPrompt[6] = ' ';

    //declare the server socket
#ifdef RI_WIN32_SOCKETS
    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    {
        RILOG_ERROR("%s WSAStartup() failed?\n", __FUNCTION__);
        return;
    }
#endif

    if (strstr(srvrIp, ":"))
    {
        hints.ai_family = AF_INET6;
    }
    else
    {
        hints.ai_family = AF_INET;
    }

    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;
    snprintf(portStr, sizeof(portStr), "%d", ate.mTelnetPort);

    if (0 != (ret = getaddrinfo(NULL, portStr, &hints, &srvrInfo)))
    {
        RILOG_ERROR("%s: getaddrinfo[%s]\n", __FUNCTION__, gai_strerror(ret));
        return;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        if (0 > (ate.mTelnetSocket = socket(pSrvr->ai_family,
                                            pSrvr->ai_socktype,
                                            pSrvr->ai_protocol)))
        {
            RILOG_WARN("%s socket() failed?\n", __FUNCTION__);
            continue;
        }

        if (0 > setsockopt(ate.mTelnetSocket, SOL_SOCKET, SO_REUSEADDR,
                           (char*) &yes, sizeof(yes)))
        {
            CLOSESOCK(ate.mTelnetSocket);
            RILOG_ERROR("%s setsockopt() failed?\n", __FUNCTION__);
            return;
        }

        if (0 > (bind(ate.mTelnetSocket, pSrvr->ai_addr, pSrvr->ai_addrlen)))
        {
            CLOSESOCK(ate.mTelnetSocket);
            RILOG_ERROR("%s bind() failed?\n", __FUNCTION__);
            continue;
        }

        // We successfully bound!
        break;
    }

    if (NULL == pSrvr)
    {
        RILOG_WARN("%s failed to bind\n", __FUNCTION__);
        freeaddrinfo(srvrInfo);
        return;
    }

    RILOG_INFO("%s bound on %s\n", __FUNCTION__, 
            net_ntop(pSrvr->ai_family, pSrvr->ai_addr, str, sizeof(str)));
    freeaddrinfo(srvrInfo);

#if 1
    (void) ate_TelnetThread(0);
#else
    if(NULL == (ate.telnet = g_thread_create(ate_TelnetThread, 0, FALSE,0)))
    {
        RILOG_ERROR("g_thread_create() returned NULL?!\n");
    }
#endif
}

/**
 * ate_InterfaceAbort
 */
void ate_InterfaceAbort(void)
{
    ate.mShutdownInProgress = TRUE;
    CLOSESOCK(ate.mTelnetSd);
    CLOSESOCK(ate.mTelnetSocket);
    RILOG_WARN("%s\n", __FUNCTION__);
}

int getRequest(int sock, char *rxBuf, int size)
{
    int bytesRcvd = 0;

    if (send(sock, ate.mPrompt, strlen(ate.mPrompt), 0) != strlen(ate.mPrompt))
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return 0;
    }

    if ((bytesRcvd = recv(sock, rxBuf, size - 1, 0)) <= 0)
    {
        RILOG_ERROR("%s recv() failed?\n", __FUNCTION__);
        return 0;
    }

    rxBuf[bytesRcvd] = 0;
    RILOG_DEBUG("%d = %s(%d);\n== %s\n", bytesRcvd, __FUNCTION__, sock, rxBuf);
    return bytesRcvd;
}

/**
 * ate_TelnetThread
 */
gpointer ate_TelnetThread(gpointer data)
{
    char str[INET6_ADDRSTRLEN] = {0};
    char *p, rxBuf[RCVBUFSIZE];
    struct sockaddr_storage their_addr; // connector's address information
    int plug = 1;
    int sin_size = 0;
    ri_bool quit = FALSE;

    RILOG_INFO("%s(%p); for %s:%d\n", __FUNCTION__, data, ate.mBindAddress,
            ate.mTelnetPort);

    if (listen(ate.mTelnetSocket, BACKLOG) == -1)
    {
        return threadError(__func__, "listen", ate.mBindAddress,
                ate.mTelnetSocket);
    }

    while (quit == FALSE)
    {
        if (CHECK_SHUTDOWN())
        {
            RILOG_ERROR("%s shutting down\n", __FUNCTION__);
            break;
        }

        RILOG_INFO("%s waiting for a connection...\n", __FUNCTION__);
        sin_size = sizeof their_addr;
        ate.mTelnetSd = accept(ate.mTelnetSocket,
                (struct sockaddr*) &their_addr, (socklen_t*) &sin_size);
        if (ate.mTelnetSd == -1)
        {
            RILOG_ERROR("%s accept(%d) failed?\n", __FUNCTION__,
                    ate.mTelnetSocket);
            continue;
        }

        RILOG_INFO("%s got connection from %s\n", __FUNCTION__,
                net_ntop(their_addr.ss_family, &their_addr, str, sizeof(str)));

        if (ate_ExchangeTelnetOptions(ate.mTelnetSd))
        {
            while (1)
            {
                if (CHECK_SHUTDOWN())
                {
                    break;
                }

                if (getRequest(ate.mTelnetSd, rxBuf, RCVBUFSIZE))
                {
                   // WARNING: process the TSP filename command first to prevent
                   // FALSE-positives on TSP filenames with the string '/XAIT'
                   // (e.g. blah/XAITSignaling/blah) 
                    if (NULL != (p = strstr(rxBuf, "/TSP-FILE")))
                    {
                        int tuner = 0;
                        p += 10;
                        ri_bool result = ate_ProcessTspFile(p, tuner);

                        if (!result)
                            RILOG_ERROR("ate_ProcessTspFile(%p) failed?\n", p);

                        if (!ate_TspFileReply(ate.mTelnetSd, result))
                            RILOG_ERROR("ate_TspFile(%d,%d) failed?\n",
                                    ate.mTelnetSd, result);
                        break;
                    }
                    else if (strstr(rxBuf, "/BOOT"))
                    {
                        sscanf(rxBuf, "/BOOT %d\n", &plug);
                        if (!ate_BootReply(ate.mTelnetSd, plug))
                            RILOG_ERROR("ate_BootReply(%d, %d) failed?\n",
                                    ate.mTelnetSd, plug);

                        ate_ProcessBoot(plug);
                        break;
                    }
                    else if (NULL != (p = strstr(rxBuf, "/XAIT")))
                    {
                        p += 6;
                        ri_bool result = ate_ProcessXait(p);

                        if (!result)
                            RILOG_ERROR("ate_ProcessXait(%p) failed?\n", p);

                        if (!ate_XaitReply(ate.mTelnetSd, result))
                            RILOG_ERROR("ate_XaitReply(%d,%d) failed?\n",
                                    ate.mTelnetSd, result);
                        break;
                    }
                    else if (NULL != (p = strstr(rxBuf, "/TESTNAME")))
                    {
                        p += 10;
                        ri_bool result = ate_ProcessTestName(p);

                        if (!result)
                            RILOG_ERROR("ate_ProcessTestName(%p) failed?\n", p);

                        if (!ate_TestNameReply(ate.mTelnetSd, result))
                            RILOG_ERROR("ate_TestName(%d,%d) failed?\n",
                                    ate.mTelnetSd, result);
                        break;
                    }
                    else if (strstr(rxBuf, "/H"))
                    {
                        ate_SendHelp(ate.mTelnetSd);
                        continue;
                    }
                    else if (strstr(rxBuf, "\n"))
                    {
                        ate_SendMenu(ate.mTelnetSd);
                        continue;
                    }
                    else
                    {
                        RILOG_WARN("%s - unrecognized: %s\n", __FUNCTION__,
                                rxBuf);
                        break;
                    }
                }
            }
        }

        CLOSESOCK(ate.mTelnetSd);
    }

    CLOSESOCK(ate.mTelnetSocket);
    RILOG_INFO("%s exiting...\n", __FUNCTION__);
    return NULL;
}

ri_bool ate_ExchangeTelnetOptions(int sock)
{
    unsigned char rxBuf[RCVBUFSIZE];
    int i;
    int bytesRcvd = 0;

    if (send(sock, (char *) ate.mProposeOptions, OPT_LEN, 0) != OPT_LEN)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return FALSE;
    }

    if ((bytesRcvd = recv(sock, (char *) rxBuf, RCVBUFSIZE - 1, 0)) <= 0)
    {
        RILOG_ERROR("%s recv() failed?\n", __FUNCTION__);
        return FALSE;
    }

    rxBuf[bytesRcvd] = 0;

    //Telnet expected options: Command: Do Echo, Command: Do Suppress Go Ahead
    for (i = 0; i < OPT_LEN; i++)
    {
        if (rxBuf[i] != ate.mExpectOptions[i])
        {
            RILOG_ERROR("Error in client option[%d] value:%02X != %02X\n", i,
                    rxBuf[i] & 0xFF, ate.mExpectOptions[i] & 0xFF);
            return FALSE;
        }
    }

    RILOG_DEBUG("Ate Reset available by Telnet on sock %d\n", ate.mTelnetSocket);
    return TRUE;
}

void ate_SendMenu(int sock)
{
    int len;

    len = strlen(MENU_TABLE);
    RILOG_INFO("%s: %d bytes, %s\n", __FUNCTION__, len, MENU_TABLE);

    if (send(sock, MENU_TABLE, len, 0) != len)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
    }
}

void ate_SendHelp(int sock)
{
    int len;

    len = strlen(HELP_STRING);
    RILOG_INFO("%s: %d bytes, %s\n", __FUNCTION__, len, HELP_STRING);

    if (send(sock, HELP_STRING, len, 0) != len)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
    }
}

ri_bool ate_BootReply(int sock, int tuner)
{
    int len;
    char reply[MAXURLLEN];

    if (0 > snprintf(reply, sizeof(reply), BOOT_REPLY, tuner, tuner, tuner))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }

    len = strlen(reply);
    RILOG_INFO("%s: %d bytes, %s\n", __FUNCTION__, len, reply);

    if (send(sock, reply, len, 0) != len)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

/**
 * Process boot message for tuner n
 * @param tuner - Number of tuner in mExec + 1
 */
void ate_ProcessBoot(int tuner)
{
    RILOG_INFO("%s(%d);\n", __FUNCTION__, tuner);
    ate_InterfaceAbort();
    exit(0);
}

unsigned char hexChar2bin(char nibble)
{
    if (nibble >= '0' && nibble <= '9')
        return nibble - '0';
    else if (nibble >= 'a' && nibble <= 'f')
        return (nibble - 'a') + 10;
    else if (nibble >= 'A' && nibble <= 'F')
        return (nibble - 'A') + 10;
    else
        RILOG_ERROR("%s conversion error for %02X\n", __FUNCTION__, nibble);
    return 0;
}

/**
 * Process XAIT message
 * @param msg buffer
 */
ri_bool ate_ProcessXait(char *msg)
{
    char *path, file[MAXURLLEN];
    unsigned char hiNib, loNib, binByte;
    FILE *fp = NULL;

    RILOG_DEBUG("%s - got: %s\n", __FUNCTION__, msg);

    if (NULL != (path = ricfg_getValue("RIPlatform",
            "RI.Headend.resources.directory")))
    {
        RILOG_DEBUG("%s got %s\n", __FUNCTION__, path);

        if (0 > snprintf(file, sizeof(file), "%s/fdcdata/Ate-XAIT.bin", path))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
        else if (NULL != (fp = fopen(file, "wb")))
        {
            while (*msg)
            {
                hiNib = *msg++;
                loNib = *msg++;
                binByte = (hexChar2bin(hiNib) << 4);
                binByte |= hexChar2bin(loNib);

                if ((hiNib != '\r') && (hiNib != '\n') && (loNib != '\r')
                        && (loNib != '\n'))
                    fputc(binByte, fp);
            }

            fclose(fp);
            return TRUE;
        }
    }

    return FALSE;
}

ri_bool ate_XaitReply(int sock, ri_bool result)
{
    int len;
    char reply[MAXURLLEN];

    if (result)
    {
        if (0 > snprintf(reply, sizeof(reply), "Accepted XAIT section"))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
    }
    else
    {
        if (0 > snprintf(reply, sizeof(reply), "Invalid XAIT data"))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
    }

    len = strlen(reply);
    RILOG_INFO("%s: %d bytes, %s\n", __FUNCTION__, len, reply);

    if (send(sock, reply, len, 0) != len)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

/**
 * Process TESTNAME message
 * @param msg buffer
 */
ri_bool ate_ProcessTestName(char *msg)
{
    RILOG_DEBUG("%s - got: %s\n", __FUNCTION__, msg);
    RILOG_WARN("running test: %s\n", msg);
    return TRUE;
}

ri_bool ate_TestNameReply(int sock, ri_bool result)
{
    int len;
    char reply[MAXURLLEN];

    if (result)
    {
        if (0 > snprintf(reply, sizeof(reply), "Successfully logged test name"))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
    }
    else
    {
        if (0 > snprintf(reply, sizeof(reply), "Invalid TESTNAME message"))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
    }

    len = strlen(reply);
    RILOG_INFO("%s: %d bytes, %s\n", __FUNCTION__, len, reply);

    if (send(sock, reply, len, 0) != len)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

/**
 * Process TSP-FILE message
 * @param msg buffer
 */
ri_bool ate_ProcessTspFile(char *msg, int tuner)
{
    int wrote = 0;
    FILE *fp = NULL;
    char* configPath;
    char filePath[MAXURLLEN];

    // Get the VLC locations from our config file
    if (NULL == (configPath = ricfg_getValue("RIPlatform",
            "RI.Headend.resources.directory")))
    {
        RILOG_ERROR("%s TS Player file not specified!\n", __func__);
        return FALSE;
    }

    if (0 > snprintf(filePath, sizeof(filePath), "%s/tsplayer-file.txt",
            configPath))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }

    if (NULL != (fp = fopen(filePath, "w")))
    {
        wrote = fwrite(msg, 1, strlen(msg), fp);
        fclose(fp);
        RILOG_INFO("%s wrote %d %s\n", __FUNCTION__, wrote, msg);
    }
    else
    {
        RILOG_ERROR("%s fwrite failed to write %s\n", __func__, msg);
        return FALSE;
    }

    return TRUE;
}

ri_bool ate_TspFileReply(int sock, ri_bool result)
{
    int len;
    char reply[MAXURLLEN];

    if (result)
    {
        if (0 > snprintf(reply, sizeof(reply), "wrote TS Player file"))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
    }
    else
    {
        if (0 > snprintf(reply, sizeof(reply), "Invalid TSP-FILE message"))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
    }

    len = strlen(reply);
    RILOG_INFO("%s: %d bytes, %s\n", __FUNCTION__, len, reply);

    if (send(sock, reply, len, 0) != len)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

