/* GStreamer
 * Copyright (C) <2005> Julien Moutte <julien@moutte.net>
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

/**************************************************************************
 * ************************************************************************
 * NOTE:  The following comments are from the original xvimagesink plugin code
 * on which this code is based.  Once this code module is completed, these
 * comments should be modified to reflect the actual behavior of this plugin.
 * ************************************************************************
 **************************************************************************/

/**
 * SECTION:element-xvimagesink
 *
 * <refsect2>
 * <para>
 * XvImageSink renders video frames to a drawable (XWindow) on a local display
 * using the XVideo extension. Rendering to a remote display is theorically
 * possible but i doubt that the XVideo extension is actually available when
 * connecting to a remote display. This element can receive a Window ID from the
 * application through the XOverlay interface and will then render video frames
 * in this drawable. If no Window ID was provided by the application, the
 * element will create its own internal window and render into it.
 * </para>
 * <title>Scaling</title>
 * <para>
 * The XVideo extension, when it's available, handles hardware accelerated
 * scaling of video frames. This means that the element will just accept
 * incoming video frames no matter their geometry and will then put them to the
 * drawable scaling them on the fly. Using the
 * <link linkend="GstXvImageSink--force-aspect-ratio">force-aspect-ratio</link>
 * property it is possible to enforce scaling with a constant aspect ratio,
 * which means drawing black borders around the video frame.
 * </para>
 * <title>Events</title>
 * <para>
 * XvImageSink creates a thread to handle events coming from the drawable. There
 * are several kind of events that can be grouped in 2 big categories: input
 * events and window state related events. Input events will be translated to
 * navigation events and pushed upstream for other elements to react on them.
 * This includes events such as pointer moves, key press/release, clicks etc...
 * Other events are used to handle the drawable appearance even when the data
 * is not flowing (GST_STATE_PAUSED). That means that even when the element is
 * paused, it will receive expose events from the drawable and draw the latest
 * frame with correct borders/aspect-ratio.
 * </para>
 * <title>Pixel aspect ratio</title>
 * <para>
 * When changing state to GST_STATE_READY, XvImageSink will open a connection to
 * the display specified in the
 * <link linkend="GstXvImageSink--display">display</link> property or the
 * default display if nothing specified. Once this connection is open it will
 * inspect the display configuration including the physical display geometry and
 * then calculate the pixel aspect ratio. When receiving video frames with a
 * different pixel aspect ratio, XvImageSink will use hardware scaling to
 * display the video frames correctly on display's pixel aspect ratio.
 * Sometimes the calculated pixel aspect ratio can be wrong, it is
 * then possible to enforce a specific pixel aspect ratio using the
 * <link linkend="GstXvImageSink--pixel-aspect-ratio">pixel-aspect-ratio</link>
 * property.
 * </para>
 * <title>Examples</title>
 * <para>
 * Here is a simple pipeline to test hardware scaling :
 * <programlisting>
 * gst-launch -v videotestsrc ! xvimagesink
 * </programlisting>
 * When the test video signal appears you can resize the window and see that
 * video frames are scaled through hardware (no extra CPU cost). You can try
 * again setting the force-aspect-ratio property to true and observe the borders
 * drawn around the scaled image respecting aspect ratio.
 * <programlisting>
 * gst-launch -v videotestsrc ! xvimagesink force-aspect-ratio=true
 * </programlisting>
 * </para>
 * <para>
 * Here is a simple pipeline to test navigation events :
 * <programlisting>
 * gst-launch -v videotestsrc ! navigationtest ! xvimagesink
 * </programlisting>
 * While moving the mouse pointer over the test signal you will see a black box
 * following the mouse pointer. If you press the mouse button somewhere on the
 * video and release it somewhere else a green box will appear where you pressed
 * the button and a red one where you released it. (The navigationtest element
 * is part of gst-plugins-good.) You can observe here that even if the images
 * are scaled through hardware the pointer coordinates are converted back to the
 * original video frame geometry so that the box can be drawn to the correct
 * position. This also handles borders correctly, limiting coordinates to the
 * image area
 * </para>
 * <para>
 * Here is a simple pipeline to test pixel aspect ratio :
 * <programlisting>
 * gst-launch -v videotestsrc ! video/x-raw-yuv, pixel-aspect-ratio=(fraction)4/3 ! xvimagesink
 * </programlisting>
 * This is faking a 4/3 pixel aspect ratio caps on video frames produced by
 * videotestsrc, in most cases the pixel aspect ratio of the display will be
 * 1/1. This means that XvImageSink will have to do the scaling to convert
 * incoming frames to a size that will match the display pixel aspect ratio
 * (from 320x240 to 320x180 in this case). Note that you might have to escape
 * some characters for your shell like '\(fraction\)'.
 * </para>
 * <para>
 * Here is a test pipeline to test the colorbalance interface :
 * <programlisting>
 * gst-launch -v videotestsrc ! xvimagesink hue=100 saturation=-100 brightness=100
 * </programlisting>
 * </para>
 * </refsect2>
 */

//
//
// command line debug options (so I don't forget them):
//  --gst-debug=ffmpeg:5,display:5,videotestsrc:5
//
//


#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstdisplay.h"

#include "ui_opengl_common.h"
#include "ui_window_common.h"

#include <gst/video/video.h>
#include <gst/gstinfo.h>
#include <gst/interfaces/navigation.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>

// *TODO* - can this really be hardcoded???
// Declares the number of bytes per pixel for RGB color formats
// used by the yuv->rgb conversion routines
//
#define RGB_BYTES_PER_PIXEL 3

// Debugging category
//
GST_DEBUG_CATEGORY( display_debug);
#define /*lint -e(652)*/ GST_CAT_DEFAULT display_debug

// ****
// Display Plugin methods
//
static GstStateChangeReturn gst_display_change_state(GstElement * element,
        GstStateChange transition);
static GstFlowReturn gst_display_buffer_alloc(GstBaseSink* bsink,
        guint64 offset, guint size, GstCaps* caps, GstBuffer** buf);
static GstFlowReturn gst_display_show_frame(GstBaseSink* bsink, GstBuffer* buf);
static gboolean gst_display_image_put(GstDisplay * display,
        GstDisplayImageBuffer * image);
static void gst_display_expose(GstXOverlay * overlay);
static void gst_display_navigation_send_event(GstNavigation * navigation,
        GstStructure * structure);
static gboolean gst_display_event(GstBaseSink* bsink, GstEvent * event);

// ****
// Initialization methods
//
static void gst_display_base_init(gpointer g_class);
static void gst_display_class_init(GstDisplay * klass);
static void gst_display_interface_init(GstImplementsInterfaceClass * klass);
static void gst_display_navigation_init(GstNavigationInterface * iface);
static void gst_display_overlay_init(GstXOverlayClass* iface);
static void gst_display_instance_init(GstDisplay* display);
static gboolean gst_display_set_caps(GstBaseSink* bsink, GstCaps* caps);
static gboolean gst_display_process_caps(GstDisplay* display, GstCaps* caps);
static gboolean gst_display_process_structure(GstDisplay* display,
        GstStructure *structure);
static gboolean gst_display_verify_dimensions(GstDisplay* display);
static void
        gst_display_window_new(GstDisplay* display, gint width, gint height);

// ****
// Update & Reset Methods
//
static void gst_display_info_clear(GstDisplay * display);
static void gst_display_imagepool_clear(GstDisplay * display);
static void gst_display_set_property(GObject* object, guint prop_id,
        const GValue* value, GParamSpec* pspec);

// ****
// Retrieval & Info Methods
//
static GstCaps* gst_display_get_caps(GstBaseSink * bsink);
static void gst_display_info_get(GstDisplay * display);
static void gst_display_get_times(GstBaseSink * bsink, GstBuffer * buf,
        GstClockTime * start, GstClockTime * end);
static gboolean gst_display_interface_supported(GstImplementsInterface* iface,
        GType type);
static void gst_display_get_property(GObject* object, guint prop_id,
        GValue* value, GParamSpec* pspec);

// ****
// Destruction & Cleanup methods
//
static void gst_display_window_destroy(GstDisplay* display);
static void gst_display_finalize(GObject * object);

// ****
// Utility functions
//
static void gst_display_pixel_aspect_ratio_calculate(DisplayInfo* displayInfo);
static void gst_display_video_dfc_adjustment(GstDisplay* display,
        guint adjustedWindowWidth, guint adjustedWindowHeight);
static void gst_display_video_dfc_pillarbox(GstDisplay* display,
        guint windowWidth, guint windowHeight);
static void gst_display_video_dfc_full(GstDisplay* display, guint windowWidth,
        guint windowHeight);
static void gst_display_video_dfc_letterbox(GstDisplay* display,
        guint windowWidth, guint windowHeight);
static void gst_display_video_dfc_zoom(GstDisplay* display, guint windowWidth,
        guint windowHeight);
static void gst_display_video_dfc_cco(GstDisplay* display, guint windowWidth,
        guint windowHeight);
static void gst_display_video_dfc_panscan(GstDisplay* display,
        guint windowWidth, guint windowHeight);
static void gst_display_graphics_aspect_ratio_adjust(GstDisplay* display);
static void gst_display_keep_aspect(GstDisplay* display);

// Element details for the image buffer information
//
static const GstElementDetails gst_display_image_buffer_details =
        GST_ELEMENT_DETAILS("Display Image Buffer", "Sink/Video",
                "An OpenGL based videosink",
                "Julien Moutte <julien@moutte.net>");

// Description of the image buffer since it is a pad.
// Describes the type of data which this image buffer will accept.
//
static GstStaticPadTemplate gst_display_image_buffer_template_factory =
        GST_STATIC_PAD_TEMPLATE("sink", GST_PAD_SINK, GST_PAD_ALWAYS,

        GST_STATIC_CAPS("video/x-raw-rgb, "
            "framerate = (fraction) [ 0, MAX ], "
            "width = (int) [ 1, MAX ], "
            "height = (int) [ 1, MAX ]; "
            "video/x-raw-yuv, "
            "framerate = (fraction) [ 0, MAX ], "
            "width = (int) [ 1, MAX ], " "height = (int) [ 1, MAX ]"));

// Enumeration of properties accessible at the application level
//
enum
{
    ARG_0,
    ARG_BACKGROUND_COLOR_RGBA8888,
    ARG_WINDOW_WIDTH,
    ARG_WINDOW_HEIGHT,
    ARG_INCOMING_VIDEO_WIDTH,
    ARG_INCOMING_VIDEO_HEIGHT,
    ARG_INCOMING_VIDEO_PAR_X,
    ARG_INCOMING_VIDEO_PAR_Y,
    ARG_OUTPUT_VIDEO_WIDTH,
    ARG_OUTPUT_VIDEO_HEIGHT,
    ARG_OUTPUT_VIDEO_PAR_X,
    ARG_OUTPUT_VIDEO_PAR_Y,
    ARG_SCALED_VIDEO_SRC_X,
    ARG_SCALED_VIDEO_SRC_Y,
    ARG_SCALED_VIDEO_SRC_WIDTH,
    ARG_SCALED_VIDEO_SRC_HEIGHT,
    ARG_SCALED_VIDEO_DEST_X,
    ARG_SCALED_VIDEO_DEST_Y,
    ARG_SCALED_VIDEO_DEST_WIDTH,
    ARG_SCALED_VIDEO_DEST_HEIGHT,
    ARG_GRAPHICS_SURFACE,
    ARG_HW_ACCELERATION_DISABLED,
    ARG_PIXEL_ASPECT_RATIO,
    ARG_FORCE_ASPECT_RATIO,
    ARG_FIXED_WINDOW,
    ARG_SUPPLIED_WINDOW,
    ARG_DFC_VALUE,
    ARG_DFC_DEFAULT,
    ARG_FORCE_RGB_VIDEO,
    ARG_AFD_VALUE,
//  ARG_HANDLE_EVENTS,
/* FILL ME */
};

// Flag to indicate that a service change is in progress.
// This flag is set to false during the start of a tune, and
// is set to true when a newSegment event is received.
// It is used to determine when video size parameters have
// been set for a service change; they are set before
// the newSegment event is received.
gboolean g_newSegmentEventReceived = FALSE;
GTimeVal g_tuneStartTime = {0, 0};


// Parent class of GstDisplay
//
static GstVideoSinkClass *parent_class = NULL;

/* =========================================== */
/*                                             */
/*           Display Plugin Methods            */
/*                                             */
/* =========================================== */

/**
 * Called by the GStreamer infrastructure to update this plugin's state.
 */
static GstStateChangeReturn gst_display_change_state(GstElement * element,
        GstStateChange transition)
{
    GST_DEBUG("function entry");

    GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;
    GstDisplay *display;
    DisplayInfo *displayInfo = NULL;
    GstClock* gstSystemClk = NULL;
    GstClockTime tmp, internal, external, rate_num, rate_denom;
    long int difference = 0;
    struct timeval tv;

    display = GST_DISPLAY(element);

    switch (transition)
    {
    case GST_STATE_CHANGE_NULL_TO_READY:
        GST_DEBUG("state changing NULL to READY");
        // Initialize the display information
        if (display->pUIInfo->pDisplayInfo == NULL)
        {
            gst_display_info_get(display);
            if (NULL == display->pUIInfo->pDisplayInfo)
            {
                return GST_STATE_CHANGE_FAILURE;
            }
        }

        GST_OBJECT_LOCK(display);

        // Set the plugin running flag to indicate plugin is no longer in NULL state
        display->running = TRUE;

        if (displayInfo)
        {
            display->pUIInfo->pDisplayInfo = displayInfo;
        }
        GST_OBJECT_UNLOCK(display);

        break;

    case GST_STATE_CHANGE_READY_TO_PAUSED:
        GST_DEBUG("state changing READY to PAUSED");

        // If there is an active window, flush out current graphics
        if ((NULL != display->pUIInfo->pWindowInfo) && (0
                != display->pUIInfo->pWindowInfo->win))
        {
            window_flush_graphics(display->pUIInfo->pWindowInfo);
        }
        break;

    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
        GST_DEBUG("state changing PAUSED to PLAYING");
        break;

    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
        GST_DEBUG("state changing PLAYING to PAUSED");
        break;

    case GST_STATE_CHANGE_PAUSED_TO_READY:
        GST_DEBUG("state changing PAUSED to READY");
        break;

    case GST_STATE_CHANGE_READY_TO_NULL:
        GST_DEBUG("state changing READY to NULL");
        break;

    default:
        GST_DEBUG("non-case state");
        break;
    }

    ret = GST_ELEMENT_CLASS(parent_class)->change_state(element, transition);

    switch (transition)
    {
    case GST_STATE_CHANGE_NULL_TO_READY:
        GST_DEBUG("transition NULL to READY");
        break;

    case GST_STATE_CHANGE_READY_TO_PAUSED:
        GST_DEBUG("transition READY to PAUSED");
        break;

    case GST_STATE_CHANGE_PAUSED_TO_PLAYING:
        GST_DEBUG("transition PAUSED to PLAYING");
        break;

    case GST_STATE_CHANGE_PLAYING_TO_PAUSED:
        GST_DEBUG("transition PLAYING to READY");
        break;

    case GST_STATE_CHANGE_PAUSED_TO_READY:
        GST_DEBUG("transition PAUSED to READY");
        display->fps_n = 0;
        display->fps_d = 1;

        // *TODO* -what does this do????
        GST_VIDEO_SINK_WIDTH(display) = 0;
        GST_VIDEO_SINK_HEIGHT(display) = 0;
        break;

    case GST_STATE_CHANGE_READY_TO_NULL:
        GST_DEBUG("transition READY to NULL");
        gst_display_reset(display);
        break;

    default:
        GST_DEBUG("non-case transition");
        break;
    }

    gstSystemClk = gst_system_clock_obtain();
    gst_clock_get_calibration(gstSystemClk,
                              &internal, &external, &rate_num, &rate_denom);
    GST_INFO("GST int: %llu, ext: %llu, num: %llu, denom: %llu",
                              internal, external, rate_num, rate_denom);
    internal = gst_clock_get_time(gstSystemClk);
    gettimeofday(&tv, NULL);
    external = tv.tv_sec;
    external *= 1000000000;
    tmp = tv.tv_usec;
    tmp *= 1000;
    external += tmp;
    difference = internal - external;
    GST_WARNING("GST clockTime: %llu, sysTime: %llu, drift: %li nS",
                internal, external, difference);
#ifdef ADJUST_FOR_DRIFT
    // If we've drifted over a half second - adjust...
    if (abs(difference) > 500000000)
    {
        gst_clock_set_calibration(gstSystemClk,
                                  internal, external, rate_num, rate_denom);
        GST_WARNING("New GST clockTime:%llu", gst_clock_get_time(gstSystemClk));
    }
#endif
    return ret;
}

