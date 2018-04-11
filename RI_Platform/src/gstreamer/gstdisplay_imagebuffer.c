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

#include <stdio.h>
#include "gstdisplay.h"

/**************************************************************************
 * The code in this module was pulled out of the main plugin code module
 * primarily as a convenience.  That is, to make the main code module
 * smaller, and easier to manage.
 * Only minor changes have been made to these functions.
 *************************************************************************/

/* image buffers */

/**
 * Performs the actual destruction of this object by freeing associated
 * memory and setting data member pointers to null.
 *
 * @param   image    freed memory associated with this object and null pointers
 */
static void gst_display_image_buffer_destroy(GstDisplayImageBuffer* image)
{
    GstDisplay *display;

    GST_DEBUG("function entry");

    display = image->display;
    if (G_UNLIKELY(display == NULL))
    {
        GST_WARNING("no sink found");
        return;
    }

    g_return_if_fail(GST_IS_DISPLAY(display));

    GST_OBJECT_LOCK(display);

    // Don't need to acquire flow lock here because calling methods have
    // acquired it already

    /* If the destroyed image is the current one we destroy our reference too */
    // Using the "object lock" big hammer approach here, so don't get the flow lock
    if (display->display_image == image)
    {
        // Set the pointer to null since about to destroy this buffer
        display->display_image = NULL;
    }

    /* We might have some buffers destroyed after changing state to NULL */
    if (display->pUIInfo->pDisplayInfo == NULL)
    {
        GST_DEBUG_OBJECT(display, "Destroying Image after Xcontext");

        goto beach;
    }

    if (NULL != image->pImageBuffer->data)
    {
        g_free(image->pImageBuffer->data);
        image->pImageBuffer->data = NULL;
        GST_BUFFER_DATA(image) = NULL;
    }

    // Get rid of image buffer
    if (NULL != image->pImageBuffer)
    {
        g_free(image->pImageBuffer);
        image->pImageBuffer = NULL;
    }

    beach: GST_OBJECT_UNLOCK(display);
    image->display = NULL;
    gst_object_unref(display);
}

/**
 * Method called by gstreamer framework when the ref count of the image reaches 0
 * and it about to be freed.  Since these images are stored in the pool, their ref
 * count may reach zero but want to keep them around so they are available if
 * one is needed.  They are destroyed if no longer running
 * or if the size of video has changed.  If neither of these are true,
 * the ref cnt is incremented and the image is put back into image pool.
 *
 * @param   image    object whose ref count has reached 0 and is about to be
 *                   freed by gstreamer framework
 */
static void gst_display_image_buffer_finalize(GstDisplayImageBuffer* image)
{
    GST_DEBUG("function entry");
    GstDisplay *display;
    gboolean running;

    display = image->display;
    if (G_UNLIKELY(display == NULL))
    {
        GST_WARNING("no sink found");
        return;
    }

    g_return_if_fail(GST_IS_DISPLAY(display));

    //g_print("image buffer finialize called with buffer %dx%d vs current video size %dx%d",
    //        image->buffer_width, image->buffer_height,
    //        display->pVideoInfo->incoming_width, display->pVideoInfo->incoming_height);

    GST_OBJECT_LOCK(display);
    running = display->running;
    GST_OBJECT_UNLOCK(display);

    // Don't acquire the flow lock here since destroying image buffer
    // and it should not be referenced anywhere, causes problems when
    // lock is acquired

    /* If our geometry changed we can't reuse that image. */
    if (running == FALSE)
    {
        GST_DEBUG_OBJECT(image, "destroy image as sink is shutting down");
        gst_display_image_buffer_destroy(image);
    }
    else if ((image->pImageBuffer->buffer_width
            != display->pUIInfo->pVideoInfo->incoming_width)
            || (image->pImageBuffer->buffer_height
                    != display->pUIInfo->pVideoInfo->incoming_height))
    {
        //g_print("destroy image buffer as its size changed %dx%d vs current %dx%d",
        //        image->buffer_width, image->buffer_height,
        //        display->pVideoInfo->incoming_width, display->pVideoInfo->incoming_height);
        gst_display_image_buffer_destroy(image);
    }
    else
    {
        /* In that case we can reuse the image and add it to our image pool. */
        //GST_DEBUG_OBJECT(image, "recycling image in pool");
        /* need to increment the refcount again to recycle */
        (void) gst_buffer_ref(GST_BUFFER(image));
        g_mutex_lock(display->pool_lock);
        display->image_pool = g_slist_prepend(display->image_pool, image);
        g_mutex_unlock(display->pool_lock);
    }
}

