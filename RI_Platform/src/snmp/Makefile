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

have_mib2c := $(wildcard /usr/bin/mib2c)
have_snmptranslate := $(wildcard /usr/bin/snmptranslate)

MIBS_ARCHIVE=mibs.tar.gz
HOSTMIB:= $(wildcard OC-STB-HOST-MIB*)
HNMIB:= $(wildcard OC-HOME-NETWORK-MIB*)
MIBFILE:= temp-mib.txt
MIB2C=env MIBS=./$(MIBFILE) mib2c -c mib2c.scalar.conf
MIBTABLE2C=env MIBS=./$(MIBFILE) mib2c -c mib2c.create-dataset.conf
SNMPTRANS=$(shell snmptranslate -Ts -m./$(MIBFILE) | sed 's/.*\.\(.*\)\..*/\1/g' | sort -u)

default: build

#
# Include top-level build definitions
#
include $(PLATFORMROOT)/defs.mk

.PHONY: build clean purge mibtest requiredmibs mibsource mibsourceupdate

#
# Directory locations
#
ORIGDIR		= ./orig
OBJDIR 		= $(PLATFORM_GEN)/snmp
BINDIR 		= $(PLATFORM_INST_DIR)/bin

ifeq ($(RI_HOST),i686-pc-mingw32)
  LIBDIR = $(PLATFORM_INST_DIR)/bin
else
  LIBDIR = $(PLATFORM_INST_DIR)/lib
endif

#
# Compile options
#
COPTS = \
	$(RI_PLATFORM_CPPFLAGS) \
	-I$(PLATFORM_INC) \
	-I$(PLATFORM_SRC) \
	-I$(PLATFORM_SRC)/logging \
	-I$(PLATFORM_SRC)/config \
	-I$(PLATFORM_INST_DIR)/include/glib-2.0 \
	-I$(PLATFORM_INST_DIR)/lib/glib-2.0/include \
	-I$(PLATFORM_INST_DIR)/include \
	$(MK_COPTS) \
	$(CL_COPTS)

LDOPTS = \
	-L$(PLATFORM_INST_DIR)/bin \
	-L$(PLATFORM_INST_DIR)/lib \
	-lglib-2.0 \
	-lgthread-2.0 \
	-lnetsnmpagent \
	-lnetsnmphelpers \
	-lnetsnmpmibs \
	-lnetsnmp \
	$(OS_SOCKET_LIBS) \
	-llog4c \
	-lri_config \
	-llogger

#
# Library built by this makefile
#
LIB = $(LIBDIR)/libplatformAgent$(SO_SUFFIX)

#
# Source files
#
SOURCES	= \
	$(MIB_SOURCES) \