/**
 * Displays a video frame.
 *
 * This function is called by the GStreamer infrastructure to display a video frame.
 *
 * *TODO* - try to understand how plugin gets a buffer which isn't an image buffer type.
 * @param bsink this plugin instance
 * @param buf points to the image buffer to be displayed
 */
static GstFlowReturn gst_display_show_frame(GstBaseSink* bsink, GstBuffer* buf)
{
    //GST_DEBUG("function entry");
    GstDisplay *display;
    display = GST_DISPLAY(bsink);

    // Need to mess with image buffer so get flow lock
    g_mutex_lock(display->pUIInfo->flow_lock);

    // If this buffer has been allocated using our buffer management we simply
    // put the image which is in the PRIVATE pointer
    if (GST_IS_DISPLAY_IMAGE_BUFFER(buf))
    {
        //GST_DEBUG_OBJECT(display, "fast put of bufferpool buffer");
        if (!gst_display_image_put(display, GST_DISPLAY_IMAGE_BUFFER(buf)))
        {
            g_mutex_unlock(display->pUIInfo->flow_lock);

            // Report an error if the window should have been created by now
            if (FALSE == display->pUIInfo->pWindowInfo->is_supplied)
            {
                // No Window available to put our image into
                GST_WARNING_OBJECT(display,
                        "could not output image - no window created");

                // Return OK so that the flow keeps coming
                //return GST_FLOW_ERROR;
                return GST_FLOW_OK;
            }
            else
            {
                // Externally supplied window has not yet been supplied, ignore
                // incoming frames for now
                //GST_DEBUG_OBJECT(display, "can't display frame until window is supplied");
                return GST_FLOW_OK;
            }
        }
    }
    else
    {
        // Never get here because it's always our image buffer
        //GST_DEBUG_OBJECT(display, "slow copy into bufferpool buffer");

        // Else we have to copy the data into our private image,
        // if we have one...

        if (!display->fill_image)
        {
            GST_DEBUG_OBJECT(display, "creating our image");

            display->fill_image = gst_display_image_buffer_new(display,
                    GST_BUFFER_CAPS(buf), GST_BUFFER_SIZE(buf));
            display->pUIInfo->pFillImageBuffer
                    = display->fill_image->pImageBuffer;

            if (!display->fill_image)
            {
                // The create method should have posted an informative error
                g_mutex_unlock(display->pUIInfo->flow_lock);
                GST_WARNING_OBJECT(display,
                        "could not create image - null fill image");
                return GST_FLOW_ERROR;
            }

            if (display->fill_image->pImageBuffer->size < GST_BUFFER_SIZE(buf))
            {
                GST_ELEMENT_ERROR(
                        display,
                        RESOURCE,
                        WRITE,
                        ("Failed to create output image buffer of %dx%d pixels, size: %d", display->fill_image->pImageBuffer->buffer_width, display->fill_image->pImageBuffer->buffer_height, (int) (display->fill_image->pImageBuffer->buffer_width
                                * display->fill_image->pImageBuffer->buffer_height
                                * 1.5)),
                        ("Allocated buffer size did not match input buffer %d", GST_BUFFER_SIZE(
                                buf)));

                gst_display_image_buffer_free(display->fill_image);
                display->fill_image = NULL;
                g_mutex_unlock(display->pUIInfo->flow_lock);
                GST_WARNING_OBJECT(display,
                        "could not create image - buffer size mismatch");
                return GST_FLOW_ERROR;
            }
        }
        else
        {
            //GST_DEBUG_OBJECT(display, "not creating image because it is not null");
        }
        memcpy(display->fill_image->pImageBuffer->data, // to
                GST_BUFFER_DATA(buf), // from
                MIN(GST_BUFFER_SIZE(buf),
                        display->fill_image->pImageBuffer->size)); // count

        if (!gst_display_image_put(display, display->fill_image))
        {
            g_mutex_unlock(display->pUIInfo->flow_lock);
            GST_WARNING_OBJECT(display,
                    "could not output image - problem putting image");
            return GST_FLOW_ERROR;
        }
    }

    // Release flow lock since done with image buffers
    g_mutex_unlock(display->pUIInfo->flow_lock);

    // Request the window to repaint since we have a new buffer
    window_request_repaint(display->pUIInfo);

    return GST_FLOW_OK;
}

/**
 * Remove the last frame of video that was displayed
 */
static void gst_display_video_clear(GstDisplay* display)
{
    //GST_ERROR("function entry");
    g_mutex_lock(display->pUIInfo->flow_lock);

    // Set UIInfo display image buffer to NULL so it no longer is displayed
    // by UI layer (image buffer will be freed normally via display reference)
    display->pUIInfo->pDisplayImageBuffer = NULL;

    g_mutex_unlock(display->pUIInfo->flow_lock);

    // Request the window to repaint since we have cleared out video frame
    window_request_repaint(display->pUIInfo);
}

/**
 * Draws the specified image to the screen.
 * This function is responsible for the following:
 * 1. Clear the background plane.
 * 2. Scale and display the video image (contained in image) onto the video plane.
 * 4. Overlay the graphics plane on top of the video and background planes.
 *
 * If the image is in I420YUV format, this function will call a utility function
 * to convert it to RGB format before drawing it to the video plane.
 *
 * @param display the plugin instance.
 * @param image the video image to be drawn
 *
 * @return <code>TRUE</code> if the image is drawn w/o error, <code>FALSE</code>
 * if there is no display window to write the image to.
 */
static gboolean gst_display_image_put(GstDisplay * display,
        GstDisplayImageBuffer * image)
{
    //GST_ERROR("function entry");
    g_return_val_if_fail(GST_IS_DISPLAY(display), FALSE);

    // if there is no display window...
    if (0 == G_UNLIKELY(display->pUIInfo->pWindowInfo->win))
    {
        //GST_DEBUG("no window has been created");
        return TRUE;
    }

    // The flow_lock was already acquired in show_frame so don't need to acquire it here

    // Store a reference to the last image we put, lose the previous one
    if (image && display->display_image != image)
    {
        if (display->display_image)
        {
            //GST_DEBUG_OBJECT(display, "unreffing %p", display->cur_image);
            gst_buffer_unref(GST_BUFFER(display->display_image));
        }
        //GST_DEBUG_OBJECT(display, "reffing %p as our current image", image);
        display->display_image = GST_DISPLAY_IMAGE_BUFFER(gst_buffer_ref(
                GST_BUFFER(image)));

        display->pUIInfo->pDisplayImageBuffer
                = display->display_image->pImageBuffer;
        display->pUIInfo->pDisplayImageBuffer->buffer_data
                = display->display_image->buffer.data;
    }

    return TRUE;
}

/**
 * Allocates a buffer based on the specified capabilites.
 * *TODO* - try to understand what's going on in the method better
 */
static GstFlowReturn gst_display_buffer_alloc(GstBaseSink* bsink,
        guint64 offset, guint size, GstCaps* caps, GstBuffer** buf)
{
    //GST_DEBUG("function entry with size %d", size);

    GstFlowReturn ret = GST_FLOW_OK;
    GstDisplay *display;
    GstDisplayImageBuffer *image = NULL;
    GstCaps *intersection = NULL;
    GstStructure *structure = NULL;
    gint width, height;

    display = GST_DISPLAY(bsink);

    if (G_LIKELY(display->last_caps && gst_caps_is_equal(caps,
            display->last_caps)))
    {
        //GST_DEBUG_OBJECT(display,
        //        "buffer alloc for same last_caps, reusing caps");
        intersection = gst_caps_ref(caps);

        goto reuse_last_caps;
    }

    //GST_DEBUG_OBJECT(display, "buffer alloc requested with caps %"
    //     GST_PTR_FORMAT ", intersecting with our caps %" GST_PTR_FORMAT, caps,
    //      display->caps);

    // Check the caps against our xcontext
    intersection = gst_caps_intersect(display->caps, caps);

    // Ensure the returned caps are fixed
    gst_caps_truncate(intersection);

    if (gst_caps_is_empty(intersection))
    {
        //GST_DEBUG_OBJECT(display, "intersection in buffer alloc was empty");

        // So we don't support this kind of buffer, let's define one we'd like
        GstCaps *new_caps = gst_caps_copy(caps);

        structure = gst_caps_get_structure(new_caps, 0);

        // Now try with RGB
        gst_structure_set_name(structure, "video/x-raw-rgb");

        // And intersect again
        gst_caps_unref(intersection);
        intersection = gst_caps_intersect(display->caps, new_caps);

        if (gst_caps_is_empty(intersection))
        {
            GST_WARNING_OBJECT(display, "we were requested a buffer with "
                    "caps %" GST_PTR_FORMAT ", but our xcontext caps %" GST_PTR_FORMAT
                    " are completely incompatible with those caps", new_caps,
                    display->caps);
            gst_caps_unref(new_caps);
            ret = GST_FLOW_UNEXPECTED;
            goto beach;
        }

        // Clean this copy
        gst_caps_unref(new_caps);
        /* We want fixed caps */
        gst_caps_truncate(intersection);

        //GST_DEBUG_OBJECT(display, "allocating a buffer with caps %"
        //      GST_PTR_FORMAT, intersection);
    }
    else if (gst_caps_is_equal(intersection, caps))
    {
        //GST_DEBUG_OBJECT(display, "intersection in buffer alloc returned %"
        //      GST_PTR_FORMAT, intersection);

        // Things work better if we return a buffer with the same caps ptr
        // as was asked for when we can
        gst_caps_replace(&intersection, caps);
    }

    // Store our caps and format as the last_caps to avoid expensive
    // caps intersection next time
    gst_caps_replace(&display->last_caps, // caps
            intersection); // newcaps

    reuse_last_caps:

    // Get geometry from caps
    structure = gst_caps_get_structure(intersection, 0);
    gboolean test1 = gst_structure_get_int(structure, "width", &width);
    gboolean test2 = gst_structure_get_int(structure, "height", &height);

    //GST_DEBUG_OBJECT(display, "reusing last caps, structure requests buffer width %d, height %d",
    //      width, height);

    if ((FALSE == test1) || (FALSE == test2))// ||(-1 == image_format))
    {
        GST_WARNING_OBJECT(display, "invalid caps for buffer allocation %"
                GST_PTR_FORMAT, intersection);
        ret = GST_FLOW_UNEXPECTED;
        goto beach;
    }

    // Get the pool lock since about to mess with the image pool
    g_mutex_lock(display->pool_lock);

    if (!display->image_pool)
    {
        //GST_DEBUG_OBJECT(display, "image pool is null");
    }

    // Walking through the pool cleaning unusable images and searching for a
    //  suitable one
    while (display->image_pool)
    {
        image = display->image_pool->data;

        // If the current item in the list has a buffer
        if (image)
        {
            //GST_DEBUG_OBJECT(display, "Grabbing an image buffer from the pool");

            // Remove image buffer from the pool
            display->image_pool = g_slist_delete_link(display->image_pool,
                    display->image_pool);

            // We check for geometry or image format changes
            if ((image->pImageBuffer->buffer_width != width)
                    || (image->pImageBuffer->buffer_height != height))
            {
                //GST_DEBUG_OBJECT(display, "destroying buffer, requested width %d, height %d, buffer width %d, height %d",
                //      image->buffer_width, image->buffer_height, width, height);

                // This image is unusable. Destroying...
                gst_display_image_buffer_free(image);
                image = NULL;
            }
            else
            {
                // We found a suitable image
                //GST_DEBUG_OBJECT(display, "found usable image in pool, width %d, height %d",
                //      image->buffer_width, image->buffer_height);
                break;
            }
        }
        else
        {
            //GST_DEBUG_OBJECT(display, "image pool data is null");
        }
    }

    // Done messing with image pool
    g_mutex_unlock(display->pool_lock);

    if (!image)
    {
        // We found no suitable image in the pool. Creating...
        GST_DEBUG_OBJECT(display,
                "no usable image in pool, creating image of size %d", size);
        image = gst_display_image_buffer_new(display, intersection, size);
        if (image && image->pImageBuffer->size < size)
        {
            // This image is unusable. Destroying...
            gst_display_image_buffer_free(image);
            image = NULL;
        }
        else
        {
            //GST_DEBUG_OBJECT(display, "created image buffer width %d, height %d",
            //     image->buffer_width, image->buffer_height);
        }
    }

    // If image was allocated, update the caps associated with the buffer
    // so if caps have changed, video source can re-nego caps
    if (image)
    { // Updating the caps of the buffer

        gst_buffer_set_caps(GST_BUFFER(image), intersection);

        //structure = gst_caps_get_structure(intersection, 0);
        //gst_structure_get_int(structure, "width", &width);
        //gst_structure_get_int(structure, "height", &height);
        //GST_DEBUG_OBJECT(display, "setting caps of buffer to width %d, height %d",
        //      width, height);
    }
    else
    {
        //g_print("Not setting caps on buffer\n");
    }

    // Return the allocated buffer by pointing the supplied pointer to the image buffer
    // that was allocated
    *buf = GST_BUFFER(image);

    beach: if (intersection)
    {
        gst_caps_unref(intersection);
    }

    return ret;
}

//
/////////////////// Display Plugin Methods end ////////////////////////////////
//


/* =========================================== */
/*                                             */
/*           Update & Reset Methods            */
/*                                             */
/* =========================================== */

/**
 * Clears the pool of allocated images.
 *
 * @param display the plugin instance.
 */
static void gst_display_imagepool_clear(GstDisplay * display)
{
    GST_DEBUG("function entry");
    g_mutex_lock(display->pool_lock);

    while (display->image_pool)
    {
        GstDisplayImageBuffer *image = display->image_pool->data;

        display->image_pool = g_slist_delete_link(display->image_pool,
                display->image_pool);
        gst_display_image_buffer_free(image);
    }

    g_mutex_unlock(display->pool_lock);
}

