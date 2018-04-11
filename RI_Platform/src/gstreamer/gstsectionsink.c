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

/**
 * SECTION:element-sectionsink
 *
 * FIXME:Describe sectionsink here.
 *
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <gst/gst.h>
#include <stdlib.h>

#include "gstsectionsink.h"

GST_DEBUG_CATEGORY_STATIC( gst_sectionsink_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_sectionsink_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    SIGNAL_SECTION_AVAILABLE, LAST_SIGNAL
};

enum
{
    PROP_0,
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
        GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS("application/x-sections;"
            "application/x-matched-sections"));
static char *caps_formats[] =
{ "application/x-sections", "application/x-matched-sections" };

/*lint -e(123) -esym(551,parent_class)*/GST_BOILERPLATE (GstSectionSink, gst_sectionsink, GstBaseSink, GST_TYPE_BASE_SINK)

static void gst_sectionsink_set_property(GObject * object, guint prop_id,
        const GValue* value, GParamSpec* pspec);
static void gst_sectionsink_get_property(GObject* object, guint prop_id,
        GValue* value, GParamSpec* pspec);

static gboolean gst_sectionsink_set_caps(GstBaseSink* sink, GstCaps* caps);
static GstCaps* gst_sectionsink_get_caps(GstBaseSink* sink);
static GstFlowReturn gst_sectionsink_render(GstBaseSink* sink, GstBuffer* buf);

static guint gst_sectionsink_signals[LAST_SIGNAL] =
{ 0 };

/* Signal marshal function */
#define g_marshal_value_peek_uint(v)     g_value_get_uint(v)
#define g_marshal_value_peek_pointer(v)  g_value_get_pointer(v)
/* VOID:UINT,UINT,POINTER,UINT */
void
g_marshal_VOID__UINT_POINTER (GClosure *closure,
        GValue *return_value G_GNUC_UNUSED,
        guint n_param_values,
        const GValue *param_values,
        gpointer invocation_hint G_GNUC_UNUSED,
        gpointer marshal_data)
{
    typedef void (*GMarshalFunc_VOID__UINT_POINTER) (gpointer data1,
            guint arg_1,
            gpointer arg_2,
            gpointer data2);
    register GMarshalFunc_VOID__UINT_POINTER callback;
    register GCClosure *cc = (GCClosure*) closure;
    register gpointer data1, data2;

    g_return_if_fail (n_param_values == 3);

    if (G_CCLOSURE_SWAP_DATA (closure))
    {
        data1 = closure->data;
        data2 = g_value_peek_pointer (param_values + 0);
    }
    else
    {
        data1 = g_value_peek_pointer (param_values + 0);
        data2 = closure->data;
    }

    callback = /*lint -e(611)*/(GMarshalFunc_VOID__UINT_POINTER)(marshal_data ? marshal_data : cc->callback);

    callback (data1,
            g_marshal_value_peek_uint (param_values + 1),
            g_marshal_value_peek_pointer (param_values + 2),
            data2);
}

/* GObject vmethod implementations */

