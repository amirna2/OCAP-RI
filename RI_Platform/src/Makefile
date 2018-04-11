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
# Build CableLabs RI executable
#
# @author mkorzen - CableLabs
#
#############################################################################

default: build

#
# Include top-level build definitions
#
include $(PLATFORMROOT)/defs.mk

.PHONY: build clean purge \
        ate_build ate_clean ate_purge \
        remap_build remap_clean remap_purge \
        ifs_build ifs_clean ifs_purge \
        cl_gst_plugin cl_gst_plugin_clean cl_gst_plugin_purge \
        config_build config_clean config_purge \
        tuner_build tuner_clean tuner_purge \
        logging_build logging_clean logging_purge \
        launcher_build launcher_clean launcher_purge \
        snmp_build snmp_clean snmp_purge \
        ui_build ui_clean ui_purge \
        w32-kill_build w32-kill_clean w32-kill_purge

#
# Help for this makefile
#
help:
	@echo "Build the RI platform library"
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
OBJDIR 		= $(PLATFORM_GEN)/srcobjs

ifeq ($(RI_HOST),i686-pc-mingw32)
  LIBDIR = $(PLATFORM_INST_DIR)/bin
  LOG4C_LIBS = -llog4c-3
  GST_LIBS = -lgstapp-0.10-0
else
  LIBDIR = $(PLATFORM_INST_DIR)/lib
  LOG4C_LIBS = -llog4c
  GST_LIBS = -lgstapp-0.10
endif


#
# Compile options
#
COPTS = \
	$(RI_PLATFORM_CPPFLAGS) \
	$(GST_INCLUDES) \
	-I$(PLATFORM_INC) \
	-I$(PLATFORM_SRC) \
	-I$(PLATFORM_SRC)/ui \
	-I$(PLATFORM_SRC)/logging \
	-I$(PLATFORM_SRC)/config \
	-I$(PLATFORM_SRC)/remap \
	-I$(PLATFORM_SRC)/gstreamer \
	-I$(PLATFORM_INST_DIR)/include \
	-I$(PLATFORM_INST_DIR)/include/libxml2 \
	-I$(PLATFORM_INST_DIR)/include/glib-2.0 \
	-I$(PLATFORM_INST_DIR)/lib/glib-2.0/include \
	-I$(PLATFORM_INST_DIR)/include/gstreamer-0.10

LDOPTS = \
	$(OS_SOCKET_LIBS) \
	-L$(PLATFORM_INST_DIR)/bin \
	-L$(PLATFORM_INST_DIR)/lib \
	-L$(PLATFORM_INST_DIR)/lib/gstreamer-0.10 \
	-lglib-2.0 \
	-lgobject-2.0 \
	-lgmodule-2.0 \
	-lgstreamer-0.10 \
	-lgstinterfaces-0.10 \
	$(GST_LIBS) \
	-lgthread-2.0 \
	-llogger \
	$(LOG4C_LIBS) \
	-lui \
	-lri_config \
	-lplatformAgent \
	-lgstcablelabs \
	-lifs \
	-lxml2 \
	-ltuner \

#
# Libraries built by this makefile
#
LIB = $(LIBDIR)/libplatform$(SO_SUFFIX)

#
# Source files
#
SOURCES	= \
	platform.c \
	genericfeatures.c \
	cablecard.c \
	pipeline_manager.c \
	pipeline.c \
	pipeline_hn_server.c \
	pipeline_hn_player.c \
	fdc.c \
	oob_pipeline.c \
	oob_section_filter.c \
	dsg.c \
	dsg_pipeline.c \
	dsg_section_filter.c \
	tuner.c \
	audio.c \
	display.c \
	video_device.c \
	section_filter.c \
	ib_section_filter.c \
	section_cache.c \
	gst_utils.c \
	ui_manager.c \
	frontpanel.c \
	backpanel.c \
	audio_output_port.c \
	video_output_port.c \
	sectionutils.c \
	sas.c \
	test_interface.c \
	tsb.c \
    stare.c

#
# Intermediate files
#
OBJS = $(patsubst %.c,$(OBJDIR)/%.o,$(SOURCES))

