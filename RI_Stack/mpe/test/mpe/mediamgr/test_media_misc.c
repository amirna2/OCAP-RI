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

/** \file test_MEDIA_MISC.c
 *
 *  \brief Test functions for MPEOS memory functions
 *
 *  This file contains tests for the following MPEOS functions :\n
 *
 *    -# mpeos_mediaCheckBounds()\n
 *    -# mpeos_mediaFrequencyToTuner()\n
 *    -# mpeos_mediaGetBounds()\n
 *    -# mpeos_mediaGetMuteState()\n
 *    -# mpeos_mediaGetScaling()\n
 *    -# mpeos_mediaGetTunerFrequency()\n
 *    -# mpeos_mediaGetTunerInfo()\n
 *    -# mpeos_mediaGetVolume()\n
 *    -# mpeos_mediaSetBounds()\n
 *    -# mpeos_mediaSetMuteState()\n
 *    -# mpeos_mediaSetVolume()\n
 */

/*

 Other Media API's tested elsewhere :

 mpeos_mediaDecode
 mpeos_mediaFreeze
 mpeos_mediaInit
 mpeos_mediaPause
 mpeos_mediaRegisterQueueForTuneEvents
 mpeos_mediaResume
 mpeos_mediaShutdown
 mpeos_mediaStop
 mpeos_mediaSwapDecoders
 mpeos_mediaTune
 mpeos_mediaUnregisterQueue

 */

#include <test_media.h>
#include <mpetest_disp.h>
#include <dispmgr.h>
#include <mpe_disp.h>

CuSuite* getTestSuite_mediaMisc(void);

/*  Test functions, defined in this source file  */

static void test_mediaCheckBounds(CuTest*);
static void test_mediaGetBounds(CuTest*);
static void test_mediaGetScaling(CuTest*);
static void test_mediaSetBounds(CuTest*);

/*  The following are only used when running tests for MPEOS  */

#if defined (TEST_MPEOS)
static void test_mediaFrequencyToTuner(CuTest*);
static void test_mediaGetTunerInfo(CuTest*);
//static void test_mediaGetTunerFrequency(CuTest*);
#endif /* defined (TEST_MPEOS) */

/* Internal utility functions, only used in this source file  */

static mpe_DispDevice *getMediaDevices(void);
#if !defined(MPE_LOG_DISABLE)
static char *evalBool(mpe_Bool);
#endif
#if defined (TEST_MPEOS)
static mpe_Bool checkTunerInfo(uint32_t, CuTest*);
#endif /* defined (TEST_MPEOS) */

#define INVALIDFREQ        1987654321  /* 1.98 GHz - invalid tune frequency */
#define MINVALIDFREQ         30000000  /* 30 MHz is minimum valid tune freq */
#define MAXVALIDFREQ        800000000  /* chan 125 is max valid tune freq  */
#define TUNETESTFREQ         55250000

#define BADBOOL            99

#define LASTVALIDTUNER     2           /* last valid tuner ID  */
#define LASTTESTTUNER      12          /* last tuner ID to test  */

static char errorBuffer[256];

/****************************************************************************
 *
 *  test_mediaCheckBounds()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaCheckBounds" function 
 *
 * \api mpeos_mediaCheckBounds()
 *
 * \strategy Call the "mpeos_mediaCheckBounds()" function with a variety of
 *           valid and invalid parameters and checks for correct return values.
 *
 * \assets none
 *
 */

