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
/** \file
 *
 * \brief Motorola port-specific code
 *
 * This file contains function that are Motorola port specific.
 * 
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

// Use this for now since there doesn't appear to be a Motorola define
#if !defined(POWERTV) && defined(TEST_MPEOS)

#include <test_media.h>
#include <test_utils.h>
#include <mpe_filterevents.h>
#include <mpe_callbackValues.h>

/****************************************************************************
 *
 *  GoToOOBChannel()
 *
 ***************************************************************************/
/**
 * \brief move the tuner to the out-of-band channel
 *
 * This function moves the tuner to the out-of-band channel, if necessary.
 * Further, it returns an mpe_FilterSource object that expresses the
 * out-of-band filter source.
 *
 * \param pFilterSource pointer to location to fill with a pointer to an
 * allocated mpe_FilterSource object for the out-of-band source
 *
 * \return any error encountered getting the out-of-band source
 *
 */
mpe_Error GoToOOBChannel(mpe_FilterSource **pFilterSource)
{
    mpe_Error err;

    err = memAllocP(MPE_MEM_TEST, sizeof(**pFilterSource), (void **)pFilterSource);
    if (err != MPE_SUCCESS)
    return err;

    (*pFilterSource)->sourceType = MPE_FILTER_SOURCE_OOB;
    (*pFilterSource)->pid = 0; // caller must fill
    (*pFilterSource)->parm.p_OOB.tsid = 0; // caller must change if desired

    return MPE_SUCCESS;
}

/****************************************************************************
 *
 *  GoToInbandChannel()
 *
 ***************************************************************************/
/**
 * \brief move the tuner to an in-band channel
 *
 * This function moves a tuner to an inband channel. Further, it returns an
 * mpe_FilterSource object that expresses the in-band filter source.
 *
 * \param pFilterSource pointer to location to fill with a pointer to an
 * allocated mpe_FilterSource object for the in-band source
 *
 * \return any error encountered getting the in-band source
 *
 */
mpe_Error GoToInbandChannel(mpe_FilterSource **pFilterSource)
{
    mpe_Error err;

    err = memAllocP(MPE_MEM_TEST, sizeof(**pFilterSource), (void **)pFilterSource);
    if (err != MPE_SUCCESS)
    return err;

    (*pFilterSource)->sourceType = MPE_FILTER_SOURCE_INB;
    (*pFilterSource)->pid = 0; // caller must fill
    (*pFilterSource)->parm.p_INB.tunerID = 1; // FIXME: use params header for this
    (*pFilterSource)->parm.p_INB.freq = 1000; // FIXME: use params header for this
    (*pFilterSource)->parm.p_INB.tsid = 0; // caller must change if desired

    return MPE_SUCCESS;
}

#endif /* !defined(POWERTV) && defined(TEST_MPEOS) */
