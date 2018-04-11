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

// Header files.
#include <string.h>
#include <stdlib.h>

#include <ri_ui_manager.h>

#include "mpeos_mem.h"
#include "mpeos_frontpanel.h"
#include "sysmgr.h"
#include "mpeos_uievent.h"
#include "jvmmgr.h"

#include "mpeos_util.h"

#include "mpeos_dbg.h"
#include "mpeos_dll.h"
#include "mpeos_thread.h"

// Local platform UI Manager instance
static ri_ui_manager_t* ri_ui_manager = NULL;
static mpe_ThreadId stackThread;

#define ENV_PATH_SEPARATOR      ';'
#define ENV_MAX_PATH_ENTRIES    16
#define ENV_MAX_PATH_ENTRY_SIZE 4096

#ifndef MIN
#define MIN(a,b)                (((a) < (b)) ? (a) : (b))
#endif

/**
 * Parse the command-line arguments. Currently supported options:
 * --modulePath <path1>[;<path2>]
 * --config <path_to_mpeenv_ini>
 **/
static void init_env(int argc, char *argv[])
{
    int i = 0;

    os_EnvConfig env_config =
    { NULL, 0, NULL };
    const char *config_opt = "--config";
    const char *module_path_opt = "--modulePath";

    char config[ENV_MAX_PATH_ENTRY_SIZE];

    char path_entries[ENV_MAX_PATH_ENTRIES][ENV_MAX_PATH_ENTRY_SIZE];
    int path_entries_length = 0;

    // Since char** is compiled differently from char[][], we need the following
    char *path_entry_pointers[ENV_MAX_PATH_ENTRIES];
    char **path_entries_pointer = &path_entry_pointers[0];
    for (i = 0; i < ENV_MAX_PATH_ENTRIES; i++)
    {
        path_entry_pointers[i] = &path_entries[i][0];
    }

    for (i = 0; i < argc; i++)
    {
        if (argv[i] == NULL)
        {
            continue;
        }
        else if (strncmp(config_opt, argv[i], sizeof(config_opt)) == 0)
        {
            if (++i >= argc)
            {
                break;
            }
            else
            {
                strncpy(config, argv[i], sizeof(config));
                env_config.os_config = config;
            }
        }
        else if (strncmp(module_path_opt, argv[i], sizeof(module_path_opt))
                == 0)
        {
            if (++i >= argc)
            {
                break;
            }
            else
            {
                char *string_start = argv[i];
                char *separator_index = string_start;
                int path_entry_length = 0; // this length excludes the string null-terminator

                while ((separator_index != NULL) && (path_entries_length
                        < ENV_MAX_PATH_ENTRIES))
                {
                    separator_index = strchr(string_start, ENV_PATH_SEPARATOR);
                    if (separator_index == NULL)
                    {
                        // terminal case: last member of path list or the only member of path list
                        path_entry_length = strlen(string_start);
                    }
                    else
                    {
                        // recursive case: ';' found, keep looking for next path members
                        path_entry_length = (int) (separator_index
                                - string_start);
                    }
                    path_entry_length
                            = MIN(path_entry_length, ENV_MAX_PATH_ENTRY_SIZE - 1);
                    strncpy(path_entries[path_entries_length], string_start,
                            path_entry_length);
                    path_entries[path_entries_length][path_entry_length] = '\0';
                    path_entries_length++;
                    if (separator_index)
                        string_start = separator_index + 1;
                }
                env_config.os_modulePath = path_entries_pointer;
                env_config.os_numModules = path_entries_length;
            }
        }
    }
    os_envInit(&env_config);
}

/**
 * ProcessTests will start processing tests via the environment variable
 * PROCESS_TESTS. The values PROCESS_TESTS are as follows;
 * NATIVE (for MPE and MPEOS tests), MPE_HOST_POD, OCAP, and SIM.
 * The name of the test dll will be stored in the environment variable
 * MPE_DLL_TEST, it will default to the name OCAPNativeTest.dll if a
 * name is not provided, or the environment variable doesn't exist.
 *
 * @return TRUE if the tests where processed, else FALSE.
 */
