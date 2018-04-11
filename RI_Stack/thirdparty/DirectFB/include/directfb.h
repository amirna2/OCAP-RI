/*
   (c) Copyright 2000-2002  convergence integrated media GmbH.
   (c) Copyright 2002       convergence GmbH.
   
   All rights reserved.

   Written by Denis Oliver Kropp <dok@directfb.org>,
              Andreas Hundt <andi@fischlustig.de> and
              Sven Neumann <sven@convergence.de>.

   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2 of the License, or (at your option) any later version.

   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the
   Free Software Foundation, Inc., 59 Temple Place - Suite 330,
   Boston, MA 02111-1307, USA.
*/

#include <config.h>

#ifndef __DIRECTFB_H__
#define __DIRECTFB_H__

#include <external.h>
#include <dfb_types.h>

#if defined(MPE_BIG_ENDIAN) && defined(MPE_LITTLE_ENDIAN)
# error MPE_BIG_ENDIAN and MPE_LITTLE_ENDIAN must not both be defined
#endif
#if !defined(MPE_BIG_ENDIAN) && !defined(MPE_LITTLE_ENDIAN)
# error MPE_BIG_ENDIAN or MPE_LITTLE_ENDIAN must be defined
#endif

#ifdef __cplusplus
extern "C"
{
#endif

/*
 * Forward declaration macro for interfaces.
 */
#ifdef interface
#undef interface
#endif
#ifdef DECLARE_INTERFACE
#undef DECLARE_INTERFACE
#endif

#define DECLARE_INTERFACE( IFACE )                \
     struct _##IFACE;                             \
     typedef struct _##IFACE IFACE;

/*
 * Macro for an interface definition.
 */
#define DEFINE_INTERFACE( IFACE )                 \
     struct _##IFACE     {                        \
          void       *priv;                       \
          DFBResult (*AddRef)( IFACE *thiz );     \
          DFBResult (*Release)( IFACE *thiz );
#define DEFINE_INTERFACE_END };


/*
 * Version handling.
 */
extern const unsigned int directfb_major_version;
extern const unsigned int directfb_minor_version;
extern const unsigned int directfb_micro_version;
extern const unsigned int directfb_binary_age;
extern const unsigned int directfb_interface_age;

/*
 * Check for a certain DirectFB version.
 * In case of an error a message is returned describing the mismatch.
 */
const char * DirectFBCheckVersion( unsigned int required_major,
                                   unsigned int required_minor,
                                   unsigned int required_micro );


/*
 * The only interface with a global "Create" function,
 * any other functionality goes from here.
 */
DECLARE_INTERFACE( IDirectFB )

/*
 * Layer configuration, creation of windows and background
 * configuration.
 */
DECLARE_INTERFACE( IDirectFBDisplayLayer )

/*
 * Surface locking, setting colorkeys and other drawing
 * parameters, clipping, flipping, blitting, drawing.
 */
DECLARE_INTERFACE( IDirectFBSurface )

/*
 * Access to palette data. Set/get entries, rotate palette.
 */
DECLARE_INTERFACE( IDirectFBPalette )

/*
 * Getting font metrics and pixel width of a string.
 */
DECLARE_INTERFACE( IDirectFBFont )

#undef DECLARE_INTERFACE

/*
 * Every interface method returns this result code.<br>
 * Any other value to be returned adds an argument pointing
 * to a location the value should be written to.
 */
typedef enum {
     DFB_OK,             /* No error occured. */
     DFB_FAILURE,        /* A general or unknown error occured. */
     DFB_INIT,           /* A general initialization error occured. */
     DFB_BUG,            /* Internal bug or inconsistency has been detected. */
     DFB_DEAD,           /* Interface has a zero reference counter
                            (after Release, only available in debug mode). */
     DFB_UNSUPPORTED,    /* The requested operation or an argument
                            is not supported by hardware or software. */
     DFB_UNIMPLEMENTED,  /* The requested operation is not yet implemented. */
     DFB_ACCESSDENIED,   /* Access to the resource is denied. */
     DFB_INVARG,         /* An invalid argument has been specified. */
     DFB_NOSYSTEMMEMORY, /* There's not enough system memory. */
     DFB_NOVIDEOMEMORY,  /* There's not enough video memory. */
     DFB_LOCKED,         /* The resource is (already) locked. */
     DFB_BUFFEREMPTY,    /* The buffer is empty. */
     DFB_FILENOTFOUND,   /* The specified file has not been found. */
     DFB_IO,             /* A general I/O error occured. */
     DFB_BUSY,           /* The resource or device is busy. */
     DFB_NOIMPL,         /* No implementation for the requested interface or
                            specified data has been found. */
     DFB_MISSINGFONT,    /* No font has been set. */
     DFB_TIMEOUT,        /* The operation timed out. */
     DFB_MISSINGIMAGE,   /* No image has been set. */
     DFB_THIZNULL,       /* 'thiz' pointer is NULL. */
     DFB_IDNOTFOUND,     /* No resource has been found by the specified id. */
     DFB_INVAREA,        /* An invalid area has been specified or detected. */
     DFB_DESTROYED,      /* The underlying object (e.g. a window or surface)
                            has been destroyed. */
     DFB_FUSION          /* Internal fusion error detected, most likely
                            related to IPC resources. */
} DFBResult;

/*
 * A boolean.
 */
typedef enum {
     DFB_FALSE = 0,
     DFB_TRUE  = !DFB_FALSE
} DFBBoolean;

/*
 * A point specified by x/y coordinates.
 */
typedef struct {
     int            x;   /* X coordinate of it */
     int            y;   /* Y coordinate of it */
} DFBPoint;

/*
 * A dimension specified by width and height.
 */
typedef struct {
     int            w;   /* width of it */
     int            h;   /* height of it */
} DFBDimension;

/*
 * A rectangle specified by a point and a dimension.
 */
typedef struct {
     int            x;   /* X coordinate of its top-left point */
     int            y;   /* Y coordinate of its top-left point */
     int            w;   /* width of it */
     int            h;   /* height of it */
} DFBRectangle;

/*
 * A region specified by two points.
 *
 * The defined region includes both endpoints.
 */
typedef struct {
     int            x1;  /* X coordinate of top-left point */
     int            y1;  /* Y coordinate of top-left point */
     int            x2;  /* X coordinate of lower-right point */
     int            y2;  /* Y coordinate of lower-right point */
} DFBRegion;

/*
 * A color defined by channels with 8bit each.
 */
typedef struct {
     __u8           a;   /* alpha channel */
     __u8           r;   /* red channel */
     __u8           g;   /* green channel */
     __u8           b;   /* blue channel */
} DFBColor;

#define DFB_COLOR_EQUAL(x,y)  ((x).a == (y).a &&  \
                               (x).r == (y).r &&  \
                               (x).g == (y).g &&  \
                               (x).b == (y).b)

/*
 * Print a description of the result code along with an
 * optional message that is put in front with a colon.
 */
DFBResult DirectFBError(
                             const char  *msg,    /* optional message */
                             DFBResult    result  /* result code to interpret */
                       );

/*
 * Behaves like DirectFBError, but shuts down the calling application.
 */
DFBResult DirectFBErrorFatal(
                             const char  *msg,    /* optional message */
                             DFBResult    result  /* result code to interpret */
                            );

