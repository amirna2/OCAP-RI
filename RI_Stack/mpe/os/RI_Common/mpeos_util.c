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
 * This file provides the CableLabs Reference Implementation of the mpeos_ utility APIs.
 */

/* Header Files */
#include <mpe_config.h>     /* Resolve configuration type references. */
#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>      /* Resolve erroc code definitions */
#include <mpe_socket.h>
#include <mpe_file.h>       /* For removeFiles utility function */
#include <mpeos_dbg.h>      /* Resolved MPEOS_LOG support. */
#include <mpeos_sync.h>     /* Resolve generic STB config structure type. */
#include <mpeos_util.h>     /* Resolve generic STB config structure type. */
#include <mpeos_mem.h>      /* Resolve memory functions. */
#include <mpe_file.h>       /* Resolve file functions. */
#include <os_util.h>        /* Resolve target specific utility definitions. */
#include <mpeos_disp.h>     /* Resolve video ports */
#include <ri_ui_manager.h>  /* UI manager for reboot help. */
#include <ri_pipeline_manager.h>  /* Determine number of tuners. */
#include <ri_test_interface.h>

#include <ri_pipeline.h>
#include <stdlib.h>
#include <stdio.h>
#include <ctype.h>
#include <string.h>

extern void dispSetEnableOutputPort(mpe_PowerStatus);

static int utilMenuInputHandler(int sock, char *rxBuf, int *retCode, char **retStr);

#define MPE_MEM_DEFAULT MPE_MEM_UTIL

/* Node for storing env vars in a linked list cache */
typedef struct EnvVarNode
{
    char* name;
    char* value;
    struct EnvVarNode *next;
} EnvVarNode;

/* Env var cache */
static EnvVarNode *g_envCache = NULL;
static mpe_Mutex g_cacheMutex;
os_EnvConfig g_osEnvironment;

/* Cached ED queue ID for power state change notification */
static mpe_EventQueue g_powerStateQueueId = (mpe_EventQueue) - 1;

/* Cached ED handle for power state change notification */
static void *g_powerStateEdHandle = NULL;

/* Cached power state value */
static uint8_t g_powerState = MPE_POWER_FULL;
static mpe_Bool g_ignoreLowPowerModeSetting = TRUE;

/* AC outlet state */
static mpe_Bool g_acOutletState = FALSE;

/* Cached audio state value */
static uint8_t g_audioState = MPE_AUDIO_ON;

// For RI test interface (telnet interface)
#define UTIL_OPTIONS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| l | Enter a log message\r\n" \
    "|---+-----------------------\r\n" \
    "| m | Enter a MARK into the log\r\n"

static MenuItem MpeosUtilMenuItem =
{ true, "l", "Logging", UTIL_OPTIONS, utilMenuInputHandler };


/**
 * MPEOS private utility function
 * Recursively remove files and directories from the given directory path
 *
 * Returns 0 if successful, -1 otherwise
 */
mpe_Error removeFiles(char *dirPath)
{
    char path[MPE_FS_MAX_PATH];
    mpe_Dir dir;
    mpe_DirEntry dirEnt;
    mpe_Error err;

    err = mpe_dirOpen(dirPath, &dir);

    // If the directory does not exist, then we are good to go
    if (err == MPE_FS_ERROR_NOT_FOUND)
        return MPE_SUCCESS;

    if (err != MPE_SUCCESS)
        return err;

    while (mpe_dirRead(dir, &dirEnt) == MPE_SUCCESS)
    {
        /* Skip ".." and "." entries */
        if (strcmp(dirEnt.name, ".") == 0 || strcmp(dirEnt.name, "..") == 0)
            continue;

        /* Create the full path for this directory entry */
        strcpy(path, dirPath);
        strcat(path, MPE_FS_SEPARATION_STRING);
        strcat(path, dirEnt.name);

        /* Determine if this is a file or directory */
        if (dirEnt.isDir)
        {
            if (removeFiles(path) != MPE_SUCCESS)
                return MPE_FS_ERROR_FAILURE;
        }
        else if ((err = mpe_fileDelete(path)) != MPE_SUCCESS)
            return err;
    }
    mpe_dirClose(dir);

    // Finally, remove the directory itself
    err = mpe_dirDelete(dirPath);
    if (err != MPE_SUCCESS && err != MPE_FS_ERROR_NOT_FOUND)
    {
        return err;
    }

    return MPE_SUCCESS;
}

