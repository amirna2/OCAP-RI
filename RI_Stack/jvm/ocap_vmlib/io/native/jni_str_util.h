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
// // COPYRIGHT_END

/*
 * These macros are used to convert JNI jstring objects to a native char*.
 * Use like this:
 *
 *        WITH_NATIVE_STRING(env, javastring, nativestring)
 *        {
 *             .... do something with native string ....
 *        }
 *        END_NATIVE_STRING(env, nativestring);
 *
 * env is the JNIEnv*, javastring is the jstring variable, and nativestring
 * is the name of a const char* that will be created for you to hold the
 * native string.
 *
 * If javastring is NULL, NullPointerException is thrown.
 */
#define WITH_NATIVE_STRING(env, javastring, nativestring)               \
    do {                                                                \
        const char* nativestring;                                       \
        jstring _##nativestring##str = (javastring);                    \
        if (_##nativestring##str == NULL) {                             \
            jclass cls = (*env)->FindClass(env, "java/lang/NullPointerException"); \
            if (cls != NULL)                                            \
            {                                                           \
                (*env)->ThrowNew(env, cls, "");                         \
            }                                                           \
            (*env)->DeleteLocalRef(env, cls);                           \
            goto _##nativestring##end;                                  \
        }                                                               \
        if (((nativestring) = (*env)->GetStringUTFChars((env), _##nativestring##str, NULL)) == NULL) \
            goto _##nativestring##end;

#define END_NATIVE_STRING(env, nativestring)                            \
        (*env)->ReleaseStringUTFChars((env), _##nativestring##str, (nativestring)); \
        _##nativestring##end: ;                                                 \
    } while (0);
