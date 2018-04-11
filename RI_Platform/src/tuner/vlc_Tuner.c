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


#include <platform.h>
#include <net_utils.h>
#include <ri_log.h>

#include "tuner.h"
#include "vlc_Tuner.h"

// Logging category
log4c_category_t* vlc_RILogCategory = NULL;

// Use VLC category for logs in this file
#define RILOG_CATEGORY vlc_RILogCategory

#define CHECK_INITIALIZATION(index) (tuner[index].mInitialized)

// contain all the module data vlc_Tuner requires:
static struct Tuner
{
    // streamer file URL
    char mSrcUrl[MAXURLLEN];
    // streamer control telnet IP
    char mTelnetIp[INET6_ADDRSTRLEN];
    // streamer control telnet port
    int mTelnetPort;
    // streamer control telnet socket
    int mTelnetSocket;
    // streamer control telnet password
    char mTelnetPassword[80];
    // streamer control telnet RX buffer
    char mTelnetRxBuf[RCVBUFSIZE];
    // streamer Launch command line
    char mServerCmd[1024];
    // set when streamer is active
    ri_bool mStreaming;
    // set when initialized
    ri_bool mInitialized;
    // Adapter serving this stream
    char mAdapterName[80];
#ifdef WIN32
    // process info for VLC telnet service
    PROCESS_INFORMATION mProcessInfo;
    // environment info for VLC telnet service
    STARTUPINFO mStartupInfo;
#else
    pid_t mPid;
#endif
} tuner[MAX_TUNERS];


/**
 * Connects to the VLC server using telnet.
 * @throws IOException
 *             Connection aborted.
 */
static ri_bool telnetConnect(int index)
{
    char str[INET6_ADDRSTRLEN] = {0};
    char portStr[8] = {0};
    char *rxBuf = tuner[index].mTelnetRxBuf;
    struct addrinfo hints = {0};
    struct addrinfo* srvrInfo = NULL;
    struct addrinfo* pSrvr = NULL;
    int bytes = 0;
    int ret = 0;
#ifdef RI_WIN32_SOCKETS
    WSADATA wsd;
#endif

    RILOG_DEBUG("%s(%d); to %s:%d\n", __FUNCTION__, index,
            tuner[index].mTelnetIp, tuner[index].mTelnetPort);
    if (!CHECK_INITIALIZATION(index))
        return FALSE;

#ifdef RI_WIN32_SOCKETS
    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    {
        RILOG_ERROR("t%d %s WSAStartup() failed?\n", index, __FUNCTION__);
        return FALSE;
    }
#endif

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    snprintf(portStr, sizeof(portStr), "%d", tuner[index].mTelnetPort);

    if (0 != (ret = getaddrinfo(tuner[index].mTelnetIp,
                                portStr, &hints, &srvrInfo)))
    {
        RILOG_ERROR("t%d %s: getaddrinfo[%s]\n", index, __FUNCTION__,
                             gai_strerror(ret));
        return FALSE;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        if (0 > (tuner[index].mTelnetSocket = socket(pSrvr->ai_family,
                                                     pSrvr->ai_socktype,
                                                     pSrvr->ai_protocol)))
        {
            RILOG_WARN("t%d %s socket() failed?\n", index, __FUNCTION__);
            continue;
        }

        if (0 > (connect(tuner[index].mTelnetSocket, pSrvr->ai_addr,
                                                     pSrvr->ai_addrlen)))
        {
            RILOG_WARN("t%d %s connect() failed?\n", index, __FUNCTION__);
            continue;
        }

        // We successfully connected!
        break;
    }

    if (NULL == pSrvr)
    {
        RILOG_WARN("t%d %s failed to connect to VLC\n", index, __FUNCTION__);
        return FALSE;
    }
    else
    {
        net_ntop(pSrvr->ai_family, pSrvr->ai_addr, str, sizeof(str));
        RILOG_INFO("t%d %s connected to VLC on %s\n", index, __FUNCTION__, str);
        freeaddrinfo(srvrInfo);
    }

    // Receive up to the buffer size (minus 1 to leave space null terminator)
    if ((bytes = recv(tuner[index].mTelnetSocket, rxBuf, RCVBUFSIZE-1, 0)) <= 0)
    {
        RILOG_ERROR("t%d %s recv() failed?\n", index, __FUNCTION__);
        return FALSE;
    }

    rxBuf[bytes] = '\0'; /* Terminate the string! */
    RILOG_DEBUG("t%d %s: %s\n", index, __FUNCTION__, rxBuf);
    bytes = strlen(tuner[index].mTelnetPassword);

    if (send(tuner[index].mTelnetSocket, tuner[index].mTelnetPassword,
            bytes, 0) != bytes)
    {
        RILOG_ERROR("t%d %s send() failed?\n", index, __FUNCTION__);
        return FALSE;
    }

    if ((bytes = recv(tuner[index].mTelnetSocket, rxBuf, RCVBUFSIZE-1, 0)) <= 0)
    {
        RILOG_ERROR("t%d %s recv() failed?\n", index, __FUNCTION__);
        return FALSE;
    }

    rxBuf[bytes] = '\0'; /* Terminate the string! */
    RILOG_DEBUG("t%d %s: %s\n", index, __FUNCTION__, rxBuf);
    return TRUE;
}

