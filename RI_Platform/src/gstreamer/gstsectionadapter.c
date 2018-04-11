/* GStreamer
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
 * FIXME:Describe sectionadapter here.
 */

#include "gstsectionadapter.h"
#include "gstmpeg.h"

GST_DEBUG_CATEGORY_STATIC ( gst_section_adapter_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_section_adapter_debug

#define _do_init(thing) \
  GST_DEBUG_CATEGORY_INIT (gst_section_adapter_debug, "sectionadapter", 0, "Single PID MPEG-2 private section adapter")

/*lint -e(123)*/GST_BOILERPLATE_FULL (GstSectionAdapter, gst_section_adapter, GObject, G_TYPE_OBJECT, _do_init)

// Forward declarations
static void gst_section_adapter_dispose (GObject * object);
static void gst_section_adapter_finalize(GObject * object);

static void gst_section_adapter_clear_adapter_data(GstSectionAdapter * adapter);
static void gst_section_adapter_clear_packet_data(GstSectionAdapter * adapter);
static void gst_section_adapter_clear_section_data(GstSectionAdapter * adapter);

static guint8 calculate_payload_start_offset(GstSectionAdapter * adapter,
        GstBuffer * buf);
static guint8 calculate_payload_unit_start_offset(GstSectionAdapter * adapter,
        GstBuffer * buf, guint8 payload_start_offset);
static gboolean packet_continuity_verified(GstSectionAdapter * adapter,
        GstBuffer * buf);

/**
 * gst_section_adapter_new:
 *
 * Creates a new #GstSectionAdapter. Free with g_object_unref().
 *
 * Returns: a new #GstSectionAdapter
 */
GstSectionAdapter *
gst_section_adapter_new(void)
{
    return g_object_new(GST_TYPE_SECTION_ADAPTER, NULL);
}

/**
 * gst_section_adapter_clear:
 * @adapter: a #GstSectionAdapter
 *
 * Removes all buffers and incomplete data from @adapter.
 */
void gst_section_adapter_clear(GstSectionAdapter * adapter)
{
    g_return_if_fail(GST_IS_SECTION_ADAPTER(adapter));

    gst_section_adapter_clear_adapter_data(adapter);
    gst_section_adapter_clear_packet_data(adapter);
    gst_section_adapter_clear_section_data(adapter);

    adapter->section_adapter_state = STATE_LOOKING_FOR_PUSI;
}

/**
 * gst_section_adapter_push:
 * @adapter: a #GstSectionAdapter
 * @buf: a #GstBuffer to add to queue in the adapter
 *
 * Adds the data from @buf to the data stored inside @adapter and takes
 * ownership of the buffer.
 */
void gst_section_adapter_push_packet(GstSectionAdapter * adapter,
        GstBuffer * buf)
{
    guint8 payload_start_offset = INVALID_PAYLOAD_OFFSET;
    guint8 current_offset = INVALID_PAYLOAD_OFFSET;

    g_return_if_fail(GST_IS_SECTION_ADAPTER(adapter));
    g_return_if_fail(GST_IS_BUFFER(buf));
    g_return_if_fail(GST_BUFFER_SIZE(buf) == TS_PACKET_SIZE);
    g_return_if_fail(GST_BUFFER_DATA(buf)[0] == TS_SYNC_BYTE);

    payload_start_offset = calculate_payload_start_offset(adapter, buf);
    current_offset = payload_start_offset;

    // packet_continutity_verified always saves the continuity_counter, even if
    // discontinutiy is detected - this allows to start at STATE_LOOKING_FOR_PUSI
    // with the continuity_counter already saved for the current packet
    if (packet_continuity_verified(adapter, buf) == FALSE)
    {
        gst_section_adapter_clear_section_data(adapter);
        adapter->section_adapter_state = STATE_LOOKING_FOR_PUSI;
    }

    // Account for case where the packet boundary was reached while assembling
    // data in a previous invocation of gst_section_adapter_push_packet. In these
    // cases we must skip past the pointer_field (if it exists) which would
    // otherwise direct us to the beginning of the next section to be built rather
    // than the currently incomplete one.
    if (adapter->section_adapter_state != STATE_LOOKING_FOR_PUSI)
    {
        gboolean payload_unit_start_indicator =
                (GST_BUFFER_DATA(buf)[1] & 0x40) ? TRUE : FALSE;
        if (payload_unit_start_indicator == TRUE)
        {
            current_offset += TS_PACKET_POINTER_FIELD_LENGTH;
        }
    }

    while (current_offset <= TS_PACKET_MAX_OFFSET
            || adapter->section_adapter_state == STATE_SECTION_COMPLETE)
    {
        switch (adapter->section_adapter_state)
        {

        /********************************************/
        /**********                        **********/
        /********** STATE_LOOKING_FOR_PUSI **********/
        /**********                        **********/
        /********************************************/
        case STATE_LOOKING_FOR_PUSI:
        {
            // don't use current_offset here as we might have gone through some
            // number of the loop iterations (which modified the current_offset)
            // before ending up in this state
            guint8 payload_unit_start_offset =
                    calculate_payload_unit_start_offset(adapter, buf,
                            payload_start_offset);
            if (payload_unit_start_offset <= TS_PACKET_MAX_OFFSET)
            {
                GST_DEBUG_OBJECT(
                        adapter,
                        "STATE_LOOKING_FOR_PUSI:  payload_unit_start_indicator set - new section start at position %u",
                        (guint32) payload_unit_start_offset);
                current_offset = payload_unit_start_offset;
                adapter->section_adapter_state = STATE_ASSEMBLING_HEADER;
            }
            else
            {
                GST_DEBUG_OBJECT(
                        adapter,
                        "STATE_LOOKING_FOR_PUSI:  No payload_unit_start_indicator detected in packet - skipping packet");
                current_offset = INVALID_PAYLOAD_OFFSET;
                // stay in STATE_LOOKING_FOR_PUSI
            }
            break;
        }

            /*********************************************/
            /**********                         **********/
            /********** STATE_ASSEMBLING_HEADER **********/
            /**********                         **********/
            /*********************************************/
        case STATE_ASSEMBLING_HEADER:
        {
            guint have_bytes = gst_adapter_available(adapter->section_adapter);
            guint can_read_bytes = TS_PACKET_SIZE - current_offset;
            guint need_bytes = SECTION_HEADER_LENGTH - have_bytes;
            guint read_bytes = MIN(need_bytes, can_read_bytes);

            GST_DEBUG_OBJECT(
                    adapter,
                    "STATE_ASSEMBLING_HEADER: have_bytes = %u, can_read_bytes = %u, need_bytes = %u, read_bytes = %u",
                    have_bytes, can_read_bytes, need_bytes, read_bytes);

            gst_adapter_push(adapter->section_adapter, gst_buffer_create_sub(
                    buf, current_offset, read_bytes));
            current_offset += read_bytes;

            if (gst_adapter_available(adapter->section_adapter)
                    == SECTION_HEADER_LENGTH)
            {
                const guint8 *section_header = gst_adapter_peek(
                        adapter->section_adapter, SECTION_HEADER_LENGTH);
                guint16 private_section_length = GST_READ_UINT16_BE(
                        &section_header[SECTION_HEADER_LENGTH_OFFSET]) & 0x0FFF;
                if (private_section_length > SECTION_DATA_MAX_LENGTH)
                {
                    GST_WARNING_OBJECT(
                            adapter,
                            "private_section_length exceeds %u (0x%X) - resetting",
                            (guint32) SECTION_DATA_MAX_LENGTH,
                            (guint32) SECTION_DATA_MAX_LENGTH);
                    gst_section_adapter_clear_section_data(adapter);
                    adapter->section_adapter_state = STATE_LOOKING_FOR_PUSI;

                    // skip the entire packet to avoid an infinite loop between
                    // STATE_LOOKING_FOR_PUSI and STATE_ASSEMBLING_HEADER for sections
                    // with private_section_length > 4093 starting at the PUSI offset
                    //
                    // we could also miss some valid sections if the "bad" section
                    // (i.e. with private_section_length > 4093) header spans two or
                    // more packets but this is better than the infinite loop
                    current_offset = INVALID_PAYLOAD_OFFSET;
                }
                else
                {
                    adapter->section_length = private_section_length
                            + SECTION_HEADER_LENGTH;
                    adapter->section_adapter_state = STATE_ASSEMBLING_DATA;
                }
            }
            break;
        }

            /*******************************************/
            /**********                       **********/
            /********** STATE_ASSEMBLING_DATA **********/
            /**********                       **********/
            /*******************************************/
        case STATE_ASSEMBLING_DATA:
        {
            guint have_bytes = gst_adapter_available(adapter->section_adapter);
            guint can_read_bytes = TS_PACKET_SIZE - current_offset;
            guint need_bytes = adapter->section_length - have_bytes;
            guint read_bytes = MIN(need_bytes, can_read_bytes);

            GST_DEBUG_OBJECT(
                    adapter,
                    "STATE_ASSEMBLING_DATA:   have_bytes = %4u, can_read_bytes = %3u, need_bytes = %4u, read_bytes = %3u, section_length = %4u",
                    have_bytes, can_read_bytes, need_bytes, read_bytes,
                    adapter->section_length);

            gst_adapter_push(adapter->section_adapter, gst_buffer_create_sub(
                    buf, current_offset, read_bytes));
            current_offset += read_bytes;

            if (gst_adapter_available(adapter->section_adapter)
                    == adapter->section_length)
            {
                adapter->section_adapter_state = STATE_SECTION_COMPLETE;
            }
            break;
        }

            /********************************************/
            /**********                        **********/
            /********** STATE_SECTION_COMPLETE **********/
            /**********                        **********/
            /********************************************/
        case STATE_SECTION_COMPLETE:
        {
            guint have_bytes = gst_adapter_available(adapter->section_adapter);
            GstBuffer *section = gst_adapter_take_buffer(
                    adapter->section_adapter, have_bytes);

            g_assert(have_bytes == adapter->section_length); // sanity check

            GST_DEBUG_OBJECT(adapter,
                    "STATE_SECTION_COMPLETE:  section_length = %4u",
                    (guint32) adapter->section_length);

            g_queue_push_tail(adapter->assembled_section_queue, section);

            if (current_offset <= TS_PACKET_MAX_OFFSET)
            {
                if (GST_BUFFER_DATA(buf)[current_offset] != INVALID_TABLE_ID)
                {
                    adapter->section_adapter_state = STATE_ASSEMBLING_HEADER;
                }
                else
                {
                    for (; current_offset < TS_PACKET_SIZE; current_offset++)
                    {
                        if (GST_BUFFER_DATA(buf)[current_offset]
                                != INVALID_TABLE_ID)
                        {
                            GST_WARNING_OBJECT(
                                    adapter,
                                    "Non-stuffing byte 0x%02X detected at position %u after end of section in current packet",
                                    (guint32) GST_BUFFER_DATA(buf)[current_offset],
                                    current_offset);
                        }
                    }
                    adapter->section_adapter_state = STATE_LOOKING_FOR_PUSI;
                }
            }
            else
            {
                adapter->section_adapter_state = STATE_LOOKING_FOR_PUSI;
            }
            gst_section_adapter_clear_section_data(adapter);
            break;
        }

            /*****************************/
            /*****************************/
        default:
        {
            GST_ERROR_OBJECT(adapter, "Invalid adapter state: %u",
                    (guint32) adapter->section_adapter_state);
            g_assert( FALSE);
        }
        }
    }
    gst_buffer_unref(buf);
}

/**
 * gst_section_adapter_take_section:
 * @adapter: a #GstSectionAdapter
 *
 * Returns a #GstBuffer containing the assembled section data of the
 * @adapter. The returned bytes will be flushed from the adapter.
 *
 * Caller owns returned value. gst_buffer_unref() after usage.
 *
 * Returns: a #GstBuffer containing the section data,
 * or #NULL if no assembled sections are available
 */
GstBuffer *
gst_section_adapter_take_section(GstSectionAdapter * adapter)
{
    g_return_val_if_fail(GST_IS_SECTION_ADAPTER(adapter), NULL);

    return g_queue_pop_head(adapter->assembled_section_queue);
}

/**
 * gst_adapter_available_sections:
 * @adapter: a #GstSectionAdapter
 *
 * Gets the maximum amount of available assembled sections.
 *
 * Returns: number of sections available in @adapter
 */
guint gst_section_adapter_available_sections(GstSectionAdapter * adapter)
{
    g_return_val_if_fail(GST_IS_SECTION_ADAPTER(adapter), 0);

    return g_queue_get_length(adapter->assembled_section_queue);
}

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

static void gst_section_adapter_base_init(gpointer g_class)
{
    /* nop */
}

static void gst_section_adapter_class_init(GstSectionAdapterClass * klass)
{
    GObjectClass *object = G_OBJECT_CLASS(klass);

    object->dispose = gst_section_adapter_dispose;
    object->finalize = gst_section_adapter_finalize;
}

static void gst_section_adapter_init(GstSectionAdapter * adapter,
        GstSectionAdapterClass * g_class)
{
    adapter->assembled_section_queue = g_queue_new();
    adapter->section_adapter = gst_adapter_new();

    gst_section_adapter_clear(adapter);
}

static void gst_section_adapter_dispose(GObject * object)
{
    GstSectionAdapter *adapter = GST_SECTION_ADAPTER(object);

    gst_section_adapter_clear(adapter);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, dispose, (object));
}

