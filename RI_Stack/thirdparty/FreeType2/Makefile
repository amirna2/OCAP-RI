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
# Build FreeType2 library
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
	@echo "Build the FreeType2 Library"
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
OBJDIR      =	$(OCAP_GEN)/freetype2

#
# Compile options
#
COPTS	  	=	-I./include \
				-I./include/freetype \
				-I./include/freetype/internal \
				-I./include/freetype/config \
				-I./include/freetype/cache \
				-I$(OCAP_DIRECTFB)/include \
				-I$(OCAP_MPE)/include \
				-I$(OCAP_MPEOS)/include \
				-I$(OCAP_MPEOS)/$(BUILD_OS)/include

#
# Library built by this makefile
#
LIB			=	$(LIBDIR)/libft2.a

#
# Source files
#
SOURCES		=   src/base/ftbase.c \
				src/base/ftbbox.c \
				src/base/ftdebug.c \
				src/base/ftinit.c \
				src/base/ftmm.c \
				src/base/ftpfr.c \
				src/base/ftsystem.c \
				src/base/ftglyph.c \
				src/cache/ftcache.c \
				src/pshinter/pshinter.c \
				src/autohint/autohint.c \
				src/psnames/psnames.c \
				src/psaux/psaux.c \
				src/sfnt/sfnt.c \
				src/pfr/pfr.c \
				src/cff/cff.c \
				src/truetype/truetype.c \
				src/raster/raster.c \
				src/smooth/smooth.c

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

