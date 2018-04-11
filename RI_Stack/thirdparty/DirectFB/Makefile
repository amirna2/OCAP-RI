#############################################################################
#                                                                           #
#                       Copyright 2004 OCAP Development LLC                 #
#                              All rights reserved                          #
#                            Reproduced Under License                       #
#                                                                           #
#  This source code is the proprietary confidential property of             #
#  OCAP Development LLC and is provided to recipient for documentation and  #
#  educational purposes only. Reproduction, publication, or distribution in #
#  any form to any party other than the recipient is strictly prohibited.   #
#                                                                           #
#############################################################################
#
# Build DirectFB library
#
# @author Amir Nathoo - Vidiom Systems Corporation
# @author Aaron Kamienski - Vidiom Systems Corporation
# @author Todd Earles - Vidiom Systems Corporation
#
#############################################################################

default: build

#
# Include top-level build definitions
#
include $(OCAPROOT)/defs.mk

#
# Help for this makefile
#
help:
	@echo "Build the DirectFB Library"
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
LIBDIR		=	$(OCAP_GEN)/lib
OBJDIR      =	$(OCAP_GEN)/directfb

#
# Compile options
#
COPTS		=	-I. \
				-I$(OBJDIR) \
				-I$(OCAP_DIRECTFB) \
				-I$(OCAP_DIRECTFB)/include \
				-I$(OCAP_DIRECTFB)/src \
				-I$(OCAP_FREETYPE2)/include \
				-I$(OCAP_MPE)/include \
				-I$(OCAP_MPEOS)/include \
				-I$(OCAP_MPEOS)/$(BUILD_OS)/include \
				-I$(OCAP_GEN)/mpe \
				$(DFB_DEFINES)

#
# Library built by this makefile
#
LIB			=	$(LIBDIR)/libdfb.a

#
# Source files
#
SOURCES		=	\
				src/gfx/clip.c \
				src/display/idirectfbsurface_layer.c \
				src/core/colorhash.c \
				src/misc/conf.c \
				src/gfx/convert.c \
				src/core/core.c \
				src/core/core_parts.c \
				src/directfb.c \
				src/core/fonts.c \
				src/gfx/generic/generic.c \
				src/core/gfxcard.c \
				src/gfx/gfxutil.c \
				src/idirectfb.c \
				src/display/idirectfbdisplaylayer.c \
				src/media/idirectfbfont.c \
				interfaces/IDirectFBFont/idirectfbfont_ft2.c \
				src/display/idirectfbpalette.c \
				src/display/idirectfbsurface.c \
				src/display/idirectfbsurface.c \
				src/interface.c \
				src/core/layers.c \
				src/core/fusion/list.c \
				src/misc/mem.c \
				src/core/fusion/object.c \
				src/core/palette.c \
				src/core/fusion/ref.c \
				src/core/state.c \
				src/core/surfacemanager.c \
				src/core/surfaces.c \
				src/core/system.c \
				src/misc/tree.c \
				src/misc/utf8.c \
				src/misc/util.c \
				src/core/fusion/lock.c \
				src/ocap/arc.c \
				src/ocap/oval.c \
				src/ocap/polygon.c

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
	$(RM) $(OBJS)
purge:
	$(call RM_LIBRARY,$(LIB))
