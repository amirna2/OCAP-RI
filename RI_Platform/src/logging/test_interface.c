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

#include <glib.h>

#include "net_utils.h"
#include "ri_log.h"
#include "test_interface.h"

static MenuItem menus[MAX_MENUS + 1];

// Logging category
log4c_category_t* test_RILogCategory = NULL;
#define RILOG_CATEGORY test_RILogCategory

// contain all the module data test_Interface requires:
static struct TestServer
{
    ri_bool quit;
    int mNumMenus;              // current number of menus registered
    int mPort;                  // telnet listener port
    int mSocket;                // telnet socket
    struct TestApp* apps;       // the list of apps currently being served
    GMutex *mutex;
    GThread *thread;
} srvr;

struct TestApp
{
    ri_bool quit;
    int mMenus[MAX_MENU_DEPTH]; // The list of menus we have gone into
    int mMenuDepth;             // Our current depth in the menu list
    int fd;                     // conversation socket descriptor
    GMutex *mutex;
    GThread *thread;
    struct TestApp* next;
};

static void testAppAdd(struct TestApp* app)
{
    struct TestApp* pApp = NULL;

    if (NULL != app)
    {
        g_mutex_lock(srvr.mutex);
        app->next = NULL;

        if (NULL == srvr.apps)
        {
            srvr.apps = app;
        }
        else
        {
            // find end of app list
            for(pApp = srvr.apps; NULL != pApp->next; pApp = pApp->next);

            pApp->next = app;
        }

        g_mutex_unlock(srvr.mutex);
    }
}

static void testAppRemove(struct TestApp* app)
{
    struct TestApp* pApp = NULL;
    struct TestApp* pPrev = NULL;

    if (NULL != app)
    {
        g_mutex_lock(srvr.mutex);

        if (NULL == srvr.apps)
        {
            RILOG_ERROR("%s srvr.apps == NULL!?\n", __FUNCTION__);
        }
        else
        {
            // find the app in the list
            for(pPrev = pApp = srvr.apps; NULL != pApp; pApp = pApp->next)
            {
                if (pApp == app)
                {
                    if (app == srvr.apps)
                    {
                        srvr.apps = srvr.apps->next;
                    }
                    else
                    {
                        pPrev->next = pApp->next;
                    }

                    break;
                }

                pPrev = pApp;
            }
        }

        g_mutex_unlock(srvr.mutex);
    }
}

static struct TestApp* testAppGet(int sock)
{
    struct TestApp* pApp = NULL;

    if (NULL == srvr.apps)
    {
        RILOG_ERROR("%s srvr.apps == NULL!?\n", __FUNCTION__);
    }
    else
    {
        g_mutex_lock(srvr.mutex);

        // find the app in the list
        for(pApp = srvr.apps; NULL != pApp; pApp = pApp->next)
        {
            if (pApp->fd == sock)
            {
                break;
            }
        }

        g_mutex_unlock(srvr.mutex);
    }

    return pApp;
}

static void menuItemCopy(MenuItem *pDst, MenuItem *pSrc)
{
    if (NULL != pDst && NULL != pSrc)
    {
        pDst->base = pSrc->base;
        pDst->handleInput = pSrc->handleInput;

        if(NULL != pDst->sel_char)
        {
            g_free(pDst->sel_char);
        }

        pDst->sel_char = g_strdup(pSrc->sel_char);

        if(NULL != pDst->title)
        {
            g_free(pDst->title);
        }

        pDst->title = g_strdup(pSrc->title);

        if(NULL != pDst->text)
        {
            g_free(pDst->text);
        }

        pDst->text = g_strdup(pSrc->text);
    }
}

/**
 * test_RegisterMenu
 */