void *mpeos_atomicOperation(void *(*op)(void*), void *data)
{
    MPE_UNUSED_PARAM(op);
    MPE_UNUSED_PARAM(data);
    return NULL;
}

uint32_t mpeos_atomicIncrement(uint32_t *value)
{
    MPE_UNUSED_PARAM(value);
    return 0;
}

uint32_t mpeos_atomicDecrement(uint32_t *value)
{
    MPE_UNUSED_PARAM(value);
    return 0;
}

/**
 * <i>mpeos_setJmp()</i>
 *
 * Save the current stack context for subsequent non-local dispatch (jump).
 * The return value indicates whether the return is from the original direct
 * call to save the current stack or from an <i>mpeos_longJmp()<i/> operation
 * that restored the saved stack image contents.
 *
 * @param jmpBuf is the pointer to the "jump" buffer for saving the context
 *          information.
 *
 * @return an integer value indicating the return context.  A value of zero indicates
 *          a return from a direct call and a non-zero value indicates an indirect
 *          return (i.e. via <i>mpeos_longJmp()<i/>).
 */
int mpeos_setJmp(mpe_JmpBuf jmpBuf)
{
    return setjmp(jmpBuf);
}

/**
 * <i>mpeos_longJmp()</i>
 *
 * Perform a non-local dispatch (jump) to the saved stack context.
 *
 * @param jmpBuf is a pointer to the "jump" buffer context to restore.
 */
void mpeos_longJmp(mpe_JmpBuf jmpBuf, int val)
{
    longjmp(jmpBuf, val);
}

static void trim(char *instr, char* outstr)
{
    char *ptr = instr;
    char *endptr = instr + strlen(instr) - 1;
    int length;

    /* Advance pointer to first non-whitespace char */
    while (isspace(*ptr))
        ptr++;

    if (ptr > endptr)
    {
        /*
         * avoid breaking things when there are
         * no non-space characters in instr (JIRA OCORI-2028)
         */
        outstr[0] = '\0';
        return;
    }

    /* Move end pointer toward the front to first non-whitespace char */
    while (isspace(*endptr))
        endptr--;

    length = endptr + 1 - ptr;
    strncpy(outstr, ptr, length);
    outstr[length] = '\0';

}

/**
 * The <i>mpeos_envInit()</i> function sets up the environment variable
 * storage and lock for the mpeos_env*() methods.
 *
 * @return None.
 */