/**
 * Resets the plugin instance.
 *
 * @param display instance of the plugin.
 */
void gst_display_reset(GstDisplay * display)
{
    GST_DEBUG("function entry");

    // Determine if display needs to be destroyed
    gst_display_window_destroy(display);

    // Set the running flag to false to indicate plugin is going back to NULL state
    // *TODO* - This lock seems unnecessary
    //GST_OBJECT_LOCK(display);
    display->running = FALSE;
    //GST_OBJECT_UNLOCK(display);

    // Get the flow lock since messing with image buffers
    g_mutex_lock(display->pUIInfo->flow_lock);

    if (display->display_image)
    {
        gst_buffer_unref(GST_BUFFER(display->display_image));
        display->display_image = NULL;
    }

    if (display->fill_image)
    {
        gst_buffer_unref(GST_BUFFER(display->fill_image));
        display->fill_image = NULL;
    }

    gst_display_imagepool_clear(display);

    if ((NULL != display->pUIInfo->pVideoInfo) && (NULL
            != display->pUIInfo->pVideoInfo->pConversionBuff))
    {
        g_free(display->pUIInfo->pVideoInfo->pConversionBuff);
        display->pUIInfo->pVideoInfo->pConversionBuff = NULL;
    }

    // Release the flow lock since done messing with image buffers
    g_mutex_unlock(display->pUIInfo->flow_lock);

    gst_display_info_clear(display);
}

/*
 * Cleans the display info context. Closing the Display, and unrefing the
 * caps for supported formats.
 *
 * @param display the plugin instance.
 */
static void gst_display_info_clear(GstDisplay * display)
{
    GST_DEBUG("function entry");
    DisplayInfo* displayInfo;

    g_return_if_fail(GST_IS_DISPLAY(display));

    // Lock the object when modifying display info since there is not a specific lock
    GST_OBJECT_LOCK(display);
    if (display->pUIInfo->pDisplayInfo == NULL)
    {
        GST_OBJECT_UNLOCK(display);
        return;
    }

    // Take the display info from the sink and clean it up
    displayInfo = display->pUIInfo->pDisplayInfo;
    display->pUIInfo->pDisplayInfo = NULL;

    GST_OBJECT_UNLOCK(display);

    gst_caps_unref(display->caps);
    if (display->last_caps)
    {
        gst_caps_replace(&display->last_caps, NULL);
    }

    g_free(displayInfo->par);

    display->pUIInfo->pDisplayInfo = NULL;
    g_free(displayInfo);
}

/**
 * Allows for applications to set internal properties of this plugin.
 *
 * @param object instance of plugin.
 * @param prop_id identifier of the property being set.
 * @param value contains new value for the property.
 * @param pspec property spec ????
 */
static void gst_display_set_property(GObject* object, guint prop_id,
        const GValue* value, GParamSpec* pspec)
{
    GST_DEBUG("function entry");

    GstDisplay *display;
    g_return_if_fail(GST_IS_DISPLAY(object));

    display = GST_DISPLAY(object);

    switch (prop_id)
    {
    case ARG_WINDOW_HEIGHT:
        display->pUIInfo->pWindowInfo->height = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting window height to %d",
                display->pUIInfo->pWindowInfo->height);
        break;

    case ARG_WINDOW_WIDTH:
        display->pUIInfo->pWindowInfo->width = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting window width to %d",
                display->pUIInfo->pWindowInfo->width);
        break;

    case ARG_OUTPUT_VIDEO_WIDTH:
        display->pUIInfo->pVideoInfo->output_width = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting video output width to %d",
                display->pUIInfo->pVideoInfo->output_width);
        break;

    case ARG_OUTPUT_VIDEO_HEIGHT:
        display->pUIInfo->pVideoInfo->output_height = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting video output height to %d",
                display->pUIInfo->pVideoInfo->output_height);
        break;

    case ARG_OUTPUT_VIDEO_PAR_X:
        display->pUIInfo->pVideoInfo->output_par_n = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting video output PARx to %d",
                display->pUIInfo->pVideoInfo->output_par_n);
        break;

    case ARG_OUTPUT_VIDEO_PAR_Y:
        display->pUIInfo->pVideoInfo->output_par_d = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting video output PARy to %d",
                display->pUIInfo->pVideoInfo->output_par_d);
        break;

    case ARG_SCALED_VIDEO_SRC_X:
        display->pUIInfo->pVideoInfo->scale_src_x = g_value_get_float(value);
        break;

    case ARG_SCALED_VIDEO_SRC_Y:
        display->pUIInfo->pVideoInfo->scale_src_y = g_value_get_float(value);
        break;

    case ARG_SCALED_VIDEO_SRC_WIDTH:
        display->pUIInfo->pVideoInfo->scale_src_width
                = g_value_get_float(value);
        break;

    case ARG_SCALED_VIDEO_SRC_HEIGHT:
        display->pUIInfo->pVideoInfo->scale_src_height = g_value_get_float(
                value);
        break;

    case ARG_SCALED_VIDEO_DEST_X:
        display->pUIInfo->pVideoInfo->scale_dest_x = g_value_get_float(value);
        break;

    case ARG_SCALED_VIDEO_DEST_Y:
        display->pUIInfo->pVideoInfo->scale_dest_y = g_value_get_float(value);
        break;

    case ARG_SCALED_VIDEO_DEST_WIDTH:
        display->pUIInfo->pVideoInfo->scale_dest_width = g_value_get_float(
                value);
        break;

    case ARG_SCALED_VIDEO_DEST_HEIGHT:
        display->pUIInfo->pVideoInfo->scale_dest_height = g_value_get_float(
                value);
        break;

    case ARG_PIXEL_ASPECT_RATIO:
        g_free(display->pUIInfo->pVideoInfo->incoming_par);
        display->pUIInfo->pVideoInfo->incoming_par = g_try_new0(GValue, 1);

        if (NULL == display->pUIInfo->pVideoInfo->incoming_par)
        {
            fprintf(stderr, "line %d of %s, %s memory allocation failure!\n",
                        __LINE__, __FILE__, __func__);
            exit(-1);
        }

        (void) g_value_init(display->pUIInfo->pVideoInfo->incoming_par,
                GST_TYPE_FRACTION);
        if (!g_value_transform(value,
                display->pUIInfo->pVideoInfo->incoming_par))
        {
            g_warning("Could not transform string to aspect ratio");
            gst_value_set_fraction(display->pUIInfo->pVideoInfo->incoming_par,
                    1, 1);
        }
        display->pUIInfo->pVideoInfo->incoming_par_n
                = gst_value_get_fraction_numerator(value);
        display->pUIInfo->pVideoInfo->incoming_par_d
                = gst_value_get_fraction_denominator(value);
        GST_DEBUG_OBJECT(display, "setting incoming video PAR to %d/%d",
                display->pUIInfo->pVideoInfo->incoming_par_n,
                display->pUIInfo->pVideoInfo->incoming_par_d);
        break;

    case ARG_GRAPHICS_SURFACE:
        g_mutex_lock(display->pUIInfo->flow_lock);
        display->pUIInfo->pGraphicsInfo = g_value_get_pointer(value);
        g_mutex_unlock(display->pUIInfo->flow_lock);
        break;

    case ARG_HW_ACCELERATION_DISABLED:
        display->pUIInfo->hw_acceleration_disabled = g_value_get_boolean(value);
        GST_DEBUG_OBJECT(display, "setting hw acceleration disabled to %d",
                display->pUIInfo->hw_acceleration_disabled);
        break;

    case ARG_BACKGROUND_COLOR_RGBA8888:
        display->pUIInfo->pBackgroundInfo->background_color
                = g_value_get_ulong(value);
        display->pUIInfo->pBackgroundInfo->update_needed = TRUE;
        break;

    case ARG_FORCE_ASPECT_RATIO:
        display->keep_aspect = g_value_get_boolean(value);
        break;

    case ARG_FIXED_WINDOW:
        display->pUIInfo->pWindowInfo->is_fixed = g_value_get_boolean(value);
        break;

    case ARG_SUPPLIED_WINDOW:
        display->pUIInfo->pWindowInfo->is_supplied = g_value_get_boolean(value);
        break;

    case ARG_DFC_VALUE:
        display->pUIInfo->pVideoInfo->dfc = g_value_get_int(value);

        // If setting dfc to platform default, retrieve value
        if (DFC_PLATFORM == display->pUIInfo->pVideoInfo->dfc)
        {
            display->pUIInfo->pVideoInfo->is_dfc_default = TRUE;
            display->pUIInfo->pVideoInfo->dfc
                    = display->pUIInfo->pVideoInfo->dfc_default;
        }
        else
        {
            display->pUIInfo->pVideoInfo->is_dfc_default = FALSE;
        }
        GST_DEBUG_OBJECT(display, "setting video dfc to %d",
                display->pUIInfo->pVideoInfo->dfc);
        break;

    case ARG_DFC_DEFAULT:
        display->pUIInfo->pVideoInfo->dfc_default = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting video dfc default to %d",
                display->pUIInfo->pVideoInfo->dfc_default);
        if (display->pUIInfo->pVideoInfo->is_dfc_default)
        {
            display->pUIInfo->pVideoInfo->dfc
                    = display->pUIInfo->pVideoInfo->dfc_default;
        }

        break;

    case ARG_AFD_VALUE:
        display->pUIInfo->pVideoInfo->afd = g_value_get_int(value);
        GST_DEBUG_OBJECT(display, "setting video afd to %d",
                display->pUIInfo->pVideoInfo->afd);
        break;

    case ARG_FORCE_RGB_VIDEO:
        display->force_rgb_video = g_value_get_boolean(value);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/**
 * Set capabilites of this plugin which configures this plugin for a particular formats
 * and register functions to let data flow through the element.
 *
 * This is an infrastructure function call used by GStreamer to establish
 * the operational capabilities that have been negotiated with the upstream
 * plugin component.
 *
 * @param bsink this plugin instance(base class)
 * @param caps cababilites that have been negotiated.
 *
 * @return
 */
static gboolean gst_display_set_caps(GstBaseSink* bsink, GstCaps* caps)
{
    GST_DEBUG("function entry");
    GstDisplay *display;
    GstStructure *structure;
    gboolean ret;

    display = GST_DISPLAY(bsink);

    // Extract cap values
    ret = gst_display_process_caps(display, caps);
    if (!ret)
    {
        // No cap values were supplied
        return FALSE;
    }

    // Get structure from caps
    structure = gst_caps_get_structure(caps, 0);
    ret = gst_display_process_structure(display, structure);
    if (!ret)
    {
        // Not all cap values were supplied
        return FALSE;
    }

    // Verify requested window size is valid
    ret = gst_display_verify_dimensions(display);
    if (!ret)
    {
        // Problems with requested window size
        GST_DEBUG("Problems with window size, unable to do caps nego");
        return FALSE;
    }

    // Adjust the video aspect ratio as necessary
    if (NULL != display->pUIInfo->pVideoInfo)
    {
        gst_display_video_aspect_ratio_adjust(display);
    }

    // We should now have a system display context, so...
    // ...create the display window if not expecting a supplied window
    if ((0 == display->pUIInfo->pWindowInfo->win) && (FALSE
            == display->pUIInfo->pWindowInfo->is_supplied))
    {
        // Window locking will be done in gst_display_window_new()
        //display->pUIInfo->pWindowInfo->win =
        gst_display_window_new(display, display->pUIInfo->pWindowInfo->width,
                display->pUIInfo->pWindowInfo->height);
    }
    else
    {
        GST_DEBUG("No window yet, expected one to be supplied");
    }

    return TRUE;
}

/**
 * Determines if there are values supplied for caps.
 *
 * @param display    plugin instance
 * @param caps       supplied capability values
 *
 * @return  TRUE if values for caps were supplied, false otherwise
 */
static gboolean gst_display_process_caps(GstDisplay* display, GstCaps* caps)
{
    GstCaps *intersection;

    GST_DEBUG_OBJECT(display,
            "In setcaps. Possible caps %" GST_PTR_FORMAT ", setting caps %"
            GST_PTR_FORMAT, display->caps, caps);

    intersection = gst_caps_intersect(display->caps, caps);
    GST_DEBUG_OBJECT(display, "intersection returned %" GST_PTR_FORMAT,
            intersection);
    if (gst_caps_is_empty(intersection))
    {
        gst_caps_unref(intersection);
        return FALSE;
    }

    gst_caps_unref(intersection);

    return TRUE;
}

/**
 * Get the caps for this plugin which includes frames per second
 * and video dimensions.
 *
 * @param display    plugin instance
 * @param structure  values specified in caps
 *
 * @return  FALSE if problems were encountered getting values from structure,
 *          TRUE if plugin values were set to values supplied in structure
 */
static gboolean gst_display_process_structure(GstDisplay* display,
        GstStructure *structure)
{
    gboolean ret;
    gint video_width;
    gint video_height = 0;
    const GValue *fps;

    // Get the incoming video parameters
    ret = gst_structure_get_int(structure, "width", &video_width);
    ret = ret && gst_structure_get_int(structure, "height", &video_height);
    fps = gst_structure_get_value(structure, "framerate");
    ret = ret && (fps != NULL);
    if (!ret)
    {
        GST_DEBUG_OBJECT(display, "Failed to retrieve either width, "
            "height or framerate from intersected caps");
        return FALSE;
    }

    display->fps_n = gst_value_get_fraction_numerator(fps);
    display->fps_d = gst_value_get_fraction_denominator(fps);

    // Unscaled incoming video width
    display->pUIInfo->pVideoInfo->incoming_width = video_width;
    display->pUIInfo->pVideoInfo->incoming_height = video_height;
    GST_DEBUG_OBJECT(display,
            "setting video incoming width to %d, incoming height to %d",
            display->pUIInfo->pVideoInfo->incoming_width,
            display->pUIInfo->pVideoInfo->incoming_height);

    const GValue *caps_par = gst_structure_get_value(structure,
            "pixel-aspect-ratio");
    if (caps_par)
    {
        if (!g_value_transform(caps_par,
                display->pUIInfo->pVideoInfo->incoming_par))
        {
            g_warning("Could not transform string to aspect ratio");
            gst_value_set_fraction(display->pUIInfo->pVideoInfo->incoming_par,
                    1, 1);
        }
        else
        {
            display->pUIInfo->pVideoInfo->incoming_par_n
                    = gst_value_get_fraction_numerator(caps_par);
            display->pUIInfo->pVideoInfo->incoming_par_d
                    = gst_value_get_fraction_denominator(caps_par);
        }
    }
    GST_DEBUG_OBJECT(display, "set video incoming PAR to %d - %d",
            display->pUIInfo->pVideoInfo->incoming_par_n,
            display->pUIInfo->pVideoInfo->incoming_par_d);

    return TRUE;
}

/**
 * Adjust the cap video dimensions in the base class if the values exceed
 * the overall display dimensions.
 *
 * @param display    plugin instance
 * @return False if the video size was invalid, true otherwise
 */
