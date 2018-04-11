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


/*
 * The POD manager implementation
 */

#include <sysmgr.h>
#include <podmgr.h>
#include <pod_util.h>
#include <mpe_ed.h>
#include <mgrdef.h>
#include <mpeos_pod.h>
#include <mpeos_mem.h>
#include <mpeos_sync.h>
#include <mpe_dbg.h>

#include <string.h>

/**
 * Static global variables.
 */

/**
 * This is the actual MPE level POD-HOST database used to maintain POD data
 * acquired from the target POD-Host interface.
 */
mpe_PODDatabase podDB;

/**
 * Session ID for the CAS resource
 */
uint32_t casSessionId = 0;

/**
 * Desired resource version for the CAS resource
 */
uint16_t casResourceVersion = 1;

/**
 * Function dispatch table for MPE POD APIs.
 */
mpe_pod_ftable_t podmgr_ftable =
{ mpe_podmgrInit,

mpe_podmgrIsReady, mpe_podmgrIsPresent, 
        mpe_podmgrGetFeatures, mpe_podmgrGetFeatureParam,
        mpe_podmgrSetFeatureParam, mpe_podImplRegister, mpe_podImplUnregister,
        mpeos_podSASConnect, mpeos_podSASClose, mpeos_podMMIConnect,
        mpeos_podMMIClose, mpeos_podAIConnect, mpe_podImplReceiveAPDU, mpeos_podSendAPDU,
        mpeos_podGetParam, mpe_podImplStartDecrypt,
        mpe_podImplStopDecrypt,
        mpeos_podCASConnect, mpeos_podCASClose, mpeos_podReleaseAPDU,
        mpe_podImplGetDecryptStreamStatus };

/**
 * <i>mpe_podSetup<i/>
 *
 * POD manager setup entry point, which installs the POD manager's
 * set of MPE functions into the MPE function table.
 */
void mpe_podSetup(void)
{
    mpe_sys_install_ftable(&podmgr_ftable, MPE_MGR_TYPE_POD);
}

/**
 * <i>mpe_podmgrInit()</i>
 *
 * Initialize interface to POD-HOST stack.  Depending on the underlying OS-specific
 * support this initialization process will likely populate the internal to MPE
 * POD database used to cache information communicated between the Host and the
 * POD.  The mpe_PODDatabase structure is used to maintain the POD data.  The
 * actual mechanics of how the database is maintained is an implementation-specific
 * issues that is dependent upon the functionality of the target POD-Host interface.
 */
void mpe_podmgrInit(void)
{
    mpe_Error ec;

    /* If the POD has not been initialized, do so. */
    static mpe_Bool inited = false;

    if (!inited)
    {
        /* At init time the POD has not been inserted and is not ready */
        podDB.pod_isPresent = FALSE;
        podDB.pod_isReady = FALSE;

        inited = true;
        /* initialize the decryption capabilities to the default (no POD ready) value of 0 */
        podDB.pod_maxElemStreams = 0;
        podDB.pod_maxPrograms = 0;
        podDB.pod_maxTransportStreams = 0;

        /* Initialize variable size parameter fields to NULL. */
        podDB.pod_featureParams.pc_pin = NULL;
        podDB.pod_featureParams.pc_setting = NULL;
        podDB.pod_featureParams.ippv_pin = NULL;
        podDB.pod_featureParams.cable_urls = NULL;
        podDB.pod_featureParams.term_assoc = NULL;
        podDB.pod_featureParams.zip_code = NULL;

        /* Initialize the mpe_PODFeatures member struct pointer */
        podDB.pod_features = NULL;

        /* Allocate the POD DB Mutex */
        ec = mpeos_mutexNew(&podDB.pod_mutex);

        /* If we failed to create the mutex, set the POD Database struct as NOT READY */
        if (ec != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_POD,
                    "<<POD>> mpe_podmgrInit, failed to get mutex, error=%d\n",
                    ec);
        }

        /* mpeos_ call is correct */
        (void) mpeos_podInit(&podDB);

        (void) mpe_podImplInit();
    }
}