static mpe_Bool ProcessTests()
{
    char name[128];
    char value[128];
    const char *envVar;
    static uint32_t port;
    void* functionPtr;
    mpe_Dlmod DLL;
    mpe_ThreadId testThread;
    mpe_Bool bVte = FALSE;

    memset(value, (int) '\0', 128);
    strcpy(name, "TEST_MPE");

    if ((envVar = mpeos_envGet(name)) == NULL)
    {
        // ERROR - XXX Set error
        return FALSE;
    }

    if ((0 == strcmp("true", envVar)) || (0 == strcmp("TRUE", envVar)))
    {
        // Get library name.
        envVar = mpeos_envGet("TEST.PROGRAM");
        memset(value, (int) '\0', 128);
        strcpy(value, (envVar != NULL) ? envVar : "vte_agent.dll");

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL, "TEST_PROGRAM: **%s**\n", value);

        if (MPE_SUCCESS != mpeos_dlmodOpen(value, &DLL))
        {
            // ERROR Set error
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
                    "ERROR: Could not load lib **%s**\n", value);
            return TRUE;
        }

        // Get entry point to test socket handler.
        envVar = mpeos_envGet("TEST.ENTRY");
        memset(value, (int) '\0', 128);

        if (envVar != NULL)
        {
            strcpy(value, envVar);
        }
        else
        {
            bVte = TRUE;
            strcpy(value, "vte_agent_Start");
            envVar = mpeos_envGet("VTE.PORT");
            if (envVar != NULL)
            {
                port = atoi(envVar);
            }
            else
            {
                port = 5200; /* default Vte_Agent port number */
            }
        }

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL, "TEST_ENTRY: **%s**\n", value);

        if (MPE_SUCCESS != mpeos_dlmodGetSymbol(DLL, value, &functionPtr))
        {
            // ERROR return
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DLL,
                    "ERROR: Could not load symbol **%s**\n", value);
            return TRUE;
        }

        // Run test in new thread, else mutex error in simulator.
        if (bVte)
        {
            if (MPE_SUCCESS != mpeos_threadCreate( /*lint -e(611)*/(void(*)(
                    void*)) functionPtr, (void*) &port, MPE_THREAD_PRIOR_DFLT,
                    MPE_THREAD_STACK_SIZE, &testThread, NULL))
            {
                // ERROR return
                return TRUE;
            }
        }
        else
        {
            if (MPE_SUCCESS != mpeos_threadCreate( /*lint -e(611)*/(void(*)(
                    void*)) functionPtr, NULL, MPE_THREAD_PRIOR_DFLT,
                    MPE_THREAD_STACK_SIZE, &testThread, NULL))
            {
                // ERROR return
                return TRUE;
            }
        }
        return TRUE;
    } // end if

    return FALSE;
}

void startJvm(void *ptr)
{
    // Initial system setup.
    mpeos_memInit();
    mpeos_envInit();
#ifdef MPE_FEATURE_FRONTPANEL
    mpeos_fpInit();
#endif

    mpe_sysInit();

    mpeos_initUIEvents();

    // Signal that MPE has ben initialized. Tools will use this event for
    // determining whether the stack has finished its initalization sequence.
    //HANDLE hEvent = OpenEvent(EVENT_ALL_ACCESS, FALSE, "MpeInitCompleteEvent");
    //SetEvent(hEvent);

    // If we're set up to do testing,
    // don't start up the rest of the stack after the testing has completed.

    if (TRUE == ProcessTests())
    {
        return;
    }

    // Ensure that the VM is the last thing done here, as any waiting (sleeping/busy-work/etc.) on the
    // primordial Java thread would lock up anything done subsequently in this function
    // (eg, key-handling wouldn't work if the key queue/thread code was placed after this CreateVM code).
    if (mpe_jvmCreateVM())
    {
        mpe_jvmExecuteMain();
    }
}

/**
 * Stack entry point function.
 **/
RI_MODULE_EXPORT void ri_stack_init(int argc, char *argv[])
{
    ri_ui_manager = ri_get_ui_manager();
    if (ri_ui_manager == NULL)
    {
        printf("Unable to obtain RI user interface manager - aborting\n");
        return;
    }

    init_env(argc, argv);

    (void) mpeos_threadCreate(startJvm, NULL, MPE_THREAD_PRIOR_DFLT,
            MPE_THREAD_STACK_SIZE, &stackThread, "JVM Main Thread");
}

RI_MODULE_EXPORT void ri_stack_term(void)
{
    fprintf(stderr, ">>>> %s called\n", __func__);

    JavaVM* jvm = mpe_jvmGetJVM();
    if (jvm != NULL)
    {
        //fprintf(stderr, ">>>> %s calling DetachCurrentThread\n", __func__);
        //(*jvm)->DetachCurrentThread(jvm);
        //fprintf(stderr, ">>>> %s calling DestroyJavaVM\n", __func__);
        //(*jvm)->DestroyJavaVM(jvm);
    }

#ifdef PERFORM_SHUTDOWN
    fprintf(stderr, ">>>> %s calling mpeos_threadDestroy\n", __func__);
    mpeos_threadDestroy(stackThread);
    fprintf(stderr, ">>>> %s exiting\n", __func__);
#endif
}