/*
 * Returns a string describing 'result'.
 */
const char *DirectFBErrorString(
                         DFBResult    result
                      );

/*
 * Retrieves information about supported command-line flags in the
 * form of a user-readable string formatted suitable to be printed
 * as usage information.
 */
const char *DirectFBUsageString( void );

/*
 * Parses the command-line and initializes some variables. You
 * absolutely need to call this before doing anything else.
 * Removes all options used by DirectFB from argv.
 */
LIBEXPORT DFBResult DirectFBInit(
                         int                *argc,    /* pointer to main()'s argc */
                         char               **argv[], /* pointer to main()'s argv */
                         DirectFB2External  *ext      /* pointer to external support routines */
                      );

/*
 * Sets configuration parameters supported on command line and in
 * config file. Can only be called before DirectFBCreate but after
 * DirectFBInit.
 */
DFBResult DirectFBSetOption(
                         const char  *name,
                         const char  *value
                      );

/*
 * Creates the super interface.
 */
LIBEXPORT DFBResult DirectFBCreate(
                          IDirectFB **interfac  /* pointer to the
                                                    created interface */
                        );

/*
 * The cooperative level controls the super interface's behaviour
 * in functions like SetVideoMode or CreateSurface for the primary.
 */
typedef enum {
     DFSCL_FULLSCREEN    = 0x00000001
} DFBCooperativeLevel;

/*
 * Capabilities of a display layer.
 */
typedef enum {
     DLCAPS_SURFACE           = 0x00000001,  /* The layer has a surface that
                                                can be drawn to. This may not
                                                be provided by layers that
                                                display realtime data, e.g.
                                                from an MPEG decoder chip.
                                                Playback control may be
                                                provided by an external API. */
} DFBDisplayLayerCapabilities;

/*
 * Used to enable some capabilities like flicker filtering or colorkeying.
 */
typedef enum {
     DLOP_NONE                = 0x00000000,  /* None of these. */
} DFBDisplayLayerOptions;

/*
 * Layer Buffer Mode.
 */
typedef enum {
     DLBM_FRONTONLY  = 0x00000000,      /* no backbuffer */
     DLBM_BACKVIDEO  = 0x00000001,      /* backbuffer in video memory */
     DLBM_BACKSYSTEM = 0x00000002       /* backbuffer in system memory */
} DFBDisplayLayerBufferMode;

/*
 * Flags defining which fields of a DFBSurfaceDescription are valid.
 */
typedef enum {
     DSDESC_CAPS         = 0x00000001,  /* caps field is valid */
     DSDESC_WIDTH        = 0x00000002,  /* width field is valid */
     DSDESC_HEIGHT       = 0x00000004,  /* height field is valid */
     DSDESC_PIXELFORMAT  = 0x00000008,  /* pixelformat field is valid */
     DSDESC_PREALLOCATED = 0x00000010,  /* Surface uses data that has been
                                           preallocated by the application.
                                           The field array 'preallocated'
                                           has to be set using the first
                                           element for the front buffer
                                           and eventually the second one
                                           for the back buffer. */
     DSDESC_PALETTE      = 0x00000020   /* Initialize the surfaces palette
                                           with the entries specified in the
                                           description. */
} DFBSurfaceDescriptionFlags;

/*
 * Flags defining which fields of a DFBPaletteDescription are valid.
 */
typedef enum {
     DPDESC_CAPS         = 0x00000001,  /* Specify palette capabilities. */
     DPDESC_SIZE         = 0x00000002,  /* Specify number of entries. */
     DPDESC_ENTRIES      = 0x00000004   /* Initialize the palette with the
                                           entries specified in the
                                           description. */
} DFBPaletteDescriptionFlags;

/*
 * The surface capabilities.
 */
typedef enum {
     DSCAPS_NONE         = 0x00000000,  /* None of these. */

     DSCAPS_PRIMARY      = 0x00000001,  /* It's the primary surface. */
     DSCAPS_SYSTEMONLY   = 0x00000002,  /* Surface data is permanently stored
                                           in system memory. <br>There's no
                                           video memory allocation/storage. */
     DSCAPS_VIDEOONLY    = 0x00000004,  /* Surface data is permanently stored
                                           in video memory. <br>There's no
                                           system memory allocation/storage. */
     DSCAPS_FLIPPING     = 0x00000010,  /* Surface is double buffered or needs
                                           Flip() calls to make updates/changes
                                           visible/usable. */
     DSCAPS_SUBSURFACE   = 0x00000020,  /* Surface is just a sub area of
                                           another one sharing the surface
                                           data. */
     DSCAPS_INTERLACED   = 0x00000040,  /* Each buffer contains interlaced
                                           video (or graphics) data consisting
                                           of two fields. <br>Their lines are
                                           stored interleaved. One field's
                                           height is a half of the surface's
                                           height. */
     DSCAPS_SEPARATED    = 0x00000080,  /* For usage with DSCAPS_INTERLACED.
                                           <br> DSCAPS_SEPARATED specifies that
                                           the fields are NOT interleaved line
                                           by line in the buffer. <br>The first
                                           field is followed by the second one
                                           in the buffer. */
     DSCAPS_STATIC_ALLOC = 0x00000100   /* The amount of video or system memory
                                           allocated for the surface is never
                                           less than its initial value. This
                                           way a surface can be resized
                                           (smaller and bigger up to the
                                           initial size) without reallocation
                                           of the buffers. It's useful for
                                           surfaces that need a guaranteed
                                           space in video memory after
                                           resizing. */
} DFBSurfaceCapabilities;

/*
 * The palette capabilities.
 */
typedef enum {
     DPCAPS_NONE         = 0x00000000   /* None of these. */
} DFBPaletteCapabilities;

/*
 * Flags controlling drawing commands.
 */
typedef enum {
     DSDRAW_NOFX               = 0x00000000, /* uses none of the effects */
     DSDRAW_BLEND              = 0x00000001, /* uses alpha from color */
     DSDRAW_DST_COLORKEY       = 0x00000002, /* write to destination only
                                                if the destination pixel
                                                matches the destination
                                                color key (not fully
                                                implemented yet) */
     DSDRAW_SRC_PREMULTIPLY    = 0x00000004, /* multiplies the color's
                                                rgb channels by the alpha
                                                channel before drawing */
     DSDRAW_DST_PREMULTIPLY    = 0x00000008, /* modulates the dest. color
                                                with the dest. alpha */
     DSDRAW_DEMULTIPLY         = 0x00000010, /* divides the color by the
                                                alpha before writing the
                                                data to the destination */
     DSDRAW_XOR                = 0x00000020  /* bitwise xor the destination
                                                pixels with the specified color
                                                after premultiplication */
} DFBSurfaceDrawingFlags;

/*
 * Flags controlling blitting commands.
 */
