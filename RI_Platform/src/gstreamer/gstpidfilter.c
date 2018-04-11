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
 *Steves First pidfilter
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
 * PID:element-pidfilter
 *
 * FIXME:Describe pidfilter here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m fakesrc ! pidfilter pidlist="0x0000 0x0001"
 * ! fakesink silent=TRUE ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <gst/gst.h>
#include <glib/gprintf.h>
#include <stdlib.h>
#include <string.h>
#include "gstpidfilter.h"

GST_DEBUG_CATEGORY_STATIC ( gst_pid_filter_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_pid_filter_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0, PROP_SILENT, PROP_REMAPINFO, PROP_PIDLIST
};

/* the capabilities of the inputs and outputs.
 *
 * describe the real formats here.
 */
#if 1
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
        GST_PAD_SINK, GST_PAD_ALWAYS, GST_STATIC_CAPS("video/mpegts,"
            "packetsize=(int)188,"
            "systemstream=(boolean)true"));
#else
static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE ("sink",
        GST_PAD_SINK,
        GST_PAD_ALWAYS,
        GST_STATIC_CAPS ("ANY")
);
#endif

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
        GST_PAD_SRC, GST_PAD_ALWAYS,

        GST_STATIC_CAPS("video/mpegts,"
            "packetsize=(int)188,"
            "systemstream=(boolean)true"));

/*lint -e(123) -esym(551,parent_class)*/GST_BOILERPLATE (GstPidFilter, gst_pid_filter, GstElement, GST_TYPE_ELEMENT)
// Forward declarations
static void gst_pid_filter_set_property (GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_pid_filter_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec);

//static gboolean gst_pid_filter_set_caps (GstPad * pad, GstCaps * caps);
static gboolean gst_pid_filter_src_event(GstPad *pad, GstEvent *event);
static GstFlowReturn gst_pid_filter_chain(GstPad * pad, GstBuffer * buf);

static gboolean gst_pid_filter_event(GstPad *pad, GstEvent *event);

static GstBuffer *gst_pid_filter_process_data(GstPidFilter *filter,
        GstBuffer *buf);

static void gst_pid_filter_set_remapinfo(GstPidFilter *filter,
        const gchar *remapinfo);
static void gst_pid_filter_set_pidlist(GstPidFilter *filter,
        const gchar *pidlist);
static void gst_pid_filter_stop_processing(GstPidFilter *filter);
static void gst_pid_filter_syncdet_init(GstPidFilter *filter);
static void gst_pid_filter_syncdet_run(GstPidFilter *filter, GstBuffer *buf);

static guint gst_pid_filter_hexstr_to_int(const gchar *val);

#define BE_16_AT(ptr) GST_READ_UINT16_BE(ptr)
#define BE_32_AT(ptr) GST_READ_UINT32_BE(ptr)

void dump(GstPidFilter *filter, guint8 *buf, gint size, gchar *name)
{
#if 0
#define DUMP_BYTES_PER_LINE 16
#define DUMP_CHARS_PER_BYTE  3
#define BYTE_LINE_LENGTH    (DUMP_BYTES_PER_LINE * DUMP_CHARS_PER_BYTE)

    guint index = 0;
    guint offset = 0;
    gchar buffer[BYTE_LINE_LENGTH+1];

    {
        GST_DEBUG_OBJECT(filter,"%s(%p):", name, buf);

        while (index < size)
        {
            g_snprintf(&buffer[offset], DUMP_CHARS_PER_BYTE+1, "%02X ", buf[index]);
            index++;
            offset = (offset + DUMP_CHARS_PER_BYTE) % BYTE_LINE_LENGTH;
            if (offset == 0)
            {
                GST_DEBUG_OBJECT(filter,"%s", buffer);
            }
        }

        if (offset != 0)
        {
            buffer[offset] = '\0';
            GST_DEBUG_OBJECT(filter,"%s", buffer);
        }
    }

#undef BYTE_LINE_LENGTH
#undef DUMP_CHARS_PER_BYTE
#undef DUMP_BYTES_PER_LINE
#endif
}

static guint gst_pid_filter_hexstr_to_int(const gchar *val)
{
    gchar *endptr;
    gint64 pid64;
    if (g_str_has_prefix(val, "0X"))
    {
        pid64 = g_ascii_strtoll(&(val[2]), &endptr, 16);
        if (endptr == &(val[2]))
        {
            pid64 = 0xFFFFFFFF;
        }
    }
    else
    {
        pid64 = g_ascii_strtoll(val, &endptr, 10);
        if (endptr == val)
        {
            pid64 = 0xFFFFFFFF;
        }
    }
    // g_printf("Converted to value %lld \n", pid64);
    /* Ignore invalid strings */
    if (endptr != val)
    {
        if (pid64 < 8192)
        {
            return (gint) pid64;
        }
    }
    return 0xFFFFFFFF;
}

