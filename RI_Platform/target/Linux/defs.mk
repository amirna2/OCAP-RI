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
#############################################################################
#
# This file defines the build rules used by this target configuration.
#
#############################################################################

#
# RI Platform C flags
#
RI_PLATFORM_CPPFLAGS += -DRI_HAVE_STDINT_H 

PLATFORM_OS = Linux

RI_CC    = gcc
RI_CXX   = g++
RI_BUILD = i686-pc-linux-gnu
RI_HOST  = i686-pc-linux-gnu

# Shared library suffix
SO_SUFFIX = .so

# Executable suffix
EXE_SUFFIX =

# Executable RPATH.  Set this to a ":" separated list of absolute paths of
# directories that the application should search for shared libraries.  This
# includes all libraries used by the Platform and all applications
LAUNCHER_RPATH = $(OCAPROOT)/bin/$(OCAPTC)/bin:$(OCAPROOT)/bin/$(OCAPTC)/env:$(PLATFORM_INST_DIR)/lib:$(PLATFORM_INST_DIR)/lib/gstreamer-0.10 

#
# CableLabs GStreamer Plugin platform-specific variables
#
GSTCL_LDFLAGS = 

OS_SOCKET_LIBS =

#
# UAL Configuration
#
UAL_LDFLAGS = -ldri -lclinkc

#
# CableLabs Display Plugin platform-specific variables
#
GSTDISP_LDFLAGS = -lGL

#
# log4c platform-specific variables
#
LOG4C_CPPFLAGS = 
LOG4C_LDFLAGS  =

#
# glib platform-specific variables
#
GLIB_CPPFLAGS = -D_FORTIFY_SOURCE=0 
GLIB_LDFLAGS  = 
GLIB_CONF_CACHE_FILE = glib.cache
GLIB_LIBICONV = --with-libiconv=no
GLIB_THREADS = --with-threads=posix

#
# GStreamer platform-specific variables
#
GST_CPPFLAGS = 
GST_LDFLAGS = 

#
# GStreamer base plugins platform-specific variables
#
GST_BASE_CPPFLAGS =
GST_BASE_LDFLAGS =

#
# GStreamer good plugins platform-specific variables
#
GST_GOOD_CPPFLAGS = 
GST_GOOD_LDFLAGS = 

#
# GStreamer FFMPEG platform-specific variables
#
GST_FFMPEG_CPPFLAGS = 
GST_FFMPEG_LDFLAGS =

#
# FFMPEG platform-specific variables
#
FFMPEG_ARCH = i686
FFMPEG_CPU = i686
FFMPEG_TARGET_OS = linux
FFMPEG_THREADS = 

GST_INCLUDES += -I/usr/include/libxml2
GST_LIBS += -lxml2

#
# uPnP platform-specific variables
#
CLINKC_CPPFLAGS =
CLINKC_LDFLAGS =

#
# gnutls platform-specific variables
#
GNUTLS_CONFIG_FLAGS = \
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
	--without-guile-site-dir \
	--with-included-libcfg \
	--without-libreadline-prefix

GNUTLS_LIBS = \
	-lgnutls \
	-lgcrypt \
	-lgpg-error \
	-lgmp

#
# wxWidgets platform-specific variables
#
# Note: Do not use WXWIDGETS_LDFLAGS as a variable name since this will
# mess up the wxWidgets configuration step.
#
WXDISPLAY_COPTS += \
	-I$(PLATFORM_INST_DIR)/include/wx-2.8 \
	-I$(PLATFORM_INST_DIR)/include/gtk-2.0 \
	-D__WXGTK__ -DUSE_OPENGL=1 -DWXUSINGDLL -DWX_PRECOMP
WXDISPLAY_LDFLAGS += \
	-lgtk-x11-2.0 \
	-lgdk-x11-2.0
WXWIDGETS_CONFIG_FLAGS += \
	LD_LIBRARY_PATH=$(PLATFORM_INST_DIR)/lib \
	--with-gtk=2 --with-opengl \
	--with-zlib=builtin --enable-threads --disable-joystick
#WXWIDGETS_TARGET = X11
WXWIDGETS_TARGET = GTKPLUS_2

# gtk+ platform specific variables
#
GTKPLUS_CONFIG_FLAGS += \
	LD_LIBRARY_PATH=$(PLATFORM_INST_DIR)/lib \
	--disable-static --enable-shared \
	--enable-fast-install=yes --enable-shm=yes \
	--disable-xdk --disable-gtk-doc --disable-man \
	--enable-modules --disable-xim --disable-xim-inst \
	--with-gnu-ld=no --with-pic \
	--without-xinput --without-wintab --without-ie55 \
	--with-gdktarget=x11 \
	--with-libpng --without-libjpeg --without-libtiff --with-x

