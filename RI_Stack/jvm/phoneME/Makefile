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

default: build install

include $(OCAPROOT)/hostconfig/$(OCAPHOST)/hostdefs.mk
include $(OCAPROOT)/target/$(OCAPTC)/buildrules.properties

###
#
# COMPONENTS
#
###
CORE = cdc tools
EXTRAS = xmlparser jsr280
COMPONENTS = $(CORE) $(EXTRAS)



###
#
# OCAP_VMLIB
#
###
OCAP_VMLIB_DIR = ../ocap_vmlib

OCAP_VMLIB_DEPENDS = $(shell find ${OCAP_VMLIB_DIR} -mindepth 1 -maxdepth 1 -name .svn -o -prune -type d -print)
OCAP_VMLIB_TARGETS =

# makeVmlibDeps(src_dir,dst_dir)
define makeVmlibDeps
  $(1)_JAVA_DEPENDS   = $(shell find $(1) -name *.java)
  $(1)_JAVA_TARGETS   = $$(subst $(1),$(2)/classes,$$($(1)_JAVA_DEPENDS))
  $(1)_NATIVE_DEPENDS = $(shell find $(1) -name *.[ch])
  $(1)_NATIVE_TARGETS = $$(subst $(1),$(2),$$($(1)_NATIVE_DEPENDS))
  OCAP_VMLIB_TARGETS += $$($(1)_JAVA_TARGETS) $$($(1)_NATIVE_TARGETS)
  $$($(1)_JAVA_TARGETS): $(2)/classes/%: $(1)/%
	@$(MKDIR) $$(@D)
	@$(CP) -v $$< $$(@D)
  $$($(1)_NATIVE_TARGETS): $(2)/%: $(1)/%
	@$(MKDIR) $$(@D)
	@$(CP) -v $$< $$(@D)	
endef

$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/awt,cdc/src/mpe/awt))
$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/io,cdc/src/mpe))
$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/net,cdc/src/mpe))
$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/focus,cdc/src/mpe/awt))
$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/ixc,cdc/src/mpe))
$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/appctx,cdc/src/mpe))
$(eval $(call makeVmlibDeps,$(OCAP_VMLIB_DIR)/reclaim,cdc/src/mpe))



###
#
# MPE_JVM
#
###
MPE_JVM_DIR = ./mpe_jvm

MPE_JVM_DEPENDS = $(shell find ${MPE_JVM_DIR} -name .svn -prune -o -type f -print)
MPE_JVM_TARGETS = $(subst ${MPE_JVM_DIR},cdc,${MPE_JVM_DEPENDS})

$(MPE_JVM_TARGETS): cdc/%: mpe_jvm/%
	@$(MKDIR) $(@D)
	@$(CP) -v $< $(@D)



###
#
# MPE_TOOLS
#
###
MPE_TOOLS_BUILD_DEPENDS = $(foreach mk,defs_jdwp.mk defs_jvmti_hprof.mk,cdc/build/${build.jvm.TOOLS_OS}/${mk})
ifeq "${build.jvm.TOOLS_OS}" "win32"
  MPE_TOOLS_BUILD_DEPENDS += $(foreach mk,rules_jvmti_hprof.mk,cdc/build/${build.jvm.TOOLS_OS}/${mk})
endif
MPE_TOOLS_BUILD_TARGETS = $(subst ${build.jvm.TOOLS_OS},mpe,${MPE_TOOLS_BUILD_DEPENDS})

$(MPE_TOOLS_BUILD_TARGETS): cdc/build/mpe/%: cdc/build/${build.jvm.TOOLS_OS}/%
	@$(MKDIR) $(@D)
	@$(CP) -v $< $(@D)

ifeq "$(wildcard cdc)" ""
  MPE_TOOLS_SRC_DEPENDS = $(filter-out %/,$(shell tar jtf cdc.tar.bz2 --anchored cdc/src/${build.jvm.TOOLS_OS}/tools))
else
  MPE_TOOLS_SRC_DEPENDS = $(shell find cdc/src/${build.jvm.TOOLS_OS}/tools -type f -print)
endif

ifeq "${build.jvm.TOOLS_OS}" "win32"
  ifeq "$(wildcard cdc)" ""
    MPE_TOOLS_SRC_WIN32_DEPENDS = $(filter-out %/,$(shell tar jtf cdc.tar.bz2 --anchored cdc/src/${build.jvm.TOOLS_OS}/javavm/include/ansi))
  else
    MPE_TOOLS_SRC_WIN32_DEPENDS = $(shell find cdc/src/${build.jvm.TOOLS_OS}/javavm/include/ansi -type f -print)
  endif
  MPE_TOOLS_SRC_DEPENDS += $(MPE_TOOLS_SRC_WIN32_DEPENDS)
endif

MPE_TOOLS_SRC_TARGETS = $(subst ${build.jvm.TOOLS_OS},mpe,${MPE_TOOLS_SRC_DEPENDS})

$(MPE_TOOLS_SRC_TARGETS): cdc/src/mpe/%: cdc/src/${build.jvm.TOOLS_OS}/%
	@$(MKDIR) $(@D)
	@$(CP) -v $< $(@D)

cdc/src/mpe/tools/jpda/back/mpelib_init.c: cdc/src/mpe/javavm/runtime/mpelib_init.c
	@$(CP) -v $< $(@D)
cdc/src/mpe/tools/jvmti/hprof/mpelib_init.c: cdc/src/mpe/javavm/runtime/mpelib_init.c
	@$(CP) -v $< $(@D)

MPE_TOOLS_DEPENDS = $(MPE_TOOLS_BUILD_DEPENDS) $(MPE_TOOLS_SRC_DEPENDS) \
                    cdc/src/javavm/runtime/mpelib_init.c