#
# MIB Object section - first: extract the list of all MIBs from the OC-HOST
#                      MIB specification provided (defined in $HOSTMIB)
#
#                      second: the MIBs to ignore (remove an item from this
#                      list if you want to support it)
#
#                      third: the derived list of supported MIBs
#                      (i.e. ALL_MIBS minus IGNORE_MIBS)
#
#                      fianlly: convert the supported MIB list to a list of
#                      'c' files to compile
#
ALL_MIBS = $(SNMPTRANS)
IGNORE_MIBS = \
	cableLabs \
	ccitt \
	clabCommonMibs \
	clabFunction \
	clabProjCableHome \
	clabProjDocsis \
	clabProject \
	clabProjOpenCable \
	clabProjPacketCable \
	clabSecCertObject \
	clabSecurity \
	docsIetfBpi2CmBaseEntry \
	docsIetfBpi2CmBaseTable \
	docsIetfBpi2CmCertObjects \
	docsIetfBpi2CmCryptoSuiteEntry \
	docsIetfBpi2CmCryptoSuiteTable \
	docsIetfBpi2CmDeviceCertEntry \
	docsIetfBpi2CmDeviceCertTable \
	docsIetfBpi2CmIpMulticastMapEntry \
	docsIetfBpi2CmIpMulticastMapTable \
	docsIetfBpi2CmMulticastObjects \
	docsIetfBpi2CmObjects \
	docsIetfBpi2CmTEKEntry \
	docsIetfBpi2CmTEKTable \
	docsIetfBpi2CmtsAuthEntry \
	docsIetfBpi2CmtsAuthTable \
	docsIetfBpi2CmtsBaseEntry \
	docsIetfBpi2CmtsBaseTable \
	docsIetfBpi2CmtsCACertEntry \
	docsIetfBpi2CmtsCACertTable \
	docsIetfBpi2CmtsCertObjects \
	docsIetfBpi2CmtsIpMulticastMapEntry \
	docsIetfBpi2CmtsIpMulticastMapTable \
	docsIetfBpi2CmtsMulticastAuthEntry \
	docsIetfBpi2CmtsMulticastAuthTable \
	docsIetfBpi2CmtsMulticastObjects \
	docsIetfBpi2CmtsObjects \
	docsIetfBpi2CmtsProvisionedCmCertEntry \
	docsIetfBpi2CmtsProvisionedCmCertTable \
	docsIetfBpi2CmtsTEKEntry \
	docsIetfBpi2CmtsTEKTable \
	docsIetfBpi2CodeDownloadControl \
	docsIetfBpi2Compliances \
	docsIetfBpi2Conformance \
	docsIetfBpi2Groups \
	docsIetfBpi2MIB \
	docsIetfBpi2MIBObjects \
	docsIfBaseObjects \
	docsIfCmMacEntry \
	docsIfCmMacTable \
	docsIfCmObjects \
	docsIfCmServiceEntry \
	docsIfCmServiceTable \
	docsIfCmStatusEntry \
	docsIfCmStatusTable \
	docsIfCmtsChannelUtilizationEntry \
	docsIfCmtsChannelUtilizationTable \
	docsIfCmtsCmStatusEntry \
	docsIfCmtsCmStatusTable \
	docsIfCmtsDownChannelCounterEntry \
	docsIfCmtsDownChannelCounterTable \
	docsIfCmtsMacEntry \
	docsIfCmtsMacTable \
	docsIfCmtsMacToCmEntry \
	docsIfCmtsMacToCmTable \
	docsIfCmtsModulationEntry \
	docsIfCmtsModulationTable \
	docsIfCmtsObjects \
	docsIfCmtsServiceEntry \
	docsIfCmtsServiceTable \
	docsIfCmtsStatusEntry \
	docsIfCmtsStatusTable \
	docsIfCmtsUpChannelCounterEntry \
	docsIfCmtsUpChannelCounterTable \
	docsIfCompliances \
	docsIfConformance \
	docsIfDownstreamChannelEntry \
	docsIfDownstreamChannelTable \
	docsIfGroups \
	docsIfMib \
	docsIfMibObjects \
	docsIfQosProfileEntry \
	docsIfQosProfileTable \
	docsIfSignalQualityEntry \
	docsIfSignalQualityTable \
	docsIfUpstreamChannelEntry \
	docsIfUpstreamChannelTable \
	dod \
	enterprises \
	host \
	hrDevice \
	hrDeviceEntry \
	hrDiskStorageEntry \
	hrDiskStorageTable \
	hrFSEntry \
	hrFSTable \
	hrMIBAdminInfo \
	hrMIBCompliances \
	hrMIBGroups \
	hrNetworkEntry \
	hrNetworkTable \
	hrPartitionEntry \
	hrPartitionTable \
	hrPrinterEntry \
	hrPrinterTable \
	hrProcessorEntry \
	hrStorageEntry \
	hrSWInstalled \
	hrSWInstalledEntry \
	hrSWInstalledTable \
	hrSWRun \
	hrSWRunEntry \
	hrSWRunPerf \
	hrSWRunPerfEntry \
	hrSystem \
	ifCompliances \
	ifConformance \
	ifEntry \
	ifGroups \
	ifMIB \
	ifMIBObjects \
	ifRcvAddressEntry \
	ifRcvAddressTable \
	ifStackEntry \
	ifStackTable \
	ifTestEntry \
	ifTestTable \
	ifXEntry \
	ifXTable \
	interfaces \
	internet \
	iso \
	mgmt \
	mib-2 \
	ocHnConformance \
	ocHnDevConnectionEntry \
	ocHnDevConnectionTable \
	ocHnDevInterfaceConfigEntry \
	ocHnDevInterfaceConfigTable \
	ocHnDevInterfaceStatus \
	ocHnDevInterfaceStatusEntry \
	ocHnDevInterfaceStatusTable \
	ocHnDevProperties \
	ocHnDevSupportedChannelEntry \
	ocHnDevSupportedChannelTable \
	ocHnDevUpnpServiceEntry \
	ocHnDevUpnpServiceTable \
	ocHnHomeNetStatus \
	ocHnMIBCompliances \
	ocHnMIBGroups \
	ocHnMibModule \
	ocHnMibObjects \
	ocHnPerStreamRotameterConfigEntry \
	ocHnPerStreamRotameterConfigTable \
	ocHnQos \
	ocHnQosPolicyEntry \
	ocHnQosPolicyTable \
	ocHnQosPriorityTrafficEntry \
	ocHnQosPriorityTrafficTable \
	ocHnQosROStatsEntry \
	ocHnQosROStatsTable \
	ocHnQosStreamStatisticsEntry \
	ocHnQosStreamStatisticsTable \
	ocHnQosTrafficDescEntry \
	ocHnQosTrafficDescTable \
	ocHnQosTrafficInfo \
	ocHnQosTspecEntry \
	ocHnQosTspecTable \
	ocHnROConfiguration \
	ocHnRotameterConfigEntry \
	ocHnRotameterConfigTable \
	ocHnRsdManagerStatus \
	ocHnStaticPQosConfiguration \
	ocHnStaticPQosDeviceEntry \
	ocHnStaticPQosDeviceTable \
	ocStbHostAnalogOutput \
	ocStbHostAnalogVideoEntry \
	ocStbHostAVInterfaceEntry \
	ocStbHostCardCpInfo \
	ocStbHostCCAppInfoEntry \
	ocStbHostCCAppInfoTable \
	ocStbHostCCApplications \
	ocStbHostComponentVideoEntry \
	ocStbHostConformance \
	ocStbHostContentErrorSummaryInfo \
	ocStbHostDevInterfaceTypes \
	ocStbHostDigitalVideoOutput \
	ocStbHostDumpTrapInfo \
	ocStbHostDVIHDMIAvailableVideoFormatEntry \
	ocStbHostDVIHDMIEntry \
	ocStbHostEasCodes \
	ocStbHostIEEE1394ConnectedDevicesEntry \
	ocStbHostIEEE1394Entry \
	ocStbHostIEEE1394Objects \
	ocStbHostInBandTunerEntry \
	ocStbHostInterfaces \
	ocStbHostJVMInfo \
	ocStbHostMIBCompliances \
	ocStbHostMIBGroups \
	ocStbHostMibModule \
	ocStbHostMibObjects \
	ocStbHostMpeg2ContentEntry \
    ocStbHostMpeg4ContentEntry \
	ocStbHostNotifications \
	ocStbHostProgramStatusEntry \
	ocStbHostRFChannelOutEntry \
	ocStbHostServiceProgramInfo \
	ocStbHostSnmpProxyInfo \
	ocStbHostSoftware \
	ocStbHostSoftwareApplicationInfo \
	ocStbHostSoftwareApplicationInfoEntry \
	ocStbHostSoftwareApplicationInfoTable \
	ocStbHostSPDIfEntry \
	ocStbHostSpecificationsInfo \
	ocStbHostStatus \
	ocStbHostSystem \
    ocStbHostSystemDriveInfoEntry \
	ocStbHostSystemHomeNetworkEntry \
	ocStbHostSystemMemoryReportEntry \
	ocStbHostSystemObjects \
	ocStbHostSystemTempEntry \
    ocStbHostVc1ContentEntry \
	org \
	pktcEnhancements \
	pktcPACMMibs \
	pktcSupportMibs \
	private \
	snmp \
	snmpEngine \
	snmpFrameworkAdmin \
	snmpFrameworkMIB \
	snmpFrameworkMIBCompliances \
	snmpFrameworkMIBConformance \
	snmpFrameworkMIBGroups \
	snmpFrameworkMIBObjects \
	snmpMIB \
	snmpMIBCompliances \
	snmpMIBConformance \
	snmpMIBGroups \
	snmpMIBObjects \
	snmpModules \
	snmpSet \
	snmpTrap \
	snmpTraps \
	snmpV2 \
	sysOREntry \
	transmission \

