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

/*
 * GStreamer
 * Copyright (C) 2005 Thomas Vander Stichele <thomas@apestaart.org>
 * Copyright (C) 2005 Ronald S. Bultje <rbultje@ronald.bitfreak.net>
 * Copyright (C) 2009  <<user@hostname.org>>
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
 * SECTION:element-gstindexingfilesink
 *
 * This plugin is used to stream content to disk files that can subsequently
 * be used by a corresponding sink element to perform trick-play functionality
 * when used with CableLabs' OCAP Reference Implementation.
 * <pre>
 * Usage notes:
 * 1. This plugin can be used in two modes:
 *  <code>bitBucket = TRUE</code> - incoming content is simply discarded.  This
 *  allows the element to be included in a pipeline and placed in the 'playing'
 *  state without causing the remainder of the pipeline to stall.
 *  <code>bitBucket = FALSE</code> - incoming content is spooled to disk files.
 *  Of course, when in this mode, the <code>filePath</code>, <code>fileName</code>,
 *  <code>maxSize</code>, <code>videoPid</code>, and <code>audioPid</code>
 *  parameters must be set appropriately before placing the element
 *  into the 'playing' state.
 * 2. Assuming that the properties are set correctly, placing the element in
 *  'playing' state causes the relevant files to be created on the disk, and
 *  content to be rendered into them.  Placing the element in the 'null' state
 *  causes the relevant files to be closed.
 * 3. The <code>ifsHandle</code> is only available when the element is in the
 * READY, PAUSED, or PLAYING operational states.
 * </pre>
 * <refsect2>
 * <title>Example launch line</title>
 *
 * |[
 * gst-launch -v -m fakesrc ! gstindexingfilesink bitBucket=FALSE filePath=/test maxSize=0 videoPid=0x0bb8 audioPid=0x0bb9
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <gst/gst.h>

#include <glib/gprintf.h>

#include <string.h>

#include "gstindexingfilesink.h"

//////////////////////////////////////////////////////////////////////////////
// GStreamer registration code begin
//////////////////////////////////////////////////////////////////////////////

//#define VERSION 0.9

GST_DEBUG_CATEGORY_STATIC ( gst_indexing_filesink_debug);

#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_indexing_filesink_debug

/* Filter signals and args */
enum
{
    /* FILL ME */
    LAST_SIGNAL
};

enum
{
    PROP_0,
    //  PROP_SILENT,
    PROP_FILE_PATH,
    PROP_FILE_NAME,
    PROP_MAX_SIZE,
    PROP_VIDEO_PID,
    PROP_AUDIO_PID,
    PROP_BIT_BUCKET,
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
                                "video/mpegts, packetsize=(int)188, systemstream=(boolean)true"));

static void pre_init(GType type)
{
    GST_DEBUG_CATEGORY_INIT(gst_indexing_filesink_debug, // cat
            "indexingfilesink", // name
            0, // color
            "gst indexing filesink debug category"); // description
}

//
// forward declarations
//
/*lint -esym(551,parent_class)*/
GST_BOILERPLATE_FULL(GstIndexingFilesink, // type
        gst_indexing_filesink, // type_as_function
        GstBaseSink, // parent_type
        GST_TYPE_BASE_SINK, // parent_type_macro
        pre_init) // additional_initializations

static void gst_indexing_filesink_set_property (GObject* object, guint prop_id,
        const GValue * value, GParamSpec * pspec);
static void gst_indexing_filesink_get_property(GObject* object, guint prop_id,
        GValue* value, GParamSpec* pspec);
static gboolean gst_file_sink_start(GstBaseSink* basesink);
static gboolean gst_file_sink_stop(GstBaseSink* basesink);
static GstFlowReturn gst_file_sink_render(GstBaseSink* basesink,
        GstBuffer* buffer);

static gboolean gst_file_sink_start_inner(GstIndexingFilesink* filesink,
        gboolean ignoreBitBucket);