static void test_mediaCheckBounds(CuTest *tc)
{
    mpe_Error ec;
    mpe_DispDevice *allDevices = NULL;
    mpe_MediaRectangle desiredSrc =
    { 10, 100, 200, 200 };
    mpe_MediaRectangle desiredDst =
    { 20, 150, 150, 150 };
    mpe_MediaRectangle actualSrc =
    { 0, 0, 0, 0 };
    mpe_MediaRectangle actualDst =
    { 0, 0, 0, 0 };

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into 'test_mediaCheckBounds()'\n");

    if (NULL == (allDevices = getMediaDevices()))
    {
        CuFail(tc, "  failed to get device descriptors");
        return;
    }

    /**
     *  \assertion 'mediaCheckBounds()' returns MPE_SUCCESS if passed a null pointer
     */
    if (MPE_SUCCESS != (ec = mediaCheckBounds(allDevices[0], &desiredSrc,
            &desiredDst, &actualSrc, &actualDst)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "'mediaCheckBounds()' failed, error == %s\n", decodeError(ec));
        CuFail(tc, "'mediaCheckBounds()' failed to return MPE_SUCCESS");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  mediaCheckBounds() returned :\n");
        TRACE(
                MPE_LOG_TRACE1,
                MPE_MOD_TEST,
                "    dSrc.x = %4d, dSrc.y = %4d, dSrc.width = %4d, dSrc.height = %4d\n",
                desiredSrc.x, desiredSrc.y, desiredSrc.width, desiredSrc.height);
        TRACE(
                MPE_LOG_TRACE1,
                MPE_MOD_TEST,
                "    dDst.x = %4d, dDst.y = %4d, dDst.width = %4d, dDst.height = %4d\n",
                desiredDst.x, desiredDst.y, desiredDst.width, desiredDst.height);
        TRACE(
                MPE_LOG_TRACE1,
                MPE_MOD_TEST,
                "    aSrc.x  = %4d, aSrc.y  = %4d, aSrc.width  = %4d, aSrc.height  = %4d\n",
                actualSrc.x, actualSrc.y, actualSrc.width, actualSrc.height);
        TRACE(
                MPE_LOG_TRACE1,
                MPE_MOD_TEST,
                "    aDst.x  = %4d, aDst.y  = %4d, aDst.width  = %4d, aDst.height  = %4d\n",
                actualDst.x, actualDst.y, actualDst.width, actualDst.height);
    }

    /*  cleanup and return  */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  freeing memory\n");

    if (NULL != allDevices)
    {
        ec = memFreeP(MPE_MEM_TEST, allDevices);
        allDevices = NULL;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'test_mediaCheckBounds()' finished\n");
}

#if defined (TEST_MPEOS)

/****************************************************************************
 *
 *  test_mediaFrequencyToTuner()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaFrequencyToTuner" function 
 *
 * \api mpeos_mediaFrequencyToTuner()
 *
 * \strategy Call the "mpeos_mediaFrequencyToTuner()" function with a
 *           variety of valid and invalid parameters and checks for correct
 *           return values.
 *
 * \assets none
 *
 *      NOTE : This function only exists in MPEOS; there is no
 *             "mpe_mediaFrequencyToTuner()" function.
 */

static void test_mediaFrequencyToTuner(CuTest *tc)
{
    mpe_Error ec;
    uint32_t tunerID = 9999;
    int i;

    uint32_t testFreqs[] =
    {   55250000, 54000000, 61250000, 60000000,
        175250000, 174000000, 470000000, 471250000,
        475250000, 474000000};

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Into 'test_mediaFrequencyToTuner()'\n");

    /**
     *  \assertion 'mediaFrequencyToTuner()' returns MPE_EINVAL if passed a null pointer
     */

    if (MPE_EINVAL != (ec = mediaFrequencyToTuner(testFreqs[0], NULL)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaFrequencyToTuner() with NULL pointer failed\n");
        CuFail (tc, "mediaFrequencyToTuner(NULL) failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaFrequencyToTuner() with NULL pointer passed\n");
    }

    /**
     *  \assertion 'mediaFrequencyToTuner()' returns MPE_EINVAL if passed an
     *   invalid frequency.
     */

    if (MPE_EINVAL != (ec = mediaFrequencyToTuner(1999999999, &tunerID)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaFrequencyToTuner() with invalid frequency failed\n");
        CuFail (tc, "mediaFrequencyToTuner() with invalid frequency failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaFrequencyToTuner() with invalid frequency passed\n");
    }

    /**
     *  \assertion 'mediaFrequencyToTuner()' returns MPE_SUCCESS if a tuner is
     *   tuned to the specified frequency.
     *
     *      need to do an actual tune to have a valid test
     *
     */

    for (i=0; i < sizeof testFreqs / sizeof (testFreqs[0]); i++)
    {
        if (MPE_SUCCESS != (ec = mediaFrequencyToTuner(testFreqs[i], &tunerID)))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  mediaFrequencyToTuner() failed, freq == %9d, tuner == %d, error == %s\n",
                    testFreqs[i], tunerID, decodeError(ec));
            CuFail (tc, "mediaFrequencyToTuner() failed");
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "  mediaFrequencyToTuner() returned %d, freq == %9d kHz\n",
                    tunerID, testFreqs[i]);
        }
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  'test_mediaFrequencyToTuner()' finished\n");
}
#endif /* defined (TEST_MPEOS) */

/****************************************************************************
 *
 *  test_mediaGetBounds()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaGetBounds" function 
 *
 * \api mpeos_mediaGetBounds()
 *
 * \strategy Call the "mpeos_mediaGetBounds()" function with a variety
 *           of valid and invalid parameters and checks for correct return
 *           values.
 *
 * \assets none
 *
 */

static void test_mediaGetBounds(CuTest *tc)
{
    mpe_Error ec;
    mpe_DispDevice *allDevices = NULL;
    mpe_MediaRectangle srcRect =
    { 10, 100, 200, 200 };
    mpe_MediaRectangle dstRect =
    { 20, 150, 150, 150 };

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into 'test_mediaGetBounds()'\n");

    if (NULL == (allDevices = getMediaDevices()))
    {
        CuFail(tc, "  failed to get device descriptors");
        return;
    }

    if (MPE_SUCCESS != (ec = mediaGetBounds(allDevices[0], &srcRect, &dstRect)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetBounds() failed, error == %s\n", decodeError(ec));
        CuFail(tc, "mediaGetBounds() failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  mediaCheckBounds() returned :\n");
        TRACE(
                MPE_LOG_TRACE1,
                MPE_MOD_TEST,
                "    src.x = %4d, src.y = %4d, src.width = %4d, src.height = %4d\n",
                srcRect.x, srcRect.y, srcRect.width, srcRect.height);
        TRACE(
                MPE_LOG_TRACE1,
                MPE_MOD_TEST,
                "    dst.x = %4d, dst.y = %4d, dst.width = %4d, dst.height = %4d\n",
                dstRect.x, dstRect.y, dstRect.width, dstRect.height);
    }

    /*  TODO: need range checks on bounds  */

    /*  cleanup and return  */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  freeing memory\n");

    if (NULL != allDevices)
    {
        ec = memFreeP(MPE_MEM_TEST, allDevices);
        allDevices = NULL;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'test_mediaGetBounds()' finished\n");
    return;
}

/****************************************************************************
 *
 *  test_mediaGetScaling()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaGetScaling" function 
 *
 * \api mpeos_mediaGetScaling()
 *
 * \strategy Call the "mpeos_mediaGetScaling()" function with a variety
 *            of valid and invalid parameters and checks for correct return
 *            values.
 *
 * \assets none
 *
 */

static void test_mediaGetScaling(CuTest *tc)
{
    mpe_Error ec;
    mpe_DispDevice *allDevices = NULL;

    mpe_MediaPositioningCapabilities posn = MPE_POS_CAP_OTHER;
    float *horiz = NULL;
    float *vert = NULL;
    mpe_Bool hRange = BADBOOL;
    mpe_Bool vRange = BADBOOL;
    mpe_Bool canClip = BADBOOL;
    mpe_Bool supportsComponent = BADBOOL;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into 'test_mediaGetScaling()'\n");

    if (NULL == (allDevices = getMediaDevices()))
    {
        CuFail(tc, "  failed to get device descriptors");
        return;
    }

    /**
     *  \assertion 'mediaGetScaling()' returns MPE_EINVAL if an invalid device is passed
     *  TODO: Determine if mpe needs to track valid vs. invalid handles other than just
     *  a NULL pointer check.
     */
#if (0)
    if (MPE_EINVAL != (ec = mediaGetScaling((mpe_DispDevice)9876, &posn, &horiz, &vert,
                            &hRange, &vRange,
                            &canClip, &supportsComponent)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetScaling() with invalid device failed\n");
        CuFail(tc, "mediaGetScaling() with invalid device failed");
    }
#endif

    /**
     *  \assertion 'mediaGetScaling()' returns MPE_SUCCESS if NULL parameter
     *   pointers are passed.
     */

    /**** TODO: add separate test cases for each paramater NULL with others valid */

    if (MPE_SUCCESS != (ec = mediaGetScaling(allDevices[0], NULL, NULL, NULL,
            NULL, NULL, NULL, NULL)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetScaling() with  NULL parameter pointers succeeded\n");
        CuFail(tc, "mediaGetScaling() with NULL parameter pointers succeeded");
    }

    /**
     *  \assertion 'mediaGetScaling()' returns valid scaling information if
     *   valid parameters are passed.
     */

    if (MPE_SUCCESS != (ec = mediaGetScaling(allDevices[0], &posn, &horiz,
            &vert, &hRange, &vRange, &canClip, &supportsComponent)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetScaling() with valid parameters failed\n");
        CuFail(tc, "mediaGetScaling() with valid parameters failed");
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  mediaGetScaling returned :\n");
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    posn     == %d\n", posn);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    horiz    == %p\n", horiz);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    vert     == %p\n", vert);
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    hRange   == %s\n", evalBool(hRange));
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    vRange   == %s\n", evalBool(vRange));
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    canClip  == %s\n",
            evalBool(canClip));
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "    supportsComponent == %s\n",
            evalBool(supportsComponent));

    /*  TODO: need range checks on returned values  */

    /*  cleanup and return  */

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "  freeing memory\n");

    if (NULL != allDevices)
    {
        ec = memFreeP(MPE_MEM_TEST, allDevices);
        allDevices = NULL;
    }

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  'test_mediaGetScaling()' finished\n");
    return;

}

#if 0 //defined (TEST_MPEOS)
/****************************************************************************
 *
 *  test_mediaGetMuteState()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaGetMuteState" function 
 *
 * \api mpeos_mediaGetMuteState()
 *
 * \strategy Call the "mpeos_mediaGetMuteState()" function and verify
 *           that it returns MPE_SUCCESS and that the returned mute state is
 *           either TRUE or FALSE.
 *
 * \assets none
 *
 */

static void test_mediaGetMuteState(CuTest *tc)
{
    mpe_Error ec;
    mpe_Bool muteState = BADBOOL;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Into 'test_mediaGetMuteState()'\n");

    /**
     *  \assertion 'mediaGetMuteState()' returns an error if passed a NULL pointer
     */

    if (MPE_EINVAL != (ec = mediaGetMuteState(NULL)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetMuteState() with  NULL pointer failed\n");
        CuFail(tc, "mediaGetMuteState() with NULL pointer failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetMuteState() with  NULL pointer passed\n");
    }

    /**
     *  \assertion 'mediaGetMuteState()' returns MPE_SUCCESS if passed a valid
     *   pointer.
     */

    muteState = BADBOOL;
    if (MPE_SUCCESS != (ec = mediaGetMuteState(&muteState)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetMuteState() with valid pointer failed\n");
        CuFail(tc, "mediaGetMuteState() with valid pointer failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetMuteState() with valid pointer returned MPE_SUCCESS (passed)\n");
    }

    /**
     *  \assertion 'mediaGetMuteState()' returns either TRUE or FALSE
     */

    if ((TRUE!=muteState)&&(FALSE!=muteState))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetMuteState() failed - returned %s\n", evalBool(muteState));
        CuFail(tc, "mediaGetMuteState() failed to return TRUE or FALSE");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetMuteState() returned %s (passed)\n", evalBool(muteState));
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  'test_mediaGetMuteState()' finished\n");
}

#endif /* #if defined (TEST_MPEOS) */

#if 0 // defined (TEST_MPEOS)
/****************************************************************************
 *
 *  test_mediaGetTunerFrequency()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaGetTunerFrequency" function 
 *
 * \api mpeos_mediaGetTunerFrequency()
 *
 * \strategy Call the "mpeos_mediaGetTunerFrequency()" function with a 
 *           variety of valid and invalid parameters and checks for correct
 *           return values.
 *
 * \assets none
 *
 */

static void test_mediaGetTunerFrequency(CuTest *tc)
{
    mpe_Error ec;
    uint32_t tuner;
    uint32_t freq;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Into 'test_mediaGetTunerFrequency()'\n");

    /**
     *  \assertion 'mediaGetTunerFrequency()' returns an error if passed a null
     *   frequency pointer.
     */

    if (MPE_EINVAL != (ec = mediaGetTunerFrequency(1, NULL)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetTunerFrequency() with NULL frequency pointer failed\n");
        CuFail(tc, "mediaGetTunerFrequency() with NULL frequency pointer failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetTunerFrequency() with NULL frequency pointer passed\n");
    }

    /**
     *  \assertion 'mediaGetTunerFrequency()' returns MPE_EINVAL if passed tuner ID == 0
     */

    if (MPE_EINVAL != (ec = mediaGetTunerFrequency(0, &freq)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetTunerFrequency() with invalid tuner ID (0) returned %s - failed\n",
                decodeError(ec));
        CuFail(tc, "mediaGetTunerFrequency() with invalid tuner ID (0) failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetTunerFrequency() with invalid tuner ID passed\n");
    }

    /**
     *  \assertion 'mediaGetTunerFrequency()' returns MPE_SUCCESS if passed a
     *   valid tuner ID.
     */

    for (tuner = 1; tuner <= LASTVALIDTUNER; tuner++)
    {
        freq = INVALIDFREQ;
        if (MPE_SUCCESS != (ec = mediaGetTunerFrequency(tuner, &freq)))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  mediaGetTunerFrequency() with tuner ID == %d failed\n", tuner);
            CuFail(tc, "mediaGetTunerFrequency() with valid tuner ID failed");
        }
        else if ((freq<MINVALIDFREQ)||(freq>MAXVALIDFREQ))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  mediaGetTunerFrequency() with tuner ID == %d returned invalid frequency (%dHz) - failed\n",
                    tuner, freq);
            CuFail(tc, "mediaGetTunerFrequency() returned invalid frequency");
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "  mediaGetTunerFrequency() : tuner ID == %d, freq == %d - pass\n",
                    tuner, freq);
        }
    }

    /**
     *  \assertion 'mediaGetTunerFrequency()' returns MPE_EINVAL if passed an
     *   invalid tuner ID.
     */

    for (tuner = LASTVALIDTUNER+1; tuner <= LASTTESTTUNER; tuner++)
    {
        if (MPE_EINVAL != (ec = mediaGetTunerFrequency(tuner, &freq)))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  mediaGetTunerFrequency() with invalid tuner ID (%d) returned %s - failed\n",
                    tuner, decodeError(ec));
            CuFail(tc, "mediaGetTunerFrequency() with invalid tuner ID failed");
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "  mediaGetTunerFrequency() with invalid tuner ID (%d) passed\n", tuner);
        }
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  'test_mediaGetTunerFrequency()' finished\n");
}

