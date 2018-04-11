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
# Build wxWidgets for CableLabs RI
#
# @author Mark Millard - enableTV Inc
#
#############################################################################

default: build 

#
# Include top-level build definitions
#
include $(PLATFORMROOT)/defs.mk

# UI library generation directory
UI_GENDIR = $(PLATFORM_GEN)/ui

#
# OCAP root directory location
#
WXWIDGETS_ARCHIVE	= wxWidgets-2.8.9.tar.bz2
WXWIDGETS_ROOT		= $(UI_GENDIR)/wxWidgets-2.8.9
ATK_ARCHIVE		= atk1.0_1.22.0.tar.gz
ATK_ROOT		= $(UI_GENDIR)/atk-1.22.0
GTKPLUS_ARCHIVE		= gtk+-2.10.14.tar.gz
GTKPLUS_ROOT		= $(UI_GENDIR)/gtk+-2.10.14
PANGO_ARCHIVE	        = pango-1.28.0.tar.bz2
PANGO_ROOT		= $(UI_GENDIR)/pango-1.28.0


.PHONY: build clean purge atk atk_build atk_clean atk_purge atk_install gtkplus gtkplus_build gtkplus_clean gtkplus_purge gtkplus_install wxWidgets wxWidgets_build wxWidgets_clean wxWidgets_purge wxWidgets_install pango pango_build pango_clean pango_purge pango_install

ifeq ($(WXWIDGETS_TARGET),GTKPLUS_2)
  build: pango atk gtkplus wxWidgets
else
  build: wxWidgets
endif

make_gendir:
	$(MKDIR) $(UI_GENDIR)

#
# wxWidgets Build Targets
#
include wxWidgets.mk
wxWidgets: make_gendir wxWidgets_dir wxWidgets_build wxWidgets_install

wxWidgets_dir:
	if [ ! -d $(WXWIDGETS_ROOT) ]; then \
		$(call extract_tar_bz2,$(WXWIDGETS_ARCHIVE),$(UI_GENDIR)); \
	fi
	patch -p0 $(WXWIDGETS_ROOT)/configure patch_IT614


$(WXWIDGETS_ROOT)/Makefile: wxWidgets.mk
	cd $(WXWIDGETS_ROOT) && $(WXWIDGETS_CONFIGURE)

wxWidgets_build: $(WXWIDGETS_ROOT)/Makefile
	$(MAKE) 'PATH=$(WXWIDGETS_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)' --directory=$(WXWIDGETS_ROOT) 

wxWidgets_clean:
	if [ -e $(WXWIDGETS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(WXWIDGETS_ROOT) clean; \
	fi

wxWidgets_purge:
	if [ -e $(WXWIDGETS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(WXWIDGETS_ROOT) uninstall; \
	fi
	$(RMTREE) $(WXWIDGETS_ROOT)

wxWidgets_install: $(WXWIDGETS_ROOT)/Makefile
	if [ -e $(WXWIDGETS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(WXWIDGETS_ROOT) install; \
	fi

#
# atk Build Targets
#
include atk.mk
atk: make_gendir atk_dir atk_build atk_install

atk_dir:
	if [ ! -d $(ATK_ROOT) ]; then \
		$(call extract_tar_gz,$(ATK_ARCHIVE),$(UI_GENDIR)); \
	fi

$(ATK_ROOT)/Makefile: atk.mk
	cd $(ATK_ROOT) && $(ATK_CONFIGURE)

atk_build: $(ATK_ROOT)/Makefile
	$(MAKE) 'PATH=$(ATK_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)' --directory=$(ATK_ROOT) 

atk_clean:
	if [ -e $(ATK_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ATK_ROOT) clean; \
	fi

atk_purge:
	if [ -e $(ATK_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ATK_ROOT) uninstall; \
	fi
	$(RMTREE) $(ATK_ROOT)

atk_install: $(ATK_ROOT)/Makefile
	if [ -e $(ATK_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ATK_ROOT) install; \
	fi

#
# gtk+ Build Targets
#
include gtkplus.mk
gtkplus: make_gendir gtkplus_dir gtkplus_build gtkplus_install

gtkplus_dir:
	if [ ! -d $(GTKPLUS_ROOT) ]; then \
		$(call extract_tar_gz,$(GTKPLUS_ARCHIVE),$(UI_GENDIR)); \
	fi

$(GTKPLUS_ROOT)/Makefile: gtkplus.mk
	cd $(GTKPLUS_ROOT) && $(GTKPLUS_CONFIGURE)

gtkplus_build: $(GTKPLUS_ROOT)/Makefile
	$(MAKE) 'PATH=$(GTKPLUS_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)' --directory=$(GTKPLUS_ROOT) 

gtkplus_clean:
	if [ -e $(GTKPLUS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GTKPLUS_ROOT) clean; \
	fi

gtkplus_purge:
	if [ -e $(GTKPLUS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GTKPLUS_ROOT) uninstall; \
	fi
	$(RMTREE) $(GTKPLUS_ROOT)

gtkplus_install: $(GTKPLUS_ROOT)/Makefile
	if [ -e $(GTKPLUS_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GTKPLUS_ROOT) install; \
	fi

#
# pango Build Targets
#
include pango.mk
pango: make_gendir pango_dir pango_build pango_install

pango_dir:
	if [ ! -d $(PANGO_ROOT) ]; then \
		$(call extract_tar_bz2,$(PANGO_ARCHIVE),$(UI_GENDIR)); \
	fi

$(PANGO_ROOT)/Makefile: pango.mk
	cd $(PANGO_ROOT) && $(PANGO_CONFIGURE)

pango_build: $(PANGO_ROOT)/Makefile
	$(MAKE) 'PATH=$(PANGO_TOOLS_PATH):$(PLATFORM_PATH):$(PATH)' --directory=$(PANGO_ROOT) 

pango_clean:
	if [ -e $(PANGO_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(PANGO_ROOT) clean; \
	fi

pango_purge:
	if [ -e $(PANGO_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(PANGO_ROOT) uninstall; \
	fi
	$(RMTREE) $(PANGO_ROOT)

pango_install: $(PANGO_ROOT)/Makefile
	if [ -e $(PANGO_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(PANGO_ROOT) install; \
	fi

#
# Clean and purge
#
ifeq ($(WXWIDGETS_TARGET),GTKPLUS_2)
  clean: atk_clean gtkplus_clean wxWidgets_clean pango_clean

  purge: atk_purge gtkplus_purge wxWidgets_purge pango_purge
else
  clean: wxWidgets_clean

  purge: wxWidgets_purge
endif
