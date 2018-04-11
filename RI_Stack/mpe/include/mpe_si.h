// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

#ifndef _MPE_SI_H_
#define _MPE_SI_H_

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpe_sys.h"

#include "mgrdef.h"
#include "../mgr/include/simgr.h"

/* This macro will extract SI DB function table from the master table */
#define mpe_si_ftable                               ((mpe_si_ftable_t*)(FTABLE[MPE_MGR_TYPE_SI]))

#define mpe_siInit                                  (*(mpe_si_ftable->mpe_si_init_ptr))
#define mpe_siShutdown                              (*(mpe_si_ftable->mpe_si_shutdown_ptr))

#define mpe_siGetPidByAssociationTag                            (*(mpe_si_ftable->mpe_si_getPidByAssociationTag_ptr))
#define mpe_siGetPidByCarouselID                                (*(mpe_si_ftable->mpe_si_getPidByCarouselID_ptr))
#define mpe_siGetPidByComponentTag                              (*(mpe_si_ftable->mpe_si_getPidByComponentTag_ptr))
#define mpe_siGetProgramNumberByDeferredAssociationTag          (*(mpe_si_ftable->mpe_si_getProgramNumberByDeferredAssociationTag_ptr))

#define mpe_siLockForRead                                       (*(mpe_si_ftable->mpe_si_lockForRead_ptr))
#define mpe_siUnLock                                            (*(mpe_si_ftable->mpe_si_unlock_ptr))

#define mpe_siGetTotalNumberOfTransports                        (*(mpe_si_ftable->mpe_si_getTotalNumberOfTransports_ptr))
#define mpe_siGetAllTransports                                  (*(mpe_si_ftable->mpe_si_getAllTransports_ptr))

#define mpe_siGetTransportDeliverySystemType                    (*(mpe_si_ftable->mpe_si_getTransportDeliverySystemType_ptr))
#define mpe_siGetNumberOfNetworksForTransportHandle             (*(mpe_si_ftable->mpe_si_getNumberOfNetworksForTransportHandle_ptr))
#define mpe_siGetAllNetworksForTransportHandle                  (*(mpe_si_ftable->mpe_si_getAllNetworksForTransportHandle_ptr))
#define mpe_siGetNumberOfTransportStreamsForTransportHandle     (*(mpe_si_ftable->mpe_si_getNumberOfTransportStreamsForTransportHandle_ptr))
#define mpe_siGetAllTransportStreamsForTransportHandle          (*(mpe_si_ftable->mpe_si_getAllTransportStreamsForTransportHandle_ptr))
#define mpe_siGetNetworkHandleByTransportHandleAndNetworkId     (*(mpe_si_ftable->mpe_si_getNetworkHandleByTransportHandleAndNetworkId_ptr))
#define mpe_siGetTransportIdForTransportHandle                  (*(mpe_si_ftable->mpe_si_getTransportIdForTransportHandle_ptr))
#define mpe_siGetTransportHandleByTransportId                   (*(mpe_si_ftable->mpe_si_getTransportHandleByTransportId_ptr))

#define mpe_siGetNetworkServiceInformationType                              (*(mpe_si_ftable->mpe_si_getNetworkServiceInformationType_ptr))
#define mpe_siGetNetworkIdForNetworkHandle                                  (*(mpe_si_ftable->mpe_si_getNetworkIdForNetworkHandle_ptr))
#define mpe_siGetNetworkNameForNetworkHandle                                (*(mpe_si_ftable->mpe_si_getNetworkNameForNetworkHandle_ptr))
#define mpe_siGetNetworkLastUpdateTimeForNetworkHandle                      (*(mpe_si_ftable->mpe_si_getNetworkLastUpdateTimeForNetworkHandle_ptr))
#define mpe_siGetNumberOfTransportStreamsForNetworkHandle                   (*(mpe_si_ftable->mpe_si_getNumberOfTransportStreamsForNetworkHandle_ptr))
#define mpe_siGetAllTransportStreamsForNetworkHandle                        (*(mpe_si_ftable->mpe_si_getAllTransportStreamsForNetworkHandle_ptr))
#define mpe_siGetTransportHandleForNetworkHandle                            (*(mpe_si_ftable->mpe_si_getTransportHandleForNetworkHandle_ptr))
#define mpe_siGetTransportStreamHandleByTransportFrequencyAndTSID           (*(mpe_si_ftable->mpe_si_getTransportStreamHandleByTransportFrequencyAndTSID_ptr))

