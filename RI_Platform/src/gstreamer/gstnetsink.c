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
 * SECTION:element-netsink
 *
 * FIXME:Describe netsink here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m netsink ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <string.h> // memcpy
#include <sys/stat.h>
#include <gst/gst.h>
#include <stdlib.h>
#include "gstpacedfilesrc.h"
#include "gstnetsink.h"

#ifdef WIN32
#define RI_WIN32_SOCKETS
#define _WIN32_WINNT 0x0501
#include <winsock2.h>
#include <ws2tcpip.h>
#define CLOSESOCK(s) (void)closesocket(s)
#else
#include <arpa/inet.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#define CLOSESOCK(s) (void)close(s)
#endif

GST_DEBUG_CATEGORY_STATIC(gst_net_sink_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_net_sink_debug

enum
{
    PROP_0,
    PROP_URI,
    PROP_HOST,
    PROP_PORT,
    PROP_BLKSIZE,
    PROP_LAST,
};

static GstStaticPadTemplate sink_factory = GST_STATIC_PAD_TEMPLATE("sink",
                                            GST_PAD_SINK,
                                            GST_PAD_ALWAYS,
                                            GST_STATIC_CAPS_ANY);

// Forward declarations
static gboolean gst_net_sink_set_uri(GstNetSink* sink, const gchar* uri);
static void gst_net_sink_uri_handler_init(gpointer, gpointer);
static void gst_net_sink_dispose(GObject* object);
static void gst_net_sink_finalize(GObject* object);

static void gst_net_sink_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec);
static void gst_net_sink_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec);

static gboolean gst_net_sink_start(GstBaseSink* basesink);
static gboolean gst_net_sink_stop(GstBaseSink* basesink);

static GstFlowReturn gst_net_sink_render(GstBaseSink* sink, GstBuffer* buf);

static void
_do_init(GType type)
{
    static const GInterfaceInfo urihandler_info =
    {
        gst_net_sink_uri_handler_init,
        NULL,
        NULL
    };

    g_type_add_interface_static(type, GST_TYPE_URI_HANDLER, &urihandler_info);
    GST_DEBUG_CATEGORY_INIT(gst_net_sink_debug, "netsink", 0, "network sink");
}

/*lint -esym(551,parent_class)*/
GST_BOILERPLATE_FULL(GstNetSink,         // type
                     gst_net_sink,       // type_as_function
                     GstBaseSink,        // parent_type
                     GST_TYPE_BASE_SINK, // parent_type_macro
                     _do_init)           // additional_initializations

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

static void gst_net_sink_base_init(gpointer gclass)
{
    GstElementClass* element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "NetSink",
            "FIXME:Generic", "FIXME:Generic Template Element",
            " <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&sink_factory));
}