static gboolean gst_file_sink_stop_inner(GstIndexingFilesink* filesink,
        gboolean ignoreBitBucket);

static void gst_file_sink_get_times(GstBaseSink * bsink, GstBuffer * buf,
        GstClockTime * start, GstClockTime * end);

//
// GObject vmethod implementations
//

/**
 * Initialize declarative information about this element.
 * Called during class initialization and any child class initialization.
 */
static void gst_indexing_filesink_base_init(gpointer gclass)
{
    GstElementClass *element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, // klass
            "IndexingFilesink", // longname
            "Sink/Video", // classification
            "Indexing file sink for CableLabs' OCAP RI", // description
            "Andy Abendschein for CableLabs <user@hostname.org>"); // author

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/**
 * One-time initialization of this class
 */
static void gst_indexing_filesink_class_init(GstIndexingFilesinkClass* klass)
{
    GST_LOG("Entry");

    GObjectClass *gobject_class;

    GstBaseSinkClass* gstbasesink_class = GST_BASE_SINK_CLASS(klass);

    gobject_class = (GObjectClass *) klass;

    gobject_class->set_property = gst_indexing_filesink_set_property;
    gobject_class->get_property = gst_indexing_filesink_get_property;

    // install class properties

    g_object_class_install_property(gobject_class, PROP_FILE_PATH,
            g_param_spec_string("filePath", // name
                    "filePath", // nick
                    "Base directory directory path of recording", // blurb
                    "", // default value
                    G_PARAM_READWRITE)); // flags

    g_object_class_install_property(gobject_class, PROP_FILE_NAME,
            g_param_spec_string("fileName", // name
                    "fileName", // nick
                    "Name of recording file, may be \"\" to generate", // blurb
                    "", // default value
                    G_PARAM_READWRITE)); // flags

    g_object_class_install_property(gobject_class, PROP_MAX_SIZE,
            g_param_spec_ulong("maxSize", // name
                    "maxSize", // nick
                    "Max size in seconds, 0->single, !0=multiple files", // blurb
                    0, // minimum
                    G_MAXULONG, // maximum
                    0, // default value
                    G_PARAM_READWRITE)); // flags

    g_object_class_install_property(gobject_class, PROP_VIDEO_PID,
            g_param_spec_uint("videoPid", // name
                    "videoPid", // nick
                    "video PID to be recorded & indexed", // blurb
                    0, // minimum
                    G_MAXSHORT, // maximum
                    0x1FFF, // default value
                    G_PARAM_READWRITE)); // flags

    g_object_class_install_property(gobject_class, PROP_AUDIO_PID,
            g_param_spec_uint("audioPid", // name
                    "audioPid", // nick
                    "audio PID to be recorded & indexed", // blurb
                    0, // minimum
                    G_MAXSHORT, // maximum
                    0x1FFF, // default value
                    G_PARAM_READWRITE)); // flags

    g_object_class_install_property(gobject_class, PROP_BIT_BUCKET,
            g_param_spec_boolean("bitBucket", // name
                    "bitBucket", // nick
                    "when true, just discards buffer contents", // blurb
                    TRUE, // default value
                    G_PARAM_READWRITE)); // flags

    // install any function pointers that are implemented by this plugin
    gstbasesink_class->start = GST_DEBUG_FUNCPTR(gst_file_sink_start);
    gstbasesink_class->stop = GST_DEBUG_FUNCPTR(gst_file_sink_stop);
    gstbasesink_class->render = GST_DEBUG_FUNCPTR(gst_file_sink_render);
    gstbasesink_class->get_times = GST_DEBUG_FUNCPTR(gst_file_sink_get_times);

    GST_LOG("Exit");
}

