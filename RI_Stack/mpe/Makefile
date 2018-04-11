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

default: build

#
# Include top-level build definitions
#
include $(OCAPROOT)/defs.mk

.PHONY: build clean purge

#
# Help for this makefile
#
help:
	@echo "Build MPE library"
	@echo ""
	@echo "Rules (build targets):"
	@echo "    help    - show this help message"
	@echo "    build   - build all code"
	@echo "    clean   - delete all temporary and intermediate files"
	@echo "    purge   - delete all final binaries"
	@echo ""

#
# Directory locations
#
LIBDIR	=	$(OCAP_GEN)/lib
OBJDIR	=	$(OCAP_GEN)/mpe

#
# Compile options
#
COPTS	=	-I./include \
			-I"$(JDK_ROOT)/include" \
			-I$(OCAP_MPEMGR)/include \
			-I$(OCAP_MPEOS)/include \
			-I$(OCAP_MPEOS)/common/include \
			-I$(OCAP_MPEOS)/$(BUILD_OS)/include \
			-I$(OCAP_MPEOS)/include/gfx \
			-I$(OCAP_ZLIB) \
			-I$(OCAP_DIRECTFB) \
			-I$(OCAP_DIRECTFB)/include \
			-DFTABLE=mpe_ftable

# The "version" header file (mpe_version.h) is generated
COPTS += -I$(OCAP_GEN)/mpe

#
# Library built by this makefile
#
LIB			=	$(LIBDIR)/libmpe.a

#
# Source files
#
SOURCES =   mgr/ccmgr/cc_mgr.c \
			mgr/cdlmgr/cdlmgr.c \
			mgr/dbgmgr/dbgmgr.c \
			mgr/dispmgr/dispmgr.c \
			mgr/edmgr/edmgr.c \
			mgr/filesysmgr/ObjectCarousel/mpe_cacheManager.c \
			mgr/filesysmgr/ObjectCarousel/mpe_dataCarousel.c \
			mgr/filesysmgr/ObjectCarousel/mpe_file_oc.c \
			mgr/filesysmgr/ObjectCarousel/mpe_objectCarousel.c \
			mgr/filesysmgr/ObjectCarousel/mpe_objectCarouselBIOPParser.c \
			mgr/filesysmgr/ObjectCarousel/mpe_objectCarouselSectionParser.c \
			mgr/filesysmgr/ObjectCarousel/mpe_objectCarouselUtils.c \
			mgr/filesysmgr/filesysmgr.c \
			mgr/filesysmgr/romfs/mpe_file_romfs.c \
			mgr/filtermgr/filter_support.c \
			mgr/filtermgr/filtermgr.c \
			mgr/vbimgr/vbimgr.c \
			mgr/jvmmgr/jvmmgr.c \
			mgr/mediamgr/mediamgr.c \
			mgr/netmgr/netmgr.c \
			mgr/osmgr/osmgr.c \
			mgr/podmgr/podmgr.c \
			mgr/podmgr/mpe_podImpl.c \
			mgr/podmgr/createCAPMT_APDU.c \
			mgr/profmgr/profmgr.c \
			mgr/simgr/si_util.c \
			mgr/simgr/simgr.c \
			mgr/simgr/sitp_parse.c \
			mgr/simgr/sitp_psi.c \
			mgr/simgr/sitp_si.c \
			mgr/sndmgr/sndmgr.c \
			mgr/sysmgr/sysmgr.c \
			mgr/testmgr/testmgr.c \
			mgr/storagemgr/storagemgr.c


ifeq ($(DVR_EXTENSION_ENABLED), 1)
SOURCES += mgr/dvrmgr/dvr_mgr.c
endif

ifeq ($(FP_EXTENSION_ENABLED), 1)
SOURCES += mgr/frontpanelmgr/frontpanelmgr.c
endif

ifeq ($(HN_EXTENSION_ENABLED), 1)
SOURCES += mgr/hnmgr/hnmgr.c
endif

ifeq ($(CONFIG_USE_PROFILING), 1)
SOURCES += mgr/profmgr/profmgr.c
endif


#
# Intermediate files
#
OBJS		=	$(patsubst %.c,$(OBJDIR)/%.o,$(SOURCES))

#
# Include dependency files
#
DEPENDS		=	$(patsubst %.c,$(OBJDIR)/%.d,$(SOURCES))
ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
-include $(DEPENDS)
endif

#
# Build everything
#
build: $(LIB)

#
# Build the library from intermediate files
#
$(LIB): $(OBJS) 
	$(call BUILD_LIBRARY,$(OBJS))

#
# Compile source files into intermediate files
#
$(OBJDIR)/%.o: %.c $(call makefile-list)
	$(call COMPILE,$(COPTS))

#
# Bring header file dependencies up to date
#
$(OBJDIR)/%.d: %.c
	$(call BUILD_DEPENDS,$(COPTS))

#
# Clean and purge
#
clean:
	$(RMTREE) $(OBJDIR)
purge:
	$(call RM_LIBRARY,$(LIB))
