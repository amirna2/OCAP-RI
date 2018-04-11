/* GStreamer
 * Copyright (C) <2005> Julien Moutte <julien@moutte.net>
 * Copyright (C) <2009> Cable Television Laboratories, Inc. 
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

#ifndef __GST_DISPLAY_H__
#define __GST_DISPLAY_H__

#include <gst/video/gstvideosink.h>
#include <gst/interfaces/xoverlay.h>
#include "ui_info.h"

G_BEGIN_DECLS

// Standard GStreamer Plugin Macros
//
#define GST_TYPE_DISPLAY \
  (gst_display_get_type())

#define GST_DISPLAY(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), GST_TYPE_DISPLAY, GstDisplay))

#define GST_DISPLAY_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass), GST_TYPE_DISPLAY, GstDisplayClass))

#define GST_IS_DISPLAY(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj), GST_TYPE_DISPLAY))

#define GST_IS_DISPLAY_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), GST_TYPE_DISPLAY))

// Types used by GstDisplay Plugin
//
typedef struct _GstDisplay GstDisplay;

typedef struct _GstDisplayImageBuffer GstDisplayImageBuffer;

// GstDisplayImageBuffer Plugin Macros
//
#define GST_TYPE_DISPLAY_IMAGE_BUFFER (gst_display_image_buffer_get_type())
#define GST_IS_DISPLAY_IMAGE_BUFFER(obj) (G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_DISPLAY_IMAGE_BUFFER))
#define GST_DISPLAY_IMAGE_BUFFER(obj) (G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_DISPLAY_IMAGE_BUFFER, GstDisplayImageBuffer))

/**
 * GstDisplayImageBuffer:
 *
 * @buffer: base buffer
 * @display: a reference to our GstDisplay plugin
 * @data: pointer to the data for this image
 * @isI420YUV: is image YUV or RGB
 * @width: the width in pixels of the image
 * @height: the height in pixels of the image
 * @size: the size in bytes of image
 *
 * Subclass of #GstBuffer containing additional information about an Image.
 */
struct _GstDisplayImageBuffer
{
    GstBuffer buffer;

    // The display instance we belong to
    GstDisplay *display;

    ImageBuffer* pImageBuffer;
};

// Image Buffer function declarations
//
GType gst_display_image_buffer_get_type(void);
void gst_display_image_buffer_free(GstDisplayImageBuffer * climage);
GstDisplayImageBuffer* gst_display_image_buffer_new(GstDisplay * display,
        GstCaps * caps, guint size);

// When a service selection is completed, video size info will be
// refreshed.  During the time when a service selection has
// started but not yet completed, attempts to retrieve the 
// video size info will block.  The following functions are used
// for this.
void gst_display_set_tune_started (void);
void gst_display_wait_for_tune_completion (void);


/**
 * GstDisplay:
 *
 * @videosink: base element
 * @pDisplayInfo: pointer to information about display
 * @pScreenInfo: pointer to os specific window information
 * @image: internal #Image used to store incoming buffers and render when
 * not using the buffer_alloc optimization mechanism
 * @cur_image: a reference to the last #Image that was put to @window. It
 * is used when Expose events are received to redraw the latest video frame
 * @screen_width: width of screen/window in pixels
 * @screen_height: height of screen/window in pixels
 * @event_thread: a thread listening for events on @xwindow and handling them
 * @running: used to inform @event_thread if it should run/shutdown
 * @fps_n: the framerate fraction numerator
 * @fps_d: the framerate fraction denominator
 * @x_lock: used to protect X calls as we are not using the XLib in threaded
 * mode
 * @flow_lock: used to protect data flow routines from external calls such as
 * events from @event_thread or methods from the #GstXOverlay interface
 * @par: used to override calculated pixel aspect ratio from @xcontext
 * @pool_lock: used to protect the buffer pool
 * @image_pool: a list of #GstXvImageBuffer that could be reused at next buffer
 * allocation call
 * @synchronous: used to store if XSynchronous should be used or not (for
 * debugging purpose only)
 * @keep_aspect: used to remember if reverse negotiation scaling should respect
 * aspect ratio
 * @handle_events: used to know if we should handle select XEvents or not
 * @video_width: the width of incoming video frames in pixels
 * @video_height: the height of incoming video frames in pixels
 * @video_scale_x: if scaling video, x position for scaled video
 * @video_scale_y: if scaling video, y position for scaled video
 * @video_scale_width: if scaling video, width to scale video to
 * @video_scale_height: if scaling video, height to scale video to
 * @video_aspect_ratio_width_factor: factor used for scaling video
 * @video_aspect_ratio_height_factor: factor used for scaling video
 * @background_color: background color of screen/window
 * @graphics_width: width of graphics output
 * @graphics_height: height of graphics output
 * @graphics_width_scale_factor: factor used to scale graphics
 * @graphics_height_scale_factor: factor used to scale graphics
 *
 * The GstDisplay Plugin structure.
 */
struct _GstDisplay
{
    // Base element
    GstVideoSink videosink;

    // UI Information
    UIInfo* pUIInfo;

    // Requests that the display maintain the requested aspect ratio
    // for graphics, video and background
    gboolean keep_aspect;

    // Pool of image bufferss for containing video frames for display
    GMutex *pool_lock;
    GSList *image_pool;

    // Image buffer objects
    GstDisplayImageBuffer *fill_image;
    GstDisplayImageBuffer *display_image;

    // Frames per sec numerator & denominator
    gint fps_n;
    gint fps_d;

    // Flag which indicates plugin has entered the ready state
    gboolean running;

    // Requests that the display element only support RBG video format
    // so testing of RGB video can be tested
    gboolean force_rgb_video;

    // Capabilities of Display Plugin
    GstCaps *caps;

    // Copy of capabiities
    GstCaps *last_caps;
};

// GStreamer Plugin Parent Class
//
struct _GstDisplayClass
{
    GstVideoSinkClass parent_class;
};

// GStreamer Plugin method declaration
//
GType gst_display_get_type(void);
void gst_display_update_configuration(GstDisplay* display, guint videoWidth,
        guint videoHeight, guint videoPARx, guint videoPARy);
void gst_display_set_window_id(GstXOverlay* overlay, gulong window_id);
void gst_display_video_block(GstDisplay* display, gboolean block);
void gst_display_video_freeze(GstDisplay* display);
void gst_display_video_resume(GstDisplay* display);
void gst_display_video_aspect_ratio_adjust(GstDisplay* display);
void gst_display_reset(GstDisplay* display);

G_END_DECLS

#endif /* __GST_DISPLAY_H__ */
