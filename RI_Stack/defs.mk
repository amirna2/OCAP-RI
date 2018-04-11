#COPYRIGHT_BEGIN
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
# 
# Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
# 
# This software is available under multiple licenses: 
# 
# (1) BSD 2-clause 
#  Redistribution and use in source and binary forms, with or without modification, are
#  permitted provided that the following conditions are met:
#       ·Redistributions of source code must retain the above copyright notice, this list 
#            of conditions and the following disclaimer.
#       ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
#            and the following disclaimer in the documentation and/or other materials provided with the 
#            distribution.
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
#  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
#  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
#  HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
#  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
# 
# (2) GPL Version 2
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, version 2. This program is distributed
#  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
#  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
#  PURPOSE. See the GNU General Public License for more details.
# 
#  You should have received a copy of the GNU General Public License along
#  with this program.If not, see<http:www.gnu.org/licenses/>.
# 
# (3)CableLabs License
#  If you or the company you represent has a separate agreement with CableLabs
#  concerning the use of this code, your rights and obligations with respect
#  to this code shall be as set forth therein. No license is granted hereunder
#  for any other purpose.
# 
#  Please contact CableLabs if you need additional information or 
#  have any questions.
# 
#      CableLabs
#      858 Coal Creek Cir
#      Louisville, CO 80027-9750
#      303 661-9100
#COPYRIGHT_END

# Host-specific build definitions
include $(OCAPROOT)/hostconfig/$(OCAPHOST)/hostdefs.mk

# Common build definitions for use in OCAP stack makefiles

# OCAP root directory in posix-style path format
OCAPROOT_POSIX = $(call TO_UNIX_PATH,$(OCAPROOT))

# OCAP MPE root directory
OCAP_MPE = $(OCAPROOT_POSIX)/mpe

# OCAP MPEOS root directory
OCAP_MPEOS = $(OCAP_MPE)/os

# OCAP MPE manager root directory
OCAP_MPEMGR = $(OCAP_MPE)/mgr

# OCAP JNI root directory
OCAP_JNI = $(OCAPROOT_POSIX)/jni

# OCAP generated files root directory (platform-specific)
OCAP_GEN = $(OCAPROOT_POSIX)/gen/$(OCAPTC)

# OCAP binary files root directory (platform-specific)
OCAP_BIN = $(OCAPROOT_POSIX)/bin/$(OCAPTC)

# OCAP DirectFB root directory
OCAP_DIRECTFB = $(OCAPROOT_POSIX)/thirdparty/DirectFB

# OCAP FreeType2 root directory
OCAP_FREETYPE2 = $(OCAPROOT_POSIX)/thirdparty/FreeType2

# OCAP ZLib root directory
OCAP_ZLIB = $(OCAPROOT_POSIX)/thirdparty/Zlib-1.2.1

# OCAP Tools root directory
OCAP_TOOLS = $(OCAPROOT_POSIX)/tools

# Java Development Kit root directory (posix-style path)
JDK_ROOT = $(call TO_UNIX_PATH,$(JAVA_HOME))

# Target-specific build definitions
include $(OCAPROOT)/target/$(OCAPTC)/defs.mk

# Function to generate POSIX-style paths for the MAKEFILE_LIST
# automatic variable
define makefile-list
	$(foreach i,$(filter-out %.d,$(MAKEFILE_LIST)),$(call TO_UNIX_PATH,$i))
endef

# Add pre-processor definitions for extensions
ifeq ($(DVR_EXTENSION_ENABLED), 1)
BUILD_CPPFLAGS += -DMPE_FEATURE_DVR
endif
ifeq ($(FP_EXTENSION_ENABLED), 1)
BUILD_CPPFLAGS += -DMPE_FEATURE_FRONTPANEL
endif
ifeq ($(DS_EXTENSION_ENABLED), 1)
BUILD_CPPFLAGS += -DMPE_FEATURE_DSEXT
endif
ifeq ($(HN_EXTENSION_ENABLED), 1)
BUILD_CPPFLAGS += -DMPE_FEATURE_HN
endif

