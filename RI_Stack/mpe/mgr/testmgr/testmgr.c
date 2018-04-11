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
 * The MPE test manager implementation
 */

/***
 * How to add a manager to the MPE
 *
 * 
 * Create a constant for your manager in mpe/mgr/include/mgrdef.h
 *
 * The enum should look something like this:
 *
 *    enum MPE_MGR_TYPES
 *    {
 *       MPE_MGR_TYPE_SYS = 0,                   // System Manager
 *       MPE_MGR_TYPE_JVM,                       // JVM Manager
 *       MPE_MGR_TYPE_MYMGR,                     // your manager
 * 		
 *       MPE_MGR_COUNT,                          // total manager count
 *       MPE_MGR_MAX = 32
 *    };
 *
 *
 * Prototype your API functions and types as you normally would,
 * in a header file specific to your manager.
 *
 * i.e. mpe/mgr/include/mymgr.h would contain:
 * 
 *    typedef int mpe_mymgr_sometype;
 *
 *    void mpe_mymgr_func1(mpe_mymgr_sometype arg);
 *    mpe_Bool mpe_mymgr_func2(void);
 *
 *    void mpe_mymgr_setup(void);
 *    void mpe_mymgr_init(void);
 * 
 *
 * This header file should also define a function table, in
 * the form of a structure containing typed function pointers for each
 * of your API functions, like so:
 * 
 *    typedef struct {
 *       void (*mpe_mymgr_init_ptr)(void);	 // void f(void) init function must be first
 *       void (*mpe_mymgr_func1_ptr)(mpe_mymgr_sometype arg);
 *       mpe_Bool (*mpe_mymgr_func2_ptr)(void);
 *    } mpe_mymgr_ftable_t;
 * 
 *
 * Your manager must define the following two functions:
 *
 *    void mpe_mymgr_setup(void);
 *    void mpe_mymgr_init(void);
 *
 * mpe_mymgr_setup() will be called explicitly by the MPE system manager.
 * mpe_mymgr_setup() must call mpe_sys_install_ftable(), to install
 * your manager's function table into the master function table
 *
 * mpe_mymgr_init() is called after all managers have been setup.  This
 * must also be the first function in your function table.  Use
 * mpe_mymgr_init() to perform initialization.  If your manager's
 * initialization process depends on other managers having already been
 * initializated, you may call the initalization functions for those
 * managers directly.  All initialization functions should protect against
 * being called redundantly.  mpe_mymgr_setup() and mpe_mymgr_init()
 * should look something like so:
 * 
 * void mpe_mymgr_setup(void)
 * {
 *    mpe_sys_install_ftable(MPE_MGR_TYPE_MYMGR, &mymgr_ftable);
 * }
 *
 * void mpe_mymgr_init(void)
 * {
 *    static mpe_Bool inited = false;
 *    if (!inited)
 *    {
 *       inited = true;	 	// first init will be single threaded, so this is safe
 *        mpe_dbgInit(); // Initialize any other managers that are needed
 *       // perform this manager initialization
 *    }
 * }
 * 
 *
 * 
 * In your manager's main source file ( mpe/mgr/include/mymgr.cpp )
 * declare and initialize a global instance of your function table:
 *
 *    mpe_mymgr_ftable_t mymgr_ftable =
 *    {
 *       mpe_mymgr_init,
 *       mpe_mymgr_func1,
 *       mpe_mymgr_func2
 *    };
 * 
 * 
 * Next, create a bindings header ( mpe/include/mpe_mgrtype.h ) in the
 * following form.  Binding headers are used to allow API functions to be
 * transparently referenced via function tables.
 * 
 * #include <sysmgr.h>				// Need this to access master ftable variable
 * #include "..\mgr\mymgr\mymgr.h"	// Use a relative path to find API header
 * 
 * // This macro will extract your manager's function table from the master table
 * #define mpe_mymgr_ftable ((mpe_mymgr_ftable_t*)(mpe_ftable[MPE_MGR_TYPE_MYMGR]))
 * 
 * // These macros redirect calls to your API to the function pointers in
 * // your function table.
 * #define mpe_mymgr_init  (*(mpe_mymgr_ftable->mpe_mymgr_init_ptr))
 * #define mpe_mymgr_func1 (*(mpe_mymgr_ftable->mpe_mymgr_func1_ptr))
 * #define mpe_mymgr_func2 (*(mpe_mymgr_ftable->mpe_mymgr_func2_ptr))
 */

#include <sysmgr.h>
#include <testmgr.h>
#include <mpeos_dbg.h>

mpe_test_ftable_t test_ftable =
{ mpe_testInit, mpe_testFunc1, mpe_testFunc2 };

void mpe_testSetup(void)
{
    mpe_sys_install_ftable(&test_ftable, MPE_MGR_TYPE_TEST);
}

void mpe_testInit(void)
{
    static mpe_Bool inited = false;

    if (!inited)
    {
        inited = true; // first init will be single threaded, so this is safe

        // Initialize any other managers that are needed

        // perform this manager initialization
    }
}

// API functions

void mpe_testFunc1(int a, int b)
{

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "mpe_testFunc1: int a = %d, int b = %d\n", a, b);
}

mpe_Bool mpe_testFunc2(void)
{
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_TEST, "mpe_testFunc2: returning true\n");
    return true;
}

