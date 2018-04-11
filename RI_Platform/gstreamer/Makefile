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
# Build GStreamer and plugins for CableLabs RI
#
# @author gdr - Vidiom Systems Inc
#
#############################################################################

default: build 

#
# Include top-level build definitions
#
include $(PLATFORMROOT)/defs.mk

# GStreamer libs generation dir
GST_GENDIR = $(PLATFORM_GEN)/gstreamer

#
# Root directory locations
#
GST_ARCHIVE        = gstreamer-0.10.35.tar.bz2
GST_ROOT           = $(GST_GENDIR)/gstreamer-0.10.35
GST_BASE_ARCHIVE   = gst-plugins-base-0.10.35.tar.bz2
GST_BASE_ROOT      = $(GST_GENDIR)/gst-plugins-base-0.10.35
GST_GOOD_ARCHIVE   = gst-plugins-good-0.10.30.tar.bz2
GST_GOOD_ROOT      = $(GST_GENDIR)/gst-plugins-good-0.10.30

GST_EDITOR_ARCHIVE = gst-editor-0.10.2.tar.bz2
GST_EDITOR_ROOT    = $(GST_GENDIR)/gst-editor-0.10.2
GOOCANVAS_ARCHIVE  = goocanvas-0.13.tar.bz2
GOOCANVAS_ROOT     = $(GST_GENDIR)/goocanvas-0.13

.PHONY: build clean purge \
	gst gst_build gst_clean gst_purge gst_install \
	gst-base gst-base_build gst-base_clean gst-base_purge gst-base_install \
	gst-good gst-good_build gst-good_clean gst-good_purge gst-good_install

build: gst gst-base gst-good

editor: goocanvas gst-editor gst-base gst-good

make_gendir:
	$(MKDIR) $(GST_GENDIR)

#
# GStreamer Build Targets
#
include gstreamer.mk
gst: make_gendir gst_dir gst_build gst_install

gst_dir:
	if [ ! -d $(GST_ROOT) ]; then \
		$(call extract_tar_bz2,$(GST_ARCHIVE),$(GST_GENDIR)); \
		$(call apply_patch,gstreamer.patch,$(GST_GENDIR)); \
	fi

$(GST_ROOT)/Makefile: gstreamer.mk
	cd $(GST_ROOT) && $(GST_CONFIGURE)

gst_build: $(GST_ROOT)/Makefile
	$(MAKE) --directory=$(GST_ROOT) 'PATH=$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)'