ifneq ($(strip $(have_snmptranslate)),)
SUPPORTED_MIBS = $(filter-out $(IGNORE_MIBS),$(ALL_MIBS))
MIB_OBJS = mib $(SUPPORTED_MIBS)
else
MIB_OBJS = \
	mib \
	hrDeviceTable \
	hrProcessorTable \
	hrStorage \
	hrStorageTable \
	hrSWRunPerfTable \
	hrSWRunTable \
	ifTable \
	ocHnNetConfig \
	ocStbCardInfo \
	ocStbHostAnalogVideoTable \
	ocStbHostAVInterfaceTable \
	ocStbHostCCMMI \
	ocStbHostComponentVideoTable \
	ocStbHostDeviceSoftwareBase \
	ocStbHostDVIHDMIAvailableVideoFormatTable \
	ocStbHostDVIHDMITable \
	ocStbHostEasObjects \
	ocStbHostFirmwareDownloadStatus \
	ocStbHostHWIdentifiers \
	ocStbHostIEEE1394ConnectedDevicesTable \
	ocStbHostIEEE1394Table \
	ocStbHostInBandTunerTable \
	ocStbHostInfo \
	ocStbHostMemoryInfo \
	ocStbHostMpeg2ContentTable \
	ocStbHostMpeg4ContentTable \
	ocStbHostPower \
	ocStbHostProgramStatusTable \
	ocStbHostQpskObjects \
	ocStbHostRebootInfo \
	ocStbHostRFChannelOutTable \
	ocStbHostSecuritySubSystem \
	ocStbHostSPDIfTable \
	ocStbHostSystemDriveInfoTable \
	ocStbHostSystemHomeNetworkTable \
	ocStbHostSystemLogging \
	ocStbHostSystemLoggingEventEntry \
	ocStbHostSystemLoggingEventTable \
	ocStbHostSystemMemoryReportTable \
	ocStbHostSystemTempTable \
	ocStbHostUserSettings \
	ocStbHostVc1ContentTable \
	sysORTable \
	system
