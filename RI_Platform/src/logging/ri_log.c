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
#include <string.h>

#include <stdlib.h>
#include <time.h>
#include <glib.h>

#include "ri_log.h"
#include "socket_client.h"

#define MAX_LOGLINE_LENGTH 4096

static const char* dated_format_nocr(const log4c_layout_t* a_layout,
        const log4c_logging_event_t*a_event);
static const char* basic_format_nocr(const log4c_layout_t* a_layout,
        const log4c_logging_event_t*a_event);
static int stream_env_overwrite_open(log4c_appender_t * this);
static int stream_env_append_open(log4c_appender_t * this);
static int stream_env_append(log4c_appender_t* this, const log4c_logging_event_t* a_event);
static int stream_env_plus_stdout_append(log4c_appender_t* this, const log4c_logging_event_t* a_event);
static int stream_env_close(log4c_appender_t * this);
static int socket_env_open(log4c_appender_t * this);
static int socket_env_append(log4c_appender_t* this, const log4c_logging_event_t* a_event);
static int socket_env_close(log4c_appender_t * this);

#define RILOG_CATEGORY defaultCategory
log4c_category_t* defaultCategory = NULL;
log4c_category_t* glibCategory = NULL;

const log4c_layout_type_t log4c_layout_type_dated_nocr =
{ "dated_nocr", dated_format_nocr, };

const log4c_layout_type_t log4c_layout_type_basic_nocr =
{ "basic_nocr", basic_format_nocr, };

const log4c_appender_type_t
        log4c_appender_type_stream_env =
        { "stream_env", stream_env_overwrite_open, stream_env_append,
                stream_env_close, };

const log4c_appender_type_t log4c_appender_type_stream_env_append =
{ "stream_env_append", stream_env_append_open, stream_env_append,
        stream_env_close, };

const log4c_appender_type_t log4c_appender_type_stream_env_plus_stdout =
{ "stream_env_plus_stdout", stream_env_overwrite_open,
        stream_env_plus_stdout_append, stream_env_close, };

const log4c_appender_type_t log4c_appender_type_stream_env_append_plus_stdout =
{ "stream_env_append_plus_stdout", stream_env_append_open,
        stream_env_plus_stdout_append, stream_env_close, };

const log4c_appender_type_t log4c_appender_type_socket_env =
{ "socket_env", socket_env_open, socket_env_append, socket_env_close, };

static struct logger_data
{
    // This boolean "semaphore" protects our logging buffer from
    // simultaneous, multi-thread access.
    GMutex* logBufferMutex;
    GCond* logBufferCond;
    gboolean bufferAvailable;
} logger;

/**
 * Injects a character string into the log output.
 * This function exists so that log messages can be injected into the log
 * output regardless of the priority setting of the default category.
 *
 * @param message the message to be output.
 */
static void injectNoticeMessageInLoggerOutput(char* message)
{
    // get the appender for the default category
    const log4c_appender_t* pAppender = log4c_category_get_appender(
            defaultCategory);

    // create a log event for the message
    log4c_logging_event_t* pEvent = log4c_logging_event_new(
            log4c_category_get_name(defaultCategory), // category
            LOG4C_PRIORITY_NOTICE, // priority
            message); // message

    // inject the event into the log output stream
    (void) log4c_appender_append((log4c_appender_t*) pAppender, pEvent);

    // free the event memory
    log4c_logging_event_delete(pEvent);
}

/**
 * Dumps category information to the appender associated with the default
 * category for all configured categories.
 * Keeping consistency with the output of the MPE logging system, this
 * function will output all active priority levels for each category.
 */
