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
 * SECTION:element-netsrc
 *
 * FIXME:Describe netsrc here.
 *
 * <refsect2>
 * <title>Example launch line</title>
 * |[
 * gst-launch -v -m netsrc ! fakesink silent=TRUE
 * ]|
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <stdlib.h>
#include <string.h> // memcpy
#include <sys/stat.h>
#include <gst/gst.h>

#include "gstpacedfilesrc.h"
#include "gstnetsrc.h"

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


GST_DEBUG_CATEGORY_STATIC(gst_net_src_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT gst_net_src_debug

enum
{
    PROP_0,
    PROP_URI,
    PROP_ADDR,
    PROP_PORT,
    PROP_BLKSIZE,
    PROP_LAST,
};

static GstStaticPadTemplate src_factory = GST_STATIC_PAD_TEMPLATE("src",
                                            GST_PAD_SRC,
                                            GST_PAD_ALWAYS,
                                            GST_STATIC_CAPS_ANY);

// Forward declarations
static gboolean gst_net_src_set_uri(GstNetSrc* src, const gchar* uri);
static void gst_net_src_uri_handler_init(gpointer, gpointer);
static void gst_net_src_dispose(GObject* object);
static void gst_net_src_finalize(GObject* object);

static void gst_net_src_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec);
static void gst_net_src_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec);

static gboolean gst_net_src_start(GstBaseSrc* basesrc);
static gboolean gst_net_src_stop(GstBaseSrc* basesrc);

static GstFlowReturn gst_net_src_create(GstPushSrc* src, GstBuffer** buf);

static void
_do_init(GType type)
{
    static const GInterfaceInfo urihandler_info =
    {
        gst_net_src_uri_handler_init,
        NULL,
        NULL
    };

    g_type_add_interface_static(type, GST_TYPE_URI_HANDLER, &urihandler_info);
    GST_DEBUG_CATEGORY_INIT(gst_net_src_debug, "netsrc", 0, "network src");
}

/*lint -e(123)*/GST_BOILERPLATE_FULL(GstNetSrc, gst_net_src,
                                     GstPushSrc, GST_TYPE_PUSH_SRC, _do_init)

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

static void gst_net_src_base_init(gpointer gclass)
{
    GstElementClass* element_class = GST_ELEMENT_CLASS(gclass);

    gst_element_class_set_details_simple(element_class, "NetSrc",
            "FIXME:Generic", "FIXME:Generic Template Element",
            " <<user@hostname.org>>");

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(&src_factory));
}