void mpeos_envInit()
{
    FILE* f;
    char lineBuffer[2048];
    char buf[20];
    char hostNameBuf[1024];
    const char *ignore_low_power_mode_setting = NULL;

    /* Open the env file */
    if ((f = fopen(g_osEnvironment.os_config, "r")) == NULL)
    {
        printf("***************************************************\n");
        printf("***************************************************\n");
        printf("**    ERROR!  Could not open mpeenv.ini file!    **\n");
        printf("***************************************************\n");
        printf("***************************************************\n");
        return;
    }

    printf("***************************************************\n");
    printf("**    Opened mpeenv.ini file!    **\n");
    printf("***************************************************\n");

    /* Read each line of the file */
    while (fgets(lineBuffer, 2048, f) != NULL)
    {
        char name[2048];
        char value[2048];
        char trimname[2048];
        char trimvalue[2048];
        EnvVarNode *node;
        char *equals;
        int length;

        /* Ignore comment lines */
        if (lineBuffer[0] == '#')
            continue;

        /* Ignore lines that do not have an '=' char */
        if ((equals = strchr(lineBuffer, '=')) == NULL)
            continue;

        /* Read the property and store in the cache */
        length = equals - lineBuffer;
        strncpy(name, lineBuffer, length);
        name[length] = '\0'; /* have to null-term */

        length = lineBuffer + strlen(lineBuffer) - (equals + 1);
        strncpy(value, equals + 1, length);
        value[length] = '\0';

        /* Trim all whitespace from name and value strings */

        trim(name, trimname);
        trim(value, trimvalue);

        if (trimvalue[0] == '\0' || trimname[0] == '\0')
        {
            continue;
        }

        node = (EnvVarNode*) malloc(sizeof(EnvVarNode));
        if (node)
        {

            node->name = strdup(trimname);
            node->value = strdup(trimvalue);

            /* Insert at the front of the list */
            node->next = g_envCache;
        }
        g_envCache = node;
    }

    // Init the mutex.
    mpeos_mutexNew(&g_cacheMutex);

    // Set up "automatic" values.
    mpeos_envSet("ocap.hardware.serialnum", "00000000");
    mpeos_envSet("ocap.hardware.vendor_id", "1");
    mpeos_envSet("ocap.hardware.version_id", "1");
    mpeos_envSet("ocap.memory.total", "1048567");
    mpeos_envSet("ocap.memory.video", "1048567");
    mpeos_envSet("ocap.system.highdef", "TRUE");
    mpeos_envSet("ocap.hardware.createdate", "January 1, 1970");
    mpeos_envSet("MPE.SYS.OSVERSION", "XP");
    mpeos_envSet("MPE.SYS.OSNAME", OS_NAME);
    mpeos_envSet("MPE.SYS.MPEVERSION", MPE_VERSION);
    mpeos_envSet("MPE.SYS.OCAPVERSION", OCAP_VERSION);
    mpeos_envSet("MPE.SYS.AC.OUTLET", "TRUE");

    if (mpeos_envGet("MPE.SYS.RFMACADDR") == NULL)
    {
        os_getMacAddress(NULL, buf);
        mpeos_envSet("MPE.SYS.RFMACADDR", buf);
    }

    if (mpeos_envGet("MPE.SYS.ID") == NULL)
    {
        /* Set the unique system ID to the RF MAC Address (withouth the ':' characters) */
        const char * mac = mpeos_envGet("MPE.SYS.RFMACADDR");
        if (mac != NULL)
        {
            // strip out the ':' characters
            int i = 0, j = 0;
            do
                if (mac[i] != ':')
                    buf[j++] = mac[i];
            while (mac[i++]);
            mpeos_envSet("MPE.SYS.ID", buf);
        }
    }

    // Get the system IP address, defaulting to 0.0.0.0
    if (mpeos_socketGetHostName(hostNameBuf, 1024) != 0)
    {
        mpe_SocketHostEntry* hostEntry = mpeos_socketGetHostByName(hostNameBuf);
        if (hostEntry != NULL)
        {
            sprintf(buf, "%d.%d.%d.%d", hostEntry->h_addr_list[0][0],
                    hostEntry->h_addr_list[0][1], hostEntry->h_addr_list[0][2],
                    hostEntry->h_addr_list[0][3]);
        }
    }
    else
    {
        strcpy(buf, "0.0.0.0");
    }

    // Add the hostname to the default list.
    mpeos_envSet("MPE.SYS.IPADDR", buf);

    // Set the number of tuners.
    // First attempt to retrieve the number of tuners from mpeenv.ini.
    if (mpeos_envGet("MPE.SYS.NUM.TUNERS") == NULL)
    {
        // Not set, so retrieve the number of tuners from the target platform.
        int numTuners = os_getNumTuners();
        if (numTuners > 0)
        {
            sprintf(buf, "%d", numTuners);
            mpeos_envSet("MPE.SYS.NUM.TUNERS", buf);
        }
        else
            // Default to a single tuner.
            mpeos_envSet("MPE.SYS.NUM.TUNERS", "1");
    }

    ignore_low_power_mode_setting = mpeos_envGet("MPE.SYS.IGNORELOWPOWER");
    if ((NULL == ignore_low_power_mode_setting) || (stricmp(ignore_low_power_mode_setting, "TRUE") == 0))
    {
        g_ignoreLowPowerModeSetting = 1;
    }
    else
    {
        g_ignoreLowPowerModeSetting = 0;
    }
    printf("mpeos_envInit: MPE.SYS.IGNORELOWPOWER set to %s\n", (g_ignoreLowPowerModeSetting ? "ON" : "OFF"));

    fclose(f);

    ri_test_RegisterMenu(&MpeosUtilMenuItem);
}