static void dumpCategoryInfo(void)
{
    // It looks like the log4j category functions do not include a function
    //  to query how many categories exist, so a maximum of 100 categories
    //  is assumed to be sufficient here.
    const int a_ncats = 100;
    log4c_category_t* a_cats[a_ncats];

    int i;
    unsigned j;
    int index;
    int count;
    char msgBuff[1024];
    int priority;

    // build a table of priority strings to nicely match the output of the
    //  mpe debug code
    typedef struct
    {
        log4c_priority_level_t priority;
        char* string;
    } priority_item_t;

    priority_item_t priority_table[] =
    {
    { LOG4C_PRIORITY_FATAL, "FATAL" },
    { LOG4C_PRIORITY_ERROR, "FATAL ERROR" },
    { LOG4C_PRIORITY_WARN, "FATAL ERROR WARNING" },
    { LOG4C_PRIORITY_INFO, "FATAL ERROR WARNING INFO" },
    { LOG4C_PRIORITY_DEBUG, "FATAL ERROR WARNING INFO DEBUG" },
    { LOG4C_PRIORITY_TRACE, "FATAL ERROR WARNING INFO DEBUG TRACE" },
    { LOG4C_PRIORITY_NOTSET, "NOTSET" },
    { LOG4C_PRIORITY_UNKNOWN, "UNKNOWN" },
    { LOG4C_PRIORITY_UNKNOWN, "BOGUS" } };

    // get the log4c categories
    count = log4c_category_list(a_cats, a_ncats);

    // if the call was successful...
    if (-1 != count)
    {
        // ...for each category...
        for (i = 0; i < count; i++)
        {
            // ... get category name
            const char* name = log4c_category_get_name(
                    (log4c_category_t*) a_cats[i]);

            // get category priority
            priority = log4c_category_get_chainedpriority(
                    (log4c_category_t*) a_cats[i]);

            // find the output string for the category (initialize index to
            //  something safe first)
            index = sizeof(priority_table) / sizeof(priority_table[0]) - 1;
            for (j = 0; j < sizeof(priority_table) / sizeof(priority_table[0]); j++)
            {
                if (priority_table[j].priority == priority)
                {
                    // the category priority has been found, so save it
                    index = j;
                    break;
                }
            }

            // format a string for output
            (void) g_snprintf(msgBuff, sizeof(msgBuff) / sizeof(msgBuff[0]),
                    "%-30s: %s\n", name, priority_table[index].string);

            // spit it out into the log
            injectNoticeMessageInLoggerOutput(msgBuff);
        }
    }
}

/**
 * Instantiate the logger...
 * @param cat The category string for this instance logging/filtering
 * @return the result of the instantiation
 */
int initLogger(char *category)
{
    /**
     * ensure that the glib threading system is up...
     */
    if (!g_thread_supported())
    {
        g_thread_init( NULL);
    }

    // Initialize our log buffer semaphore
    logger.logBufferMutex = g_mutex_new();
    logger.logBufferCond = g_cond_new();
    logger.bufferAvailable = TRUE;

    // These must be set before calling log4c_init so that the log4crc file
    // will configure them
    (void) log4c_appender_type_set(&log4c_appender_type_stream_env);
    (void) log4c_appender_type_set(&log4c_appender_type_stream_env_append);
    (void) log4c_appender_type_set(&log4c_appender_type_stream_env_plus_stdout);
    (void) log4c_appender_type_set(
            &log4c_appender_type_stream_env_append_plus_stdout);
    (void) log4c_appender_type_set(&log4c_appender_type_socket_env);
    (void) log4c_layout_type_set(&log4c_layout_type_dated_nocr);
    (void) log4c_layout_type_set(&log4c_layout_type_basic_nocr);

    if (log4c_init())
    {
        fprintf(stderr, "log4c_init() failed?!");
        return -1;
    }
    else
    {
        defaultCategory = log4c_category_get(category);
        glibCategory = log4c_category_get("RI.GLib");

        // inject initial category and logging levels into the log file
        injectNoticeMessageInLoggerOutput("\n");
        injectNoticeMessageInLoggerOutput(
                "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n");
        injectNoticeMessageInLoggerOutput("Platform category logging levels:\n");
        dumpCategoryInfo();
        injectNoticeMessageInLoggerOutput(
                "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n\n");
    }

    return 0;
}

/**
 * Free the logger instantiation;
 * @param none
 * @return the result of the closure
 */
