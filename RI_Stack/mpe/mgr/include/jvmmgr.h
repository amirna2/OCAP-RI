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

#ifndef _JVMMGR_H_
#define _JVMMGR_H_

#ifdef __cplusplus 
extern "C"
{
#endif

#include "mgrdef.h"
#include "sysmgr.h"
#include "jni.h"

/* The JNI_VERSION_x_y is defined in jni.h.  Java 2 is version 1_2. */
#ifndef JNI_VERSION_1_2
#error "JNI Version 1.2 expected"
#endif

typedef struct
{
    void (*jvmInit_ptr)(void);
    uint32_t (*jvmCreateVM_ptr)(void);
    void (*jvmExecuteMain_ptr)(void);
} mpe_jvm_ftable_t;

void mpe_jvmSetup(void);

/**
 * <i>mpe_jvmCreateVM</i> uses the JNI library to instantiate and store
 * a JavaVM and thread local JNI environment
 */
uint32_t mpe_jvmCreateVM(void);

/**
 * execute static main method
 *
 * @param mainArgs - an array of null terminated character strings which contain arguements to be
 *                   passed into the main method. The first arguement should be the class name.
 * @param iNumArgs - the number of arguements contained in mainArgs
 */
void mpe_jvmExecuteMain(void);

/**
 * Returns a pointer to the instantiated VM's JNI environment pointer
 * Used by the JVM manager internally
 *
 * @return pointer to a JNIEnv structure
 */
JNIEnv *mpe_jvmGetJNIEnv(void);

/**
 * Returns a pointer to the instantiated VM's JavaVM structure pointer
 *
 * @return pointer to a JNIEnv structure
 */
JavaVM *mpe_jvmGetJVM(void);

#ifdef __cplusplus 
}
;
#endif

#endif /* _JVMMGR_H_ */
