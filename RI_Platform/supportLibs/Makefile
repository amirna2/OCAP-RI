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
# Build CableLabs RI MPE OS library
#
# @author gdr - Vidiom Systems Inc
#
#############################################################################

default: build

#
# Include top-level build definitions
#
include $(PLATFORMROOT)/defs.mk

# Supports libs generation dir
SL_GENDIR = $(PLATFORM_GEN)/supportLibs

#
# Root directory location
#
GETTEXT_ARCHIVE  = gettext-0.17.tar.gz
GETTEXT_ROOT     = $(SL_GENDIR)/gettext-0.17
HDHR_ARCHIVE     = libhdhomerun_20100828.tar.gz
HDHR_ROOT        = $(SL_GENDIR)/libhdhomerun
OIL_ARCHIVE      = liboil-0.3.15.tar.gz
OIL_ROOT         = $(SL_GENDIR)/liboil-0.3.15
ICONV_ARCHIVE    = libiconv-1.12.tar.gz
ICONV_ROOT       = $(SL_GENDIR)/libiconv-1.12
XML2_ARCHIVE     = libxml2-2.6.32.tar.gz
XML2_ROOT        = $(SL_GENDIR)/libxml2-2.6.32
PTHREADS_ARCHIVE = pthreads-w32-2-8-0-release.tar.gz
PTHREADS_ROOT    = $(SL_GENDIR)/pthreads-w32-2-8-0-release
LOG4C_ARCHIVE    = liblog4c-1.2.1.tar.gz
LOG4C_ROOT       = $(SL_GENDIR)/log4c-1.2.1
ZLIB_ARCHIVE     = zlib-1.2.3.tar.gz
ZLIB_ROOT        = $(SL_GENDIR)/zlib-1.2.3
FFMPEG_ARCHIVE   = ffmpeg-0.5.tar.bz2
FFMPEG_ROOT      = $(SL_GENDIR)/ffmpeg-0.5
NETSNMP_ARCHIVE  = net-snmp-5.6.1.tar.gz
NETSNMP_ROOT     = $(SL_GENDIR)/net-snmp-5.6.1

.PHONY: build clean purge \
        gettext gettext_build gettext_clean gettext_purge gettext_install \
        hdhr hdhr_build hdhr_clean hdhr_purge hdhr_install \
        oil oil_build oil_clean oil_purge oil_install \
        iconv iconv_build iconv_clean iconv_purge iconv_install \
        zlib zlib_build zlib_clean zlib_purge zlib_install \
        xml2 xml2_build xml2_clean xml2_purge xml2_install \
        log4c log4c_build log4c_clean log4c_purge log4c_install \
        ffmpeg ffmpeg_build ffmpeg_clean ffmpeg_purge ffmpeg_install \
        netsnmp netsnmp_build netsnmp_clean netsnmp_purge netsnmp_install \
        syslog-client syslog-client_build syslog-client_clean syslog-client_purge

# Only required for windows builds
ifeq ($(RI_HOST),i686-pc-mingw32)
SYSLOG_CLIENT_ROOT = syslog-client
endif

ifeq ($(RI_HOST),i686-pc-linux-gnu)
build: log4c ffmpeg hdhr netsnmp
else
build: oil iconv gettext hdhr pthreads zlib xml2 log4c ffmpeg netsnmp 
endif

make_gendir:
	$(MKDIR) $(SL_GENDIR)

#
# gettext Build Targets
#
include gettext.mk
gettext: make_gendir gettext_dir gettext_build gettext_install