#endif /* #if defined (TEST_MPEOS) */

#if defined (TEST_MPEOS)

/****************************************************************************
 *
 *  test_mediaGetTunerInfo()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaGetTunerInfo" function 
 *
 * \api mpeos_mediaGetTunerInfo()
 *
 * \strategy Call the "mpeos_mediaGetTunerInfo()" function with a
 *           variety of valid and invalid parameters and checks for correct
 *           return values.
 *
 * \assets none
 *
 *      NOTE : This function only exists in MPEOS; there is no
 *             "mpe_mediaGetTunerInfo()" function.
 */

static void test_mediaGetTunerInfo(CuTest *tc)
{
    mpe_Error ec;
    mpe_MediaTuneParams tuneParams;
    uint32_t tuner;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into 'test_mediaGetTunerInfo()'\n");

    /**
     *  \assertion 'mediaGetTunerInfo()' returns an error if passed a null
     *   mpe_MediaTuneParams pointer.
     */

    if (MPE_EINVAL != (ec = mediaGetTunerInfo(1, NULL)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetTunerInfo() with NULL info pointer failed\n");
        CuFail(tc, "mediaGetTunerInfo() with NULL info pointer failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetTunerInfo() with NULL info pointer passed\n");
    }

    /**
     *  \assertion 'mediaGetTunerInfo()' returns an error if passed an
     *   invalid tuner ID.
     */

    if (MPE_EINVAL != (ec = mediaGetTunerInfo(9999999, &tuneParams)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetTunerInfo() with invalid tuner ID failed\n");
        CuFail(tc, "mediaGetTunerInfo() with invalid tuner ID failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetTunerInfo() with invalid tuner ID passed\n");
    }

    /**
     *  \assertion 'mediaGetTunerInfo()' returns an error if passed
     *   tuner ID == 0
     */

    if (MPE_EINVAL != (ec = mediaGetTunerInfo(0, &tuneParams)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetTunerInfo() with tuner ID == 0 failed\n");
        CuFail(tc, "mediaGetTunerInfo() with tuner ID == 0 failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetTunerInfo() with tuner ID == 0 passed\n");
    }

    /**
     *  \assertion 'mediaGetTunerInfo()' returns MPE_SUCCESS if passed a valid
     *   tuner ID.
     */

    for (tuner = 1; tuner <= LASTVALIDTUNER; tuner++)
    {
        if (!checkTunerInfo(tuner, tc))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  mediaGetTunerInfo() with tuner ID == %d failed\n", tuner);
            CuFail(tc, "mediaGetTunerInfo() with valid tuner ID failed");
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "  mediaGetTunerInfo() with tuner ID == %d passed\n", tuner);
        }
    }

    /**
     *  \assertion 'mediaGetTunerInfo()' returns MPE_EINVAL if passed an invalid
     *   tuner ID.
     */

    for (tuner = LASTVALIDTUNER+1; tuner <= LASTTESTTUNER; tuner++)
    {
        if (checkTunerInfo(tuner, tc))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "  mediaGetTunerInfo() with invalid tuner ID (%d) failed\n", tuner);
            CuFail(tc, "mediaGetTunerInfo() with invalid tuner ID failed");
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                    "  mediaGetTunerInfo() with invalid tuner ID (%d) passed\n", tuner);
        }
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  'test_mediaGetTunerInfo()' finished\n");
}