/**
 * Closes the telnet connection.
 */
static void telnetClose(int index)
{
    RILOG_DEBUG("%s(%d); to %s:%d\n", __FUNCTION__, index,
            tuner[index].mTelnetIp, tuner[index].mTelnetPort);
    CLOSESOCK(tuner[index].mTelnetSocket);
#ifdef RI_WIN32_SOCKETS
    WSACleanup();
#endif
}

/**
 * Sends a command to to VLC server.
 * @param command: the command to send
 */
static char *telnetCommand(int index, char *command)
{
    char *rxBuf = tuner[index].mTelnetRxBuf;
    char txBuf[512];
    int commandLen;
    int bytes = 0;
    RILOG_DEBUG("%s(%d, %s); to %s:%d\n", __FUNCTION__, index, command,
            tuner[index].mTelnetIp, tuner[index].mTelnetPort);

    if (!CHECK_INITIALIZATION(index))
        return "ERROR - tuner not initialized!?";

    if (0 > snprintf(txBuf, sizeof(txBuf), "%s\n", command))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
        return "ERROR in snprintf";
    }

    commandLen = strlen(txBuf);

    if (send(tuner[index].mTelnetSocket, txBuf, commandLen, 0) != commandLen)
    {
        RILOG_ERROR("t%d %s send() failed?\n", index, __FUNCTION__);
        return "ERROR in send";
    }

    // Receive up to the buffer size (minus 1 to leave space null terminator)
    if ((bytes = recv(tuner[index].mTelnetSocket, rxBuf, RCVBUFSIZE - 1, 0))
            > 0)
    {
        rxBuf[bytes] = '\0'; /* Terminate the string! */
        RILOG_DEBUG("t%d %s result[%d]: %s\n", index, __FUNCTION__, bytes,
                rxBuf);
        return rxBuf;
    }

    return "ERROR in recv";
}

#ifdef WIN32
void getErrorString(unsigned long error, char *message, int length)
{
    LPVOID lpMsgBuf;

    if (FormatMessage(FORMAT_MESSAGE_ALLOCATE_BUFFER |
                    FORMAT_MESSAGE_FROM_SYSTEM |
                    FORMAT_MESSAGE_IGNORE_INSERTS,
                    NULL, error, 0, (LPTSTR) &lpMsgBuf, 0, NULL) == 0)
    {
        sprintf(message, "%s- Unknown Error (%ld)", __FUNCTION__, error);
        lpMsgBuf = NULL;
    }
    else
    {
        memcpy(message, (char *)lpMsgBuf, length);
    }

    if (lpMsgBuf)
    (void)LocalFree(lpMsgBuf);
}
#endif

/**
 * Kill the standalone VLC subprocess
 */
static ri_bool killVlcExe(int index)
{
    RILOG_DEBUG("%s(%d);\n", __FUNCTION__, index);

#ifdef WIN32
    if (tuner[index].mProcessInfo.hThread && tuner[index].mProcessInfo.hProcess)
    {
        long e;
        char eMsg[80];
        RILOG_INFO("Killing existing t[%p], p[%p]\n",
                tuner[index].mProcessInfo.hThread,
                tuner[index].mProcessInfo.hProcess);
        if (!CloseHandle(tuner[index].mProcessInfo.hProcess))
        {
            e = GetLastError();
            getErrorString(e, eMsg, 79);
            RILOG_ERROR("%ld = CloseHandle(hProcess) ERROR = %s\n", e, eMsg);
        }
        else if (!CloseHandle(tuner[index].mProcessInfo.hThread))
        {
            e = GetLastError();
            getErrorString(e, eMsg, 79);
            RILOG_ERROR("%ld = CloseHandle(hThread) ERROR = %s\n", e, eMsg);
        }
        else
        {
            tuner[index].mProcessInfo.hThread = 0;
            tuner[index].mProcessInfo.hProcess = 0;
            return TRUE;
        }
    }
#else
    if (tuner[index].mPid)
    {
        RILOG_INFO("Killing existing pid[%x]\n", tuner[index].mPid);

        if (kill(tuner[index].mPid, SIGTERM))
        {
            char *eMsg = strerror(errno);
            RILOG_ERROR("%d = kill(mPid) ERROR\n== %s\n", errno, eMsg);
        }
        else
        {
            tuner[index].mPid = 0;
            return TRUE;
        }
    }
#endif
    return FALSE;
}