/**
 * <i>mpe_podmgrIsReady()</i>
 *
 * Determine availability of POD device.
 *
 * @return MPE_SUCCESS if the POD is available.
 */
mpe_Error mpe_podmgrIsReady(void)
{
    mpe_Bool ready;
    mpe_mutexAcquire(podDB.pod_mutex);
    ready = podDB.pod_isReady;
    mpe_mutexRelease(podDB.pod_mutex);

    /* Return state of POD based on initialization phase. */
    return (ready == TRUE ? MPE_SUCCESS : MPE_ENODATA);

}

/**
 * <i>mpe_podmgrIsPresent()</i>
 *
 * Determine presence of POD device.
 *
 * @return MPE_SUCCESS if the POD is present.
 */
mpe_Error mpe_podmgrIsPresent(void)
{
    /* Return state of POD based on initialization phase. */
    return (podDB.pod_isPresent == TRUE ? MPE_SUCCESS : MPE_ENODATA);
}

/**
 * <i>mpeos_podmgrGetFeatures</i>
 *
 * Get the POD's generic features list.  Note the generic features list
 * can be updated by the HOST-POD interface asynchronously and since there
 * is no mechanism in the native POD support APIs to get notification of this
 * asynchronous update, the list must be refreshed from the interface every
 * the list is requested. The last list acquired will be buffered within the
 * POD database, but it may not be up to date.  Therefore, the MPEOS layer will
 * release any previous list and refresh with a new one whenever a request for
 * the list is made.
 *
 * @param features is a pointer for returning a pointer to the current features
 *                 list.
 *
 * @return MPE_SUCCESS if the features list was successfully acquired.
 */
mpe_Error mpe_podmgrGetFeatures(mpe_PODFeatures **features)
{
    mpe_Error ec;

    /* Make sure caller parameter is valid. */
    if (NULL == features)
        return MPE_EINVAL;

    /* Call MPEOS layer to acquire feature list from POD-Host interface. */
    if ((ec = mpeos_podGetFeatures(&podDB)) != MPE_SUCCESS)
    {
        return ec;
    }

    /* Validate POD features were available. */
    if ((TRUE == podDB.pod_isReady) && (NULL != podDB.pod_features))
    {
        /* Return POD database feature list reference. */
        *features = podDB.pod_features;
        return MPE_SUCCESS;
    }

    return MPE_ENODATA;
}

/**
 * <i>mpe_podmgrGetFeatureParam<i/>
 *
 * Get the specified feature parameter.
 *
 * @param featureId is the Generic Feature identifier of the feature to retrieve.
 * @param param is a pointer for returning the pointer to the parameter value.
 * @param length is a pointer for returning the length of the associated parameter value.
 *
 * @return MPE_SUCCESS if the feature parameter was successfully retrieved.
 */