ri_bool test_RegisterMenu(MenuItem *pMenuItem)
{
    int index = 0;
    RILOG_DEBUG("%s(%p)\n", __FUNCTION__, pMenuItem);

    if ((srvr.mNumMenus < MAX_MENUS) && (NULL != pMenuItem))
    {
        g_mutex_lock(srvr.mutex);

        if ((index = test_FindMenu(pMenuItem->title)) < MAX_MENUS)
        {
            menuItemCopy(&menus[index], pMenuItem);
            RILOG_INFO("%s modified MenuItem %s\n", __FUNCTION__,
                    pMenuItem->title);
        }
        else
        {
            index = srvr.mNumMenus;
            srvr.mNumMenus++;
            memset(&menus[index], 0, sizeof(MenuItem));
            menuItemCopy(&menus[index], pMenuItem);
            RILOG_INFO("%s added MenuItem %s (now have: %d)\n", __FUNCTION__,
                    pMenuItem->title, srvr.mNumMenus);
        }

        g_mutex_unlock(srvr.mutex);
        return TRUE;
    }

    return FALSE;
}

/**
 * test_SetNextMenu
 */
ri_bool test_SetNextMenu(int sock, int index)
{
    struct TestApp* app = testAppGet(sock);

    if (NULL != app)
    {
        if (index < MAX_MENUS)
        {
            app->mMenus[app->mMenuDepth + 1] = index;
            return TRUE;
        }
        else
        {
            RILOG_ERROR("%s out of range\n", __FUNCTION__);
            return FALSE;
        }
    }
    else
    {
        RILOG_ERROR("%s couldn't find app!?\n", __FUNCTION__);
        return FALSE;
    }
}

/**
 * test_FindMenu
 */
int test_FindMenu(char *title)
{
    int index = MAX_MENUS;

    if (NULL == title)
    {
        RILOG_ERROR("%s null title!\n", __FUNCTION__);
        return MAX_MENUS;
    }

    for (index = 0; index < srvr.mNumMenus; index++)
    {
        if (0 == strcmp(menus[index].title, title))
            break;
    }

    if (index >= srvr.mNumMenus)
    {
        RILOG_INFO("%s couldn't find %s!\n", __FUNCTION__, title);
        return MAX_MENUS;
    }

    return index;
}

/**
 * test_GetBytes
 */