/**
 * Instance initialization (i.e. constructor).
 * initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_indexing_filesink_init(GstIndexingFilesink * filesink,
        GstIndexingFilesinkClass * gclass)
{
    GST_LOG_OBJECT(filesink, "Entry");
    //    asm("INT $0x3");


    // initialize properties to default values
    filesink->pFilePath = g_strdup("");
    filesink->pFileName = g_strdup("");
    filesink->maxSize = 0; // in milliseconds
    filesink->videoPid = 0x1FFF;
    filesink->audioPid = 0x1FFF;
    filesink->bitBucket = FALSE;
    filesink->ifsHandle = NULL;

    GST_LOG_OBJECT(filesink, "Exit");
}

static void gst_indexing_filesink_set_property(GObject* object, guint prop_id,
        const GValue* value, GParamSpec* pspec)
{
    GstIndexingFilesink *filesink = GST_GSTINDEXINGFILESINK(object);

    gchar* valueInfo = g_strdup_value_contents(value);
    GST_INFO_OBJECT(filesink, "Setting property %s to %s",
            g_param_spec_get_name(pspec), valueInfo);
    g_free(valueInfo);

    GST_OBJECT_LOCK(filesink);

    switch (prop_id)
    {
    case PROP_FILE_PATH:
        // if we have a file name...
        if (NULL != filesink->pFilePath)
        {
            // ...relinquish its memory
            g_free(filesink->pFilePath);
            filesink->pFilePath = NULL;
        }
        filesink->pFilePath = g_strdup(g_value_get_string(value));
        break;

    case PROP_FILE_NAME:
        // if we have a file name...
        if (NULL != filesink->pFileName)
        {
            // ...relinquish its memory
            g_free(filesink->pFileName);
            filesink->pFileName = NULL;
        }
        filesink->pFileName = g_strdup(g_value_get_string(value));
        break;

    case PROP_MAX_SIZE:
    {
        // save the original maxSize value
        IfsTime originalMaxSize = filesink->maxSize;

        // set the new maxSize value
        filesink->maxSize = g_value_get_ulong(value);

        // if an indexing file sink is currently open...
        if (NULL != filesink->ifsHandle)
        {
            // ...request that its maxSize value be updated
            IfsReturnCode ifsReturnCode;
            ifsReturnCode = IfsSetMaxSize(filesink->ifsHandle, // IfsHandle ifsHandle Input (must be a writer)
                    filesink->maxSize); // IfsTime   maxSize   Input (in seconds, 0 is illegal)

            // if something bad happened...
            if (IfsReturnCodeNoErrorReported != ifsReturnCode)
            {
                // ...say so and...
                GST_ERROR_OBJECT(filesink,
                        "Error setting maxSize property: %s",
                        IfsReturnCodeToString(ifsReturnCode));

                // ...restore the original value
                filesink->maxSize = originalMaxSize;
            }
        }
    }
        break;

    case PROP_VIDEO_PID:
        filesink->videoPid = g_value_get_uint(value);
        break;

    case PROP_AUDIO_PID:
        filesink->audioPid = g_value_get_uint(value);
        break;

    case PROP_BIT_BUCKET:
        filesink->bitBucket = g_value_get_boolean(value);
        if (filesink->bitBucket)
        {
            if (FALSE == gst_file_sink_stop_inner(filesink, TRUE))
            {
                GST_ERROR_OBJECT(filesink,
                        "Error setting bitBucket property - gst_file_sink_stop_inner() failed");
            }
        }
        else
        {
            if (FALSE == gst_file_sink_start_inner(filesink, TRUE))
            {
                GST_ERROR_OBJECT(filesink,
                        "Error setting bitBucket property - gst_file_sink_start_inner() failed");
            }
        }
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
    GST_OBJECT_UNLOCK(filesink);
}

static void gst_indexing_filesink_get_property(GObject * object, guint prop_id,
        GValue* value, GParamSpec * pspec)
{
    GstIndexingFilesink *filesink = GST_GSTINDEXINGFILESINK(object);

    GST_OBJECT_LOCK(filesink);
    switch (prop_id)
    {
    case PROP_FILE_PATH:
        g_value_set_string(value, filesink->pFilePath);
        break;

    case PROP_FILE_NAME:
        g_value_set_string(value, filesink->pFileName);
        break;

    case PROP_MAX_SIZE:
        g_value_set_ulong(value, filesink->maxSize);
        break;

    case PROP_VIDEO_PID:
        g_value_set_uint(value, filesink->videoPid);
        break;

    case PROP_AUDIO_PID:
        g_value_set_uint(value, filesink->audioPid);
        break;

    case PROP_BIT_BUCKET:
        g_value_set_boolean(value, filesink->bitBucket);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
    GST_OBJECT_UNLOCK(filesink);
    gchar* valueInfo = g_strdup_value_contents(value);

    GST_INFO_OBJECT(filesink, "Getting property, %s = %s",
            g_param_spec_get_name(pspec), valueInfo);
    g_free(valueInfo);
}

//////////////////////////////////////////////////////////////////////////////
// Implementation code begin
//////////////////////////////////////////////////////////////////////////////


/**
 * Allocate resources prior to rendering.
 * Open output file for writing content.
 */
