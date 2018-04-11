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

#ifndef INCLUDED_JNI_STATICS_MD_H
#define INCLUDED_JNI_STATICS_MD_H

#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <signal.h>

/* Use this macro to access a variable in this structure */
#define JNI_STATIC_MD(CLASS, VARIABLE) \
    (CVMjniGlobalStatics.platformStatics.CLASS ## _ ## VARIABLE)

/* This macro is used internally to define the structure */
#define DECL_JNI_STATIC_MD(TYPE, CLASS, VARIABLE) \
    TYPE CLASS ## _ ## VARIABLE

struct _CVMJNIStatics_md
{
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, IO_fd_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_timeoutID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_localPortID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_trafficClassID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_connected);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_connectedAddress);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_connectedPort);
#if defined(AF_INET6) 
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_multicastInterfaceID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_loopbackID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainDatagramSocketImpl, pdsi_ttlID);
#endif
    DECL_JNI_STATIC_MD(jclass, java_net_PlainDatagramSocketImpl, ia_clazz);
    DECL_JNI_STATIC_MD(jmethodID,java_net_PlainDatagramSocketImpl, ia_ctor);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, IO_fd_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_addressID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_portID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_localportID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_timeoutID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_trafficClassID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_serverSocketID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_fdLockID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_PlainSocketImpl, psi_closePendingID);
    DECL_JNI_STATIC_MD(int, java_net_PlainSocketImpl, tcp_level); /* = -1 */
    DECL_JNI_STATIC_MD(jclass, java_net_PlainSocketImpl, socketExceptionCls);
    DECL_JNI_STATIC_MD(int, java_net_PlainSocketImpl, preferredConnectionTimeout);
    DECL_JNI_STATIC_MD(jfieldID, java_net_SocketInputStream, IO_fd_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_SocketInputStream, sis_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_SocketInputStream, sis_implID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_SocketOutputStream, sos_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_net_SocketOutputStream, IO_fd_fdID);
    DECL_JNI_STATIC_MD(jfieldID, java_io_MPEFileSystem, ids_path);
};

typedef struct _CVMJNIStatics_md CVMJNIStatics_md;

#endif /* INCLUDED_JNI_STATICS_MD_H */