gst_clean:
	if [ -e $(GST_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_ROOT) clean; \
	fi

gst_purge:
	if [ -e $(GST_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_ROOT) uninstall; \
	fi
	$(RMTREE) $(GST_ROOT)

gst_install: $(GST_ROOT)/Makefile
	if [ -e $(GST_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_ROOT) install; \
	fi

#
# GStreamer Base-Plugins Build Targets
#
include gst-plugins-base.mk
gst-base: make_gendir gst-base_dir gst-base_build gst-base_install

gst-base_dir: 
	if [ ! -d $(GST_BASE_ROOT) ]; then \
		$(call extract_tar_bz2,$(GST_BASE_ARCHIVE),$(GST_GENDIR)); \
		$(call apply_patch,gst-plugins-base.patch,$(GST_GENDIR)); \
	fi

$(GST_BASE_ROOT)/Makefile: gst-plugins-base.mk
	cd $(GST_BASE_ROOT) && $(GST_BASE_CONFIGURE)

gst-base_build: $(GST_BASE_ROOT)/Makefile
	$(MAKE) --directory=$(GST_BASE_ROOT) 'PATH=$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)'

gst-base_clean: 
	if [ -e $(GST_BASE_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_BASE_ROOT) clean; \
	fi

gst-base_purge: 
	if [ -e $(GST_BASE_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_BASE_ROOT) uninstall; \
	fi
	$(RMTREE) $(GST_BASE_ROOT)

gst-base_install: $(GST_BASE_ROOT)/Makefile
	if [ -e $(GST_BASE_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_BASE_ROOT) install; \
	fi

#
# GStreamer Good-Plugins Build Targets
#
include gst-plugins-good.mk
gst-good: make_gendir gst-good_dir gst-good_build gst-good_install

gst-good_dir: 
	if [ ! -d $(GST_GOOD_ROOT) ]; then \
		$(call extract_tar_bz2,$(GST_GOOD_ARCHIVE),$(GST_GENDIR)); \
	fi

$(GST_GOOD_ROOT)/Makefile: gst-plugins-good.mk
	cd $(GST_GOOD_ROOT) && $(GST_GOOD_CONFIGURE)

gst-good_build: $(GST_GOOD_ROOT)/Makefile
	$(MAKE) --directory=$(GST_GOOD_ROOT) 'PATH=$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)'

gst-good_clean: 
	if [ -e $(GST_GOOD_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_GOOD_ROOT) clean; \
	fi

gst-good_purge: 
	if [ -e $(GST_GOOD_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_GOOD_ROOT) uninstall; \
	fi
	$(RMTREE) $(GST_GOOD_ROOT)

gst-good_install: $(GST_GOOD_ROOT)/Makefile
	if [ -e $(GST_GOOD_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_GOOD_ROOT) install; \
	fi

#
# GStreamer Editor libs
#
include goocanvas.mk
goocanvas: make_gendir goocanvas_dir goocanvas_build goocanvas_install

goocanvas_dir:
	if [ ! -d $(GOOCANVAS_ROOT) ]; then \
		$(call extract_tar_bz2,$(GOOCANVAS_ARCHIVE),$(GST_GENDIR)); \
	fi

$(GOOCANVAS_ROOT)/Makefile: goocanvas.mk
	cd $(GOOCANVAS_ROOT) && $(GOOCANVAS_CONFIGURE)

goocanvas_build: $(GOOCANVAS_ROOT)/Makefile
	$(MAKE) --directory=$(GOOCANVAS_ROOT) 'PATH=$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)'

goocanvas_clean:
	if [ -e $(GOOCANVAS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GOOCANVAS_ROOT) clean; \
	fi

goocanvas_purge:
	if [ -e $(GOOCANVAS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GOOCANVAS_ROOT) uninstall; \
	fi
	$(RMTREE) $(GOOCANVAS_ROOT)

goocanvas_install: $(GOOCANVAS_ROOT)/Makefile
	if [ -e $(GOOCANVAS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GOOCANVAS_ROOT) install; \
	fi

include gst-editor.mk
$(GST_ROOT)/Make4Editor: gst-editor.mk
	cd $(GST_ROOT) && $(GST4EDITOR_CONFIGURE) && cp Makefile Make4Editor

gst4Editor: $(GST_ROOT)/Make4Editor
	$(MAKE) --directory=$(GST_ROOT) 'PATH=$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)'

gst-editor: make_gendir gst_dir gst4Editor gst_install gst-editor_dir gst-editor_build gst-editor_install

gst-editor_dir:
	if [ ! -d $(GST_EDITOR_ROOT) ]; then \
		$(call extract_tar_bz2,$(GST_EDITOR_ARCHIVE),$(GST_GENDIR)); \
	fi

$(GST_EDITOR_ROOT)/Makefile: gst-editor.mk
	cd $(GST_EDITOR_ROOT) && $(GST_EDITOR_CONFIGURE)

gst-editor_build: $(GST_EDITOR_ROOT)/Makefile
	$(MAKE) --directory=$(GST_EDITOR_ROOT) 'PATH=$(GLIB_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)'

gst-editor_clean:
	if [ -e $(GST_EDITOR_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_EDITOR_ROOT) clean; \
	fi

gst-editor_purge:
	if [ -e $(GST_EDITOR_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_EDITOR_ROOT) uninstall; \
	fi
	$(RMTREE) $(GST_EDITOR_ROOT)

gst-editor_install: $(GST_EDITOR_ROOT)/Makefile
	if [ -e $(GST_EDITOR_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GST_EDITOR_ROOT) install; \
	fi

#
# Clean and purge
#
clean: gst_clean gst-base_clean gst-good_clean

purge: gst_purge gst-base_purge gst-good_purge