#define mpe_siGetTransportStreamIdForTransportStreamHandle                  (*(mpe_si_ftable->mpe_si_getTransportStreamIdForTransportStreamHandle_ptr))
#define mpe_siGetDescriptionForTransportStreamHandle                        (*(mpe_si_ftable->mpe_si_getDescriptionForTransportStreamHandle_ptr))
#define mpe_siGetTransportStreamNameForTransportStreamHandle                (*(mpe_si_ftable->mpe_si_getTransportStreamNameForTransportStreamHandle_ptr))
#define mpe_siGetFrequencyForTransportStreamHandle                          (*(mpe_si_ftable->mpe_si_getFrequencyForTransportStreamHandle_ptr))
#define mpe_siGetModulationForTransportStreamHandle                         (*(mpe_si_ftable->mpe_si_getModulationFormatForTransportStreamHandle_ptr))
#define mpe_siGetTransportStreamServiceInformationType                      (*(mpe_si_ftable->mpe_si_getTransportStreamServiceInformationType_ptr))
#define mpe_siGetTransportStreamLastUpdateTimeForTransportStreamHandle      (*(mpe_si_ftable->mpe_si_getTransportStreamLastUpdateTimeForTransportStreamHandle_ptr))
#define mpe_siGetNumberOfServicesForTransportStreamHandle                   (*(mpe_si_ftable->mpe_si_getNumberOfServicesForTransportStreamHandle_ptr))
#define mpe_siGetAllServicesForTransportStreamHandle                        (*(mpe_si_ftable->mpe_si_getAllServicesForTransportStreamHandle_ptr))
#define mpe_siGetNetworkHandleForTransportStreamHandle                      (*(mpe_si_ftable->mpe_si_getNetworkHandleForTransportStreamHandle_ptr))
#define mpe_siGetTransportHandleForTransportStreamHandle                    (*(mpe_si_ftable->mpe_si_getTransportHandleForTransportStreamHandle_ptr))

#define mpe_siGetServiceHandleBySourceId                                (*(mpe_si_ftable->mpe_si_getServiceHandleBySourceId_ptr))
#define mpe_siGetServiceHandleByAppId                                   (*(mpe_si_ftable->mpe_si_getServiceHandleByAppId_ptr))
#define mpe_siGetServiceHandleByFrequencyProgramNumber                  (*(mpe_si_ftable->mpe_si_getServiceHandleByFrequencyProgramNumber_ptr))
#define mpe_siGetServiceHandleByServiceName                             (*(mpe_si_ftable->mpe_si_getServiceHandleByServiceName_ptr))
#define mpe_siCreateDynamicServiceHandle                                (*(mpe_si_ftable->mpe_si_createDynamicServiceHandle_ptr))
#define mpe_siCreateDSGServiceHandle                                    (*(mpe_si_ftable->mpe_si_createDSGServiceHandle_ptr))