endif
MIB_SOURCES = $(addsuffix .c,$(addprefix $(OBJDIR)/,agent $(MIB_OBJS)))


#
# EXE section
# 
EXE = $(BINDIR)/platformAgent$(EXE_SUFFIX)

EXE_SOURCES = main.c \
	$(SOURCES)

# MinGW GCC does not support RPATH
ifeq ($(RI_HOST),i686-pc-linux-gnu)
EXE_LIBS = \
	-Wl,-rpath -Wl,$(LAUNCHER_RPATH) -ldl \
	-L$(PLATFORM_INST_DIR)/lib \
	$(LDOPTS)
else
EXE_LIBS = \
	-L$(PLATFORM_INST_DIR)/bin \
	$(LDOPTS)
endif

#
# Intermediate files
#
OBJS = $(patsubst %.c,%.o,$(SOURCES))
EXE_OBJS = $(patsubst %.c,%.o,$(EXE_SOURCES))

#
# Include dependency files
#
DEPENDS = $(patsubst %.c,%.d,$(SOURCES)) \

ifeq ($(strip $(filter clean purge,$(MAKECMDGOALS))),)
-include $(DEPENDS)
endif

#
# Build everything
#
build: mibsource
	make $(LIB)

#
# Build platformAgent
#
exe: $(EXE)