/* GObject vmethod implementations */
static void gst_pid_filter_set_remapinfo(GstPidFilter * filter,
        const gchar *remapinfo)
{
    gchar **p_info;
    gchar *p_map;
    guint prog;
    guint pid;
    guint map;
    gchar *upperinfo;
    RemapReturnCode rc;

    /* clear up old pid list strings */
    if (filter->stringremapinfo)
    {
        g_free(filter->stringremapinfo);
        filter->stringremapinfo = NULL;
    }

    /* If not NULL, populate the new values */
    if (remapinfo != NULL)
    {
        /* Make a copy of the passed string */
        filter->stringremapinfo = g_strdup(remapinfo);

        /* Convert the string to all upper case*/
        upperinfo = g_ascii_strup(filter->stringremapinfo, -1);

        /* Break up the string based on space delimiters */
        p_info = g_strsplit(upperinfo, " ", 0);

        /* get the program number from the list of info */
        if (p_info[0] != NULL)
        {
            GST_DEBUG_OBJECT(filter, "Processing program number string %s",
                    p_info[0]);
            prog = gst_pid_filter_hexstr_to_int(p_info[0]);

            if (prog < 65535)
            {
                map = prog; // default to pass and not map
                GST_DEBUG_OBJECT(filter, "Found program number 0x%4.4X", prog);

                if (NULL != (p_map = strstr(p_info[0], "=")))
                {
                    p_map++; // inrement past the '='
                    map = gst_pid_filter_hexstr_to_int(p_map);
                    GST_DEBUG_OBJECT(filter, "map program number 0x%X to 0x%X",
                            prog, map);
                }
                else
                {
                    GST_DEBUG_OBJECT(filter, "set program number 0x%4.4X", prog);
                }

                filter->oldprognum = prog;
                filter->newprognum = map;
            }
        }

        /* get the PMT PID from the list of info */
        if (p_info[1] != NULL)
        {
            GST_DEBUG_OBJECT(filter, "Processing PMT PID string %s", p_info[1]);
            pid = gst_pid_filter_hexstr_to_int(p_info[1]);

            if (pid < 8192)
            {
                map = pid; // default to pass and not map
                GST_DEBUG_OBJECT(filter, "Found PMT pid 0x%4.4X", pid);

                if (NULL != (p_map = strstr(p_info[1], "=")))
                {
                    p_map++; // inrement past the '='
                    map = gst_pid_filter_hexstr_to_int(p_map);
                    GST_DEBUG_OBJECT(filter, "map PMT pid 0x%4.4X to 0x%4.4X",
                            pid, map);
                }
                else
                {
                    GST_DEBUG_OBJECT(filter, "set PMT pid 0x%4.4X", pid);
                }

                filter->oldpmtpid = pid;
                filter->newpmtpid = map;

                rc = RemapPats(filter->remap_handle, filter->newprognum,
                        filter->newpmtpid);
                if (rc == RemapReturnCodeNoErrorReported)
                {
                    GST_INFO_OBJECT(filter,
                            "RemapPats(%p, 0x%X, 0x%X) succeeded",
                            filter->remap_handle, filter->newprognum,
                            filter->newpmtpid);
                }
                else
                {
                    GST_ERROR_OBJECT(filter,
                            "RemapPats(%p, 0x%X, 0x%X) failed with code %d",
                            filter->remap_handle, filter->newprognum,
                            filter->newpmtpid, rc);
                }

                rc = RemapPmts(filter->remap_handle, filter->oldpmtpid);
                if (rc == RemapReturnCodeNoErrorReported)
                {
                    GST_INFO_OBJECT(filter, "RemapPmts(%p, 0x%X) succeeded.",
                            filter->remap_handle, filter->oldpmtpid);
                }
                else
                {
                    GST_ERROR_OBJECT(filter,
                            "RemapPmts(%p, 0x%X) failed with code %d.",
                            filter->remap_handle, filter->oldpmtpid, rc);
                }
            }
        }

        /* Free up the upper and tokenized list. No need to check if NULL */
        g_free(upperinfo);
        g_strfreev(p_info);
        GST_INFO_OBJECT(filter, "Provided with remapinfo=%s", remapinfo);
    }
    else
    {
        GST_INFO_OBJECT(filter, "Handed empty remapinfo, no PAT/PMT remapping");
    }
}

