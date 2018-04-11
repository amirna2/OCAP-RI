/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009 Cable Television Laboratories, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * Alternatively, the contents of this file may be used under the
 * GNU Lesser General Public License Version 2.1 (the "LGPL"), in
 * which case the following provisions apply instead of the ones
 * mentioned above:
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:element-sectionassembler
 *
 * FIXME:Describe sectionassembler here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! sectionassembler ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <gst/gst.h>

#include "gstsectionassembler.h"
#include "interfaces/isectionassembler.h"
#include "gstsectionadaptercontainer.h"

GST_DEBUG_CATEGORY_STATIC ( gst_section_assembler_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_section_assembler_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0,
    PROP_DROP_TEI_PACKETS,
    PROP_DROP_TSC_PACKETS,
    PROP_MAX_SECTION_ASSEMBLERS,
    PROP_ASSEMBLE_ON_ALL_PIDS,
    PROP_CHECK_CRC,
    PROP_TEST_INTERFACE
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
static GstStaticPadTemplate
        sink_factory =
                GST_STATIC_PAD_TEMPLATE(
                        "sink",
                        GST_PAD_SINK,
                        GST_PAD_ALWAYS,
                        GST_STATIC_CAPS(
                                "video/mpegts," "packetsize=(int)188," "systemstream=(boolean)true"));

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC, GST_PAD_ALWAYS, GST_STATIC_CAPS("application/x-sections"));

/*lint -esym(578,GstSectionAssembler)*/
GST_BOILERPLATE_WITH_INTERFACE (GstSectionAssembler, gst_section_assembler, GstElement,
        GST_TYPE_ELEMENT, GstISectionAssembler, GST_TYPE_ISECTION_ASSEMBLER, gst_isection_assembler)

// Forward declarations
static void gst_section_assembler_dispose (GObject * object);
static void gst_section_assembler_finalize(GObject * object);

static void gst_section_assembler_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_section_assembler_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

static GstFlowReturn gst_section_assembler_chain(GstPad * pad, GstBuffer * buf);
static GstFlowReturn gst_section_assembler_process_one_packet(GstPad * pad, GstBuffer * buf);

static GstSectionAdapterContainer* gst_section_assembler_enable(
        GstSectionAssembler* assembler, guint16 PID, gboolean enable);
static gpointer gst_section_assembler_test_thread(
        gpointer user_data_section_assembler);

//
//
//
// INTERNAL IMPLEMENTATION
//
//
//

/********************************************/
/**********                        **********/
/********** GObject IMPLEMENTATION **********/
/**********                        **********/
/********************************************/

static void gst_section_assembler_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "SectionAssembler",
            "FIXME:Generic", "FIXME:Generic Template Element",
            "U-PRESTOMarcin <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the sectionassembler's class */
