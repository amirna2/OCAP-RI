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

#include "launcher_porting.h"

#include <ri_config.h>

#include <glib.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#define STRBUFFER_SZ 1024
#define MAX_NUM_PF_APPS 10

#undef PROFILE_MEMORY


#ifdef PROFILE_MEMORY

#include <pthread.h>

extern GMemVTable   *glib_mem_profiler_table;

#define TRACK_MEMORY
#undef EXTRA_MEM_CHECKS
#undef VERBOSE_MEM_LOGS

#define MAX_TRACK_BLOCKS    16000
#define BLOCK_SIZE          (1024-sizeof(gsize))
#define MIN_BLOCK_SIZE(_sz) (_sz < BLOCK_SIZE? BLOCK_SIZE : _sz)

#define MEM_LOG(level, format, ...) \
{ \
    char _msg[1024]; \
    snprintf(_msg, sizeof(_msg) - 1, format, ## __VA_ARGS__); \
    fprintf(stderr, "\r\n+=+=+=+= %s - %s: %s", level, __func__, _msg); \
    fflush(stderr);\
}

typedef struct
{
    gpointer p;
    gsize sz;
} TrackMem;

static struct
{
    gboolean initialized;
    pthread_mutex_t mutex;   // can't use GMutex as it uses g_malloc!
    gsize in_use;
    gsize watermark;
#ifdef EXTRA_MEM_CHECKS
    gpointer lo_ptr;
    gpointer hi_ptr;
#endif
    long reallocs;
    long allocs;
    long frees;
    int highest_allocated;
    int last_freed;

    TrackMem track_ptrs[MAX_TRACK_BLOCKS];
} mem = { 0 };

void dump_mem_info(void)
{
#ifdef EXTRA_MEM_CHECKS
    MEM_LOG("INFO", "in use:%10d,   hwm:%10d, lo:0x%p, hi:0x%p",
            mem.in_use, mem.watermark, mem.lo_ptr, mem.hi_ptr);
    MEM_LOG("INFO", "lfi:%d, hai:%d", mem.last_freed, mem.highest_allocated);
    MEM_LOG("INFO", "allocs:%10ld, frees:%10ld, reallocs:%ld\n",
                    mem.allocs, mem.frees, mem.reallocs);
#else
    MEM_LOG("INFO", "\n         in use:%10d,   hwm:%10d"
                    "\n         allocs:%10ld, frees:%10ld, reallocs:%ld\n",
                    mem.in_use, mem.watermark,
                    mem.allocs, mem.frees, mem.reallocs);
#endif
}

void mem_lock(void)
{
    if (FALSE == mem.initialized)
    {
        pthread_mutex_init(&mem.mutex, NULL);
        mem.initialized = TRUE;
    }

    pthread_mutex_lock(&mem.mutex);
}

void mem_unlock(void)
{
    pthread_mutex_unlock(&mem.mutex);
}

gboolean g_set_alloc_block(gpointer p, gsize sz, int index)
{
    //mem_lock();  INTERNAL - don't nest locks

    if (mem.track_ptrs[index].p == NULL)
    {
        mem.track_ptrs[index].sz = sz;
        mem.track_ptrs[index].p = p;
        mem.in_use += sz;

#ifdef EXTRA_MEM_CHECKS
        if ((p < mem.lo_ptr) || (NULL == mem.lo_ptr))
            mem.lo_ptr = p;
        else if (p > mem.hi_ptr)
            mem.hi_ptr = p;
#endif
        if (mem.in_use > mem.watermark)
        {
            mem.watermark = mem.in_use;
            dump_mem_info();
        }

        if (index > mem.highest_allocated)
            mem.highest_allocated = index;

        //mem_unlock();
        return TRUE;
    }

    //mem_unlock();
    return FALSE;
}

gboolean g_set_unused_alloc(gpointer p, gsize sz)
{
    int i;
    
    if (NULL != p)
    {
        //mem_lock();  INTERNAL - don't nest locks

        for (i = mem.last_freed; i < MAX_TRACK_BLOCKS; i++)
        {
            if (g_set_alloc_block(p, sz, i))
            {
                //mem_unlock();
                return TRUE;
            }
        }

        for (i = 0; i < mem.last_freed; i++)
        {
            if (g_set_alloc_block(p, sz, i))
            {
                //mem_unlock();
                return TRUE;
            }
        }

        //mem_unlock();
    }

    return FALSE;
}

TrackMem *g_find_alloc(gpointer p, gsize sz, int *index)
{
    int i = 0;
    int j = mem.highest_allocated;
    
    if (NULL != p)
    {
        //mem_lock();  INTERNAL - don't nest locks

        while (j > i)
        {
            if (mem.track_ptrs[j].p == p)
            {
                if (sz == 0 || mem.track_ptrs[j].sz == sz)
                {
                    if (NULL != index)
                        *index = j;
                    //mem_unlock();
                    return &mem.track_ptrs[j];
                }
            }

            j--;

            if (mem.track_ptrs[i].p == p)
            {
                if (sz == 0 || mem.track_ptrs[i].sz == sz)
                {
                    if (NULL != index)
                        *index = i;
                    //mem_unlock();
                    return &mem.track_ptrs[i];
                }
            }

            i++;
        }

        //mem_unlock();
    }

    return NULL;
}

TrackMem *g_find_within_alloc(gpointer p)
{
    int i = mem.highest_allocated;
    
    if (NULL != p)
    {
        //mem_lock();  INTERNAL - don't nest locks

        while (i > 0)
        {
            if ((p > mem.track_ptrs[i].p) &&
                (p < mem.track_ptrs[i].p+mem.track_ptrs[i].sz))
            {
                //mem_unlock();
                return &mem.track_ptrs[i];
            }

            i--;
        }

        //mem_unlock();
    }

    return NULL;
}

void g_track_allocs(gpointer p, gsize sz)
{
    TrackMem *ptm = NULL;
    mem_lock();

    if (NULL != p)
    {
        mem.allocs++;

        if (NULL == (ptm = g_find_alloc(p, sz, NULL)))
        {
            if (!g_set_unused_alloc(p, sz))
            {
                MEM_LOG("ERROR", "0x%p[%d] out of blks\n", p, sz);
            }
        }
        else if (ptm->sz != sz)
        {
            MEM_LOG("WARN", "0x%p sz change %d -> %d\n", p, ptm->sz, sz);
        }
        else
        {
            MEM_LOG("ERROR", "0x%p[%d] already in tracked!?\n", p, sz);
        }
    }

    mem_unlock();
}

void g_track_reallocs(gpointer np, gpointer p, gsize sz)
{
    TrackMem *ptm = NULL;
    mem_lock();

    if (NULL != np)
    {
        mem.reallocs++;

        if (np == p)
        {
            if (NULL != (ptm = g_find_alloc(p, 0, NULL)))
            {
                mem.in_use -= ptm->sz;
                ptm->sz = sz;
                mem.in_use += sz;
            }
            else
            {
                MEM_LOG("ERROR", "0x%p realloc'd ptr not found!?\n", p);
            }
        }
        else if (NULL == g_find_alloc(np, sz, NULL))
        {
            if ((NULL != p) && (NULL != (ptm = g_find_alloc(p, 0, NULL))))
            {
                mem.in_use -= ptm->sz;
                ptm->sz = 0;
                ptm->p = NULL;
                mem.frees++;
            }
            else
            {
                MEM_LOG("ERROR", "prev ptr 0x%p not found!?\n", p);
            }

            if (!g_set_unused_alloc(np, sz))
            {
                MEM_LOG("ERROR", "0x%p[%d] out of blks\n", p, sz);
            }
        }
        else
        {
            MEM_LOG("ERROR", "0x%p[%d] already tracked!?\n", np, sz);
        }
    }

    mem_unlock();
}

gboolean g_track_frees(gpointer p, gsize sz)
{
    TrackMem *ptm = NULL;
    mem_lock();

    if (NULL != p)
    {
        if (NULL != (ptm = g_find_alloc(p, sz, &mem.last_freed)))
        {
            mem.in_use -= ptm->sz;
            ptm->sz = 0;
            ptm->p = NULL;
            mem.frees++;

            if (mem.last_freed == mem.highest_allocated)
                mem.highest_allocated--;
        }
        else if (NULL != (ptm = g_find_within_alloc(p)))
        {
            MEM_LOG("ERROR", "0x%p found within tracked ptr %p[%d]!?\n",
                    p, ptm->p, ptm->sz);
            mem_unlock();
            return FALSE;
        }
#ifdef EXTRA_MEM_CHECKS
        else if ((p < mem.lo_ptr) || (p > mem.hi_ptr))
        {
            MEM_LOG("ERROR", "0x%p out of range!?\n", p);
            mem_unlock();
            return FALSE;
        }
#endif
        else
        {
            MEM_LOG("ERROR", "0x%p not found/tracked, double free!?\n", p);
#ifdef _WIN32
            //*((char *)0) = 0;   // force a seg fault!
#else
            //G_BREAKPOINT();
#endif
            mem_unlock();
            return FALSE;
        }
    }

    mem_unlock();
    return TRUE;
}

gpointer ri_try_malloc(gsize bytes)
{
    bytes = MIN_BLOCK_SIZE(bytes);  // minimum block size reduces reallocs
    gpointer p = malloc(bytes+sizeof(gsize));
    gsize *fp = p;

    if (NULL != p)
    {
        *fp = bytes;
        p += sizeof(gsize);

#ifdef TRACK_MEMORY
        g_track_allocs(p, bytes);
#endif
    }

    return p;
}

gpointer ri_try_realloc(gpointer p, gsize bytes)
{
    gpointer np = NULL;
    gsize *fp = NULL;
    gsize sz = 0;

    if (NULL == p)
        return ri_try_malloc(bytes);

    bytes = MIN_BLOCK_SIZE(bytes);  // minimum block size reduces reallocs
    fp = p - sizeof(gsize);
    sz = *fp;

    if (bytes <= sz)
        return p;

    MEM_LOG("INFO", "%d -> %d\n", sz, bytes);
    np = realloc(fp, bytes+sizeof(gsize));

    if (NULL != np)
    {
        fp = np;
        *fp = bytes;
        np += sizeof(gsize);

#ifdef TRACK_MEMORY
        g_track_reallocs(np, p, bytes);
#endif
    }

    return np;
}

gpointer ri_calloc(gsize blocks, gsize bytes)
{
    gpointer p = ri_try_malloc(blocks*bytes);

    if (NULL != p)
        memset(p, 0, blocks*bytes);

    return p;
}

gpointer ri_malloc(gsize bytes)
{
    gpointer p = ri_try_malloc(bytes);

    if (NULL == p)
        exit(-1);

    return p;
}

gpointer ri_realloc(gpointer p, gsize bytes)
{
    gpointer np = ri_try_realloc(p, bytes);

    if (NULL == np)
        exit(-2);

    return np;
}

void ri_free(gpointer p)
{
    gsize *fp = p - sizeof(gsize);
#ifdef TRACK_MEMORY
    gsize sz = *fp;

    if (g_track_frees(p, sz))
        free(fp);

#ifdef VERBOSE_MEM_LOGS
    static int i = 0;

    if (++i > 1999)
    {
        dump_mem_info();
        i = 0;
    }
#endif

#else
    free(fp);
#endif
}

static GMemVTable   ri_mem_profiler_table =
{
    ri_malloc,
    ri_realloc,
    ri_free,
    ri_calloc,
    ri_try_malloc,
    ri_try_realloc
};

#endif

void usage()
{
    printf("RI Platform Launcher.  Usage:\n");
    printf("\tri -config <path to configuration file>\n");
}

/**
 * Opens the module indicated by the give configuration variable name.
 *
 * @param configName the configuration variable name that can be used
 *        to retrieve the module library name
 * @return a handle to the module, or NULL if the module could not be
 *         loaded
 */
static ri_module_t getModule(char* configName)
{
    char* configValue;
    ri_module_t module;

    if ((configValue = ricfg_getValue("RILaunch", configName)) != NULL)
    {
        if ((module = ri_load_module(configValue)) == NULL)
        {
            printf("ERROR! Could not load library -- %s\n", configValue);
            return NULL;
        }
    }
    else
    {
        printf("ERROR! %s not defined\n", configName);
        return NULL;
    }

    return module;
}

/**
 * Returns a handle to the module function indicated by the given
 * configuration variable name.
 *
 * @param module the module that contains the desired function
 * @param configName the configuration variable name that can be used
 *        to retrieve the module function name
 * @return a handle to the module function, or NULL if the function
 *         could not be found
 */
static ri_proc_addr_t getModuleFunc(ri_module_t module, char* configName)
{
    char* configValue;
    ri_proc_addr_t proc;

    if ((configValue = ricfg_getValue("RILaunch", configName)) != NULL)
    {
        if ((proc = ri_get_proc(module, configValue)) == NULL)
        {
            printf("Could not find function (%s) in module!\n", configValue);
            return NULL;
        }
    }
    else
    {
        printf("ERROR! %s not defined\n", configName);
        return NULL;
    }

    return proc;
}

int main(int argc, char** argv)
{
    // Platform module vars
    ri_module_t platformModule;
    ri_proc_addr_t platformInitFunc, platformLoopFunc, platformTermFunc;
    void (*platform_init_func)(int, char**);
    void (*platform_loop_func)();
    void (*platform_term_func)();

    // App module vars
    ri_module_t appModule;
    ri_proc_addr_t appMainFunc, appTermFunc;
    void (*app_init_func)(int, char**);

    // Array of termination functions
    // void (*app_term_func)();
    typedef void (*app_term_func_t)();
    app_term_func_t app_term_funcs[MAX_NUM_PF_APPS];

    char appKey[64]; // for "RI.Launch.App.X.app_data_field" cfg file entries

    char* configValue;
    int maxArgs;
    int numArgs;
    int numApps = 0;
    int curApp = 0;
    char* appName = NULL;
    char** args;
    int i = 0;

    // Retrieve our configuration file name
    if (argc < 2)
    {
        usage();
        return 1;
    }

#ifdef PROFILE_MEMORY
    // Set-up the memory function table for profiling...
    g_mem_set_vtable(&ri_mem_profiler_table);
    //g_mem_set_vtable(glib_mem_profiler_table);
    //g_mem_profile();  // initial snapshot...
    //g_atexit(g_mem_profile);
#endif

    // Load and parse our config file
    if (ricfg_parseConfigFile("RILaunch", argv[1]) != RICONFIG_SUCCESS)
    {
        printf(
                "**    ERROR!  Could not parse platform configuration file!    **\n");
        printf("%s\n", argv[1]);
        return 1;
    }

    /////////////
    // Get the maximum number of values allowed for multi-value
    // configuration names
    maxArgs = ricfg_getMaxMultiValues("RILaunch");
    printf("* Platform Config * : maxArgs = %d\n", maxArgs);
    args = (char**) g_try_malloc(sizeof(char*) * (maxArgs + 1)); // the +1 makes room for the
    // app name in argv arrays
    if (NULL == args)
    {
        printf("**    ERROR!  Could not allocate args!    **\n");
        printf("%s\n", argv[1]);
        return 2;
    }

    // Set log4c resource file path to its default value if its not already set
    if (ri_get_env("LOG4C_RCPATH") == NULL)
    {
        ri_set_env("LOG4C_RCPATH", ri_get_env("PLATFORMROOT"));
        printf(
                "* Platform Config * : setting log4crc path to its default (PLATFORMROOT)\n");
    }

    /////////////
    // Construct and set shared library path env var
    numArgs = maxArgs;
    ricfg_getMultiValue("RILaunch", "RI.Launch.Platform.sharedLibPath", args,
            &numArgs);
    if (numArgs > 0)
    {
        char pathBuffer[STRBUFFER_SZ + 1];
        int pathLength = 0;

        // calculate the length of the path
        for (i = 0; i < numArgs; i++)
            pathLength += strlen(args[i]) + 1;
        if (pathLength > STRBUFFER_SZ)
        {
            printf("ERROR! RI.Launch.Platform.sharedLibPath too long!\n");
            goto error_exit;
        }

        pathBuffer[0] = '\0';

        for (i = 0; i < numArgs; i++)
        {
            strcat(pathBuffer, args[i]);
            strcat(pathBuffer, RI_PATH_SEPARATOR);
        }

        ri_set_env(RI_SHARED_LIB_PATH_ENV_VAR, (const char*) pathBuffer);
        printf("* Platform Config * : shared lib path = %s\n", pathBuffer);
    }

    /////////////////////////////////////////////////////////
    // Open platform libraries and retrieve all
    // required entry points
    /////////////////////////////////////////////////////////

    // Platform library
    if ((platformModule = getModule("RI.Launch.Platform.moduleName")) == NULL)
        goto error_exit;

    // Platform init function
    if ((platformInitFunc = getModuleFunc(platformModule,
            "RI.Launch.Platform.initFunc")) == NULL)
        goto error_exit;
    platform_init_func = (void(*)(int, char**)) platformInitFunc;

    // Platform main loop function
    if ((platformLoopFunc = getModuleFunc(platformModule,
            "RI.Launch.Platform.loopFunc")) == NULL)
        goto error_exit;
    platform_loop_func = (void(*)()) platformLoopFunc;

    // Platform termination function
    if ((platformTermFunc = getModuleFunc(platformModule,
            "RI.Launch.Platform.termFunc")) == NULL)
        goto error_exit;
    platform_term_func = (void(*)()) platformTermFunc;

    /////////////////////////////////////////////////////////
    // Platform Startup
    /////////////////////////////////////////////////////////

    // Call platform init function with arguments
    args[0] = "ri_platform_init";
    numArgs = maxArgs;
    ricfg_getMultiValue("RILaunch", "RI.Launch.Platform.arg", &args[1],
            &numArgs);
    platform_init_func(numArgs + 1, args);

    /////////////////////////////////////////////////////////
    // Read info for apps and start em up
    /////////////////////////////////////////////////////////

    // Figure out how many environments we will be starting
    printf("\n* Environments to be loaded * : \n");
    numApps = 0;
    do
    {
        sprintf(appKey, "RI.Launch.App.%d", numApps);
        if ((appName = ricfg_getValue("RILaunch", appKey)) == NULL)
            break;
        printf("    Platform Config * : env %d: %s\n", numApps, appName);
        numApps++;

    } while (appName != NULL);

    printf("    Total Environments: %d\n\n", numApps);
    if (numApps > MAX_NUM_PF_APPS)
    {
        printf("**    ERROR!  Num Apps to load (%d) exceeds max (%d)    **\n",
                numApps, MAX_NUM_PF_APPS );
        return 1;
    }

    // OK, now load em up
    curApp = 0;
    do
    {
        sprintf(appKey, "RI.Launch.App.%d", curApp);
        if ((appName = ricfg_getValue("RILaunch", appKey)) == NULL)
            break;

        printf("\n* Platform Config * : *** processing %s ***\n", appKey);
        printf("* Platform Config * : loading app: %s\n\n", appName);

        // Load curent app module
        sprintf(appKey, "RI.Launch.App.%d.moduleName", curApp);
        if ((appModule = getModule(appKey)) == NULL)
            goto error_exit;

        // Get current app main function
        sprintf(appKey, "RI.Launch.App.%d.mainFunc", curApp);
        if ((appMainFunc = getModuleFunc(appModule, appKey)) == NULL)
            goto error_exit;
        app_init_func = (void(*)(int, char**)) appMainFunc;

        // Get current apps termination function
        sprintf(appKey, "RI.Launch.App.%d.termFunc", curApp);
        if ((appTermFunc = getModuleFunc(appModule, appKey)) == NULL)
            goto error_exit;
        app_term_funcs[curApp] = (void(*)()) appTermFunc;

        // Set current app's working directory
        sprintf(appKey, "RI.Launch.App.%d.cwd", curApp);
        if ((configValue = ricfg_getValue("RILaunch", appKey)) != NULL)
        {
            ri_set_cwd(configValue);
            printf("* Platform Config * : app cwd = %s\n", configValue);
        }

        // Start the current app
        //args[0] = "Platform App Init Function",;
        char appFxn[64]; // TODO does this have to be unique memory accross mul apps?
        sprintf(appFxn, "Platform App [%s] Init Function", appName);
        args[0] = appFxn;

        numArgs = maxArgs;
        sprintf(appKey, "RI.Launch.App.%d.arg", curApp);
        ricfg_getMultiValue("RILaunch", appKey, &args[1], &numArgs); // make room in argv for app name

        printf("* Platform Config * : app args = ");
        for (i = 0; i <= numArgs; i++)
            printf(" %s", args[i]);
        printf("\n");

        app_init_func(numArgs + 1, args);

        curApp++;

    } while (appName != NULL);

    /////////////////////////////////////////////////////////
    // Platform Loop
    /////////////////////////////////////////////////////////

    // Run the platform loop until a reset request or shutdown is requested.
    printf("********** Starting platform loop **********\n");
    platform_loop_func();

    /////////////////////////////////////////////////////////
    // Termination
    /////////////////////////////////////////////////////////

    // call all of the app termination functions
    for (curApp = 0; curApp < numApps; curApp++)
    {
        app_term_funcs[curApp]();
    }
    platform_term_func();

    // Release the memory for our configuration list
    // Don't g_free the config list until we have a clean shutdown.
    // See OCORI-639 for more details.
    //ricfg_freeConfigList("RILaunch");

    // Release our argument list
    g_free(args);

    return 0;

    error_exit:

    g_free(args);
    return 1;
}