#endif /* defined (TEST_MPEOS) */

#if 0 // defined (TEST_MPEOS)
/****************************************************************************
 *
 *  test_mediaGetVolume()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaGetVolume" function 
 *
 * \api mpeos_mediaGetVolume()
 *
 * \strategy Call the "mpeos_mediaGetVolume()" function with a variety
 *           of valid and invalid parameters and checks for correct return values.
 *
 * \assets none
 *
 */

static void test_mediaGetVolume(CuTest *tc)
{
    mpe_Error ec;
    uint32_t volume;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into 'test_mediaGetVolume()'\n");

    /**
     *  \assertion 'mediaGetVolume()' returns MPE_EINVAL if passed a null
     *   pointer.
     */

    if (MPE_EINVAL != (ec = mediaGetVolume(NULL)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetVolume() with NULL pointer failed\n");
        CuFail(tc, "mediaGetVolume() with NULL pointer failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetVolume() with NULL pointer passed\n");
    }

    /**
     *  \assertion 'mediaGetVolume()' returns MPE_SUCCESS if passed a
     *   valid pointer.
     */

    if (MPE_SUCCESS != (ec = mediaGetVolume(&volume)))
    {
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                "  mediaGetVolume() with valid pointer failed\n");
        CuFail(tc, "mediaGetVolume() with valid pointer failed");
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "  mediaGetVolume() with valid pointer returned %d (OK)\n", volume);
    }
    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  'test_mediaGetVolume()' finished\n");
}