static void gst_section_assembler_class_init(GstSectionAssemblerClass * klass)
{
    GObjectClass *gobject_class;

    GST_DEBUG_CATEGORY_INIT(gst_section_assembler_debug, "sectionassembler", 0,
            "MPEG-2 Private Section Assembler");

    gobject_class = (GObjectClass *) klass;

    gobject_class->dispose = gst_section_assembler_dispose;
    gobject_class->finalize = gst_section_assembler_finalize;

    gobject_class->set_property = gst_section_assembler_set_property;
    gobject_class->get_property = gst_section_assembler_get_property;

    g_object_class_install_property(gobject_class, PROP_DROP_TEI_PACKETS,
            g_param_spec_boolean("drop-tei", "TEI",
                    "Drop packets that have transport_error_indicator bit set",
                    DEFAULT_DROP_TEI_PACKETS, G_PARAM_READWRITE
                            | G_PARAM_CONSTRUCT));

    g_object_class_install_property(
            gobject_class,
            PROP_DROP_TSC_PACKETS,
            g_param_spec_boolean(
                    "drop-tsc",
                    "TSC",
                    "Drop packets that have transport_scrambling_control flag set",
                    DEFAULT_DROP_TSC_PACKETS, G_PARAM_READWRITE
                            | G_PARAM_CONSTRUCT));

    g_object_class_install_property(
            gobject_class,
            PROP_MAX_SECTION_ASSEMBLERS,
            g_param_spec_uint(
                    "max-assemblers",
                    "Maximum Assemblers",
                    "Specify maximum number of individual (per PID) section assemblers",
                    0, TS_MAX_PIDS, DEFAULT_MAX_SECTION_ASSEMBLERS,
                    G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

    g_object_class_install_property(gobject_class, PROP_ASSEMBLE_ON_ALL_PIDS,
            g_param_spec_boolean("assemble-all", "Assemble all packets",
                    "Assemble sections on all detected incoming PIDs",
                    DEFAULT_ASSEMBLE_ON_ALL_PIDS, G_PARAM_READWRITE
                            | G_PARAM_CONSTRUCT));

    g_object_class_install_property(gobject_class, PROP_CHECK_CRC,
            g_param_spec_boolean("check-crc", "Check CRC",
                    "Verify MPEG-2 CRC of each assembled section",
                    DEFAULT_CHECK_CRC, G_PARAM_READWRITE | G_PARAM_CONSTRUCT));

    g_object_class_install_property(gobject_class, PROP_TEST_INTERFACE,
            g_param_spec_boolean("test-interface",
                    "Test interface ISectionAssembler",
                    "Run ISectionAssembler test thread",
                    DEFAULT_TEST_INTERFACE, G_PARAM_READWRITE
                            | G_PARAM_CONSTRUCT));
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_section_assembler_init(GstSectionAssembler * filter,
        GstSectionAssemblerClass * gclass)
{
    // sink (input) pad
    filter->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    gst_pad_set_chain_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_section_assembler_chain));
    gst_pad_use_fixed_caps(filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->sinkpad);

    // section assembler fifo
    filter->adapter = gst_adapter_new();

    // source (output) pad
    filter->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    gst_pad_use_fixed_caps(filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->srcpad);

    // section assembler specific initialization
    filter->adapter_containers_lock = g_mutex_new();
    filter->adapter_containers_tree = g_tree_new(
            gst_section_adapter_container_key_compare_func);
}

static void gst_section_assembler_dispose(GObject * object)
{
    GstSectionAssembler *assembler = GST_SECTION_ASSEMBLER(object);

    assembler->test_interface = FALSE;

    if (g_tree_nnodes(assembler->adapter_containers_tree) > 0)
    {
        GQueue *container_queue = g_queue_new();

        g_mutex_lock(assembler->adapter_containers_lock);
        g_tree_foreach(assembler->adapter_containers_tree,
                gst_section_adapter_container_add_to_queue_func,
                container_queue);
        while (g_queue_is_empty(container_queue) == FALSE)
        {
            GstSectionAdapterContainer *container =
                    GST_SECTION_ADAPTER_CONTAINER(g_queue_pop_head(
                            container_queue));
            guint16 PID = GST_READ_UINT16_BE(&GST_BUFFER_DATA(
                    container->metadata)[0]);
            (void) gst_section_assembler_enable(assembler, PID, FALSE);
        }
        g_mutex_unlock(assembler->adapter_containers_lock);
    }

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, dispose, (object));
}

static void gst_section_assembler_finalize(GObject * object)
{
    GstSectionAssembler *assembler = GST_SECTION_ASSEMBLER(object);

    g_mutex_free(assembler->adapter_containers_lock);
    g_tree_destroy(assembler->adapter_containers_tree);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, finalize, (object));
}