/**
 * Create the standalone VLC subprocess
 * @param serverCmd - command line to create VLC server in subprocess
 * @param destIp -
 * @param destPort -
 */
static ri_bool launchVlcExe(int index, char *serverCmd, char *destIp, int destPort)
{
    char tsCmd[512];
    RILOG_DEBUG("%s(%d, %s, %d);\n", __FUNCTION__, index, destIp, destPort);

#ifdef WIN32
    if (tuner[index].mProcessInfo.hThread && tuner[index].mProcessInfo.hProcess)
    {
        RILOG_INFO("Using existing t[%p], p[%p]\n",
                tuner[index].mProcessInfo.hThread,
                tuner[index].mProcessInfo.hProcess);
        return TRUE;
    }

    // Replace %s:%d with dest ip and port
    if (0 > snprintf(tsCmd, sizeof(tsCmd), serverCmd, "Win32",
                     tuner[index].mTelnetIp, tuner[index].mTelnetPort,
                     destIp, destPort))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
        return FALSE;
    }

    RILOG_INFO("%s - tsCmd:%s\n", __func__, tsCmd);

    if (CreateProcess(NULL, tsCmd, NULL, NULL, FALSE, 0, NULL, NULL,
                    &tuner[index].mStartupInfo, &tuner[index].mProcessInfo))
    {
        RILOG_INFO("CreateProcess(%s) returned t[%p], p[%p]\n", tsCmd,
                tuner[index].mProcessInfo.hThread,
                tuner[index].mProcessInfo.hProcess);
        return TRUE;
    }
    else
    {
        long e = GetLastError();
        char eMsg[80];
        getErrorString(e, eMsg, 79);
        RILOG_ERROR("%ld = CreateProcess(%s) ERROR\n== %s\n", e, tsCmd, eMsg);
    }
#else
#define NUM_VLC_ARGS 8
    int i;
    char *args;
    char *pArg[NUM_VLC_ARGS + 1];

    if (tuner[index].mPid)
    {
        RILOG_INFO("Using existing pid[%x]\n", tuner[index].mPid);
        return TRUE;
    }

    // Replace %s:%d with dest ip and port
    if (0 > snprintf(tsCmd, sizeof(tsCmd), serverCmd, "Linux",
                     tuner[index].mTelnetIp, tuner[index].mTelnetPort,
                     destIp, destPort))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
        return FALSE;
    }

    RILOG_INFO("%s - tsCmd:%s\n", __func__, tsCmd);

    // split the command string for execv
    args = g_strdup(tsCmd);
    pArg[0] = strtok(args, " ");
    pArg[NUM_VLC_ARGS] = NULL;
    for (i = 1; i < NUM_VLC_ARGS; i++)
        pArg[i] = strtok(NULL, " ");

    // create the child process for execv of VLC
    tuner[index].mPid = fork();

    if (0 > tuner[index].mPid)
    {
        char *eMsg = strerror(errno);
        RILOG_ERROR("%d = fork(%s) ERROR\n== %s\n", errno, tsCmd, eMsg);
        tuner[index].mPid = 0;
        g_free(args);
        return FALSE;
    }
    else if (0 == tuner[index].mPid)
    {
        char *path, file[512];

        path = ricfg_getValue("RIPlatform", "RI.Platform.SnapshotDir");
        sprintf(file, "%svlc-stderr.log", path);
        RILOG_INFO("%s VLC process redirecting STDERR to %s\n", __func__, file);
        if (freopen(file, "w", stderr) == NULL)
        {
            char *eMsg = strerror(errno);
            RILOG_ERROR("%d = freopen(%s) ERROR\n== %s\n", errno, file, eMsg);
            tuner[index].mPid = 0;
            g_free(args);
            return FALSE;
        }

        if (0 > (execv(pArg[0], pArg)))
        {
            char *eMsg = strerror(errno);
            RILOG_ERROR("%d = execv(%s) ERROR\n== %s\n", errno, tsCmd, eMsg);
            tuner[index].mPid = 0;
            g_free(args);
            return FALSE;
        }
    }
    else
    {
        RILOG_INFO("fork(%s) returned pid[%d]\n", tsCmd, tuner[index].mPid);
    }

    g_free(args);