static void gst_section_adapter_finalize(GObject * object)
{
    GstSectionAdapter *adapter = GST_SECTION_ADAPTER(object);

    g_queue_free(adapter->assembled_section_queue);
    g_object_unref(adapter->section_adapter);

    /*lint -e(123)*/GST_CALL_PARENT(G_OBJECT_CLASS, finalize, (object));
}

/******************************************************/
/**********                                  **********/
/********** GstSectionAdapter IMPLEMENTATION **********/
/**********                                  **********/
/******************************************************/

static void gst_section_adapter_clear_adapter_data(GstSectionAdapter * adapter)
{
    g_queue_foreach(adapter->assembled_section_queue,
            (GFunc) gst_mini_object_unref, NULL);
    g_queue_clear(adapter->assembled_section_queue);
}

static void gst_section_adapter_clear_packet_data(GstSectionAdapter * adapter)
{
    adapter->last_continuity_counter = INVALID_CONTINUITY_COUNTER;
    adapter->still_discontinuous = 0;
}

static void gst_section_adapter_clear_section_data(GstSectionAdapter * adapter)
{
    gst_adapter_clear(adapter->section_adapter);
    adapter->section_length = INVALID_SECTION_LENGTH;
}

static guint8 calculate_payload_start_offset(GstSectionAdapter * adapter,
        GstBuffer * buf)
{
    guint8 adaptation_field_control = (GST_BUFFER_DATA(buf)[3] & 0x30) >> 4;
    guint8 payload_start_offset = INVALID_PAYLOAD_OFFSET;

    switch (adaptation_field_control)
    {
    case ADAPTATION_FIELD_RESERVED:
    {
        GST_WARNING_OBJECT(adapter,
                "adaptation_field_control set to reserved - skipping packet");
        payload_start_offset = INVALID_PAYLOAD_OFFSET;
        break;
    }
    case ADAPTATION_FIELD_PAYLOAD_ONLY:
    {
        // Packet payload starts right after the continuity counter
        GST_LOG_OBJECT(
                adapter,
                "adaptation_field_control set to \"No adaptation_field, payload only\" - data starts at position %u",
                (guint32) TS_PACKET_DATA_START_OFFSET);
        payload_start_offset = TS_PACKET_DATA_START_OFFSET;
        break;
    }
    case ADAPTATION_FIELD_ONLY:
    {
        // No payload - skip packet
        GST_LOG_OBJECT(
                adapter,
                "adaptation_field_control set to \"Adaptaion_field only, no payload\" - skipping packet");
        payload_start_offset = INVALID_PAYLOAD_OFFSET;
        break;
    }
    case ADAPTATION_FIELD_AND_PAYLOAD:
    {
        // Extract the adaptation_field_length
        guint8 adaptation_field_length =
                GST_BUFFER_DATA(buf)[TS_PACKET_DATA_START_OFFSET];
        payload_start_offset = TS_PACKET_DATA_START_OFFSET
                + TS_PACKET_ADAPTATION_FIELD_LENGTH_LENGTH
                + adaptation_field_length;
        if (payload_start_offset > TS_PACKET_MAX_OFFSET)
        {
            GST_WARNING_OBJECT(
                    adapter,
                    "adaptation_field_control set to \"Adaptation_field followed by payload\" but the payload offset is calculated to start past the packet boundary (%u) - skipping packet",
                    (guint32) payload_start_offset);
            payload_start_offset = INVALID_PAYLOAD_OFFSET;
        }
        else
        {
            GST_LOG_OBJECT(
                    adapter,
                    "adaptation_field_control set to \"Adaptation_field followed by payload\" - data is calculated to start at position %u",
                    (guint32) payload_start_offset);
        }
        break;
    }
    default:
    {
        GST_ERROR_OBJECT(adapter, "adaptation_field_control > 0x11 ?");
        gst_util_dump_mem(GST_BUFFER_DATA(buf), GST_BUFFER_SIZE(buf));
        g_assert( FALSE);
        break;
    }
    }

    return payload_start_offset;
}