#endif /* defined (TEST_MPEOS) */

/*\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/
 = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
 /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\*/

/****************************************************************************
 *
 *  test_mediaSetBounds()
 *
 ***************************************************************************/
/**
 * \testdescription Tests the "mpeos_mediaSetBounds" function 
 *
 * \api mpeos_mediaSetBounds()
 *
 * \strategy Call the "mpeos_mediaSetBounds()" function with a variety
 *           of valid and invalid parameters and checks for correct return values.
 *
 * \assets none
 *
 */

/*
 mpe_Error mpeos_mediaSetBounds(
 mpe_DispDevice videoDevice,
 mpe_MediaRectangle * srcRect,
 mpe_MediaRectangle * destRect );
 */

static void test_mediaSetBounds(CuTest *tc)
{
    //    mpe_Error ec;
    //    uint32_t volume;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Into 'test_mediaSetBounds()'\n");

    /**
     *  \assertion 'mediaGetVolume()' returns MPE_EINVAL if passed a null
     *   pointer.
     */

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "\n ###### 'test_mediaSetBounds()' not implemented yet\n");
}

#if defined (TEST_MPEOS)

/****************************************************************************
 *
 *  checkTunerInfo() - Calls "mediaGetTunerInfo()" and does sanity checks
 *                     on returned information.
 *
 ***************************************************************************/