static gboolean gst_display_verify_dimensions(GstDisplay* display)
{
    // Creating our window and our image with the display size in pixels
    if (display->pUIInfo->pWindowInfo->width <= 0
            || display->pUIInfo->pWindowInfo->height <= 0)
    {
        GST_ELEMENT_ERROR(
                display,
                CORE,
                NEGOTIATION,
                (NULL),
                ("Window dimension is invalid, width %d, height %d", display->pUIInfo->pWindowInfo->width, display->pUIInfo->pWindowInfo->height));
        return FALSE;
    }

    if (display->pUIInfo->pWindowInfo->width
            > display->pUIInfo->pDisplayInfo->width)
    {
        GST_ELEMENT_ERROR(
                display,
                CORE,
                NEGOTIATION,
                (NULL),
                ("Requested screen width %d greater than physical screen width, setting to %d.", display->pUIInfo->pWindowInfo->width, display->pUIInfo->pDisplayInfo->width));
        display->pUIInfo->pWindowInfo->width
                = display->pUIInfo->pDisplayInfo->width;
    }

    if (display->pUIInfo->pWindowInfo->height
            > display->pUIInfo->pDisplayInfo->height)
    {
        GST_ELEMENT_ERROR(
                display,
                CORE,
                NEGOTIATION,
                (NULL),
                ("Requested screen height %d greater than physical screen height, setting to %d.", display->pUIInfo->pWindowInfo->height, display->pUIInfo->pDisplayInfo->height));
        display->pUIInfo->pWindowInfo->height
                = display->pUIInfo->pDisplayInfo->height;
    }

    return TRUE;
}

/**
 * Adjusts the video aspect ratio factors based on output parameters.
 *
 * @param display    display plugin
 * @return  false if problems were encountered adjusting ratio, true otherwise
 */
void gst_display_video_aspect_ratio_adjust(GstDisplay* display)
{
    GST_DEBUG_OBJECT(display, "function entry");

    /*
     The scaling of the input video to the RI display has two parts:
     1. Scaling the output video window requested by the chosen coherent config into the RI display window
     2. Scaling the input video into the out video window

     In the first part, if the aspect ratios of the output video window and the RI display window are the same,
     then the video output window is simply made to fill all available RI display space.  On the other hand,
     if the aspect ratios are different, then the output video window is scaled so that it is the largest that
     can fit in the RI display window while still having the output video's selected aspect ratio (as specified
     by the coherent config).  The portions of the RI display window that are not occupied by the output video
     window are deemed to not be part of the TV, and are ignored.

     In the second part, the input video is scaled into the output video window using the rules of the particular
     DFC transformation chosen.  Depending on the chosen DFC, the DFC transformation may or may not preserve aspect
     ratio, and may or may not clip the input video.  DFC details can be found in the OCAP spec
     (e.g. OC-SP-OCAP1.0.0.1-070824).
     */

    g_mutex_lock(display->pUIInfo->flow_lock);

    GST_DEBUG_OBJECT(
            display,
            "video aspect adjustment parameters - incoming wd %d, ht %d par %d/%d, outgoing wd %d, ht %d, par %d/%d",
            display->pUIInfo->pVideoInfo->incoming_width,
            display->pUIInfo->pVideoInfo->incoming_height,
            display->pUIInfo->pVideoInfo->incoming_par_n,
            display->pUIInfo->pVideoInfo->incoming_par_d,
            display->pUIInfo->pVideoInfo->output_width,
            display->pUIInfo->pVideoInfo->output_height,
            display->pUIInfo->pVideoInfo->output_par_n,
            display->pUIInfo->pVideoInfo->output_par_d);

    // First, some preliminaries: calculate the aspect ratios for the display window, the video output, and the video input

    // Calculate the Incoming Video Screen Aspect Ratio (SAR)
    //
    // SARx   PARx   PixelW
    // ---- = ---- * ------
    // SARy   PARy   PixelH
    //
    display->pUIInfo->pVideoInfo->incoming_sar
            = (float) (display->pUIInfo->pVideoInfo->incoming_par_n
                    * display->pUIInfo->pVideoInfo->incoming_width)
                    / (float) (display->pUIInfo->pVideoInfo->incoming_par_d
                            * display->pUIInfo->pVideoInfo->incoming_height);

    // Calculate the desired Video Output SAR
    //
    // SARx   PARx   PixelW
    // ---- = ---- * ------
    // SARy   PARy   PixelH
    //
    display->pUIInfo->pVideoInfo->output_sar
            = (float) (display->pUIInfo->pVideoInfo->output_par_n
                    * display->pUIInfo->pVideoInfo->output_width)
                    / (float) (display->pUIInfo->pVideoInfo->output_par_d
                            * display->pUIInfo->pVideoInfo->output_height);

    // Calculate the Window SAR
    //
    // SARx   PARx   PixelW
    // ---- = ---- * ------
    // SARy   PARy   PixelH
    //
    float window_sar = (float) (display->pUIInfo->pDisplayInfo->par_n
            * display->pUIInfo->pWindowInfo->width)
            / (float) (display->pUIInfo->pDisplayInfo->par_d
                    * display->pUIInfo->pWindowInfo->height);

    // Now, do step 1: adjust the display window width and height to be the largest version of the output video window that
    // can fit in the display window, while preserving the output video aspect ratio

    float output_video_window_width = display->pUIInfo->pWindowInfo->width;
    float output_video_window_height = display->pUIInfo->pWindowInfo->height;
    if (window_sar > display->pUIInfo->pVideoInfo->output_sar)
    {
        output_video_window_width = (display->pUIInfo->pVideoInfo->output_sar
                / window_sar) * display->pUIInfo->pWindowInfo->width;
    }
    else if (window_sar < display->pUIInfo->pVideoInfo->output_sar)
    {
        output_video_window_height = (window_sar
                / display->pUIInfo->pVideoInfo->output_sar)
                * display->pUIInfo->pWindowInfo->height;
    }

    GST_DEBUG_OBJECT(
            display,
            "output_sar = %f, incoming_sar = %f, outputVideoWindowWidth = %f, outputVideoWindowHeight = %f",
            display->pUIInfo->pVideoInfo->output_sar,
            display->pUIInfo->pVideoInfo->incoming_sar,
            output_video_window_width, output_video_window_height);

    // Now do step 2: scale the input video into the output video window.
    //
    // If the aspect ratios of the input video and the output video are the same, then simply scale the input video to fill
    // the entire output video window.
    //
    // If the aspect ratios of the input video and the output video are the different, then apply the appropriate DFC transformation
    // to scale the input video into the output video window.

    // HD can arrive with 1920 x 1088 resolution instead of 1920 x 1080.  If so, still treat video as HD.
    float hdSAR = (1920.0 / 1080.0);
    float hdSAR_MPEG = (1920.0 / 1088.0);

    if ((display->pUIInfo->pVideoInfo->incoming_sar == display->pUIInfo->pVideoInfo->output_sar) ||
        ((display->pUIInfo->pVideoInfo->incoming_sar == hdSAR_MPEG) && (display->pUIInfo->pVideoInfo->output_sar == hdSAR)))
    {
        // If the incoming video aspect ratio is to be preserved, make additional adjustments
        // THIS IS NO LONGER USED
        if (display->keep_aspect)
        {
            gst_display_keep_aspect(display);
        }
        else
        {
            // scale the input video to fill the entire output video window
            display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
                    = output_video_window_width
                            / (float) display->pUIInfo->pVideoInfo->incoming_width;

            display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
                    = output_video_window_height
                            / (float) display->pUIInfo->pVideoInfo->incoming_height;
        }
    }
    else
    {
        GST_DEBUG_OBJECT(display,
                "SARs are different, incoming SAR %f, output SAR %f",
                display->pUIInfo->pVideoInfo->incoming_sar,
                display->pUIInfo->pVideoInfo->output_sar);

        // apply the appropriate DFC transformation to scale the input video into the output video window.
        gst_display_video_dfc_adjustment(display,
                (guint) output_video_window_width,
                (guint) output_video_window_height);
    }

    GST_DEBUG_OBJECT(display,
            "video aspect factors after adjustment - width %f, height %f",
            display->pUIInfo->pVideoInfo->aspect_ratio_width_factor,
            display->pUIInfo->pVideoInfo->aspect_ratio_height_factor);

    g_mutex_unlock(display->pUIInfo->flow_lock);
}

/**
 * Adjust the window height used in calculations to ensure the window height
 * is set to a value which ensures the video output SAR matches window SAR with
 * adjusted height.
 *
 * @param   display  display information
 */

/**
 * Adjust the VARF values to ensure the video is scaled in a manner which maintains
 * the incoming SAR.
 *
 * @param display information
 */
static void gst_display_keep_aspect(GstDisplay* display)
{
    GST_DEBUG_OBJECT(display,
            "aspect factors prior to adjustment, width %f, height %f",
            display->pUIInfo->pVideoInfo->aspect_ratio_width_factor,
            display->pUIInfo->pVideoInfo->aspect_ratio_height_factor);

    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            = ((float) display->pUIInfo->pVideoInfo->output_width
                    * ((float) display->pUIInfo->pVideoInfo->output_par_n
                            / (float) display->pUIInfo->pVideoInfo->output_par_d))
                    / ((float) display->pUIInfo->pVideoInfo->incoming_width
                            * ((float) display->pUIInfo->pVideoInfo->incoming_par_n
                                    / (float) display->pUIInfo->pVideoInfo->incoming_par_d));

    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            = ((float) display->pUIInfo->pVideoInfo->output_height
                    * ((float) display->pUIInfo->pVideoInfo->output_par_d
                            / (float) display->pUIInfo->pVideoInfo->output_par_n))
                    / ((float) display->pUIInfo->pVideoInfo->incoming_height
                            * ((float) display->pUIInfo->pVideoInfo->incoming_par_d
                                    / (float) display->pUIInfo->pVideoInfo->incoming_par_n));

    // Use the smaller of the two factors to ensure output fits
    if (display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            < display->pUIInfo->pVideoInfo->aspect_ratio_width_factor)
    {
        // Set width factor to smaller height factor
        display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
                = display->pUIInfo->pVideoInfo->aspect_ratio_height_factor;
    }
    else // Use width factor since it is smaller than height factor
    {
        // Set height factor to smaller width factor
        display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
                = display->pUIInfo->pVideoInfo->aspect_ratio_width_factor;
    }

    // Adjust the factor for the display window, maintaining the aspect of video output
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            *= ((float) display->pUIInfo->pWindowInfo->width
                    * ((float) display->pUIInfo->pDisplayInfo->par_n
                            / (float) display->pUIInfo->pDisplayInfo->par_d))
                    / ((float) display->pUIInfo->pVideoInfo->incoming_width
                            * display->pUIInfo->pVideoInfo->aspect_ratio_width_factor);

    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            *= ((float) display->pUIInfo->pWindowInfo->height
                    * ((float) display->pUIInfo->pDisplayInfo->par_d
                            / (float) display->pUIInfo->pDisplayInfo->par_n))
                    / ((float) display->pUIInfo->pVideoInfo->incoming_height
                            * display->pUIInfo->pVideoInfo->aspect_ratio_height_factor);

    GST_DEBUG_OBJECT(display,
            "aspect factors after display adjustment, width %f, height %f",
            display->pUIInfo->pVideoInfo->aspect_ratio_width_factor,
            display->pUIInfo->pVideoInfo->aspect_ratio_height_factor);

    // Use the smaller of the two factors to ensure output fits
    if (display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            < display->pUIInfo->pVideoInfo->aspect_ratio_width_factor)
    {
        // Set width factor to smaller height factor
        display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
                = display->pUIInfo->pVideoInfo->aspect_ratio_height_factor;
    }
    else // Use width factor since it is smaller than height factor
    {
        // Set height factor to smaller width factor
        display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
                = display->pUIInfo->pVideoInfo->aspect_ratio_width_factor;
    }
}