static guint8 calculate_payload_unit_start_offset(GstSectionAdapter * adapter,
        GstBuffer * buf, guint8 payload_start_offset)
{
    gboolean payload_unit_start_indicator =
            (GST_BUFFER_DATA(buf)[1] & 0x40) ? TRUE : FALSE;
    guint8 payload_unit_start_offset = INVALID_PAYLOAD_OFFSET;

    if (payload_unit_start_indicator)
    {
        if (payload_start_offset > TS_PACKET_MAX_OFFSET)
        {
            GST_ERROR_OBJECT(
                    adapter,
                    "Passed payload_start_offset points beyond the packet boundary (%u)",
                    (guint32) payload_start_offset);
            payload_unit_start_offset = INVALID_PAYLOAD_OFFSET;
        }
        else if ((GST_READ_UINT32_BE(
                &GST_BUFFER_DATA(buf)[TS_PACKET_DATA_START_OFFSET])
                & 0xFFFFFF00) == 0x00000100)
        {
            GST_WARNING_OBJECT(adapter,
                    "Detected PES packet_start_code_prefix - ignoring payload_unit_start_indicator");
            payload_unit_start_offset = INVALID_PAYLOAD_OFFSET;
        }
        else
        {
            guint8 pointer_field_offset = payload_start_offset;
            guint8 pointer_field_value =
                    GST_BUFFER_DATA(buf)[pointer_field_offset];
            payload_unit_start_offset = payload_start_offset
                    + TS_PACKET_POINTER_FIELD_LENGTH + pointer_field_value;
            if (payload_unit_start_offset >= TS_PACKET_SIZE)
            {
                GST_WARNING_OBJECT(
                        adapter,
                        "Calculated payload_unit_start_offset points beyond the packet boundary (%u)",
                        (guint32) payload_unit_start_offset);
                payload_unit_start_offset = INVALID_PAYLOAD_OFFSET;
            }
            else
            {
                GST_LOG_OBJECT(adapter,
                        "payload_unit_start_offset at position %u",
                        (guint32) payload_unit_start_offset);
            }
        }
    }
    else
    {
        GST_LOG_OBJECT(adapter,
                "No payload_unit_start_indicator in current packet");
        payload_unit_start_offset = INVALID_PAYLOAD_OFFSET;
    }

    return payload_unit_start_offset;
}