/* initialize the netsink's class */
static void gst_net_sink_class_init(GstNetSinkClass* klass)
{
    GObjectClass* gobject_class;
    GstBaseSinkClass* gstbasesink_class;


    gobject_class = (GObjectClass *) klass;
    gstbasesink_class = (GstBaseSinkClass *) klass;

    parent_class = g_type_class_peek_parent(klass);

    gobject_class->set_property = gst_net_sink_set_property;
    gobject_class->get_property = gst_net_sink_get_property;

    g_object_class_install_property(gobject_class, PROP_URI,
            g_param_spec_string("uri", "URI",
                    "URI in the form of udp://host:port", DEFAULT_TX_URI,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_HOST,
            g_param_spec_string("host", "host address",
                    "Address to send packets to", DEFAULT_TX_HOST,
                    G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property(gobject_class, PROP_PORT,
            g_param_spec_uint("port", "network port",
                    "network port to listen on for packets", DEFAULT_TX_PORT,
                    65535, DEFAULT_TX_PORT, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_BLKSIZE,
            g_param_spec_uint("blksize", "Generated GstBuffer size",
                    "Size in bytes to read per buffer", MIN_BLKSIZE,
                    MAX_BLKSIZE, DEFAULT_BLKSIZE, G_PARAM_READWRITE));

    gobject_class->dispose = GST_DEBUG_FUNCPTR(gst_net_sink_dispose);
    gobject_class->finalize = GST_DEBUG_FUNCPTR(gst_net_sink_finalize);
    gstbasesink_class->start = GST_DEBUG_FUNCPTR(gst_net_sink_start);
    gstbasesink_class->stop = GST_DEBUG_FUNCPTR(gst_net_sink_stop);
    gstbasesink_class->render = GST_DEBUG_FUNCPTR(gst_net_sink_render);
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_net_sink_init(GstNetSink* sink, GstNetSinkClass* gclass)
{
    sink->props_lock = g_mutex_new();

    gst_net_sink_set_uri(sink, DEFAULT_TX_URI);   // sets uri and host
    sink->port = DEFAULT_TX_PORT;
    sink->sock = -1;
    sink->blksize = DEFAULT_BLKSIZE;

    //gst_base_sink_set_format(GST_BASE_SINK(sink), GST_FORMAT_TIME);
    //gst_base_sink_set_do_timestamp(GST_BASE_SINK(sink), TRUE);
}

static void gst_net_sink_dispose(GObject* object)
{
    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static void gst_net_sink_finalize(GObject* object)
{
    GstNetSink* sink = GST_NETSINK(object);

    g_mutex_free(sink->props_lock);
    g_free(sink->uri);
    g_free(sink->host);

    if (sink->sock >= 0)
        CLOSESOCK(sink->sock);

#ifdef RI_WIN32_SOCKETS
    WSACleanup();
#endif

    G_OBJECT_CLASS(parent_class)->finalize(object);
}

static gboolean
gst_net_sink_set_host(GstNetSink* sink, const gchar* host)
{
    gboolean retVal = FALSE;
    g_mutex_lock(sink->props_lock);

    if(NULL == sink->host || 0 != strcmp(sink->host, host))
    {
        g_free(sink->host);
        sink->host = g_strdup(host);
        GST_DEBUG_OBJECT(sink, "New host set: \"%s\".", sink->host);
    }

    g_mutex_unlock(sink->props_lock);
    return retVal;
}

static gboolean
gst_net_sink_set_uri(GstNetSink* sink, const gchar* uri)
{
    gboolean retVal = FALSE;
    gchar *p, *host = NULL;
    gchar *protocol = gst_uri_get_protocol(uri);

    if (NULL != protocol)
    {
        if (strcmp(protocol, "udp") == 0)
        {
            if (NULL != (host = gst_uri_get_location(uri)))
            {
                g_mutex_lock(sink->props_lock);

                if(NULL == sink->uri || 0 != strcmp(sink->uri, uri))
                {
                    g_free(sink->uri);
                    sink->uri = g_strdup(uri);
                    GST_DEBUG_OBJECT(sink, "New URI set: \"%s\".", sink->uri);
                }

                if (NULL != (p = strchr(host, ':')))
                {
                    *p = 0; // so that the host is null terminated where the address ends.
                    sink->port = atoi(++p);
                    GST_DEBUG_OBJECT(sink, "Port retrieved: \"%d\".", sink->port);
                }
                g_mutex_unlock(sink->props_lock);
                retVal = gst_net_sink_set_host(sink, host);
                g_free(host);
            }
        }
        else
        {
            GST_ELEMENT_ERROR(sink, RESOURCE, READ, (NULL),
                ("error parsing URI %s: %s != udp", uri, protocol));
        }

        g_free(protocol);
    }

    return retVal;
}

static void gst_net_sink_set_property(GObject* object,
                                      guint prop_id,
                                      const GValue* value,
                                      GParamSpec* pspec)
{
    GstNetSink* sink = GST_NETSINK(object);

    switch (prop_id)
    {
    case PROP_URI:
    {
        (void)gst_net_sink_set_uri(sink, g_value_get_string(value));
        break;
    }
    case PROP_HOST:
    {
        (void)gst_net_sink_set_host(sink, g_value_get_string(value));
        break;
    }
    case PROP_PORT:
        g_mutex_lock(sink->props_lock);
        sink->port = g_value_get_uint(value);
        GST_DEBUG_OBJECT(sink, "New port set: %u.", sink->port);
        g_mutex_unlock(sink->props_lock);
        break;
    case PROP_BLKSIZE:
        g_mutex_lock(sink->props_lock);
        sink->blksize = g_value_get_uint(value);
        GST_DEBUG_OBJECT(sink, "New blksize set: %u.", sink->blksize);
        g_mutex_unlock(sink->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_net_sink_get_property(GObject* object,
                                      guint prop_id,
                                      GValue* value,
                                      GParamSpec* pspec)
{
    GstNetSink* sink = GST_NETSINK(object);

    switch (prop_id)
    {
    case PROP_URI:
        g_mutex_lock(sink->props_lock);
        g_value_set_string(value, sink->uri);
        g_mutex_unlock(sink->props_lock);
        break;
    case PROP_HOST:
        g_mutex_lock(sink->props_lock);
        g_value_set_string(value, sink->host);
        g_mutex_unlock(sink->props_lock);
        break;
    case PROP_PORT:
        g_mutex_lock(sink->props_lock);
        g_value_set_uint(value, sink->port);
        g_mutex_unlock(sink->props_lock);
        break;
    case PROP_BLKSIZE:
        g_mutex_lock(sink->props_lock);
        g_value_set_uint(value, sink->blksize);
        g_mutex_unlock(sink->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/***********************************************/
/**********                           **********/
/********** GstBaseSink IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

/* create a socket for sending to remote machine */
static gboolean
gst_net_sink_start(GstBaseSink *bsink)
{
    GstNetSink *sink = GST_NETSINK(bsink);
    char portStr[8] = {0};
    struct addrinfo hints = {0};
    struct addrinfo* srvrInfo = NULL;
    struct addrinfo* pSrvr = NULL;
    int yes = 1;
    int ret = 0;
#ifdef RI_WIN32_SOCKETS
    WSADATA wsd;

    if (WSAStartup(MAKEWORD(2, 2), &wsd))
    {
        GST_ERROR_OBJECT(sink, "%s WSAStartup() failed?\n", __func__);
        return FALSE;
    }
#endif

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_DGRAM;
    snprintf(portStr, sizeof(portStr), "%d", sink->port);

    if (0 != (ret = getaddrinfo(sink->host, portStr, &hints, &srvrInfo)))
    {
        GST_ERROR_OBJECT(sink, "%s getaddrinfo[%s]\n", __func__,
                         gai_strerror(ret));
        return FALSE;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        if (0 > (sink->sock = socket(pSrvr->ai_family,
                                     pSrvr->ai_socktype,
                                     pSrvr->ai_protocol)))
        {
            GST_ERROR_OBJECT(sink, "%s socket() failed?\n", __func__);
            continue;
        }

        if (0 > setsockopt(sink->sock, SOL_SOCKET, SO_REUSEADDR,
                           (char*) &yes, sizeof(yes)))
        {
            GST_ERROR_OBJECT(sink, "%s setsockopt() failed?\n", __func__);
            return FALSE;
        }

        GST_INFO_OBJECT(sink, "%s got sock: %d\n", __func__, sink->sock);

        if (0 > (connect(sink->sock, pSrvr->ai_addr, pSrvr->ai_addrlen)))
        {
            CLOSESOCK(sink->sock);
            GST_ERROR_OBJECT(sink, "%s connect() failed?\n", __func__);
            continue;
        }

        // We successfully connected!
        break;
    }

    if (NULL == pSrvr)
    {
        GST_ERROR_OBJECT(sink, "%s failed to connect\n", __func__);
        freeaddrinfo(srvrInfo);
        return FALSE;
    }

    freeaddrinfo(srvrInfo);
    return TRUE;
}

static gboolean
gst_net_sink_stop(GstBaseSink * bsink)
{
    GstNetSink *sink = GST_NETSINK(bsink);

    GST_INFO_OBJECT(sink, "%s", __func__);

    if (sink->sock >= 0)
        CLOSESOCK(sink->sock);

    return TRUE;
}


/**********************************************/
/**********                          **********/
/********** GstNetSink IMPLEMENTATION **********/
/**********                          **********/
/**********************************************/


static GstFlowReturn
gst_net_sink_render(GstBaseSink *psink, GstBuffer *buf)
{
    GstNetSink *sink = GST_NETSINK(psink);
    int bytesTxd = 0;
    int bytesToTx = GST_BUFFER_SIZE(buf);
    unsigned char *txBuf = GST_BUFFER_DATA(buf);

    if ((bytesTxd = send(sink->sock, txBuf, bytesToTx, 0)) < -1)
    {
        GST_ERROR_OBJECT(sink, "%s %d = send() failed?\n", __func__, bytesTxd);
        return GST_FLOW_ERROR;
    }
    else if (bytesTxd == -1)
    {
        GST_LOG_OBJECT(sink, "%s 0 = send() not connected?\n", __func__);
    }
    else if (bytesTxd != sink->blksize)
    {
        GST_WARNING_OBJECT(sink, "%s sent %d bytes instead of %d",
                           __func__, bytesTxd, sink->blksize);
    }

    return GST_FLOW_OK;
}


/*********************************************/
/**********                         **********/
/********** GstUriHandler INTERFACE **********/
/**********                         **********/
/*********************************************/

static GstURIType
gst_net_sink_uri_get_type(void)
{
    return GST_URI_SINK;
}

static gchar **
gst_net_sink_uri_get_protocols(void)
{
    static gchar *protocols[] = { "udp", NULL };
    return protocols;
}

static const gchar *
gst_net_sink_uri_get_uri(GstURIHandler* handler)
{
    GstNetSink* sink = GST_NETSINK(handler);
    return sink->uri;
}

static gboolean
gst_net_sink_uri_set_uri(GstURIHandler* handler, const gchar* uri)
{
    GstNetSink* sink = GST_NETSINK(handler);
    return gst_net_sink_set_uri(sink, uri);
}

static void
gst_net_sink_uri_handler_init(gpointer g_iface, gpointer iface_data)
{
  GstURIHandlerInterface *iface = (GstURIHandlerInterface *)g_iface;

  iface->get_type = gst_net_sink_uri_get_type;
  iface->get_protocols = gst_net_sink_uri_get_protocols;
  iface->get_uri = gst_net_sink_uri_get_uri;
  iface->set_uri = gst_net_sink_uri_set_uri;
}