mpe_Error mpe_podmgrGetFeatureParam(uint32_t featureId, void *param,
        uint32_t *length)
{
    mpe_Error ec; /* Error condition code. */
    uint8_t **real_param = (uint8_t**) param;

    if ((NULL == param) || (NULL == length))
        return MPE_EINVAL;

    /* Make sure feature parameter is cached in the POD database. */
    if ((ec = mpeos_podGetFeatureParam(&podDB, featureId)) != MPE_SUCCESS)
    {
        return ec;
    }

    /* Return requested generic feature parameter byte stream. */
    switch (featureId)
    {
    case MPE_POD_GF_RF_OUTPUT_CHANNEL:
        /* Return address of RF output channel info. */
        *real_param = &(podDB.pod_featureParams.rf_output_channel[0]);
        *length = sizeof(uint8_t) * 2;
        break;

    case MPE_POD_GF_P_C_PIN:
        /* Return a pointer to the pin number list. */
        *real_param = podDB.pod_featureParams.pc_pin;
        *length = sizeof(uint8_t) * (*podDB.pod_featureParams.pc_pin + 1);
        break;

    case MPE_POD_GF_RESET_P_C_PIN:
        /* Return a pointer to the pin reset value. */
        *real_param = &podDB.pod_featureParams.reset_pin_ctrl;
        *length = sizeof(uint8_t);
        break;

    case MPE_POD_GF_P_C_SETTINGS:
        /* Return a pointer to the parental control settings list. */
        *real_param = podDB.pod_featureParams.pc_setting;
        { /* Get channel count. */
            uint16_t channel_count = 0;

            channel_count += *(podDB.pod_featureParams.pc_setting + 1) << 8;
            channel_count += *(podDB.pod_featureParams.pc_setting + 2);

            /* Calculate length: sizeof(channel_count) + (count * 24-bits). */
            *length = sizeof(uint8_t) + sizeof(uint16_t) + (channel_count
                    * (sizeof(uint8_t) * 3));
        }
        break;

    case MPE_POD_GF_IPPV_PIN:
        *real_param = podDB.pod_featureParams.ippv_pin;
        *length = sizeof(uint8_t) * (*podDB.pod_featureParams.ippv_pin + 1);
        break;

    case MPE_POD_GF_TIME_ZONE:
        /* Return address of time zone. */
        *real_param = &(podDB.pod_featureParams.time_zone_offset[0]);
        *length = sizeof(uint8_t) * 2;
        break;

    case MPE_POD_GF_DAYLIGHT_SAVINGS:
        /* Return address of dalylight savings control. */
        *real_param = &(podDB.pod_featureParams.daylight_savings[0]);
        *length = sizeof(uint8_t) * 10;
        break;

    case MPE_POD_GF_AC_OUTLET:
        /* Return address of AC outlet setting. */
        *real_param = &(podDB.pod_featureParams.ac_outlet_ctrl);
        *length = sizeof(uint8_t);
        break;

    case MPE_POD_GF_LANGUAGE:
        /* Return a pointer to the language encoding. */
        *real_param = &(podDB.pod_featureParams.language_ctrl[0]);
        *length = sizeof(uint8_t) * 3;
        break;

    case MPE_POD_GF_RATING_REGION:
        /* Return address of ratings region. */
        *real_param = &(podDB.pod_featureParams.ratings_region);
        *length = sizeof(uint8_t);
        break;

    case MPE_POD_GF_CABLE_URLS:
        /* Return address of cable URLs. */
        *real_param = podDB.pod_featureParams.cable_urls;
        *length = podDB.pod_featureParams.cable_urls_length; /* Get length. */
        break;

    case MPE_POD_GF_EA_LOCATION:
        /* Return address of emergency alert location. */
        *real_param = &(podDB.pod_featureParams.ea_location[0]);
        *length = sizeof(uint8_t) * 3;
        break;

    case MPE_POD_GF_VCT_ID:
        /* Return address of VCT ID. */
        *real_param = &(podDB.pod_featureParams.vct_id[0]);
        *length = sizeof(uint8_t) * 2;
        break;

    case MPE_POD_GF_TURN_ON_CHANNEL:
        /* Return address of turn-on channel. */
        *real_param = &(podDB.pod_featureParams.turn_on_channel[0]);
        *length = sizeof(uint8_t) * 2;
        break;

    case MPE_POD_GF_TERM_ASSOC:
        /* Return address of terminal association ID. */
        *real_param = podDB.pod_featureParams.term_assoc;
        *length = (podDB.pod_featureParams.term_assoc[0] << 8)
                + (podDB.pod_featureParams.term_assoc[1]) + (sizeof(uint8_t)
                * 2);
        break;

    case MPE_POD_GF_DOWNLOAD_GRP_ID:
        /* Return address of common download group ID. */
        *real_param = &(podDB.pod_featureParams.cdl_group_id[0]);
        *length = sizeof(uint8_t) * 2;
        break;

    case MPE_POD_GF_ZIP_CODE:
        /* Return address of zip code. */
        *real_param = podDB.pod_featureParams.zip_code;
        *length = (podDB.pod_featureParams.zip_code[0] << 8)
                + (podDB.pod_featureParams.zip_code[1]) + (sizeof(uint8_t) * 2);
        break;

    default:
        /* Return invalid parameter error. */
        *real_param = NULL;
        *length = 0;
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}

/**
 * <i>mpe_podmgrSetFeatureParam<i/>
 *
 * Set the specified POD-Host feature parameter value.  The feature change
 * request will be passed on to the POD-Host interface and based on the
 * resulting acceptance or denial the internal MPE POD infromation database
 * will be updated (or not) to reflect the new feature parameter value.
 *
 * @param featureId is the identifier of the target feature.
 * @param param is a pointer to the byte array containing the new value.
 *
 * @return MPE_SUCCESS if the prosed value was accepted by the POD.
 */
mpe_Error mpe_podmgrSetFeatureParam(uint32_t featureId, void *param,
        uint32_t length)
{
    mpe_Error ec; /* Error condition code. */
    uint32_t size; /* size used for memory allocations. */
    uint32_t i; /* loop index. */
    uint8_t *real_param = (uint8_t*) param;

    if (NULL == param)
        return MPE_EINVAL;

    switch (featureId)
    {
    case MPE_POD_GF_RF_OUTPUT_CHANNEL:

        if (length != sizeof(uint8_t) * 2)
        {
            return MPE_EINVAL;
        }
        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param,
                sizeof(uint8_t) * 2)) != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Value accepted by POD so, copy RF channel setting to database. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        memcpy(podDB.pod_featureParams.rf_output_channel, real_param,
                sizeof(uint8_t) * 2);
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_P_C_PIN:
        size = real_param[0] + 1; /* Get size of PIN parameter data. */
        if (length != size)
        {
            return MPE_EINVAL;
        }

        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param, size))
                != MPE_SUCCESS)
        {
            return ec;
        }

        /* Need to allocate PIN array. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        if ((NULL == podDB.pod_featureParams.pc_pin)
                || (podDB.pod_featureParams.pc_pin[0] != real_param[0]))
        {
            /* Release memory for previous PIN array. */
            if (NULL != podDB.pod_featureParams.pc_pin)
            {
                /* Free space for previous PIN array. */
                mpeos_memFreeP(MPE_MEM_POD,
                        (void*) podDB.pod_featureParams.pc_pin);
            }
            /* Allocate new PIN array. */
            if ((ec = mpeos_memAllocP(MPE_MEM_POD, size,
                    (void**) &podDB.pod_featureParams.pc_pin)) != MPE_SUCCESS)
            {
                mpeos_mutexRelease(podDB.pod_mutex);
                return ec;
            }
        }
        /* Copy new PIN length and setting. */
        memcpy(podDB.pod_featureParams.pc_pin, real_param, size);
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_P_C_SETTINGS:
    {
        uint16_t channel_count = 0;

        channel_count += *(real_param + 1) << 8;
        channel_count += *(real_param + 2);

        /* Determine size of space for new setting. */
        size = sizeof(uint8_t) + sizeof(uint16_t) + (channel_count
                * (sizeof(uint8_t) * 3));

        if (length != size)
        {
            return MPE_EINVAL;
        }

        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param, size))
                != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Determine need to allocate parental control settings array. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        if ((NULL == podDB.pod_featureParams.pc_setting)
                || (podDB.pod_featureParams.pc_setting[1] != real_param[1])
                || (podDB.pod_featureParams.pc_setting[2] != real_param[2]))/* Different size? */
        {
            /* Release memory for previous channel setting array. */
            if (NULL != podDB.pod_featureParams.pc_setting)
            {
                /* Free space for previous settings. */
                mpeos_memFreeP(MPE_MEM_POD,
                        (void*) podDB.pod_featureParams.pc_setting);
            }
            /* Allocate new array: reset_flag(8-bits), count(16-bits), count * 24-bits */
            if ((ec = mpeos_memAllocP(MPE_MEM_POD, size,
                    (void**) &podDB.pod_featureParams.pc_setting))
                    != MPE_SUCCESS)
            {
                mpeos_mutexRelease(podDB.pod_mutex);
                return ec;
            }
        }
        /* Copy new PC settings. */
        memcpy(podDB.pod_featureParams.pc_setting, real_param, size);
        mpeos_mutexRelease(podDB.pod_mutex);
        break;
    }
    case MPE_POD_GF_IPPV_PIN:
        /* Determine size of space for new IPPV PIN. */
        size = (real_param[0] + 1);
        if (length != size)
        {
            return MPE_EINVAL;
        }

        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param, size))
                != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Need to allocate PIN array. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        if ((NULL == podDB.pod_featureParams.ippv_pin)
                || (podDB.pod_featureParams.ippv_pin[0] != real_param[0]))
        {
            /* Release memory for previous PIN array. */
            if (NULL != podDB.pod_featureParams.ippv_pin)
            {
                /* Free previous IPPV PIN. */
                mpeos_memFreeP(MPE_MEM_POD,
                        (void*) podDB.pod_featureParams.ippv_pin);
            }
            /* Allocate new PIN array. */
            if ((ec = mpeos_memAllocP(MPE_MEM_POD, size,
                    (void**) &podDB.pod_featureParams.ippv_pin)) != MPE_SUCCESS)
            {
                mpeos_mutexRelease(podDB.pod_mutex);
                return ec;
            }
        }
        /* Copy new PIN length and setting. */
        memcpy(podDB.pod_featureParams.ippv_pin, real_param, size);
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_TIME_ZONE:
        if (length != sizeof(uint8_t) * 2)
        {
            return MPE_EINVAL;
        }

        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param,
                sizeof(uint8_t) * 2)) != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Copy time zone to database. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        podDB.pod_featureParams.time_zone_offset[0] = real_param[0];
        podDB.pod_featureParams.time_zone_offset[1] = real_param[1];
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_DAYLIGHT_SAVINGS:
        size = 10;
        if (length != (10 * sizeof(uint8_t)) && length != (1 * sizeof(uint8_t)))
        {
            return MPE_EINVAL;
        }

        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param, length))
                != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Copy new daylight savings setting. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        memcpy(podDB.pod_featureParams.daylight_savings, real_param, size);
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_AC_OUTLET:
        if (length != sizeof(uint8_t))
        {
            return MPE_EINVAL;
        }
        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param,
                sizeof(uint8_t))) != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Copy AC Outlet setting. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        podDB.pod_featureParams.ac_outlet_ctrl = *real_param;
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_LANGUAGE:
        if (length != sizeof(uint8_t) * 3)
        {
            return MPE_EINVAL;
        }
        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param,
                sizeof(uint8_t) * 3)) != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Copy the new language setting. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        for (i = 0; i < 3; ++i)
        {
            podDB.pod_featureParams.language_ctrl[i] = *real_param++;
        }
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    case MPE_POD_GF_RATING_REGION:
        if (length != sizeof(uint8_t))
        {
            return MPE_EINVAL;
        }
        /* Call POD to perform & verify set operation. */
        if ((ec = mpeos_podSetFeatureParam(featureId, real_param,
                sizeof(uint8_t))) != MPE_SUCCESS)
        {
            return ec; /* Set rejected. */
        }

        /* Copy new ratings region. */
        mpeos_mutexAcquire(podDB.pod_mutex);
        podDB.pod_featureParams.ratings_region = *real_param;
        mpeos_mutexRelease(podDB.pod_mutex);
        break;

    default:
        /* Return invalid parameter error. */
        return MPE_EINVAL;
    }

    return MPE_SUCCESS;
}
