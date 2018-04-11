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
# Build the MPE module (Simulator Debug).
#
# @author Greg Rutz - Vidiom Systems Inc
# @modified Mark Millard = Vidiom Systems Inc
#
#############################################################################

default: build

#
# Include top-level build definitions
#
include $(OCAPROOT)/defs.mk

#
# Explicitly name the default build target
#
.PHONY: libobjs

PLATFORM_INSTALL = $(PLATFORMROOT)/install/$(PLATFORMTC)

#
# Help for this makefile
#
help:
	@echo "Build MPE module"
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
LIBDIR	=	$(OCAP_GEN)/lib
OBJDIR	=	$(OCAP_GEN)/sim

#
# Compile options
#
#
COPTS		=	

#
# Library definitions
#
MPE_SHLIB	=	$(OCAP_BIN)/bin/libmpe.so
DFB_SHLIB	=	$(OCAP_BIN)/bin/directfb.so

#
# Include dependency files
#
DEPENDS	=	$(patsubst %.c,$(OBJDIR)/%.d,$(MODULE_SRC))
#ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
#-include $(DEPENDS)
#endif

#
# Build everything
#
build: $(DFB_SHLIB) $(MPE_SHLIB)

.PHONY: $(DFB_SHLIB) $(MPE_SHLIB)

DFB_LINK_OPTS = -L$(LIBDIR) -lft2

#
# Build the DirectFB library
#
$(DFB_SHLIB):
	$(MKDIR) tmp_dfb
	$(CD) tmp_dfb && \
	$(LIBGEN) -x $(LIBDIR)/libdfb.a
	$(call BUILD_SHARED_LIBRARY, tmp_dfb/*.o $(DFB_LINK_OPTS))
	$(RMTREE) tmp_dfb


MPE_LINK_OPTS = \
	-L$(LIBDIR) -ldfb \
	-L$(PLATFORM_INSTALL)/lib \
	-L$(PLATFORM_INSTALL)/bin \
	-lplatform \
	-lglib-2.0

#
# Build the MPE library
#
$(MPE_SHLIB):
	$(MKDIR) tmp
	$(CD) tmp && \
	$(LIBGEN) -x $(LIBDIR)/libmpe.a && \
	$(LIBGEN) -x $(LIBDIR)/libmpeos.a && \
	$(LIBGEN) -x $(LIBDIR)/libft2.a && \
	$(LIBGEN) -x $(LIBDIR)/libzlib.a && \
	$(LIBGEN) -x $(LIBDIR)/libmpejni.a
	$(call BUILD_SHARED_LIBRARY, tmp/*.o $(MPE_LINK_OPTS))
	$(RMTREE) tmp

#
# Bring header file dependencies up to date
#
$(OBJDIR)/%.d: %.c
	@$(call BUILD_DEPENDS,$(COPTS))

$(LIB_OBJS): $(call makefile-list)
	@$(call COMPILE,$(COPTS))

#
# Clean and purge
#
clean:
	@$(RM) $(LIB_OBJS)

purge:
	$(RM) $(MPE_SHLIB) $(DFB_SHLIB)

