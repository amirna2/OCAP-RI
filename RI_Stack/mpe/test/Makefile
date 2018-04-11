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

#
# Explicitly name the default build target
#
.PHONY: symfile

# Create the name of the module to be generated
MODNAME	:=		mpe_test

#
# Help for this makefile
#
help:
	@echo "Build MPE test code"
	@echo ""
	@echo "Rules (build targets):"
	@echo "    help    - show this help message"
	@echo "    build   - build all code"
	@echo "    clean   - delete all temporary and intermediate files"
	@echo "    purge   - delete all final binaries"
	@echo "    symfile - create symbol file"
	@echo ""

#
# Directory locations
#
LIBDIR		=	$(OCAP_GEN)/lib
OBJDIR		=	$(OCAP_GEN)/$(MODNAME)

#
# Define additional include paths for MPE realm
#
INCS	= -I.
INCS	+= -I$(OCAP_MPE)/include
INCS	+= -I$(OCAP_MPEMGR)/include
INCS	+= -I$(OCAP_MPEOS)/include
INCS	+= -I$(OCAP_MPEOS)$(BUILD_OS)/include
INCS    += -Icommon
INCS	+= -Iinclude
INCS	+= -I$(OBJDIR)

#
# Define BUILD_TARGET_CFLAGS which gets included in the compilation rules
# in the main target configuration build.mak file.
#
# To build a MPEOS test un-comment out the TEST_*_MPEOS references below.
# TEST_MEDIA_MPEOS - builds MPEOS media tests.
# TEST_FILTER_MPEOS - builds MPEOS filter tests.
# TEST_SI_MPEOS - builds MPEOS SI tests.
# TEST_SYS_MPEOS - builds MPEOS system tests.
#
BUILD_TARGET_CFLAGS	= $(MPE_TEST_TARGET_CFLAGS)
#BUILD_TARGET_CFLAGS	+= -DTEST_MEDIA_MPEOS
#BUILD_TARGET_CFLAGS	+= -DTEST_FILTER_MPEOS
#BUILD_TARGET_CFLAGS	+= -DTEST_SI_MPEOS
BUILD_TARGET_CFLAGS	+= -DTEST_SYS_MPEOS

# To build mpe_test using a simple test harness, you can not use the VTE
# agent.  To disable build using the variable NO_VTE_AGENT.
#BUILD_TARGET_CFLAGS	+= -DNO_VTE_AGENT

#
# Compile options
#
COPTS		=	$(INCS)  $(BUILD_TARGET_CFLAGS) \
				-DPTV_MODULE=\"$(MODNAME)\"

OCAP_TEST_ROOT		:= . 

MPE_TEST_FILEIO_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/filesysmgr/test_mpe_fileio_all.c \
			$(OCAP_TEST_ROOT)/mpe/filesysmgr/test_mpe_fileio_api.c

MPE_TEST_MEDIA_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/mediamgr/test_media_all.c \
			$(OCAP_TEST_ROOT)/mpe/mediamgr/test_media_decoder.c \
			$(OCAP_TEST_ROOT)/mpe/mediamgr/test_media_misc.c \
			$(OCAP_TEST_ROOT)/mpe/mediamgr/test_media_neg.c \
			$(OCAP_TEST_ROOT)/mpe/mediamgr/test_media_tuner.c

MPE_TEST_FILTER_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/sectionFiltering_Motorola.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/sectionFiltering_PowerTV.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/sectionFiltering_Utils.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_Basic.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_Cancel.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_Dump.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_Negative.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_Priority.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_SimpleFilter.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_OOBDump.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_OOBChange.c \
			$(OCAP_TEST_ROOT)/mpe/filtermgr/test_filter_sectionFiltering_all.c

MPE_TEST_SYS_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_all.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_sync.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_event.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_mem.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_thread.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_time.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_util.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_dbg.c \
			$(OCAP_TEST_ROOT)/mpe/sysmgr/test_sys_math.c

MPE_TEST_GFX_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_color.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_context.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_draw.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_font.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_fontfact.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_gfx_smoke.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_surface.c \
			$(OCAP_TEST_ROOT)/mpe/dispmgr/test_uievent.c

MPE_TEST_NET_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_all.c \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_api.c \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_connect.c \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_lookup.c \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_options.c \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_readWrite.c \
			$(OCAP_TEST_ROOT)/mpe/netmgr/test_mpe_net_select.c

MPE_TEST_COMMON_SRCS	:= \
			$(OCAP_TEST_ROOT)/common/vte_errorUtils.c \
			$(OCAP_TEST_ROOT)/common/vte_crcUtils.c \
			$(OCAP_TEST_ROOT)/common/cutest.c \
			$(OCAP_TEST_ROOT)/common/vte_agent.c \
			$(OCAP_TEST_ROOT)/common/vte_dumpUtils.c \
			$(OCAP_TEST_ROOT)/common/VTE_CuTest.c \
			$(OCAP_TEST_ROOT)/mpelib_init.c

MPE_TEST_GENERIC_SRCS	:= \
			$(OCAP_TEST_ROOT)/os/generic/vpk_addTest.c \
			$(OCAP_TEST_ROOT)/os/generic/vpk_testHarness.c

