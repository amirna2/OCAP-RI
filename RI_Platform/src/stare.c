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




#include <stdlib.h>
#include <string.h>
#include <glib.h>
#include <inttypes.h>
#include <platform.h>
#include <ri_log.h>

#ifdef WIN32
#define RI_WIN32_SOCKETS
#include <winsock2.h>
#include <ws2tcpip.h>
#define CLOSESOCK(s) (void)closesocket(s)
#else
#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>
#define CLOSESOCK(s) (void)close(s)
#endif

#include <glib.h>


#include "stare.h"

// Logging category
log4c_category_t* eyes_RILogCategory = NULL;

// Use VLC category for logs in this file
#define RILOG_CATEGORY eyes_RILogCategory


static ri_bool socketConnect(char *pIp, int nPort, int *pnSocket);
static ri_bool exchangeHello (int nSocket, unsigned char uProtocolVersion, char *pUser);
static ri_bool sendMsg (int nSocket, unsigned char *pMsg, int nMsgSz);
static ri_bool recvMsg (int nSocket, unsigned char **ppMsg, int *pnMsgSz);
static ri_bool recvBytes (int nSocket, unsigned char *pBytes, int nBytesSz);
static void socketCleanup();
static ri_bool processMsg (unsigned char *pMsg, int nMsgSz);
static ri_bool formatSTAREMsg(unsigned char uMessageType, unsigned short uPayloadLength, 
    unsigned char *pPayload, unsigned char **ppMsg, int *pnMsgSz);
static ri_bool parseSTAREMsg(unsigned char *pMsg, unsigned char * puMessageType, unsigned short *puPayloadLength, 
    unsigned char **ppPayload);
static ri_bool formatHelloMsg(unsigned char uProtocolVersion, char *pUser, unsigned char **ppMsg, int *pnMsgSz);
static gpointer eyesCommThread(gpointer data);


#define STARE_PACKET     0x01
#define STARE_SECTION    0x02
#define STARE_HELO       0xff
#define STARE_FULL       0xfe
#define STARE_USERS      0xfd
#define STARE_NOSUPP     0xfc
#define STARE_PACKETLOSS 0xfb
#define STARE_REBOOT     0xfa




// contain all the module data test_Interface requires:
static struct s_eyesData
{
    ri_bool quit;

    char pSvrIp[32];   // GORP: dynamically allocate this?
    int nSvrPort;
    unsigned char uProtocolVersion;
    char pUser[32];  // GORP: dynamically allocate this?

    int nSocket;
    GThread *pThread;
} g_eyesData;


void terminateEYEsReceiver()
{
    g_eyesData.quit = TRUE;
    socketCleanup();
}

ri_bool initEYEsReceiver(char *pSvrIP, int nSvrPort, unsigned char uProtocolVersion, char *pUser)
{
    // Create our logging category
    if (NULL == eyes_RILogCategory)
    {
        eyes_RILogCategory = log4c_category_get("RI.EYES");
    }
        
    RILOG_INFO("Initing eyesCommThread\n");

    g_eyesData.quit = FALSE;
    strcpy (g_eyesData.pSvrIp, pSvrIP);
    g_eyesData.nSvrPort = nSvrPort;
    g_eyesData.uProtocolVersion = uProtocolVersion;
    strcpy (g_eyesData.pUser, pUser);


    // start thread
    if (NULL == (g_eyesData.pThread = g_thread_create(eyesCommThread, 0, FALSE, 0)))
    {
        RILOG_ERROR("g_thread_create() for eyesCommThread returned NULL\n");
    }

    return TRUE;
}

static gpointer eyesCommThread(gpointer data)
{
    unsigned char *pMsg; 
    int nMsgSz;

    ri_bool bReturn;

    RILOG_INFO("Inside eyesCommThread\n");

    while (g_eyesData.quit == FALSE)
    {

        // open socket connection to EYEs server
        bReturn = socketConnect(g_eyesData.pSvrIp, g_eyesData.nSvrPort, &g_eyesData.nSocket);
        if (!bReturn)
        {
            RILOG_ERROR("%s Error opening socket connection\n", __FUNCTION__);
            socketCleanup();
            g_usleep(10000000L); // sleep 10 seconds before trying again
            continue; 
        }
    
        RILOG_INFO("Socket connect successful\n");

        // exchange hello msg to regiser with EYEs server
        bReturn = exchangeHello(g_eyesData.nSocket, g_eyesData.uProtocolVersion, g_eyesData.pUser);
        if (!bReturn)
        {
            RILOG_ERROR("%s Error exchanging EYEs hello\n", __FUNCTION__);
            socketCleanup();
            g_usleep(10000000L); // sleep 10 seconds before trying again
            continue;
        }

        RILOG_INFO("Hello exchange successful\n");

        while(1)
        {
            bReturn = recvMsg (g_eyesData.nSocket, &pMsg, &nMsgSz);
            if (!bReturn)
            {
                RILOG_ERROR("%s Error receiving msg\n", __FUNCTION__);
                break;
            }

            bReturn = processMsg (pMsg, nMsgSz);
            if (!bReturn)
            {
                RILOG_ERROR("%s Error processing msg\n", __FUNCTION__);
                break;
            }
        }

        socketCleanup();

    }

    RILOG_INFO("Leaving eyesCommThread\n");
    return NULL;

}