#
# Build everything
#
ifeq ($(RI_HOST),i686-pc-mingw32)
build:  logging_build config_build launcher_build remap_build ifs_build $(LIB) ate_build w32-kill_build
else
build:  logging_build config_build launcher_build remap_build ifs_build $(LIB) ate_build
endif


#
# Include dependency files
#
DEPENDS     =   $(patsubst %.c,$(OBJDIR)/%.d,$(SOURCES)) 

ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
-include $(DEPENDS)
endif

#
# Build the library from intermediate files
#
$(LIB): ui_build ifs_build remap_build cl_gst_plugin tuner_build snmp_build $(OBJS) 
	$(call BUILD_SHARED_LIBRARY,$(OBJS) $(LDOPTS))

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
# ATE Interface (stand-alone executable)
#
ate_build:
	$(MAKE) --directory=ate

ate_clean:
	$(MAKE) --directory=ate clean

ate_purge:
	$(MAKE) --directory=ate purge

#
# IFS Interface
#
ifs_build:
	$(MAKE) --directory=ifs build

ifs_clean:
	$(MAKE) --directory=ifs clean

ifs_purge:
	$(MAKE) --directory=ifs purge

#
# IFS Interface
#
ifeq ($(RI_HOST),i686-pc-mingw32)
w32-kill_build:
	$(MAKE) --directory=w32-kill build

w32-kill_clean:
	$(MAKE) --directory=w32-kill clean

w32-kill_purge:
	$(MAKE) --directory=w32-kill purge
endif

#
# Remap library
#
remap_build:
	$(MAKE) --directory=remap build

remap_clean:
	$(MAKE) --directory=remap clean

remap_purge:
	$(MAKE) --directory=remap purge

#
# CableLabs GStreamer Plugins
#
cl_gst_plugin:
	$(MAKE) --directory=gstreamer

cl_gst_plugin_clean:
	$(MAKE) --directory=gstreamer clean

cl_gst_plugin_purge:
	$(MAKE) --directory=gstreamer purge

#
# Configuration library
#
config_build:
	$(MAKE) --directory=config

config_clean:
	$(MAKE) --directory=config clean

config_purge:
	$(MAKE) --directory=config purge

#
# Tuner library
#
tuner_build:
	$(MAKE) --directory=tuner

tuner_clean:
	$(MAKE) --directory=tuner clean

tuner_purge:
	$(MAKE) --directory=tuner purge

#
# Logging library
#
logging_build:
	$(MAKE) --directory=logging

logging_clean:
	$(MAKE) --directory=logging clean

logging_purge:
	$(MAKE) --directory=logging purge

#
# Launcher 
#
launcher_build:
	$(MAKE) --directory=launcher

launcher_clean:
	$(MAKE) --directory=launcher clean

launcher_purge:
	$(MAKE) --directory=launcher purge

#
# SNMP
#
snmp_build:
	$(MAKE) --directory=snmp

snmp_clean:
	$(MAKE) --directory=snmp clean

snmp_purge:
	$(MAKE) --directory=snmp purge

#
# User Interface directory (ui)
#
ui_build:
	$(MAKE) --directory=ui/$(UI_TARGET)

ui_clean:
	$(MAKE) --directory=ui/$(UI_TARGET) clean

ui_purge:
	$(MAKE) --directory=ui/$(UI_TARGET) purge

#
# Clean and purge
#
ifeq ($(RI_HOST),i686-pc-mingw32)
clean: ate_clean launcher_clean logging_clean config_clean ifs_clean remap_clean cl_gst_plugin_clean tuner_clean ui_clean snmp_clean w32-kill_clean
	$(RMTREE) $(OBJDIR)
else
clean: ate_clean launcher_clean logging_clean config_clean ifs_clean remap_clean cl_gst_plugin_clean tuner_clean ui_clean snmp_clean 
	$(RMTREE) $(OBJDIR)
endif

ifeq ($(RI_HOST),i686-pc-mingw32)
purge: ate_purge launcher_purge logging_purge config_purge ifs_purge remap_purge cl_gst_plugin_purge tuner_purge ui_purge snmp_purge w32-kill_purge
	$(call RM_LIBRARY,$(LIB))
else
purge: ate_purge launcher_purge logging_purge config_purge ifs_purge remap_purge cl_gst_plugin_purge tuner_purge ui_purge snmp_purge
	$(call RM_LIBRARY,$(LIB))
endif

