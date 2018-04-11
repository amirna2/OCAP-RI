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

#define MPE_DBG_DEFINE_STRINGS /* to pick up the debug string constants in this file */
#include <assert.h>
#include <ctype.h>
#include <limits.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <mpe_types.h>
#include <mpeos_socket.h>
#include <mpeos_util.h>
#include <mpeos_dbg.h>

/* MACROS */

/** Skip whitespace in a c-style string. */
#define SKIPWHITE(cptr) while ((*cptr != '\0') && isspace(*cptr)) cptr++

/** Bit mask for trace (TRACE1 thru TRACE9) logging levels. */
#define LOG_TRACE ( (1 << MPE_LOG_TRACE1) | (1 << MPE_LOG_TRACE2) | (1 << MPE_LOG_TRACE3) \
 | (1 << MPE_LOG_TRACE4) | (1 << MPE_LOG_TRACE5) | (1 << MPE_LOG_TRACE6) \
 | (1 << MPE_LOG_TRACE7) | (1 << MPE_LOG_TRACE8) | (1 << MPE_LOG_TRACE9) )

/** Turns on FATAL, ERROR, WARN & INFO. */
#define LOG_ALL (  (1 << MPE_LOG_FATAL) | (1 << MPE_LOG_ERROR) | \
                   (1 << MPE_LOG_WARN)  | (1 << MPE_LOG_INFO) )

/** All logging 'off'. */
#define LOG_NONE 0

#define HOSTADDR_STR_MAX 255

/* GLOBALS */

/** Global variable used to control logging. */
uint32_t mpeos_g_logControlTbl[ENUM_MPE_MOD_COUNT];

/* UDP logging variables. */
mpe_Bool dbg_logViaUDP = FALSE;
mpe_Socket dbg_udpSocket;
mpe_SocketIPv4SockAddr dbg_logHostAddr;

/* MODULE */

enum
{
    /* Used as an array index. */
    ERR_INVALID_MOD_NAME = 0, ERR_INVALID_LOG_NAME
};

enum
{
    RC_ERROR, RC_OK
};

static const char *errorMsgs[] =
{ "Error: Invalid module name.", "Warning: Ignoring invalid log name(s)." };

#define UDPFMTBUF_MAX 1023
static char udpFmtBuffer[UDPFMTBUF_MAX + 1];

/*****************************************************************************
 *
 * PRIVATE FUNCTIONS
 *
 ****************************************************************************/

static void forceUpperCase(char *token);
static int modNameToEnum(const char *name);
static int logNameToEnum(const char *name);
static void extractToken(const char **srcStr, char *tokBuf);
static int parseLogConfig(const char *cfgStr, uint32_t *configEntry,
        const char **msg);
static mpe_Bool createUDPSocket(const char *cfgStr);

/**
 * Safely force a string to uppercase. I hate this mundane rubbish.
 *
 * @param token String to be forced to uppercase.
 */
static void forceUpperCase(char *token)
{
    while (*token)
    {
        if (islower(*token))
        {
            *token = (char) toupper(*token);
        }
        token++;
    }
}

/**
 * Translate the module name to the corresponding module enum value.
 *
 * @param name Module name, which must be uppercase.
 * @return Corresponding enumeration value or -1 on error.
 */
static int modNameToEnum(const char *name)
{
    int i = 0;
    while (i < ENUM_MPE_MOD_COUNT)
    {
        if (strcmp(name, mpe_logModuleStrings[i]) == 0)
        {
            return i;
        }
        i++;
    }
    return -1;
}

/**
 * Convert a log level name to the correspodning log level enum value.
 *
 * @param name Log level name, which must be uppercase.
 * @param Corresponding enumeration value or -1 on error.
 */
static int logNameToEnum(const char *name)
{
    int i = 0;
    while (i < ENUM_MPE_LOG_COUNT)
    {
        if (strcmp(name, mpe_logLevelStrings[i]) == 0)
        {
            return i;
        }
        i++;
    }

    return -1;
}

