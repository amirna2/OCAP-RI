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

.PHONY: build clean purge

#
# Help for this makefile
#
help:
	@echo "Build the RI launcher"
	@echo ""
	@echo "Environment variables:"
	@echo "    PLATFORMTC - The current target configuration."
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
OBJDIR 		= $(PLATFORM_GEN)/launcher
BINDIR		= $(PLATFORM_INST_DIR)/bin

#
# Compile options
#
COPTS = \
	-I./$(PLATFORM_OS) \
	-I$(PLATFORM_INST_DIR)/include \
	-I$(PLATFORM_INST_DIR)/include/glib-2.0 \
	-I$(PLATFORM_INST_DIR)/lib/glib-2.0/include \
	-I$(PLATFORM_SRC)/config 

#
# Executable built by this makefile
#
EXE = $(BINDIR)/ri$(EXE_SUFFIX)

EXE_SOURCES = main.c \
	          ./$(PLATFORM_OS)/port.c

# MinGW GCC does not support RPATH
ifeq ($(RI_HOST),i686-pc-linux-gnu)
EXE_LIBS = \
	-Wl,-rpath -Wl,$(LAUNCHER_RPATH) -ldl \
	-L$(PLATFORM_INST_DIR)/lib \
	-lglib-2.0 \
	-lri_config -lpthread
else
EXE_LIBS = \
	-L$(PLATFORM_INST_DIR)/bin \
	-L$(PLATFORM_INST_DIR)/lib \
	-lglib-2.0 \
	-lri_config -lpthreadGC2
endif

#
# Intermediate files
#
EXE_OBJS = $(patsubst %.c,$(OBJDIR)/%.o,$(EXE_SOURCES))

#
# Include dependency files
#
DEPENDS = $(patsubst %.c,$(OBJDIR)/%.d,$(EXE_SOURCES))

ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
-include $(DEPENDS)
endif

#
# Build everything
#
build: $(EXE)

$(EXE): $(EXE_OBJS)
	$(call BUILD_EXECUTABLE,$(EXE_OBJS) $(EXE_LIBS)) 

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
	$(RM) $(EXE)