static void gst_pid_filter_set_pidlist(GstPidFilter * filter,
        const gchar *pidlist)
{
    gchar **p_pids;
    gchar *p_map;
    guint pid;
    guint map;
    gint i;
    gchar *upperpids;
    NumPairs num_pairs = 0; // default to filter all (i.e. pass none)
    RemapPair *pairs = NULL;
    RemapReturnCode rc;

    /* clear up old pid list strings */
    if (filter->stringpidlist)
    {
        g_free(filter->stringpidlist);
        filter->stringpidlist = NULL;
    }

    /* If not NULL, populate the new values */
    if (pidlist != NULL) /* Set leftovers to none */
    {
        pairs = (RemapPair*) g_try_malloc0(sizeof(RemapPair) * 8192);

        if (NULL == pairs)
        {
            GST_ERROR_OBJECT(filter,
                            "line %d of %s, %s memory allocation failure!\n",
                            __LINE__, __FILE__, __func__);
            exit(-1);
        }

        /* Make a copy of the passed string */
        filter->stringpidlist = g_strdup(pidlist);
        /* Convert the string to all upper case*/
        upperpids = g_ascii_strup(filter->stringpidlist, -1);
        /* Break up the string based on space delimiters */
        p_pids = g_strsplit(upperpids, " ", 0);

        /* Cycle through the list of text pids */
        i = 0;
        while (p_pids[i] != NULL)
        {
            // g_printf("Processing PID string %d which is %s\n",i, p_pids[i]);
            pid = gst_pid_filter_hexstr_to_int(p_pids[i]);
            if (pid < 8192)
            {
                map = pid; // default to pass and not map
                // g_printf("Found pid 0x%4.4X\n", pid);
                if (NULL != (p_map = strstr(p_pids[i], "=")))
                {
                    p_map++; // inrement past the '='
                    map = gst_pid_filter_hexstr_to_int(p_map);
                    GST_INFO_OBJECT(filter,
                            "map active pid 0x%4.4X to 0x%4.4X", pid, map);
                }
                else
                {
                    GST_INFO_OBJECT(filter, "set active pid 0x%4.4X", pid);
                }

                pairs[num_pairs].oldPid = pid;
                pairs[num_pairs].newPid = map;
                num_pairs++;
            }
            i++;
        }
        /* Free up the upper and tokenized list. No need to check if NULL */
        g_free(upperpids);
        g_strfreev(p_pids);

        //g_printf("pidfilter: provided with pidlist=%s\n",pidlist);
    }
    else
    {
        //g_printf("pidfilter: handed empty pidlist, allowing all through\n");
        num_pairs = 8192;
        pairs = NULL;
    }

    rc = RemapAndFilterPids(filter->remap_handle, num_pairs, pairs);
    if (rc == RemapReturnCodeNoErrorReported)
    {
        GST_INFO_OBJECT(filter, "RemapAndFilterPids(%p, %d, %p) succeeded.",
                filter->remap_handle, num_pairs, pairs);
    }
    else
    {
        GST_ERROR_OBJECT(filter,
                "RemapAndFilterPids(%p, %d, %p) failed with code %d.",
                filter->remap_handle, num_pairs, pairs, rc);
    }

    if (pairs)
    {
        g_free(pairs);
    }
}

/* gst_pid_filter_stop_processing
 * Description: Called when the end of stream is signalled.
 *              Also called during filter init to init some object members
 */
static void gst_pid_filter_stop_processing(GstPidFilter * filter)
{
    /* Set up the sync detector */
    gst_pid_filter_syncdet_init(filter);
}

/* gst_pid_filter_syncdet_init
 * Description: Called to re-init the sync detector (looks for 0x47's).
 */
static void gst_pid_filter_syncdet_init(GstPidFilter *filter)
{
    gint i;

    /* Reset the sync detector */
    filter->packet_sync_present = FALSE;
    filter->packet_sync_offset = 0;
    filter->sd_running_offset = 0;
    /* set all sync detector values to TRUE */
    for (i = 0; i < 188; i++)
    {
        filter->b47[i] = TRUE;
    }
    filter->num_47s = 188;
    /* Set leftovers and bytes_since_start to none */
    filter->leftover_count = 0;
    filter->bytes_since_start = 0;

}