gettext_dir:
	if [ ! -d $(GETTEXT_ROOT) ]; then \
		$(call extract_tar_gz,$(GETTEXT_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/$(GETTEXT_CONF_CACHE_FILE) ]; then \
			$(CP) $(PLATFORM_TARGET_OS_DIR)/$(GETTEXT_CONF_CACHE_FILE) $(GETTEXT_ROOT); \
		fi \
	fi

$(GETTEXT_ROOT)/Makefile: gettext.mk
	cd $(GETTEXT_ROOT) && $(GETTEXT_CONFIGURE)

gettext_build: $(GETTEXT_ROOT)/Makefile
	$(MAKE) --directory=$(GETTEXT_ROOT)

gettext_clean:
	if [ -e $(GETTEXT_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GETTEXT_ROOT) clean; \
	fi

gettext_purge:
	if [ -e $(GETTEXT_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GETTEXT_ROOT) uninstall; \
	fi
	$(RMTREE) $(GETTEXT_ROOT)

gettext_install: $(GETTEXT_ROOT)/Makefile
	if [ -e $(GETTEXT_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(GETTEXT_ROOT) install; \
	fi

#
# hdhr Build Targets
#
hdhr: make_gendir hdhr_dir hdhr_build hdhr_install

hdhr_dir:
	if [ ! -d $(HDHR_ROOT) ]; then \
		$(call extract_tar_gz,$(HDHR_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/hdhr.patch ]; then \
			$(call apply_patch,$(PLATFORM_TARGET_OS_DIR)/hdhr.patch,$(SL_GENDIR)); \
		fi \
	fi

$(HDHR_ROOT)/Makefile:
	cd $(HDHR_ROOT)

hdhr_build: $(HDHR_ROOT)/Makefile
ifeq ($(OS),Windows_NT)
	$(MAKE) CC=$(RI_CC) --directory=$(HDHR_ROOT) libhdhomerun.dll
else
	$(MAKE) CC=$(RI_CC) --directory=$(HDHR_ROOT) libhdhomerun.so
endif

hdhr_clean:
	if [ -e $(HDHR_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(HDHR_ROOT) clean; \
	fi

hdhr_purge:
	if [ -e $(HDHR_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(HDHR_ROOT) clean; \
	fi
ifeq ($(OS),Windows_NT)
	$(RM) $(PLATFORM_INST_DIR)/bin/libhdhomerun.dll 
else
	$(RM) $(PLATFORM_INST_DIR)/lib/libhdhomerun.so 
endif
	$(RM) $(PLATFORM_INST_DIR)/include/hdhomerun*
	$(RMTREE) $(HDHR_ROOT)

hdhr_install: $(HDHR_ROOT)/Makefile
	$(CP) $(HDHR_ROOT)/hdhomerun.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_channels.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_channelscan.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_control.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_debug.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_device.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_device_selector.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_discover.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_os.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_os_posix.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_os_windows.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_pkt.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_sock.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_types.h $(PLATFORM_INST_DIR)/include
	$(CP) $(HDHR_ROOT)/hdhomerun_video.h $(PLATFORM_INST_DIR)/include
ifeq ($(OS),Windows_NT)
	$(CP) $(HDHR_ROOT)/libhdhomerun.dll $(PLATFORM_INST_DIR)/bin
else
	$(CP) $(HDHR_ROOT)/libhdhomerun.so $(PLATFORM_INST_DIR)/lib
endif

#
# liboil Build Targets
#
include oil.mk
oil: make_gendir oil_dir oil_build oil_install

oil_dir:
	if [ ! -d $(OIL_ROOT) ]; then \
		$(call extract_tar_gz,$(OIL_ARCHIVE),$(SL_GENDIR)); \
	fi

$(OIL_ROOT)/Makefile: oil.mk
	cd $(OIL_ROOT) && $(OIL_CONFIGURE)

oil_build: $(OIL_ROOT)/Makefile
	$(MAKE) --directory=$(OIL_ROOT)

oil_clean:
	if [ -e $(OIL_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(OIL_ROOT) clean; \
	fi

oil_purge:
	if [ -e $(OIL_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(OIL_ROOT) uninstall; \
	fi
	$(RMTREE) $(OIL_ROOT)

oil_install: $(OIL_ROOT)/Makefile
	if [ -e $(OIL_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(OIL_ROOT) install; \
	fi

#
# libiconv Build Targets
#
include iconv.mk
iconv: make_gendir iconv_dir iconv_build iconv_install

iconv_dir:
	if [ ! -d $(ICONV_ROOT) ]; then \
		$(call extract_tar_gz,$(ICONV_ARCHIVE),$(SL_GENDIR)); \
	fi

$(ICONV_ROOT)/Makefile: iconv.mk
	cd $(ICONV_ROOT) && $(ICONV_CONFIGURE)

iconv_build: $(ICONV_ROOT)/Makefile
	$(MAKE) --directory=$(ICONV_ROOT)

iconv_clean:
	if [ -e $(ICONV_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ICONV_ROOT) clean; \
	fi

iconv_purge:
	if [ -e $(ICONV_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ICONV_ROOT) uninstall; \
	fi
	$(RMTREE) $(ICONV_ROOT)

iconv_install: $(ICONV_ROOT)/Makefile
	if [ -e $(ICONV_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ICONV_ROOT) install; \
	fi

#
# libxml2 Build Targets
#
include xml2.mk
xml2: make_gendir xml2_dir xml2_build xml2_install

xml2_dir:
	if [ ! -d $(XML2_ROOT) ]; then \
		$(call extract_tar_gz,$(XML2_ARCHIVE),$(SL_GENDIR)); \
	fi

$(XML2_ROOT)/Makefile: xml2.mk
	cd $(XML2_ROOT) && $(XML2_CONFIGURE)

xml2_build: $(XML2_ROOT)/Makefile
	$(MAKE) --directory=$(XML2_ROOT)

xml2_clean:
	if [ -e $(XML2_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(XML2_ROOT) clean; \
	fi

xml2_purge:
	$(RMTREE) $(XML2_ROOT)

xml2_install: $(XML2_ROOT)/Makefile
	if [ -e $(XML2_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(XML2_ROOT) install; \
	fi

#
# pthreads win32 Build Targets
#
pthreads: make_gendir pthreads_dir pthreads_build pthreads_install

pthreads_dir:
	if [ ! -d $(PTHREADS_ROOT) ]; then \
		$(call extract_tar_gz,$(PTHREADS_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/pthreads.patch ]; then \
			$(call apply_patch,$(PLATFORM_TARGET_OS_DIR)/pthreads.patch,$(SL_GENDIR)); \
		fi \
	fi

pthreads_build: pthreads_dir
	if [ -d $(PTHREADS_ROOT) ]; then \
		$(MAKE) --directory=$(PTHREADS_ROOT) PTW32_FLAGS=$(PTHREADS_CFLAGS) GC; \
	fi

pthreads_test: pthreads_build
	if [ -d $(PTHREADS_ROOT) ]; then \
		$(MAKE) --directory=$(PTHREADS_ROOT)/tests GC; \
	fi

pthreads_clean:
	if [ -d $(PTHREADS_ROOT) ]; then \
		$(MAKE) --directory=$(PTHREADS_ROOT) clean; \
	fi

pthreads_purge:
	$(RM) $(PLATFORM_INST_DIR)/include/pthread.h
	$(RM) $(PLATFORM_INST_DIR)/include/semaphore.h
	$(RM) $(PLATFORM_INST_DIR)/include/sched.h
	$(RM) $(PLATFORM_INST_DIR)/lib/libpthreadGC2.a
	$(RM) $(PLATFORM_INST_DIR)/bin/pthreadGC2.dll
	$(RMTREE) $(PTHREADS_ROOT)

pthreads_install:
	$(CP) $(PTHREADS_ROOT)/pthread.h $(PLATFORM_INST_DIR)/include
	$(CP) $(PTHREADS_ROOT)/semaphore.h $(PLATFORM_INST_DIR)/include
	$(CP) $(PTHREADS_ROOT)/sched.h $(PLATFORM_INST_DIR)/include
	$(CP) $(PTHREADS_ROOT)/libpthreadGC2.a $(PLATFORM_INST_DIR)/lib
	$(CP) $(PTHREADS_ROOT)/pthreadGC2.dll $(PLATFORM_INST_DIR)/bin

# 
# syslog client build targets
#
syslog-client:
	$(MAKE) --directory=$(SYSLOG_CLIENT_ROOT) build

syslog-client_clean:
	$(MAKE) --directory=$(SYSLOG_CLIENT_ROOT) clean

syslog-client_purge:
	$(MAKE) --directory=$(SYSLOG_CLIENT_ROOT) purge


#
# liblog4c Build Targets
#
include log4c.mk
ifeq ($(RI_HOST),i686-pc-mingw32)
log4c: make_gendir syslog-client log4c_dir log4c_build log4c_install
else
log4c: make_gendir log4c_dir log4c_build log4c_install
endif

log4c_dir:
	if [ ! -d $(LOG4C_ROOT) ]; then \
		$(call extract_tar_gz,$(LOG4C_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/log4c.patch ]; then \
			$(call apply_patch,$(PLATFORM_TARGET_OS_DIR)/log4c.patch,$(SL_GENDIR)); \
		fi \
	fi

$(LOG4C_ROOT)/Makefile: log4c.mk
	cd $(LOG4C_ROOT) && $(LOG4C_CONFIGURE)

log4c_build: $(LOG4C_ROOT)/Makefile
	$(MAKE) --directory=$(LOG4C_ROOT)

log4c_clean:
	if [ -e $(LOG4C_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(LOG4C_ROOT) clean; \
	fi

log4c_purge:
	$(RMTREE) $(LOG4C_ROOT)

log4c_install: $(LOG4C_ROOT)/Makefile
	if [ -e $(LOG4C_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(LOG4C_ROOT) install; \
	fi

#
# zlib Build Targets
#
include zlib.mk
zlib: make_gendir zlib_dir zlib_build zlib_install

zlib_dir:
	if [ ! -d $(ZLIB_ROOT) ]; then \
		$(call extract_tar_gz,$(ZLIB_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/zlib.patch ]; then \
			$(call apply_patch,$(PLATFORM_TARGET_OS_DIR)/zlib.patch,$(SL_GENDIR)); \
		fi \
	fi

$(ZLIB_ROOT)/Makefile: zlib.mk
	cd $(ZLIB_ROOT) && $(ZLIB_CONFIGURE)

zlib_build: $(ZLIB_ROOT)/Makefile
	$(MAKE) --directory=$(ZLIB_ROOT)

zlib_clean:
	if [ -e $(ZLIB_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ZLIB_ROOT) clean; \
	fi

zlib_purge:
	$(RMTREE) $(ZLIB_ROOT)

zlib_install: $(ZLIB_ROOT)/Makefile
	if [ -e $(ZLIB_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(ZLIB_ROOT) install; \
	fi

#
# FFMPEG Build Targets
#
include ffmpeg.mk
ffmpeg: make_gendir ffmpeg_dir ffmpeg_build ffmpeg_install

ffmpeg_dir:
	if [ ! -d $(FFMPEG_ROOT) ]; then \
		$(call extract_tar_bz2,$(FFMPEG_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/ffmpeg.patch ]; then \
			$(call apply_patch,$(PLATFORM_TARGET_OS_DIR)/ffmpeg.patch,$(SL_GENDIR)); \
		fi \
	fi	

$(FFMPEG_ROOT)/config.h: ffmpeg.mk
	cd $(FFMPEG_ROOT) && $(FFMPEG_CONFIGURE)

ffmpeg_build: $(FFMPEG_ROOT)/config.h
	$(MAKE) --directory=$(FFMPEG_ROOT)

ffmpeg_clean: 
	if [ -e $(FFMPEG_ROOT)/config.h ]; then \
		$(MAKE) --directory=$(FFMPEG_ROOT) clean; \
	fi

ffmpeg_purge: 
	if [ -e $(FFMPEG_ROOT)/config.h ]; then \
		$(MAKE) --directory=$(FFMPEG_ROOT) uninstall; \
	fi
	$(RMTREE) $(FFMPEG_ROOT)

ffmpeg_install:
	if [ -e $(FFMPEG_ROOT)/config.h ]; then \
		$(MAKE) --directory=$(FFMPEG_ROOT) install; \
	fi

#
# netsnmp Build Targets
#
include netsnmp.mk
netsnmp: make_gendir netsnmp_dir netsnmp_build netsnmp_install

netsnmp_dir:
	if [ ! -d $(NETSNMP_ROOT) ]; then \
		$(call extract_tar_gz,$(NETSNMP_ARCHIVE),$(SL_GENDIR)); \
		if [ -e $(PLATFORM_TARGET_OS_DIR)/netsnmp.patch ]; then \
			$(call apply_patch,$(PLATFORM_TARGET_OS_DIR)/netsnmp.patch,$(SL_GENDIR)); \
		fi \
	fi

$(NETSNMP_ROOT)/Makefile: netsnmp.mk
	cd $(NETSNMP_ROOT) && $(NETSNMP_CONFIGURE)

netsnmp_build: $(NETSNMP_ROOT)/Makefile
	$(MAKE) --directory=$(NETSNMP_ROOT)

netsnmp_clean:
	if [ -e $(NETSNMP_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(NETSNMP_ROOT) clean; \
	fi

netsnmp_purge:
	if [ -e $(NETSNMP_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(NETSNMP_ROOT) uninstall; \
	fi
	$(RMTREE) $(NETSNMP_ROOT)

netsnmp_install: $(NETSNMP_ROOT)/Makefile
	if [ -e $(NETSNMP_ROOT)/Makefile ]; then \
		$(MAKE) --directory=$(NETSNMP_ROOT) install; \
	fi


#
# Clean and purge
#
clean: gettext_clean hdhr_clean oil_clean iconv_clean zlib_clean xml2_clean pthreads_clean log4c_clean ffmpeg_clean netsnmp_clean 

purge: gettext_purge hdhr_purge oil_purge iconv_purge zlib_purge xml2_purge pthreads_purge log4c_purge ffmpeg_purge netsnmp_purge 
