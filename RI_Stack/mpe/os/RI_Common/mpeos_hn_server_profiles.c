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

#include <string.h>

#include "mpeos_hn.h"
#include "mpeos_dbg.h"

//#define COMPLICATED_DLNA_PROFILE_ID_TESTING

/**
 * Describes a supported DLNA profile
 */
typedef struct
{
    char profileId[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];
    char mimeType[MPE_HN_MAX_MIME_TYPE_STR_SIZE];
    uint32_t playspeedsCnt;
    float* playspeeds;
    uint32_t frameTypesInTrickModeCnt;
    mpe_HnHttpHeaderFrameTypesInTrickMode* frameTypesInTrickMode;
    uint32_t frameRateInTrickModeCnt;
    int32_t* frameRateInTrickMode;
    mpe_HnStreamContentLocation contentLocationType;
}
hn_server_profile;


typedef struct
{
    int32_t id;                         ///< The numerical ID for the transformation
    char sourceProfile[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];
                                        ///< The source DLNA profile that can be converted
    char transformedProfile[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE];
                                        ///< The transformed DLNA profile
    char mimeType[MPE_HN_MAX_MIME_TYPE_STR_SIZE];
    int32_t bitrate;                    ///< The maximum (fixed) bitrate for this transformation (in kbps)
    int32_t width;                      ///< The maximum horizontal resolution for this transformation
    int32_t height;                     ///< The maximum vertical resolution for this transformation
    mpe_Bool progressive;               ///< The supported frame format (progressive/interlaced)
} hn_server_transformation;

// Local defines to avoid all the unnecessary typing
#define trick_t mpe_HnHttpHeaderFrameTypesInTrickMode
#define FT_I    MPE_HN_TRICK_MODE_FRAME_TYPE_I
#define FT_IP   MPE_HN_TRICK_MODE_FRAME_TYPE_IP
#define FT_ALL  MPE_HN_TRICK_MODE_FRAME_TYPE_ALL
#define FT_NONE MPE_HN_TRICK_MODE_FRAME_TYPE_NONE

static hn_server_transformation g_transformationCapabilities[] =
{ // id       sourceProfile   transformedProfile   mimetype   bitrate   width     height    progressive
  // (uint32) (char [])       (char [])            (char [])  (int32_t) (int32_t) (int32_t) mpe_Bool
  //          DLNA profile    DLNA Profile                    (kbps)    (pixels)  (pixels)
    { 1,   "MPEG_TS_NA_ISO",  "AVC_TS_NA_ISO",     "video/mpeg",   1000,     1280,    720,   1 },
    { 2,   "MPEG_TS_NA_ISO",  "AVC_TS_NA_ISO",     "video/mpeg",   1000,     1280,    720,   0 },
    { 3,   "MPEG_TS_NA_ISO",  "AVC_TS_NA_ISO",     "video/mpeg",   2000,     1920,    1080,  1 },
    { 4,   "MPEG_TS_NA_ISO",  "AVC_TS_NA_ISO",     "video/mpeg",    256,     352,     288,   1 },
};

static uint32_t g_transformationCnt = sizeof(g_transformationCapabilities)
                                      / sizeof(g_transformationCapabilities[0]);

// The playspeeds defined in ps_MPEG_TS_SD_NA_ISO_mpeg, the frame type in trick modes
// defined in ft_MPEG_TS_SD_NA_ISO_mpeg, and the frame rates define in fr_MPEG_TS_SD_NA_ISO_mpeg
// are the same for MPEG_TS_NA_ISO and MPEG_TS_SD_NA_ISO profiles.  Instead of creating duplicates
// of these arrays, only one version is created named "*_MPEG_TS_SD_NA_ISO_mpeg" which is used for both
static float   ps_MPEG_TS_SD_NA_ISO_mpeg[] = { -64., -32., -16.,  -8.,  -4.,  -2.,  -1., -0.5,  0.5,   2.,   4.,   8.,  16.,  32.,  64. };
static trick_t ft_MPEG_TS_SD_NA_ISO_mpeg[] = { FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I };
static int32_t fr_MPEG_TS_SD_NA_ISO_mpeg[] = {    2,    2,    4,    4,    8,    8,    8,    8,    8,    8,    8,    4,    4,    2,    2 };