int freeLogger()
{
    if(log4c_fini())
    {
        fprintf(stderr, "log4c_fini() failed?!");
        return -1;
    }

    g_mutex_free(logger.logBufferMutex);
    g_cond_free(logger.logBufferCond);

    return 0;
}

/**
 * Output INFO log messages on behalf of the glib lib
 * @param format Format string for the actual log message
 * @param ... Possible parameters to the format string
 */
void rilog_info_printf(const char *string)
{
    RILOG_GLIB_DEBUG(string);
}

/**
 * Output ERROR log messages on behalf of the glib lib
 * @param format Format string for the actual log message
 * @param ... Possible parameters to the format string
 */
void rilog_error_printf(const char *string)
{
    RILOG_GLIB_ERROR(string);
}

#if 0
// Call this in your custom layout format functions to lock the
// log message buffer before you write to it
static void lockLogBuffer()
{
    g_mutex_lock(logger.logBufferMutex);
    while (!logger.bufferAvailable)
        g_cond_wait(logger.logBufferCond, logger.logBufferMutex);

    logger.bufferAvailable = FALSE;
    g_mutex_unlock(logger.logBufferMutex);
}

// Call this in your custom appender functions to unlock the log
// message buffer after you have appended its contents
static void unlockLogBuffer()
{
    g_mutex_lock(logger.logBufferMutex);
    logger.bufferAvailable = TRUE;
    g_cond_signal(logger.logBufferCond);
    g_mutex_unlock(logger.logBufferMutex);
}
#endif

/****************************************************************
 * Dated layout with no ending carriage return / line feed
 */
static const char* dated_format_nocr(const log4c_layout_t* a_layout,
        const log4c_logging_event_t*a_event)
{
    char *rendered_msg = g_try_malloc(MAX_LOGLINE_LENGTH+1);

    if (NULL == rendered_msg)
    {
        fprintf(stderr, "\nline %d of %s %s memory allocation of %d failure!\n",
                    __LINE__, __FILE__, __func__, MAX_LOGLINE_LENGTH+1);
        return "------- ERROR: couldn't allocate memory for log message!?\r\n";
    }

#ifndef _WIN32
    struct tm tm;
    localtime_r(&a_event->evt_timestamp.tv_sec, &tm);
    (void) snprintf(rendered_msg, MAX_LOGLINE_LENGTH,
            "%04d%02d%02d %02d:%02d:%02d.%03ld %-8s %s- %s", tm.tm_year + 1900,
            tm.tm_mon + 1, tm.tm_mday, tm.tm_hour, tm.tm_min, tm.tm_sec,
            a_event->evt_timestamp.tv_usec / 1000, log4c_priority_to_string(
                    a_event->evt_priority), a_event->evt_category,
            a_event->evt_msg);
#else
    SYSTEMTIME stime;
    FILETIME ftime;

    if ( FileTimeToLocalFileTime(&a_event->evt_timestamp, &ftime))
    {
        if ( FileTimeToSystemTime(&ftime, &stime))
        {

            (void)snprintf(rendered_msg, MAX_LOGLINE_LENGTH, "%04d%02d%02d %02d:%02d:%02d.%03d %-8s %s- %s",
                    stime.wYear, stime.wMonth , stime.wDay,
                    stime.wHour, stime.wMinute, stime.wSecond,
                    stime.wMilliseconds,
                    log4c_priority_to_string(a_event->evt_priority),
                    a_event->evt_category, a_event->evt_msg);
        }
    }
#endif
    return rendered_msg;
}

/****************************************************************
 * Basic layout with no ending carriage return / line feed
 */
static const char* basic_format_nocr(const log4c_layout_t* a_layout,
        const log4c_logging_event_t* a_event)
{
    char *rendered_msg = g_try_malloc(MAX_LOGLINE_LENGTH+1);

    if (NULL == rendered_msg)
    {
        fprintf(stderr, "\nline %d of %s %s memory allocation of %d failure!\n",
                    __LINE__, __FILE__, __func__, MAX_LOGLINE_LENGTH+1);
        return "------- ERROR: couldn't allocate memory for log message!?\r\n";
    }

    (void) snprintf(rendered_msg, MAX_LOGLINE_LENGTH, "%-8s %s - %s",
            log4c_priority_to_string(a_event->evt_priority),
            a_event->evt_category, a_event->evt_msg);

    return rendered_msg;
}

