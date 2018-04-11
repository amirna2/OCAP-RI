# COPYRIGHT_BEGIN
#  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
#  
#  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
#  
#  This software is available under multiple licenses: 
#  
#  (1) BSD 2-clause 
#   Redistribution and use in source and binary forms, with or without modification, are
#   permitted provided that the following conditions are met:
#        ·Redistributions of source code must retain the above copyright notice, this list 
#             of conditions and the following disclaimer.
#        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
#             and the following disclaimer in the documentation and/or other materials provided with the 
#             distribution.
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
#   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
#   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
#   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
#   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
#   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
#   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
#   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
#   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
#   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#  
#  (2) GPL Version 2
#   This program is free software; you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, version 2. This program is distributed
#   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
#   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
#   PURPOSE. See the GNU General Public License for more details.
#  
#   You should have received a copy of the GNU General Public License along
#   with this program.If not, see<http:www.gnu.org/licenses/>.
#  
#  (3)CableLabs License
#   If you or the company you represent has a separate agreement with CableLabs
#   concerning the use of this code, your rights and obligations with respect
#   to this code shall be as set forth therein. No license is granted hereunder
#   for any other purpose.
#  
#   Please contact CableLabs if you need additional information or 
#   have any questions.
#  
#       CableLabs
#       858 Coal Creek Cir
#       Louisville, CO 80027-9750
#       303 661-9100
# COPYRIGHT_END

## Build Personal Basis Profile
AWT_IMPLEMENTATION = mpe

CVM_TARGETROOT	= $(CVM_TOP)/src/$(TARGET_OS)
MPE_ROOT        = $(call HOST2POSIX,$(OCAPROOT)/mpe)

CVM_STATICLINK_LIBS = true

#
# Platform specific defines
#
CVM_DEFINES	+= -D_GNU_SOURCE -DCVM_MPE

CVM_BUILDTIME_CLASSES += \
    org.cablelabs.impl.java.ReclaimThread \
    org.cablelabs.impl.appctx.AppContextManager \
    org.cablelabs.impl.appctx.AppContextHandler \
    org.cablelabs.impl.io.DefaultOpenFile \
    org.cablelabs.impl.io.DefaultFileSys \
    org.cablelabs.impl.io.DefaultWriteableFileSys \
    org.cablelabs.impl.io.FileData \
    org.cablelabs.impl.io.FileMetadataManager \
    org.cablelabs.impl.io.FileSys \
    org.cablelabs.impl.io.FileSysManager \
    org.cablelabs.impl.io.FileSysMgr \
    org.cablelabs.impl.io.OpenFile \
    org.cablelabs.impl.io.StorageMediaFullException \
    org.cablelabs.impl.io.StdOut \
    org.cablelabs.impl.io.WriteableFileSys \
    org.cablelabs.impl.net.Socket

#
# Platform specific source directories
#
CVM_SRCDIRS += \
	$(CVM_TARGETROOT)/bin \
	$(CVM_TARGETROOT)/javavm/runtime \
	$(CVM_TARGETROOT)/native/java/lang \
	$(CVM_TARGETROOT)/native/java/io \
	$(CVM_TARGETROOT)/native/java/net \
	$(CVM_TARGETROOT)/native

CVM_INCLUDE_DIRS += \
	$(CVM_TOP)/src \
	$(CVM_TARGETROOT) \
	$(CVM_TARGETROOT)/native/java/net \
	$(CVM_TARGETROOT)/native/common \
	$(MPE_ROOT)/include \
	$(MPE_ROOT)/mgr/include \
	$(MPE_ROOT)/os/include \

#
# Platform specific objects
#

CVM_TARGETOBJS_SPACE += \
	canonicalize_md.o \
	io_md.o \
	net_md.o \
	time_md.o \
	io_util.o \
	memory_md.o \
	sync_md.o \
	system_md.o \
	threads_md.o \
	globals_md.o \
	java_props_md.o \
    ReclaimThread.o \
	DefaultFileSys.o \
	DefaultOpenFile.o \
	DefaultWriteableFileSys.o \
	StdOut.o \
	Socket.o \
	mpelib_init.o

ifeq ($(CVM_USE_MEM_MGR), true)
CVM_TARGETOBJS_SPACE += \
	mem_mgr_md.o
endif

ifeq ($(CVM_DYNAMIC_LINKING), true)
	CVM_TARGETOBJS_SPACE += linker_md.o
endif

# Include timezone information for NorthAmerica only
TZFILE = northamerica
