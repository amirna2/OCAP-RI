#COPYRIGHT_BEGIN
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
# 
# Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
# 
# This software is available under multiple licenses: 
# 
# (1) BSD 2-clause 
#  Redistribution and use in source and binary forms, with or without modification, are
#  permitted provided that the following conditions are met:
#       ·Redistributions of source code must retain the above copyright notice, this list 
#            of conditions and the following disclaimer.
#       ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
#            and the following disclaimer in the documentation and/or other materials provided with the 
#            distribution.
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
#  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
#  PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
#  HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
#  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
#  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
#  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
# 
# (2) GPL Version 2
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, version 2. This program is distributed
#  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
#  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
#  PURPOSE. See the GNU General Public License for more details.
# 
#  You should have received a copy of the GNU General Public License along
#  with this program.If not, see<http:www.gnu.org/licenses/>.
# 
# (3)CableLabs License
#  If you or the company you represent has a separate agreement with CableLabs
#  concerning the use of this code, your rights and obligations with respect
#  to this code shall be as set forth therein. No license is granted hereunder
#  for any other purpose.
# 
#  Please contact CableLabs if you need additional information or 
#  have any questions.
# 
#      CableLabs
#      858 Coal Creek Cir
#      Louisville, CO 80027-9750
#      303 661-9100
#COPYRIGHT_END

# Configurations for Win32/Cygwin targets
#

# The following build targets are always phony
#
.PHONY: default help build clean purge FORCE
FORCE:

#
# Common commands used throughout the build environment
#
CD     = cd 
CP     = cp
CPTREE = cp -r
CHMOD  = chmod
ECHO   = echo
MKDIR  = mkdir -p
PERL   = perl
RM     = rm -f
RMTREE = rm -rf
SED    = sed
TOUCH  = touch
XARGS  = xargs

CC  = gcc-3
CXX = g++-3

CFLAGS = $(BUILD_CFLAGS) -mno-cygwin

CXXFLAGS = $(CFLAGS)

CPPFLAGS = $(BUILD_CPPFLAGS) -Werror-implicit-function-declaration

LIBGEN := ar
LIBGEN_OPTS := -cr

SHARED_LIBGEN := $(CC)
SHARED_LIBGEN_OPTS := -Wl,--kill-at -shared -mwindows -mno-cygwin --enable-auto-import

EXEGEN := $(CC)
EXEGEN_OPTS := -mwindow -mno-cygwin

define EXTRACT_TAR_GZ
	tar zxvf $1
endef

define EXTRACT_TAR_BZ2
	tar jxvf $1
endef

define APPLY_PATCH
	patch -p0 < $1
endef

#
# Compile C or CXX source file into an intermediate file
#
# $@ = The name of the object (.o) file to create
# $< = The name of the source (.c or .cpp) file (must be first prerequisite listed in rule)
# $1 = Additional compile options
#
define COMPILE
    @$(ECHO) Compiling $< into $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    $(if $(filter .c,$(suffix $<)), $(CC) -c $(CPPFLAGS) $(CFLAGS) $1 $< -o $@,)
    $(if $(filter .cpp,$(suffix $<)), $(CXX) -c $(CPPFLAGS) $(CXXFLAGS) $1 $< -o $@,)
endef

#
# Build a dependency file from a C or CXX source file
#
# $@ = The name of the dependency (.d) file to create
# $< = The name of the source (.c or .cpp) file (must be first prerequisite listed in rule)
# $1 = Additional compile options
#
define BUILD_DEPENDS
    @$(ECHO) Building dependency file for $<
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    $(if $(filter .c,$(suffix $<)), $(CC) -M $(CPPFLAGS) $(CFLAGS) $1 $< > $@.tmp,)
    $(if $(filter .cpp,$(suffix $<)), $(CXX) -M $(CPPFLAGS) $(CXXFLAGS) $1 $< > $@.tmp,)
    $(SED) 's,.*\.o[ :]*,$(@:.d=.o) $@ : ,g' < $@.tmp > $@
    @$(RM) $@.tmp
endef

#
# Build a library from a list of object files
#
# $@ = The name of the library (.a) file to create
# $1 = The list of all object (.o) files to put into the library
#
define BUILD_LIBRARY
    @$(ECHO) Building library $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    @$(MKDIR) $(dir $@)
    $(LIBGEN) $(LIBGEN_OPTS) $@ $1
endef

#
# Build a shared library from a list of object files
#
# $@ = The name of the library (.dll) file to create
# $1 = The list of all object (.o) files to put into the library
#
define BUILD_SHARED_LIBRARY
    @$(ECHO) Building shared library $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    @$(MKDIR) $(dir $@)
    $(SHARED_LIBGEN) $(SHARED_LIBGEN_OPTS) -o $@ $1
endef

#
# Build an executable from a list of object files
#
# $@ = The name of the executable (.exe) file to create
# $1 = The list of all object (.o) files to put into the library
#
define BUILD_EXECUTABLE
    @$(ECHO) Building executable $@
    @$(MKDIR) $(dir $@)
    @$(RM) $@
    @$(MKDIR) $(dir $@)
    $(EXEGEN) $(EXEGEN_OPTS) -o $@ $1
endef

#
# Convert from a platform-dependent path to a unix-style path
#
TO_UNIX_PATH = $(shell cygpath $1)