/****************************************************************
 * Stream layout that will parse environment variables from the
 * stream name (env vars have a leading "$(" and end with )"
 */
static int stream_env_open(log4c_appender_t* this, int append)
{
    FILE* fp = log4c_appender_get_udata(this);
    char* name = g_strdup(log4c_appender_get_name(this));
    int nameLen = strlen(name);
    char* temp = name;
    char *varBegin, *varEnd;
    char* envVar;
    const int MAX_VAR_LEN = 1024;
    char newName[MAX_VAR_LEN+1];
    int newNameLen = 0;

    if (fp)
    return 0;

    newName[0] = '\0';

    // Parse any environment variables
    while ((varBegin = strchr(temp,'$')) != NULL)
    {
        // Search for opening and closing parens
        if (((varBegin - name + 1) >= nameLen) || (*(varBegin+1) != '('))
        goto parse_error;

        // Append characters up to this point to the new name
        strncat(newName, temp, varBegin-temp);
        newNameLen += varBegin-temp;
        if (newNameLen > MAX_VAR_LEN)
        goto length_error;

        varBegin += 2; // start of env var name

        if ((varEnd = strchr(varBegin,')')) == NULL)
        goto parse_error;

        *varEnd = '\0';
        if ((envVar = getenv(varBegin)) == NULL)
        goto parse_error;

        // Append env var value to the new name
        strncat(newName, envVar, strlen(envVar));
        newNameLen += strlen(envVar);
        if (newNameLen >MAX_VAR_LEN)
        goto length_error;

        temp = varEnd + 1;
    }

    // Append remaining characters
    strncat(newName, temp, (name + nameLen) - temp);
    newNameLen += (name + nameLen) - temp;
    if (newNameLen >MAX_VAR_LEN)
    goto length_error;

    g_free(name);

    if (!strcmp(newName,"stderr"))
    fp = stderr;
    else if (!strcmp(newName,"stdout"))
    fp = stdout;
    else if (append)
    {
        if ((fp = fopen(newName, "a")) == NULL)
        return -1;
    }
    else
    {
        if ((fp = fopen(newName, "w")) == NULL)
        return -1;
    }

    /* unbuffered mode */
    setbuf(fp, NULL);

    (void)log4c_appender_set_udata(this, fp);
    return 0;

    parse_error:
    fprintf(stderr, "*(*(*(*( log4c appender stream_env, %s -- Illegal env var name or format! %s\n",
            __FUNCTION__, name);
    (void)fflush(stderr);
    g_free(name);
    return -1;

    length_error:
    fprintf(stderr, "*(*(*(*( log4c appender stream_env, %s -- Path is too long! %s\n",
            __FUNCTION__, name);
    (void)fflush(stderr);
    g_free(name);
    return -1;
}

static int stream_env_overwrite_open(log4c_appender_t* this)
{
    return stream_env_open(this, 0);
}

static int stream_env_append_open(log4c_appender_t* this)
{
    return stream_env_open(this, 1);
}

static int stream_env_append(log4c_appender_t* this,
        const log4c_logging_event_t* a_event)
{
    int retval;
    FILE* fp = log4c_appender_get_udata(this);

    retval = fprintf(fp, "%s", a_event->evt_rendered_msg);
    (void)fflush(fp);

    g_free((void *)a_event->evt_rendered_msg);

    return retval;
}

static int stream_env_plus_stdout_append(log4c_appender_t* this,
        const log4c_logging_event_t* a_event)
{
    int retval;
    FILE* fp = log4c_appender_get_udata(this);

    retval = fprintf(fp, "%s", a_event->evt_rendered_msg);
    fprintf(stdout, "%s", a_event->evt_rendered_msg);

    (void)fflush(fp);
    (void)fflush(stdout);

    g_free((void *)a_event->evt_rendered_msg);

    return retval;
}