static gboolean packet_continuity_verified(GstSectionAdapter * adapter,
        GstBuffer * buf)
{
    guint8 packet_continuity_counter = GST_BUFFER_DATA(buf)[3] & 0x0F;
    gboolean continuity_verified = FALSE;

    switch (adapter->section_adapter_state)
    {
    case STATE_ASSEMBLING_HEADER:
    case STATE_ASSEMBLING_DATA:
    {
        if (packet_continuity_counter == (adapter->last_continuity_counter + 1)
                % 16)
        {
            GST_LOG_OBJECT(
                    adapter,
                    "Continuity counter verified ok (last continuity_counter %u, current %u)",
                    (guint32) adapter->last_continuity_counter,
                    (guint32) packet_continuity_counter);
            continuity_verified = TRUE;
            adapter->still_discontinuous = 0;
        }
        else
        {
            if ((0 == adapter->still_discontinuous) ||
                (1000 <= adapter->still_discontinuous))
            {
                GST_WARNING_OBJECT(
                    adapter,
                    "Discontinuity detected (last continuity_counter %u, current %u)",
                    (guint32) adapter->last_continuity_counter,
                    (guint32) packet_continuity_counter);
                adapter->still_discontinuous = 0;
            }
            continuity_verified = FALSE;
            adapter->still_discontinuous++;
        }
        break;
    }
    default:
    {
        GST_LOG_OBJECT(adapter, "Continuity counter saved (%u) for packet %p",
                (guint32) packet_continuity_counter, buf);
        continuity_verified = TRUE;
        break;
    }
    }

    adapter->last_continuity_counter = packet_continuity_counter;

    return continuity_verified;
}