typedef enum {
     DSBLIT_NOFX               = 0x00000000, /* uses none of the effects */
     DSBLIT_BLEND_ALPHACHANNEL = 0x00000001, /* enables blending and uses
                                                alphachannel from source */
     DSBLIT_BLEND_COLORALPHA   = 0x00000002, /* enables blending and uses
                                                alpha value from color */
     DSBLIT_COLORIZE           = 0x00000004, /* modulates source color with
                                                the color's r/g/b values */
     DSBLIT_SRC_COLORKEY       = 0x00000008, /* don't blit pixels matching
                                                the source color key */
     DSBLIT_DST_COLORKEY       = 0x00000010, /* write to destination only
                                                if the destination pixel
                                                matches the destination
                                                color key (not fully
                                                implemented yet) */
     DSBLIT_SRC_PREMULTIPLY    = 0x00000020, /* modulates the source color
                                                with the (modulated) source
                                                alpha */
     DSBLIT_DST_PREMULTIPLY    = 0x00000040, /* modulates the dest. color
                                                with the dest. alpha */
     DSBLIT_DEMULTIPLY         = 0x00000080, /* divides the color by the
                                                alpha before writing the
                                                data to the destination */
     DSBLIT_DEINTERLACE        = 0x00000100,  /* deinterlaces the source during
                                                blitting by reading only one
                                                field (every second line of full
                                                image) scaling it vertically */

	 DSBLIT_XOR				   = 0x00000200  /* bitwise xor the destination
                                                pixels with the source pixels
                                                after premultiplication */

} DFBSurfaceBlittingFlags;

/*
 * Mask of accelerated functions.
 */
typedef enum {
     DFXL_NONE           = 0x00000000,  /* None of these. */
     DFXL_FILLRECTANGLE  = 0x00000001,  /* FillRectangle() is accelerated. */
     DFXL_DRAWRECTANGLE  = 0x00000002,  /* DrawRectangle() is accelerated. */
     DFXL_DRAWLINE       = 0x00000004,  /* DrawLine() is accelerated. */
     DFXL_FILLOVAL       = 0x00000100,  /* FillOval() is accelerated. */
     DFXL_DRAWOVAL       = 0x00000200,  /* DrawOval() is accelerated. */
     DFXL_FILLARC        = 0x00000400,  /* FillArc() is accelerated. */
     DFXL_DRAWARC        = 0x00000800,  /* DrawArc() is accelerated. */
     DFXL_FILLPOLYGON    = 0x00001000,  /* FillPolygon() is accelerated. */
     DFXL_FILLROUNDRECT  = 0x00002000,  /* FillRoundRect() is accelerated. */
     DFXL_DRAWROUNDRECT  = 0x00004000,  /* DrawRoundRect() is accelerated. */

     DFXL_BLIT           = 0x00010000,  /* Blit() is accelerated. */
     DFXL_STRETCHBLIT    = 0x00020000,  /* StretchBlit() is accelerated. */
     DFXL_DRAWSTRING     = 0x00040000,  /* DrawString() is accelerated. */

                           /* All drawing/blitting functions. */

     DFXL_ALL            = DFXL_FILLRECTANGLE | \
                           DFXL_DRAWRECTANGLE | \
                           DFXL_DRAWLINE | \
                           DFXL_FILLOVAL | \
                           DFXL_DRAWOVAL | \
                           DFXL_FILLARC | \
                           DFXL_DRAWARC | \
                           DFXL_FILLPOLYGON | \
                           DFXL_FILLROUNDRECT | \
                           DFXL_DRAWROUNDRECT | \
                           DFXL_BLIT | \
                           DFXL_STRETCHBLIT | \
                           DFXL_DRAWSTRING
} DFBAccelerationMask;

#define DFB_DRAWING_FUNCTION(a)    ((a) & 0x0000FFFF)
#define DFB_BLITTING_FUNCTION(a)   ((a) & 0xFFFF0000)

/*
 * Rough information about hardware capabilities.
 */
typedef struct {
     DFBAccelerationMask     acceleration_mask;   /* drawing/blitting
                                                     functions */
     DFBSurfaceDrawingFlags  drawing_flags;       /* drawing flags */
     DFBSurfaceBlittingFlags blitting_flags;      /* blitting flags */
     unsigned int            video_memory;        /* amount of video
                                                     memory in bytes */
} DFBCardCapabilities;

/*
 * Type of display layer for basic classification.
 * Values may be or'ed together.
 */
typedef enum {
     DLTF_NONE           = 0x00000000,  /* Unclassified, no specific type. */

     DLTF_GRAPHICS       = 0x00000001,  /* Can be used for graphics output. */
} DFBDisplayLayerTypeFlags;

/*
 * Flags describing how to load a font.
 *
 * These flags describe how a font is loaded and affect how the
 * glyphs are drawn. There is no way to change this after the font
 * has been loaded. If you need to render a font with different
 * attributes, you have to create multiple FontProviders of the
 * same font file.
 */
typedef enum {
     DFFA_NONE           = 0x00000000,  /* none of these flags */
     DFFA_NOKERNING      = 0x00000001,  /* don't use kerning */
     DFFA_NOHINTING      = 0x00000002,  /* don't use hinting */
     DFFA_MONOCHROME     = 0x00000004,  /* don't use anti-aliasing */
     DFFA_NOCHARMAP      = 0x00000008   /* no char map, glyph indices are
                                           specified directly */
} DFBFontAttributes;

/*
 * Flags defining which fields of a DFBFontDescription are valid.
 */
typedef enum {
     DFDESC_ATTRIBUTES   = 0x00000001,  /* attributes field is valid */
     DFDESC_HEIGHT       = 0x00000002,  /* height is specified */
     DFDESC_WIDTH        = 0x00000004,  /* width is specified */
     DFDESC_INDEX        = 0x00000008,  /* index is specified */
     DFDESC_FIXEDADVANCE = 0x00000010   /* specify a fixed advance overriding
                                           any character advance of fixed or
                                           proportional fonts */
} DFBFontDescriptionFlags;

/*
 * Description of how to load glyphs from a font file.
 *
 * The attributes control how the glyphs are rendered. Width and
 * height can be used to specify the desired face size in pixels.
 * If you are loading a non-scalable font, you shouldn't specify
 * a font size. Please note that the height value in the
 * FontDescription doesn't correspond to the height returned by
 * the font's GetHeight() method.
 * 
 * The index field controls which face is loaded from a font file
 * that provides a collection of faces. This is rarely needed.
 */
typedef struct {
     DFBFontDescriptionFlags            flags;

     DFBFontAttributes                  attributes;
     unsigned int                       height;
     unsigned int                       width;
     unsigned int                       index;
     unsigned int                       fixed_advance;
} DFBFontDescription;

/*
 * Flags describing the font style. These values are bit flags
 * and may be combined.
 */
typedef enum {
     DFFS_NORMAL         = 0x00000000,  /* no special style */
     DFFS_BOLD           = 0x00000001,  /* bold style */
     DFFS_ITALIC         = 0x00000002   /* italic style */
} DFBFontStyle;

/*
 * Pixel format of a surface.
 * Contains information about the format (see following definition).
 *
 * Format constants are encoded in the following way (bit 31 - 0):
 *
 * -hgg:ffff | eeee:dddc | bbbb:bbbb | aaaa:aaaa
 *
 * a) pixelformat index<br>
 * b) effective bits per pixel of format<br>
 * c) alpha channel present<br>
 * d) bytes per pixel in a row (1/8 fragment, i.e. bits)<br>
 * e) bytes per pixel in a row (decimal part, i.e. bytes)<br>
 * f) multiplier for planes minus one (1/16 fragment)<br>
 * g) multiplier for planes minus one (decimal part)<br>
 * h) indexed pixelformat (using a palette)
 */