static void gst_section_assembler_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstSectionAssembler *filter = GST_SECTION_ASSEMBLER(object);

    switch (prop_id)
    {
    case PROP_DROP_TEI_PACKETS:
        filter->drop_tei_packets = g_value_get_boolean(value);
        break;
    case PROP_DROP_TSC_PACKETS:
        filter->drop_tsc_packets = g_value_get_boolean(value);
        break;
    case PROP_MAX_SECTION_ASSEMBLERS:
        filter->max_section_assemblers = g_value_get_uint(value);
        break;
    case PROP_ASSEMBLE_ON_ALL_PIDS:
        filter->assemble_on_all_PIDs = g_value_get_boolean(value);
        break;
    case PROP_CHECK_CRC:
        filter->check_crc = g_value_get_boolean(value);
        break;
    case PROP_TEST_INTERFACE:
        filter->test_interface = g_value_get_boolean(value);
        if (filter->test_interface == TRUE)
        {
            (void) g_thread_create(gst_section_assembler_test_thread, filter,
                    TRUE, NULL);
        }
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_section_assembler_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstSectionAssembler *filter = GST_SECTION_ASSEMBLER(object);

    switch (prop_id)
    {
    case PROP_DROP_TEI_PACKETS:
        g_value_set_boolean(value, filter->drop_tei_packets);
        break;
    case PROP_DROP_TSC_PACKETS:
        g_value_set_boolean(value, filter->drop_tsc_packets);
        break;
    case PROP_MAX_SECTION_ASSEMBLERS:
        g_value_set_uint(value, filter->max_section_assemblers);
        break;
    case PROP_ASSEMBLE_ON_ALL_PIDS:
        g_value_set_boolean(value, filter->assemble_on_all_PIDs);
        break;
    case PROP_CHECK_CRC:
        g_value_set_boolean(value, filter->check_crc);
        break;
    case PROP_TEST_INTERFACE:
        g_value_set_boolean(value, filter->test_interface);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/***********************************************/
/**********                           **********/
/********** GstElement IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

/*
 * chain function
 * this function does the actual processing
 *
 *       IN:
 * ---------------
 * transport_packet()
 * {
 *   sync_byte                                                                    8 bslbf
 *   transport_error_indicator                                                    1 bslbf
 *   payload_unit_start_indicator                                                 1 bslbf
 *   transport_priority                                                           1 bslbf
 *   PID                                                                         13 uimsbf
 *   transport_scrambling_control                                                 2 bslbf
 *   adaptation_field_control                                                     2 bslbf
 *   continuity_counter                                                           4 uimsbf
 *   if(adaptation_field_control = = '10' || adaptation_field_control = = '11')
 *   {
 *     adaptation_field()
 *   }
 *   if(adaptation_field_control = = '01' || adaptation_field_control = = '11')
 *   {
 *     for (i = 0; i < N; i++)
 *     {
 *       data_byte                                                                8 bslbf
 *     }
 *   }
 * }
 *
 *       OUT:
 * ----------------
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
 *
 */
static GstFlowReturn gst_section_assembler_chain(GstPad * pad, GstBuffer * buf)
{
    GstFlowReturn ret = GST_FLOW_OK;
    GstSectionAssembler *filter = GST_SECTION_ASSEMBLER(GST_OBJECT_PARENT(pad));
    GstAdapter* adapter = filter->adapter;

    // put the input buf into our fifo for processing...
    gst_adapter_push(adapter, buf);

    while (gst_adapter_available(adapter) >= TS_PACKET_SIZE)
    {
        const guint8 *peek_buf = gst_adapter_peek(adapter, 1);

        // are we in sync?
        if (peek_buf[0] != TS_SYNC_BYTE)
        {
            int i = 0;

            // no - grab a whole packet and sync-up
            peek_buf = gst_adapter_peek(adapter, TS_PACKET_SIZE);

            for (i = 0; peek_buf[i] != TS_SYNC_BYTE && i < TS_PACKET_SIZE; i++);

            if (i == TS_PACKET_SIZE)
            {
                // this should never happen; i.e. we should always be able to
                // sync within a packet at this point in the stream...
                gst_adapter_flush(adapter, i);
                GST_ERROR_OBJECT(filter, "couldn't sync to transport stream!?");
                return GST_FLOW_ERROR;
            }
            else if (i > 0)
            {
                // drop all the stuff before the sync byte on the floor...
                gst_adapter_flush(adapter, i);
            }
        }

        // if we have at least one packet worth of data to process - do it
        if (gst_adapter_available(adapter) >= TS_PACKET_SIZE)
        {
             GstBuffer *out = gst_adapter_take_buffer(adapter, TS_PACKET_SIZE);
             gst_section_assembler_process_one_packet(pad, out);
        }
    }

    return ret;
}

static GstFlowReturn gst_section_assembler_process_one_packet(GstPad * pad, GstBuffer * buf)
{
    GstSectionAssembler *filter = GST_SECTION_ASSEMBLER(GST_OBJECT_PARENT(pad));
    GstFlowReturn ret = GST_FLOW_OK;
    if (GST_BUFFER_DATA(buf)[0] != TS_SYNC_BYTE)
    {
        GST_WARNING_OBJECT(filter,
                "Received non MPEG-2 transport stream packet %p", buf);
        gst_util_dump_mem(GST_BUFFER_DATA(buf), GST_BUFFER_SIZE(buf));
        gst_buffer_unref(buf);
    }
    else
    {
        gboolean transport_error_indicator =
                (GST_BUFFER_DATA(buf)[1] & 0x80) ? TRUE : FALSE;
        guint8 transport_scrambling_control = (GST_BUFFER_DATA(buf)[3] & 0xC0)
                >> 6;
        guint16 PID = GST_READ_UINT16_BE(&GST_BUFFER_DATA(buf)[1]) & 0x1FFF;

        if (filter->drop_tei_packets && transport_error_indicator)
        {
            GST_LOG_OBJECT(
                    filter,
                    "transport_error_indicator set on packet for PID %u, dropping buffer %p",
                    (guint32) PID, buf);
            gst_buffer_unref(buf);
        }
        else if (filter->drop_tsc_packets && transport_scrambling_control != 0)
        {
            GST_LOG_OBJECT(
                    filter,
                    "transport_scrambling_control set on packet for PID %u, dropping buffer %p",
                    (guint32) PID, buf);
            gst_buffer_unref(buf);
        }
        else
        {
            GstSectionAdapterContainer *container = NULL;

            // TAKE GLOBAL LOCK
            g_mutex_lock(filter->adapter_containers_lock);
            container = g_tree_lookup(filter->adapter_containers_tree,
                    GUINT_TO_POINTER((guint32) PID));
            if (filter->assemble_on_all_PIDs && container == NULL)
            {
                container = gst_section_assembler_enable(filter, PID, TRUE);
            }

            if (container)
            {
                // lock the individual PID container before releasing the tree lock
                // TAKE CONTAINER LOCK
                g_mutex_lock(container->adapter_lock);
            }
            // RELEASE GLOBAL LOCK
            g_mutex_unlock(filter->adapter_containers_lock);

            if (container)
            {
                GQueue assembled_sections_queue = G_QUEUE_INIT;
                gst_section_adapter_push_packet(container->adapter, buf);
                while (gst_section_adapter_available_sections(
                        container->adapter) > 0)
                {
                    GstBuffer *section = gst_section_adapter_take_section(
                            container->adapter);
                    gboolean section_syntax_indicator = (GST_BUFFER_DATA(
                            section)[1] & 0x80) ? TRUE : FALSE;
                    if (filter->check_crc && section_syntax_indicator
                            && crc32_calc(GST_BUFFER_DATA(section),
                                    GST_BUFFER_SIZE(section)) != 0)
                    {
                        GST_WARNING_OBJECT(
                                filter,
                                "CRC failed - skipping assembled section for PID %u",
                                (guint32) PID);

                        gst_section_adapter_clear(container->adapter);
                    }
                    else
                    {
                        // Prepend header (PID) information and add data to the queue
                        g_queue_push_tail(&assembled_sections_queue,
                                gst_buffer_merge(container->metadata, section));
                    }
                    gst_buffer_unref(section);
                }
                // RELEASE CONTAINER LOCK
                g_mutex_unlock(container->adapter_lock);

                // Pushing sections over the source pad is done without holding any locks
                while (ret == GST_FLOW_OK && g_queue_is_empty(
                        &assembled_sections_queue) == FALSE)
                {
                    GstCaps *pcaps;
                    GstBuffer *out =
                            g_queue_pop_head(&assembled_sections_queue);

                    // set caps on buffer before pushing
                    pcaps = gst_caps_copy(gst_pad_get_pad_template_caps(
                            filter->srcpad));
                    gst_buffer_set_caps(out, pcaps);
                    gst_caps_unref(pcaps);

                    ret = gst_pad_push(filter->srcpad, out);
                }
                g_queue_foreach(&assembled_sections_queue,
                        (GFunc) gst_mini_object_unref, NULL);
                g_queue_clear(&assembled_sections_queue);
            }
            else
            {
                gst_buffer_unref(buf);
            }
        }
    }

    return ret;
}

/********************************************************/
/**********                                    **********/
/********** GstSectionAssembler IMPLEMENTATION **********/
/**********                                    **********/
/********************************************************/

// adapter_containers_lock must be held when calling this function
static GstSectionAdapterContainer*
gst_section_assembler_enable(GstSectionAssembler* assembler, guint16 PID,
        gboolean enable)
{
    gpointer p_PID = GUINT_TO_POINTER((guint32) PID);
    GstSectionAdapterContainer *adapter_container = g_tree_lookup(
            assembler->adapter_containers_tree, p_PID);
    if (enable)
    {
        if (adapter_container != NULL)
        {
            (void) g_object_ref(adapter_container);
        }
        else if (g_tree_nnodes(assembler->adapter_containers_tree)
                == assembler->max_section_assemblers)
        {
            GST_WARNING_OBJECT(
                    assembler,
                    "Maximum number of assemblers already active (%u) - ignorting enable request for PID %4u",
                    (guint32) assembler->max_section_assemblers, (guint32) PID);
        }
        else
        {
            GST_DEBUG_OBJECT(assembler, "Enabling assembler for PID %4u",
                    (guint32) PID);
            adapter_container = gst_section_adapter_container_new(PID);
            g_tree_insert(assembler->adapter_containers_tree, p_PID,
                    adapter_container);
        }
    }
    else
    {
        if (adapter_container == NULL)
        {
            GST_WARNING_OBJECT(
                    assembler,
                    "Assembler not active on PID %4u - ignoring disable request",
                    (guint32) PID);
        }
        else if (G_OBJECT(adapter_container)->ref_count > 1)
        {
            g_object_unref(adapter_container);
        }
        else
        {
            GST_DEBUG_OBJECT(assembler, "Disabling assembler for PID %4u",
                    (guint32) PID);
            g_mutex_lock(adapter_container->adapter_lock);
            (void) g_tree_remove(assembler->adapter_containers_tree, p_PID);
            g_mutex_unlock(adapter_container->adapter_lock);
            g_object_unref(adapter_container);
            adapter_container = NULL;
        }
    }
    return adapter_container;
}

//#include <windows.h>
static gpointer gst_section_assembler_test_thread(
        gpointer user_data_section_assembler)
{
    GstSectionAssembler *assembler = NULL;
    GstISectionAssembler *iassembler = NULL;
    //GRand *rand = g_rand_new();
    gboolean enable = TRUE;

    g_return_val_if_fail(GST_IS_SECTION_ASSEMBLER(user_data_section_assembler),
            NULL);
    assembler = GST_SECTION_ASSEMBLER(user_data_section_assembler);
    iassembler = GST_ISECTION_ASSEMBLER(assembler);

    GST_INFO_OBJECT(assembler, "gst_section_assembler_test_thread started");

    /*
     while (GST_IS_ISECTION_ASSEMBLER(iassembler) &&
     GST_IS_SECTION_ASSEMBLER(assembler) && assembler->test_interface == TRUE)
     {
     gst_isection_assembler_enable(iassembler, 3000, enable);
     //Sleep(g_rand_int_range(rand, 1, 100));
     enable = !enable;
     }
     */
    gst_isection_assembler_enable(iassembler, 256, enable);

    GST_INFO_OBJECT(assembler, "gst_section_assembler_test_thread terminated");

    return NULL;
}

/*******************************************************************/
/**********                                               **********/
/********** GstISectionAssembler INTERFACE IMPLEMENTATION **********/
/**********                                               **********/
/*******************************************************************/

static void gst_isection_assembler_enable_impl(
        GstISectionAssembler* iassembler, guint16 PID, gboolean enable)
{
    GstSectionAssembler *assembler = GST_SECTION_ASSEMBLER(iassembler);

    g_mutex_lock(assembler->adapter_containers_lock);
    (void) gst_section_assembler_enable(assembler, PID, enable);
    g_mutex_unlock(assembler->adapter_containers_lock);
}

static void gst_isection_assembler_interface_init(
        GstISectionAssemblerClass * iface)
{
    iface->enable = gst_isection_assembler_enable_impl;
}

static gboolean gst_isection_assembler_supported(GstSectionAssembler *object,
        GType iface_type)
{
    g_assert(iface_type == GST_TYPE_ISECTION_ASSEMBLER);
    return TRUE;
}
