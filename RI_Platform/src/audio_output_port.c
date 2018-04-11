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

// Include system header files.
#include <stdlib.h>
#include <glib.h>

// Include RI Platform header files.
#include <ri_log.h>
#include <ri_config.h>
#include "audio_output_port.h"

// Back Panel logging.
#define RILOG_CATEGORY riAudioOutputPortCat
log4c_category_t *riAudioOutputPortCat = NULL;

// AudioOutputPort object information.
struct ri_audioOutputPortData_s
{
    char* id;
    ri_ap_compression compression;
    float gain;
    ri_ap_encoding encoding;
    float level;
    float optimalLevel;
    float maxDb;
    float minDb;
    ri_ap_stereo_mode stereoMode;
    ri_ap_compression* supportedCompressions;
    uint8_t supportedCompressionsCount;
    ri_ap_encoding* supportedEncodings;
    uint8_t supportedEncodingsCount;
    ri_ap_stereo_mode* supportedStereoModes;
    uint8_t supportedStereoModesCount;
    ri_bool loopThru;
    ri_bool muted;
    ri_bool loopThruSupported;
};

// AudioOutputPort forward declarations
char* ap_getId(ri_audioOutputPort_t*);
ri_ap_compression ap_getCompression(ri_audioOutputPort_t*);
void ap_setCompression(ri_audioOutputPort_t*, ri_ap_compression);
float ap_getGain(ri_audioOutputPort_t*);
void ap_setGain(ri_audioOutputPort_t*, float);
ri_ap_encoding ap_getEncoding(ri_audioOutputPort_t*);
void ap_setEncoding(ri_audioOutputPort_t*, ri_ap_encoding);
float ap_getLevel(ri_audioOutputPort_t*);
void ap_setLevel(ri_audioOutputPort_t*, float);
float ap_getOptimalLevel(ri_audioOutputPort_t*);
float ap_getMaxDb(ri_audioOutputPort_t*);
float ap_getMinDb(ri_audioOutputPort_t*);
ri_ap_stereo_mode ap_getStereoMode(ri_audioOutputPort_t*);
void ap_setStereoMode(ri_audioOutputPort_t*, ri_ap_stereo_mode);
ri_ap_compression* ap_getSupportedCompressions(ri_audioOutputPort_t*);
uint8_t ap_getSupportedCompressionsCount(ri_audioOutputPort_t*);
ri_ap_encoding* ap_getSupportedEncodings(ri_audioOutputPort_t*);
uint8_t ap_getSupportedEncodingsCount(ri_audioOutputPort_t*);
ri_ap_stereo_mode* ap_getSupportedStereoModes(ri_audioOutputPort_t*);
uint8_t ap_getSupportedStereoModesCount(ri_audioOutputPort_t*);
ri_bool ap_isLoopThru(ri_audioOutputPort_t*);
void ap_setLoopThru(ri_audioOutputPort_t*, ri_bool);
ri_bool ap_isMuted(ri_audioOutputPort_t*);
void ap_setMuted(ri_audioOutputPort_t*, ri_bool);
ri_bool ap_isLoopThruSupported(ri_audioOutputPort_t*);

//Fallback default values
static int d_compression = 0;
static float d_gain = 70.0;
static int d_encoding = 3;
static float d_level = 0.6;
static float d_optimalLevel = 0.8;
static float d_maxDb = 90.0;
static float d_minDb = 0.0;
static int d_stereoMode = 3;
static int d_supportedCompressions[] =
{ 0, 1, 2, 3 };
static int d_supportedEncodings[] =
{ 0, 1, 2, 3 };
static int d_supportedStereoModes[] =
{ 0, 1, 2, 3 };
static ri_bool d_loopThru = FALSE;
static ri_bool d_muted = FALSE;
static ri_bool d_loopThruSupported = FALSE;

//Convenience function for retrieving values from configuration file
static char *apCfgNameBase = "RI.Platform.backpanel.audio_output_port.";
static char* getConfigOrDefault(char* configItem, int i)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    char cvn[256];
    char *cv;
    sprintf(cvn, "%s%d.%s", apCfgNameBase, i, configItem);
    cv = ricfg_getValue("RIPlatform", cvn);
    if (cv == NULL)
    {
        sprintf(cvn, "%sx.%s", apCfgNameBase, configItem);
        cv = ricfg_getValue("RIPlatform", cvn);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return cv;
}