typedef enum {
     DSPF_UNKNOWN        = 0x00000000,  /* no specific format,
                                           unusual and unsupported */
     DSPF_ARGB1555       = 0x00211001,  /* 16bit ARGB (2 bytes, alpha 1@15,
                                           red 5@10, green 5@5, blue 5@0) */
     DSPF_RGB16          = 0x00201002,  /* 16bit  RGB (2 bytes, red 5@11,
                                           green 6@5, blue 5@0) */
     DSPF_RGB24          = 0x00301803,  /* 24bit  RGB (3 bytes, red 8@16,
                                           green 8@8, blue 8@0) */
     DSPF_RGB32          = 0x00401804,  /* 24bit  RGB (4 bytes, nothing@24,
                                           red 8@16, green 8@8, blue 8@0)*/
     DSPF_ARGB           = 0x00412005,  /* 32bit ARGB (4 bytes, alpha 8@24,
                                           red 8@16, green 8@8, blue 8@0)*/
     DSPF_A8             = 0x00110806,  /* 8bit alpha (1 byte, alpha 8@0 ),
                                           e.g. anti-aliased text glyphs */
     DSPF_YUY2           = 0x00201007,  /* A macropixel (32bit / 2 pixel)
                                           contains YUYV (starting with
                                           the LOWEST byte on the LEFT) */
     DSPF_RGB332         = 0x00100808,  /* 8bit true color (1 byte,
                                           red 3@5, green 3@2, blue 2@0 */
     DSPF_UYVY           = 0x00201009,  /* A macropixel (32bit / 2 pixel)
                                           contains UYVY (starting with
                                           the LOWEST byte on the LEFT) */
     DSPF_I420           = 0x08100C0A,  /* 8 bit Y plane followed by 8 bit
                                           2x2 subsampled U and V planes */
     DSPF_YV12           = 0x08100C0B,  /* 8 bit Y plane followed by 8 bit
                                           2x2 subsampled V and U planes */
     DSPF_LUT8           = 0x4011080C,  /* 8 bit lookup table (palette) */

     DSPF_RGB15          = DSPF_ARGB1555
} DFBSurfacePixelFormat;

/* Number of pixelformats defined */
#define DFB_NUM_PIXELFORMATS            12

/* These macros extract information about the pixel format. */
#define DFB_PIXELFORMAT_INDEX(fmt)      (((fmt) & 0x0000FF) - 1)

#define DFB_BYTES_PER_PIXEL(fmt)        (((fmt) & 0xF00000) >> 20)

#define DFB_BITS_PER_PIXEL(fmt)         (((fmt) & 0x00FF00) >>  8)

#define DFB_PIXELFORMAT_HAS_ALPHA(fmt)  ((fmt) & 0x00010000)

#define DFB_PIXELFORMAT_IS_INDEXED(fmt) ((fmt) & 0x40000000)

#define DFB_BYTES_PER_LINE(fmt,width)   (((((fmt) & 0xFE0000) >> 17) * \
                                          (width)) >> 3)

#define DFB_PLANAR_PIXELFORMAT(fmt)     ((fmt) & 0x3F000000)

#define DFB_PLANE_MULTIPLY(fmt,height)  ((((((fmt) & 0x3F000000) >> 24) + \
                                           0x10) * (height)) >> 4 )

/*
 * Description of the surface that is to be created.
 */
typedef struct {
     DFBSurfaceDescriptionFlags         flags;       /* field validation */

     DFBSurfaceCapabilities             caps;        /* capabilities */
     unsigned int                       width;       /* pixel width */
     unsigned int                       height;      /* pixel height */
     DFBSurfacePixelFormat              pixelformat; /* pixel format */

     struct {
          void                         *data;        /* data pointer of
                                                        existing buffer */
          int                           pitch;       /* pitch of buffer */
     } preallocated[2];

     struct {
          DFBColor                     *entries;
          unsigned int                  size;
     } palette;
} DFBSurfaceDescription;

/*
 * Description of the palette that is to be created.
 */
typedef struct {
     DFBPaletteDescriptionFlags         flags;       /* Validation of fields. */

     DFBPaletteCapabilities             caps;        /* Palette capabilities. */
     unsigned int                       size;        /* Number of entries. */
     DFBColor                          *entries;     /* Preset palette
                                                        entries. */
} DFBPaletteDescription;

#define DFB_DISPLAY_LAYER_DESC_NAME_LENGTH   30

/*
 * Description of the display layer capabilities.
 */
typedef struct {
     DFBDisplayLayerTypeFlags           type;        /* Classification of the
                                                        display layer. */
     DFBDisplayLayerCapabilities        caps;        /* Capability flags of
                                                        the display layer. */

     char name[DFB_DISPLAY_LAYER_DESC_NAME_LENGTH];  /* Display layer name. */
} DFBDisplayLayerDescription;

/*
 * Return value of callback function of enumerations.
 */
typedef enum {
     DFENUM_OK           = 0x00000000,  /* Proceed with enumeration */
     DFENUM_CANCEL       = 0x00000001   /* Cancel enumeration */
} DFBEnumerationResult;

typedef unsigned int DFBDisplayLayerID;

/*
 * Called for each existing display layer.
 * "layer_id" can be used to get an interface to the layer.
 */
typedef DFBEnumerationResult (*DFBDisplayLayerCallback) (
     DFBDisplayLayerID                  layer_id,
     DFBDisplayLayerDescription         desc,
     void                              *callbackdata
);

/*
 * Flags defining which fields of a DFBColorAdjustment are valid.
 */
typedef enum {
     DCAF_NONE         = 0x00000000,  /* none of these              */
     DCAF_BRIGHTNESS   = 0x00000001,  /* brightness field is valid  */
     DCAF_CONTRAST     = 0x00000002,  /* contrast field is valid    */
     DCAF_HUE          = 0x00000004,  /* hue field is valid         */
     DCAF_SATURATION   = 0x00000008   /* saturation field is valid  */
} DFBColorAdjustmentFlags;

/*
 * Color Adjustment used to adjust video colors.
 *
 * All fields are in the range 0x0 to 0xFFFF with
 * 0x8000 as the default value (no adjustment).
 */
typedef struct {
     DFBColorAdjustmentFlags  flags;

     __u16                    brightness;
     __u16                    contrast;
     __u16                    hue;
     __u16                    saturation;
} DFBColorAdjustment;


/*
 * <i><b>IDirectFB</b></i> is the main interface. It can be
 * retrieved by a call to <i>DirectFBCreate</i>. It's the only
 * interface with a global creation facility. Other interfaces
 * are created by this interface or interfaces created by it.
 *
 * <b>Hardware capabilities</b> such as the amount of video
 * memory or a list of supported drawing/blitting functions and
 * flags can be retrieved.  It also provides enumeration of all
 * supported video modes.
 *
 * <b>Surfaces</b> for general purpose use can be created via
 * <i>CreateSurface</i>. These surfaces are so called "offscreen
 * surfaces" and could be used for sprites or icons.
 *
 * The <b>primary surface</b> is an abstraction and API shortcut
 * for getting a surface for visual output. Fullscreen games for
 * example have the whole screen as their primary
 * surface. The primary surface is also created via
 * <i>CreateSurface</i> but with the special capability
 * DSCAPS_PRIMARY.
 *
 * The <b>video mode</b> can be changed via <i>SetVideoMode</i>
 * and is the size and depth of the primary surface, i.e. the
 * screen.
 * 
 * <b>Fonts</b> are created by this
 * interface. There are different implementations for different
 * content types. On creation a suitable implementation is
 * automatically chosen.
 */