#define mpe_siGetTransportStreamHandleForServiceHandle                  (*(mpe_si_ftable->mpe_si_getTransportStreamHandleForServiceHandle_ptr))
#define mpe_siGetPMTPidForServiceHandle                                 (*(mpe_si_ftable->mpe_si_getPMTPidForServiceHandle_ptr))
#define mpe_siGetServiceTypeForServiceHandle                            (*(mpe_si_ftable->mpe_si_getServiceTypeForServiceHandle_ptr))
#define mpe_siGetLongNameForServiceHandle                               (*(mpe_si_ftable->mpe_si_getLongNameForServiceHandle_ptr))
#define mpe_siGetServiceNumberForServiceHandle                          (*(mpe_si_ftable->mpe_si_getServiceNumberForServiceHandle_ptr))
#define mpe_siGetSourceIdForServiceHandle                               (*(mpe_si_ftable->mpe_si_getSourceIdForServiceHandle_ptr))
#define mpe_siGetFrequencyForServiceHandle                              (*(mpe_si_ftable->mpe_si_getFrequencyForServiceHandle_ptr))
#define mpe_siGetProgramNumberForServiceHandle                          (*(mpe_si_ftable->mpe_si_getProgramNumberForServiceHandle_ptr))
#define mpe_siGetModulationFormatForServiceHandle                       (*(mpe_si_ftable->mpe_si_getModulationFormatForServiceHandle_ptr))
#define mpe_siGetServiceDetailsLastUpdateTimeForServiceHandle           (*(mpe_si_ftable->mpe_si_getServiceDetailsLastUpdateTimeForServiceHandle_ptr))
#define mpe_siGetIsFreeFlagForServiceHandle                             (*(mpe_si_ftable->mpe_si_getIsFreeFlagForServiceHandle_ptr))
#define mpe_siGetServiceDescriptionForServiceHandle                     (*(mpe_si_ftable->mpe_si_getServiceDescriptionForServiceHandle_ptr))
#define mpe_siGetServiceDescriptionLastUpdateTimeForServiceHandle       (*(mpe_si_ftable->mpe_si_getServiceDescriptionLastUpdateTimeForServiceHandle_ptr))
#define mpe_siGetNumberOfComponentsForServiceHandle                     (*(mpe_si_ftable->mpe_si_getNumberOfComponentsForServiceHandle_ptr))
#define mpe_siGetAllComponentsForServiceHandle                          (*(mpe_si_ftable->mpe_si_getAllComponentsForServiceHandle_ptr))
#define mpe_siGetPcrPidForServiceHandle                                 (*(mpe_si_ftable->mpe_si_getPcrPidForServiceHandle_ptr))
#define mpe_siGetPATVersionForServiceHandle                             (*(mpe_si_ftable->mpe_si_getPATVersionForServiceHandle_ptr))
#define mpe_siGetPMTVersionForServiceHandle                             (*(mpe_si_ftable->mpe_si_getPMTVersionForServiceHandle_ptr))
#define mpe_siGetPATCRCForServiceHandle                                 (*(mpe_si_ftable->mpe_si_getPATCRCForServiceHandle_ptr))
#define mpe_siGetPMTCRCForServiceHandle                                 (*(mpe_si_ftable->mpe_si_getPMTCRCForServiceHandle_ptr))
#define mpe_siGetDeliverySystemTypeForServiceHandle                     (*(mpe_si_ftable->mpe_si_getDeliverySystemTypeForServiceHandle_ptr))
#define mpe_siGetCASystemIdArrayLengthForServiceHandle                  (*(mpe_si_ftable->mpe_si_getCASystemIdArrayLengthForServiceHandle_ptr))
#define mpe_siGetCASystemIdArrayForServiceHandle                        (*(mpe_si_ftable->mpe_si_getCASystemIdArrayForServiceHandle_ptr))
#define mpe_siGetMultipleInstancesFlagForServiceHandle                  (*(mpe_si_ftable->mpe_si_getMultipleInstancesFlagForServiceHandle_ptr))
#define mpe_siGetNumberOfServiceDetailsForServiceHandle                 (*(mpe_si_ftable->mpe_si_getNumberOfServiceDetailsForServiceHandle_ptr))
#define mpe_siGetServiceDetailsForServiceHandle                         (*(mpe_si_ftable->mpe_si_getServiceDetailsForServiceHandle_ptr))
#define mpe_siGetAppIdForServiceHandle                                  (*(mpe_si_ftable->mpe_si_getAppIdForServiceHandle_ptr))