MPE_TEST_CC_SRCS		:= \
				$(OCAP_TEST_ROOT)/mpe/ccmgr/test_cc_all.c

MPE_TEST_DVR_SRCS		:= \
				$(OCAP_TEST_ROOT)/mpe/ccmgr/test_dvr_all.c \
				$(OCAP_TEST_ROOT)/mpe/ccmgr/test_dvr_TuneToAChannel.c \
				$(OCAP_TEST_ROOT)/mpe/ccmgr/test_dvr_recording.c \
				$(OCAP_TEST_ROOT)/mpe/ccmgr/test_dvr_tsb.c

MPE_TEST_OC_SRCS	:= \
			$(OCAP_TEST_ROOT)/mpe/filesysmgr/ObjectCarousel/test_mpe_oc_api.c \
			$(OCAP_TEST_ROOT)/mpe/filesysmgr/ObjectCarousel/test_mpe_oc_all.c

MPE_TEST_SRCS		:= \
			$(MPE_TEST_OC_SRCS) \
			$(MPE_TEST_CC_SRCS) \
			$(MPE_TEST_COMMON_SRCS) \
			$(MPE_TEST_FILEIO_SRCS) \
			$(MPE_TEST_GFX_SRCS) \
			$(MPE_TEST_SYS_SRCS) \
			$(MPE_TEST_NET_SRCS) \
			$(MPE_TEST_FILTER_SRCS) 

#			$(MPE_TEST_MEDIA_SRCS)

SOURCES		=   $(MPE_TEST_SRCS)

ifeq ($(DVR_EXTENSION_ENABLED), 1)
#SOURCES += 		$(MPE_TEST_DVR_SRCS)
endif


VPATH	= ./mpe/dvrmgr:./mpe/ccmgr:./common:./mpe/sysmgr:./mpe/dispmgr:./mpe/netmgr:./mpe/filesysmgr:./mpe/mediamgr:./os/generic:./mpe/filtermgr:./mpe/filesysmgr/ObjectCarousel:.

#
# Intermediate files
#
OBJS		= 	$(addprefix $(OBJDIR)/, $(addsuffix .o, $(basename $(notdir $(SOURCES)))))

#
# Module definitions
#
MODULE		=	$(MPE_TEST_MODDIR)/$(MODNAME).$(MPE_TEST_MODEXT)
SYMFILE		=	$(BINDIR)/$(MODNAME).abs
MODULE_OBJS	=	$(OBJS) $(MPE_TEST_SYMTBL_OBJ)
#MODULE_LIBS	=	$(LIBDIR)/libmpelib.a $(LIBDIR)/libmpeoslib.a
#MODULE_LIBS	=	$(LIBDIR)/libmpelib.a

#
# Include dependency files
#
#DEPENDS		=	$(patsubst %.c,$(OBJDIR)/%.d,$(SOURCES))
DEPENDS		= 	$(addprefix $(OBJDIR)/, $(addsuffix .d, $(basename $(notdir $(SOURCES)))))
ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
-include $(DEPENDS)
endif

#
# Build everything
#
.PHONY: build
build: $(MODULE) 

#
# Build dependanceis
#
#.PHONY: libmpelib
#$(MODULE_LIBS): libmpelib

#libmpelib:
#	$(CD) $(OCAPROOT)/mpe/libsrc/mpelib; $(MAKE) build

#
# Build the module
#
$(MODULE): $(MODULE_OBJS) $(MODULE_LIBS) $(filter-out %.d,$(MAKEFILE_LIST))
	@$(call BUILD_MODULE,$(MODULE_OBJS),$(MODULE_LIBS),$(OBJDIR))

#
# Build the symbol file
#
symfile: $(SYMFILE)
$(SYMFILE): $(MODULE) $(filter-out %.d,$(MAKEFILE_LIST)) FORCE
	@$(call BUILD_SYMFILE,$(OBJDIR))

#
# Compile source files into intermediate files
#
$(OBJDIR)/%.o: %.c $(filter-out %.d,$(MAKEFILE_LIST))
	$(call COMPILE,$(COPTS))
$(LIBDIR)/%.o: %.c $(filter-out %.d,$(MAKEFILE_LIST))
	@$(call COMPILE,$(COPTS))

#
# Build the entrypoints table
#
ENTRYPOINTS	= exports
EXPORTLIBS	= $(MODULE_LIBS) $(OBJS)
$(OBJDIR)/sysentry.h: $(ENTRYPOINTS) $(EXPORTLIBS) $(filter-out %.d,$(MAKEFILE_LIST))
	@$(call BUILD_ENTRYPOINTS_TABLE,$(ENTRYPOINTS),$(EXPORTLIBS))

#
# Bring header file dependencies up to date
#
$(OBJDIR)/%.d: %.c 
	@$(call BUILD_DEPENDS,$(COPTS))
$(OBJDIR)/%.d: %.c
	@$(call BUILD_DEPENDS,$(COPTS))

#
# Clean and purge
#
.PHONY: clean purge
clean:
	$(RMTREE) $(OBJDIR)
purge:
	$(call RM_MODULE,$(MODULE))
	$(call RM_SYMFILE,$(SYMFILE))

$(MPE_TEST_SYMTBL_OBJ): $(OBJDIR)/sysentry.h