/**
 * The <i>mpeos_envGet()</i> function will get the value of the specified
 * environment variable.
 *
 * @param name is a pointer to the name of the target environment variable.
 * @param buf is a pointer to the buffer to copy the variable value to.
 * @param len is a pointer to the maximum number of bytes to copy to the
 *          buffer.  Also, the number of bytes actually copies is returned
 *          via this pointer.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
const char* mpeos_envGet(const char *name)
{
    EnvVarNode *node = g_envCache;

    mpeos_mutexAcquire(g_cacheMutex);

    while (node != NULL)
    {
        /* Env var name match */
        if (strcmp(name, node->name) == 0)
        {
            /* return the value */
            mpeos_mutexRelease(g_cacheMutex);
            return node->value;
        }

        node = node->next;
    }

    /* Not found */
    mpeos_mutexRelease(g_cacheMutex);
    return NULL;
}

/**
 * The <i>mpeos_envSet()</i> function will set the value of the specified
 * environment variable.
 *
 * @param name is a pointer to the name of the target environment variable.
 * @param value is a pointer to a NULL terminated value string.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_envSet(char *name, char *value)
{
    EnvVarNode *node = g_envCache;

    mpeos_mutexAcquire(g_cacheMutex);

    /* Search for an existing entry */
    while (node != NULL)
    {
        if (strcmp(name, node->name) == 0)
        {
            /* Free the existing value and store the new one */
            free(node->value);
            node->value = strdup(value);
            mpeos_mutexRelease(g_cacheMutex);
            return MPE_SUCCESS;
        }

        node = node->next;
    }

    /* Not found, so create a new entry */
    node = (EnvVarNode*) malloc(sizeof(EnvVarNode));
    if (node)
    {
        node->name = strdup(name);
        node->value = strdup(value);

        /* Place at the front of the list */
        node->next = g_envCache;
    }
    g_envCache = node;

    mpeos_mutexRelease(g_cacheMutex);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbGetPowerStatus()</i>
 *
 * Get the current STB power status.
 *
 * @return unsigned integer representing the current power mode status.
 */
mpe_PowerStatus mpeos_stbGetPowerStatus(void)
{
    return g_powerState; /* Return the power mode. */
}

/**
 * <i>mpeos_stbGetAudioStatus()</i>
 *
 * Get the current STB audio status.
 *
 * @return unsigned integer representing the current audio status.
 */
mpe_AudioStatus mpeos_stbGetAudioStatus(void)
{
    return g_audioState; /* Return the audio state. */
}

/**
 * <i>mpeos_registerForPowerKey()</i>
 *
 * Registers the queue ID and the Event Dispatcher handle to be used
 * to notify a power state change. The values are cached in global variables
 * and used by <i>togglePowerMode</i> when power key is detected.
 *
 * @param qId specifies the ID of the queue to be used for events notification.
 * @param act the Event Dispatcher handle to be used for events notification.
 *
 * @return MPE_SUCCESS, perhaps the call should fail if someone has already registered
 */
mpe_Error mpeos_registerForPowerKey(mpe_EventQueue qId, void *act)
{
    g_powerStateQueueId = qId;
    g_powerStateEdHandle = act;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbBoot()</i>
 *
 * Service various "bootstrap" related requests.  The bootstrap operations consist of:
 * <ul>
 * <li> 1. Reboot the entire STB (equivilent to power-on reset).
 * <li> 2. Fast boot 2-way mode (forward & reverse data channels enabled).
 * </ul>
 *
 */
mpe_Error mpeos_stbBoot(mpe_STBBootMode code)
{
    ri_ui_manager_t* ui_mgr = NULL;
    switch (code)
    {
    case MPE_BOOTMODE_RESET:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                "mpeos_stbBoot: resetting the stack\n");

        // Get the UI manager and ask for a reset
        ui_mgr = ri_get_ui_manager();
        if (ui_mgr != NULL)
        {
            printf("*+*+*+*+*+*+*+* calling platform_reset *+*+*+*+*+*+*+*\n");
            ui_mgr->platform_reset();
        }
        else
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                    "mpeos_stbBoot: Could not retreive UI manager, skipping platform reset\n");
        }
        return MPE_SUCCESS;

    case MPE_BOOTMODE_FAST:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                "mpeos_stbBoot: fast boot for 2-way network invoked\n");
        return MPE_SUCCESS;

    default:
        return MPE_EINVAL;
    }
}