#endif
    return TRUE;
}

void vlc_TunerInit(int index, char *srvrIp, int srvrPort, char *passwd)
{
    char *path, vlcPath[1024];
    char buf[512];
    char *ipAddr;
    char *port;

    // Create our logging category
    if (NULL == vlc_RILogCategory)
        vlc_RILogCategory = log4c_category_get("RI.Tuner.VLC");

    RILOG_INFO("%s(%d, %s, %d, %s);\n", __FUNCTION__, index, srvrIp, srvrPort,
            passwd);
    tuner[index].mInitialized = FALSE;
    tuner[index].mStreaming = FALSE;

    // Get VLC directory...
    if ((path = ricfg_getValue("RIPlatform", "RI.Headend.resources.directory")))
    {
        if (0 > snprintf(vlcPath, sizeof(vlcPath), "%s", path))
        {
            RILOG_ERROR("%s snprintf failure?!\n", __func__);
        }
        else
        {
            strcat(vlcPath, VlcSourceCmd);
        }
    }
    else
    {
        RILOG_FATAL(-9, "%s VLC directory not specified!\n", __FUNCTION__);
    }

    if (0 > snprintf(tuner[index].mTelnetIp, sizeof(tuner[index].mTelnetIp),
            "%s", srvrIp))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }
    tuner[index].mTelnetPort = srvrPort;
    if (0 > snprintf(tuner[index].mTelnetPassword,
            sizeof(tuner[index].mTelnetPassword), "%s", passwd))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }
    memcpy(tuner[index].mServerCmd, vlcPath, strlen(vlcPath));
    if (0 > snprintf(tuner[index].mAdapterName,
            sizeof(tuner[index].mAdapterName), "tuner-%d", index))
    {
        RILOG_ERROR("%s snprintf failure?!\n", __func__);
    }
    tuner[index].mTelnetSocket = 0;
#ifdef WIN32
    memset(&tuner[index].mStartupInfo, 0, sizeof(tuner[index].mStartupInfo));
    tuner[index].mStartupInfo.cb = sizeof(tuner[index].mStartupInfo);
#endif
    RILOG_DEBUG("%s Done\n", __FUNCTION__);
    tuner[index].mInitialized = TRUE;

    // Get the RI Platform IP address the Streamer should talk to...
    if (NULL == (ipAddr = ricfg_getValue("RIPlatform", "RI.Platform.IpAddr")))
    {
        ipAddr = "127.0.0.1";
        RILOG_WARN("%s RI Platform IP address not specified!\n", __FUNCTION__);
    }

    sprintf(buf, "RI.Headend.tuner.%d.%s", index, "TunerRxPort");
    if (NULL == (port = ricfg_getValue("RIPlatform", buf)))
    {
        port = "4140";
        RILOG_WARN("%s %s not specified!\n", __FUNCTION__, buf);
    }

    RILOG_DEBUG("%s got %s:%s\n", __FUNCTION__, ipAddr, port);
#if 0
    if (!launchVlcExe(index, tuner[index].mServerCmd, ipAddr, atoi(port)))
    {
        RILOG_ERROR("%s couldn't launch VLC!?\n", __FUNCTION__);
    }
#endif
}