static gboolean gst_file_sink_start_inner(GstIndexingFilesink* filesink,
        gboolean ignoreBitBucket)
{
    IfsReturnCode ifsReturnCode;
    gboolean retVal = FALSE; // assume failure

    GST_LOG_OBJECT(filesink, "Entry");

    // if not functioning as a 'bit bucket'...
    if (ignoreBitBucket || FALSE == filesink->bitBucket)
    {
        // ...prepare to function as a file sink
        filesink->timeInitial = 0;

        // if the input parameters are ok...
        if ((NULL != filesink->pFileName) && (0 < strlen(filesink->pFilePath))
                && (0 != filesink->videoPid) && (0 != filesink->audioPid))
        {
            // ...open the output file
            ifsReturnCode = IfsOpenWriter(filesink->pFilePath, // const char * path      Input
                    filesink->pFileName, // const char * name      Input  (if NULL the name is generated)
                    filesink->maxSize, // IfsTime maxSize        Input  (in seconds, 0 = no max)
                    &filesink->ifsHandle); // IfsHandle * pIfsHandle Output (use IfsClose() to free)

            GST_DEBUG_OBJECT(filesink,
                    "IfsOpenWriter info: path = %s, name = %s, maxSize = %lu",
                    filesink->pFilePath, filesink->pFileName, filesink->maxSize);

            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                // the file was opened ok, so set the PIDs to be recorded
                ifsReturnCode = IfsStart(filesink->ifsHandle, // IfsHandle ifsHandle Input (must be a writer)
                        filesink->videoPid, // IfsPid    videoPid  Input
                        filesink->audioPid); // IfsPid    audioPid  Input

                if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                {
                    GST_INFO_OBJECT(
                            filesink,
                            "IFS using PIDs (videoPid = 0x%x, audioPid = 0x%x) set ok",
                            filesink->videoPid, filesink->audioPid);

                    // indicate success
                    retVal = TRUE;
                }
                else
                {
                    GST_ERROR_OBJECT(filesink, "Error calling IfsStart(): %s",
                            IfsReturnCodeToString(ifsReturnCode));

                    // there appears to be a problem setting the PIDs, so close the file
                    ifsReturnCode = IfsClose(filesink->ifsHandle);
                    if (IfsReturnCodeNoErrorReported == ifsReturnCode)
                    {
                        filesink->ifsHandle = NULL;
                    }
                    else
                    {
                        GST_ERROR_OBJECT(filesink,
                                "Error calling IfsClose(): %s",
                                IfsReturnCodeToString(ifsReturnCode));
                    }
                }
            }
            else
            {
                GST_ERROR_OBJECT(filesink, "Unable to open file: %s",
                        IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            GST_ERROR_OBJECT(
                    filesink,
                    "Bad input parameter: path = %s, name = %s, videoPid = 0x%x, audioPid = 0x%x",
                    filesink->pFilePath, filesink->pFileName,
                    filesink->videoPid, filesink->audioPid);
        }
    }
    else
    {
        // ...functioning as a bit bucket, so say that we are ready to go
        GST_INFO_OBJECT(filesink, "Operating in bit bucket mode");
        retVal = TRUE;
    }

    GST_LOG_OBJECT(filesink, "Exit, return value = %d", retVal);
    return retVal;
}

static gboolean gst_file_sink_start(GstBaseSink* basesink)
{
    gboolean bReturn;
    GstIndexingFilesink* filesink = GST_GSTINDEXINGFILESINK(basesink);

    GST_OBJECT_LOCK(filesink);

    bReturn = gst_file_sink_start_inner(filesink, FALSE);

    GST_OBJECT_UNLOCK(filesink);

    return bReturn;
}

/**
 * Deallocate resources after rendering.
 */
static gboolean gst_file_sink_stop(GstBaseSink* basesink)
{
    gboolean bReturn;
    GstIndexingFilesink* filesink = GST_GSTINDEXINGFILESINK(basesink);

    GST_OBJECT_LOCK(filesink);

    bReturn = gst_file_sink_stop_inner(filesink, FALSE);

    GST_OBJECT_UNLOCK(filesink);

    return bReturn;
}

static gboolean gst_file_sink_stop_inner(GstIndexingFilesink* filesink,
        gboolean ignoreBitBucket)
{
    IfsReturnCode ifsReturnCode;
    gboolean retVal = FALSE; // assume failure

    GST_LOG_OBJECT(filesink, "Entry");

    // if not functioning as a 'bit bucket'...
    if (ignoreBitBucket || FALSE == filesink->bitBucket)
    {
        // ...cleanup the file sink operation
        if (NULL != filesink->ifsHandle)
        {
            ifsReturnCode = IfsClose(filesink->ifsHandle);
            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                filesink->ifsHandle = NULL;
                retVal = TRUE;
            }
            else
            {
                GST_ERROR_OBJECT(filesink, "Error closing file: %s",
                        IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            GST_INFO_OBJECT(filesink,
                    "File handle parameter is NULL, ignoreBitBucket=%d",
                    ignoreBitBucket);
            retVal = TRUE;
        }

        // if we have a file name...
        if (NULL != filesink->pFileName)
        {
            // ...relinquish its memory
            g_free(filesink->pFileName);
            filesink->pFileName = NULL;
        }

        // if we have a path...
        if (NULL != filesink->pFilePath)
        {
            // ...relinquish its memory
            g_free(filesink->pFilePath);
            filesink->pFilePath = NULL;
        }
    }
    else
    {
        // ...functioning as a bit bucket, so say that we are ready to go
        GST_INFO_OBJECT(filesink, "Operating in bit bucket mode");

        // indicate success
        retVal = TRUE;
    }

    GST_LOG_OBJECT(filesink, "Exit, return value = %d", retVal);
    return retVal;
}

/**
 * Writes the contents of buffers to the IFS library in chunks that are
 * multiples of <code>IFS_TRANSPORT_PACKET_SIZE (188)</code> bytes.
 */
static GstFlowReturn gst_file_sink_render(GstBaseSink* basesink,
        GstBuffer* buffer)
{
    GstIndexingFilesink* filesink = GST_GSTINDEXINGFILESINK(basesink);

    GST_LOG_OBJECT(filesink, "Entry");

    GstFlowReturn retVal = GST_FLOW_OK;

    GST_OBJECT_LOCK(filesink);

    // if not functioning as a 'bit bucket'...
    if (FALSE == filesink->bitBucket)
    {
        GST_DEBUG_OBJECT(
                filesink,
                "%s(), buffer size=%d, timestamp(raw) = %llu, timestamp(gmt) = %llu, free_func = %s",
                __FUNCTION__, buffer->size, buffer->timestamp,
                buffer->timestamp + filesink->timeInitial, ((NULL
                        != buffer->free_func) ? "Exists" : "NULL"));

        // determine # of packets in buffer
        int packetCount = buffer->size / IFS_TRANSPORT_PACKET_SIZE;

        GST_DEBUG_OBJECT(filesink, "\tPacket count = %d", packetCount);

        GstClock* gstClock = gst_system_clock_obtain();

        if (NULL != gstClock)
        {
            IfsReturnCode ifsReturnCode;

            // write out the complete packets in the buffer
            ifsReturnCode = IfsWrite(filesink->ifsHandle, // IfsHandle   ifsHandle  Input (must be a writer)
                    gst_clock_get_time(gstClock), // IfsClock    ifsClock   Input, in nanoseconds
                    packetCount, // NumPackets  numPackets Input
                    (IfsPacket*) GST_BUFFER_DATA(buffer)); // IfsPacket * pData      Input

            g_object_unref(gstClock);

            if (IfsReturnCodeNoErrorReported == ifsReturnCode)
            {
                GST_DEBUG_OBJECT(filesink, "\t%s() IfsWrite is ok",
                        __FUNCTION__);
            }
            else
            {
                GST_ERROR_OBJECT(filesink, "\t%s() IfsWrite is bad: %s",
                        __FUNCTION__, IfsReturnCodeToString(ifsReturnCode));
            }
        }
        else
        {
            GST_ERROR_OBJECT(filesink,
                    "\tUnable to obtain clock during IfsWrite.");
        }

    }
    else
    {
        GST_DEBUG_OBJECT(filesink, "\tFunctioning as bitBucket");
    }

    GST_OBJECT_UNLOCK(filesink);

    GST_LOG_OBJECT(filesink, "Exit, return value = %d", retVal);
    return retVal;
}

static void gst_file_sink_get_times(GstBaseSink* bsink, GstBuffer* buf,
        GstClockTime* start, GstClockTime* end)
{
    //GST_DEBUG("function entry");

    GstIndexingFilesink* filesink = GST_GSTINDEXINGFILESINK(bsink);

    GstClock* gstClock = gst_element_get_clock(GST_ELEMENT(bsink));
    if (NULL == gstClock)
    {
        GST_ERROR_OBJECT(filesink, "element clock was NULL");
        gst_object_unref(gstClock);
        return;
    }
    GstClockTime now = gst_clock_get_time(gstClock);
    GstClockTime base_time = gst_element_get_base_time(GST_ELEMENT(bsink));
    GstClockTime stream_time = now - base_time;
    GstClockTimeDiff start_delta = (GstClockTimeDiff) GST_CLOCK_TIME_NONE;

    gst_object_unref(gstClock);

    GST_DEBUG_OBJECT(filesink, "now = %" GST_TIME_FORMAT ", base_time = %"
            GST_TIME_FORMAT ", stream_time = %" GST_TIME_FORMAT, GST_TIME_ARGS(now),
            GST_TIME_ARGS(base_time), GST_TIME_ARGS(stream_time));

    if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
    {
        *start = GST_BUFFER_TIMESTAMP(buf);

        if (base_time > 0) // don't adjust buffers in live pipelines
        {
            start_delta = *start - stream_time;
            if (start_delta > GST_SECOND)
            {
                //*start -= start_delta;
GST_ERROR_OBJECT            (filesink, "Buffer is over 1 second early (approx."
                    " %llis) - adjusting to %" GST_TIME_FORMAT ".",
                    start_delta / GST_SECOND, GST_TIME_ARGS(*start));
        }
    }

    if (GST_BUFFER_DURATION_IS_VALID(buf))
    {
        *end = *start + GST_BUFFER_DURATION(buf);
    }
    else
    {
        /*
         if (display->fps_n > 0)
         {
         *end = *start +
         gst_util_uint64_scale_int(GST_SECOND, display->fps_d,
         display->fps_n);
         }
         */
    }
}
}