/**
 * <i>mpeos_stbBootStatus</i>
 *
 * Get/Update the current system bootstrap status.  This is an mpeos_ API to allow the MPE
 * and MPEOS layers to query or update the boot status.  The boot status information is
 * simply a 32-bit value with each bit defined to represent a "stage" of the boot process or
 * the "availability" of a resource (e.g. network status).
 *
 * If the "update" flag is TRUE the call is an update call and the second parameter contains
 * the additional boot status information to be added to the current status.  The third
 * parameter is used to optionally clear any status bit information for any status that
 * may have changed.  If any bits are not to be cleared 0xFFFFFFFF should be passed for
 * the bit mask value.
 *
 * @param update is a boolean indicating a call to update the boot status
 * @param statusBits is a 32-bit value representing the new status update information
 *        the bits are logically ORed with the current boot status.
 * @param bitMask is a bit pattern for optionally clearing any particular status buts.
 *
 * @return a 32-bit bit pattern indicating the status of the various elements
 *         of the system.
 *
 * TODO: fill out status of OCAP stack (add calls to this routine). The OCAP stack status
 *       could include MPE managers installed, JVM started, etc.
 */
uint32_t mpeos_stbBootStatus(mpe_Bool update, uint32_t statusBits,
        uint32_t bitMask)
{
    static uint32_t bootStatus = 0;

    /* Check for updating status. */
    if (TRUE == update)
    {
        bootStatus &= bitMask; /* Clear any selected status bits. */
        bootStatus |= statusBits; /* Set new bits. */
    }

    /* Return bootstrap status. */
    return bootStatus;
}

/**
 * <i>mpeos_stbGetAcOutletState()</i>
 *
 * Get the current AC outlet state.
 *
 * @param value indicating the current state of the external AC outlet.
 *
 * @return MPE_SUCCESS if the outlet state was successfully retrieved
 * @return MPE_EINVAL if the start parameter is invalid
 */
mpe_Error mpeos_stbGetAcOutletState(mpe_Bool *state)
{
    if (NULL == state)
        return MPE_EINVAL;
    *state = g_acOutletState;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbSetACOutletState()</i>
 *
 * Set the current AC outlet state .
 *
 * @param enable value indicating the new state of the AC outlet
 *
 * @return MPE_SUCCESS if the AC outlet state was set successfully
 */
mpe_Error mpeos_stbSetAcOutletState(mpe_Bool enable)
{
    g_acOutletState = enable;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbGetRootCerts()</i>
 *
 * Acquire the initial set of root certificates for the platform. The format of
 * the root certificate(s) must be in either raw "binary" or base64 encoding. In
 * the latter case each certificate must be properly delineated with the
 * '-----BEGIN CERTIFICATE-----' and '-----END CERTIFICATE-----' separators.
 * Essentially, the byte array must be in a format suitable for use with the
 * java.security.cert.CertificateFactory.generateCertificate(ByteArrayInputStream)
 * method.
 *
 * @param roots is a pointer for returning a pointer to the memory location
 *        containing the platform root certificate(s).
 * @param len is a pointer for returning the size (in bytes) of the memory location
 *        containing the roots.
 *
 * @return MPE_SUCCESS if the pointer to and length of the root certificate image
 *         was successfully acquired.
 */
mpe_Error mpeos_stbGetRootCerts(uint8_t **roots, uint32_t *len)
{
    static uint8_t *staticRoots = NULL;
    char *rootFile;
    mpe_File h;
    mpe_FileInfo info;
    uint32_t fileSize;
    uint32_t size;

    /* Check for root certs already acquired. */
    if (staticRoots == NULL)
    {
        /* Get the name of the root certs file. */
        if ((rootFile = (char*) mpeos_envGet("MPE.ROOTCERTS")) == NULL)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                    "Can't retrieve MPE_ROOTCERTS environment variable.\n");
            return MPE_ENODATA;
        }

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                "Attempting to open root certificate file: %s.\n", rootFile);

        /* Open the root certs file. */
        if (mpe_fileOpen(rootFile, MPE_FS_OPEN_READ, &h)
                != MPE_FS_ERROR_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                    "Can't open root certificate file: %s.\n", rootFile);
            return MPE_ENODATA;
        }

        /* Get the size of the file. */
        if ((mpe_fileGetFStat(h, MPE_FS_STAT_SIZE, &info)
                != MPE_FS_ERROR_SUCCESS) || (info.size == 0))
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                    "Can't get size of root certificate file: %s.\n", rootFile);
            mpe_fileClose(h);
            return MPE_ENODATA;
        }
        else
            fileSize = info.size;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                "Root certificate file size is %d.\n", (int) fileSize);

        /* Allocate static buffer for root certs. */
        size = *len = (uint32_t)(fileSize & 0xFFFFFFFFL);
        if (mpeos_memAllocP(MPE_MEM_UTIL, size, (void**) &staticRoots)
                != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                    "Can't allocate root certificate memory of size %d.\n",
                    size);
            mpe_fileClose(h);
            staticRoots = NULL;
            return MPE_ENODATA;
        }

        /* Read root certs into static buffer. */
        if (mpe_fileRead(h, len, staticRoots) != MPE_FS_ERROR_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                    "Can't read root certificate file: %s.\n", rootFile);
            mpe_fileClose(h);
            mpeos_memFreeP(MPE_MEM_UTIL, staticRoots);
            staticRoots = NULL;
            return MPE_ENODATA;
        }
        mpe_fileClose(h);
    }
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
            "Certificate file acquired, size = %d.\n", *len);

    /* Return root certs. */
    *roots = staticRoots;
    return MPE_SUCCESS;
}

