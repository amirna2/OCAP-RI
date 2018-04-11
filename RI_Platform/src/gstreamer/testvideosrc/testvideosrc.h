/* GStreamer
 * Copyright (C) <2003> David A. Schleef <ds@schleef.org>
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

#ifndef __TEST_VIDEO_SRC_H__
#define __TEST_VIDEO_SRC_H__

#include <glib.h>

enum
{
    VTS_YUV, VTS_RGB, VTS_BAYER
};

struct vts_color_struct
{
    guint8 Y, U, V;
    guint8 R, G, B;
    guint8 A;
};

typedef struct paintinfo_struct paintinfo;
struct paintinfo_struct
{
    unsigned char *dest; /* pointer to first byte of video data */
    unsigned char *yp, *up, *vp; /* pointers to first byte of each component
     * for both packed/planar YUV and RGB */
    unsigned char *ap; /* pointer to first byte of alpha component */
    unsigned char *endptr; /* pointer to byte beyond last video data */
    int ystride;
    int ustride;
    int vstride;
    int width;
    int height;
    const struct vts_color_struct *color;
    void (*paint_hline)(paintinfo * p, int x, int y, int w);
};

struct fourcc_list_struct
{
    int type;
    char *fourcc;
    char *name;
    int bitspp;
    void (*paint_setup)(paintinfo * p, unsigned char *dest);
    void (*paint_hline)(paintinfo * p, int x, int y, int w);
    int depth;
    unsigned int red_mask;
    unsigned int green_mask;
    unsigned int blue_mask;
    unsigned int alpha_mask;
};

struct fourcc_list_struct *
paintrect_find_fourcc(int find_fourcc);
struct fourcc_list_struct *
paintrect_find_name(const char *name);
struct fourcc_list_struct *
paintinfo_find_by_structure(const GstStructure *structure);
GstStructure *
paint_get_structure(struct fourcc_list_struct *format);
int gst_test_video_src_get_size(GstTestVideoSrc * v, int w, int h);
void gst_test_video_src_smpte(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_snow(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_black(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_white(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_red(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_green(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_blue(GstTestVideoSrc * v, unsigned char *dest, int w,
        int h);
void gst_test_video_src_checkers1(GstTestVideoSrc * v, unsigned char *dest,
        int w, int h);
void gst_test_video_src_checkers2(GstTestVideoSrc * v, unsigned char *dest,
        int w, int h);
void gst_test_video_src_checkers4(GstTestVideoSrc * v, unsigned char *dest,
        int w, int h);
void gst_test_video_src_checkers8(GstTestVideoSrc * v, unsigned char *dest,
        int w, int h);
void gst_test_video_src_circular(GstTestVideoSrc * v, unsigned char *dest,
        int w, int h);

extern struct fourcc_list_struct fourcc_list[];
extern int n_fourccs;

#endif
