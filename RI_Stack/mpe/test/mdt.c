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

#include "powertv.h"
#include <mpeos_dll.h>

#define MODULE_INFO "Name:" PTV_MODULE "\0Version:0.1\0Company:CableLabs\0Type:Application\0"

static const os_SymbolTbl DllSymTbl;

#ifdef __cplusplus__
extern "C"
{
#endif
void _dummy(void);
#ifdef __cplusplus__
}
#endif

void _dummy(void)
{
}

ModuleDispatchTableStart( MPEComponent);

/*  0 *//* table size -- will be filled in by the packager */
/*  1 *//* info string  -- will be filled in by the packager */
/*  2 */DispatchTableNull();
/*  3 */DispatchTableFunction( _dummy);
/*  4 */DispatchTableValue( kLd_UsesWindows);
/*  5 */DispatchTableNull();
/*  6 */DispatchTableNull();
/*  7 */DispatchTableNull();
/*  8 */DispatchTableNull();
/*  9 */DispatchTableValue( kApp_LargeStackSize);
/* 10 */DispatchTableValue( kApp_HighPriority);
/* 11 */DispatchTableNull();
/* 12 */DispatchTableNull();
/* 13 */DispatchTableNull();
/* 14 */DispatchTableNull();
/* 15 */DispatchTableNull();

/*************************************/
/* Beginning of MPE Defined Area */
/*************************************/

/* 16 */DispatchTableValue(&DllSymTbl);

/* ToDo: define shared module function table linkage entrypoint */

ModuleDispatchTableEnd( MPEComponent);

#include <sysentry.h>
