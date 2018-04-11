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
# Build ARES (DNS Resolver) library
#
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
	@echo "Build the ARES (DNS Resolver) Library"
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
OBJDIR      =	$(OCAP_GEN)/zlib

#
# Compile options
#
COPTS		=	-I. -I$(OCAP_MPEOS)/$(BUILD_OS)/include

#
# Library built by this makefile
#
LIB			=	$(LIBDIR)/libzlib.a

#
# Source files
#
SOURCES		=   adler32.c \
                compress.c \
                crc32.c \
                gzio.c \
                uncompr.c \
                deflate.c \
                trees.c \
                zutil.c \
                inflate.c \
                infback.c \
                inftrees.c \
                inffast.c

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