MPE_TOOLS_TARGETS = $(MPE_TOOLS_BUILD_TARGETS) $(MPE_TOOLS_SRC_TARGETS) \
                    cdc/src/mpe/tools/jpda/back/mpelib_init.c cdc/src/mpe/tools/jvmti/hprof/mpelib_init.c



###
#
# Install
#
###
INSTALL_DIR = ../../bin/$(OCAPTC)/env/cvm

INSTALL_BIN_DEPENDS = $(foreach bin,${build.jvm.BINARIES},cdc/build/${build.jvm.BUILDDIR}/${bin})
INSTALL_BIN_TARGETS = $(foreach bin,${INSTALL_BIN_DEPENDS},${INSTALL_DIR}/bin/$(notdir ${bin}))

INSTALL_JARS = $(filter-out cdc/build/${build.jvm.BUILDDIR}/lib/java_crw_demo.jar,$(wildcard cdc/build/${build.jvm.BUILDDIR}/lib/*.jar))
INSTALL_LIB_DEPENDS = $(INSTALL_JARS) $(foreach file,content-types.properties security/java.policy security/java.security,cdc/build/${build.jvm.BUILDDIR}/lib/$(file))
INSTALL_LIB_TARGETS = $(foreach lib,$(subst cdc/build/${build.jvm.BUILDDIR}/,,${INSTALL_LIB_DEPENDS}),${INSTALL_DIR}/$(lib))

STACK_JAR = $(OCAPROOT)/tools/generic/java/pbp11.jar

###
#
# Patching
#
###

# makePatchFileList(dir,exclusions)
#define makePatchFileList
#  $(1)_ALLFILES = $(shell echo find $(1) -type f | sed 's/\$$/\\\$$/g')
#  $(1)_EXCLUDED = $(shell echo $(2) | sed 's/\$$/\\\$$/g')
#  $(1)_PATCHLST = $(filter-out $(1)_EXCLUDED,$(1)_ALLFILES)
#endef
#
#$(eval $(call makePatchFileList,cdc,$(CDC_EXCLUDED)))

cdc_ALLFILES = $(shell find cdc -type f | sed 's/\$$/\\\$$/g')
cdc_EXCLUDED = $(shell echo ${MPE_JVM_TARGETS} \
                            ${MPE_TOOLS_TARGETS} \
                            ${OCAP_VMLIB_TARGETS} \
                            $(subst ./,,$(shell find cdc/build/${build.jvm.BUILDDIR} -type f -print 2>/dev/null | sed 's/\$$/\\\$$/g')) \
                       | sed 's/\$$/\\\$$/g' )
cdc_PATCHLST = $(filter-out $(cdc_EXCLUDED),$(cdc_ALLFILES))

tools_ALLFILES = $(shell find tools -type f | sed 's/\$$/\\\$$/g')
tools_EXCLUDED = $(shell find tools/output -type f | sed 's/\$$/\\\$$/g')
tools_PATCHLST = $(filter-out $(tools_EXCLUDED),$(tools_ALLFILES))

%-orig: %.tar.bz2
	@touch $*.patch
	mkdir -p $@
	tar jxvfm $< --strip-components=1 -C $@

.FORCE:
%.patch: %-orig .FORCE
	mv $@ $@.bak
	@for file in ${$*_PATCHLST}; do \
		echo "Comparing $$file..."; \
		diff -Nu `echo $$file | sed 's/^$*/$*-orig/g'` $$file >> $@; \
	done

###
#
# Pattern Rules (non-static)
#
###

.PHONY: build install clean purge

phoneme: $(COMPONENTS) mpe_jvm mpe_tools ocap_vmlib

%: %.tar.bz2
	tar jxvfm $<
	patch -p0 -i $@.patch
	-unix2dos cdc/src/share/basis/classes/common/java/awt/EventDispatchThread.java

mpe_jvm: $(MPE_JVM_TARGETS)

ocap_vmlib: $(OCAP_VMLIB_TARGETS)

mpe_tools: cdc mpe_jvm $(MPE_TOOLS_TARGETS)

build: phoneme
	$(MAKE) -C cdc/build/${build.jvm.BUILDDIR} $(build.jvm.BUILD_TARGET)

install:
	$(MKDIR) $(INSTALL_DIR)/bin $(INSTALL_DIR)/lib/security
	@$(CP) -v -t $(INSTALL_DIR)/bin $(INSTALL_BIN_DEPENDS)
	@find cdc/build/${build.jvm.BUILDDIR}/lib/*.jar -name java_crw_demo.jar -prune -o -exec cp -v -t $(INSTALL_DIR)/lib {} \;
	@$(CP) -v cdc/build/${build.jvm.BUILDDIR}/lib/content-types.properties $(INSTALL_DIR)/lib
	@$(CP) -v -t $(INSTALL_DIR)/lib/security cdc/security/*

stackjar:
	@jar cvf $(STACK_JAR) -C cdc/build/${build.jvm.BUILDDIR}/btclasses .
	@jar uvf $(STACK_JAR) -C cdc/build/${build.jvm.BUILDDIR}/basis_classes .
	@for extra in $(EXTRAS); do \
		jar uvf $(STACK_JAR) -C cdc/build/${build.jvm.BUILDDIR}/$$extra/classes . ;\
	done

clean:
	@if [ -d cdc/build/${build.jvm.BUILDDIR} ]; then \
		$(MAKE) -C cdc/build/${build.jvm.BUILDDIR} $(build.jvm.CLEAN_TARGET) ; \
	fi

purge:
	$(RMTREE) $(foreach component,$(COMPONENTS),$(component) $(component)-orig)