ri_bool vlc_TunerTune(int index, Stream *stream)
{
    ri_bool retVal = FALSE;
    char command[512];
    char *result;
    int i;

    RILOG_DEBUG("%s(%d, %p);\n", __FUNCTION__, index, stream);

    if (!CHECK_INITIALIZATION(index))
    {
        RILOG_ERROR("%s tuner not initialized!?\n", __FUNCTION__);
        return retVal;
    }

    if (NULL == stream)
    {
        RILOG_ERROR("%s NULL Stream?!\n", __FUNCTION__);
        return retVal;
    }

    // Connect to the server using telnet
    if (!telnetConnect(index))
    {
#ifdef WIN32
        if (tuner[index].mProcessInfo.hThread ||
            tuner[index].mProcessInfo.hProcess)
#else
        if (tuner[index].mPid)
#endif
        {
            if (!killVlcExe(index))
            {
                RILOG_FATAL(-7, "%s couldn't kill non-responding VLC!?\n",
                            __FUNCTION__);
            }

        }
        if (!launchVlcExe(index, tuner[index].mServerCmd,
                stream->destinationAddress, stream->destinationPort))
        {
            RILOG_FATAL(-7, "%s couldn't launch VLC!?\n", __FUNCTION__);
        }

        for (i = 0; i < VLC_CONNECT_ATTEMPTS; i++)
        {
            g_usleep(VLC_LAUNCH_DELAY / VLC_CONNECT_ATTEMPTS);

            if (telnetConnect(index))
                break;
        }

        if (i >= VLC_CONNECT_ATTEMPTS)
        {
            RILOG_ERROR("%s couldn't connect by telnet!?\n", __FUNCTION__);
            return retVal;
        }
    }

    if (tuner[index].mStreaming)
    {
        // Clean-up the previous stream
        sprintf(command, cmdStop, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);
        g_usleep( VLC_STOP_DELAY);
        sprintf(command, cmdDel, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);
    }

    sprintf(command, cmdNew, tuner[index].mAdapterName);
    (void) telnetCommand(index, command);
    sprintf(command, cmdInput, tuner[index].mAdapterName, stream->srcUrl);
    (void) telnetCommand(index, command);
    sprintf(command, cmdLoop, tuner[index].mAdapterName);
    result = telnetCommand(index, command);

    if (!strstr(result, "ERROR"))
    {
        // Set-up the play
        sprintf(command, cmdPlay, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);
        retVal = tuner[index].mStreaming = TRUE;
    }
    else
    {
        tuner[index].mStreaming = FALSE;
        RILOG_ERROR("%s tuning error for Tuner%d\n", __FUNCTION__, index);
    }

    // Close connection
    telnetClose(index);
    RILOG_DEBUG("%s Returning: %s\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

char *vlc_TunerStatus(int index)
{
    char *retStr = "ERROR obtaining status";
    char command[512];
    RILOG_DEBUG("%s(%d);\n", __FUNCTION__, index);

    if (!CHECK_INITIALIZATION(index))
        return "ERROR - tuner not initialized!?";

    if (!telnetConnect(index))
    {
        RILOG_ERROR("%s- Status error: No VLC connection!?\n", __FUNCTION__);
    }
    else
    {
        sprintf(command, cmdStatus, tuner[index].mAdapterName);
        retStr = telnetCommand(index, command);
        telnetClose(index);
    }

    return retStr;
}

/**
 * Orders a stream to stop.  Not a normal tuner operation,
 * as it typically is tuned somewhere.
 */
void vlc_TunerStop(int index)
{
    char command[512];
    RILOG_DEBUG("%s(%d);\n", __FUNCTION__, index);

    if (tuner[index].mStreaming)
    {
        if (!telnetConnect(index))
        {
            RILOG_ERROR("%s- Tune error: No VLC connection!?\n", __FUNCTION__);
        }
        else
        {
            sprintf(command, cmdStop, tuner[index].mAdapterName);
            (void) telnetCommand(index, command);
            telnetClose(index);
        }

        tuner[index].mStreaming = FALSE;
    }

    RILOG_INFO("%s nothing to do, tuner not streaming...\n", __FUNCTION__);
}

/**
 * Shut-down VLC Tuner instance
 */
void vlc_TunerExit(int index)
{
    char command[512];
    RILOG_DEBUG("%s(%d); to %s:%d\n", __FUNCTION__, index,
            tuner[index].mTelnetIp, tuner[index].mTelnetPort);

    if (!CHECK_INITIALIZATION(index))
        return;

    vlc_TunerStop(index);

    if (!telnetConnect(index))
    {
#ifdef WIN32
        if (0 == tuner[index].mProcessInfo.hProcess)
#else
        if (0 == tuner[index].mPid)
#endif
            return;
    }
    else
    {
        sprintf(command, cmdDel, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);
        (void) telnetCommand(index, "exit");
        telnetClose(index);
    }

#ifdef WIN32
    ri_bool bRet;
    bRet = TerminateProcess(tuner[index].mProcessInfo.hProcess, 0);
    RILOG_INFO("%d= terminate p[%p]\n",bRet,tuner[index].mProcessInfo.hProcess);
    bRet = CloseHandle(tuner[index].mProcessInfo.hThread);
    RILOG_DEBUG("%d = closing t[%p]\n",bRet,tuner[index].mProcessInfo.hThread);
    bRet = CloseHandle(tuner[index].mProcessInfo.hProcess);
    RILOG_DEBUG("%d = closing p[%p]\n",bRet,tuner[index].mProcessInfo.hProcess);
    tuner[index].mProcessInfo.hThread = 0;
    tuner[index].mProcessInfo.hProcess = 0;
#else
    kill(tuner[index].mPid, SIGKILL);
    RILOG_INFO("t%d kill p[%d]\n", index, tuner[index].mPid);
#endif
    RILOG_INFO("%s Done\n", __FUNCTION__);
}

ri_bool vlc_TunerRetune(int index)
{
    char *status = vlc_TunerStatus(index);
    ri_bool retVal = FALSE;
    char command[512];
    char *result;
    int i;

    //
    // NOTE:
    // This method is called from ate_if.exe where all logging is to stderr
    // so don't use RILOG_XXXX, use fprintf followed by fflush.
    //

    if (status && strstr(status, "state : playing"))
    {
        // Connect to the server using telnet
        if (!telnetConnect(index))
        {
            for (i = 0; i < VLC_CONNECT_ATTEMPTS; i++)
            {
                g_usleep(VLC_LAUNCH_DELAY / VLC_CONNECT_ATTEMPTS);

                if (telnetConnect(index))
                    break;
            }

            if (i >= VLC_CONNECT_ATTEMPTS)
            {
                fprintf(stderr, "ERROR: %s couldn't connect by telnet!\n",
                        __func__);
                (void) fflush(stderr);
                return retVal;
            }
        }

        // Stop the previous stream
        sprintf(command, cmdStop, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);
        g_usleep( VLC_STOP_DELAY);

        sprintf(command, cmdDel, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);
        sprintf(command, cmdNew, tuner[index].mAdapterName);
        (void) telnetCommand(index, command);

        // Set-up the next...
        if (!vlc_GetTspFileURL())
        {
            fprintf(stderr, "ERROR: %s Tuner%d couldn't get TspFileURL\n",
                    __func__, index);
            (void) fflush(stderr);
        }

        sprintf(command, cmdInput, tuner[index].mAdapterName,
                tuner[index].mSrcUrl);
        (void) telnetCommand(index, command);
        sprintf(command, cmdLoop, tuner[index].mAdapterName);
        result = telnetCommand(index, command);

        if (!strstr(result, "ERROR"))
        {
            // and play
            sprintf(command, cmdPlay, tuner[index].mAdapterName);
            (void) telnetCommand(index, command);
            retVal = tuner[index].mStreaming = TRUE;
        }
        else
        {
            tuner[index].mStreaming = FALSE;
            fprintf(stderr, "ERROR: %s(%d) tune failed!? \n", __FUNCTION__,
                    index);
            (void) fflush(stderr);
        }

        // Close connection
        telnetClose(index);
    }

    fprintf(stderr, "DEBUG: %s Returning %s\n", __func__, boolStr(retVal));
    (void) fflush(stderr);
    return retVal;
}

char *vlc_GetTspFileURL(void)
{
    int index = 0;
    int bytesRead;
    char *path, file[512];
    FILE *fp = NULL;

    // Get the RI Platform IP address the Streamer should talk to...
    if (NULL == (path = ricfg_getValue("RIPlatform",
            "RI.Headend.resources.directory")))
    {
        path = "c:\\resources\\tsplayer-file.txt";
        RILOG_WARN("%s TS Player file not specified!\n", __FUNCTION__);
    }

    RILOG_DEBUG("%s got %s\n", __FUNCTION__, path);
    sprintf(file, "%s/tsplayer-file.txt", path);

    if (NULL != (fp = fopen(file, "r")))
    {
        if (0
                != (bytesRead = fread(tuner[index].mSrcUrl, 1, MAXURLLEN - 1,
                        fp)))
        {
            tuner[index].mSrcUrl[bytesRead] = 0;
        }

        fclose(fp);
    }

    RILOG_INFO("%s returning %s\n", __FUNCTION__, tuner[index].mSrcUrl);
    return tuner[index].mSrcUrl;
}

ri_bool vlc_TunerIsStreaming(int index)
{
    return tuner[index].mStreaming;
}

/**
 * update transport stream PID list
 *
 * @param object The tuner "this" pointer
 * @return An error code detailing the success or failure of the request.
 */
ri_error vlc_TunerUpdatePidList(int index, guint16 pids[MAX_PIDS])
{
    RILOG_DEBUG("%s -- did not update PID list for VLC tuner\n", __func__);
    return RI_ERROR_NONE;
}