DEFINE_INTERFACE( IDirectFB )

   /** Cooperative level, video mode **/

     /*
      * Switch the current video mode (primary layer).
      *
      * If in shared cooperative level this function sets the
      * resolution of the window that is created implicitly for
      * the primary surface.
      */
     DFBResult (*SetVideoMode) (
          IDirectFB                *thiz,
          unsigned int              width,
          unsigned int              height,
          unsigned int              bpp
     );


   /** Hardware capabilities **/

     /*
      * Get a rough description of all drawing/blitting functions
      * along with drawing/blitting flags supported by the hardware.
      *
      * For more detailed information use
      * IDirectFBSurface->GetAccelerationMask().
      */
     DFBResult (*GetCardCapabilities) (
          IDirectFB                *thiz,
          DFBCardCapabilities      *caps
     );

   /** Surfaces & Palettes **/

     /*
      * Create a surface matching the specified description.
      */
     DFBResult (*CreateSurface) (
          IDirectFB                *thiz,
          DFBSurfaceDescription    *desc,
          IDirectFBSurface        **interface
     );

     /*
      * Create a palette matching the specified description.
      *
      * Passing a NULL description creates a default palette with
      * 256 entries filled with colors matching the RGB332 format.
      */
     DFBResult (*CreatePalette) (
          IDirectFB                *thiz,
          DFBPaletteDescription    *desc,
          IDirectFBPalette        **interface
     );


   /** Display Layers **/

     /*
      * Enumerate all existing display layers.
      *
      * Calls the given callback for all available display
      * layers. The callback is passed the layer id that can be
      * used to retrieve an interface on a specific layer using
      * IDirectFB->GetDisplayLayer().
      */
     DFBResult (*EnumDisplayLayers) (
          IDirectFB                *thiz,
          DFBDisplayLayerCallback   callback,
          void                     *callbackdata
     );

     /*
      * Retrieve an interface to a specific display layer.
      */
     DFBResult (*GetDisplayLayer) (
          IDirectFB                *thiz,
          DFBDisplayLayerID         layer_id,
          IDirectFBDisplayLayer   **interface
     );

     /*
      * Load a font from the specified buffer given a description
      * of how to load the glyphs. The buffer is not copied and therefore
      * should not be freed prematurely by the caller.
      */
     DFBResult (*CreateFontFromBuffer) (
          IDirectFB                *thiz,
          const char               *fontbuffer,
          int                       fontsize,
          DFBFontDescription       *desc,
          IDirectFBFont           **interface
     );
DEFINE_INTERFACE_END

/* predefined layer ids */
#define DLID_PRIMARY          0x00

/*
 * Cooperative level handling the access permissions.
 */
typedef enum {
     DLSCL_EXCLUSIVE          = 1
} DFBDisplayLayerCooperativeLevel;

/*
 * Layer configuration flags
 */
typedef enum {
     DLCONF_WIDTH             = 0x00000001,
     DLCONF_HEIGHT            = 0x00000002,
     DLCONF_PIXELFORMAT       = 0x00000004,
     DLCONF_BUFFERMODE        = 0x00000008,
     DLCONF_OPTIONS           = 0x00000010
} DFBDisplayLayerConfigFlags;

/*
 * Layer configuration
 */
typedef struct {
     DFBDisplayLayerConfigFlags    flags;       /* Which fields of the
                                                   configuration are set */

     unsigned int                  width;       /* Pixel width */
     unsigned int                  height;      /* Pixel height */
     DFBSurfacePixelFormat         pixelformat; /* Pixel format */
     DFBDisplayLayerBufferMode     buffermode;  /* Buffer mode */
     DFBDisplayLayerOptions        options;     /* Enable capabilities */
} DFBDisplayLayerConfig;


/*************************
 * IDirectFBDisplayLayer *
 *************************/

/*
 * <i>No summary yet...</i>
 */
DEFINE_INTERFACE ( IDirectFBDisplayLayer )

   /** Retrieving information **/

     /*
      * Get the unique layer ID.
      */
     DFBResult (*GetID) (
          IDirectFBDisplayLayer              *thiz,
          DFBDisplayLayerID                  *layer_id
     );

     /*
      * Get a description of this display layer, i.e. the capabilities.
      */
     DFBResult (*GetDescription) (
          IDirectFBDisplayLayer              *thiz,
          DFBDisplayLayerDescription         *desc
     );


   /** Surface **/

     /*
      * Get an interface to layer's surface.
      *
      * Only available in exclusive mode.
      */
     DFBResult (*GetSurface) (
          IDirectFBDisplayLayer              *thiz,
          IDirectFBSurface                  **interface
     );


   /** Configuration handling **/

     /*
      * Get current layer configuration.
      */
     DFBResult (*GetConfiguration) (
          IDirectFBDisplayLayer              *thiz,
          DFBDisplayLayerConfig              *config
     );

     /*
      * Test layer configuration.
      *
      * If configuration fails and 'failed' is not NULL it will
      * indicate which fields of the configuration caused the
      * error.
      */
     DFBResult (*TestConfiguration) (
          IDirectFBDisplayLayer              *thiz,
          DFBDisplayLayerConfig              *config,
          DFBDisplayLayerConfigFlags         *failed
     );

DEFINE_INTERFACE_END


/*
 * Flipping flags controlling the behaviour of Flip().
 */
typedef enum {
     DSFLIP_BLIT         = 0x00000002   /* copy backbuffer into
                                           frontbuffer rather than
                                           just swapping these buffers */
} DFBSurfaceFlipFlags;

/*
 * Flags controlling the text layout.
 */
typedef enum {
     DSTF_LEFT           = 0x00000000,  /* left aligned */
     DSTF_CENTER         = 0x00000001,  /* horizontally centered */
     DSTF_RIGHT          = 0x00000002,  /* right aligned */

     DSTF_TOP            = 0x00000004,  /* y specifies the top
                                           instead of the baseline */
     DSTF_BOTTOM         = 0x00000008,  /* y specifies the bottom
                                           instead of the baseline */

     DSTF_TOPLEFT        = DSTF_TOP | DSTF_LEFT,
     DSTF_TOPCENTER      = DSTF_TOP | DSTF_CENTER,
     DSTF_TOPRIGHT       = DSTF_TOP | DSTF_RIGHT,

     DSTF_BOTTOMLEFT     = DSTF_BOTTOM | DSTF_LEFT,
     DSTF_BOTTOMCENTER   = DSTF_BOTTOM | DSTF_CENTER,
     DSTF_BOTTOMRIGHT    = DSTF_BOTTOM | DSTF_RIGHT
} DFBSurfaceTextFlags;

/*
 * Flags defining the type of data access.
 * These are important for surface swapping management.
 */
