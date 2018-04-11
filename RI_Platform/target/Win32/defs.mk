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
# COPYRIGHT_END
#############################################################################
#
# This file defines the build rules used by this target configuration.
#
#############################################################################

#
# RI Platform C flags
#
RI_PLATFORM_CPPFLAGS += -DRI_HAVE_STDINT_H 

PLATFORM_OS = Win32

CFLAGS += -g

RI_CC    = 'gcc-3 -mno-cygwin'
RI_CXX   = 'g++-3 -mno-cygwin'
RI_BUILD = i686-pc-cygwin
RI_HOST  = i686-pc-mingw32

# Shared library suffix
SO_SUFFIX = .dll

# Executable suffix
EXE_SUFFIX = .exe

#
# CableLabs GStreamer Plugin platform-specific variables
#
GSTCL_LDFLAGS = -lgdi32 -lopengl32

OS_SOCKET_LIBS = -lws2_32 -liphlpapi

#
# UAL Configuration
#
UAL_LDFLAGS = -ldri -lclinkc

#
# CableLabs Display Plugin platform-specific variables
#
GSTDISP_LDFLAGS = -lgdi32 -lopengl32

#
# gettext platform-specific variables
#
GETTEXT_CONF_CACHE_FILE = gettext.cache

#
# Win32 pthreads platform-specific variables
#
PTHREADS_CFLAGS = -mno-cygwin

#
# libxml2 platform-specific variables
#
XML2_LDFLAGS = -L/usr/lib/w32api

#
# log4c platform-specific variables
#
LOG4C_CPPFLAGS = "-DHAVE_CONFIG_H -DLOG4C_EXPORTS -DWINVER=0x501 -D_USE_W32_SOCKETS -I$(PLATFORMROOT_POSIX)/supportLibs/syslog-client"
LOG4C_LDFLAGS  = "-no-undefined -L/usr/lib/w32api -L$(PLATFORM_INST_DIR)/lib -lsyslog -lws2_32"

#
# gnutls platform-specific variables
#
GNUTLS_CONFIG_FLAGS = \
	NM=nm \
	LDFLAGS='-L${PLATFORM_INST_DIR} -L/lib/w32api' \
	--disable-gtk-doc \
	--enable-gtk-doc-html \
	--enable-gtk-doc-pdf \
	--disable-cxx \
	--disable-rpath \
	--enable-valgrind-tests \
	--disable-guile  \
	--disable-fast-install \
	--disable-libtool-lock \
	--disable-gcc-warnings \
	--without-html-dir \
	--with-libgcrypt-prefix=$(PLATFORM_INST_DIR) \
	--without-libtasn1-prefix \
	--without-lzo \
	--with-included-libcfg \
	--without-libreadline-prefix

#
# glib platform-specific variables
#
GLIB_CPPFLAGS = -DWINVER=0x501
GLIB_LDFLAGS  = -L/lib/w32api
GLIB_CONF_CACHE_FILE = glib.cache
GLIB_LIBICONV = --with-libiconv=gnu
GLIB_THREADS = --with-threads=win32

#
# GStreamer platform-specific variables
#
GST_CPPFLAGS = '-D__MSVCRT_VERSION__=0x0700 -DWINVER=0x501'
GST_LDFLAGS = '-L/lib/w32api -lws2_32'

#
# GStreamer base plugins platform-specific variables
#
GST_BASE_CPPFLAGS = -DWINVER=0x501
GST_BASE_LDFLAGS = -L/lib/w32api

#
# GStreamer good plugins platform-specific variables
#
GST_GOOD_CPPFLAGS = -DWINVER=0x501
GST_GOOD_LDFLAGS = -L/lib/w32api

#
# GStreamer FFMPEG platform-specific variables
#
GST_FFMPEG_CPPFLAGS = -DWINVER=0x501
GST_FFMPEG_LDFLAGS = -L/lib/w32api

#
# FFMPEG platform-specific variables
#
FFMPEG_ARCH = i686
FFMPEG_CPU = i686
FFMPEG_TARGET_OS = mingw32
FFMPEG_THREADS = --enable-w32threads

ifeq ($(GST_EDITOR), 1)
GST_INCLUDES += -I$(PLATFORM_INST_DIR)/include/libxml2
GST_LIBS += -lxml2
endif

#
# GNU MP platform-specific variables
#
GMP_CONF_FLAGS = NM=nm gmp_cv_asm_underscore=yes

#
# uPnP platform-specific variables
#
CLINKC_CPPFLAGS = '-DWINVER=0x501 -I$(PLATFORM_INST_DIR)/include'
CLINKC_LDFLAGS = '-L$(PLATFORM_INST_DIR)/lib -L/usr/lib/w32api -lpthreadGC2 -liphlpapi'
GCRYPT_LDFLAGS = '-L$(PLATFORM_INST_DIR)/lib -liconv'
GNUTLS_LIBS = \
	-lgnutls \
	-lgcrypt \
	-lgpg-error \
	-lintl \
	-lws2_32 \
	-lgmp-10

#
# Platform Logger platform-specific variables
#
LOGGER_LDFLAGS = -lws2_32

#
# wxWidgets platform-specific variables
#
# Note: Do not use WXWIDGETS_LDFLAGS as a variable name since this will
# mess up the wxWidgets configuration step.
#
WXDISPLAY_COPTS += \
	-I$(PLATFORM_INST_DIR)/include/wx-2.8 \
	-D__WXMSW__ -DUSE_OPENGL=1 -DWXUSINGDLL -DWX_PRECOMP
WXDISPLAY_LDFLAGS += \
	-loleaut32 -lole32 -luuid -lwinspool \
	-lwinmm -lshell32 -lcomctl32 -lcomdlg32 \
	-lwsock32
WXWIDGETS_CONFIG_FLAGS += \
	--with-msw --with-opengl \
	--with-zlib=builtin --enable-threads