/**
 * Extract a whitespace delimited token from a string.
 *
 * @param srcStr Pointer to the source string, this will be modified
 * to point to the first character after the token extracted.
 *
 * @param tokBuf This is a string that will be filled with the
 * token. Note: this buffer is assumed to be large enough to hold the
 * largest possible token.
 */
static void extractToken(const char **srcStr, char *tokBuf)
{
    const char *src = *srcStr;
    while (*src && !isspace(*src))
    {
        *tokBuf++ = *src++;
    }
    *tokBuf = '\0';
    *srcStr = src;
}

/**
 * Parse a whitespace delimited list of log types and log type meta names.
 *
 * @param cfgStr String containing one more log types. (Right hand
 * side of INI file entry.)
 *
 * @param defConfig Default configuration to base the return value on.
 *
 * @return Returns a bit mask that can be used as an entry in
 * #mpeos_g_logControlTbl.
 */
static int parseLogConfig(const char *cfgStr, uint32_t *configEntry,
        const char **msg)
{
    uint32_t config = *configEntry;
    char logTypeName[128] =
    { 0 };
    int rc = RC_OK;

    *msg = "";

    SKIPWHITE(cfgStr)
        ;
    if (*cfgStr == '\0')
    {
        *msg = "Warning: Empty log level confguration.";
        return RC_ERROR;
    }

    while (*cfgStr != '\0')
    {
        /* Extract and normalise log type name token. */

        memset(logTypeName, 0, sizeof(logTypeName));
        extractToken(&cfgStr, logTypeName);
        forceUpperCase(logTypeName);

        /* Handle special meta names. */

        if (strcmp(logTypeName, "ALL") == 0)
        {
            config |= LOG_ALL;
        }
        else if (strcmp(logTypeName, "NONE") == 0)
        {
            config = LOG_NONE;
        }
        else if (strcmp(logTypeName, "TRACE") == 0)
        {
            config |= LOG_TRACE;
        }
        else if (strcmp(logTypeName, "!TRACE") == 0)
        {
            config &= ~LOG_TRACE;
        }
        else
        {
            /* Determine the corresponding bit for the log name. */
            int invert = 0;
            const char *name = logTypeName;
            int logLevel = -1;

            if (logTypeName[0] == '!')
            {
                invert = 1;
                name = &logTypeName[1];
            }

            logLevel = logNameToEnum(name);
            if (logLevel != -1)
            {
                if (invert)
                {
                    config &= ~(1 << logLevel);
                }
                else
                {
                    config |= (1 << logLevel);
                }
            }
            else
            {
                *msg = errorMsgs[ERR_INVALID_LOG_NAME];
                rc = RC_ERROR;
            }
        }

        SKIPWHITE(cfgStr)
            ;
    }

    *configEntry = config;
    return rc;
}

/*
 * Initialize udp logging support based on the configuration string
 * provided in "mpeenv.ini".
 *
 * Returns FALSE if either the configuration is invalid or any system
 * call used to initialize the networking support fails.
 */
static mpe_Bool createUDPSocket(const char *cfgStr)
{
    long port = 0;
    char hostaddr[HOSTADDR_STR_MAX + 1];
    const char *portStr = NULL;
    char *strEnd = NULL;
    int status = 0;

    portStr = strchr(cfgStr, ':');
    if (portStr == NULL)
    {
        /* no colon = no port number */
        printf("*** invalid configuration no colon!\n");
        return FALSE;
    }

    memset(hostaddr, 0, sizeof(hostaddr));
    strncpy(hostaddr, cfgStr, portStr - cfgStr);
    printf("*** hostaddr = '%s'\n", hostaddr);
    ++portStr; /* skip past ':' */
    if (*portStr == 0)
    {
        /* there was a colon but no port specified */
        printf("*** no port specified\n");
        return FALSE;
    }

    port = strtol(portStr, &strEnd, 10);
    if (port <= 0 || *strEnd != 0 || port >= SHRT_MAX)
    {
        /* invalid number, invalid port or has trailing garbage */
        printf("*** invalid number specified for port or trailing garbage.\n");
        return FALSE;
    }

#if 0
    if (mpeos_socketInit() == FALSE)
    {
        printf("** socket layer could not be initialized\n");
        return FALSE;
    }
#endif

    dbg_udpSocket = mpeos_socketCreate(AF_INET, SOCK_DGRAM, 0);
    if (dbg_udpSocket == MPE_SOCKET_INVALID_SOCKET)
    {
        /* failed to create socket */
        printf("*** failed to create socket\n");
        return FALSE;
    }

    /* initialise the logging host address */

    (void) memset((uint8_t *) &dbg_logHostAddr, 0, sizeof(dbg_logHostAddr));
    dbg_logHostAddr.sin_family = AF_INET;
    status = mpeos_socketAtoN(hostaddr, &(dbg_logHostAddr.sin_addr));
    if (status == 0)
    {
        /* invalid or unknown host name */
        printf("*** invalid or unknown host\n");
        return FALSE;
    }

    printf("*** port number = %li\n", port);
    dbg_logHostAddr.sin_port = htons((uint16_t) port);

#if 0
    status = mpeos_socketConnect(dbg_udpSocket,
            (mpe_SocketSockAddr*)&dbg_logHostAddr,
            sizeof(dbg_logHostAddr));
    if (status == 0 )
    {
        printf("*** error failed to connect socket");
        return FALSE;
    }
#endif

    printf("*** successfully setup logging socket\n");
    return TRUE;
}