static int stream_env_close(log4c_appender_t* this)
{
    FILE* fp = log4c_appender_get_udata(this);

    if (!fp || fp == stdout || fp == stderr)
    return 0;

    return fclose(fp);
}

static int socket_env_open(log4c_appender_t* this)
{
    int retval = 0;
    char* name = g_strdup(log4c_appender_get_name(this));
    int nameLen = strlen(name);
    char* temp = name;
    char *varBegin, *varEnd;
    char* envVar;
    const int MAX_VAR_LEN = 1024;
    char newName[MAX_VAR_LEN+1];
    int newNameLen = 0;

    newName[0] = '\0';

    // Parse any environment variables.
    while ((varBegin = strchr(temp,'$')) != NULL)
    {
        // Search for opening and closing parens.
        if (((varBegin - name + 1) >= nameLen) || (*(varBegin+1) != '('))
        goto parse_error;

        // Append characters up to this point to the new name.
        strncat(newName, temp, varBegin-temp);
        newNameLen += varBegin-temp;
        if (newNameLen > MAX_VAR_LEN)
        goto length_error;

        varBegin += 2; // Start of env var name.

        if ((varEnd = strchr(varBegin,')')) == NULL)
        goto parse_error;

        *varEnd = '\0';
        if ((envVar = getenv(varBegin)) == NULL)
        goto parse_error;

        // Append env var value to the new name.
        strncat(newName, envVar, strlen(envVar));
        newNameLen += strlen(envVar);
        if (newNameLen >MAX_VAR_LEN)
        goto length_error;

        temp = varEnd + 1;
    }

    // Append remaining characters.
    strncat(newName, temp, (name + nameLen) - temp);
    newNameLen += (name + nameLen) - temp;
    if (newNameLen >MAX_VAR_LEN)
    goto length_error;

    // Call the socket client implementation.
    if (strcmp(newName, "socket"))
    // Must be specifying the configuration directory for the socket.host file.
    (void)set_socket_conf_dir(newName);
    socket_open_log(name, 0);

    g_free(name);

    return retval;

    parse_error:
    fprintf(stderr, "*(*(*(*( log4c appender socket_env, %s -- Illegal env var name or format! %s\n",
            __FUNCTION__, name);
    (void)fflush(stderr);
    g_free(name);
    return -1;

    length_error:
    fprintf(stderr, "*(*(*(*( log4c appender socket_env, %s -- Path is too long! %s\n",
            __FUNCTION__, name);
    (void)fflush(stderr);
    g_free(name);
    return -1;
}

static int socket_env_append(log4c_appender_t* this, const log4c_logging_event_t* a_event)
{
    int retval = 0;

    // Call the socket client implementation.
    socket_append_msg("%s", a_event->evt_rendered_msg);

    g_free((void *)a_event->evt_rendered_msg);

    return retval;
}

static int socket_env_close(log4c_appender_t* this)
{
    // Call the socket client implementation.
    socket_close_log();

    return 0;
}

