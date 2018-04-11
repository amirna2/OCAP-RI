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

#include "test_si.h"
#include "mpe_si.h"
#include "mpe_types.h"

/**
 * Tests the following APIs:
 *     <ul>
 *     <li> void GetSeviceIdByComponent()
 *     </ul>
 *
 * @param tc pointer to test case structure
 * @requirement Shuts down the si engine successfully. 
 * @requirement will return true on success
 * @requirement will return false on failure
 *
 * @assert 1. Calls to api should work for every mpeos_SiComponent.
 * @assert 2. Test same calls from assert 1 on all 3 available channels.
 *     <ul>
 *     <li> 
 *     </ul>
 * @purpose Verify basic functionality of GetServiceIdByComponent() 
 */
/*
 void test_si_GetServiceIdByComponent (CuTest *tc)
 {
 mpe_Error rc = 0;
 uint32_t outServiceId = 0; // See mpeos test for more info.
 mpeos_SiElement* pCh;
 uint32_t serviceId;
 int count;

 mpeos_SiElement* apCh[3];

 TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_si_GetServiceIdByComponent Enter\n");

 apCh[0] = SITest_GetChannelZero();
 apCh[1] = SITest_GetChannelOne();
 apCh[2] = SITest_GetChannelTwo();

 // Test that all three values will return something.
 for( count=0; count<3; ++count )
 {
 pCh = apCh[count];
 serviceId = pCh->serviceSourceId; // golden value

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_SOURCE_ID,
 (void*)&(pCh->serviceSourceId),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_PROGRAM_NUMBER,
 (void*)&(pCh->serviceProgramNumber),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_MAJOR_CHANNEL_NUMBER,
 (void*)&(pCh->serviceMajorChannelNumber),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_MINOR_CHANNEL_NUMBER,
 (void*)&(pCh->serviceMinorChannelNumber),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_TS_ID,
 (void*)&(pCh->serviceTsId),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_FREQUENCY,
 (void*)&(pCh->serviceFrequency),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_MODULATION_MODE,
 (void*)&(pCh->serviceModulationMode),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_TYPE,
 (void*)&(pCh->serviceType),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_PMT_PID,
 (void*)&(pCh->servicePmtPid),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 outServiceId = 0;
 rc = GetServiceIdByComponent(MPEOS_SI_SERVICE_PCR_PID,
 (void*)&(pCh->servicePcrPid),
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );
 } // end for

 TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "test_si_GetServiceIdByComponent Exit\n");
 } // end test_si_GetServiceIdByComponent(CuTest*)
 */

/**
 * Tests the following APIs:
 *     <ul>
 *     <li> void mpeos_GetAllServiceIdByComponent
 *     </ul>
 *
 * @param tc pointer to test case structure
 * @requirement Shuts down the si engine successfully. 
 * @requirement will return true on success
 * @requirement will return false on failure
 *
 * @assert 1. Calls to api should work for every mpeos_SiComponent.
 * @assert 2. Test same calls from assert 1 on all 3 available channels.
 *     <ul>
 *     <li> 
 *     </ul>
 * @purpose Verify basic functionality of GetAllServiceIdByComponent() 
 */
/*
 static void test_si_GetAllServiceIdByComponent(CuTest *tc)
 {
 mpe_Error rc = 0;
 uint32_t index = 0;
 uint32_t outServiceId = 0;
 mpeos_SiElement* pCh;
 uint32_t serviceId;
 int count;

 mpeos_SiElement* apCh[3];
 apCh[0] = SITest_GetChannelZero();
 apCh[1] = SITest_GetChannelOne();
 apCh[2] = SITest_GetChannelTwo();

 TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "test_si_GetAllServiceIdByComponent Enter\n");

 // Test that all three values will return something.
 for( count=0; count<3; ++count )
 {
 pCh = apCh[count];
 serviceId = pCh->serviceSourceId; // golden value

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_SOURCE_ID,
 (void*)&(pCh->serviceSourceId),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_PROGRAM_NUMBER,
 (void*)&(pCh->serviceProgramNumber),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_MAJOR_CHANNEL_NUMBER,
 (void*)&(pCh->serviceMajorChannelNumber),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_MINOR_CHANNEL_NUMBER,
 (void*)&(pCh->serviceMinorChannelNumber),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_TS_ID,
 (void*)&(pCh->serviceTsId),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_FREQUENCY,
 (void*)&(pCh->serviceFrequency),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_MODULATION_MODE,
 (void*)&(pCh->serviceModulationMode),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_TYPE,
 (void*)&(pCh->serviceType),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_PMT_PID,
 (void*)&(pCh->servicePmtPid),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );

 index = 0;
 outServiceId = 0;
 rc = GetAllServiceIdByComponent(MPEOS_SI_SERVICE_PCR_PID,
 (void*)&(pCh->servicePcrPid),
 &index,
 &outServiceId );
 CuAssertIntEquals_Msg(tc,"GetAllServiceIdByComponent",MPE_SUCCESS,rc);
 CuAssertIntEquals( tc, serviceId, outServiceId );
 } // end for
 } // end test_si_GetAllServiceIdByComponent(CuTest*)
 */