/*****************************************************************************
 *
 * EXPORTED FUNCTIONS
 *
 ****************************************************************************/

/**
 * Initialize the debug log control table. This should be called from
 * the initialization routine of the debug manager.
 */
void mpeos_dbgLogControlInit(void)
{
    char envVarName[128] =
    { 0 };
    const char *envVarValue = NULL;
    uint32_t defaultConfig = 0;
    int mod = 0;
    const char *msg = "";

    /* Configure UDP logging if desired. */
    envVarValue = mpeos_envGet("LOG.MPE.UDP");
    if (envVarValue != NULL && createUDPSocket(envVarValue) == TRUE)
    {
        printf("udp logging enabled!");
        dbg_logViaUDP = TRUE;
    }

    /* Pre-condition the control table to disable all logging.  This
     * means that if no logging control statements are present int the
     * mpeenv.ini file all logging will be suppressed. */
    memset(mpeos_g_logControlTbl, 0, sizeof(mpeos_g_logControlTbl));

    /* Intialize to the default configuration for all modules. */
    strcpy(envVarName, "LOG.MPE.DEFAULT");
    envVarValue = mpeos_envGet(envVarName);
    if ((envVarValue != NULL) && (envVarValue[0] != 0))
    {
        (void) parseLogConfig(envVarValue, &defaultConfig, &msg);
        for (mod = ENUM_MPE_MOD_BEGIN; mod < ENUM_MPE_MOD_COUNT; mod++)
        {
            mpeos_g_logControlTbl[mod] = defaultConfig;
        }
    }

    /* Configure each module from the ini file. Note: It is not an
     * error to have no entry in the ini file for a module - we simply
     * leave it at the default logging. */
    for (mod = ENUM_MPE_MOD_BEGIN; mod < ENUM_MPE_MOD_COUNT; mod++)
    {
        sprintf(envVarName, "LOG.MPE.%s", mpe_logModuleStrings[mod]);
        envVarValue = mpeos_envGet(envVarName);
        if ((envVarValue != NULL) && (envVarValue[0] != '\0'))
        {
            (void) parseLogConfig(envVarValue, &mpeos_g_logControlTbl[mod],
                    &msg);
        }
    }
}

/**
 * Initially created to specifically support PowerTV, this function is
 * intended to provide a convenient interface to be invoked by command
 * line/console commands,
 *
 * @param modName Module name to be configured, must not be prefixed
 * by 'MPE.LOG.', it does not need to be uppercase but *must* be
 * modifiable.
 *
 * @param cfgStr Space seperated log levels as single string, any line
 * ending will be ignored.
 *
 * @return A static string which may printed out to provide
 * information to the user on status of the command; and in the case
 * of failure the reason.
 */