static void gst_sectionsink_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(
            element_class,
            "MPEOS Section Sink",
            "Sink/Sections/Generic",
            "An MPEG-2 section sink with APIs for dispatching sections to application",
            "Greg Rutz <<grutz@enabletv.com>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the sectionsink's class */
static void gst_sectionsink_class_init(GstSectionSinkClass * klass)
{
    GObjectClass *gobject_class;
    GstBaseSinkClass *gstbase_sink_class;

    gobject_class = G_OBJECT_CLASS(klass);
    gstbase_sink_class = GST_BASE_SINK_CLASS(klass);

    gobject_class->set_property = gst_sectionsink_set_property;
    gobject_class->get_property = gst_sectionsink_get_property;

    /* Signals */
    gst_sectionsink_signals[SIGNAL_SECTION_AVAILABLE] = g_signal_new(
            "section_available", G_TYPE_FROM_CLASS(klass), G_SIGNAL_RUN_LAST,
            0, NULL, NULL, g_marshal_VOID__UINT_POINTER, G_TYPE_NONE, 2,
            G_TYPE_UINT, G_TYPE_POINTER);

    /* Virtual method implementations */
    gstbase_sink_class->render = gst_sectionsink_render;
    gstbase_sink_class->set_caps = gst_sectionsink_set_caps;
    gstbase_sink_class->get_caps = gst_sectionsink_get_caps;

    GST_DEBUG_CATEGORY_INIT(gst_sectionsink_debug, "sectionsink", 0,
            "GST Section Sink");
}

/*
 * initialize instance structure
 */
static void gst_sectionsink_init(GstSectionSink* section_sink,
        GstSectionSinkClass* gclass)
{
    section_sink->data_format = SS_DF_INVALID;
    // sink (input) pad
    // section_sink->sinkpad = gst_pad_new_from_static_template (&sink_factory, "sink");
    // gst_pad_set_chain_function (section_sink->sinkpad, GST_DEBUG_FUNCPTR(gst_es_assembler_chain));
    // gst_pad_use_fixed_caps (section_sink->sinkpad);
    // gst_pad_set_setcaps_function (section_sink->sinkpad,
    //                              GST_DEBUG_FUNCPTR(gst_sectionsink_set_pad_caps));
    // gst_pad_set_getcaps_function (section_sink->sinkpad,
    //                              GST_DEBUG_FUNCPTR(gst_pad_proxy_getcaps));
    // gst_pad_set_event_function(filter->sinkpad, gst_es_assembler_event);

}

static void gst_sectionsink_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    //GstSectionSink *filter = GST_SECTIONSINK (object);

    switch (prop_id)
    {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_sectionsink_get_property(GObject* object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    //GstSectionSink *filter = GST_SECTIONSINK(object);

    switch (prop_id)
    {
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/* this function handles the link with other elements */
static gboolean gst_sectionsink_set_caps(GstBaseSink* sink, GstCaps* caps)
{
    GstCaps *pcaps;
    gboolean found = FALSE;
    int i = 0;
    GstSectionSink *ss = GST_SECTIONSINK(sink);

    /* Work out what format was negotiated so we 
     can process data correctly.
     Iterate through all supported caps */
    while ((i < (sizeof(caps_formats) / sizeof(char *))) && !found)
    {
        pcaps = gst_caps_new_simple(caps_formats[i], NULL);

        /* if requested caps matches, adopt new caps */
        if (gst_caps_is_equal(pcaps, caps))
        {
            ss->data_format = (SS_DATA_FORMAT) i;
            found = TRUE;
        } /* endif caps matched */

        gst_caps_unref(pcaps);
        i++;
    } /* endwhile iterating through supported caps */

    if (!found)
    {
        ss->data_format = SS_DF_INVALID;
    }

    return found;
}

/* this function returns the caps on this sink */
static GstCaps*
gst_sectionsink_get_caps(GstBaseSink* sink)
{
    GstCaps *pcaps;

    pcaps = gst_caps_copy(
            gst_pad_get_pad_template_caps(GST_BASE_SINK_PAD(sink)));
    return pcaps;
}

static guint8* gst_sectionsink_parse_uint32(guint8* inBuf, guint32* out_int)
{
    guint32 val = 0;

    val |= (inBuf[0] << 24) & 0xFF000000;
    val |= (inBuf[1] << 16) & 0xFF0000;
    val |= (inBuf[2] << 8) & 0xFF00;
    val |= inBuf[3] & 0xFF;
    inBuf += 4;

    *out_int = val;
    return inBuf;
}

static guint8* gst_sectionsink_parse_uint16(guint8* inBuf, guint16* out_int)
{
    guint16 val = 0;

    val |= (inBuf[0] << 8) & 0xFF00;
    val |= inBuf[1] & 0xFF;
    inBuf += 2;

    *out_int = val;
    return inBuf;
}

/* render function
 * this function does the actual processing
 */
static GstFlowReturn gst_sectionsink_render(GstBaseSink* sink, GstBuffer* buf)
{
    GstSectionSink *section_sink = GST_SECTIONSINK(sink);
    guint8 *ptr;

    ptr = GST_BUFFER_DATA(buf);

    if (section_sink->data_format == SS_DF_MATCHED_SECTIONS)
    {
        /**
         * Process section header and read list of filter IDs that caused this section
         * to be matched.  Section syntax is:
         *
         * matched_section_data() {
         *     PID                            16bit                uimsbf
         *     section_offset                 16bit                uimsbf
         *     section_length                 16bit                uimsbf
         *     filter_offset                  16bit                uimsbf
         *     filter_length                  16bit                uimsbf
         *     <packet_start + section_offset>
         *     for (i=0; i<section_length; i++) {
         *         section_data_byte           8bit                bslbf
         *     }
         *     <packet_start + filter_offset>
         *     for (i=0; i<filter_length; i++) {
         *         filter_id                  32bit                uimsbf
         *     }
         * }
         */

        guint8 *filter_id_ptr;
        guint16 section_offset, section_length, filter_offset, filter_length;
        guint32* filter_ids;
        int i;

        // Skip PID -- don't need it
        ptr += 2;

        // Parse section info fields
        ptr = gst_sectionsink_parse_uint16(ptr, &section_offset);
        ptr = gst_sectionsink_parse_uint16(ptr, &section_length);
        ptr = gst_sectionsink_parse_uint16(ptr, &filter_offset);
        ptr = gst_sectionsink_parse_uint16(ptr, &filter_length);

        (void) ptr;

        // Allocate our array of filter IDs
        filter_ids = (guint32*) g_try_malloc(sizeof(guint32) * filter_length);

        if (NULL == filter_ids)
        {
            GST_ERROR_OBJECT(section_sink,
                            "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
            exit(-1);
        }

        // Iterate over filter IDs associated with this section data
        filter_id_ptr = GST_BUFFER_DATA(buf) + filter_offset;
        for (i = 0; i < filter_length; i++)
        {
            // Create a sub-buffer that contains only the section data
            GstBuffer* section = gst_buffer_create_sub(buf, section_offset,
                    section_length);

            // Parse the filter_id
            filter_id_ptr = gst_sectionsink_parse_uint32(filter_id_ptr,
                    &filter_ids[i]);

            GST_DEBUG_OBJECT(sink, "Created sub-buffer (%p) from %p", section,
                    buf);

            // Send a signal indicating the arrival of the section
            g_signal_emit(G_OBJECT(section_sink),
                    gst_sectionsink_signals[SIGNAL_SECTION_AVAILABLE], 0,
                    filter_ids[i], section);
        }

        g_free(filter_ids);

    }
    else
    {
        if (section_sink->data_format == SS_DF_SECTIONS)
        {
            /*
             * Process section header and read list of filter IDs that caused this section
             * to be matched.  Section syntax is:
             *
             * PID                                                16 uimsbf
             * private_section()
             * {
             *   table_id                                          8 uimsbf
             *   section_syntax_indicator                          1 bslbf
             *   private_indicator                                 1 bslbf
             *   reserved                                          2 bslbf
             *   private_section_length                           12 uimsbf
             *   if (section_syntax_indicator = = '0')
             *   {
             *     for (i = 0; i < N; i++)
             *     {
             *       private_data_byte                             8 bslbf
             *     }
             *   }
             *   else
             *   {
             *     table_id_extension                             16 uimsbf
             *     reserved                                        2 bslbf
             *     version_number                                  5 uimsbf
             *     current_next_indicator                          1 bslbf
             *     section_number                                  8 uimsbf
             *     last_section_number                             8 uimsbf
             *     for (i = 0; i < private_section_length-9; i++)
             *     {
             *       private_data_byte                             8 bslbf
             *     }
             *     CRC_32                                         32 rpchof
             *   }
             * }
             **/

            guint16 section_length;

            // Skip PID and Table ID - need to get section length
            ptr += 3;

            // Get section length
            ptr = gst_sectionsink_parse_uint16(ptr, &section_length);
            (void) ptr;

            /* top 2 bits are not length */
            section_length &= 0x3FF;

            // Create a sub-buffer that contains only the section data
            // Re-include the table ID and 2 length bytes
            GstBuffer* section = gst_buffer_create_sub(buf, 2, section_length
                    + 3);

            GST_DEBUG_OBJECT(sink, "Created sub-buffer (%p) from %p", section,
                    buf);

            // Send a signal indicating the arrival of the section
            g_signal_emit(G_OBJECT(section_sink),
                    gst_sectionsink_signals[SIGNAL_SECTION_AVAILABLE], 0, -1,
                    section);

        }
        else
        {
            GST_WARNING_OBJECT(section_sink,
                    "Buffer received unknown data format");
            gst_buffer_unref(buf);
        }
    }

    /* just push out the incoming buffer without touching it */
    return GST_FLOW_OK;
}