/**
 * Will test the GetComponent API call for functional accuracy.
 *
 * @param tc The CuTest* required by all CuTest test cases.
 */
/*
 static void test_si_GetComponent(CuTest *tc)
 {
 mpeos_SiElement* apCh[3];

 mpe_Bool boolComp;
 uint32_t uintComp;
 void* voidpComp;
 uint32_t index;
 uint32_t serviceId = 0;
 uint32_t progNum = 0;
 int count;
 mpeos_SiServiceComponent* serComp;

 apCh[0] = SITest_GetChannelZero();
 apCh[1] = SITest_GetChannelOne();
 apCh[2] = SITest_GetChannelTwo();


 for( count=0; count<3; ++count )
 {
 boolComp = FALSE;
 uintComp = 0;
 voidpComp = NULL;

 progNum = apCh[count]->serviceProgramNumber;
 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetServiceIdByComponent(MPEOS_SI_SERVICE_PROGRAM_NUMBER,
 (void*)&progNum,
 &serviceId ) );
 CuAssertIntEquals(tc, apCh[count]->serviceSourceId, serviceId );


 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_PROGRAM_NUMBER,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceProgramNumber, uintComp );
 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_SOURCE_ID,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceSourceId, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_MAJOR_CHANNEL_NUMBER,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceMajorChannelNumber, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_MINOR_CHANNEL_NUMBER,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceMinorChannelNumber, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_TS_ID,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceTsId, uintComp );


 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_FREQUENCY,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceFrequency, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_MODULATION_MODE,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceModulationMode, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_TYPE,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceType, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_PMT_PID,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->servicePmtPid, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_PCR_PID,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->servicePcrPid, uintComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_ACCESS_CONTROLLED,
 &boolComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceAccessControlled, boolComp );
 
 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_HIDDEN,
 &boolComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceHidden, boolComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_HIDE_GUIDE,
 &boolComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceHideGuide, boolComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_SHORT_NAME,
 &voidpComp ) );
 CuAssertStrEquals(tc, apCh[count]->serviceShortName, (char*)voidpComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_RATING,
 &voidpComp ) );
 CuAssertStrEquals(tc, apCh[count]->serviceRating, (char*)voidpComp );

 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_LONG_NAME,
 &voidpComp ) );
 CuAssertStrEquals(tc, apCh[count]->serviceLongName, (char*)voidpComp );



 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_NUM_COMPONENTS,
 &uintComp ) );
 CuAssertIntEquals(tc, apCh[count]->serviceNumComponents, uintComp );

 // Mine the service components for data.
 serComp = (mpeos_SiServiceComponent*) voidpComp;
 for( index=0; index<apCh[count]->serviceNumComponents; ++index )
 {
 CuAssertIntEquals( tc,
 MPE_SUCCESS,
 GetComponent( serviceId,
 MPEOS_SI_SERVICE_COMPONENTS,
 &voidpComp ) );
 CuAssertIntEquals(tc,
 (apCh[count]->serviceComponents[index]).streamType,
 serComp->streamType );
 CuAssertIntEquals(tc,
 (apCh[count]->serviceComponents[index]).pid,
 serComp->pid );

 } // end for
 } // end for
 } // end test_si_GetComponent(CuTest*)
 */
/**
 * Will test the GetComponentRangeByValue API call for functional
 * accuracy.
 *
 * @param tc The CuTest* required by all CuTest test cases.
 */
/*
 static void test_si_GetComponentRangeByValue( CuTest* tc )
 {
 CuAssertIntEquals_Msg( tc,
 "GetComponentRangeByValue not supported",
 0,
 1 );
 } // end test_si_GetComponentRangeByValue(CuTest*)
 */
/**
 * Will test the GetComponentRangeByCount API call for functional
 * accuracy.
 *
 * @param tc The CuTest* required by all CuTest test cases.
 */
/*
 static void test_si_GetComponentRangeByCount( CuTest* tc )
 {
 CuAssertIntEquals_Msg( tc,
 "GetComponentRangeByCount not supported",
 0,
 1 );
 } // end test_si_GetComponentRangeByCount(CuTest*)
 */

/* NEW SI DB APIs */
/**
 ** Test SI database APIs
 **
 ** @param a cutest pointer
 **
 **/
#define OC_ASSOCIATION_TAG 0x19
#define OC_COUROUSEL_ID    0x15