/* gst_pid_filter_syncdet_run
 * Description: Called to process a buffer and update the packet sync state.
 * Maintains state from one buffer to the next
 */
static void gst_pid_filter_syncdet_run(GstPidFilter *filter, GstBuffer *buf)
{
    gint i;
    gint buflen = GST_BUFFER_SIZE(buf);
    guint8 *pdata = GST_BUFFER_DATA(buf);
    guint8 databyte;

    /* iterate until done with buffer or sync detected */
    while ((!filter->packet_sync_present) && (buflen))
    {
        buflen--;
        databyte = *(pdata++);
        //        g_printf("Byte at offset %d is 0x%2.2X\n", filter->sd_running_offset, databyte);
        if (databyte != 0x47)
        {
            filter->b47[filter->sd_running_offset] = FALSE;
            filter->num_47s--;
            /* If only one recurring 47 left then sync is found */
            /* 0 should never happen, but deal with that later */
            if (filter->num_47s <= 1)
            {

                /* Now find the only 47 left */
                for (i = 0; i < 188; i++)
                {
                    //                    g_printf("b47[%d] = %s.\n", i, (filter->b47[i]?"TRUE":"FALSE"));
                    if ((filter->b47[i]) == TRUE)
                    {
                        filter->packet_sync_offset = i;
                        //                        g_printf("Sync at offset %d.\n", i);
                        break;
                    }
                } /* endfor each candidate location */

                /* defensive: if did not find 0x47, reset syncdet*/
                /* else mark sync as found */
                if (i == 188)
                {
                    gst_pid_filter_syncdet_init(filter);
                }
                else
                {
                    filter->packet_sync_present = TRUE;
                    //                    g_printf("Gained Packet Sync\n");
                } /* endif found NO 0x47's */
            } /* endif have a single candidate position left */
        } /* endif current byte is not 0x47 */
        /* Advance in the sync detector */
        filter->sd_running_offset++;
        filter->sd_running_offset %= 188;
    } /* endwhile iterating through packet */
}