/**
 * Initialize the porting environment configuration.
 *
 * @param env A pointer to the structure containing the information
 * necessary for intiializing the ports environment.
 */
void os_envInit(os_EnvConfig *env)
{
    if (env != NULL)
    {
        int i;

        g_osEnvironment.os_config = strdup(env->os_config);
        g_osEnvironment.os_numModules = env->os_numModules;
        g_osEnvironment.os_modulePath = (char **) malloc(env->os_numModules
                * sizeof(char *));
        for (i = 0; i < env->os_numModules; i++)
        {
            char *path = env->os_modulePath[i];
            g_osEnvironment.os_modulePath[i] = strdup(path);
        }
    }
}

/**
 * <i>mpeos_stbSetSystemMuteKeyControl</i>
 *
 * Enables/disabled system handling of Mute key. If disabled, the OCAP 
 * apps rather than the system are responsible for handling the mute functionality. 
 * In both cases, mute key events are sent to OCAP applications.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param enable, true if system muting control is enabled.  False if not.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetSystemMuteKeyControl(mpe_Bool enable)
{
    /* TODO: not yet implemented */
    MPE_UNUSED_PARAM(enable);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbSetSystemVolumeKeyControl</i>
 *
 * Enables/disabled system handling of Volume keys. If disabled, the OCAP 
 * apps rather than the system are responsible for handling the volume change. 
 * In both cases, volume key events are sent to OCAP applications.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param enable, true if system volume control is enabled.  False if not.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetSystemVolumeKeyControl(mpe_Bool enable)
{
    /* TODO: not yet implemented */
    MPE_UNUSED_PARAM(enable);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbSetSystemVolumeRange</i>
 *
 * Sets the overall audio range of the audio ports.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @param range, one of three values:  RANGE_NORMAL, RANGE_NARROW, and RANGE_WIDE.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetSystemVolumeRange(uint32_t range)
{
    /* TODO: not yet implemented */
    MPE_UNUSED_PARAM(range);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbResetAllDefaults</i>
 *
 * Resets all STB defaults.
 *
 * DSExt functionality.
 *
 * NOT IMPLEMENTED
 *
 * @return MPE_SUCCESS
 */

mpe_Error mpeos_stbResetAllDefaults(void)
{
    /* TODO: not yet implemented */
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_stbSetPowerStatus</i>
 *
 * Sets the power state to the value passed in.  Does not move the power state through a state machine,
 * just sets the value and notifies listeners.
 * <p>
 * DSExt functionality.
 * </p>
 *
 * @param newPowerMode, one of the mpe_PowerStatus values.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetPowerStatus(mpe_PowerStatus newPowerMode)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_UTIL,
            "mpeos_stbSetPowerStatus: power mode has changed from %s to %s\n",
            ((g_powerState == MPE_POWER_FULL) ? "MPE_POWER_FULL"
                    : "MPE_POWER_STANDBY"),
            ((newPowerMode == MPE_POWER_FULL) ? "MPE_POWER_FULL"
                    : "MPE_POWER_STANDBY"));

    g_powerState = newPowerMode;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_UTIL,
            "mpeos_stbSetPowerStatus: g_ignoreLowPowerModeSetting: %d" , g_ignoreLowPowerModeSetting);

    if(!g_ignoreLowPowerModeSetting)
    {
        // If the ini setting is set to NOT ignore low power state
        // we need to un-block/block video/graphics
        // depending on the previous state
        ri_display_t* display = NULL;
        ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
        if (NULL != pMgr)
        {
            display = pMgr->get_display(pMgr);
            if (NULL != display)
            {
                mpe_Bool block = (g_powerState == MPE_POWER_STANDBY);
                display->block_display(display, (ri_bool) block);
            }
        }
    }

    /* If somebody is listening, let them know that the power state has changed. */
    if (g_powerStateEdHandle != NULL)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
                "Push power key event into ED queue\n");
        mpeos_eventQueueSend(g_powerStateQueueId, g_powerState, 0,
                (void*) g_powerStateEdHandle, 0);
    }

    return MPE_SUCCESS;
}