typedef enum {
     DSLF_READ           = 0x00000001,  /* request read access while
                                           surface is locked */
     DSLF_WRITE          = 0x00000002   /* request write access */
} DFBSurfaceLockFlags;

/*
 * Available Porter/Duff rules.
 */
typedef enum {
                              /* pixel = (source * fs + destination * fd),
                                 sa = source alpha,
                                 da = destination alpha */
     DSPD_NONE           = 0, /* fs: sa      fd: 1.0-sa (defaults) */
     DSPD_CLEAR          = 1, /* fs: 0.0     fd: 0.0    */
     DSPD_SRC            = 2, /* fs: 1.0     fd: 0.0    */
     DSPD_SRC_OVER       = 3, /* fs: 1.0     fd: 1.0-sa */
     DSPD_DST_OVER       = 4, /* fs: 1.0-da  fd: 1.0    */
     DSPD_SRC_IN         = 5, /* fs: da      fd: 0.0    */
     DSPD_DST_IN         = 6, /* fs: 0.0     fd: sa     */
     DSPD_SRC_OUT        = 7, /* fs: 1.0-da  fd: 0.0    */
     DSPD_DST_OUT        = 8, /* fs: 0.0     fd: 1.0-sa */
	 DSPD_XOR			 = 9
} DFBSurfacePorterDuffRule;

/*
 * Blend functions to use for source and destination blending
 */
typedef enum {
     DSBF_ZERO               = 1,  /* */
     DSBF_ONE                = 2,  /* */
     DSBF_SRCCOLOR           = 3,  /* */
     DSBF_INVSRCCOLOR        = 4,  /* */
     DSBF_SRCALPHA           = 5,  /* */
     DSBF_INVSRCALPHA        = 6,  /* */
     DSBF_DESTALPHA          = 7,  /* */
     DSBF_INVDESTALPHA       = 8,  /* */
     DSBF_DESTCOLOR          = 9,  /* */
     DSBF_INVDESTCOLOR       = 10, /* */
     DSBF_SRCALPHASAT        = 11, /* */
	 DSBF_XOR				 = 12
} DFBSurfaceBlendFunction;

/********************
 * IDirectFBSurface *
 ********************/

/*
 * <i>No summary yet...</i>
 */