//Convenience function for dealing with integers in the configuration file
// returns converted value or defaultValue
static int getAndConvertInteger(char* configName, int portIndex, int lowLimit,
        int highLimit, int defaultValue)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    int returnThis = defaultValue;
    char* cv = getConfigOrDefault(configName, portIndex);
    char* errorCheck_ptr;
    if (NULL != cv)
    {
        long converted_l = strtol(cv, &errorCheck_ptr, 10);
        if (*errorCheck_ptr != '\0' || errorCheck_ptr == cv)
        {
            RILOG_ERROR("Invalid integer value in configuration for %s: %s\n",
                    configName, cv);
        }
        else
        {
            int converted = (int) converted_l;
            if (converted >= lowLimit && converted <= highLimit)
            {
                returnThis = converted;
            }
            else
            {
                RILOG_ERROR("Configuration value for %s is out of range!\n",
                        configName);
            }
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return returnThis;
}

ri_audioOutputPort_t* create_audio_output_port(int i)
{
    char cvn[256];
    char *cv;
    ri_audioOutputPort_t* ap = NULL;

    riAudioOutputPortCat = log4c_category_get("RI.BackPanel.audioOutputPort");
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    sprintf(cvn, "%s%d.%s", apCfgNameBase, i, "id");
    cv = ricfg_getValue("RIPlatform", cvn);
    if (NULL != cv)
    {
        ap = g_try_malloc0(sizeof(ri_audioOutputPort_t));

        if (NULL != ap)
        {
            ap->m_data = g_try_malloc0(sizeof(ri_audioOutputPortData_t));

            if (NULL != ap->m_data)
            {
                ap->getId = ap_getId;
                ap->getCompression = ap_getCompression;
                ap->setCompression = ap_setCompression;
                ap->getGain = ap_getGain;
                ap->setGain = ap_setGain;
                ap->getEncoding = ap_getEncoding;
                ap->setEncoding = ap_setEncoding;
                ap->getLevel = ap_getLevel;
                ap->setLevel = ap_setLevel;
                ap->getOptimalLevel = ap_getOptimalLevel;
                ap->getMaxDb = ap_getMaxDb;
                ap->getMinDb = ap_getMinDb;
                ap->getStereoMode = ap_getStereoMode;
                ap->setStereoMode = ap_setStereoMode;
                ap->getSupportedCompressions = ap_getSupportedCompressions;
                ap->getSupportedCompressionsCount
                        = ap_getSupportedCompressionsCount;
                ap->getSupportedEncodings = ap_getSupportedEncodings;
                ap->getSupportedEncodingsCount = ap_getSupportedEncodingsCount;
                ap->getSupportedStereoModes = ap_getSupportedStereoModes;
                ap->getSupportedStereoModesCount
                        = ap_getSupportedStereoModesCount;
                ap->isLoopThru = ap_isLoopThru;
                ap->setLoopThru = ap_setLoopThru;
                ap->isMuted = ap_isMuted;
                ap->setMuted = ap_setMuted;
                ap->isLoopThruSupported = ap_isLoopThruSupported;
                ap->m_data->id = cv;

                ap->m_data->compression = getAndConvertInteger("compression",
                        i, RI_AP_COMPRESSION_NONE, RI_AP_COMPRESSION_HEAVY,
                        d_compression);

                cv = getConfigOrDefault("gain", i);
                if (NULL != cv)
                {
                    ap->m_data->gain = g_ascii_strtod(cv, NULL);
                }
                else
                {
                    ap->m_data->gain = d_gain;
                }

                ap->m_data->encoding = getAndConvertInteger("encoding", i,
                        RI_AP_ENCODING_NONE, RI_AP_ENCODING_AC3, d_encoding);

                cv = getConfigOrDefault("level", i);
                if (NULL != cv)
                {
                    float level = g_ascii_strtod(cv, NULL);
                    if (level >= 0.0 && level <= 1.0)
                    {
                        ap->m_data->level = level;
                    }
                }
                else
                {
                    ap->m_data->level = d_level;
                }

                cv = getConfigOrDefault("optimalLevel", i);
                if (NULL != cv)
                {
                    float optimalLevel = g_ascii_strtod(cv, NULL);
                    if (optimalLevel >= 0.0 && optimalLevel <= 1.0)
                    {
                        ap->m_data->optimalLevel = optimalLevel;
                    }
                }
                else
                {
                    ap->m_data->optimalLevel = d_optimalLevel;
                }

                cv = getConfigOrDefault("maxDb", i);
                if (NULL != cv)
                {
                    ap->m_data->maxDb = g_ascii_strtod(cv, NULL);
                }
                else
                {
                    ap->m_data->maxDb = d_maxDb;
                }

                cv = getConfigOrDefault("minDb", i);
                if (NULL != cv)
                {
                    ap->m_data->minDb = g_ascii_strtod(cv, NULL);
                }
                else
                {
                    ap->m_data->minDb = d_minDb;
                }

                ap->m_data->stereoMode = getAndConvertInteger("stereoMode", i,
                        RI_AP_STEREO_MODE_MONO, RI_AP_STEREO_MODE_SURROUND,
                        d_stereoMode);

                int maxIntEntries = 4;
                int intArray[maxIntEntries];
                cv = getConfigOrDefault("supportedCompressions", i);
                if (NULL != cv)
                {
                    char* strInt = strtok(cv, ",");
                    int n = 0;
                    while (NULL != strInt)
                    {
                        intArray[n] = atoi(strInt);
                        n++;
                        if (n > maxIntEntries - 1)
                        {
                            strInt = NULL;
                        }
                        else
                        {
                            strInt = strtok(NULL, ",");
                        }
                    }
                    ap->m_data->supportedCompressionsCount = n;
                    ap->m_data->supportedCompressions =
                            g_try_malloc(n * sizeof(int));

                    if (NULL == ap->m_data->supportedCompressions)
                    {
                        RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
                    }

                    int j;
                    for (j = 0; j < n; j++)
                    {
                        ap->m_data->supportedCompressions[j] = intArray[j];
                    }
                }
                else
                {
                    ap->m_data->supportedCompressionsCount
                            = sizeof(d_supportedCompressions) / sizeof(int);
                    ap->m_data->supportedCompressions
                            = (ri_ap_compression*) d_supportedCompressions;
                }

                cv = getConfigOrDefault("supportedEncodings", i);
                if (NULL != cv)
                {
                    char* strInt = strtok(cv, ",");
                    int n = 0;
                    while (NULL != strInt)
                    {
                        intArray[n] = atoi(strInt);
                        n++;
                        if (n > maxIntEntries - 1)
                        {
                            strInt = NULL;
                        }
                        else
                        {
                            strInt = strtok(NULL, ",");
                        }
                    }
                    ap->m_data->supportedEncodingsCount = n;
                    ap->m_data->supportedEncodings =
                            g_try_malloc(n * sizeof(int));

                    if (NULL == ap->m_data->supportedEncodings)
                    {
                        RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
                    }

                    int j;
                    for (j = 0; j < n; j++)
                    {
                        ap->m_data->supportedEncodings[j] = intArray[j];
                    }
                }
                else
                {
                    ap->m_data->supportedEncodingsCount
                            = sizeof(d_supportedEncodings) / sizeof(int);
                    ap->m_data->supportedEncodings
                            = (ri_ap_encoding*) d_supportedEncodings;
                }

                cv = getConfigOrDefault("supportedStereoModes", i);
                if (NULL != cv)
                {
                    char* strInt = strtok(cv, ",");
                    int n = 0;
                    while (NULL != strInt)
                    {
                        intArray[n] = atoi(strInt);
                        n++;
                        if (n > maxIntEntries - 1)
                        {
                            strInt = NULL;
                        }
                        else
                        {
                            strInt = strtok(NULL, ",");
                        }
                    }
                    ap->m_data->supportedStereoModesCount = n;
                    ap->m_data->supportedStereoModes =
                            g_try_malloc(n * sizeof(int));

                    if (NULL == ap->m_data->supportedStereoModes)
                    {
                        RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
                    }

                    int j;
                    for (j = 0; j < n; j++)
                    {
                        ap->m_data->supportedStereoModes[j] = intArray[j];
                    }
                }
                else
                {
                    ap->m_data->supportedStereoModesCount
                            = sizeof(d_supportedStereoModes) / sizeof(int);
                    ap->m_data->supportedStereoModes
                            = (ri_ap_stereo_mode*) d_supportedStereoModes;
                }

                cv = getConfigOrDefault("loopThru", i);
                if (NULL != cv)
                {
                    if (strcmp(cv, "TRUE") == 0)
                    {
                        ap->m_data->loopThru = TRUE;
                    }
                    else
                    {
                        ap->m_data->loopThru = FALSE;
                    }
                }
                else
                {
                    ap->m_data->loopThru = d_loopThru;
                }

                cv = getConfigOrDefault("muted", i);
                if (NULL != cv)
                {
                    if (strcmp(cv, "TRUE") == 0)
                    {
                        ap->m_data->muted = TRUE;
                    }
                    else
                    {
                        ap->m_data->muted = FALSE;
                    }
                }
                else
                {
                    ap->m_data->muted = d_muted;
                }

                cv = getConfigOrDefault("loopThruSupported", i);
                if (NULL != cv)
                {
                    if (strcmp(cv, "TRUE") == 0)
                    {
                        ap->m_data->loopThruSupported = TRUE;
                    }
                    else
                    {
                        ap->m_data->loopThruSupported = FALSE;
                    }
                }
                else
                {
                    ap->m_data->loopThruSupported = d_loopThruSupported;
                }
            }
            else
            {
                g_free(ap);
                ap = NULL;
            }
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return ap;
}

void destroy_audio_output_port(ri_audioOutputPort_t* ap)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    g_free(ap->m_data->supportedStereoModes);
    g_free(ap->m_data->supportedEncodings);
    g_free(ap->m_data->supportedCompressions);
    g_free(ap->m_data);
    g_free(ap);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

char* ap_getId(ri_audioOutputPort_t* ap)
{
    return ap->m_data->id;
}

ri_ap_compression ap_getCompression(ri_audioOutputPort_t* ap)
{
    return ap->m_data->compression;
}
void ap_setCompression(ri_audioOutputPort_t* ap, ri_ap_compression compression)
{
    ap->m_data->compression = compression;
}
float ap_getGain(ri_audioOutputPort_t* ap)
{
    return ap->m_data->gain;
}
void ap_setGain(ri_audioOutputPort_t* ap, float gain)
{
    ap->m_data->gain = gain;
}
ri_ap_encoding ap_getEncoding(ri_audioOutputPort_t* ap)
{
    return ap->m_data->encoding;
}
void ap_setEncoding(ri_audioOutputPort_t* ap, ri_ap_encoding encoding)
{
    ap->m_data->encoding = encoding;
}
float ap_getLevel(ri_audioOutputPort_t* ap)
{
    return ap->m_data->level;
}
void ap_setLevel(ri_audioOutputPort_t* ap, float level)
{
    ap->m_data->level = level;
}
float ap_getOptimalLevel(ri_audioOutputPort_t* ap)
{
    return ap->m_data->optimalLevel;
}
float ap_getMaxDb(ri_audioOutputPort_t* ap)
{
    return ap->m_data->maxDb;
}
float ap_getMinDb(ri_audioOutputPort_t* ap)
{
    return ap->m_data->minDb;
}
ri_ap_stereo_mode ap_getStereoMode(ri_audioOutputPort_t* ap)
{
    return ap->m_data->stereoMode;
}
void ap_setStereoMode(ri_audioOutputPort_t* ap, ri_ap_stereo_mode stereoMode)
{
    ap->m_data->stereoMode = stereoMode;
}
ri_ap_compression* ap_getSupportedCompressions(ri_audioOutputPort_t* ap)
{
    return ap->m_data->supportedCompressions;
}
uint8_t ap_getSupportedCompressionsCount(ri_audioOutputPort_t* ap)
{
    return ap->m_data->supportedCompressionsCount;
}
ri_ap_encoding* ap_getSupportedEncodings(ri_audioOutputPort_t* ap)
{
    return ap->m_data->supportedEncodings;
}
uint8_t ap_getSupportedEncodingsCount(ri_audioOutputPort_t* ap)
{
    return ap->m_data->supportedEncodingsCount;
}
ri_ap_stereo_mode* ap_getSupportedStereoModes(ri_audioOutputPort_t* ap)
{
    return ap->m_data->supportedStereoModes;
}
uint8_t ap_getSupportedStereoModesCount(ri_audioOutputPort_t* ap)
{
    return ap->m_data->supportedStereoModesCount;
}
ri_bool ap_isLoopThru(ri_audioOutputPort_t* ap)
{
    return ap->m_data->loopThru;
}
void ap_setLoopThru(ri_audioOutputPort_t* ap, ri_bool flag)
{
    ap->m_data->loopThru = flag;
}
ri_bool ap_isMuted(ri_audioOutputPort_t* ap)
{
    return ap->m_data->muted;
}
void ap_setMuted(ri_audioOutputPort_t* ap, ri_bool flag)
{
    ap->m_data->muted = flag;
}
ri_bool ap_isLoopThruSupported(ri_audioOutputPort_t* ap)
{
    return ap->m_data->loopThruSupported;
}