static int test_GetBytes(int sock, char *rxBuf, int size)
{
    int bytesRcvd = 0;

    // get the response...
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
 * test_GetString
 */
int test_GetString(int sock, char *rxBuf, int size, char *prompt)
{
    int bytesRcvd = 0;
    int bufIndex = 0;
    memset(rxBuf, 0, size);

    if (NULL != prompt)
    {
        test_SendString(sock, prompt);
        test_SendString(sock, " >");
    }

    while ((bytesRcvd = test_GetBytes(sock, &rxBuf[bufIndex], 2)) != 0)
    {
        RILOG_TRACE("%s:%X(%c)\n", __FUNCTION__, rxBuf[bufIndex],
                rxBuf[bufIndex]);

        if ((rxBuf[bufIndex] == '\b') || (rxBuf[bufIndex] == 127))
        {
            test_SendString(sock, "\b \b");
            rxBuf[bufIndex] = 0;
            bufIndex--;
            continue;
        }

        if ((rxBuf[bufIndex] == '\n') || (rxBuf[bufIndex] == '\r'))
        {
            rxBuf[bufIndex] = 0;
            test_FlushInput(sock);
            return bufIndex;
        }

        test_SendString(sock, &rxBuf[bufIndex]);
        bufIndex += bytesRcvd;
    }

    RILOG_DEBUG("%d = %s(%d);\n== %s\n", bufIndex, __FUNCTION__, sock, rxBuf);
    return bufIndex;
}

/**
 * test_GetNumber
 */
int test_GetNumber(int sock, char *rxBuf, int size, char *prompt, int dfault)
{
    if (test_GetString(sock, rxBuf, size, prompt))
    {
        RILOG_DEBUG("%s telnet interface buf: %s\n", __FUNCTION__, rxBuf);
        return atoi(rxBuf);
    }

    return dfault;
}

/**
 * test_FlushInput
 */
void test_FlushInput(int sock)
{
    int size = 1024;
    char rxBuf[size];
    (void)test_GetBytes(sock, rxBuf, size);
}

/**
 * sendString
 */
static ri_bool sendString(int sock, char *string)
{
    if ((0 != sock) && (NULL != string))
    {
        int len = strlen(string);
        RILOG_DEBUG("%s: %d bytes, %s\r\n", __FUNCTION__, len, string);

        if (send(sock, string, len, 0) != len)
        {
            RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
            return FALSE;
        }

        return TRUE;
    }

    return FALSE;
}

/**
 * test_SendString
 */
void test_SendString(int sock, char *string)
{
    if ((0 != sock) && (NULL != string))
    {
        (void) sendString(sock, string);
    }
}

/**
 * sendMenu
 */
static ri_bool sendMenu(int sock)
{
    int i;
    char prompt[256];
    struct TestApp* app = testAppGet(sock);

    if (NULL == app)
    {
        RILOG_ERROR("%s couldn't find app!?\n", __func__);
        return FALSE;
    }

    if (0 == app->mMenuDepth) // if we're at the base menu, build & send it...
    {
        char line[80];

        if (!sendString(sock, "\r\n\r\n  Test Application v1.00\r\n\r\n"))
        {
            RILOG_INFO("%s sendMenu: call to sendString failed\n", __func__);
            return FALSE;
        }

        for (i = 0; NULL != menus[i].title; i++)
        {
            if (!menus[i].base)
                continue;

            if (!sendString(sock, "|---+-----------------------\r\n"))
                return FALSE;

            sprintf(line, "| %s | %s\r\n", menus[i].sel_char, menus[i].title);

            if (!sendString(sock, line))
                return FALSE;
        }
    } // otherwise, just send the current title & menu.
    else if (!sendString(sock, "\r\n\n----------------------------\r\n| "))
        return FALSE;
    else if (!sendString(sock, menus[app->mMenus[app->mMenuDepth]].title))
        return FALSE;
    else if (!sendString(sock, menus[app->mMenus[app->mMenuDepth]].text))
        return FALSE;

    // always end up with the 'x' to exit option on any menu.
    if (!sendString(sock, "|---+-----------------------\r\n"))
        return FALSE;
    if (!sendString(sock, "| x | Exit \r\n"))
        return FALSE;
    if (!sendString(sock, "|---+-----------------------\r\n"))
        return FALSE;

    // build a prompt that shows our depth.
    sprintf(prompt, "\r\n//Test");

    for (i = 1; i <= app->mMenuDepth; i++)
    {
        strcat(prompt, "/");
        strcat(prompt, menus[app->mMenus[i]].title);
    }

    strcat(prompt, " > ");

    // send the prompt...
    if (!sendString(sock, prompt))
        return FALSE;

    return TRUE;
}

static ri_bool exchangeTelnetOptions(int sock)
{
    int i;
    unsigned char proposedOptions[NOPTS];
    unsigned char expectedOptions[NOPTS];
    unsigned char rxBuf[NOPTS * 2];
    ri_bool retVal = TRUE;

    // Load proposed options: Command: Will Echo, Command: Will Suppress Go Ahead
    proposedOptions[0] = 0xff;
    proposedOptions[1] = 0xfb;
    proposedOptions[2] = 0x01;
    proposedOptions[3] = 0xff;
    proposedOptions[4] = 0xfb;
    proposedOptions[5] = 0x03;

    // Load expected options: Command: Do Echo, Command: Do Suppress Go Ahead
    expectedOptions[0] = 0xff;
    expectedOptions[1] = 0xfd;
    expectedOptions[2] = 0x01;
    expectedOptions[3] = 0xff;
    expectedOptions[4] = 0xfd;
    expectedOptions[5] = 0x03;

    memset(rxBuf, 0, NOPTS);

    if (send(sock, (char *) proposedOptions, NOPTS, 0) != NOPTS)
    {
        RILOG_ERROR("%s send() failed?\n", __FUNCTION__);
        return FALSE;
    }

    // get the response...
    if ((i = recv(sock, rxBuf, NOPTS, 0)) <= 0)
    {
        RILOG_ERROR("%s recv() failed?\n", __FUNCTION__);
        return FALSE;
    }

    for (i = 0; i < NOPTS; i++)
    {
        if (rxBuf[i] != expectedOptions[i])
        {
            RILOG_DEBUG("Difference in client option[%d] value:%02X != %02X\n",
                    i, rxBuf[i] & 0xFF, expectedOptions[i] & 0xFF);
            retVal = FALSE;
        }
    }

    return retVal;
}

/**
 * processBaseMenuInput
 */
static ri_bool processBaseMenuInput(int sock, char *input)
{
    struct TestApp* app = testAppGet(sock);
    int i;

    if (NULL == app)
    {
        RILOG_ERROR("%s couldn't find app!?\n", __func__);
        return FALSE;
    }

    // walk the currently registered menus and look for a match on sel_char
    for (i = 0; NULL != menus[i].title; i++)
    {
        if (!menus[i].base)
            continue;

        if (strstr(input, menus[i].sel_char)) // we have a match...
        {
            app->mMenuDepth++; // increase depth to new menu
            app->mMenus[app->mMenuDepth] = i; // assign the menu to enter
            return TRUE;
        }
    }

    return FALSE;
}

static char* menuRetCodeToString(int retCode)
{
    switch(retCode)
    {
        case MENU_SUCCESS:
            return "SUCCESS";
        case MENU_FAILURE:
            return "FAILURE";
        case MENU_INVALID:
            return "INVALID";
        default:
            return "UNKNOWN";
    }
}

/**
 * testAppThread
 */
static gpointer testAppThread(gpointer data)
{
    const char *rspStr = "\nRESULT: menu %s, selected %c, returned %d, string %s: %s\n";
    char rxBuf[RXBUFSIZE];
    char resultBuf[RXBUFSIZE];
    int retCode = 0;             // the return code from the menu I/O handler
    char *retStr = NULL;         // the return string from the menu I/O handler
    char selected = 0;           // the current menu selected character
    struct TestApp* app = testAppGet((int)data);
 
    RILOG_INFO("%s(%p)\n", __FUNCTION__, data);

    if (!exchangeTelnetOptions(app->fd))
    {
        RILOG_DEBUG("%s exchangeTelnetOptions failed?\n", __func__);
        return NULL;
    }

    g_mutex_lock(app->mutex);
    app->mMenuDepth = 0; // start with no depth...
    app->mMenus[app->mMenuDepth] = 0; // start at the base menu...
    (void) sendMenu(app->fd);
    g_mutex_unlock(app->mutex);

    while (app->quit == FALSE)
    {
        int numBytesReceived = 0;
        rxBuf[0] = '\0';

        if ((numBytesReceived = test_GetBytes(app->fd, rxBuf, RXBUFSIZE)) == 0)
        {
            RILOG_ERROR("%s test_interface test_GetBytes error, closing client"
                        "socket\n", __func__);
            CLOSESOCK(app->fd);
            app->fd = -1;
            break;
        }

        RILOG_INFO("%s test_interface test_GetBytes finished: %d bytes"
                   "received: %s\n", __func__, numBytesReceived, rxBuf);

        selected = rxBuf[0];
        retCode = 0;
        retStr = NULL;

        g_mutex_lock(app->mutex);

        if ((numBytesReceived >= 3) &&
           (0x1B == rxBuf[0]) && (0x5B == rxBuf[1]) && (0x41 == rxBuf[2]))
        {
            char rxCmdStringBuf[RXBUFSIZE];
            int index = 0;
            char *p;

            if (numBytesReceived > 3)
            {
                memcpy(rxCmdStringBuf, rxBuf + 3, numBytesReceived-3);
                memcpy(rxCmdStringBuf+numBytesReceived-3, "", 1); // append null
            }
            else
            {
                rxCmdStringBuf[0] = 0;
                test_GetBytes(app->fd, rxCmdStringBuf, 64);
            }

            p = strchr(rxCmdStringBuf, ',');
            if (NULL != p)
            {
                *p++ = 0;       // NULL terminate the menu title
                selected = *p;  // get the menu selection
                        
                if ((index = test_FindMenu(rxCmdStringBuf)) < MAX_MENUS)
                {
                    menus[index].handleInput(app->fd, p,
                        &retCode, &retStr);
                    snprintf(resultBuf, sizeof(resultBuf), rspStr,
                        menus[index].title, selected, retCode,
                        menuRetCodeToString(retCode),
                        (NULL == retStr?  "NULL" : retStr));
                    test_SendString(app->fd, resultBuf);

                    if (retStr != NULL)
                    {
                        g_free (retStr);
                        retStr = NULL;
                    }
                }
            }
            else
            {
                RILOG_ERROR("%s unable to find menu selection: %s\n",
                            __FUNCTION__, rxCmdStringBuf);
            }
        }
        else
        {
            if (app->mMenuDepth > 0) // process nested menu input
            {
                int depth = app->mMenuDepth;
                g_mutex_unlock(app->mutex);
                depth += menus[app->mMenus[depth]].handleInput(app->fd, rxBuf,
                                                             &retCode, &retStr);
                g_mutex_lock(app->mutex);
                app->mMenuDepth = depth;
                snprintf(resultBuf, sizeof(resultBuf), rspStr,
                         menus[app->mMenus[depth]].title, selected, retCode,
                         menuRetCodeToString(retCode),
                         (NULL == retStr?  "NULL" : retStr));
                // To avoid breaking external dependencies on the Telnet
                // Interface responses (or lack of which), only log the
                // response here.  Note that the escaped invocation above
                // still sends the response string via telnet.
                RILOG_INFO("%s: %s\n", __FUNCTION__, resultBuf);

                if (retStr != NULL)
                {
                    g_free (retStr);
                    retStr = NULL;
                }
            }
            else if (!processBaseMenuInput(app->fd, rxBuf))
            {
                if (strstr(rxBuf, "x"))
                {
                    g_mutex_unlock(app->mutex);
                    RILOG_INFO("%s closing client socket\n", __func__);
                    CLOSESOCK(app->fd);
                    app->fd = -1;
                    break;
                }
                else if (NULL == strstr(rxBuf, "\r"))
                {
                    RILOG_DEBUG("%s unrecognized: %s\n", __FUNCTION__, rxBuf);
                }
                else if (NULL == strstr(rxBuf, "\n"))
                {
                    RILOG_DEBUG("%s unrecognized: %s\n", __FUNCTION__, rxBuf);
                }
            }
        }

        (void) sendMenu(app->fd);
        g_mutex_unlock(app->mutex);
    }

    if (-1 != app->fd)
    {
        test_SendString(app->fd, "\r\n\r\nbye\r\n\r\n");
        CLOSESOCK(app->fd);
    }

    testAppRemove(app);
    g_mutex_free(app->mutex);
    g_free(app);

    RILOG_INFO("%s exiting...\n", __FUNCTION__);
    return NULL;
}

/**
 * testServerThread
 */
static gpointer testServerThread(gpointer data)
{
    char str[INET6_ADDRSTRLEN] = {0};
    struct sockaddr_storage their_addr; // connector's address information
    int sin_size = 0;
    int sock = 0;
    struct TestApp* app = NULL;
 
    RILOG_INFO("%s(%p); for %d\n", __FUNCTION__, data, srvr.mPort);

    if (listen(srvr.mSocket, 2) == -1)
    {
        RILOG_ERROR("%s listen(%d, %d) failed?\n", __func__, srvr.mSocket, 2);
        return NULL;
    }

    while (srvr.quit == FALSE)
    {
        RILOG_INFO("%s waiting for a connection...\n", __FUNCTION__);
        sin_size = sizeof their_addr;
        sock = accept(srvr.mSocket, (struct sockaddr*) &their_addr,
                                           (socklen_t*) &sin_size);
        if (sock == -1)
        {
            RILOG_ERROR("%s accept(%d) failed?\n", __FUNCTION__, srvr.mSocket);
            continue;
        }

        RILOG_INFO("%s got connection from %s\n", __FUNCTION__,
                net_ntop(their_addr.ss_family, &their_addr, str, sizeof(str)));

        app = g_malloc0(sizeof(struct TestApp));
        app->mutex = g_mutex_new();
        app->fd = sock;
        testAppAdd(app);
        
        if (NULL == (app->thread = g_thread_create(testAppThread,
                                                  (void*)sock, FALSE, 0)))
        {
            RILOG_ERROR("g_thread_create() returned NULL?!\n");
        }
    }

    CLOSESOCK(srvr.mSocket);
    RILOG_INFO("%s exiting...\n", __FUNCTION__);
    return NULL;
}

/**
 * test_InterfaceInit
 */
void test_InterfaceInit(char* srvrIp, int srvrPort)
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

    // Create our logging category
    test_RILogCategory = log4c_category_get("RI.TEST");
    RILOG_INFO("%s(%d);\n", __FUNCTION__, srvrPort);

    if (0 == srvrPort)
    {
        RILOG_ERROR("%s zero port!?\n", __FUNCTION__);
        return;
    }

    srvr.mPort = srvrPort;
    srvr.mSocket = 0;
    srvr.mutex = g_mutex_new();
    srvr.mNumMenus = 0;
    srvr.quit = FALSE;
    memset(menus, 0, (MAX_MENUS + 1) * sizeof(MenuItem));

    // declare the server socket
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
    snprintf(portStr, sizeof(portStr), "%d", srvr.mPort);

    if (0 != (ret = getaddrinfo(NULL, portStr, &hints, &srvrInfo)))
    {
        RILOG_ERROR("%s: getaddrinfo[%s]\n", __FUNCTION__, gai_strerror(ret));
        return;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        if (0 > (srvr.mSocket = socket(pSrvr->ai_family,
                                       pSrvr->ai_socktype,
                                       pSrvr->ai_protocol)))
        {
            RILOG_WARN("%s socket() failed?\n", __FUNCTION__);
            continue;
        }

        if (0 > setsockopt(srvr.mSocket, SOL_SOCKET, SO_REUSEADDR,
                           (char*) &yes, sizeof(yes)))
        {
            CLOSESOCK(srvr.mSocket);
            RILOG_ERROR("%s setsockopt() failed?\n", __FUNCTION__);
            return;
        }

        if (0 > (bind(srvr.mSocket, pSrvr->ai_addr, pSrvr->ai_addrlen)))
        {
            CLOSESOCK(srvr.mSocket);
            RILOG_ERROR("%s bind() failed, closed server socket\n",
                        __FUNCTION__);
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

    if (NULL == (srvr.thread = g_thread_create(testServerThread, 0, FALSE, 0)))
        RILOG_ERROR("g_thread_create() returned NULL?!\n");
}

/**
 * test_InterfaceAbort
 */
void test_InterfaceAbort(void)
{
    struct TestApp* pApp = NULL;

    if (NULL != srvr.apps)
    {
        g_mutex_lock(srvr.mutex);

        for(pApp = srvr.apps; NULL != pApp; pApp = pApp->next)
        {
            pApp->quit = TRUE;
        }

        g_mutex_unlock(srvr.mutex);
    }

    srvr.quit = TRUE;
    CLOSESOCK(srvr.mSocket);
    RILOG_WARN("%s exiting...\n", __FUNCTION__);
}

char *dateString(char *date, int dateSize)
{
#ifdef _WIN32
    SYSTEMTIME stime;

    GetLocalTime(&stime);
    (void) snprintf(date, dateSize, "-%04d%02d%02d%02d%02d%02d.",
            stime.wYear, stime.wMonth , stime.wDay,
            stime.wHour, stime.wMinute, stime.wSecond);
#else
    struct timeval timestamp;
    struct tm *tm;

    gettimeofday(&timestamp, NULL);
    tm = localtime(&timestamp.tv_sec);
    snprintf(date, dateSize, "-%04d%02d%02d%02d%02d%02d.", tm->tm_year + 1900,
            tm->tm_mon + 1, tm->tm_mday, tm->tm_hour, tm->tm_min, tm->tm_sec);
#endif
    return date;
}

