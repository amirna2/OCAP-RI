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
# Build CableLabs RI Platform
#
# @author gdr - enableTV
#
#############################################################################

default: build 

#
# Include top-level build definitions
#
include $(PLATFORMROOT)/defs.mk

#
# Root directory location
#
GLIB_ROOT      = glib
SUPPORT_ROOT   = supportLibs
GST_ROOT       = gstreamer
WXWIDGETS_ROOT = userInterface
SOURCE_ROOT    = src
CONFIG_ROOT    = config

.PHONY: build clean purge \
	support-libs support-libs_clean support-libs_purge \
	glib glib_clean glib_purge \
	gst gst_clean gst_purge \
	wxWidgets wxWidgets_clean wxWidgets_purge \
	source source_clean source_purge

ifeq ($(RI_BUILD_SUPPORTLIBS),1)
    BUILD_TARGETS += support-libs \
                     glib \
                     gst \
                     wxWidgets
    CLEAN_TARGETS += support-libs_clean \
                     glib_clean \
                     gst_clean \
                     wxWidgets_clean
    PURGE_TARGETS += support-libs_purge \
                     glib_purge \
                     gst_purge \
                     wxWidgets_purge
endif

# Always build RI platform source
BUILD_TARGETS += source
CLEAN_TARGETS += source_clean
PURGE_TARGETS += source_purge

#
# Build everything
#
build: $(BUILD_TARGETS)

#
# GLib/GStreamer Support Library Build Targets
#
support-libs:
	$(MAKE) --directory=$(SUPPORT_ROOT) build

support-libs_clean:
	$(MAKE) --directory=$(SUPPORT_ROOT) clean

support-libs_purge:
	$(MAKE) --directory=$(SUPPORT_ROOT) purge

#
# GLib Build Targets
#
glib:
	$(MAKE) --directory=$(GLIB_ROOT) build

glib_clean:
	$(MAKE) --directory=$(GLIB_ROOT) clean

glib_purge:
	$(MAKE) --directory=$(GLIB_ROOT) purge

#
# GStreamer Build Targets
#
gst:
	$(MAKE) --directory=$(GST_ROOT) build

gst_clean:
	$(MAKE) --directory=$(GST_ROOT) clean

gst_purge:
	$(MAKE) --directory=$(GST_ROOT) purge

#
# wxWidgets Build Targets
#
wxWidgets:
	$(MAKE) --directory=$(WXWIDGETS_ROOT) build

wxWidgets_clean:
	$(MAKE) --directory=$(WXWIDGETS_ROOT) clean

wxWidgets_purge:
	$(MAKE) --directory=$(WXWIDGETS_ROOT) purge

#
# RI Platform source
#
source:
	$(MAKE) --directory=$(SOURCE_ROOT) build

source_clean:
	$(MAKE) --directory=$(SOURCE_ROOT) clean

source_purge:
	$(MAKE) --directory=$(SOURCE_ROOT) purge

#
# Clean and purge
#
clean: $(CLEAN_TARGETS)
purge: $(PURGE_TARGETS)