void test_simgr_sidb_extract_all(CuTest *tc)
{
#if 0
    uint32_t i;
    char debugMessage[1024];
    mpe_SiHandle si_handle;
    mpe_SiUniqueifier u_id;
    mpe_SiServiceComponentHandle comp_handle;

    uint32_t tsId = -1;
    //mpe_SiElementaryStreamList elementary_stream_list;
    mpe_SiServiceType service_type;
    char *service_name = NULL;
    uint32_t service_number= -1;
    uint32_t minor_number=-1;
    uint32_t sourceId;
    uint32_t freq;
    uint32_t prog_num;
    uint32_t new_carousel_id=0;
    uint32_t num_sourceids=0;
    uint32_t comp_pid=0;
    char language[4];
    uint32_t num_pids=0;
    char comp_name[20];
    uint8_t comp_tag;
    mpe_SiElemStreamType stream_type;

    uint16_t assoc_tag= OC_ASSOCIATION_TAG;
    uint32_t carousel_id = OC_COUROUSEL_ID;
    mpe_FilterSource oc_source;
    uint32_t carouselId=0;

    for(i=0;i<2;i++)
    {
        if(getSiHandleBySourceId(0x7D3,&si_handle)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getSiHandleBySourceId() didn't find SourceId = %d\n",i);
            continue;
        }

        if(getTransportStreamId(si_handle,&tsId)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - mpe_getTransportStreamId() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getServiceType(si_handle,&service_type)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getServiceType() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getServiceName(si_handle,&service_name)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getServiceName() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getServiceNumber(si_handle,&service_number,&minor_number)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getServiceNumber() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getSourceId(si_handle,&sourceId)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getSourceId() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        /*if(sourceId != i)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - getSourceId() failed for SourceId = %d\n",i);
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - problem either in mpe_getSourceId() or in mpe_getSiHandleBySourceId\n",i);
         goto ERROR_RETURN;

         }*/

        if(getFreqProgramNumber(si_handle,&freq,&prog_num)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getFreqProgramNumber() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getPidByAssociationTag(si_handle,assoc_tag,&oc_source.pid) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getPidByAssociationTag() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getPidByCarouselID (si_handle,carousel_id,&oc_source.pid) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getPidByCarouselID () failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        /*
         if(getDefaultObjectCarousel (si_handle,&new_carousel_id,&oc_source) != MPE_SUCCESS)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - getDefaultObjectCarousel () failed for SourceId = %d\n",i);
         goto ERROR_RETURN;
         }
         */

        if(getSiHandleByFreqProgramNumber(freq,prog_num,&si_handle) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getSiHandleByFreqProgramNumber() failed for SourceId = %d\n",i);
            goto ERROR_RETURN;
        }

        if(getSourceId(si_handle,&sourceId) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getSourceId() failed for frequency = %d and program_number = %d\n",freq,prog_num);
            goto ERROR_RETURN;
        }

        /*if(sourceId != i)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - getSourceId() failed for SourceId = %d\n",i);
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - problem either in mpe_getSourceId() or in getSiHandleByFreqProgramNumber(most probable one\n",i);
         goto ERROR_RETURN;

         }*/

        if(getNumberOfSourceIds(&num_sourceids)!=MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getNumberOfSourceIds() failed\n");
            goto ERROR_RETURN;
        }

        if(getPidByComponentName(si_handle, "MPE_SI_DESC_COMPONENT_NAME", &comp_pid) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getPidByComponentName() failed\n");
            goto ERROR_RETURN;
        }

        /*		if(getPidByComponentTag(si_handle, COMPONENT_TAG, &comp_pid) != MPE_SUCCESS)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - getPidByComponentTag() failed\n");
         goto ERROR_RETURN;
         }
         */
        if(getPidByStreamType(si_handle, MPE_SI_ELEM_MPEG_2_VIDEO, &comp_pid)!= MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getPidByStreamType() failed\n");
            goto ERROR_RETURN;
        }

        if(getPidByLanguage(si_handle, "eng", &comp_pid) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getPidByLanguage() failed\n");
            goto ERROR_RETURN;
        }

        /*		if(getLanguageByPid(si_handle, comp_pid, &language) != MPE_SUCCESS)
         {
         TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
         "test_simgr_sidb_extract_all- Error: - getLanguageByPid() failed\n");
         goto ERROR_RETURN;
         }
         */
        if(getNumberOfPids(si_handle, &num_pids) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getNumberOfPids() failed\n");
            goto ERROR_RETURN;
        }

        if(createUniqueifierBySiHandle(si_handle, &u_id) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - createUniqueifierBySiHandle() failed\n");
            goto ERROR_RETURN;
        }

        if(getSiHandleByUniqueifier(u_id, &si_handle) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getSiHandleByUniqueifier() failed\n");
            goto ERROR_RETURN;
        }

        if(deleteUniqueifier(u_id) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - deleteUniqueifier() failed\n");
            goto ERROR_RETURN;
        }

        if(getComponentHandleByPid(si_handle, comp_pid, &comp_handle) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getComponentHandleByPid() failed\n");
            goto ERROR_RETURN;
        }

        if(getComponentPid(si_handle, comp_handle, &comp_pid) != MPE_SUCCESS)
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "test_simgr_sidb_extract_all- Error: - getComponentPid() failed\n");
            goto ERROR_RETURN;
        }

        if(getComponentName(si_handle, comp_handle, &comp_name) != MPE_SUCCESS)
        {
            sprintf(debugMessage,"test_simgr_sidb_extract_all- Error: - getComponentName() failed\n");
            TRACE( debugMessage);
            goto ERROR_RETURN;
        }

        if(getComponentTag(si_handle, comp_handle, &comp_tag) != MPE_SUCCESS)
        {
            sprintf(debugMessage,"test_simgr_sidb_extract_all- Error: - getComponentTag() failed\n");
            TRACE( debugMessage);
            goto ERROR_RETURN;
        }

        if(getCarouselId(si_handle, comp_handle, &carouselId) != MPE_SUCCESS)
        {
            sprintf(debugMessage,"test_simgr_sidb_extract_all- Error: - getCarouselId() failed\n");
            TRACE( debugMessage);
            goto ERROR_RETURN;
        }

        if(getComponentLanguage(si_handle, comp_handle, &language) != MPE_SUCCESS)
        {
            sprintf(debugMessage,"test_simgr_sidb_extract_all- Error: - getComponentLanguage() failed\n");
            TRACE( debugMessage);
            goto ERROR_RETURN;
        }

        if(getStreamType(si_handle, comp_handle, &stream_type) != MPE_SUCCESS)
        {
            sprintf(debugMessage,"test_simgr_sidb_extract_all- Error: - getStreamType() failed\n");
            TRACE( debugMessage);
            goto ERROR_RETURN;
        }

        //print the available data
        TRACE("---------------------------------------------------------------------\n");
        sprintf(debugMessage,"SourceId = %d\n",i);
        TRACE( debugMessage);

        sprintf(debugMessage,"Transport StreamID = %d\n",tsId);
        TRACE( debugMessage);

        sprintf(debugMessage,"frequency = %d\n",freq);
        TRACE( debugMessage);

        sprintf(debugMessage,"program number  = %d\n",prog_num);
        TRACE( debugMessage);

        sprintf(debugMessage,"service number  = %d\n",service_number);
        TRACE( debugMessage);
        sprintf(debugMessage,"minor number  = %d\n",minor_number);
        TRACE( debugMessage);

        if(service_name)
        {
            sprintf(debugMessage,"service_name  = %s\n",service_name);
            TRACE( debugMessage);
        }
        sprintf(debugMessage,"Number of sourceids  = %d\n",num_sourceids);
        TRACE( debugMessage);

        sprintf(debugMessage,"Number of pids  = %d\n",num_pids);
        TRACE( debugMessage);

        sprintf(debugMessage,"Component pid  = %d\n",comp_pid);
        TRACE( debugMessage);

        sprintf(debugMessage,"Comp_handle = 0x%x\n", comp_handle);
        TRACE( debugMessage);

        sprintf(debugMessage,"comp_pid = %d\n", comp_pid);
        TRACE( debugMessage);

        sprintf(debugMessage,"comp_name = %s\n", comp_name);
        TRACE( debugMessage);

        sprintf(debugMessage,"comp_tag = 0x%x\n", comp_tag);
        TRACE( debugMessage);

        sprintf(debugMessage,"carousel_id = 0x%x\n", carouselId);
        TRACE( debugMessage);

        sprintf(debugMessage,"language = %s\n", language);
        TRACE( debugMessage);

        sprintf(debugMessage,"stream_type = %d\n", stream_type);
        TRACE( debugMessage);

        sprintf(debugMessage,"uniqueifier = 0x%x\n", u_id);
        TRACE( debugMessage);

        sprintf(debugMessage,"si_handle = 0x%x\n", si_handle);
        TRACE( debugMessage);

        TRACE("---------------------------------------------------------------------\n");

        ERROR_RETURN:
        releaseSiHandle(si_handle);
    }
#endif
}//end test_simgr_sidb_extract_all(CuTest *tc)

/**
 * Will setup the CuSuite test suite that is used to test the SI APIs.
 *
 * @return CuSuite pointer that is used to test the SI components.
 */
extern CuSuite* getTestSuite_siAPI(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_simgr_sidb_extract_all);

    return suite;
} /* end getTestSuite_siAPI(void) */
