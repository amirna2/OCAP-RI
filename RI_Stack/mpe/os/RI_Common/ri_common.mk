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
# Common makefile for CableLabs reference implementation
# (Windows and Linux).
#
# @author gdr - enableTV
#
#############################################################################

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
	@echo "Build the MPE OS library for RI Platform (Windows)"
	@echo ""
	@echo "Rules (build targets):"
	@echo "    help   - show this help message"
	@echo "    build  - build all code"
	@echo "    clean  - delete all temporary and intermediate files"
	@echo "    purge  - delete all final binaries"
	@echo ""

#
# Directory locations
#
LIBDIR		= $(OCAP_GEN)/lib
OBJDIR_ROOT	= $(OCAP_GEN)/mpeos
OBJDIR 		= $(OBJDIR_ROOT)/$(BUILD_OS)

RI_PLATFORM_ROOT = $(call TO_UNIX_PATH,$(PLATFORMROOT))
RI_PLATFORM_INSTALL = $(RI_PLATFORM_ROOT)/install/$(PLATFORMTC)

#
# Compile options
#
# Add -DDFB_USE_NATIVE_SURFACES on platforms with hardware accelerated graphics

COPTS = -I./include \
		-I./include/gfx/dfb \
		-I$(OCAP_MPEOS)/include \
		-I$(OCAP_MPEOS)/include/gfx \
		-I$(OCAP_MPEOS)/common/include \
		-I$(OCAP_MPEOS)/$(BUILD_OS)/include \
		-I$(OCAP_MPE)/include \
		-I$(OCAP_MPEMGR)/include \
		-I$(RI_PLATFORM_ROOT)/include \
		-I$(RI_PLATFORM_INSTALL)/include \
		-I$(RI_PLATFORM_INSTALL)/include/glib-2.0 \
		-I$(RI_PLATFORM_INSTALL)/lib/glib-2.0/include \
		-I"$(JDK_ROOT)/include" \
		-I$(OCAP_DIRECTFB) \
		-I$(OCAP_DIRECTFB)/include \
		-I$(OCAP_DIRECTFB)/src \
		-DDFB_USE_NATIVE_SURFACES

ifdef DTCPIP_BUILD_ROOT
DTCPIP_BUILD_DLL=$(shell echo $(call TO_UNIX_PATH,$(DTCPIP_BUILD_ROOT)/$(DTCPIP_DLL)) | $(SED) 's/^\/cygdrive//')
COPTS += -D'DTCPIP_BUILD_DLL="$(DTCPIP_BUILD_DLL)"'
BLDCFG = $(word 4,$(subst /, ,$(OCAPTC)))
endif

# The "version" header file (mpe_version.h) is generated
COPTS += -I$(OCAP_GEN)/mpe

#
# Library built by this makefile
#
LIB		=	$(LIBDIR)/libmpeos.a

#
# Source files
#
SOURCES	+=	\
			../common/gfxdfb/ConvertUTF.c \
			../common/gfxdfb/mpeos_context.c \
			../common/gfxdfb/mpeos_draw.c \
			../common/gfxdfb/mpeos_fontfact.c \
			../common/gfxdfb/mpeos_font.c \
			../common/gfxdfb/mpeos_screen.c \
			../common/gfxdfb/mpeos_surface.c \
			../common/mpeos_dbg_log.c \
			../RI_Common/directfb/ritvext.c \
			../RI_Common/directfb/vdfb_primary.c \
			../RI_Common/directfb/rip_display.c \
			../RI_Common/directfb/vdfb_system.c \
			../RI_Common/mpeos_caption.c \
			../RI_Common/mpeos_cdl.c \
			../RI_Common/mpeos_dbg.c \
			../RI_Common/mpeos_disp.c \
			../RI_Common/mpeos_filter.c \
			../RI_Common/mpeos_media.c \
			../RI_Common/mpeos_pod.c \
			../RI_Common/mpeos_snd.c \
			../RI_Common/mpeos_storage.c \
			../RI_Common/mpeos_uievent.c \
			../RI_Common/mpeos_util.c \
			../RI_Common/mpeos_vbi.c \
			../RI_Common/stack.c \
			../RI_Common/test_3dtv.c \
			../$(BUILD_OS)/mpeos_dll.c \
			../$(BUILD_OS)/mpeos_event.c \
			../$(BUILD_OS)/mpeos_file.c \
			../$(BUILD_OS)/mpeos_mem.c \
			../$(BUILD_OS)/mpeos_socket.c \
			../$(BUILD_OS)/mpeos_sync.c \
			../$(BUILD_OS)/mpeos_thread.c \
			../$(BUILD_OS)/mpeos_time.c 

ifeq ($(DVR_EXTENSION_ENABLED), 1)
SOURCES += 		../RI_Common/mpeos_dvr.c
endif

ifeq ($(FP_EXTENSION_ENABLED), 1)
SOURCES += 		../RI_Common/mpeos_frontpanel.c
endif

ifeq ($(HN_EXTENSION_ENABLED), 1)
SOURCES += 		../RI_Common/mpeos_hn.c \
			../RI_Common/mpeos_hn_server_profiles.c \
			../RI_Common/hn_server.c \
			../RI_Common/hn_server_send_thread.c \
			../RI_Common/hn_player.c \
			../RI_Common/hn_player_read_thread.c \
			../RI_Common/hn_player_http.c \
			../RI_Common/hn_dtcpip.c
endif

#
# Intermediate files
#
OBJS	= $(patsubst %.c,$(OBJDIR)/%.o,$(SOURCES))

#
# Include dependency files
#
DEPENDS	= $(patsubst %.c,$(OBJDIR)/%.d,$(SOURCES)) \

ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
-include $(DEPENDS)
endif

#
# Build everything
#
build: $(LIB)
ifdef DTCPIP_BUILD_ROOT
	$(MAKE) -C $(DTCPIP_BUILD_ROOT) \
		BLDCFG=$(BLDCFG) \
		DTCPIP_DLL=$(DTCPIP_DLL) \
		HN_DTCPIP_H=$(OCAPROOT)/mpe/os/RI_Common \
		shlib
endif

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
	$(RMTREE) $(OBJDIR_ROOT)
ifdef DTCPIP_BUILD_ROOT
	$(MAKE) -C $(DTCPIP_BUILD_ROOT) BLDCFG=$(BLDCFG) clean
endif

purge:
	$(call RM_LIBRARY,$(LIB))
ifdef DTCPIP_BUILD_ROOT
	$(MAKE) -C $(DTCPIP_BUILD_ROOT) BLDCFG=$(BLDCFG) purge
endif