static gboolean gst_pid_filter_event(GstPad *pad, GstEvent *event)
{
    GstPidFilter *filter = GST_PIDFILTER(gst_pad_get_parent(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_EOS:
        /* end-of-stream, we should close down all stream leftovers here */
        gst_pid_filter_stop_processing(filter);
        break;
    case GST_EVENT_NEWSEGMENT:
        GST_INFO_OBJECT(filter, "Received NEWSEGMENT event");
        break;
    default:
        break;
    }
    gst_object_unref(filter);
    return gst_pad_event_default(pad, event);
}

gboolean performMapAndFilter(GstPidFilter *filter, RemapPacket *pResidualData,
        RemapPacket *pData, NumPackets count)
{
    RemapReturnCode rc;
    NumPackets prev_count = 0;

    PREV_PKT = CURR_PKT;
    CURR_PKT++;
    CURR_PKT %= MAX_PKT_INDEX;
    GST_LOG_OBJECT(filter, "count:%d, CURR:%c, PREV:%c", (int) count, CURR_PKT
            + 'A', PREV_PKT + 'A');

    rc
            = RemapAndFilter(filter->remap_handle, pResidualData, count, pData,
                    &filter->ppkts[CURR_PKT][0], &prev_count,
                    &filter->pppkts[PREV_PKT]);

    if (RemapReturnCodeNoErrorReported == rc)
    {
        filter->pkt_count[PREV_PKT] = prev_count;
        GST_LOG_OBJECT(filter, "output count: %d", (int) prev_count);
        return TRUE;
    }
    else
    {
        filter->pkt_count[PREV_PKT] = 0;
        GST_ERROR_OBJECT(
                filter,
                "gstgstRemapAndFilter(%p, %d, %p, %p, %p, %p) failed with code %d.",
                filter->remap_handle, (int) count, &pData,
                filter->ppkts[CURR_PKT], &prev_count,
                &filter->pppkts[PREV_PKT], rc);
        return FALSE;
    }
}

#define COPY_INBOUND_BUFFER

static GstBuffer *gst_pid_filter_process_data(GstPidFilter *filter,
        GstBuffer *buf)
{
    /* Use reference to filter instance data here */
    guint i;
    gint buflen = GST_BUFFER_SIZE(buf);
    guint8 *pdata = GST_BUFFER_DATA(buf);
    GstBuffer *outbuf = (GstBuffer *) NULL;
    guint8 *poutdata;
    gboolean entry_sync = filter->packet_sync_present;
    gint m, n, p;
    gint discard_count;
    NumPackets count;
    gboolean process_residual = FALSE;

    GST_LOG_OBJECT(filter, "Buffer received: %d bytes.", buflen);
    //dump(filter, pdata, 256, "gstbuf");
#ifdef COPY_INBOUND_BUFFER
    if (NULL != filter->saveptr[CURR_PKT])
    {
        g_free(filter->saveptr[CURR_PKT]);
    }

    filter->saveptr[CURR_PKT] = g_try_malloc0(GST_BUFFER_SIZE(buf));

    if (NULL == filter->saveptr[CURR_PKT])
    {
        GST_ERROR_OBJECT(filter,
                "failed to allocate %d bytes for input buf copy?!", buflen);
        return (GstBuffer *) NULL;
    }
    else
    {
        memcpy(filter->saveptr[CURR_PKT], GST_BUFFER_DATA(buf), buflen);
        pdata = filter->saveptr[CURR_PKT];
    }
#endif
    /* if no sync at present, run sync detector */
    if (!filter->packet_sync_present)
    {
        gst_pid_filter_syncdet_run(filter, buf);
    } /* endif not in packet sync */

    /* If we gained sync, work out where to start */
    if (!entry_sync && (filter->packet_sync_present))
    {
        /* == Logic for where to find sync ==
         * Sync detector starts with first byte at filter start.
         * f->packet_sync_offset (p) is relative to that zero point
         * This GstBuffer starts at f->bytes_since_start and has a logical
         * offset compared to the sync detector of (n) where
         * n = bytes_since_start % 188 .
         * We have (m) bytes at the start of this buffer to get to a 188
         * byte boundary to line up with the sync detector. m+n = 188, m = 188-n
         * If p < n, then the first sync byte in this buffer is at offset m+p
         * if p > n, then the first sync byte in this buffer is at offset p-n */
        n = filter->bytes_since_start % 188;
        p = filter->packet_sync_offset;
        m = (188 - n) % 188; // %188 catches the n = 0 case
        /* output some debug */
        GST_LOG_OBJECT(filter,
                "Gained sync in this packet, bss: %d, m: %d, n: %d, p: %d",
                filter->bytes_since_start, m, n, p);
        if (p <= n)
        {
            discard_count = m + p;
        }
        else
        {
            discard_count = p - n;
        } /* endif */

        /* discard fractional packet at start of this buffer */
        pdata += discard_count;
        buflen -= discard_count;
        GST_LOG_OBJECT(
                filter,
                "Discarded %d bytes at the start of buffer, sync byte is 0x%2.2X",
                discard_count, *pdata);

    } /* endif we went from no sync to sync on this packet */

    /* if we have sync, process the buffer */
    if (filter->packet_sync_present)
    {
        GST_LOG_OBJECT(filter, "Packet Sync present, processing the buffer.");
        /* First deal with leftovers from last time */
        /* Perhaps could be done more efficiently, future improvement */
        if (filter->leftover_count)
        {
            GST_LOG_OBJECT(filter,
                    "%d leftover bytes, copying %d bytes to form packet",
                    filter->leftover_count, min(188 - (filter->leftover_count),
                            buflen));
            //dump(filter, pdata,
            //     min(188 - (filter->leftover_count), buflen),"end leftovers");
            memcpy(&(filter->residual[CURR_PKT][filter->leftover_count]),
                    pdata, min(188 - (filter->leftover_count), buflen));
            /* if we reconstructed a full packet */
            if ((filter->leftover_count + buflen) >= 188)
            {
                GST_LOG_OBJECT(filter,
                        "Reconstructed a complete leftover packet - processing...");
                /* Remove the leftover makeup from the head of the buffer */
                pdata += (188 - filter->leftover_count);
                buflen -= (188 - filter->leftover_count);

                /* queue up the reconstructed leftover packet */
                if (filter->residual[CURR_PKT][0] == 0x47)
                {
                    process_residual = TRUE;
                }
                else
                {
                    /* Sync byte was not correct, signal sync loss & give up */
                    GST_DEBUG_OBJECT(filter,
                            "Sync byte on leftover packet invalid - lost sync.");
                    gst_pid_filter_syncdet_init(filter);
                    return (GstBuffer *) NULL;
                }
            }
            else
            {
                /* account for the bytes we appended to the leftovers */
                filter->leftover_count += buflen;
                filter->bytes_since_start += buflen;

                /* return that we have no downstream buffer to send */
                return (GstBuffer *) NULL;
            } /* if we had enough to reconstitute a packet */
        }
        else
        {
            GST_LOG_OBJECT(filter, "No Leftovers to process");
        } /* endif we had leftovers from previous GstBuffer */

        if (MAX_PACKETS < (count = buflen / 188))
        {
            GST_ERROR_OBJECT(filter,
                    "error - given more than %d packets at one time!",
                    MAX_PACKETS);
            gst_pid_filter_syncdet_init(filter);
            return (GstBuffer *) NULL;
        }

        GST_LOG_OBJECT(filter, "Buffer data to process - check Packet Sync.");
        /* Check for packet sync still good */
        if (*pdata == 0x47)
        {
#ifndef COPY_INBOUND_BUFFER
            // free the previous buffer and save the one to be mapped/filtered
            if (NULL != filter->savebuf[CURR_PKT])
            {
                gst_buffer_unref(filter->savebuf[CURR_PKT]);
            }

            (void)gst_buffer_ref(buf);
            filter->savebuf[CURR_PKT] = buf;
#endif
            filter->savetimestamp[CURR_PKT] = GST_BUFFER_TIMESTAMP(buf);
            buflen -= (count * 188);

            if (!performMapAndFilter(
                    filter,
                    (process_residual ? (RemapPacket*) filter->residual[CURR_PKT]
                            : NULL), (RemapPacket*) pdata, count))
            {
                return (GstBuffer *) NULL;
            }

            pdata += (count * 188);
        }
        else
        {
            /* Sync byte was not correct, signal sync loss and give up */
            GST_DEBUG_OBJECT(filter, "Sync byte on packet invalid - lost sync");
            gst_pid_filter_syncdet_init(filter);
            return (GstBuffer *) NULL;
        }

        /* Now create the output GstBuffer and assemble it */
        if (filter->pkt_count[PREV_PKT])
        {
            GST_LOG_OBJECT(filter, "processing %d pkts, C:%c, P:%c",
                    filter->pkt_count[PREV_PKT], CURR_PKT + 'A', PREV_PKT + 'A');
            outbuf = gst_buffer_try_new_and_alloc(filter->pkt_count[PREV_PKT]
                    * 188);
            if (outbuf)
            {
                if (filter->savetimestamp[PREV_PKT])
                {
                    GST_BUFFER_TIMESTAMP(outbuf)
                            = filter->savetimestamp[PREV_PKT];
                }

                poutdata = GST_BUFFER_DATA(outbuf);

                if (filter->pkt_count[PREV_PKT])
                {
                    GST_LOG_OBJECT(filter, "processing %d PREV [%c] pkts",
                            filter->pkt_count[PREV_PKT], PREV_PKT + 'A');
                    for (i = 0; i < filter->pkt_count[PREV_PKT]; i++)
                    {
                        memcpy(poutdata, filter->pppkts[PREV_PKT][i], 188);
                        //dump(filter, poutdata, 188, "output");
                        poutdata += 188;
                    }
                }
            }
            else
            {
                GST_ERROR_OBJECT(filter,
                        "Failed to allocate new gst output buffer of size %d",
                        filter->pkt_count[PREV_PKT] * 188);
            } /* endif created output buffer OK */
        } /* endif any output packets */

        /* now copy any leftovers from out input buffer to state data */
        filter->leftover_count = buflen;
        GST_LOG_OBJECT(filter, "Copying Leftovers - buflen = %d", buflen);

        if (buflen < 188)
        {
            memcpy(filter->residual[CURR_PKT], pdata, buflen);
            //dump(filter, pdata, buflen, "start of leftovers");
        }
        else
        {
            GST_ERROR_OBJECT(filter, "too many leftover bytes: %d", buflen);
        }

        /* return newly filled output buffer */
        return outbuf;
    }
    else
    {
        /* not in sync, return null (nothing to forward) */
        filter->bytes_since_start += buflen;
        return (GstBuffer *) NULL;
    }
}

static void gst_pid_filter_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class,
            "MPEG-2 transport stream pid filter.", "Codec/Demuxer",
            "Outputs a PID filtered MPEG Transport Stream (for now). ",
            "Steve Glennon <s.glennon@cablelabs.com>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the pidfilter's class */
static void gst_pid_filter_class_init(GstPidFilterClass * klass)
{
    GObjectClass *gobject_class;

    GST_DEBUG_CATEGORY_INIT(gst_pid_filter_debug, "pidfilter", 0,
            "PID Filter element");

    gobject_class = (GObjectClass *) klass;

    gobject_class->set_property = gst_pid_filter_set_property;
    gobject_class->get_property = gst_pid_filter_get_property;

    g_object_class_install_property(gobject_class, PROP_SILENT,
            g_param_spec_boolean("silent", "Silent",
                    "Produce verbose output ?", FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_REMAPINFO,
            g_param_spec_string("remapinfo", "RemapInfo",
                    "Program Number and PMT PID info for the remapper", NULL,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_PIDLIST,
            g_param_spec_string("pidlist", "PIDList",
                    "List of included PIDs as 0xnnnn 0xnnnn", NULL,
                    G_PARAM_READWRITE));
}

/* initialize the new element
 * instantiate pads and add them to element GstPidFilter
 * set pad calback functions
 * initialize instance structure
 */
static void gst_pid_filter_init(GstPidFilter * filter,
        GstPidFilterClass * gclass)
{
    gint i;
    RemapReturnCode rc;
    filter->sinkpad = gst_pad_new_from_static_template(&sink_factory, "sink");
    //    gst_pad_set_setcaps_function (filter->sinkpad,
    //                                  GST_DEBUG_FUNCPTR(gst_pid_filter_set_caps));
    //    gst_pad_set_getcaps_function (filter->sinkpad,
    //                                  GST_DEBUG_FUNCPTR(gst_pad_proxy_getcaps));
    gst_pad_set_chain_function(filter->sinkpad, GST_DEBUG_FUNCPTR(
            gst_pid_filter_chain));
    gst_pad_use_fixed_caps(filter->sinkpad);

    filter->srcpad = gst_pad_new_from_static_template(&src_factory, "src");
    //    gst_pad_set_getcaps_function (filter->srcpad,
    //                                  GST_DEBUG_FUNCPTR(gst_pad_proxy_getcaps));

    gst_pad_use_fixed_caps(filter->srcpad);

    gst_element_add_pad(GST_ELEMENT(filter), filter->sinkpad);
    gst_element_add_pad(GST_ELEMENT(filter), filter->srcpad);
    /* Add event handler to the sink pad */
    gst_pad_set_event_function(filter->sinkpad, gst_pid_filter_event);
    /* Add event handler to the src pad */
    gst_pad_set_event_function(filter->srcpad, gst_pid_filter_src_event);

    /* Set up any other data in this filter */
    filter->props_lock = g_mutex_new();
    filter->silent = FALSE;
    filter->stringpidlist = NULL;

    /* Call the stop processing function to init everything else */
    gst_pid_filter_stop_processing(filter);

    PREV_PKT = 1;
    CURR_PKT = 2;

    for (i = 0; i < MAX_PKT_INDEX; i++)
    {
        filter->savebuf[i] = NULL;
        filter->saveptr[i] = NULL;
        filter->savetimestamp[i] = 0;
    }

    rc = RemapOpen(&filter->remap_handle);
    if (rc == RemapReturnCodeNoErrorReported)
    {
        GST_INFO_OBJECT(filter, "RemapOpen(%p) succeeded.",
                filter->remap_handle);
    }
    else
    {
        GST_ERROR_OBJECT(filter, "RemapOpen(%p) failed with code %d.",
                filter->remap_handle, rc);
    }
}

static void gst_pid_filter_set_property(GObject * object, guint prop_id,
        const GValue * value, GParamSpec * pspec)
{
    GstPidFilter *filter = GST_PIDFILTER(object);

    switch (prop_id)
    {
    case PROP_SILENT:
        g_mutex_lock(filter->props_lock);
        filter->silent = g_value_get_boolean(value);
        g_mutex_unlock(filter->props_lock);
        break;
    case PROP_REMAPINFO:
        g_mutex_lock(filter->props_lock);
        gst_pid_filter_set_remapinfo(filter,
                (const gchar *) g_value_get_string(value));
        g_mutex_unlock(filter->props_lock);
        break;
    case PROP_PIDLIST:
        g_mutex_lock(filter->props_lock);
        gst_pid_filter_set_pidlist(filter, (const gchar *) g_value_get_string(
                value));
        g_mutex_unlock(filter->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_pid_filter_get_property(GObject * object, guint prop_id,
        GValue * value, GParamSpec * pspec)
{
    GstPidFilter *filter = GST_PIDFILTER(object);

    switch (prop_id)
    {
    case PROP_SILENT:
        g_mutex_lock(filter->props_lock);
        g_value_set_boolean(value, filter->silent);
        g_mutex_unlock(filter->props_lock);
        break;
    case PROP_REMAPINFO:
        g_mutex_lock(filter->props_lock);
        g_value_set_string(value, filter->stringremapinfo);
        g_mutex_unlock(filter->props_lock);
        break;
    case PROP_PIDLIST:
        g_mutex_lock(filter->props_lock);
        g_value_set_string(value, filter->stringpidlist);
        g_mutex_unlock(filter->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/* GstElement vmethod implementations */

#if 0
/* this function handles the link with other elements */
static gboolean
gst_pid_filter_set_caps (GstPad * pad, GstCaps * caps)
{
    GstPidFilter *filter;
    GstPad *otherpad;

    filter = GST_PIDFILTER (gst_pad_get_parent (pad));
    otherpad = (pad == filter->srcpad) ? filter->sinkpad : filter->srcpad;
    gst_object_unref (filter);

    return gst_pad_set_caps (otherpad, caps);
}
#endif

static gboolean gst_pid_filter_src_event(GstPad *pad, GstEvent *event)
{
    GstPidFilter *filter = GST_PIDFILTER(GST_OBJECT_PARENT(pad));

    switch (GST_EVENT_TYPE(event))
    {
    case GST_EVENT_QOS:
        gst_event_parse_qos(event, &filter->qos_proportion,
                &filter->qos_time_diff, &filter->qos_timestamp);
        GST_DEBUG_OBJECT(filter, "QOS: proportion %f, diff %lld, time %llu",
                filter->qos_proportion, filter->qos_time_diff,
                filter->qos_timestamp);
        GST_DEBUG_OBJECT(filter, "QOS diff %lldms", filter->qos_time_diff
                / 1000000);
        break;
    case GST_EVENT_SEEK:
        GST_INFO_OBJECT(filter, "Received SEEK event");
        break;
    case GST_EVENT_NAVIGATION:
        GST_INFO_OBJECT(filter, "Received NAVIGATION event");
        break;
    case GST_EVENT_LATENCY:
        GST_INFO_OBJECT(filter, "Received LATENCY event");
        break;
    case GST_EVENT_FLUSH_START:
        GST_INFO_OBJECT(filter, "Received FLUSH_START event");
        break;
    case GST_EVENT_FLUSH_STOP:
        GST_INFO_OBJECT(filter, "Received FLUSH_STOP event");
        break;
    default:
        GST_INFO_OBJECT(filter, "Received other (0x%X) event", GST_EVENT_TYPE(
                event));
        break;
    }

    return gst_pad_event_default(pad, event);
}

/* chain function
 * this function does the actual processing
 */
static GstFlowReturn gst_pid_filter_chain(GstPad * pad, GstBuffer * buf)
{
    GstPidFilter *filter;
    GstBuffer *outbuf;

    filter = GST_PIDFILTER(GST_OBJECT_PARENT(pad));

    outbuf = gst_pid_filter_process_data(filter, buf);
    gst_buffer_unref(buf);
    if (filter->silent == FALSE)
    {
        //g_print ("I'm plugged, therefore I'm in.\n");
    }

    /* If we got an output buffer to send, go ahead and send it, otherwise signal OK */
    if (outbuf)
    {
        return gst_pad_push(filter->srcpad, outbuf);
    }
    else
    {
        return GST_FLOW_OK;
    }
}

void gst_pid_filter_finalize(GObject * object)
{
    GstPidFilter *filter = GST_PIDFILTER(object);
    g_mutex_free(filter->props_lock);
}

#if 0
/* entry point to initialize the plug-in
 * initialize the plug-in itself
 * register the element factories and other features
 */
static gboolean
pidfilter_init (GstPidFilter * pidfilter)
{
    /* debug category for fltering log messages
     *
     * exchange the string 'Template pidfilter' with your description
     */

    return gst_element_register ((GstPlugin *)pidfilter, "pidfilter", GST_RANK_NONE,
            GST_TYPE_PIDFILTER);
}

/* gstreamer looks for this structure to register pidfilters
 *
 * exchange the string 'Template pidfilter' with your pidfilter description
 */
GST_PLUGIN_DEFINE (
        GST_VERSION_MAJOR,
        GST_VERSION_MINOR,
        "pidfilter",
        "PID Filter element",
        (GstPluginInitFunc)pidfilter_init,
        VERSION,
        "LGPL",
        "gst-cablelabs_ri",
        "http://gstreamer.net/"
)
#endif