static void gst_display_video_dfc_adjustment(GstDisplay* display,
        guint adjustedWindowWidth, guint adjustedWindowHeight)
{
    GST_DEBUG_OBJECT(display, "adjusted window width %d, height %d, dfc %d",
            adjustedWindowWidth, adjustedWindowHeight,
            display->pUIInfo->pVideoInfo->dfc);

    // Adjust for DFCs which can be adjusted via the scale factors
    switch (display->pUIInfo->pVideoInfo->dfc)
    {
    case (DFC_PROCESSING_NONE):
        // according to OCAP spec, DFC = NONE implies DFC = FULL
        GST_DEBUG_OBJECT(display, "DFC set to none -- calling DFC full");
        gst_display_video_dfc_full(display, adjustedWindowWidth,
                adjustedWindowHeight);
        break;

    case (DFC_PROCESSING_FULL):
        GST_DEBUG_OBJECT(display, "DFC set to full");
        gst_display_video_dfc_full(display, adjustedWindowWidth,
                adjustedWindowHeight);
        break;

        // These formats use the Letterbox function for conversion
    case (DFC_PROCESSING_LB_16_9):
        GST_DEBUG_OBJECT(display, "DFC set to letter box 16:9");
        // Determine if conversion applies
        if (is_16_9_to_4_3(display->pUIInfo))
        {
            gst_display_video_dfc_letterbox(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_LB_16_9");
        }
        break;
    case (DFC_PROCESSING_LB_14_9):
        GST_DEBUG_OBJECT(display, "DFC set to letter box 14:9");
        // Determine if conversion applies
        if (is_14_9_to_4_3(display->pUIInfo))
        {
            gst_display_video_dfc_letterbox(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_LB_14_9");
        }
        break;
    case (DFC_PROCESSING_LB_2_21_1_ON_4_3):
        GST_DEBUG_OBJECT(display, "DFC set to letter box 2.21 on 4:3");
        // Determine if conversion applies
        if (is_221_100_to_4_3(display->pUIInfo))
        {
            gst_display_video_dfc_letterbox(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_LB_2_21_1_ON_4_3");
        }
        break;
    case (DFC_PROCESSING_LB_2_21_1_ON_16_9):
        GST_DEBUG_OBJECT(display, "DFC set to letter box 2.21 on 16:9");
        // Determine if conversion applies
        if (is_221_100_to_16_9(display->pUIInfo))
        {
            gst_display_video_dfc_letterbox(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_LB_2_21_1_ON_16_9");
        }
        break;

    case (DFC_PROCESSING_PILLARBOX_4_3):
        GST_DEBUG_OBJECT(display, "DFC set to pillar box");
        if (is_4_3_to_16_9(display->pUIInfo))
        {
            gst_display_video_dfc_pillarbox(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_PILLARBOX_4_3");
        }
        break;

    case (DFC_PROCESSING_16_9_ZOOM):
        GST_DEBUG_OBJECT(display, "DFC set to zoom");
        if (is_4_3_to_16_9(display->pUIInfo))
        {
            gst_display_video_dfc_zoom(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_16_9_ZOOM");
        }
        break;

    case (DFC_PROCESSING_CCO):
        GST_DEBUG_OBJECT(display, "DFC set to center cut out");
        if (is_16_9_to_4_3(display->pUIInfo))
        {
            gst_display_video_dfc_cco(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_CCO");
        }
        break;

    case (DFC_PROCESSING_PAN_SCAN):
        GST_DEBUG_OBJECT(display, "DFC set to pan scan");
        if (is_16_9_to_4_3(display->pUIInfo))
        {
            gst_display_video_dfc_panscan(display, adjustedWindowWidth,
                    adjustedWindowHeight);
        }
        else
        {
            GST_ERROR_OBJECT(display,
                    "Video streams not compatible with DFC_PROCESSING_PAN_SCAN");
        }
        break;

    case (DFC_PROCESSING_WIDE_4_3):
        GST_ERROR_OBJECT(display,
                "DFC set to wide which requires modification to input buffer");
        break;

    case (DFC_PROCESSING_UNKNOWN):
        GST_ERROR_OBJECT(display, "DFC set to unknown or platform");
        break;

    default:
        GST_ERROR_OBJECT(display, "Unrecognized DFC value: %d",
                display->pUIInfo->pVideoInfo->dfc);
    }
}

/**
 * Adjusts the scaling factors so the decoder format conversion is "pillar box"
 * which means the width is reduced but the height is maintained,
 * aspect ratio is maintained.
 *
 * @param   display        display plugin
 * @param   windowWidth    "adjusted" width of the window which reflects the desired output SAR
 * @param   windowHeight   "adjusted" height of the window which reflects the desired output SAR
 */
static void gst_display_video_dfc_pillarbox(GstDisplay* display,
        guint windowWidth, guint windowHeight)
{
    GST_DEBUG_OBJECT(display, "gst_display_video_dfc_pillarbox");

    // height is full height
    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            = ((float) windowHeight)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_height);

    // width is that which corresponds to full height while preserving the aspect ratio
    float newWidth = display->pUIInfo->pVideoInfo->incoming_sar
            * display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            * ((float) display->pUIInfo->pVideoInfo->incoming_height);
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor = newWidth
            / ((float) display->pUIInfo->pVideoInfo->incoming_width);
}

/**
 * Adjusts the scaling factors so the decoder format conversion is "zoom"
 * which means the height is reduced but the width is maintained,
 * aspect ratio is also maintained.
 *
 * @param   display        display plugin
 * @param   windowWidth    "adjusted" width of the window which reflects the desired output SAR
 * @param   windowHeight   "adjusted" height of the window which reflects the desired output SAR
 */
static void gst_display_video_dfc_zoom(GstDisplay* display, guint windowWidth,
        guint windowHeight)
{
    GST_DEBUG_OBJECT(display, "gst_display_video_dfc_zoom");

    // width is full width
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            = ((float) windowWidth)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_width);

    // height is that which corresponds to full width while preserving the aspect ratio
    float newHeight = display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            * ((float) display->pUIInfo->pVideoInfo->incoming_width)
            / display->pUIInfo->pVideoInfo->incoming_sar;
    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor = newHeight
            / ((float) display->pUIInfo->pVideoInfo->incoming_height);

    // NOTE: CLIPPING OF THE INPUT VIDEO, IF NECESSARY, IS PERFORMED IN opengl_dfc_zoom
}

/**
 * Adjusts the scaling factors so the decoder format conversion is "center cut out"
 * which means the width is reduced but the height is maintained,
 * aspect ratio is also maintained.
 *
 * @param   display        display plugin
 * @param   windowWidth    "adjusted" width of the window which reflects the desired output SAR
 * @param   windowHeight   "adjusted" height of the window which reflects the desired output SAR
 */
static void gst_display_video_dfc_cco(GstDisplay* display, guint windowWidth,
        guint windowHeight)
{
    GST_DEBUG_OBJECT(display, "gst_display_video_dfc_cco");

    // height is full height
    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            = ((float) windowHeight)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_height);

    // width is that which corresponds to full height while preserving the aspect ratio
    float newWidth = display->pUIInfo->pVideoInfo->incoming_sar
            * display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            * ((float) display->pUIInfo->pVideoInfo->incoming_height);
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor = newWidth
            / ((float) display->pUIInfo->pVideoInfo->incoming_width);

    // NOTE: CLIPPING OF THE INPUT VIDEO, IF NECESSARY, IS PERFORMED IN opengl_dfc_cco
}

/**
 * Adjusts the scaling factors so the decoder format conversion is "pan scan"
 * which means the width is reduced but the height is maintained,
 * aspect ratio is also maintained using supplied vectors which describe the horizontal
 * and vertical size and offsets.
 *
 * @param   display        display plugin
 * @param   windowWidth    "adjusted" width of the window which reflects the desired output SAR
 * @param   windowHeight   "adjusted" height of the window which reflects the desired output SAR
 */
static void gst_display_video_dfc_panscan(GstDisplay* display,
        guint windowWidth, guint windowHeight)
{
    GST_DEBUG_OBJECT(display, "gst_display_video_dfc_panscan");

    // height is full height
    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            = ((float) windowHeight)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_height);

    // width is that which corresponds to full height while preserving the aspect ratio
    float newWidth = display->pUIInfo->pVideoInfo->incoming_sar
            * display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            * ((float) display->pUIInfo->pVideoInfo->incoming_height);
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor = newWidth
            / ((float) display->pUIInfo->pVideoInfo->incoming_width);

    // NOTE: CLIPPING OF THE INPUT VIDEO, IF NECESSARY, IS PERFORMED IN opengl_dfc_panscan
}

/**
 * Adjusts the scaling factors so the decoder format conversion is "full"
 * which means the output is stretched both vertically and horizontally
 * to take up the full screen, aspect ratio is not maintained.
 *
 * @param   display        display plugin
 * @param   windowWidth    "adjusted" width of the window which reflects the desired output SAR
 * @param   windowHeight   "adjusted" height of the window which reflects the desired output SAR
 */
static void gst_display_video_dfc_full(GstDisplay* display, guint windowWidth,
        guint windowHeight)
{
    GST_DEBUG_OBJECT(display, "gst_display_video_dfc_full");

    // expand video to fill app space in video window, allowing aspect ratio to change
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            = ((float) windowWidth)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_width);
    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor
            = ((float) windowHeight)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_height);
}

/**
 * Adjusts the scaling factors so the decoder format conversion is "letterbox"
 * which means the height is scaled down but width takes up entire screen.
 * Aspect ratio is maintained.
 *
 * @param   display        display plugin
 * @param   windowWidth    "adjusted" width of the window which reflects the desired output SAR
 * @param   windowHeight   "adjusted" height of the window which reflects the desired output SAR
 */
static void gst_display_video_dfc_letterbox(GstDisplay* display,
        guint windowWidth, guint windowHeight)
{
    GST_DEBUG_OBJECT(display, "gst_display_video_dfc_letterbox");

    // width is full width
    display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            = ((float) windowWidth)
                    / ((float) display->pUIInfo->pVideoInfo->incoming_width);

    // height is that which corresponds to full width while preserving the aspect ratio
    float newHeight = display->pUIInfo->pVideoInfo->aspect_ratio_width_factor
            * ((float) display->pUIInfo->pVideoInfo->incoming_width)
            / display->pUIInfo->pVideoInfo->incoming_sar;
    display->pUIInfo->pVideoInfo->aspect_ratio_height_factor = newHeight
            / ((float) display->pUIInfo->pVideoInfo->incoming_height);
}

/**
 * Adjusts the graphics aspect ratio factors based on output parameters.
 *
 * @param display    display plugin
 * @return  false if problems were encountered adjusting ratio, true otherwise
 */
static void gst_display_graphics_aspect_ratio_adjust(GstDisplay* display)
{
    // Make sure that the GraphicsInfo context is available. This is set
    // after the stack has constructed and initialized its video/graphics
    // buffers.
    if (display->pUIInfo->pGraphicsInfo == NULL)
        return;

    g_mutex_lock(display->pUIInfo->flow_lock);

    GST_DEBUG_OBJECT(
            display,
            "graphics aspect adjustment parameters - incoming wd %d, ht %d, outgoing par %d/%d",
            display->pUIInfo->pGraphicsInfo->width,
            display->pUIInfo->pGraphicsInfo->height,
            display->pUIInfo->pGraphicsInfo->par_n,
            display->pUIInfo->pGraphicsInfo->par_d);

    // the graphics window must fit completely in the video output window, so get the adjusted display window height and width
    float window_sar = (float) (display->pUIInfo->pDisplayInfo->par_n
            * display->pUIInfo->pWindowInfo->width)
            / (float) (display->pUIInfo->pDisplayInfo->par_d
                    * display->pUIInfo->pWindowInfo->height);

    float adjusted_window_width = display->pUIInfo->pWindowInfo->width;
    float adjusted_window_height = display->pUIInfo->pWindowInfo->height;
    if (window_sar > display->pUIInfo->pVideoInfo->output_sar)
    {
        adjusted_window_width = (display->pUIInfo->pVideoInfo->output_sar
                / window_sar) * display->pUIInfo->pWindowInfo->width;
    }
    else if (window_sar < display->pUIInfo->pVideoInfo->output_sar)
    {
        adjusted_window_height = (window_sar
                / display->pUIInfo->pVideoInfo->output_sar)
                * display->pUIInfo->pWindowInfo->height;
    }

    // Calculate the scaling factors
    display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor
            = adjusted_window_width
                    / (float) display->pUIInfo->pGraphicsInfo->width;
    display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor
            = adjusted_window_height
                    / (float) display->pUIInfo->pGraphicsInfo->height;

    /*
     NEW CODE: THIS CODE SCALES THE GRAPHICS WINDOW INTO THE OUTPUT VIDEO WINDOW - UNTESTED AT THE MOMENT< SO I AM LEAVING IT COMMENTED OUTn-- SA
     // Calculate the graphics output SAR
     float outputSAR = (float)(display->pUIInfo->pGraphicsInfo->width * display->pUIInfo->pGraphicsInfo->par_n) /
     (float)(display->pUIInfo->pGraphicsInfo->height * display->pUIInfo->pGraphicsInfo->par_d);

     // Adjust the window height if necessary to maintain the desired output SAR
     guint windowWidth = display->pUIInfo->pWindowInfo->width;
     guint windowHeight = display->pUIInfo->pWindowInfo->height;

     // Calculate the SAR of the window
     float windowSAR = (float)(display->pUIInfo->pWindowInfo->width * display->pUIInfo->pDisplayInfo->par_d) /
     (float)(display->pUIInfo->pWindowInfo->height * display->pUIInfo->pDisplayInfo->par_n);

     if (windowSAR != outputSAR)
     {
     // Adjust the window height
     windowHeight = (guint)((float)(display->pUIInfo->pWindowInfo->width * display->pUIInfo->pDisplayInfo->par_d) /
     ((float)display->pUIInfo->pDisplayInfo->par_n * outputSAR));

     // Make sure adjusted window height is not larger than actual window
     if (windowHeight > display->pUIInfo->pWindowInfo->height)
     {
     windowHeight = display->pUIInfo->pWindowInfo->height;

     // Adjust the width instead of height
     windowWidth = (guint)((float)(display->pUIInfo->pWindowInfo->height * display->pUIInfo->pDisplayInfo->par_d * outputSAR) /
     (float)display->pUIInfo->pDisplayInfo->par_n);
     }
     }

     GST_DEBUG_OBJECT(display, "output SAR %f, window SAR %f, window width %d height %d",
     outputSAR, windowSAR, windowWidth, windowHeight);

     // Calculate the scaling factors
     display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor = (float)windowWidth / (float)display->pUIInfo->pGraphicsInfo->width;
     display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor = (float)windowHeight / (float)display->pUIInfo->pGraphicsInfo->height;

     GST_DEBUG_OBJECT(display, "aspect factors prior to adjustment, width %f, height %f",
     display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor, display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor);

     // Use the smaller of the two factors to ensure output fits
     if (display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor < display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor)
     {
     // Height factor is smaller, set width factor to height factor
     display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor = display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor;

     // Adjust for PAR now, need to just adjust the width factor
     display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor = (display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor *
     display->pUIInfo->pGraphicsInfo->par_n) /
     display->pUIInfo->pGraphicsInfo->par_d;
     }
     else
     {
     // Width factor is smaller, set height factor to width factor
     display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor = display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor;

     // Adjust for PAR now, need to just adjust the height
     display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor = (display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor *
     display->pUIInfo->pGraphicsInfo->par_d) /
     display->pUIInfo->pGraphicsInfo->par_n;
     }

     */

    GST_DEBUG_OBJECT(display,
            "graphics aspect factors after adjustment, width %f, height %f",
            display->pUIInfo->pGraphicsInfo->aspect_ratio_width_factor,
            display->pUIInfo->pGraphicsInfo->aspect_ratio_height_factor);

    g_mutex_unlock(display->pUIInfo->flow_lock);
}

/**
 * Creates a new window. This function is used by this plugin to create
 * a new display window(as opposed to using a display window created
 * by an overriding application).
 *
 * @param display current plugin instance.
 * @param width the width, in pixels of the new window.
 * @param height the height, in pixels of the new window
 *
 * @return id of created window, 0 if window was not created
 */
static void gst_display_window_new(GstDisplay* display, gint width, gint height)
{
    GST_DEBUG("function entry");

    // Create the new window
    GST_DEBUG_OBJECT(display, "creating new window with width %d, height %d",
            width, height);

    gulong win = window_open(display->pUIInfo, width, height);

    // Indicate the we are using a plugin-created window
    display->pUIInfo->pWindowInfo->is_created_internally = TRUE;

    // Set the window id to valid value
    gst_display_set_window_id((GstXOverlay*) display, win);
}

/**
 * Allows for specifying a window id to be used for video display.
 * This method will be called externally when a window is to be
 * used which was created outside this plugin.
 *
 * NOTE: If the supplied window id is 0, this plugin will
 * create its own window for display.
 *
 * @param overlay this plugin instance.
 * @xwindow_id ID of window to be used for display.
 */
void gst_display_set_window_id(GstXOverlay* overlay, gulong window_id)
{
    GST_DEBUG("function entry");
    GstDisplay *display = GST_DISPLAY(overlay);

    g_return_if_fail(GST_IS_DISPLAY(display));

    // If we already use that window return
    if (window_id == display->pUIInfo->pWindowInfo->win)
    {
        GST_DEBUG(
                "already using this window %ld, no additional actions required",
                window_id);
        return;
    }

    // If the element has not initialized the display try to do so.
    if (NULL == display->pUIInfo->pDisplayInfo)
    {
        gst_display_info_get(display);
        if (NULL == display->pUIInfo->pDisplayInfo)
        {
            GST_ERROR("display information could not be initialized");
            return;
        }
    }

    /* *TODO* - remove this because it should not be necessary
     * to clear the pool just because a window has been supplied
     // Clear image pool as the images are unusable anyway
     gst_display_imagepool_clear(display);

     // Acquire the flow lock since messing with image buffers
     g_mutex_lock(display->pUIInfo->flow_lock);

     // Clear the image
     if (display->fill_image)
     {
     gst_display_image_buffer_free(display->fill_image);
     display->fill_image = NULL;
     }

     g_mutex_unlock(display->pUIInfo->flow_lock);
     */
    // If a window is there already we destroy it
    if (0 != display->pUIInfo->pWindowInfo->win)
    {
        GST_DEBUG("destroying the existing window");

        // Window lock is acquired in gst_display_window_destroy()
        gst_display_window_destroy(display);
    }

    // Lock down the window because we are now setting window id
    g_mutex_lock(display->pUIInfo->window_lock);

    // Now get information about the display window
    if (NULL != display->pUIInfo->pWindowInfo->pWindowOSInfo)
    {
        // Populate the window structure with information about the window
        // including setting the window id
        window_update_info(display->pUIInfo, window_id);
    }

    GST_DEBUG("setting window id to %ld", window_id);

    display->pUIInfo->pWindowInfo->win = window_id;

    // Inform superclass that the have an window id
    GST_DEBUG("informing superclasss of window id");
    gst_x_overlay_got_xwindow_id(GST_X_OVERLAY(display), window_id);

    g_mutex_unlock(display->pUIInfo->window_lock);
}

/**
 * Updates the aspect ratio factors since the output configuration
 * parameters could have changed.
 *
 * @param display current plugin instance.
 */
void gst_display_update_configuration(GstDisplay* display, guint videoWidth,
        guint videoHeight, guint videoPARx, guint videoPARy)
{
    GST_DEBUG("function entry");

    // The only thing that needs to be done is to adjust the
    // video and graphics aspect ratio factors
    //
    g_mutex_lock(display->pUIInfo->flow_lock);

    // Set the properties that support video parameters, graphics are set in display.c
    g_object_set(G_OBJECT(display), "output-video-width", videoWidth, NULL);
    g_object_set(G_OBJECT(display), "output-video-height", videoHeight, NULL);
    g_object_set(G_OBJECT(display), "output-video-par-x", videoPARx, NULL);
    g_object_set(G_OBJECT(display), "output-video-par-y", videoPARy, NULL);

    g_mutex_unlock(display->pUIInfo->flow_lock);

    gst_display_video_aspect_ratio_adjust(display);

    gst_display_graphics_aspect_ratio_adjust(display);
}

/**
 * Disables or re-enables the display of video frames based on
 * the supplied parameter.
 *
 * @param display current plugin instance.
 * @param block   disables video presentation if true, enables
 *                video presenation if false
 */
void gst_display_video_block(GstDisplay* display, gboolean block)
{
    GST_DEBUG("function entry");

    if ((block) && (!display->pUIInfo->pVideoInfo->is_blocked))
    {
        // Currently not blocked and requesting to block
        display->pUIInfo->pVideoInfo->is_blocked = TRUE;
    }
    else if ((!block) && (display->pUIInfo->pVideoInfo->is_blocked))
    {
        // Currently blocked and requesting to unblock
        display->pUIInfo->pVideoInfo->is_blocked = FALSE;
    }
}

/**
 * Disables update of the video frames and shows current frame
 * until resumed.
 *
 * @param display current plugin instance.
 */
void gst_display_video_freeze(GstDisplay* display)
{
    GST_DEBUG("function entry");

    // Set flag so video frames are no longer updated
    if (!display->pUIInfo->pVideoInfo->is_frozen)
    {
        // Currently not frozen, requesting to freeze
        display->pUIInfo->pVideoInfo->is_frozen = TRUE;
    }
}

/**
 * Re-enables update of the video frames.
 *
 * @param display current plugin instance.
 */
void gst_display_video_resume(GstDisplay* display)
{
    GST_DEBUG("function entry");

    // Set flag so video frames are updated
    if (display->pUIInfo->pVideoInfo->is_frozen)
    {
        // Currently frozen, requesting to resume
        display->pUIInfo->pVideoInfo->is_frozen = FALSE;
    }
}

//
/////////////////// Update & Reset Methods end ////////////////////////////////


//
/* =========================================== */
/*                                             */
/*              Init & Class init              */
/*                                             */
/* =========================================== */

/**
 * Performs the actions necessary to establish the display
 * plugin within the GStreamer framework.
 *
 * @return the object which defines this display plugin
 */
GType gst_display_get_type(void)
{
    static GType display_type = 0;

    // Set up GstDisplay object
    if (!display_type)
    {
        static const GTypeInfo display_info =
        { sizeof(GstDisplay), gst_display_base_init, // base_init function
                NULL, // base_finalize function
                (GClassInitFunc) gst_display_class_init, // class_init function
                NULL, // class_finalize function
                NULL, // class_data
                sizeof(GstDisplay), //
                0, // n_preallocations
                (GInstanceInitFunc) gst_display_instance_init, // instance_init function
                };
        display_type = g_type_register_static(GST_TYPE_VIDEO_SINK,
                "GstDisplay", &display_info, 0);

        // Add a interface initialization method
        static const GInterfaceInfo iface_info =
        { (GInterfaceInitFunc) gst_display_interface_init, // interface_init
                NULL, // interface_finalize
                NULL, // interface_data
                };
        g_type_add_interface_static(display_type,
                GST_TYPE_IMPLEMENTS_INTERFACE, &iface_info);

        // Add a navigation interface
        static const GInterfaceInfo navigation_info =
        { (GInterfaceInitFunc) gst_display_navigation_init, // interface_init
                NULL, // interface_finalize
                NULL, // interface_data
                };
        g_type_add_interface_static(display_type, GST_TYPE_NAVIGATION,
                &navigation_info);

        // Add an overlay interface
        static const GInterfaceInfo overlay_info =
        { (GInterfaceInitFunc) gst_display_overlay_init, // inteface_init
                NULL, // interface_finalize
                NULL, // interface_data
                };
        g_type_add_interface_static(display_type, GST_TYPE_X_OVERLAY,
                &overlay_info);

        // Add a reference to the image buffer class which is used by this plugin
        (void) g_type_class_ref(gst_display_image_buffer_get_type());
    }

    return display_type;
}

/**
 * Initializes class and child class properties during each new child class creation
 *
 * @param g_class child class which will be image buffer
 */
static void gst_display_base_init(gpointer g_class)
{
    // Logging hasn't been initialized yet so use g_print
    g_print("gst_display_base_init called\n");

    GstElementClass *element_class = GST_ELEMENT_CLASS(g_class);

    gst_element_class_set_details(element_class,
            &gst_display_image_buffer_details);

    gst_element_class_add_pad_template(element_class,
            gst_static_pad_template_get(
                    &gst_display_image_buffer_template_factory));
}

/**
 * Initializes the class only once (specifying what signals, arguments and
 * virtual functions the class has and setting up global state).
 *
 * @param klass   object used to initialize plugin class
 */
static void gst_display_class_init(GstDisplay* klass)
{
    // Initialize logging
    GST_DEBUG_CATEGORY_INIT(display_debug, "display", 0,
            "CableLabs Overlay sink");

    // Uncomment this to get debug level messages no matter
    // what the level is set to for other components
    //gst_debug_category_set_threshold(display_debug,
    //      GST_LEVEL_DEBUG);

    GST_DEBUG("function entry");
    GObjectClass *gobject_class;
    GstElementClass *gstelement_class;
    GstBaseSinkClass *gstbasesink_class;

    gobject_class = (GObjectClass *) klass;
    gstelement_class = (GstElementClass *) klass;
    gstbasesink_class = (GstBaseSinkClass *) klass;

    parent_class = g_type_class_peek_parent(klass);

    gobject_class->set_property = gst_display_set_property;
    gobject_class->get_property = gst_display_get_property;

    g_object_class_install_property(gobject_class, ARG_PIXEL_ASPECT_RATIO,
            g_param_spec_string("pixel-aspect-ratio", "Pixel Aspect Ratio",
                    "The pixel aspect ratio of the device", "1/1",
                    G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, ARG_FORCE_ASPECT_RATIO,
            g_param_spec_boolean("force-aspect-ratio", "Force aspect ratio",
                    "When enabled, scaling will respect original aspect ratio",
                    FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            ARG_FIXED_WINDOW,
            g_param_spec_boolean(
                    "fixed-window",
                    "Fixed window style",
                    "When enabled, window will be in fixed position with no borders",
                    FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(
            gobject_class,
            ARG_SUPPLIED_WINDOW,
            g_param_spec_boolean(
                    "supplied-window",
                    "Externally supplied window",
                    "When enabled, it is expected that window will be supplied rather than created by this plugin",
                    FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class,
            ARG_BACKGROUND_COLOR_RGBA8888, g_param_spec_ulong(
                    "background-screen-color", "Background screen color",
                    "Background color specified as an RGBA 8888 color", 0,
                    0x00FFFFFFFF, 0x00000000FF, G_PARAM_READABLE
                            | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_GRAPHICS_SURFACE,
            g_param_spec_pointer("graphics-surface-pointer",
                    "Graphics surface pointer",
                    "Pointer to instance of GraphicsInfo", G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, ARG_WINDOW_WIDTH,
            g_param_spec_int("window-width", "window width",
                    "Width of display window",
                    // realistic values
                    //           640, 960, 640,
                    // experimental, flexible values
                    100, 2000, 640, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_WINDOW_HEIGHT,
            g_param_spec_int("window-height", "window height",
                    "Height of display window",
                    // realistic values
                    //           480, 540, 480,
                    // experimental, flexible values
                    100, 2000, 480, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_INCOMING_VIDEO_WIDTH,
            g_param_spec_int("incoming-video-width", "video incoming width",
                    "Width of incoming video in pixels",
                    // experimental, flexible values
                    100, 2000, 640, //(min, max, default)
                    G_PARAM_READABLE));

    g_object_class_install_property(gobject_class, ARG_INCOMING_VIDEO_HEIGHT,
            g_param_spec_int("incoming-video-height", "video incoming height",
                    "Height of incoming video in pixels",
                    // experimental, flexible values
                    100, 2000, 480, //(min, max, default)
                    G_PARAM_READABLE));

    g_object_class_install_property(gobject_class, ARG_INCOMING_VIDEO_PAR_X,
            g_param_spec_int("incoming-video-par-x",
                    "incoming video pixel aspect ratio x",
                    "Incoming Video Pixel Aspect Ratio numerator value", 1,
                    100, 1, //(min, max, default)
                    G_PARAM_READABLE));

    g_object_class_install_property(gobject_class, ARG_INCOMING_VIDEO_PAR_Y,
            g_param_spec_int("incoming-video-par-y",
                    "incoming video pixel aspect ratio y",
                    "Incoming Video Pixel Aspect Ratio denominator value", 1,
                    100, 1, //(min, max, default)
                    G_PARAM_READABLE));

    g_object_class_install_property(gobject_class, ARG_OUTPUT_VIDEO_WIDTH,
            g_param_spec_int("output-video-width", "video output width",
                    "Width of video output in pixels",
                    // experimental, flexible values
                    100, 2000, 640, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_OUTPUT_VIDEO_HEIGHT,
            g_param_spec_int("output-video-height", "video output height",
                    "Height of video output in pixels",
                    // experimental, flexible values
                    100, 2000, 480, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_OUTPUT_VIDEO_PAR_X,
            g_param_spec_int("output-video-par-x",
                    "video output pixel aspect ratio x",
                    "Output Video Pixel Aspect Ratio numerator value", 1, 2000,
                    1, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_OUTPUT_VIDEO_PAR_Y,
            g_param_spec_int("output-video-par-y",
                    "video output pixel aspect ratio y",
                    "Output Video Pixel Aspect Ratio denominator value", 1,
                    2000, 1, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_SRC_X,
            g_param_spec_float("scaled-video-src-x",
                    "scaled video source x position",
                    "Horizontal screen position of source video to be scaled",
                    0.0, 1.0, 0.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_SRC_Y,
            g_param_spec_float("scaled-video-src-y",
                    "scaled video source y position",
                    "Vertical screen position of source video to be scaled",
                    0.0, 1.0, 0.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_SRC_WIDTH,
            g_param_spec_float("scaled-video-src-width",
                    "scaled video source width",
                    "Width of source video to be scaled", 0, 1.0, 1.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_SRC_HEIGHT,
            g_param_spec_float("scaled-video-src-height",
                    "scaled video source height",
                    "Height of source video to be scaled", 0, 1.0, 1.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_DEST_X,
            g_param_spec_float("scaled-video-dest-x",
                    "scaled video destination x position",
                    "Destination horizontal screen position of scaled video",
                    0.0, 1.0, 0.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_DEST_Y,
            g_param_spec_float("scaled-video-dest-y",
                    "scaled video destination y position",
                    "Destination vertical screen position of scaled video",
                    0.0, 1.0, 0.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_SCALED_VIDEO_DEST_WIDTH,
            g_param_spec_float("scaled-video-dest-width",
                    "scaled video destination width",
                    "Destination width of scaled video", 0, 1.0, 1.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class,
            ARG_SCALED_VIDEO_DEST_HEIGHT, g_param_spec_float(
                    "scaled-video-dest-height",
                    "scaled video destination height",
                    "Destination height of scaled video", 0, 1.0, 1.0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class,
            ARG_HW_ACCELERATION_DISABLED, g_param_spec_boolean(
                    "hw-acceleration-disabled",
                    "Disable OpenGL Hardware Acceleration",
                    "When enabled, Hardware acceleration will not be used",
                    FALSE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, ARG_DFC_VALUE,
            g_param_spec_int("dfc", "video decoder format conversion",
                    "Video decoder format conversion value", -1, 200, 0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(gobject_class, ARG_DFC_DEFAULT,
            g_param_spec_int("dfc_default",
                    "video decoder format conversion default",
                    "Video decoder format conversion default for platform", -1,
                    200, 0, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    g_object_class_install_property(
            gobject_class,
            ARG_FORCE_RGB_VIDEO,
            g_param_spec_boolean(
                    "force-rgb-video",
                    "Force format of video to be RGB",
                    "When enabled, display will not accept YUV format, only RGB, used for testing",
                    TRUE, G_PARAM_READWRITE));

    g_object_class_install_property(gobject_class, ARG_AFD_VALUE,
            g_param_spec_int("incoming-video-afd",
                    "video active format descriptor",
                    "Video active format descriptor value", -1, 20, -1, //(min, max, default)
                    G_PARAM_READABLE | G_PARAM_WRITABLE));

    gobject_class->finalize = gst_display_finalize;

    gstelement_class->change_state
            = GST_DEBUG_FUNCPTR(gst_display_change_state);

    gstbasesink_class->get_caps = GST_DEBUG_FUNCPTR(gst_display_get_caps);
    gstbasesink_class->set_caps = GST_DEBUG_FUNCPTR(gst_display_set_caps);
    gstbasesink_class->buffer_alloc = GST_DEBUG_FUNCPTR(
            gst_display_buffer_alloc);
    gstbasesink_class->get_times = GST_DEBUG_FUNCPTR(gst_display_get_times);
    gstbasesink_class->preroll = GST_DEBUG_FUNCPTR(gst_display_show_frame);
    gstbasesink_class->render = GST_DEBUG_FUNCPTR(gst_display_show_frame);
    gstbasesink_class->event = GST_DEBUG_FUNCPTR(gst_display_event);
}

/**
 * Initialize an instance of the GstDisplay class.
 *
 * @param display plugin instance to be initialized.
 */
static void gst_display_instance_init(GstDisplay* display)
{
    GST_DEBUG("function entry");

    // No need to acuire mutexs here because we haven't even created them yet

    display->fill_image = NULL;
    display->display_image = NULL;
    display->force_rgb_video = FALSE;

    // Allocate structure for UI info
    display->pUIInfo = g_try_new0(UIInfo, 1);

    if (NULL == display->pUIInfo)
    {
        fprintf(stderr, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
        exit(-1);
    }

    // Allocate structure for window info
    display->pUIInfo->pWindowInfo = g_try_new0(WindowInfo, 1);

    // Set size to default values
    if (NULL != display->pUIInfo->pWindowInfo)
    {
        display->pUIInfo->pWindowInfo->width = DEFAULT_SCREEN_WIDTH;
        display->pUIInfo->pWindowInfo->height = DEFAULT_SCREEN_HEIGHT;

        // Set externally supplied window as default
        display->pUIInfo->pWindowInfo->is_supplied = TRUE;
    }

    // Allocate structure for video info
    display->pUIInfo->pVideoInfo = g_try_new0(VideoInfo, 1);
    if (NULL != display->pUIInfo->pVideoInfo)
    {
        display->pUIInfo->pVideoInfo->incoming_par = g_try_new0(GValue, 1);
        (void) g_value_init(display->pUIInfo->pVideoInfo->incoming_par,
                GST_TYPE_FRACTION);
        gst_value_set_fraction(display->pUIInfo->pVideoInfo->incoming_par, 1, 1);
        display->pUIInfo->pVideoInfo->incoming_par_d = 1;
        display->pUIInfo->pVideoInfo->incoming_par_n = 1;

        display->pUIInfo->pVideoInfo->output_width = DEFAULT_SCREEN_WIDTH;
        display->pUIInfo->pVideoInfo->output_height = DEFAULT_SCREEN_HEIGHT;

        display->pUIInfo->pVideoInfo->output_par_d = 1;
        display->pUIInfo->pVideoInfo->output_par_n = 1;

        // default video scaling
        display->pUIInfo->pVideoInfo->scale_src_x = 0.0;
        display->pUIInfo->pVideoInfo->scale_src_y = 0.0;
        display->pUIInfo->pVideoInfo->scale_src_width = 1.0;
        display->pUIInfo->pVideoInfo->scale_src_height = 1.0;

        display->pUIInfo->pVideoInfo->scale_dest_x = 0.0;
        display->pUIInfo->pVideoInfo->scale_dest_y = 0.0;
        display->pUIInfo->pVideoInfo->scale_dest_width = 1.0;
        display->pUIInfo->pVideoInfo->scale_dest_height = 1.0;

        display->pUIInfo->pVideoInfo->is_dfc_default = TRUE;
        display->pUIInfo->pVideoInfo->dfc = 0;
        display->pUIInfo->pVideoInfo->dfc_default = 0;

        display->pUIInfo->pVideoInfo->afd = -1;

        display->pUIInfo->pVideoInfo->clip_buffer_size = 0;
        display->pUIInfo->pVideoInfo->clip_buffer_width = 0;
        display->pUIInfo->pVideoInfo->clip_buffer_height = 0;
        display->pUIInfo->pVideoInfo->pClipBuffer = NULL;
        display->pUIInfo->pVideoInfo->clip_conversion_buffer_size = 0;
        display->pUIInfo->pVideoInfo->pClipConversionBuff = NULL;

        display->pUIInfo->pVideoInfo->conversion_buffer_size = 0;
        display->pUIInfo->pVideoInfo->pConversionBuff = NULL;

        display->pUIInfo->pVideoInfo->freeze_size = 0;
        display->pUIInfo->pVideoInfo->freeze_width = 0;
        display->pUIInfo->pVideoInfo->freeze_height = 0;
        display->pUIInfo->pVideoInfo->freeze_buffer = NULL;

        display->pUIInfo->pVideoInfo->block_screen_size = 0;
        display->pUIInfo->pVideoInfo->block_screen_width = 0;
        display->pUIInfo->pVideoInfo->block_screen_height = 0;
        display->pUIInfo->pVideoInfo->block_screen_buffer = NULL;
    }

    // Allocate structure for background info
    display->pUIInfo->pBackgroundInfo = g_try_new0(BackgroundInfo, 1);
    if (NULL != display->pUIInfo->pBackgroundInfo)
    {
        display->pUIInfo->pBackgroundInfo->background_color = 0x00000000FF;
        display->pUIInfo->pBackgroundInfo->update_needed = FALSE;
    }

    // Don't allocate image buffer structures since they will
    // be allocated when the GstDisplayImageBuffers are created
    display->pUIInfo->pFillImageBuffer = NULL;
    display->pUIInfo->pDisplayImageBuffer = NULL;

    // Don't allocate graphics structure since that is
    // allocated by Display.c
    display->pUIInfo->pGraphicsInfo = NULL;

    // Don't allocate structure here since processing needs
    // to happen to determine display characteristics
    display->pUIInfo->pDisplayInfo = NULL;

    display->keep_aspect = FALSE;
    display->pUIInfo->hw_acceleration_disabled = FALSE;

    display->fps_n = 0;
    display->fps_d = 0;

    display->pUIInfo->window_lock = g_mutex_new();
    display->pUIInfo->flow_lock = g_mutex_new();

    display->image_pool = NULL;
    display->pool_lock = g_mutex_new();

    display->running = FALSE;

    // set default screen dimensions on the superclass
    GST_VIDEO_SINK_WIDTH(display) = DEFAULT_SCREEN_WIDTH;
    GST_VIDEO_SINK_HEIGHT(display) = DEFAULT_SCREEN_HEIGHT;
}

/**
 * Defines the method in this plugin which will allows
 * this plugin to be queried about which interfaces it supports.
 *
 * @param klass   object which will contain the function in
 *                this plugin to call to determine if an
 *                interface is supported
 */
static void gst_display_interface_init(GstImplementsInterfaceClass * klass)
{
    GST_DEBUG("function entry");
    klass->supported = gst_display_interface_supported;
}

/**
 * Defines the methods in this plugin which will be called when
 * this plugin needs to generate navigation events that inform
 * upstream elements about changes to this plugin.
 *
 * @param   iface navigation interface which is supported by
 *                this plugin and currently being initialized
 */
static void gst_display_navigation_init(GstNavigationInterface * iface)
{
    GST_DEBUG("function entry");
    iface->send_event = gst_display_navigation_send_event;
}

/**
 * Defines the methods in this plugin which will be called by the
 * XOverlay interface which is part of a Video Sink in the
 * GStreamer infrastructure.
 *
 * @param iface the interface instance being initialized.
 */
static void gst_display_overlay_init(GstXOverlayClass* iface)
{
    GST_DEBUG("function entry");

    // This allows a window to be created externally & used by this plugin
    iface->set_xwindow_id = gst_display_set_window_id;

    // There is an expose function but it does nothing when running on Win32
    iface->expose = gst_display_expose;

    // This plugin does no event handling so leave this as null
    //iface->handle_events = gst_display_set_event_handling;
}

//
/////////////////// Initialization Methods end ////////////////////////////////
//


/* =========================================== */
/*                                             */
/*        Retrieval & Get Info Methods         */
/*                                             */
/* =========================================== */

/**
 * Determine if the supplied type is supported by this plugin
 *
 * @param iface   specific interface to examine, ignored by this plugin
 * @param type    type of interface to check
 * @return  true if supplied interface type is supported by this plugin,
 *          false otherwise
 */
static gboolean gst_display_interface_supported(GstImplementsInterface* iface,
        GType type)
{
    GST_DEBUG("function entry");
    g_assert(type == GST_TYPE_NAVIGATION || type == GST_TYPE_X_OVERLAY);
    return TRUE;
}

/**
 * Allows for base sink element to get the application time and allow for
 * display to control rendering speed.  The base sink element does not know
 * about application level timing so this method provides a method to allow
 * application specific timing control.
 *
 * @param bsink   display plugin
 * @param buf     image buffer to retrieve info from
 * @param start   start time retreived from buffer
 * @param end     end time retrieved from buffer
 */
static void gst_display_get_times(GstBaseSink* bsink, GstBuffer* buf,
        GstClockTime* start, GstClockTime* end)
{
    //GST_DEBUG("function entry");

    GstDisplay *display = GST_DISPLAY(bsink);

    GstClock* gstClock = gst_element_get_clock(GST_ELEMENT(bsink));
    if (NULL == gstClock)
    {
        GST_ERROR_OBJECT(display, "Unable to get element clock");
        gst_object_unref(gstClock);
        return;
    }
    GstClockTime now = gst_clock_get_time(gstClock);
    GstClockTime base_time = gst_element_get_base_time(GST_ELEMENT(bsink));
    GstClockTime stream_time = now - base_time;

    gst_object_unref(gstClock);

    GST_LOG_OBJECT(display, "now = %" GST_TIME_FORMAT ", base_time = %"
            GST_TIME_FORMAT ", stream_time = %" GST_TIME_FORMAT, GST_TIME_ARGS(now),
            GST_TIME_ARGS(base_time), GST_TIME_ARGS(stream_time));

    if (GST_BUFFER_TIMESTAMP_IS_VALID(buf))
    {
        *start = GST_BUFFER_TIMESTAMP(buf);

        if (base_time > 0) // don't adjust buffers in live pipelines
        {
            GstClockTimeDiff start_delta = *start - stream_time;
            if (start_delta > GST_SECOND)
            {
                GST_INFO_OBJECT(display,
                        "start = %" GST_TIME_FORMAT ", stream_time = %" GST_TIME_FORMAT ", %llis",
                        GST_TIME_ARGS(*start), GST_TIME_ARGS(stream_time), start_delta);

                //*start -= start_delta;
                if (start_delta > (GST_SECOND * 2))
                {
                    GST_ERROR_OBJECT(display,
                        "Buffer is over 1 second early (approx."
                            " %llis) - this might cause a deadlock!!!",
                        start_delta / GST_SECOND);
                }
            }
        }

        if (GST_BUFFER_DURATION_IS_VALID(buf))
        {
            *end = *start + GST_BUFFER_DURATION(buf);
        }
        else
        {
            if (display->fps_n > 0)
            {
                *end = *start + gst_util_uint64_scale_int(GST_SECOND,
                        display->fps_d, display->fps_n);
            }
        }
    }
}

/**
 * Allows for applications to retrieve internal properties of this plugin.
 *
 * @param object instance of plugin.
 * @param prop_id identifier of the property being set.
 * @param value contains return value for the property.
 * @param pspec property spec
 */
static void gst_display_get_property(GObject* object, guint prop_id,
        GValue* value, GParamSpec* pspec)
{
    GST_DEBUG("function entry");

    GstDisplay* display;

    g_return_if_fail(GST_IS_DISPLAY(object));

    display = GST_DISPLAY(object);

    switch (prop_id)
    {
    case ARG_PIXEL_ASPECT_RATIO:
        if (display->pUIInfo->pVideoInfo->incoming_par)
            (void) g_value_transform(
                    display->pUIInfo->pVideoInfo->incoming_par, value);
        break;

    case ARG_FORCE_ASPECT_RATIO:
        g_value_set_boolean(value, display->keep_aspect);
        break;

    case ARG_FIXED_WINDOW:
        g_value_set_boolean(value, display->pUIInfo->pWindowInfo->is_fixed);
        break;

    case ARG_SUPPLIED_WINDOW:
        g_value_set_boolean(value, display->pUIInfo->pWindowInfo->is_supplied);
        break;

    case ARG_BACKGROUND_COLOR_RGBA8888:
        g_value_set_ulong(value,
                display->pUIInfo->pBackgroundInfo->background_color);
        break;

    case ARG_WINDOW_HEIGHT:
        g_value_set_int(value, display->pUIInfo->pDisplayInfo->height);
        break;

    case ARG_WINDOW_WIDTH:
        g_value_set_int(value, display->pUIInfo->pDisplayInfo->width);
        break;

    case ARG_INCOMING_VIDEO_HEIGHT:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->incoming_height);
        break;

    case ARG_INCOMING_VIDEO_WIDTH:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->incoming_width);
        break;

    case ARG_INCOMING_VIDEO_PAR_X:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->incoming_par_n);
        break;

    case ARG_INCOMING_VIDEO_PAR_Y:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->incoming_par_d);
        break;

    case ARG_OUTPUT_VIDEO_HEIGHT:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->output_height);
        break;

    case ARG_OUTPUT_VIDEO_WIDTH:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->output_width);
        break;

    case ARG_OUTPUT_VIDEO_PAR_X:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->output_par_n);
        break;

    case ARG_OUTPUT_VIDEO_PAR_Y:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->output_par_d);
        break;

    case ARG_SCALED_VIDEO_SRC_X:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_src_x);
        break;

    case ARG_SCALED_VIDEO_SRC_Y:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_src_y);
        break;
    case ARG_SCALED_VIDEO_SRC_WIDTH:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_src_width);
        break;

    case ARG_SCALED_VIDEO_SRC_HEIGHT:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_src_height);
        break;

    case ARG_SCALED_VIDEO_DEST_X:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_dest_x);
        break;

    case ARG_SCALED_VIDEO_DEST_Y:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_dest_y);
        break;
    case ARG_SCALED_VIDEO_DEST_WIDTH:
        g_value_set_float(value, display->pUIInfo->pVideoInfo->scale_dest_width);
        break;

    case ARG_SCALED_VIDEO_DEST_HEIGHT:
        g_value_set_float(value,
                display->pUIInfo->pVideoInfo->scale_dest_height);
        break;

    case ARG_HW_ACCELERATION_DISABLED:
        g_value_set_boolean(value, display->pUIInfo->hw_acceleration_disabled);
        break;

    case ARG_DFC_VALUE:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->dfc);
        break;

    case ARG_DFC_DEFAULT:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->dfc_default);
        break;

    case ARG_FORCE_RGB_VIDEO:
        g_value_set_boolean(value, display->force_rgb_video);
        break;

    case ARG_AFD_VALUE:
        g_value_set_int(value, display->pUIInfo->pVideoInfo->afd);
        break;

    default:
        G_OBJECT_WARN_INVALID_PROPERTY_ID(object, prop_id, pspec);
        break;
    }
}

/**
 * Gets information about the computer display. Everything is
 * stored in our object and will be cleaned when the object is disposed. Note
 * here that caps for supported format are generated without any window or
 * image creation.  This function also sets the capabilities for this plugin
 * based on the display information.
 *
 * @param display the plugin instance
 */
void gst_display_info_get(GstDisplay * display)
{
    GST_DEBUG("function entry");

    g_return_if_fail(GST_IS_DISPLAY(display));

    // If display info has already been retrieved, return
    if ((NULL != display->pUIInfo->pDisplayInfo) && (0
            < display->pUIInfo->pDisplayInfo->width))
    {
        // Nothing to initialize
        return;
    }

    g_mutex_lock(display->pUIInfo->window_lock);

    if (NULL == display->pUIInfo->pDisplayInfo)
    {
        display->pUIInfo->pDisplayInfo = g_try_new0(DisplayInfo, 1);
    }

    // get information about the display using platform-specific calls
    if (FALSE == window_init_display(display->pUIInfo))
    {
        GST_WARNING_OBJECT(display, "Unable to initialize display context");
        g_mutex_unlock(display->pUIInfo->window_lock);
        return;
    }

    GST_DEBUG_OBJECT(display, "Display info: %dx%d pixels and %d mm x %d mm",
            display->pUIInfo->pDisplayInfo->width,
            display->pUIInfo->pDisplayInfo->height,
            display->pUIInfo->pDisplayInfo->widthmm,
            display->pUIInfo->pDisplayInfo->heightmm);

    gst_display_pixel_aspect_ratio_calculate(display->pUIInfo->pDisplayInfo);

    // Set the RGB related parameters
    display->pUIInfo->pDisplayInfo->bpp = 32;
    display->pUIInfo->pDisplayInfo->depth = 32;

    if (!display->force_rgb_video)
    {
        display->caps = gst_caps_new_simple("video/x-raw-rgb", "bpp",
                G_TYPE_INT, display->pUIInfo->pDisplayInfo->bpp, "depth",
                G_TYPE_INT, display->pUIInfo->pDisplayInfo->depth, "red_mask",
                G_TYPE_INT, 0xff000000, "green_mask", G_TYPE_INT, 0x00ff0000,
                "blue_mask", G_TYPE_INT, 0x0000ff00, "alpha_mask", G_TYPE_INT,
                0x000000ff, "width", GST_TYPE_INT_RANGE, 1, G_MAXINT, "height",
                GST_TYPE_INT_RANGE, 1, G_MAXINT, "framerate",
                GST_TYPE_FRACTION_RANGE, 0, 1, G_MAXINT, 1, NULL);

        GstCaps* gCapsTemp = gst_caps_new_simple("video/x-raw-yuv",
                "endianness", G_TYPE_INT,
                display->pUIInfo->pDisplayInfo->endianness, "format",
                GST_TYPE_FOURCC, GST_MAKE_FOURCC('I', '4', '2', '0'), "width",
                GST_TYPE_INT_RANGE, 1, G_MAXINT, "height", GST_TYPE_INT_RANGE,
                1, G_MAXINT, "framerate", GST_TYPE_FRACTION_RANGE, 0, 1,
                G_MAXINT, 1, NULL);

        gst_caps_append(display->caps, gCapsTemp);
    }
    // Want to only advertise that RGB is supported so ffmpeg colorspace element does conversion
    else
    {
        display->caps = gst_caps_new_simple("video/x-raw-rgb", "bpp",
                G_TYPE_INT, display->pUIInfo->pDisplayInfo->bpp, "depth",
                G_TYPE_INT, display->pUIInfo->pDisplayInfo->depth, "red_mask",
                G_TYPE_INT, 0xff000000, "green_mask", G_TYPE_INT, 0x00ff0000,
                "blue_mask", G_TYPE_INT, 0x0000ff00, "alpha_mask", G_TYPE_INT,
                0x000000ff, "width", GST_TYPE_INT_RANGE, 1, G_MAXINT, "height",
                GST_TYPE_INT_RANGE, 1, G_MAXINT, "framerate",
                GST_TYPE_FRACTION_RANGE, 0, 1, G_MAXINT, 1, NULL);
    }

    if (gst_caps_is_empty(display->caps))
    {
        GST_ELEMENT_ERROR(display, STREAM, WRONG_TYPE, (NULL),
                ("No caps were generated"));
    }
    else
    {
        // print out the generated caps
        gchar* caps_str = gst_caps_to_string(display->caps);
        GST_LOG_OBJECT(display, "Generated the following caps: %s", caps_str);
        g_free(caps_str);
    }

    g_mutex_unlock(display->pUIInfo->window_lock);
}

/**
 * Gets the capabilities of this plugin.
 *
 * This is an infrastructure function call used by GStreamer to negotiate
 * capabilities with upstream plugin components.
 *
 * @param bsink this plugin instance(base class).
 */
static GstCaps *
gst_display_get_caps(GstBaseSink * bsink)
{
    GST_DEBUG("function entry");
    GstDisplay *display;

    display = GST_DISPLAY(bsink);

    if (display->caps)
    {
        // print out the generated caps
        gchar* caps_str = gst_caps_to_string(display->caps);
        GST_LOG_OBJECT(display, "Returning the following caps: %s", caps_str);
        g_free(caps_str);

        return gst_caps_ref(display->caps);
    }

    return gst_caps_copy(gst_pad_get_pad_template_caps(GST_VIDEO_SINK_PAD(
            display)));
}

//
/////////////////// Retrieval & Get Info Methods end ////////////////////////////////
//


/* ============================================================= */
/*                                                               */
/*               Destruction & Cleanup Methods                   */
/*                                                               */
/* ============================================================= */
/**
 * Destroys the window representing the display screen.
 * The window that will be destroyed is identified in the
 * operating system-specific structure contained
 * in the GstDisplay's screen information structure.
 *
 * Since the screen is being destroyed, the corresponding overlay surface
 * will also be destroyed.
 *
 * @param display current GstDisplay instance.
 */
static void gst_display_window_destroy(GstDisplay* display)
{
    GST_DEBUG("function entry");

    g_return_if_fail(GST_IS_DISPLAY(display));

    g_mutex_lock(display->pUIInfo->window_lock);

    if ((NULL != display->pUIInfo->pWindowInfo) && (0
            != display->pUIInfo->pWindowInfo->win))
    {
        // If we created the window...
        if (TRUE == display->pUIInfo->pWindowInfo->is_created_internally)
        {
            // ...nuke it
            window_close(display->pUIInfo->pWindowInfo);
        }
        else
        {
            // If created externally, don't destroy it, just flush graphics
            window_flush_graphics(display->pUIInfo->pWindowInfo);
        }
    }

    g_mutex_unlock(display->pUIInfo->window_lock);
}

/**
 * Finalize is called only once, dispose can be called multiple times.
 * We use mutexes and don't reset stuff to NULL here so let's register
 * as a finalize.
 *
 * @param   object   object which is being disposed
 */
static void gst_display_finalize(GObject* object)
{
    GST_DEBUG("function entry");
    GstDisplay* display;

    display = GST_DISPLAY(object);

    gst_display_reset(display);

    if (display->pUIInfo->pVideoInfo->incoming_par)
    {
        g_free(display->pUIInfo->pVideoInfo->incoming_par);
        display->pUIInfo->pVideoInfo->incoming_par = NULL;
    }
    if (display->pUIInfo->window_lock)
    {
        g_mutex_free(display->pUIInfo->window_lock);
        display->pUIInfo->window_lock = NULL;
    }
    if (display->pUIInfo->flow_lock)
    {
        g_mutex_free(display->pUIInfo->flow_lock);
        display->pUIInfo->flow_lock = NULL;
    }
    if (display->pool_lock)
    {
        g_mutex_free(display->pool_lock);
        display->pool_lock = NULL;
    }

    G_OBJECT_CLASS(parent_class)->finalize(object);
}

//
/////////////////// Destruction & Cleanup Methods end ////////////////////////////////
//


/* ============================================================= */
/*                                                               */
/*                       Utility Methods                         */
/*                                                               */
/* ============================================================= */

/*
 * Calculates the pixel aspect ratio of the physical display
 * based on the properties in the displayInfo structure and
 * stores it there.
 *
 * @param displayInfo information about the physical display.
 */
static void gst_display_pixel_aspect_ratio_calculate(DisplayInfo* displayInfo)
{
    GST_DEBUG("funtion entry");

    // *TODO* - What are these???
    static const gint par[][2] =
    {
    { 1, 1 }, /* regular screen */
    { 16, 15 }, /* PAL TV */
    { 11, 10 }, /* 525 line Rec.601 video */
    { 54, 59 }, /* 625 line Rec.601 video */
    { 64, 45 }, /* 1280x1024 on 16:9 display */
    { 5, 3 }, /* 1280x1024 on 4:3 display */
    { 4, 3 } /*  800x600 on 16:9 display */
    };

    guint i;
    guint index;
    gdouble ratio;
    gdouble delta;

#define DELTA(idx)(ABS(ratio -((gdouble) par[idx][0] / par[idx][1])))

    // *TODO* - have these values been set correctly???

    // First calculate the "real" or "physical display" pixel aspect ratio
    // based on the display/monitor hardware values;
    // which is the "physical" w/h divided by the w/h in pixels of the display
    ratio = (gdouble)(displayInfo->widthmm * displayInfo->height)
            / (displayInfo->heightmm * displayInfo->width);

    // DirectFB's X in 720x576 reports the physical dimensions wrong, so
    // override here
    /*
     if (displayInfo->width == 720 && displayInfo->height == 576)
     {
     ratio = 4.0 * 576 /(3.0 * 720);
     }
     */
    GST_DEBUG("calculated pixel aspect ratio: %f", ratio);

    // Now find the one from par[][2] with the lowest delta to the real one
    delta = DELTA(0);
    index = 0;

    for (i = 1; i < sizeof(par) / (sizeof(gint) * 2); ++i)
    {
        gdouble this_delta = DELTA(i);

        if (this_delta < delta)
        {
            index = i;
            delta = this_delta;
        }
    }

    GST_DEBUG("Decided on index %d(%d/%d)", index, par[index][0], par[index][1]);

    g_free(displayInfo->par);
    displayInfo->par = g_try_new0(GValue, 1);

    if (NULL == displayInfo->par)
    {
        fprintf(stderr, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
        exit(-1);
    }

    (void) g_value_init(displayInfo->par, GST_TYPE_FRACTION);
    gst_value_set_fraction(displayInfo->par, par[index][0], par[index][1]);
    displayInfo->par_n = par[index][0];
    displayInfo->par_d = par[index][1];
    GST_DEBUG("set display calculated PAR to %d/%d, par n %d, par d %d",
            gst_value_get_fraction_numerator(displayInfo->par),
            gst_value_get_fraction_denominator(displayInfo->par),
            displayInfo->par_n, displayInfo->par_d);
}

/**
 * This is a function which is called when an X Window is
 * exposed - XExposeEvent, i.e. part of window was covered and
 * becomes uncovered.  In Win32 this function is never called.
 * This will redraw the current frame in the drawable even
 * if the pipeline is PAUSED.
 *
 * @param overlay    interface which needs to process expose event.
 */
static void gst_display_expose(GstXOverlay * overlay)
{
    GST_DEBUG("function entry");
}

/**
 * This method creates navigations events.  Navigation events are sent
 * to inform upstream elements of where the mouse pointer is, if and where
 * mouse pointer clicks have happened.  All this information is contained
 * in the event structure which is created in this method.
 *
 * NOTE: This method is not currently used by this plugin.
 *
 * Check out the navigationtest element in gst-plugins-good for an idea
 * how to extract navigation information from this event.
 *
 * @param   interface   instance
 * @param   structure   information to use to generate navigation event
 */
static void gst_display_navigation_send_event(GstNavigation * navigation,
        GstStructure * structure)
{
    GST_DEBUG("function entry");
    /*
     GstDisplay *display = GST_DISPLAY(navigation);
     GstPad *peer;

     if ((peer = gst_pad_get_peer(GST_VIDEO_SINK_PAD(display))))
     {
     GstEvent *event;
     GstVideoRectangle src, dst, result;
     gdouble x, y, xscale = 1.0, yscale = 1.0;

     event = gst_event_new_navigation(structure);

     // *TODO* - this seems wrong, should be if equal to zero, we have no window
     if ((NULL == display->pUIInfo->pWindowInfo) || (0 == display->pUIInfo->pWindowInfo->win))
     {
     return;
     }

     // We get the frame position using the calculated geometry from _setcaps
     // that respect pixel aspect ratios
     src.w = GST_VIDEO_SINK_WIDTH(display);
     src.h = GST_VIDEO_SINK_HEIGHT(display);
     dst.w = display->pUIInfo->pWindowInfo->width;
     dst.h = display->pUIInfo->pWindowInfo->height;

     if (display->keep_aspect)
     {
     gst_video_sink_center_rect(src, dst, &result, TRUE);
     }
     else
     {
     result.x = result.y = 0;
     result.w = dst.w;
     result.h = dst.h;
     }

     // We calculate scaling using the original video frames geometry to include
     // pixel aspect ratio scaling.
     xscale = (gdouble) display->pUIInfo->pVideoInfo->incoming_width / result.w;
     yscale = (gdouble) display->pUIInfo->pVideoInfo->incoming_height / result.h;

     // Converting pointer coordinates to the non scaled geometry
     if (gst_structure_get_double(structure, "pointer_x", &x))
     {
     x = MIN(x, result.x + result.w);
     x = MAX(x - result.x, 0);
     gst_structure_set(structure, "pointer_x", G_TYPE_DOUBLE,
     (gdouble) x * xscale, NULL);
     }
     if (gst_structure_get_double(structure, "pointer_y", &y))
     {
     y = MIN(y, result.y + result.h);
     y = MAX(y - result.y, 0);
     gst_structure_set(structure, "pointer_y", G_TYPE_DOUBLE,
     (gdouble) y * yscale, NULL);
     }

     gst_pad_send_event(peer, event);
     gst_object_unref(peer);
     }
     */
}

/**
 *  Handles events sent to this plugin.  If parent class has
 *  handler, lets parent class handle event.
 *
 *  @param  bsink this element
 *  @param  event event to handle
 *  @return true if event should be passed on, false if not
 */
static gboolean gst_display_event(GstBaseSink* bsink, GstEvent * event)
{
    GstDisplay* display = GST_DISPLAY(bsink);

    if (GST_BASE_SINK_CLASS(parent_class)->event)
    {
        GST_INFO(
                "gst_display_event called with event type: %s, sending to parent",
                GST_EVENT_TYPE_NAME(event));

        return GST_BASE_SINK_CLASS(parent_class)->event(bsink, event);
    }
    else
    {
        GST_INFO("%s called with event type: %s, handling locally",
                __FUNCTION__, GST_EVENT_TYPE_NAME(event));

        switch (GST_EVENT_TYPE(event))
        {
        case GST_EVENT_FLUSH_START:
            GST_INFO("%s - flush start received, clearing image pool",
                    __FUNCTION__);

            gst_display_imagepool_clear(display);
            gst_display_video_clear(display);
            break;

        case GST_EVENT_NEWSEGMENT:
            GST_INFO("%s() -- setting g_newSegmentEventReceived to true\n", __FUNCTION__);
            g_newSegmentEventReceived = TRUE;
            // no break here -- fall through to default case

        default:
            GST_DEBUG("%s: not handling event %s", __FUNCTION__,
                    GST_EVENT_TYPE_NAME(event));
            break;
        }

        return TRUE;
    }
}

void gst_display_set_tune_started ()
{
    // setup the vars to detect when video size info has been refreshed
    GST_DEBUG("%s() -- function entry\n", __FUNCTION__);
    g_newSegmentEventReceived = FALSE;
    g_get_current_time(&g_tuneStartTime);
}

void gst_display_wait_for_tune_completion ()
{
    int nTimeoutSecs = 10;

    GTimeVal now = { 0, 0 };
    GTimeVal timeoutTime = g_tuneStartTime;

    GST_DEBUG("%s() -- waiting to receive new_segment event\n", __FUNCTION__);

    g_time_val_add(&timeoutTime, nTimeoutSecs * 1000000); // function expects usec

    // loop waiting for timeout OR for new segment to be received
    while (TRUE)
    {
        if (g_newSegmentEventReceived)
        {
            GST_DEBUG("%s() -- new_segment event received -- exiting \n", __FUNCTION__);
            return;
        }

        g_get_current_time(&now);
        if (now.tv_sec > timeoutTime.tv_sec || 
            ((now.tv_sec == timeoutTime.tv_sec) && (now.tv_usec >= timeoutTime.tv_usec)))
        {
            break;
        }

        g_usleep(200000L); // sleep 0.2 seconds before trying again
    }

    GST_DEBUG("%s() -- timed-out  waiting to receive new_segment event \n", __FUNCTION__);
}