// DLNA Profiles supported by this server
#ifndef COMPLICATED_DLNA_PROFILE_ID_TESTING

static hn_server_profile g_serverProfiles[] =
{
    { "MPEG_TS_SD_NA_ISO", "video/mpeg",
      sizeof(ps_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(ft_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(fr_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_SD_NA_ISO_mpeg,
      MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT },

    // ps_MPEG_TS_SD_NA_ISO_mpeg, ft_MPEG_TS_SD_NA_ISO_mpeg, fr_MPEG_TS_SD_NA_ISO_mpeg
    // apply to both MPEG_TS_NA_ISO and MPEG_TS_SD_NA_ISO profiles
    { "MPEG_TS_NA_ISO", "video/mpeg",
      sizeof(ps_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(ft_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(fr_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_SD_NA_ISO_mpeg,
      MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT },

    { "MPEG_TS_SD_NA_ISO", "video/mpeg", 0, NULL, 0, NULL, 0, NULL,
      MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT},

    { "MPEG_TS_NA_ISO", "video/mpeg", 0, NULL, 0, NULL, 0, NULL,
      MPE_HN_CONTENT_LOCATION_LOCAL_FILE_CONTENT},

    { "MPEG_TS_SD_NA_ISO", "video/mpeg",
      sizeof(ps_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(ft_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(fr_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_SD_NA_ISO_mpeg,
      MPE_HN_CONTENT_LOCATION_LOCAL_TSB },

    { "MPEG_TS_NA_ISO", "video/mpeg",
      sizeof(ps_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(ft_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_SD_NA_ISO_mpeg,
      sizeof(fr_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_SD_NA_ISO_mpeg,
      MPE_HN_CONTENT_LOCATION_LOCAL_TSB },

    { "MPEG_TS_SD_NA_ISO", "video/mpeg", 0, NULL, 0, NULL, 0, NULL,
      MPE_HN_CONTENT_LOCATION_LOCAL_TUNER},

    { "MPEG_TS_NA_ISO", "video/mpeg", 0, NULL, 0, NULL, 0, NULL,
      MPE_HN_CONTENT_LOCATION_LOCAL_TUNER},

    { "MPEG_TS_SD_NA_ISO", "video/mpeg", 0, NULL, 0, NULL, 0, NULL,
      MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE},

    { "MPEG_TS_NA_ISO", "video/mpeg", 0, NULL, 0, NULL, 0, NULL,
      MPE_HN_CONTENT_LOCATION_LOCAL_VIDEO_DEVICE}
};

#else // COMPLICATED_DLNA_PROFILE_ID_TESTING

static float   ps_MPEG_TS_SD_NA_ISO_vnd[] = { -16.,   -8.,   -4.,    -2.,    -1.,     2.,    4.,    8.,  16. };
static trick_t ft_MPEG_TS_SD_NA_ISO_vnd[] = { FT_I, FT_IP, FT_IP, FT_ALL, FT_ALL, FT_ALL, FT_IP, FT_IP, FT_I };
static int32_t fr_MPEG_TS_SD_NA_ISO_vnd[] = {    2,     8,     8,     30,     30,     30,     8,     8,    2 };

static float   ps_MPEG_TS_HD_NA_ISO_mpeg[] = { -64., -32., -16.,  -8.,  -4.,  -2.,  -1.,   2.,   4.,   8.,  16.,  32.,  64. };
static trick_t ft_MPEG_TS_HD_NA_ISO_mpeg[] = { FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I, FT_I };
static int32_t fr_MPEG_TS_HD_NA_ISO_mpeg[] = {    1,    1,    1,    2,    2,    4,    8,    8,    2,    2,    1,    1,    1 };

static float   ps_MPEG_TS_HD_NA_ISO_vnd[] = { -16.,  -8.,  -4.,    2.,    -1.,    2.,   4.,   8.,  16. };
static trick_t ft_MPEG_TS_HD_NA_ISO_vnd[] = { FT_I, FT_I, FT_I, FT_IP, FT_ALL, FT_IP, FT_I, FT_I, FT_I };
static int32_t fr_MPEG_TS_HD_NA_ISO_vnd[] = {    1,    1,    2,     8,     30,     8,    2,    1,    2 };

static float   ps_MPEG_TS_SD_NA_XAC3_ISO_mpeg[] = {  -32.,  -16.,   -8.,   -4.,   -2.,   -1.,    2.,    4.,    8.,   16.,   32. };
static trick_t ft_MPEG_TS_SD_NA_XAC3_ISO_mpeg[] = { FT_IP, FT_IP, FT_IP, FT_IP, FT_IP, FT_IP, FT_IP, FT_IP, FT_IP, FT_IP, FT_IP };
static int32_t fr_MPEG_TS_SD_NA_XAC3_ISO_mpeg[] = {    15,    15,    15,    15,    15,    15,    15,    15,    15,    15,    15 };

static float   ps_MPEG_TS_DTS_T_vnd[] = {      0. };
static trick_t ft_MPEG_TS_DTS_T_vnd[] = { FT_NONE };
static int32_t fr_MPEG_TS_DTS_T_vnd[] = {       0 };

static float   ps_MPEG4_P2_TS_SP_AAC_ISO_mpeg[] = {    -8.,    -4.,    -2.,    -1.,     2.,     4.,     8. };
static trick_t ft_MPEG4_P2_TS_SP_AAC_ISO_mpeg[] = { FT_ALL, FT_ALL, FT_ALL, FT_ALL, FT_ALL, FT_ALL, FT_ALL };
static int32_t fr_MPEG4_P2_TS_SP_AAC_ISO_mpeg[] = {     24,     24,     24,     24,     24,     24,     24 };

static float   ps_MPEG4_P2_TS_SP_AAC_ISO_mp4[] = {   -4.,   -2.,    -1.,     2.,     4. };
static trick_t ft_MPEG4_P2_TS_SP_AAC_ISO_mp4[] = { FT_IP, FT_IP, FT_ALL, FT_ALL, FT_ALL };
static int32_t fr_MPEG4_P2_TS_SP_AAC_ISO_mp4[] = {    15,    15,     30,     30,     30 };

static float   ps_MPEG4_P2_TS_SP_AAC_ISO_vnd[] = {    -2.,    -1.,     2. };
static trick_t ft_MPEG4_P2_TS_SP_AAC_ISO_vnd[] = { FT_ALL, FT_ALL, FT_ALL };
static int32_t fr_MPEG4_P2_TS_SP_AAC_ISO_vnd[] = {     30,     30,     30 };

static float   ps_MPEG4_P2_TS_SP_AAC_ISO_3gpp[] = {   -4.,   -2.,   -1.,     2,    4. };
static trick_t ft_MPEG4_P2_TS_SP_AAC_ISO_3gpp[] = { FT_IP, FT_IP, FT_IP, FT_IP, FT_IP };
static int32_t fr_MPEG4_P2_TS_SP_AAC_ISO_3gpp[] = {    15,    15,    15,    15,    15 };

static float   ps_MPEG4_P2_MP4_ASP_AAC_mp4[] = {   -1 };
static trick_t ft_MPEG4_P2_MP4_ASP_AAC_mp4[] = { FT_I };
static int32_t fr_MPEG4_P2_MP4_ASP_AAC_mp4[] = {    8 };

static hn_server_profile g_serverProfiles[] =
{
    { "MPEG_TS_SD_NA_ISO", "video/mpeg",
        sizeof(ps_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_SD_NA_ISO_mpeg,
        sizeof(ft_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_SD_NA_ISO_mpeg,
        sizeof(fr_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_SD_NA_ISO_mpeg,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG_TS_SD_NA_ISO", "video/vnd.dlna.mpeg-tts",
        sizeof(ps_MPEG_TS_SD_NA_ISO_vnd) / sizeof(float),   ps_MPEG_TS_SD_NA_ISO_vnd,
        sizeof(ft_MPEG_TS_SD_NA_ISO_vnd) / sizeof(trick_t), ft_MPEG_TS_SD_NA_ISO_vnd,
        sizeof(fr_MPEG_TS_SD_NA_ISO_vnd) / sizeof(int32_t), fr_MPEG_TS_SD_NA_ISO_vnd,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG_TS_HD_NA_ISO", "video/mpeg",
        sizeof(ps_MPEG_TS_HD_NA_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_HD_NA_ISO_mpeg,
        sizeof(ft_MPEG_TS_HD_NA_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_HD_NA_ISO_mpeg,
        sizeof(fr_MPEG_TS_HD_NA_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_HD_NA_ISO_mpeg,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG_TS_HD_NA_ISO", "video/vnd.dlna.mpeg-tts",
        sizeof(ps_MPEG_TS_HD_NA_ISO_vnd) / sizeof(float),   ps_MPEG_TS_HD_NA_ISO_vnd,
        sizeof(ft_MPEG_TS_HD_NA_ISO_vnd) / sizeof(trick_t), ft_MPEG_TS_HD_NA_ISO_vnd,
        sizeof(fr_MPEG_TS_HD_NA_ISO_vnd) / sizeof(int32_t), fr_MPEG_TS_HD_NA_ISO_vnd,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG_TS_SD_NA_XAC3_ISO", "video/mpeg",
        sizeof(ps_MPEG_TS_SD_NA_XAC3_ISO_mpeg) / sizeof(float),   ps_MPEG_TS_SD_NA_XAC3_ISO_mpeg,
        sizeof(ft_MPEG_TS_SD_NA_XAC3_ISO_mpeg) / sizeof(trick_t), ft_MPEG_TS_SD_NA_XAC3_ISO_mpeg,
        sizeof(fr_MPEG_TS_SD_NA_XAC3_ISO_mpeg) / sizeof(int32_t), fr_MPEG_TS_SD_NA_XAC3_ISO_mpeg,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG_TS_DTS_T", "video/vnd.dlna.mpeg-tts",
        sizeof(ps_MPEG_TS_DTS_T_vnd) / sizeof(float),   ps_MPEG_TS_DTS_T_vnd,
        sizeof(ft_MPEG_TS_DTS_T_vnd) / sizeof(trick_t), ft_MPEG_TS_DTS_T_vnd,
        sizeof(fr_MPEG_TS_DTS_T_vnd) / sizeof(int32_t), fr_MPEG_TS_DTS_T_vnd,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG4_P2_TS_SP_AAC_ISO", "video/mpeg", 
        sizeof(ps_MPEG4_P2_TS_SP_AAC_ISO_mpeg) / sizeof(float),   ps_MPEG4_P2_TS_SP_AAC_ISO_mpeg,
        sizeof(ft_MPEG4_P2_TS_SP_AAC_ISO_mpeg) / sizeof(trick_t), ft_MPEG4_P2_TS_SP_AAC_ISO_mpeg,
        sizeof(fr_MPEG4_P2_TS_SP_AAC_ISO_mpeg) / sizeof(int32_t), fr_MPEG4_P2_TS_SP_AAC_ISO_mpeg,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG4_P2_TS_SP_AAC_ISO", "video/mp4",
        sizeof(ps_MPEG4_P2_TS_SP_AAC_ISO_mp4) / sizeof(float),   ps_MPEG4_P2_TS_SP_AAC_ISO_mp4,
        sizeof(ft_MPEG4_P2_TS_SP_AAC_ISO_mp4) / sizeof(trick_t), ft_MPEG4_P2_TS_SP_AAC_ISO_mp4,
        sizeof(fr_MPEG4_P2_TS_SP_AAC_ISO_mp4) / sizeof(int32_t), fr_MPEG4_P2_TS_SP_AAC_ISO_mp4,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG4_P2_TS_SP_AAC_ISO", "video/vnd.dlna.mpeg-tts",
        sizeof(ps_MPEG4_P2_TS_SP_AAC_ISO_vnd) / sizeof(float),   ps_MPEG4_P2_TS_SP_AAC_ISO_vnd,
        sizeof(ft_MPEG4_P2_TS_SP_AAC_ISO_vnd) / sizeof(trick_t), ft_MPEG4_P2_TS_SP_AAC_ISO_vnd,
        sizeof(fr_MPEG4_P2_TS_SP_AAC_ISO_vnd) / sizeof(int32_t), fr_MPEG4_P2_TS_SP_AAC_ISO_vnd,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG4_P2_TS_SP_AAC_ISO", "video/3gpp",
        sizeof(ps_MPEG4_P2_TS_SP_AAC_ISO_3gpp) / sizeof(float),   ps_MPEG4_P2_TS_SP_AAC_ISO_3gpp,
        sizeof(ft_MPEG4_P2_TS_SP_AAC_ISO_3gpp) / sizeof(trick_t), ft_MPEG4_P2_TS_SP_AAC_ISO_3gpp,
        sizeof(fr_MPEG4_P2_TS_SP_AAC_ISO_3gpp) / sizeof(int32_t), fr_MPEG4_P2_TS_SP_AAC_ISO_3gpp,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  },
    { "MPEG4_P2_MP4_ASP_AAC", "video/mp4",
        sizeof(ps_MPEG4_P2_MP4_ASP_AAC_mp4) / sizeof(float),   ps_MPEG4_P2_MP4_ASP_AAC_mp4,
        sizeof(ft_MPEG4_P2_MP4_ASP_AAC_mp4) / sizeof(trick_t), ft_MPEG4_P2_MP4_ASP_AAC_mp4,
        sizeof(fr_MPEG4_P2_MP4_ASP_AAC_mp4) / sizeof(int32_t), fr_MPEG4_P2_MP4_ASP_AAC_mp4,
        MPE_HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT  }
};

#endif // COMPLICATED_DLNA_PROFILE_ID_TESTING

static uint32_t g_serverProfilesCnt = sizeof(g_serverProfiles) / sizeof(g_serverProfiles[0]);

// Forward declarations of local methods.
static char* getActualDLNAProfileID(char * profileIdStr);

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetContentTransformationCnt(uint32_t * transformCapabilityCnt)
{
    *transformCapabilityCnt = g_transformationCnt;
    return MPE_SUCCESS;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetContentTransformations(
        mpe_hnContentTransformation transformationCapabilities[])
{
     int i;

     for (i=0; i < g_transformationCnt; i++)
     {
         transformationCapabilities[i].id = g_transformationCapabilities[i].id;
         strncpy( transformationCapabilities[i].sourceProfile,
                  g_transformationCapabilities[i].sourceProfile,
                  MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE );
         strncpy( transformationCapabilities[i].transformedProfile,
                  g_transformationCapabilities[i].transformedProfile,
                  MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE );
         transformationCapabilities[i].bitrate = g_transformationCapabilities[i].bitrate;
         transformationCapabilities[i].width = g_transformationCapabilities[i].width;
         transformationCapabilities[i].height = g_transformationCapabilities[i].height;
         transformationCapabilities[i].progressive = g_transformationCapabilities[i].progressive;
     }
     return MPE_SUCCESS;
}


/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetDLNAProfileIDsCnt(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        uint32_t * profileIDCnt)
{
    int i = 0;

    MPE_UNUSED_PARAM(contentDescription);

    *profileIDCnt = 0;

    // Look at all possible profiles
    for (i = 0; i < g_serverProfilesCnt; i++)
    {
        // If content location of possible profile matches, this is a supported profile
        if (g_serverProfiles[i].contentLocationType == contentLocation)
        {
            (*profileIDCnt)++;
        }
    }

    return MPE_SUCCESS;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetDLNAProfileIDStr(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        uint32_t idx, char profileIDStr[MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE])
{
    mpe_Error ret = MPE_EINVAL;

    MPE_UNUSED_PARAM(contentDescription);

    if (idx >= 0)
    {
        int i = 0;
        int matchingProfileIdx = 0;

        for (i = 0; i < g_serverProfilesCnt; i++)
        {
            if (g_serverProfiles[i].contentLocationType == contentLocation)
            {
                if (matchingProfileIdx == idx)
                {
                    strncpy(profileIDStr, g_serverProfiles[i].profileId,
                        MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE);
                    ret = MPE_SUCCESS;
                    break;
                }
                matchingProfileIdx++;
            }
        }
    }
    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetMimeTypesCnt(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIdStr, uint32_t * mimeTypeCnt)
{
    mpe_Error ret = MPE_EINVAL;

    int i = 0;
    uint32_t mimeTypes = 0;
    char *actualProfileId = getActualDLNAProfileID(profileIdStr);

    MPE_UNUSED_PARAM(contentDescription);

    for (i = 0; i < g_serverProfilesCnt; i++)
    {
        if ((strncmp(actualProfileId, g_serverProfiles[i].profileId,
                    MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0) &&
            (g_serverProfiles[i].contentLocationType == contentLocation))
        {
            mimeTypes++;
        }
    }

    for (i=0; i < g_transformationCnt; i++)
    {
        if ((strncmp(actualProfileId, g_transformationCapabilities[i].transformedProfile,
                    MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0))
        {
            mimeTypes++;
            break; // Profiles to MIME types are 1-to-1. Just count the first match
        }
    }

    if (mimeTypes > 0)
    {
        *mimeTypeCnt = mimeTypes;
        ret = MPE_SUCCESS;
    }

    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetMimeTypeStr(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIdStr, uint32_t idx,
        char mimeTypeStr[MPE_HN_MAX_MIME_TYPE_STR_SIZE])
{
    int i = 0;
    uint32_t mimeTypeIdx = 0;
    char *actualProfileId = getActualDLNAProfileID(profileIdStr);

    MPE_UNUSED_PARAM(contentDescription);

    for (i = 0; i < g_serverProfilesCnt; i++)
    {
        if ((strncmp(actualProfileId, g_serverProfiles[i].profileId,
                    MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0) &&
            (g_serverProfiles[i].contentLocationType == contentLocation))
        {
            if (idx == mimeTypeIdx)
            {
                strncpy(mimeTypeStr, g_serverProfiles[i].mimeType, MPE_HN_MAX_MIME_TYPE_STR_SIZE);
                return MPE_SUCCESS;
            }
            mimeTypeIdx++;
        }
    }

    for (i=0; i < g_transformationCnt; i++)
    {
        if ((strncmp(actualProfileId, g_transformationCapabilities[i].transformedProfile,
                     MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0))
        {
            if (idx == mimeTypeIdx)
            {
                strncpy( mimeTypeStr, g_transformationCapabilities[i].mimeType,
                         MPE_HN_MAX_MIME_TYPE_STR_SIZE );
                return MPE_SUCCESS;
            }
            mimeTypeIdx++;
        }
    }


    return MPE_EINVAL;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetPlayspeedsCnt(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation * transformation,
        uint32_t * playspeedCnt)
{
    int i = 0;
    char *actualProfileId = getActualDLNAProfileID(profileIDStr);

    MPE_UNUSED_PARAM(contentDescription);
    MPE_UNUSED_PARAM(transformation);

    for (i = 0; i < g_serverProfilesCnt; i++)
    {
        if (strncmp(actualProfileId, g_serverProfiles[i].profileId,
                    MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0 &&
            strncmp(mimeTypeStr, g_serverProfiles[i].mimeType,
                    MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0 &&
            g_serverProfiles[i].contentLocationType == contentLocation)
        {
            *playspeedCnt = g_serverProfiles[i].playspeedsCnt;
            return MPE_SUCCESS;
        }
    }

    for (i=0; i < g_transformationCnt; i++)
    {
        if ( (strncmp(actualProfileId, g_transformationCapabilities[i].transformedProfile,
                      MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0)
             && (strncmp(mimeTypeStr, g_transformationCapabilities[i].mimeType,
                      MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0) )
        {
            *playspeedCnt = sizeof(ps_MPEG_TS_SD_NA_ISO_mpeg) / sizeof(float);
            return MPE_SUCCESS;
        }
    }

    return MPE_EINVAL;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetPlayspeedStr(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation * transformation,
        uint32_t idx, char playspeedStr[MPE_HN_MAX_PLAYSPEED_STR_SIZE])
{
    int i = 0;
    char *actualProfileId = getActualDLNAProfileID(profileIDStr);

    MPE_UNUSED_PARAM(contentDescription);
    MPE_UNUSED_PARAM(transformation);

    if (transformation != NULL)
    {
        snprintf(playspeedStr, MPE_HN_MAX_PLAYSPEED_STR_SIZE, "%f", ps_MPEG_TS_SD_NA_ISO_mpeg[idx]);
        return MPE_SUCCESS;
    }

    for (i = 0; i < g_serverProfilesCnt; i++)
    {
        if (strncmp(actualProfileId, g_serverProfiles[i].profileId,
                    MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0 &&
            strncmp(mimeTypeStr, g_serverProfiles[i].mimeType,
                    MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0 &&
            g_serverProfiles[i].contentLocationType == contentLocation)
        {
            if (g_serverProfiles[i].playspeedsCnt > 0 &&
                (idx >= 0 || idx < g_serverProfiles[i].playspeedsCnt))
            {
                snprintf(playspeedStr, MPE_HN_MAX_PLAYSPEED_STR_SIZE, "%f", g_serverProfiles[i].playspeeds[idx]);
                return MPE_SUCCESS;
            }
        }
    }

    for (i=0; i < g_transformationCnt; i++)
    {
        if ( (strncmp(actualProfileId, g_transformationCapabilities[i].transformedProfile,
                      MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0)
             && (strncmp(mimeTypeStr, g_transformationCapabilities[i].mimeType,
                      MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0) )
        {
            snprintf(playspeedStr, MPE_HN_MAX_PLAYSPEED_STR_SIZE, "%f", ps_MPEG_TS_SD_NA_ISO_mpeg[idx]);
            return MPE_SUCCESS;
        }
    }

    return MPE_EINVAL;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetFrameTypesInTrickMode(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation * transformation,
        float playspeedRate, mpe_HnHttpHeaderFrameTypesInTrickMode * frameType)
{
    mpe_Error ret = MPE_EINVAL;

    int i = 0;
    char *actualProfileId = getActualDLNAProfileID(profileIDStr);

    MPE_UNUSED_PARAM(contentDescription);
    MPE_UNUSED_PARAM(transformation);

    for (i = 0; i < g_serverProfilesCnt && ret == MPE_EINVAL; i++)
    {
        if (strncmp(actualProfileId, g_serverProfiles[i].profileId,
                    MPE_HN_MAX_DLNA_PROFILE_ID_STR_SIZE) == 0 &&
            strncmp(mimeTypeStr, g_serverProfiles[i].mimeType,
                    MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0 &&
            g_serverProfiles[i].contentLocationType == contentLocation)
        {
             // Sanity check
            if (g_serverProfiles[i].frameTypesInTrickModeCnt != g_serverProfiles[i].playspeedsCnt)
            {
                ret = MPE_ENODATA;
            }
            else if (playspeedRate == 1.0)
            {
                // Handle special case of 1x, which cannot be listed in the array
                // due to DLNA requirement [7.3.35].
                *frameType = MPE_HN_TRICK_MODE_FRAME_TYPE_ALL;
                ret = MPE_SUCCESS;
            }
            else
            {
                int j = 0;
                for (j = 0; j < g_serverProfiles[i].frameTypesInTrickModeCnt; j++)
                {
                    if (g_serverProfiles[i].playspeeds[j] == playspeedRate)
                    {
                        *frameType = g_serverProfiles[i].frameTypesInTrickMode[j];
                        ret = MPE_SUCCESS;
                        break;
                    }
                }
            }
        }
    }

    return ret;
}

/**
 * Refer to mpeos_hn.h for full method description.
 **/
mpe_Error mpeos_hnServerGetFrameRateInTrickMode(
        mpe_HnStreamContentLocation contentLocation, void * contentDescription,
        char * profileIDStr, char * mimeTypeStr, mpe_hnContentTransformation* transformation,
        float playspeedRate, int32_t * framesPerSec)
{
    mpe_Error ret = MPE_EINVAL;

    int i = 0;

    char *actualProfileId = getActualDLNAProfileID(profileIDStr);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "mpeos_hnServerGetFrameRateInTrickMode() - profileIDStr:%s actualProfileId:%s mimeTypeStr:%s \n",
            profileIDStr, actualProfileId, mimeTypeStr);

    if(transformation != NULL && transformation->id != 0)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_HN, "mpeos_hnServerGetFrameRateInTrickMode() - profile:%s\n",
                                             transformation->transformedProfile);
    }

    MPE_UNUSED_PARAM(contentDescription);

    for (i = 0; i < g_serverProfilesCnt && ret == MPE_EINVAL; i++)
    {
        if (strncmp(mimeTypeStr, g_serverProfiles[i].mimeType,
                    MPE_HN_MAX_MIME_TYPE_STR_SIZE) == 0 &&
            g_serverProfiles[i].contentLocationType == contentLocation)
        {
            // Sanity check
            if (g_serverProfiles[i].frameRateInTrickModeCnt != g_serverProfiles[i].playspeedsCnt)
            {
                ret = MPE_ENODATA;
            }
            else if (playspeedRate == 1.0)
            {
                // Handle special case of 1x, which cannot be listed in the array
                // due to DLNA requirement [7.3.35].
                *framesPerSec = 30;
                ret = MPE_SUCCESS;
            }
            else
            {
                int j = 0;
                for (j = 0; j < g_serverProfiles[i].frameRateInTrickModeCnt; j++)
                {
                    if (g_serverProfiles[i].playspeeds[j] == playspeedRate)
                    {
                        *framesPerSec = g_serverProfiles[i].frameRateInTrickMode[j];
                        ret = MPE_SUCCESS;
                        break;
                    }
                }
            }
        }
    }

    return ret;
}



// Local functions only below.

#define STRLEN_DTCP_ 5 // strlen("DTCP_");

static char* getActualDLNAProfileID(char * profileIdStr)
{
    char* ret = profileIdStr;

    if (strlen(profileIdStr) >= STRLEN_DTCP_ &&
        profileIdStr[0] == 'D' &&
        profileIdStr[1] == 'T' &&
        profileIdStr[2] == 'C' &&
        profileIdStr[3] == 'P' &&
        profileIdStr[4] == '_')
    {
        ret = profileIdStr + STRLEN_DTCP_;
    }

    return ret;
}