standalone: mibsource
	make exe MK_COPTS+=-DSTANDALONE

#
# Executable built by this makefile
#
$(EXE): $(EXE_OBJS)
	$(call BUILD_EXECUTABLE,$(EXE_OBJS) $(EXE_LIBS))

#
# Build the library from intermediate files
#
$(LIB): $(OBJS)
	$(call BUILD_SHARED_LIBRARY,$(OBJS) $(LDOPTS))

#
# Compile source files into intermediate files
#
%.o: %.c $(call makefile-list)
	$(call COMPILE,$(COPTS))

#
# Bring header file dependencies up to date
#
%.d: %.c
	$(call BUILD_DEPENDS,$(COPTS))

ifneq ($(strip $(have_mib2c)),)
mibtest: requiredmibs
	@$(ECHO) "****************************************************"
	@$(ECHO) "Supported MIBs (from $(HOSTMIB) and $(HNMIB)):"
	@$(ECHO) "****************************************************"
	@for mib in $(MIB_OBJS); do \
		$(ECHO) $$mib; \
	done;

mibsourceupdate: requiredmibs
	@$(ECHO) "*************************************************************"
	@$(ECHO) "Updating supported MIBs (from $(HOSTMIB) and $(HNMIB)):"
	@$(ECHO) "*************************************************************"
	@for obj in $(SUPPORTED_MIBS); do \
		$(ECHO) $$obj | grep -v Table || $(MIBTABLE2C) $$obj; \
		$(ECHO) $$obj | grep Table || $(MIB2C) $$obj; \
		mv -f $$obj.* $(ORIGDIR) ; \
	done;
else
mibtest: requiredmibs
	@$(ECHO) "**************************************************"
	@$(ECHO) "Supported MIBs (from hard-coded list in Makefile!):"
	@$(ECHO) "**************************************************"
	@for mib in $(MIB_OBJS); do \
		$(ECHO) $$mib; \
	done;

mibsourceupdate: 
	@$(ECHO) "******************************************"
	@$(ECHO) "Could not find mib2c - can't compile mibs!"
	@$(ECHO) "******************************************"
endif

requiredmibs:
	@cat $(HOSTMIB) > $(MIBFILE)
	@cat $(HNMIB) >> $(MIBFILE)
	@if [ ! -e $(PLATFORM_INST_DIR)/share/snmp ]; then \
		mkdir -p $(PLATFORM_INST_DIR)/share/snmp; \
		$(call extract_tar_gz,$(MIBS_ARCHIVE),$(PLATFORM_INST_DIR)/share/snmp);\
	fi

mibsource: requiredmibs
	@mkdir -p $(OBJDIR)
	@if [ ! -e $(OBJDIR)/agent.c ]; then \
		cp agent.[c,h] $(OBJDIR) ; \
	fi
	@for obj in $(MIB_OBJS); do \
		if [ ! -e $(OBJDIR)/$$obj.c ]; then \
			cp $(ORIGDIR)/$$obj.[c,h] $(OBJDIR) ; \
			if [ -e $(ORIGDIR)/$$obj.patch ]; then \
				patch -p0 $(OBJDIR)/$$obj.c < $(ORIGDIR)/$$obj.patch ; \
			fi \
		fi \
	done;

#
# Clean and purge
#
clean:
	$(RMTREE) $(OBJDIR)

purge:
	$(call RM_LIBRARY,$(LIB))


