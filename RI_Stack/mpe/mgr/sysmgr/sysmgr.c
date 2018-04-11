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
 * The MPE system manager implementation
 */

#include <sysmgr.h>
#include <mpe_sys.h>
#include <testmgr.h>
#include <filesysmgr.h>
#include <netmgr.h>
#include <jvmmgr.h>
#include <dbgmgr.h>
#include <mediamgr.h>
#include <dispmgr.h>
#include <simgr.h>
#include <osmgr.h>
#include <filtermgr.h>
#include <vbimgr.h>
#include <cc_mgr.h>
#include <podmgr.h>
#include <edmgr.h>
#include <sndmgr.h>
#include <cdlmgr.h>
#include <storagemgr.h>

#ifdef MPE_FEATURE_FRONTPANEL
#include <frontpanelmgr.h>
#endif

#ifdef MPE_FEATURE_DVR
#include <dvr_mgr.h>
#endif

#ifdef MPE_FEATURE_HN
#include <hnmgr.h>
#endif

#include <cdlmgr.h>

#ifdef MPE_FEATURE_PROF
#include <profmgr.h>
#endif

#include <mpe_config.h>
#include <mpeos_dbg.h>

#ifdef MPE_FEATURE_DCAS_MGR
#include <strings.h> /* strcasecmp(3) */
extern void dpl_init( void );
#endif

typedef void (*init_func_ptr)(void);

typedef struct
{
    mpe_Error (*init_fptr)(void);
    //  mpe_Error (*shutdown_fptr)(void);
} ftable;

static ftable*( static_mpe_ftable[MPE_MGR_MAX]) =
{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0 };
void **mpe_ftable = (void**) static_mpe_ftable;

/**
 * <I>mpe_sys_install_ftable</i> Replace the current function table
 * with a new one.
 *
 * @param ftable The function table to insert.
 * @param index  The position/module to insert this function table
 *               in.
 * @return A pointer to the ftable which was replace.  Returned as a void*.
 **/
void* mpe_sys_install_ftable(void* new_ftable, uint32_t index)
{
    void* old_ftable;

    /* Validate manager index. */
    if (index >= MPE_MGR_COUNT)
    {
        return NULL;
    }

    /* Get current entry. */
    old_ftable = mpe_ftable[index];

    /* Set new entry. */
    mpe_ftable[index] = new_ftable;

    /* Return previous entry. */
    return old_ftable;
}

/**
 * <i>mpe_sysInit()</i>
 * Initialize all the subcomponents.
 * First invokes the setup routine for each subcompontent to create the
 * functions tables, then invokes the init routines in each module.
 */
void mpe_sysInit()
{
    static mpe_Bool inited = false;
    int i;

    /*
     * Should not be called from multiple threads until after the first call returns,
     * so, no need for thread synchronizing.
     */
    if (!inited)
    {
        inited = true;

        /*
         * For each default manager, setup should install the ftable
         * (but not yet initialize the manager).
         */
        mpe_osSetup();

#ifndef MPE_PORT_NO_FILESYS
        mpe_filesysSetup();
        mpe_storageSetup();
#endif /* MPE_PORT_NO_FILESYS */

#ifndef MPE_PORT_NO_NET
        mpe_netSetup();
#endif /* MPE_PORT_NO_NET */

        mpe_testSetup();
        mpe_jvmSetup();
        mpe_edSetup();
        mpe_dbgSetup();

#ifndef MPE_PORT_NO_GFX
        mpe_dispSetup();
#endif /* MPE_PORT_NO_GFX */

#ifndef MPE_PORT_NO_MEDIA
        mpe_media_setup();
        mpe_siSetup();
        mpe_filterSetup();
        mpe_ccSetup();
        mpe_cdlSetup();
#endif /* MPE_PORT_NO_MEDIA */

#ifndef MPE_PORT_NO_POD
        mpe_podSetup();
#endif /* MPE_PORT_NO_POD */
        mpe_ccSetup(); /* set up closed captioning manager fct table */

#ifdef MPE_FEATURE_DVR
        mpe_dvrSetup(); /* set up dvr manager function table */
#endif
#ifdef MPE_FEATURE_HN
        mpe_hn_setup(); /* set up home neetworking manager function table */
#endif
#ifdef MPE_FEATURE_PROF
        mpe_profSetup(); // profile manager
#endif

#ifdef MPE_FEATURE_FRONTPANEL
        mpe_fpSetup();
#endif

#ifdef MPE_FEATURE_DSEXT
#endif

        mpe_sndSetup();
        mpe_cdlSetup(); // download manager
        mpe_vbiSetup();

        // TODO:
        // Read in override modules
        // For each override module, call mpe_mgr_setup() equivilent
        // Passes in mpe_ftable, so override module may make almost any type of alterations

        // Initialize all ftables that are non-NULL at this point.
        // First entry in each ftable must be an init routine
        for (i = 0; i < MPE_MGR_MAX; i++)
        {
            ftable *tab = mpe_ftable[i];
            if (tab)
            {
                (void) tab->init_fptr();
            }
        }

#ifdef MPE_FEATURE_DCAS_MGR
        {
            const char *envVar = mpeos_envGet("MPE_DCAS_MGR_ENABLED");
            if ((0 != envVar) && (0 == strcasecmp(envVar, "true")))
            {
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TARGET, "MPE_DCAS_MGR_ENABLED ... calling dpl_init() ...\n");
                dpl_init();
            }
        }
#endif

        // Print out the build version string we're running
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_TARGET, "MPE Build Version '%s'\n",
                OCAP_VERSION);
    }
}
