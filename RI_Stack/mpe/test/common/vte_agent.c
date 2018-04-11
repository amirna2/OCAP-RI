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

#ifndef NO_VTE_AGENT
#include <mpe_types.h>
#include <mpe_dbg.h>
#include <mpe_os.h>
#include <mpe_dll.h>

#include <vte_agent.h>
#include <stdarg.h>

#if defined(WIN32)
#  define OCAP_LIBNAME "vte_agent.dll"
#elif defined(POWERTV)
#  define OCAP_LIBNAME "vte_agent.ptv"
#else
#endif

/**
 * Private variables to this module (VTE_Client.cpp).
 * @private_var m_VTEClientDLL The library containing the logging call.
 * @private_var m_VTEClientLog The func pointer to connect to logging call.
 */
static mpe_Dlmod m_VTEClientDLL = NULL;
static mpe_Bool (*m_VTEClientLog)(char*);

/**
 * vte_agent_Log -
 * Will log the VTE Client to connect to VTE. This will be done via functions
 * in the VTE_Client.dll. Right now we are writing to a log file.
 *
 * @return TRUE on success, else FALSE.
 */
mpe_Bool vte_agent_Log(const char* format, ...)
{
    char myStr[VTE_MAX_STRLEN];
    va_list arg;

    // Initialization should only occur the first time Log is called.
    if (NULL == m_VTEClientDLL)
    {
        // libName will be the library name containing the vte_agent_Log func.
        char libName[] = OCAP_LIBNAME;
        void* funcPtr;

        if (MPE_SUCCESS != mpe_dlmodOpen(libName, &m_VTEClientDLL))
        {
            MPE_LOG(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "ERROR vte_agent_Log: Could not load logger lib.\n");
            return FALSE;
        }

        if (MPE_SUCCESS != mpe_dlmodGetSymbol(m_VTEClientDLL, "vte_agent_Log",
                &funcPtr))
        {
            MPE_LOG(MPE_LOG_FATAL, MPE_MOD_TEST,
                    "ERROR vte_agent_Log: Could not get vte_agent_Log.\n");
            return FALSE;
        }

        // Convert the void* to the function pointer it should be.
        m_VTEClientLog = (mpe_Bool(*)(char*)) funcPtr;
    } // end init (end if block)

    va_start(arg, format);
    vsprintf(myStr, format, arg);
    va_end(arg);

    return m_VTEClientLog(myStr);
} /* end vte_agent_Log(const char*,...) */

#endif /* #ifndef NO_VTE_AGENT */