/**
 * A StareMessage represents a message in the STARE protocol
 * 
 * A message is formatted as follows
 * 
 * stare_message() {
 *     message_type                       1 byte    uimsbf
 *     length                             2 bytes   uimsbf 
 *     for(i=0;i<length;i++) {
 *         payload_byte                   1 byte    uimsbf
 *     }
 * }
 */
static ri_bool formatSTAREMsg(unsigned char uMessageType, unsigned short uPayloadLength, 
                               unsigned char *pPayload, unsigned char **ppMsg, int *pnMsgSz)
{
    unsigned short uPayloadLengthTemp;

    *pnMsgSz = uPayloadLength + 3;
    *ppMsg = (unsigned char *) g_try_malloc (uPayloadLength + 3);
    if (*ppMsg == NULL)
    {
        return FALSE;
    }

    *ppMsg[0] = uMessageType;
    uPayloadLengthTemp = htons (uPayloadLength);
    memcpy (*ppMsg + 1, &uPayloadLengthTemp, 2);
    memcpy (*ppMsg + 3, pPayload, uPayloadLength);

    return TRUE;
}

static ri_bool parseSTAREMsg(unsigned char *pMsg, unsigned char *puMessageType, unsigned short *puPayloadLength, 
                               unsigned char **ppPayload)
{
    *puMessageType = pMsg[0];
    memcpy (puPayloadLength, &(pMsg[1]), 2);
    *puPayloadLength = ntohs(*puPayloadLength);

    *ppPayload = (unsigned char *) g_try_malloc (*puPayloadLength);
    if (*ppPayload == NULL)
    {
        return FALSE;
    }

    memcpy (*ppPayload, &(pMsg[3]), *puPayloadLength);

    return TRUE;
}

static ri_bool formatHelloMsg(unsigned char uProtocolVersion, char *pUser, unsigned char **ppMsg, int *pnMsgSz)
{
    unsigned short uPayloadLength;
    unsigned char *pPayload;
    unsigned char uMessageType = 0xFF;
    ri_bool bReturn;

    uPayloadLength = strlen(pUser) + 1;
    
    pPayload = (unsigned char *) g_try_malloc (uPayloadLength);
    if (pPayload == NULL)
    {
        return FALSE;
    }

    pPayload[0] = uProtocolVersion;
    memcpy (pPayload + 1, pUser, strlen(pUser));

    bReturn = formatSTAREMsg(uMessageType, uPayloadLength, pPayload, ppMsg, pnMsgSz);
    g_free (pPayload);

    return bReturn;
}