#define mpe_siGetIsFreeFlagForServiceDetailsHandle                      (*(mpe_si_ftable->mpe_si_getIsFreeFlagForServiceDetailsHandle_ptr))
#define mpe_siGetSourceIdForServiceDetailsHandle                        (*(mpe_si_ftable->mpe_si_getSourceIdForServiceDetailsHandle_ptr))
#define mpe_siGetFrequencyForServiceDetailsHandle                       (*(mpe_si_ftable->mpe_si_getFrequencyForServiceDetailsHandle_ptr))
#define mpe_siGetProgramNumberForServiceDetailsHandle                   (*(mpe_si_ftable->mpe_si_getProgramNumberForServiceDetailsHandle_ptr))
#define mpe_siGetModulationFormatForServiceDetailsHandle                (*(mpe_si_ftable->mpe_si_getModulationFormatForServiceDetailsHandle_ptr))
#define mpe_siGetServiceTypeForServiceDetailsHandle                     (*(mpe_si_ftable->mpe_si_getServiceTypeForServiceDetailsHandle_ptr))
#define mpe_siGetLongNameForServiceDetailsHandle                        (*(mpe_si_ftable->mpe_si_getLongNameForServiceDetailsHandle_ptr))
#define mpe_siGetDeliverySystemTypeForServiceDetailsHandle              (*(mpe_si_ftable->mpe_si_getDeliverySystemTypeForServiceDetailsHandle_ptr))
#define mpe_siGetServiceInformationTypeForServiceDetailsHandle          (*(mpe_si_ftable->mpe_si_getServiceInformationTypeForServiceDetailsHandle_ptr))
#define mpe_siGetServiceDetailsLastUpdateTimeForServiceDetailsHandle    (*(mpe_si_ftable->mpe_si_getServiceDetailsLastUpdateTimeForServiceDetailsHandle_ptr))
#define mpe_siGetCASystemIdArrayLengthForServiceDetailsHandle           (*(mpe_si_ftable->mpe_si_getCASystemIdArrayLengthForServiceDetailsHandle_ptr))
#define mpe_siGetCASystemIdArrayForServiceDetailsHandle                 (*(mpe_si_ftable->mpe_si_getCASystemIdArrayForServiceDetailsHandle_ptr))
#define mpe_siGetTransportStreamHandleForServiceDetailsHandle           (*(mpe_si_ftable->mpe_si_getTransportStreamHandleForServiceDetailsHandle_ptr))
#define mpe_siGetServiceHandleForServiceDetailsHandle                   (*(mpe_si_ftable->mpe_si_getServiceHandleForServiceDetailsHandle_ptr))

#define mpe_siGetServiceComponentHandleByPid                            (*(mpe_si_ftable->mpe_si_getServiceComponentHandleByPid_ptr))
#define mpe_siGetServiceComponentHandleByName                           (*(mpe_si_ftable->mpe_si_getServiceComponentHandleByName_ptr))
#define mpe_siGetServiceComponentHandleByTag                            (*(mpe_si_ftable->mpe_si_getServiceComponentHandleByTag_ptr))
#define mpe_siGetServiceComponentHandleByAssociationTag                 (*(mpe_si_ftable->mpe_si_getServiceComponentHandleByAssociationTag_ptr))
#define mpe_siGetServiceComponentHandleByCarouselId                     (*(mpe_si_ftable->mpe_si_getServiceComponentHandleByCarouselId_ptr))
#define mpe_siGetServiceComponentHandleForDefaultCarousel               (*(mpe_si_ftable->mpe_si_getServiceComponentHandleForDefaultCarousel_ptr))
#define mpe_siReleaseServiceComponentHandle                             (*(mpe_si_ftable->mpe_si_releaseServiceComponentHandle_ptr))