DEFINE_INTERFACE ( IDirectFBSurface )

   /** Retrieving information **/

     /*
      * Return the capabilities of this surface.
      */
     DFBResult (*GetCapabilities) (
          IDirectFBSurface         *thiz,
          DFBSurfaceCapabilities   *caps
     );

     /*
      * Get the surface's width and height in pixels.
      */
     DFBResult (*GetSize) (
          IDirectFBSurface         *thiz,
          unsigned int             *width,
          unsigned int             *height
     );

     /*
      * Created sub surfaces might be clipped by their parents,
      * this function returns the resulting rectangle relative
      * to this surface.
      *
      * For non sub surfaces this function returns
      * { 0, 0, width, height }.
      */
     DFBResult (*GetVisibleRectangle) (
          IDirectFBSurface         *thiz,
          DFBRectangle             *rect
     );

     /*
      * Get the current pixel format.
      */
     DFBResult (*GetPixelFormat) (
          IDirectFBSurface         *thiz,
          DFBSurfacePixelFormat    *format
     );

     /*
      * Get a mask of drawing functions that are hardware
      * accelerated with the current settings.
      *
      * If a source surface is specified the mask will also
      * contain accelerated blitting functions.  Note that there
      * is no guarantee that these will actually be accelerated
      * since the surface storage (video/system) is examined only
      * when something actually gets drawn or blitted.
      */
     DFBResult (*GetAccelerationMask) (
          IDirectFBSurface         *thiz,
          IDirectFBSurface         *source,
          DFBAccelerationMask      *mask
     );


   /** Palette control **/

     /*
      * Get access to the surface's palette.
      *
      * Returns an interface that can be used to gain
      * read and/or write access to the surface's palette.
      */
     DFBResult (*GetPalette) (
          IDirectFBSurface         *thiz,
          IDirectFBPalette        **interface
     );

     /*
      * Change the surface's palette.
      */
     DFBResult (*SetPalette) (
          IDirectFBSurface         *thiz,
          IDirectFBPalette         *palette
     );
   
     
   /** Buffer operations **/

     /*
      * Lock the surface for the access type specified.
      *
      * Returns a data pointer and the line pitch of it.
      */
     DFBResult (*Lock) (
          IDirectFBSurface         *thiz,
          DFBSurfaceLockFlags       flags,
          void                    **ptr,
          int                      *pitch
     );

     /*
      * Unlock the surface after direct access.
      */
     DFBResult (*Unlock) (
          IDirectFBSurface         *thiz
     );

     /*
      * Flip the two buffers of the surface.
      *
      * If no region is specified the whole surface is flipped,
      * otherwise blitting is used to update the region.
      * This function fails if the surfaces capabilities don't
      * include DSCAPS_FLIPPING.
      */
     DFBResult (*Flip) (
          IDirectFBSurface         *thiz,
          const DFBRegion          *region,
          DFBSurfaceFlipFlags       flags
     );

     /*
      * Set the active field.
      *
      * Interlaced surfaces consist of two fields. Software driven
      * deinterlacing uses this method to manually switch the field
      * that is displayed, e.g. scaled up vertically by two.
      */
     DFBResult (*SetField) (
          IDirectFBSurface         *thiz,
          int                       field
     );

     /*
      * Clear the surface with an extra color.
      *
      * Fills the whole (sub) surface with the specified color
      * ignoring drawing flags and color of the current state,
      * but limited to the current clip.
      *
      * As with all drawing and blitting functions the backbuffer
      * is written to. If you are initializing a double buffered
      * surface you may want to clear both buffers by doing a
      * Clear-Flip-Clear sequence.
      */
     DFBResult (*Clear) (
          IDirectFBSurface         *thiz,
          __u8                      r,
          __u8                      g,
          __u8                      b,
          __u8                      a
     );


   /** Drawing/blitting control **/

     /*
      * Set the clipping region used to limitate the area for
      * drawing, blitting and text functions.
      *
      * If no region is specified (NULL passed) the clip is set
      * to the surface extents (initial clip).
      */
     DFBResult (*SetClip) (
          IDirectFBSurface         *thiz,
          const DFBRegion          *clip
     );

	 /*
      * Set the alpha constant, the source alpha value is modulated with
      * For example if alpha = 127 and the source alpha value is 127.
      * The source alpha value is adjusted to 127 * 0.5 = 63
      */
	 DFBResult (*SetAlphaConstant) (
		 IDirectFBSurface *thiz, __u8 a 
	 );

     /*
      * Set the color used for drawing/text functions or
      * alpha/color modulation (blitting functions).
      *
      * If you are not using the alpha value it should be set to
      * 0xff to ensure visibility when the code is ported to or
      * used for surfaces with an alpha channel.
      *
      * This method should be avoided for surfaces with an indexed
      * pixelformat, e.g. DSPF_LUT8, otherwise an expensive search
      * in the color/alpha lookup table occurs.
      */
     DFBResult (*SetColor) (
          IDirectFBSurface         *thiz,
          __u8                      r,
          __u8                      g,
          __u8                      b,
          __u8                      a
     );

     /*
      * Set the color like with SetColor() but using
      * an index to the color/alpha lookup table.
      *
      * This method is only supported by surfaces with an
      * indexed pixelformat, e.g. DSPF_LUT8. For these formats
      * this method should be used instead of SetColor().
      */
     DFBResult (*SetColorIndex) (
          IDirectFBSurface         *thiz,
          unsigned int              index
     );

     /*
      * Set the blend function that applies to the source.
      */
     DFBResult (*SetSrcBlendFunction) (
          IDirectFBSurface         *thiz,
          DFBSurfaceBlendFunction   function
     );

     /*
      * Set the blend function that applies to the destination.
      */
     DFBResult (*SetDstBlendFunction) (
          IDirectFBSurface         *thiz,
          DFBSurfaceBlendFunction   function
     );

     /*
      * Set the source and destination blend function by
      * specifying a Porter/Duff rule.
      */
     DFBResult (*SetPorterDuff) (
          IDirectFBSurface         *thiz,
          DFBSurfacePorterDuffRule  rule
     );

     /*
      * Set the source color key, i.e. the color that is excluded
      * when blitting FROM this surface TO another that has
      * source color keying enabled.
      */
     DFBResult (*SetSrcColorKey) (
          IDirectFBSurface         *thiz,
          __u8                      r,
          __u8                      g,
          __u8                      b
     );

     /*
      * Set the source color key like with SetSrcColorKey() but using
      * an index to the color/alpha lookup table.
      *
      * This method is only supported by surfaces with an
      * indexed pixelformat, e.g. DSPF_LUT8. For these formats
      * this method should be used instead of SetSrcColorKey().
      */
     DFBResult (*SetSrcColorKeyIndex) (
          IDirectFBSurface         *thiz,
          unsigned int              index
     );

     /*
      * Set the destination color key, i.e. the only color that
      * gets overwritten by drawing and blitting to this surface
      * when destination color keying is enabled.
      */
     DFBResult (*SetDstColorKey) (
          IDirectFBSurface         *thiz,
          __u8                      r,
          __u8                      g,
          __u8                      b
     );

     /*
      * Set the destination color key like with SetDstColorKey() but using
      * an index to the color/alpha lookup table.
      *
      * This method is only supported by surfaces with an
      * indexed pixelformat, e.g. DSPF_LUT8. For these formats
      * this method should be used instead of SetDstColorKey().
      */
     DFBResult (*SetDstColorKeyIndex) (
          IDirectFBSurface         *thiz,
          unsigned int              index
     );


   /** Blitting functions **/

     /*
      * Set the flags for all subsequent blitting commands.
      */
     DFBResult (*SetBlittingFlags) (
          IDirectFBSurface         *thiz,
          DFBSurfaceBlittingFlags   flags
     );

     /*
      * Blit an area from the source to this surface.
      *
      * Pass a NULL rectangle to use the whole source surface.
      * Source may be the same surface.
      */
     DFBResult (*Blit) (
          IDirectFBSurface         *thiz,
          IDirectFBSurface         *source,
          const DFBRectangle       *source_rect,
          int                       x,
          int                       y
     );

     /*
      * Blit an area from the source tiled to this surface.
      *
      * Pass a NULL rectangle to use the whole source surface.
      * Source may be the same surface.
      */
     DFBResult (*TileBlit) (
          IDirectFBSurface         *thiz,
          IDirectFBSurface         *source,
          const DFBRectangle       *source_rect,
          int                       x,
          int                       y
     );

     /*
      * Blit an area scaled from the source to the destination
      * rectangle.
      *
      * Pass a NULL rectangle to use the whole source surface.
      */
     DFBResult (*StretchBlit) (
          IDirectFBSurface         *thiz,
          IDirectFBSurface         *source,
          const DFBRectangle       *source_rect,
          const DFBRectangle       *destination_rect
     );


   /** Drawing functions **/

     /*
      * Set the flags for all subsequent drawing commands.
      */
     DFBResult (*SetDrawingFlags) (
          IDirectFBSurface         *thiz,
          DFBSurfaceDrawingFlags    flags
     );

     /*
      * Fill the specified rectangle with the given color
      * following the specified flags.
      */
     DFBResult (*FillRectangle) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h
     );

     /*
      * Draw an outline of the specified rectangle with the given
      * color following the specified flags.
      */
     DFBResult (*DrawRectangle) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h
     );

     /*
      * Fill the specified rounded rectangle with the given color
      * following the specified flags.
      */
     DFBResult (*FillRoundRect) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h,
          int                       xd,
          int                       yd
     );

     /*
      * Draw an outline of the specified rounded rectangle with the given
      * color following the specified flags.
      */
     DFBResult (*DrawRoundRect) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h,
          int                       xd,
          int                       yd
     );

     /*
      * Fill the specified oval with the given color
      * following the specified flags.
      */
     DFBResult (*FillOval) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h
     );

     /*
      * Draw an outline of the specified oval with the given
      * color following the specified flags.
      */
     DFBResult (*DrawOval) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h
     );

     /*
      * Fill the specified arc with the given color
      * following the specified flags.
      */
     DFBResult (*FillArc) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h,
          int                       start,
          int                       arcAngle
     );

     /*
      * Draw an outline of the specified arc with the given
      * color following the specified flags.
      */
     DFBResult (*DrawArc) (
          IDirectFBSurface         *thiz,
          int                       x,
          int                       y,
          int                       w,
          int                       h,
          int                       start,
          int                       arcAngle
     );

     /*
      * Fill the specified polygon with the given color
      * following the specified flags.
      */
     DFBResult (*FillPolygon) (
          IDirectFBSurface         *thiz,
          int                       *xPoints,
          int                       *yPoints,
          int                       nPoints
     );

     /*
      * Draw a line from one point to the other with the given color
      * following the drawing flags.
      */
     DFBResult (*DrawLine) (
          IDirectFBSurface         *thiz,
          int                       x1,
          int                       y1,
          int                       x2,
          int                       y2
     );

     /*
      * Draw 'num_lines' lines with the given color following the
      * drawing flags. Each line specified by a DFBRegion.
      */
     DFBResult (*DrawLines) (
          IDirectFBSurface         *thiz,
          const DFBRegion          *lines,
          unsigned int              num_lines
     );

   /** Text functions **/

     /*
      * Set the font used by DrawString() and DrawGlyph().
      * You can pass NULL here to unset the font.
      */
     DFBResult (*SetFont) (
          IDirectFBSurface         *thiz,
          IDirectFBFont            *font
     );

     /*
      * Get the font associated with a surface.
      *
      * This function increases the font's reference count.
      */
     DFBResult (*GetFont) (
          IDirectFBSurface         *thiz,
          IDirectFBFont           **font
     );

     /*
      * Draw an UTF-8 string at the specified position with the
      * given color following the specified flags.
      *
      * If font was loaded with the DFFA_CHARMAP flag, the string 
      * specifies UTF-8 encoded raw glyph indices.
      * 
      * Bytes specifies the number of bytes to take from the
      * string or -1 for the complete NULL-terminated string. You
      * need to set a font using the SetFont() method before
      * calling this function.
      */
     DFBResult (*DrawString) (
          IDirectFBSurface         *thiz,
          const char               *text,
          int                       bytes,
          int                       x,
          int                       y,
          DFBSurfaceTextFlags       flags
     );

     /*
      * Draw a single glyph specified by its Unicode index at the
      * specified position with the given color following the
      * specified flags.
      *
      * If font was loaded with the DFFA_NOCHARMAP flag, index specifies
      * the raw glyph index in the font.
      * 
      * You need to set a font using the SetFont() method before
      * calling this function.
      */
     DFBResult (*DrawGlyph) (
          IDirectFBSurface         *thiz,
          unsigned int              index,
          int                       x,
          int                       y,
          DFBSurfaceTextFlags       flags
     );

   /** Lightweight helpers **/

     /*
      * Get an interface to a sub area of this surface.
      *
      * No image data is duplicated, this is a clipped graphics
      * within the original surface. This is very helpful for
      * lightweight components in a GUI toolkit.  The new
      * surface's state (color, drawingflags, etc.) is
      * independent from this one. So it's a handy graphics
      * context.  If no rectangle is specified, the whole surface
      * (or a part if this surface is a subsurface itself) is
      * represented by the new one.
      */
     DFBResult (*GetSubSurface) (
          IDirectFBSurface         *thiz,
          const DFBRectangle       *rect,
          IDirectFBSurface        **interface
     );