const char * mpeos_dbgLogControlOpSysIntf(char *modName, char *cfgStr)
{
    char *name = modName;
    int mod = -1;
    int rc = RC_ERROR;
    const char *msg = NULL;

    assert(modName);
    assert(cfgStr);

    forceUpperCase(name);

    /* Allow the user to reset all logs at once. */
    if (strcmp(name, "DEFAULT") == 0)
    {
        uint32_t config = 0;
        rc = parseLogConfig(cfgStr, &config, &msg);
        if (rc == RC_OK)
        {
            for (mod = ENUM_MPE_MOD_BEGIN; mod < ENUM_MPE_MOD_COUNT; mod++)
            {
                mpeos_g_logControlTbl[mod] = config;
            }
        }
    }
    else
    {
        mod = modNameToEnum(name);
        if (mod == -1)
        {
            return errorMsgs[ERR_INVALID_MOD_NAME];
        }
        (void) parseLogConfig(cfgStr, &mpeos_g_logControlTbl[mod], &msg);
    }

    return msg;
}

/**
 * Provides an interface to query the configuration of logging in a
 * specific module as a user readable string. Note: the mimimum
 * acceptable length of the supplied configuration buffer is 32 bytes.
 *
 * @param modName Name of the module.
 *
 * @param cfgStr The configuration strng: which should be space
 * separated module names.
 *
 * @param cfgStrMaxLen The maximum length of the configuration string
 * to be returned including the NUL character.
 *
 * @return A string containing a user readable error message if an
 * error occured; or "OK" upon success.
 */
const char * mpeos_dbgLogQueryOpSysIntf(char *modName, char *cfgStr,
        int cfgStrMaxLen)
{
    char *name = modName;
    int mod = -1;
    uint32_t modCfg = 0;
    int level = -1;

    assert(modName);
    assert(cfgStr);
    assert(cfgStrMaxLen > 32);

    cfgStrMaxLen -= 1; /* Ensure there is space for NUL. */
    strcpy(cfgStr, "");

    /* Get the module configuration. Note: DEFAULT is not valid as it
     * is an alias. */
    forceUpperCase(name);
    mod = modNameToEnum(modName);
    if (mod < 0)
    {
        return "Unknown module specified.";
    }
    modCfg = mpeos_g_logControlTbl[mod];

    /* Try and find a succinct way of describing the configuration. */

    if (modCfg == 0)
    {
        strcpy(cfgStr, "NONE");
        return "OK"; /* This is a canonical response. */
    }

#if 0 /* BAT: print out the level details instead of just these abbreviations */

    /* Look for appropriate abberviations. */

    if ((modCfg & LOG_ALL) == LOG_ALL)
    {
        strcpy(cfgStr, " ALL");
        modCfg &= ~LOG_ALL;
    }

    if ((modCfg & LOG_TRACE) == LOG_TRACE)
    {
        strcat(cfgStr, " TRACE");
        modCfg &= ~LOG_TRACE;
    }
#endif /* BAT */

    if (modCfg == 0)
    {
        /* The abbreviations have covered all the enabled levels. */
        return "OK";
    }

    /* Loop through the control word and print out the enabled levels. */

    for (level = ENUM_MPE_LOG_BEGIN; level < ENUM_MPE_LOG_COUNT; level++)
    {
        if (modCfg & (1 << level))
        {
            /* Stop building out the config string if it would exceed
             * the buffer length. */
            if (strlen(cfgStr) + strlen(mpe_logLevelStrings[level])
                    > (size_t) cfgStrMaxLen)
            {
                return "Warning: Config string too long, config concatenated.";
            }

            strcat(cfgStr, " "); /* Not efficient - rah rah. */
            strcat(cfgStr, mpe_logLevelStrings[level]);
        }
    }

    return "OK";
}

/**
 * Send a log message over UDP.
 */
void mpeos_dbgLogUDP(const char *format, va_list args)
{
    size_t len = 0;

    vsprintf(udpFmtBuffer, format, args);

    len = strlen(udpFmtBuffer);
    if (len == 0)
    {
        return;
    }

    (void) mpeos_socketSendTo(dbg_udpSocket, udpFmtBuffer, len + 1, 0, /* include the NUL character */
    (mpe_SocketSockAddr*) &dbg_logHostAddr, sizeof(dbg_logHostAddr));
}