/**
 * Prepares buffer for destruction by settings the width and height to invalid
 * value so they are not re-used in the image pool.  Also decrements ref count.
 * This image buffer will be destroyed when finalize() is called since the width
 * and height are invalid.
 *
 * @param   image buffer to mark for deletion
 */
void gst_display_image_buffer_free(GstDisplayImageBuffer* image)
{
    GST_DEBUG("function entry");

    // Don't need to acquire the flow lock because image is passed in
    /* make sure it is not recycled */
    image->pImageBuffer->buffer_width = -1;
    image->pImageBuffer->buffer_height = -1;

    gst_buffer_unref(GST_BUFFER(image));
}

static void gst_display_image_buffer_init(GstDisplayImageBuffer* image,
        gpointer g_class)
{
    GST_DEBUG("function entry");
}

static void gst_display_image_buffer_class_init(gpointer g_class,
        gpointer class_data)
{
    GstMiniObjectClass *mini_object_class = GST_MINI_OBJECT_CLASS(g_class);

    mini_object_class->finalize
            = (GstMiniObjectFinalizeFunction) gst_display_image_buffer_finalize;
}

GType gst_display_image_buffer_get_type(void)
{
    GST_DEBUG("function entry");
    static GType _gst_display_image_buffer_type;

    if (G_UNLIKELY(_gst_display_image_buffer_type == 0))
    {
        static const GTypeInfo display_image_buffer_info =
        { sizeof(GstBufferClass), NULL, NULL,
                gst_display_image_buffer_class_init, NULL, NULL,
                sizeof(GstDisplayImageBuffer), 0,
                (GInstanceInitFunc) gst_display_image_buffer_init, NULL };
        _gst_display_image_buffer_type = g_type_register_static(
                GST_TYPE_BUFFER, "GstDisplayImageBuffer",
                &display_image_buffer_info, 0);
    }
    return _gst_display_image_buffer_type;
}

/* This function handles GstXvImage creation */
GstDisplayImageBuffer *
gst_display_image_buffer_new(GstDisplay* display, GstCaps * caps, guint size)
{
    GST_DEBUG("function entry");
    //g_print("gst_display_image_buffer_new called\n");
    GstDisplayImageBuffer *image = NULL;
    GstStructure *structure = NULL;
    gboolean succeeded = FALSE;
    guint32 fourcc;

    g_return_val_if_fail(GST_IS_DISPLAY(display), NULL);

    image = (GstDisplayImageBuffer *) gst_mini_object_new(
            GST_TYPE_DISPLAY_IMAGE_BUFFER);
    GST_DEBUG_OBJECT(image, "Creating new GstDisplayImageBuffer");

    // Allocate the structure in the buffer to store info
    image->pImageBuffer = g_try_new0(ImageBuffer, 1);

    if (NULL == image->pImageBuffer)
    {
        fprintf(stderr, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }
    else
    {
        structure = gst_caps_get_structure(caps, 0);

        // set the GstDisplayImageBuffer height and width from the caps structure
        if (!gst_structure_get_int(structure, "width",
                &image->pImageBuffer->buffer_width) || !gst_structure_get_int(
                structure, "height", &image->pImageBuffer->buffer_height))
        {
            GST_WARNING("failed getting geometry from caps %" GST_PTR_FORMAT, caps);
        }

        // get the fourcc format
        if ((TRUE == gst_structure_get_fourcc(structure, "format", &fourcc)) &&
                (GST_MAKE_FOURCC('I', '4', '2', '0') == fourcc))
        {
            image->pImageBuffer->isI420YUV = TRUE;
        }
        else
        {
            image->pImageBuffer->isI420YUV = FALSE;
        }

        image->display = gst_object_ref(display);

        succeeded = TRUE;

        // allocate memory for the image
        image->pImageBuffer->size = size;
        image->pImageBuffer->data = g_try_malloc(image->pImageBuffer->size);

        if (NULL == image->pImageBuffer->data)
        {
            fprintf(stderr,
                    "line %d of %s, %s memory allocation of %d failure!\n",
                    __LINE__, __FILE__, __func__, size);
            succeeded = FALSE;
        }

        GST_BUFFER_DATA(image) = image->pImageBuffer->data;
        GST_BUFFER_SIZE(image) = image->pImageBuffer->size;

        //g_print("gst_display_image_buffer_new created image buffer width = %d, height = %d, size = %d\n",
        //     image->pImageBuffer->buffer_width, image->pImageBuffer->buffer_height, image->pImageBuffer->size);
    }

//beach_unlocked:
if (!succeeded)
{
    gst_display_image_buffer_free(image);
    image = NULL;
}

return image;
}