DEFINE_INTERFACE_END


/********************
 * IDirectFBPalette *
 ********************/

/*
 * <i>No summary yet...</i>
 */
DEFINE_INTERFACE ( IDirectFBPalette )

   /** Retrieving information **/

     /*
      * Return the capabilities of this palette.
      */
     DFBResult (*GetCapabilities) (
          IDirectFBPalette         *thiz,
          DFBPaletteCapabilities   *caps
     );

     /*
      * Get the number of entries in the palette.
      */
     DFBResult (*GetSize) (
          IDirectFBPalette         *thiz,
          unsigned int             *size
     );


   /** Palette entries **/

     /*
      * Write entries to the palette.
      *
      * Writes the specified number of entries to the palette at the
      * specified offset.
      */
     DFBResult (*SetEntries) (
          IDirectFBPalette         *thiz,
          DFBColor                 *entries,
          unsigned int              num_entries,
          unsigned int              offset
     );

     /*
      * Read entries from the palette.
      *
      * Reads the specified number of entries from the palette at the
      * specified offset.
      */
     DFBResult (*GetEntries) (
          IDirectFBPalette         *thiz,
          DFBColor                 *entries,
          unsigned int              num_entries,
          unsigned int              offset
     );

     /*
      * Find the best matching entry.
      *
      * Searches the map for an entry which best matches the specified color.
      */
     DFBResult (*FindBestMatch) (
          IDirectFBPalette         *thiz,
          __u8                      r,
          __u8                      g,
          __u8                      b,
          __u8                      a,
          unsigned int             *index
     );


   /** Clone **/

     /*
      * Create a copy of the palette.
      */
     DFBResult (*CreateCopy) (
          IDirectFBPalette         *thiz,
          IDirectFBPalette        **interface
     );
DEFINE_INTERFACE_END


/*****************
 * IDirectFBFont *
 *****************/

/*
 * <i>No summary yet...</i>
 */
DEFINE_INTERFACE (IDirectFBFont)

   /** Retrieving information **/

     /*
      * Get the distance from the baseline to the top of the
      * logical extents of this font.
      */
     DFBResult (*GetAscender) (
          IDirectFBFont       *thiz,
          int                 *ascender
     );

     /*
      * Get the distance from the baseline to the bottom of
      * the logical extents of this font.
      *
      * This is a negative value!
      */
     DFBResult (*GetDescender) (
          IDirectFBFont       *thiz,
          int                 *descender
     );

     /*
      * Get the logical height of this font. This is the vertical
      * distance from one baseline to the next when writing
      * several lines of text. Note that this value does not
      * correspond the height value specified when loading the
      * font.
      */
     DFBResult (*GetHeight) (
          IDirectFBFont       *thiz,
          int                 *height
     );

     /*
      * Get the maximum character width.
      *
      * This is a somewhat dubious value. Not all fonts
      * specify it correcly. It can give you an idea of
      * the maximum expected width of a rendered string.
      */
     DFBResult (*GetMaxAdvance) (
          IDirectFBFont       *thiz,
          int                 *maxadvance
     );

     /*
      * Get the kerning to apply between two glyphs specified by
      * their Unicode indices.
      */
     DFBResult (*GetKerning) (
          IDirectFBFont       *thiz,
          unsigned int         prev_index,
          unsigned int         current_index,
          int                 *kern_x,
          int                 *kern_y
     );

   /** String extents measurement **/

     /*
      * Get the logical width of the specified UTF-8 string
      * as if it were drawn with this font.
      *
      * Bytes specifies the number of bytes to take from the
      * string or -1 for the complete NULL-terminated string.
      *
      * The returned width may be different than the actual drawn
      * width of the text since this function returns the logical
      * width that should be used to layout the text. A negative
      * width indicates right-to-left rendering.
      */
     DFBResult (*GetStringWidth) (
          IDirectFBFont       *thiz,
          const char          *text,
          int                  bytes,
          int                 *width
     );

     /*
      * Get the logical and real extents of the specified
      * UTF-8 string as if it were drawn with this font.
      *
      * Bytes specifies the number of bytes to take from the
      * string or -1 for the complete NULL-terminated string.
      *
      * The logical rectangle describes the typographic extents
      * and should be used to layout text. The ink rectangle
      * describes the smallest rectangle containing all pixels
      * that are touched when drawing the string. If you only
      * need one of the rectangles, pass NULL for the other one.
      *
      * The ink rectangle is guaranteed to be a valid rectangle
      * with positive width and height, while the logical
      * rectangle may have negative width indicating right-to-left
      * layout.
      *
      * The rectangle offsets are reported relative to the
      * baseline and refer to the text being drawn using
      * DSTF_LEFT.
      */
     DFBResult (*GetStringExtents) (
          IDirectFBFont       *thiz,
          const char          *text,
          int                  bytes,
          DFBRectangle        *logical_rect,
          DFBRectangle        *ink_rect
     );

     /*
      * Get the extents of a glyph specified by its Unicode
      * index.
      *
      * The rectangle describes the the smallest rectangle
      * containing all pixels that are touched when drawing the
      * glyph. It is reported relative to the baseline. If you
      * only need the advance, pass NULL for the rectangle.
      *
      * The advance describes the horizontal offset to the next
      * glyph (without kerning applied). It may be a negative
      * value indicating left-to-right rendering. If you don't
      * need this value, pass NULL for advance.
      */
    DFBResult (*GetGlyphExtents) (
          IDirectFBFont       *thiz,
          unsigned int         index,
          DFBRectangle        *rect,
          int                 *advance
     );
     /*
      * Get the family name for this font.
      */
     DFBResult (*GetFamilyName) (
          IDirectFBFont       *thiz,
          char                **family
     );

     /*
      * Get the style flags for this font.
      */
     DFBResult (*GetStyleFlags) (
          IDirectFBFont       *thiz,
          DFBFontStyle        *style
     );
DEFINE_INTERFACE_END

#ifdef __cplusplus
}
#endif

#endif