/*
 *  Passed : Tuner number and CuTest pointer
 *
 *  Returns : TRUE if info looks OK
 *            FALSE if "mediaGetTunerInfo()" returns an error or info
 *                  looks invalid
 *
 */

static mpe_Bool checkTunerInfo(uint32_t tuner, CuTest *tc)
{
    mpe_Error ec;
    mpe_MediaTuneParams tuneParams;
    mpe_Bool ret = TRUE;

    tuneParams.tuneType = 254;
    tuneParams.sourceId = 987654321;
    tuneParams.frequency = INVALIDFREQ;
    tuneParams.programNumber = 987654321;
    tuneParams.qamMode = 199;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "  checkTunerInfo() : tuner ID == %d\n", tuner);

    ec = mediaGetTunerInfo(tuner, &tuneParams);

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
            "  mediaGetTunerInfo() with tuner ID == %d returned %s\n",
            tuner, decodeError(ec));

    if (MPE_SUCCESS == ec)
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    frequency == %d - ", tuneParams.frequency);
        if ((tuneParams.frequency<MINVALIDFREQ)||(tuneParams.frequency>MAXVALIDFREQ))
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "invalid frequency (%d) - failed\n", tuneParams.frequency);
            ret = FALSE;
            CuFail(tc, "mediaGetTunerInfo() returned invalid frequency");
        }
        else
        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "OK\n");
        }

        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    sourceId == %d - ", tuneParams.sourceId);
        if (tuneParams.sourceId>100000) // TODO : fix hardcoded constant

        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "invalid sourceId - failed\n");
            ret = FALSE;
            CuFail(tc, "mediaGetTunerInfo() returned invalid sourceId");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OK\n");
        }

        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    tuneType == %d - ", tuneParams.tuneType);
        if (tuneParams.tuneType>253) // TODO : fix hardcoded constant

        {
            TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "invalid tuneType - failed\n");
            ret = FALSE;
            CuFail(tc, "mediaGetTunerInfo() returned invalid tuneType");
        }
        else
        {
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OK\n");
        }
    }
    else if (MPE_ERROR_MEDIA_RESOURCE_NOT_ACTIVE == ec)
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    Tuner %d isn't tuned\n", tuner);
        ret = TRUE;
    }
    else
    {
        ret = FALSE;
    }

    return (ret);
}