void hex_dump(void *data, int size)
{
    unsigned char *p = data;
    unsigned char c;
    int n;
    char bytestr[4];
    char addrstr[10];
    char hexstr[16 * 3 + 5];
    char charstr[16 * 1 + 5];

    if (NULL == p)
    {
        return;
    }

    memset(bytestr, 0, 4);
    memset(addrstr, 0, 10);
    memset(hexstr, 0, 16 * 3 + 5);
    memset(charstr, 0, 16 * 1 + 5);

    for (n = 1; n <= size; n++)
    {
        if (n % 16 == 1)
        {
            /* store address for this line */
            (void) snprintf(addrstr, sizeof(addrstr), "%.4x", ((unsigned int) p
                    - (unsigned int) data));
        }

        c = *p;

        if (! (c >= ' ' && c <= '~'))
        {
            c = '.';
        }

        /* store hex str (for left side) */
        (void) snprintf(bytestr, sizeof(bytestr), "%02X ", *p);
        strncat(hexstr, bytestr, sizeof(hexstr) - strlen(hexstr) - 1);

        /* store char str (for right side) */
        (void) snprintf(bytestr, sizeof(bytestr), "%c", c);
        strncat(charstr, bytestr, sizeof(charstr) - strlen(charstr) - 1);

        if (n % 16 == 0)
        {
            /* line completed */
            printf("[%4.4s]   %-50.50s  %s\n", addrstr, hexstr, charstr);
            hexstr[0] = 0;
            charstr[0] = 0;
        }
        else if (n % 8 == 0)
        {
            /* half line: add whitespaces */
            strncat(hexstr, "  ", sizeof(hexstr) - strlen(hexstr) - 1);
            strncat(charstr, " ", sizeof(charstr) - strlen(charstr) - 1);
        }

        p++; /* next byte */
    }

    if (strlen(hexstr) > 0)
    {
        /* print rest of buffer if not empty */
        printf("[%4.4s]   %-50.50s  %s\n", addrstr, hexstr, charstr);
    }
}

/**
 *   ri_errorToString: method called to return a string representation of a
 *                     ri_error enumeration for logging, printing, etc.
 * @param riErrorCode: the ri_error code to convert to a string
 *           @returns: the resultant string
 */
char *ri_errorToString(ri_error riErrorCode)
{
    switch(riErrorCode)
    {
        case RI_ERROR_NONE:
            return "RI_ERROR_NONE";
        case RI_ERROR_GENERAL:
            return "RI_ERROR_GENERAL";
        case RI_ERROR_NOT_IMPLEMENTED:
            return "RI_ERROR_NOT_IMPLEMENTED";
        case RI_ERROR_ILLEGAL_ARG:
            return "RI_ERROR_ILLEGAL_ARG";
        case RI_ERROR_OUT_OF_RESOURCES:
            return "RI_ERROR_OUT_OF_RESOURCES";
        case RI_ERROR_TUNE_IN_PROGRESS:
            return "RI_ERROR_TUNE_IN_PROGRESS";
        case RI_ERROR_FILTER_NOT_AVAILABLE:
            return "RI_ERROR_FILTER_NOT_AVAILABLE";
        case RI_ERROR_INVALID_TUNE_REQUEST:
            return "RI_ERROR_INVALID_TUNE_REQUEST";
        case RI_ERROR_INVALID_FREQUENCY:
            return "RI_ERROR_INVALID_FREQUENCY";
        case RI_ERROR_INVALID_MODULATION_MODE:
            return "RI_ERROR_INVALID_MODULATION_MODE";
        case RI_ERROR_ALREADY_EXISTS:
            return "RI_ERROR_ALREADY_EXISTS";
        case RI_ERROR_NO_TSB:
            return "RI_ERROR_NO_TSB";
        case RI_ERROR_NO_PLAYBACK:
            return "RI_ERROR_NO_PLAYBACK";
        case RI_ERROR_NO_CONVERSION:
            return "RI_ERROR_NO_CONVERSION";
        case RI_ERROR_RECORDING_IN_USE:
            return "RI_ERROR_RECORDING_IN_USE";
        case RI_ERROR_CABLECARD_NOT_READY:
            return "RI_ERROR_CABLECARD_NOT_READY";
        case RI_ERROR_GF_NOT_SUPPORTED:
            return "RI_ERROR_GF_NOT_SUPPORTED";
        case RI_ERROR_INVALID_SAS_APPID:
            return "RI_ERROR_INVALID_SAS_APPID";
        case RI_ERROR_CONNECTION_NOT_AVAIL:
            return "RI_ERROR_CONNECTION_NOT_AVAIL";
        case RI_ERROR_APDU_SEND_FAIL:
            return "RI_ERROR_APDU_SEND_FAIL";
        case RI_ERROR_EOS:
            return "RI_ERROR_EOS";
        default:
            return "unrecognized ri_error code!?";
    }
}

