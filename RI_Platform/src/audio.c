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


#include "audio.h"
#include "gst_utils.h"
#include <ri_log.h>
#include <ri_types.h>
#include <test_interface.h>

#define RILOG_CATEGORY riAudioCat
log4c_category_t* riAudioCat = NULL;

/**
 * Audio information
 */
struct ri_audio_data_s
{
    GstElement* gst_audiosrc;
    GstElement* gst_audiosink;
    GstCaps *gst_caps;
    int volume;
    gboolean muted;
};

void test_audio(ri_audio_t* audio);
static GstCaps *get_caps(ri_audio_t* object);
static gboolean set_caps(ri_audio_t* object, GstCaps *);
static void play_audio(ri_audio_t* object, unsigned char *buffer);
static void set_volume(ri_audio_t* object, int volume);
static void mute_audio(ri_audio_t* object);

#define STANDALONE

#ifdef STANDALONE
void gst_audio_play(GstElement *audio, unsigned char *buffer)
{
    RILOG_WARN("%s UNIMPLEMENTED!\n", __FUNCTION__);
}
void gst_audio_set_volume(GstElement *audio, int volume)
{
    RILOG_WARN("%s UNIMPLEMENTED!\n", __FUNCTION__);
}
#endif

/**
 * Creates the audio plugin.
 *
 * @param audiosink  audio plugin which serves as the sink element for audio.
 * @return  audio context
 */
ri_audio_t* create_audio(GstElement* audiosink)
{
    riAudioCat = log4c_category_get("RI.Audio");
    RILOG_TRACE("%s -- Entry, audiosink = (%p)\n", __FUNCTION__, audiosink);

    // Allocate object memory
    ri_audio_t* audio = g_try_malloc0(sizeof(ri_audio_t));

    if (NULL == audio)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Associate the methods defined in the plugin
    audio->get_caps = get_caps;
    audio->set_caps = set_caps;
    audio->set_volume = set_volume;
    audio->play = play_audio;
    audio->mute = mute_audio;

    // Allocate data memory
    audio->ri_audio_data = g_try_malloc0(sizeof(ri_audio_data_t));

    if (NULL == audio->ri_audio_data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Init elements in the data structure
    audio->ri_audio_data->gst_audiosink = audiosink;
    audio->ri_audio_data->gst_caps = NULL;
    audio->ri_audio_data->volume = 1;
    audio->ri_audio_data->muted = FALSE;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return audio;
}

/**
 * Create a pipeline which has a test audio source
 */
void test_audio(ri_audio_t* audio)
{
    RILOG_TRACE("%s -- Entry, audio = (%p)\n", __FUNCTION__, audio);

    // For test pipeline, retrieve test audio source
    GstElement *audiosrc = gst_load_element("testaudiosrc", "test-source");
    GstElement *audiosink = audio->ri_audio_data->gst_audiosink;

    g_object_set(G_OBJECT(audio), "volume", 1, NULL);
    audio->ri_audio_data->gst_audiosrc = audiosrc;

    // Create a test pipeline
    GstElement* pipeline = gst_pipeline_new("audio pipeline");
    gst_bin_add_many(GST_BIN(pipeline), audiosrc, audiosink, NULL);
    (void) gst_element_link_many(audiosrc, audiosink, NULL);

    // Start up the test audio pipeline
    (void) gst_element_set_state(pipeline, GST_STATE_PLAYING);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * gets the audio capabilities
 *
 * @param object  audio object which holds a reference to the audio plugin
 */
static GstCaps* get_caps(ri_audio_t* object)
{
    GstCaps* retVal = NULL; // assume failure

    RILOG_TRACE("%s -- Entry, object = (%p)\n", __FUNCTION__, object);

    if (NULL != object)
    {
        GstElement* audio = object->ri_audio_data->gst_audiosink;

        if (NULL != audio)
        {
            // return caps here...
            retVal = object->ri_audio_data->gst_caps;
        }
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

/**
 * sets the audio capabilities
 *
 * @param object  audio object which holds a reference to the audio plugin
 * @param caps    The capabilities to set
 */
static gboolean set_caps(ri_audio_t* object, GstCaps *caps)
{
    gboolean retVal = FALSE; // assume failure
    RILOG_TRACE("%s -- Entry, object = (%p), caps = (%p)\n", __FUNCTION__,
            object, caps);

    if ((NULL != object) && (NULL != caps))
    {
        GstElement* audio = object->ri_audio_data->gst_audiosink;

        if (NULL != audio)
        {
            // set caps here...
            object->ri_audio_data->gst_caps = caps;
            retVal = TRUE;
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retVal;
}

/**
 * Plays audio from stream or provided buffer or restore volume after a mute
 *
 * @param object  audio object which holds a reference to the audio plugin
 * @param buffer  sound buffer to play, null to play from stream
 */
static void play_audio(ri_audio_t* object, unsigned char *buffer)
{
    RILOG_TRACE("%s -- Entry, object = (%p)\n", __FUNCTION__, object);

    if (NULL != object)
    {
        GstElement* audio = object->ri_audio_data->gst_audiosink;

        if (NULL != audio)
        {
            if (NULL == buffer)
            {
                // a 'play' when muted means 'resume'
                if (object->ri_audio_data->muted)
                {
                    RILOG_INFO("%s: is simply restoring volume!\n",
                            __FUNCTION__);
                    set_volume(object, object->ri_audio_data->volume);
                }
                else
                {
                    RILOG_INFO("%s: sound from stream(%p)\n", __FUNCTION__,
                            buffer);
                    gst_audio_play(audio, NULL);
                }
            }
            else
            {
                RILOG_INFO("%s: sound from buffer(%p)\n", __FUNCTION__, buffer);
                gst_audio_play(audio, buffer);
            }
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * sets the audio volume
 *
 * @param object  audio object which holds a reference to the audio plugin
 */
static void set_volume(ri_audio_t* object, int volume)
{
    RILOG_TRACE("%s -- Entry, object = (%p), volume = (%d)\n", __FUNCTION__,
            object, volume);

    if (NULL != object)
    {
        GstElement* audio = object->ri_audio_data->gst_audiosink;
        object->ri_audio_data->volume = volume;
        object->ri_audio_data->muted = FALSE;

        if (NULL != audio)
        {
            gst_audio_set_volume(audio, volume);
            RILOG_INFO("%s: set volume to %d & mute to FALSE\n", __FUNCTION__,
                    volume);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

/**
 * mutes the audio volume
 *
 * @param object  audio object which holds a reference to the audio plugin
 */
static void mute_audio(ri_audio_t* object)
{
    RILOG_TRACE("%s -- Entry, object = (%p)\n", __FUNCTION__, object);

    if (NULL != object)
    {
        GstElement* audio = object->ri_audio_data->gst_audiosink;
        object->ri_audio_data->muted = TRUE;

        if (NULL != audio)
        {
            gst_audio_set_volume(audio, 0);
            RILOG_INFO("%s: set volume to 0 & mute to TRUE\n", __FUNCTION__);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}