#endif  /* defined (TEST_MPEOS) */

/****************************************************************************
 *
 *  getMediaDevices() - get an array of available media device handles
 *
 ***************************************************************************/
/*
 *  Returns a pointer to an array of "mpe_DispDevice" display device handles
 *  which represents all of the display devices availabel on the system.
 *
 *  For now, it just returns the handles for the devices for the first screen.
 *
 *  Returns NULL on error
 *
 *  NOTE : This function allocates memory for the returned handles and the
 *         caller should free that memory by calling memFreeP() on the
 *         pointer returned by this function.
 *
 */

static mpe_DispDevice *getMediaDevices(void)
{
    mpe_Error ec;
    mpe_DispDevice *allDevices = NULL;
    mpe_DispScreen *allScreens = NULL;
    uint32_t screenCount = 9999;
    uint32_t deviceCount = 9999;

    TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "  getMediaDevices() :\n");

    /*  Get the screen count  */

    ec = dispGetScreenCount(&screenCount);
    if ((MPE_SUCCESS != ec) || (0 == screenCount))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    dispGetScreenCount() failed, error = %d, count == %d\n",
                ec, screenCount);
        return (NULL);
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    dispGetScreenCount() returned %d\n", screenCount);
    }

    /*  Allocate memory for screen descriptors  */

    if (MPE_SUCCESS != (ec = memAllocP(MPE_MEM_TEST, (sizeof(mpe_DispScreen)
            * screenCount) + 10, (void*) &allScreens)))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    memAllocP() failed to allocate memory for screen descriptors\n");
        return (NULL);
    }

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
            "    allocated memory for screens at %p\n", allScreens);

    /*  Get screen descriptors  */

    if (MPE_SUCCESS != (ec = dispGetScreens(allScreens)))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    dispGetScreens() failed to get screen descriptors (%d)\n",
                ec);
        memFreeP(MPE_MEM_TEST, allScreens);
        return (NULL);
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "    got screen descriptors\n");
    }

    /*  Get the device count for the first screen  */

    if (MPE_SUCCESS != (ec = dispGetDeviceCount(allScreens[0],
            MPE_DISPLAY_ALL_DEVICES, &deviceCount)))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST, "    dispGetDeviceCount() failed\n");
        memFreeP(MPE_MEM_TEST, allScreens);
        return (NULL);
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    dispGetDeviceCount() returned %d\n", deviceCount);
    }

    /*  Allocate memory for device descriptors  */

    if (MPE_SUCCESS != (ec = memAllocP(MPE_MEM_TEST, (sizeof(mpe_DispDevice)
            * deviceCount) + 10, (void*) &allDevices)))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    memAllocP() failed to allocate memory for device descriptors\n");
        memFreeP(MPE_MEM_TEST, allScreens);
        return (NULL);
    }
    else
    {
        TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST,
                "    allocated memory for devices at %p\n", allDevices);
    }

    /*  Get device descriptors  */

    if (MPE_SUCCESS != (ec = dispGetDevices(allScreens[0],
            MPE_DISPLAY_ALL_DEVICES, allDevices)))
    {
        TRACE(MPE_LOG_FATAL, MPE_MOD_TEST,
                "    dispGetScreens() failed to get device descriptors (%d)\n",
                ec);
        memFreeP(MPE_MEM_TEST, allDevices);
        allDevices = NULL;

        /* on error fall through, free screen memory and return NULL 'allDevices' */

    }

    /*  Free screen memory. Caller is responsible for freeing display handle memory  */

    if (NULL != allScreens)
    {
        memFreeP(MPE_MEM_TEST, allScreens);
    }

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "    getMediaDevices() returning %p\n",
            allDevices);

    return (allDevices);
}

