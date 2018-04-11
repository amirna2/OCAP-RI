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

/**
 * This file defines the porting APIs for the RI Platform launcher.
 */

#include <os_launcher.h>

/**
 * Loadable module handle -- port should define os_module_t
 */
typedef os_module_t ri_module_t;

/**
 * Pointer to a procedure sybmol from a loadable module -- platform
 * should define os_proc_addr_t
 */
typedef os_proc_addr_t ri_proc_addr_t;

/**
 * The name of the platform-specific environment variable that
 * defines the path search list for shared libraries
 */
extern const char* RI_SHARED_LIB_PATH_ENV_VAR;

/**
 * The platform-specific path separator char
 */
extern const char* RI_PATH_SEPARATOR;

/**
 * Load a software module (dynamic link library or shared object)
 * and return a handle that can be used to load symbols from that
 * module
 *
 * @param module_name The full path to the loadable module file
 * @return A handle to the module or NULL if the module was not
 * successfully loaded.
 */
ri_module_t ri_load_module(const char* module_path);

/**
 * Load a procedure from the given module with the given name
 *
 * @param module The module handle as returned by ri_load_module()
 * @param proc_name The name of the procedure to load
 * @return A pointer to the remote procedure or NULL if the procedure
 *         could not be loaded
 */
ri_proc_addr_t ri_get_proc(ri_module_t module, const char* proc_name);

/**
 * Return the value of the environment variable with the given name
 *
 * @param name The environment variable name
 * @return The value of the given environment variable or NULL if
 *         it does not exist.
 */
const char* ri_get_env(const char* name);

/**
 * Set (or create) an environment variable with a given value.  If
 * the given variable name does not exist, it is created.
 *
 * @param name The name of the environment variable to set.
 * @param value The value to assign to the environment variable
 */
void ri_set_env(const char* name, const char* value);

/**
 * Set the current working directory
 *
 * @param dir_name The directory name
 */
void ri_set_cwd(const char* dir_name);