/**
 * <i>togglePowerMode()</i>
 *
 * Called when the power key is pressed. Used to push the power state change
 * events into the Event Dispatcher queue. Currently the supported power
 * states are MPE_POWER_FULL and MPE_POWER_STANDBY.
 */
void togglePowerMode(void)
{
    if (g_powerState == MPE_POWER_FULL)
        (void) mpeos_stbSetPowerStatus(MPE_POWER_STANDBY);
    else
        (void) mpeos_stbSetPowerStatus(MPE_POWER_FULL);
}

/**
 * <i>mpeos_stbSetAudioStatus</i>
 *
 * Sets the audio state to the value passed in.
 * <p>
 * DSExt functionality.
 * </p>
 *
 * @param newAudioMode, one of the mpe_AudioStatus values.
 *
 * @return MPE_SUCCESS
 */
mpe_Error mpeos_stbSetAudioStatus(mpe_AudioStatus newAudioMode)
{

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
            "mpeos_stbSetAudioStatus: audio mode has changed from %s to %s\n",
            ((g_audioState == MPE_AUDIO_ON) ? "MPE_AUDIO_ON"
                    : "MPE_AUDIO_MUTED"),
            ((newAudioMode == MPE_AUDIO_ON) ? "MPE_AUDIO_ON"
                    : "MPE_AUDIO_MUTED"));

    g_audioState = newAudioMode;

    return MPE_SUCCESS;
}

/**
 * <i>os_getNumTuners</i>
 *
 * MPEOS private utility for retrieving the number of tuners from the underlying
 * target platform.
 */
int os_getNumTuners(void)
{
    unsigned int numTuners = 0;

    // There is a 1-to-1 association between a RI Platform pipeline and a tuner.
    // If this association changes, the number of tuners may be extracted from the
    // RI platform.cfg file instead.
    ri_pipeline_manager_t *mgr = ri_get_pipeline_manager();
    (void) mgr->get_live_pipelines(mgr, &numTuners);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UTIL,
            "os_getNumTuners: %d tuners found in target platform.\n", numTuners);

    return numTuners;
}

static int utilMenuInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_MEDIA, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    static int markNumber = 0;
    static char messageBuf[512];

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_UTIL, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    switch (rxBuf[0])
    {
        case 'm':
        {
            markNumber++;
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_UTIL,
                      "%s - MARK #%d\n", __FUNCTION__, markNumber);
            snprintf(messageBuf, sizeof(messageBuf), "\r\n\r\nEntered MARK #%d into the log\r\n", markNumber);
            ri_test_SendString(sock, messageBuf);
            break;
        }
        case 'l':
        {
            ri_test_GetString(sock, messageBuf, sizeof(messageBuf), "\r\nLog message to enter: ");
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_UTIL,
                      "%s - USER MESSAGE: %s\n", __FUNCTION__, messageBuf);
            ri_test_SendString(sock, "\r\n\r\nLog message entered\r\n");
            break;
        }
        default:
        {
            strcat(rxBuf, " - unrecognized\r\n\n");
            ri_test_SendString(sock, rxBuf);
            *retCode = MENU_INVALID;
        }
    } // END switch (rxBuf[0])

    return 0;
} // END utilMenuInputHandler()