#define mpe_siGetPidForServiceComponentHandle                           (*(mpe_si_ftable->mpe_si_getPidForServiceComponentHandle_ptr))
#define mpe_siGetNameForServiceComponentHandle                          (*(mpe_si_ftable->mpe_si_getNameForServiceComponentHandle_ptr))
#define mpe_siGetComponentTagForServiceComponentHandle                  (*(mpe_si_ftable->mpe_si_getComponentTagForServiceComponentHandle_ptr))
#define mpe_siGetAssociationTagForServiceComponentHandle                (*(mpe_si_ftable->mpe_si_getAssociationTagForServiceComponentHandle_ptr))
#define mpe_siGetCarouselIdForServiceComponentHandle                    (*(mpe_si_ftable->mpe_si_getCarouselIdForServiceComponentHandle_ptr))
#define mpe_siGetLanguageForServiceComponentHandle                      (*(mpe_si_ftable->mpe_si_getLanguageForServiceComponentHandle_ptr))
#define mpe_siGetStreamTypeForServiceComponentHandle                    (*(mpe_si_ftable->mpe_si_getStreamTypeForServiceComponentHandle_ptr))
#define mpe_siGetComponentLastUpdateTimeForServiceComponentHandle       (*(mpe_si_ftable->mpe_si_getComponentLastUpdateTimeForServiceComponentHandle_ptr))
#define mpe_siGetServiceInformationTypeForServiceComponentHandle        (*(mpe_si_ftable->mpe_si_getServiceInformationTypeForServiceComponentHandle_ptr))

#define mpe_siGetTotalNumberOfServices                                  (*(mpe_si_ftable->mpe_si_getTotalNumberOfServices_ptr))
#define mpe_siGetAllServices                                            (*(mpe_si_ftable->mpe_si_getAllServices_ptr))

#define mpe_siRegisterForSIEvents                                       (*(mpe_si_ftable->mpe_si_registerForSIEvents_ptr))
#define mpe_siUnRegisterForSIEvents                                     (*(mpe_si_ftable->mpe_si_unRegisterForSIEvents_ptr))

#define mpe_siGetNumberOfSupportedRatingDimensions                      (*(mpe_si_ftable->mpe_si_getNumberOfSupportedRatingDimensions_ptr))
#define mpe_siGetSupportedRatingDimensions                              (*(mpe_si_ftable->mpe_si_getSupportedRatingDimensions_ptr))
#define mpe_siGetNumberOfLevelsForRatingDimensionHandle                 (*(mpe_si_ftable->mpe_si_getNumberOfLevelsForRatingDimensionHandle_ptr))
#define mpe_siGetNameForRatingDimensionHandle                           (*(mpe_si_ftable->mpe_si_getNameForRatingDimensionHandle_ptr))
#define mpe_siGetDimensionInformationForRatingDimensionHandleAndLevel   (*(mpe_si_ftable->mpe_si_getDimensionInformationForRatingDimensionHandleAndLevel_ptr))
#define mpe_siGetRatingDimensionHandleByName                            (*(mpe_si_ftable->mpe_si_getRatingDimensionHandleByName_ptr))

#define mpe_siRegisterForPSIAcquisition                                  (*(mpe_si_ftable->mpe_si_registerForPSIAcquisition_ptr))
#define mpe_siUnRegisterForPSIAcquisition                                (*(mpe_si_ftable->mpe_si_unRegisterForPSIAcquisition_ptr))

#define mpe_siRegisterForHNPSIAcquisition                                (*(mpe_si_ftable->mpe_si_registerForHNPSIAcquisition_ptr))
#define mpe_siUnRegisterForHNPSIAcquisition                              (*(mpe_si_ftable->mpe_si_unRegisterForHNPSIAcquisition_ptr))

#define mpe_siGetNumberOfServiceEntriesForSourceId                       (*(mpe_si_ftable->mpe_si_getNumberOfServiceEntriesForSourceId_ptr))
#define mpe_siGetAllHandlesForSourceId                                   (*(mpe_si_ftable->mpe_si_getAllServiceHandlesForSourceId_ptr))

#endif /* _MPE_SI_H_ */