#if !defined(MPE_LOG_DISABLE)
/****************************************************************************
 *
 *  evalBool() - return a string indicating the value of an 'mpe_Bool'
 *
 ***************************************************************************/
/*
 *  Return a string indicating the value of an 'mpe_Bool'.
 *
 *    This seems like a generally useful function which should live
 *    somewhere else, so other code can use it.
 *
 */
static char *evalBool(mpe_Bool b)
{
    switch (b)
    {
    case TRUE:
        return ("true");
        break;
    case FALSE:
        return ("false");
        break;
    case BADBOOL:
        return ("bad-bool");
        break;
    default:
        return ("invalid");
        break;
    }
}
#endif

/****************************************************************************
 *
 * getTestSuite_mediaMisc() - return the Media misc test suite
 *
 ***************************************************************************/

CuSuite* getTestSuite_mediaMisc(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_ADD_TEST(suite, test_mediaCheckBounds);
    SUITE_ADD_TEST(suite, test_mediaGetBounds);
    SUITE_ADD_TEST(suite, test_mediaGetScaling);
    SUITE_ADD_TEST(suite, test_mediaSetBounds);
#if defined (TEST_MPEOS)
    SUITE_ADD_TEST(suite, test_mediaFrequencyToTuner);
    SUITE_ADD_TEST(suite, test_mediaGetTunerInfo);
    //    SUITE_ADD_TEST(suite, test_mediaGetTunerFrequency);
#endif /* defined (TEST_MPEOS) */

    return suite;
}