static ri_bool exchangeHello (int nSocket, unsigned char uProtocolVersion, char *pUser)
{
    unsigned char *pMsg = 0;
    int nMsgSz = 0;
    ri_bool bReturn;

    unsigned char uMessageType;
    unsigned short uPayloadLength;
    unsigned char *pPayload;

    bReturn = formatHelloMsg(uProtocolVersion, pUser, &pMsg, &nMsgSz);
    if (!bReturn)
    {
        RILOG_ERROR("%s Error formatting EYEs hello msg\n", __FUNCTION__);
        return FALSE;
    }

    // send hello msg
    bReturn = sendMsg (nSocket, pMsg, nMsgSz);
    if (!bReturn)
    {
        RILOG_ERROR("%s Error sending EYEs hello msg\n", __FUNCTION__);
        return FALSE;
    }

    // receive hello reply
    bReturn = recvMsg (nSocket, &pMsg, &nMsgSz);
    if (!bReturn)
    {
        RILOG_ERROR("%s Error receiving EYEs hello response\n", __FUNCTION__);
        return FALSE;
    }

    // parse hello response
    bReturn = parseSTAREMsg(pMsg, &uMessageType, &uPayloadLength, &pPayload);
    if (!bReturn)
    {
        RILOG_ERROR("%s Error parsing EYEs hello response\n", __FUNCTION__);
        return FALSE;
    }

    // validate response
    RILOG_INFO("uMessageType = %d\n", uMessageType);
    if (uMessageType == STARE_FULL)
    {
        RILOG_ERROR("%s STARE server returned STARE_FULL for user -- %s\n", __FUNCTION__, g_eyesData.pUser);
        return FALSE;
    }
    else if (uMessageType == STARE_NOSUPP)
    {
        RILOG_ERROR("%s STARE server returned STARE_NOSUPP for user -- %s\n", __FUNCTION__, g_eyesData.pUser);
        return FALSE;
    }
    else if (uMessageType != STARE_HELO)
    {
        RILOG_ERROR("%s STARE server returned unexpected message\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

static ri_bool sendMsg (int nSocket, unsigned char *pMsg, int nMsgSz)
{
    if (send(nSocket, pMsg, nMsgSz, 0) != nMsgSz)
    {
        RILOG_ERROR("%s EYEs send failed\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

static ri_bool recvMsg (int nSocket, unsigned char **ppMsg, int *pnMsgSz)
{
    int nPayloadSz = 0;
    unsigned char pMsgHdr[3];
    ri_bool bReturn;

    // first receive header and payload length
    bReturn = recvBytes (nSocket, pMsgHdr, 3);
    if (!bReturn)
    {
        RILOG_ERROR("%s Error receiving hdr bytes\n", __FUNCTION__);
        return FALSE;
    }
            
    RILOG_INFO("Header receive successful: %x %x %x\n", pMsgHdr[0], pMsgHdr[1], pMsgHdr[2]);

    memcpy (&nPayloadSz, &(pMsgHdr[1]), 2);
    nPayloadSz = ntohs(nPayloadSz);

    RILOG_INFO("nPayloadSz = %d\n", nPayloadSz);

    *pnMsgSz = nPayloadSz + 3;
    *ppMsg = (unsigned char *) g_try_malloc (*pnMsgSz);
    if (*ppMsg == NULL)
    {
        return FALSE;
    }

    memcpy (*ppMsg, pMsgHdr, 3);

    // next, receive payload
    bReturn = recvBytes (nSocket, *ppMsg + 3, nPayloadSz);
    if (!bReturn)
    {
        RILOG_ERROR("%s Error receiving payload bytes\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

static ri_bool recvBytes (int nSocket, unsigned char *pBytes, int nBytesSz)
{
    int numBytesTotal = 0;
    int numBytes = 0;

    // GORP: does recv block??
    while (numBytesTotal < nBytesSz)
    {
        // Receive up to the buffer size
        if ((numBytes = recv(nSocket, pBytes+numBytesTotal, nBytesSz-numBytesTotal, 0)) <= 0)
        {
            RILOG_ERROR("%s EYEs recv() failed\n", __FUNCTION__);
            return FALSE;
        }

        numBytesTotal += numBytes;
        RILOG_INFO("received %d bytes\n", numBytes);
    }

    return TRUE;
}

static ri_bool processMsg (unsigned char *pMsg, int nMsgSz)
{
    int i;
    for (i=0; i<nMsgSz; i++)
    {
        RILOG_INFO("Processing msg: byte %d: %x\n", i, pMsg[i]);
    }

    // GORP: fill in

    return TRUE;
}

static void socketCleanup()
{
    if (g_eyesData.nSocket != 0)
    {
        CLOSESOCK(g_eyesData.nSocket);
    }
    g_eyesData.nSocket = 0;
}

static ri_bool socketConnect(char *pIp, int nPort, int *pnSocket)
{
    struct sockaddr_in srvrAddr;

#ifdef RI_WIN32_SOCKETS
    WSADATA wsd;
#endif

    RILOG_DEBUG("%s; to %s:%d\n", __FUNCTION__, pIp, nPort);

#ifdef RI_WIN32_SOCKETS
    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    {
        RILOG_ERROR("%s WSAStartup() failed?\n", __FUNCTION__);
        return FALSE;
    }
#endif
    if ((*pnSocket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP)) < 0)
    {
        RILOG_ERROR("%s socket() failed?\n", __FUNCTION__);
        return FALSE;
    }

    memset(&srvrAddr, 0, sizeof(srvrAddr));
    srvrAddr.sin_family = AF_INET;
    srvrAddr.sin_addr.s_addr = inet_addr(pIp);
    srvrAddr.sin_port = htons(nPort);

    if (connect(*pnSocket, (struct sockaddr *) &srvrAddr, sizeof(srvrAddr)) < 0)
    {
        RILOG_WARN("%s connect() failed?\n", __FUNCTION__);
        return FALSE;
    }

    return TRUE;
}