/* initialize the netsrc's class */
static void gst_net_src_class_init(GstNetSrcClass* klass)
{
    GObjectClass* gobject_class;
    GstBaseSrcClass* gstbasesrc_class;
    GstPushSrcClass* gstpushsrc_class;


    gobject_class = (GObjectClass *) klass;
    gstbasesrc_class = (GstBaseSrcClass *) klass;
    gstpushsrc_class = (GstPushSrcClass *) klass;

    parent_class = g_type_class_peek_parent(klass);

    gobject_class->set_property = gst_net_src_set_property;
    gobject_class->get_property = gst_net_src_get_property;

    g_object_class_install_property(gobject_class, PROP_URI,
            g_param_spec_string("uri", "URI",
                    "URI in the form of udp://addr:port", DEFAULT_RX_URI,
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_ADDR,
            g_param_spec_string("address", "address or group",
                    "Address or group to join", DEFAULT_RX_ADDR,
                    G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

    g_object_class_install_property(gobject_class, PROP_PORT,
            g_param_spec_uint("port", "network port",
                    "network port to listen on for packets", DEFAULT_RX_PORT,
                    65535, DEFAULT_RX_PORT, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, PROP_BLKSIZE,
            g_param_spec_uint("blksize", "Generated GstBuffer size",
                    "Size in bytes to read per buffer", MIN_BLKSIZE,
                    MAX_BLKSIZE, DEFAULT_BLKSIZE, G_PARAM_READWRITE));

    gobject_class->dispose = GST_DEBUG_FUNCPTR(gst_net_src_dispose);
    gobject_class->finalize = GST_DEBUG_FUNCPTR(gst_net_src_finalize);
    gstbasesrc_class->start = GST_DEBUG_FUNCPTR(gst_net_src_start);
    gstbasesrc_class->stop = GST_DEBUG_FUNCPTR(gst_net_src_stop);
    gstpushsrc_class->create = GST_DEBUG_FUNCPTR(gst_net_src_create);
}

/* initialize the new element
 * instantiate pads and add them to element
 * set pad calback functions
 * initialize instance structure
 */
static void gst_net_src_init(GstNetSrc* src, GstNetSrcClass* gclass)
{
    src->props_lock = g_mutex_new();

    gst_net_src_set_uri(src, DEFAULT_RX_URI);   // sets uri and addr
    src->port = DEFAULT_RX_PORT;
    src->blksize = DEFAULT_BLKSIZE;
    src->sock = -1;

    gst_base_src_set_live(GST_BASE_SRC(src), TRUE);
    gst_base_src_set_format(GST_BASE_SRC(src), GST_FORMAT_TIME);
    gst_base_src_set_do_timestamp(GST_BASE_SRC(src), TRUE);
}

static void gst_net_src_dispose(GObject* object)
{
    G_OBJECT_CLASS(parent_class)->dispose(object);
}

static void gst_net_src_finalize(GObject* object)
{
    GstNetSrc* src = GST_NETSRC(object);

    g_mutex_free(src->props_lock);
    g_free(src->uri);
    g_free(src->addr);

    if (src->sock >= 0)
        CLOSESOCK(src->sock);

#ifdef RI_WIN32_SOCKETS
    WSACleanup();
#endif

    G_OBJECT_CLASS(parent_class)->finalize(object);
}

static gboolean
gst_net_src_set_addr(GstNetSrc* src, const gchar* addr)
{
    gboolean retVal = FALSE;
    g_mutex_lock(src->props_lock);

    if(NULL == src->addr || 0 != strcmp(src->addr, addr))
    {
        g_free(src->addr);
        src->addr = g_strdup(addr);
        GST_DEBUG_OBJECT(src, "New addr set: \"%s\".", src->addr);
    }

    g_mutex_unlock(src->props_lock);
    return retVal;
}

static gboolean
gst_net_src_set_uri(GstNetSrc* src, const gchar* uri)
{
    gboolean retVal = FALSE;
    gchar *p, *addr = NULL;
    gchar *protocol = gst_uri_get_protocol(uri);

    if (NULL != protocol)
    {
        if (strcmp(protocol, "udp") == 0)
        {
            if (NULL != (addr = gst_uri_get_location(uri)))
            {
                g_mutex_lock(src->props_lock);

                if(NULL == src->uri || 0 != strcmp(src->uri, uri))
                {
                    g_free(src->uri);
                    src->uri = g_strdup(uri);
                    GST_DEBUG_OBJECT(src, "New URI set: \"%s\".", src->uri);
                }

                if (NULL != (p = strchr(addr, ':')))
                {
                    *p = 0; // so that the addr is null terminated where the address ends.
                    src->port = atoi(++p);
                    GST_DEBUG_OBJECT(src, "Port retrieved: \"%d\".", src->port);
                }
                g_mutex_unlock(src->props_lock);
                retVal = gst_net_src_set_addr(src, addr);
                g_free(addr);
            }
        }
        else
        {
            GST_ELEMENT_ERROR(src, RESOURCE, READ, (NULL),
                ("error parsing URI %s: %s != udp", uri, protocol));
        }

        g_free(protocol);
    }

    return retVal;
}

static void gst_net_src_set_property(GObject* object,
                                            guint prop_id,
                                            const GValue* value,
                                            GParamSpec* pspec)
{
    GstNetSrc* src = GST_NETSRC(object);

    switch (prop_id)
    {
    case PROP_URI:
    {
        (void)gst_net_src_set_uri(src, g_value_get_string(value));
        break;
    }
    case PROP_ADDR:
    {
        (void)gst_net_src_set_addr(src, g_value_get_string(value));
        break;
    }
    case PROP_PORT:
        g_mutex_lock(src->props_lock);
        src->port = g_value_get_uint(value);
        GST_DEBUG_OBJECT(src, "New port set: %u.", src->port);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_BLKSIZE:
        g_mutex_lock(src->props_lock);
        src->blksize = g_value_get_uint(value);
        GST_DEBUG_OBJECT(src, "New blksize set: %u.", src->blksize);
        g_mutex_unlock(src->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

static void gst_net_src_get_property(GObject* object,
                                            guint prop_id,
                                            GValue* value,
                                            GParamSpec* pspec)
{
    GstNetSrc* src = GST_NETSRC(object);

    switch (prop_id)
    {
    case PROP_URI:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->uri);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_ADDR:
        g_mutex_lock(src->props_lock);
        g_value_set_string(value, src->addr);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_PORT:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->port);
        g_mutex_unlock(src->props_lock);
        break;
    case PROP_BLKSIZE:
        g_mutex_lock(src->props_lock);
        g_value_set_uint(value, src->blksize);
        g_mutex_unlock(src->props_lock);
        break;
    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/***********************************************/
/**********                           **********/
/********** GstBaseSrc IMPLEMENTATION **********/
/**********                           **********/
/***********************************************/

/* create a socket for sending to remote machine */
static gboolean
gst_net_src_start(GstBaseSrc *bsrc)
{
    GstNetSrc *src = GST_NETSRC(bsrc);
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
        GST_ERROR_OBJECT(src, "%s WSAStartup() failed?\n", __func__);
        return FALSE;
    }
#endif

    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_DGRAM;
    snprintf(portStr, sizeof(portStr), "%d", src->port);

    if (0 != (ret = getaddrinfo(src->addr, portStr, &hints, &srvrInfo)))
    {
        GST_ERROR_OBJECT(src, "%s getaddrinfo[%s]\n", __func__,
                         gai_strerror(ret));
        return FALSE;
    }

    for(pSrvr = srvrInfo; pSrvr != NULL; pSrvr = pSrvr->ai_next)
    {
        if (0 > (src->sock = socket(pSrvr->ai_family,
                                    pSrvr->ai_socktype,
                                    pSrvr->ai_protocol)))
        {
            GST_ERROR_OBJECT(src, "%s socket() failed?\n", __func__);
            continue;
        }

        if (0 > setsockopt(src->sock, SOL_SOCKET, SO_REUSEADDR,
                           (char*) &yes, sizeof(yes)))
        {
            GST_ERROR_OBJECT(src, "%s setsockopt() failed?\n", __func__);
            return FALSE;
        }

        GST_INFO_OBJECT(src, "%s got sock: %d\n", __func__, src->sock);

        if (0 > (bind(src->sock, pSrvr->ai_addr, pSrvr->ai_addrlen)))
        {
            CLOSESOCK(src->sock);
            GST_ERROR_OBJECT(src, "%s bind() failed?\n", __func__);
            continue;
        }

        // We successfully bound!
        break;
    }

    if (NULL == pSrvr)
    {
        GST_ERROR_OBJECT(src, "%s failed to bind\n", __func__);
        freeaddrinfo(srvrInfo);
        return FALSE;
    }

    freeaddrinfo(srvrInfo);
    return TRUE;
}

static gboolean
gst_net_src_stop(GstBaseSrc * bsrc)
{
    GstNetSrc *src = GST_NETSRC(bsrc);

    GST_INFO_OBJECT(src, "%s", __func__);

    if (src->sock >= 0)
        CLOSESOCK(src->sock);

    return TRUE;
}


/**********************************************/
/**********                          **********/
/********** GstNetSrc IMPLEMENTATION **********/
/**********                          **********/
/**********************************************/


static GstFlowReturn
gst_net_src_create(GstPushSrc *psrc, GstBuffer **buf)
{
    GstNetSrc *src = GST_NETSRC(psrc);
    int bytesRcvd = 0;

    GstBuffer *outbuf = gst_buffer_new_and_alloc(src->blksize+1);
    unsigned char *rxBuf = GST_BUFFER_DATA(outbuf);

    if ((bytesRcvd = recv(src->sock, rxBuf, src->blksize, 0)) <= 0)
    {
        GST_ERROR_OBJECT(src, "%s recv() failed?\n", __func__);
        return GST_FLOW_ERROR;
    }

    GST_BUFFER_SIZE(outbuf) = bytesRcvd;

    if (bytesRcvd != src->blksize)
    {
        GST_WARNING_OBJECT(src, "%s read %d bytes instead of %d",
                           __func__, bytesRcvd, src->blksize);
    }

    *buf = outbuf;
    return GST_FLOW_OK;
}


/*********************************************/
/**********                         **********/
/********** GstUriHandler INTERFACE **********/
/**********                         **********/
/*********************************************/

static GstURIType
gst_net_src_uri_get_type(void)
{
    return GST_URI_SRC;
}

static gchar **
gst_net_src_uri_get_protocols(void)
{
    static gchar *protocols[] = { "udp", NULL };
    return protocols;
}

static const gchar *
gst_net_src_uri_get_uri(GstURIHandler* handler)
{
    GstNetSrc* src = GST_NETSRC(handler);
    return src->uri;
}

static gboolean
gst_net_src_uri_set_uri(GstURIHandler* handler, const gchar* uri)
{
    GstNetSrc* src = GST_NETSRC(handler);
    return gst_net_src_set_uri(src, uri);
}

static void
gst_net_src_uri_handler_init(gpointer g_iface, gpointer iface_data)
{
  GstURIHandlerInterface *iface = (GstURIHandlerInterface *)g_iface;

  iface->get_type = gst_net_src_uri_get_type;
  iface->get_protocols = gst_net_src_uri_get_protocols;
  iface->get_uri = gst_net_src_uri_get_uri;
  iface->set_uri = gst_net_src_uri_set_uri;
}

