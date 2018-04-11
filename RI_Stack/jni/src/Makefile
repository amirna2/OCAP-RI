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

default: build

#
# Include top-level build definitions
#
include $(OCAPROOT)/defs.mk

.PHONY: help build clean purge

#
# Help for this makefile
#
help:
	@echo "OBJS is $(OBJS)"
	@echo "Build the JNI Library"
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
OBJDIR		=	$(OCAP_GEN)/jni

#
# Compile options
#
COPTS		=	-I"$(JDK_ROOT)/include" \
				-I$(OCAP_JNI)/include \
				-I$(OCAP_GEN)/java/javah \
				-I$(OCAP_MPE)/include \
				-I$(OCAP_MPEMGR)/include \
				-I$(OCAP_MPEOS)/include \
				-I$(OCAP_MPEOS)/$(BUILD_OS)/include

#
# Library built by this makefile
#
LIB			=	$(LIBDIR)/libmpejni.a

#
# Source files
#
SOURCES		=   ./jni_util.c \
                ./base/org_cablelabs_debug_Memory.c \
                ./base/org_cablelabs_debug_Profile.c \
                ./base/org_cablelabs_debug_status_MpeStatusProducer.c \
                ./base/org_cablelabs_impl_davic_net_tuning_NetworkInterfaceImpl.c \
                ./base/org_cablelabs_impl_davic_net_tuning_NetworkInterfaceManagerImpl.c \
                ./base/org_cablelabs_impl_debug_Debug.c \
                ./base/org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl.c \
                ./base/org_cablelabs_impl_dvb_dsmcc_DSMCCStreamEventImpl.c \
                ./base/org_cablelabs_impl_dvb_dsmcc_NPTTimebase.c \
                ./base/org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel.c \
                ./base/org_cablelabs_impl_dvb_dsmcc_PrefetchingServiceDomain.c \
                ./base/org_cablelabs_impl_havi_MpegBackgroundImage.c \
                ./base/org_cablelabs_impl_havi_port_mpe_HDCoherentConfig.c \
                ./base/org_cablelabs_impl_havi_port_mpe_HDConfigInfo.c \
                ./base/org_cablelabs_impl_havi_port_mpe_HDScreen.c \
                ./base/org_cablelabs_impl_manager_auth_RootCertSerializer.c \
                ./base/org_cablelabs_impl_manager_cdl_DownloadManagerImpl.c \
                ./base/org_cablelabs_impl_manager_ed_EventDispatchImpl.c \
                ./base/org_cablelabs_impl_manager_filesys_FileManagerImpl.c \
                ./base/org_cablelabs_impl_manager_filesys_OCFileData.c \
                ./base/org_cablelabs_impl_manager_filesys_OCFileSys.c \
                ./base/org_cablelabs_impl_manager_pod_PodManagerImpl.c \
                ./base/org_cablelabs_impl_manager_reclaim_RRMgrImpl.c \
                ./base/org_cablelabs_impl_manager_ResourceReclamationManager_ContextID.c \
                ./base/org_cablelabs_impl_manager_sections_MpeSectionFilterApi.c \
                ./base/org_cablelabs_impl_manager_signalling_SignallingMgr.c \
                ./base/org_cablelabs_impl_manager_service_SIDatabaseImpl.c \
                ./base/org_cablelabs_impl_manager_service_SIEventCodes.c \
                ./base/org_cablelabs_impl_manager_system_mmi_MmiSystemModule.c \
                ./base/org_cablelabs_impl_manager_system_SASSystemModule.c \
                ./base/org_cablelabs_impl_manager_system_SystemModuleMgr.c \
                ./base/org_cablelabs_impl_manager_system_SystemModuleRegistrarImpl.c \
                ./base/org_cablelabs_impl_manager_vbi_NativeVBIFilterApi.c \
                ./base/org_cablelabs_impl_media_mpe_MediaAPIImpl.c \
                ./base/org_cablelabs_impl_ocap_hardware_HostImpl.c \
                ./base/org_cablelabs_impl_ocap_hardware_VideoOutputPortImpl.c \
                ./base/org_cablelabs_impl_ocap_hardware_IEEE1394NodeImpl.c \
                ./base/org_cablelabs_impl_ocap_OcapMain.c \
                ./base/org_cablelabs_impl_ocap_si_ByteParser.c \
                ./base/org_cablelabs_impl_sound_mpe_SoundAPIImpl.c \
                ./base/org_cablelabs_impl_storage_StorageManagerImpl.c \
                ./base/org_cablelabs_impl_storage_StorageProxyImpl.c \
                ./base/org_cablelabs_impl_storage_DetachableStorageOptionImpl.c \
                ./base/org_cablelabs_impl_storage_RemovableStorageOptionImpl.c \
                ./base/org_cablelabs_impl_util_MPEEnv.c \
                ./base/org_cablelabs_impl_ocap_manager_eas_EASAlert.c \
                ./base/org_ocap_hardware_Host.c \
                ./base/org_ocap_media_ClosedCaptioningAttribute.c

ifeq ($(DVR_EXTENSION_ENABLED), 1)
SOURCES +=      ./dvr/org_cablelabs_impl_manager_recording_RecordedServiceImpl.c \
                ./dvr/org_cablelabs_impl_manager_recording_RecordingImpl.c \
                ./dvr/org_cablelabs_impl_manager_recording_RecordingManagerImpl.c \
                ./dvr/org_cablelabs_impl_manager_timeshift_TimeShiftBufferImpl.c \
                ./dvr/org_cablelabs_impl_media_mpe_DVRAPIImpl.c \
                ./dvr/org_cablelabs_impl_storage_MediaStorageOptionImpl.c \
                ./dvr/org_cablelabs_impl_storage_MediaStorageVolumeImpl.c \
                ./dvr/org_cablelabs_impl_storage_DVRStorageManagerImpl.c

endif

ifeq ($(HN_EXTENSION_ENABLED), 1)
SOURCES += \
                ./jni_util_hn.c \
                ./hn/org_cablelabs_impl_media_mpe_HNAPIImpl.c \
                ./hn/org_cablelabs_impl_ocap_hn_NetManagerImpl.c \
                ./hn/org_cablelabs_impl_ocap_hn_transformation_TransformationManagerImpl.c
endif

ifeq ($(FP_EXTENSION_ENABLED), 1)
SOURCES += \
                ./fp/org_cablelabs_impl_ocap_hardware_frontpanel_FrontPanelManagerImpl.c \
                ./fp/org_cablelabs_impl_ocap_hardware_frontpanel_IndicatorImpl.c \
                ./fp/org_cablelabs_impl_ocap_hardware_frontpanel_TextDisplayImpl.c 
endif

ifeq ($(DS_EXTENSION_ENABLED), 1)
SOURCES += \
                ./ds/org_cablelabs_impl_ocap_hardware_device_AudioOutputPortImpl.c \
                ./ds/org_cablelabs_impl_ocap_hardware_device_HostSettingsProxy.c \
                ./ds/org_cablelabs_impl_ocap_hardware_device_VideoOutputSettingsProxy.c \
                ./ds/org_cablelabs_impl_manager_host_DeviceSettingsHostManagerImpl.c 
endif

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

