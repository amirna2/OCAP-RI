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

#ifdef HAVE_CONFIG_H
#  include "config.h"
#endif

#include <sys/time.h>
#include <gst/gst.h>

#include "gstelements.h"
#include "gstsectionassembler.h"
#include "gstsectionfilter.h"
#include "gstpassthru.h"
#include "gstsectionsink.h"
#include "gstdisplay.h"
#include "gsttransportsync.h"
#include "gstesassembler.h"
#include "gstmpegdecoder.h"
#include "gstpidfilter.h"
#include "gstindexingfilesink.h"
#include "gsttrickplayfilesrc.h"
#include "gstnetsink.h"
#include "gstnetsrc.h"
#include "gstpacedfilesrc.h"
#include "gstsptsfilesrc.h"
#include "gstsptsassembler.h"

#include <libavcodec/avcodec.h>

struct _elements_entry
{
    const gchar *name;
    guint rank;
    GType (*type)(void);
};

static struct _elements_entry _elements[] =
{
{ "display", GST_RANK_NONE, gst_display_get_type },
{ "sectionassembler", GST_RANK_NONE, gst_section_assembler_get_type },
{ "sectionfilter", GST_RANK_NONE, gst_section_filter_get_type },
{ "passthru", GST_RANK_NONE, gst_pass_thru_get_type },
{ "sectionsink", GST_RANK_NONE, gst_sectionsink_get_type },
{ "transportsync", GST_RANK_NONE, gst_transport_sync_get_type },
{ "esassembler", GST_RANK_NONE, gst_es_assembler_get_type },
{ "sptsassembler", GST_RANK_NONE, gst_spts_assembler_get_type },
{ "mpegdecoder", GST_RANK_NONE, gst_mpeg_decoder_get_type },
{ "pidfilter", GST_RANK_NONE, gst_pid_filter_get_type },
{ "indexingfilesink", GST_RANK_NONE, gst_indexing_filesink_get_type },
{ "trickplayfilesrc", GST_RANK_NONE, gst_trick_play_file_src_get_type },
{ "netsink", GST_RANK_NONE, gst_net_sink_get_type },
{ "netsrc", GST_RANK_NONE, gst_net_src_get_type },
{ "pacedfilesrc", GST_RANK_NONE, gst_paced_file_src_get_type },
{ "sptsfilesrc", GST_RANK_NONE, gst_spts_file_src_get_type },
{ NULL, 0 }, };

static gboolean plugin_init(GstPlugin * plugin)
{
    struct _elements_entry *my_elements = _elements;

    GstClock* gstSystemClk = gst_system_clock_obtain();
    g_object_set(gstSystemClk, "clock-type", GST_CLOCK_TYPE_REALTIME, NULL);

    // Uncomment the following if you need to use the CableLabs GStreamer library
    // in standalone mode (i.e. gst-launch, gst-inspect) mode
    //gst_debug_add_log_function (gst_debug_log_default, NULL);

    // Non thread-safe initialization of FFMPEG library...
    avcodec_init();
    avcodec_register_all();

    while ((*my_elements).name)
    {
        if (!gst_element_register(plugin, (*my_elements).name,
                (*my_elements).rank, ((*my_elements).type)()))
            return FALSE;
        my_elements++;
    }

    // In the prior version of the GStreamer library in use (0.10.22) the
    // GstSystemClk was based on "real time".  When we upgraded to the
    // latest library (0.10.35) we discovered the system clock was now
    // monotonic from instantiation.  To restore the "real time" nature of
    // the system clock, we calculate the sysseconds since epoch along with
    // systicks, shift and merge the two together, and set the offset within
    // the GStreamer library such that all further accesses are real time.
    GstClockTime tmp, internal, external, rate_num, rate_denom;
    GstClockType clockType;
    struct timeval tv;

    g_object_get(gstSystemClk, "clock-type", &clockType, NULL);
    GST_INFO("GST clockType: %s", clockType == 0? "REALTIME" : "MONOTONIC");
    gst_clock_get_calibration(gstSystemClk,
                              &internal, &external, &rate_num, &rate_denom);
    GST_INFO("GST int: %llu, ext: %llu, num: %llu, denom: %llu",
                              internal, external, rate_num, rate_denom);
    internal = gst_clock_get_time(gstSystemClk);
    GST_INFO("GST clockTime: %llu", internal);
    gettimeofday(&tv, NULL);
    external = tv.tv_sec;
    external *= 1000000000;
    tmp = tv.tv_usec;
    tmp *= 1000;
    external += tmp;
    GST_INFO("GST external: %llu", external);
    gst_clock_set_calibration(gstSystemClk,
                              internal, external, rate_num, rate_denom);
    GST_INFO("GST clockTime: %llu", gst_clock_get_time(gstSystemClk));
    return TRUE;
}

GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
        GST_VERSION_MINOR,
        "goodelements",
        "Section Filter GStreamer elements",
        plugin_init,
        VERSION,
        GST_LICENSE,
        GST_PACKAGE_NAME,
        GST_PACKAGE_ORIGIN)
