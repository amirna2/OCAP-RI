/*
  ,---------------------------------------------------------------------------,
  |                                                                           |
  |                     Copyright 2004-2005 OCAP Development LLC              |
  |                              All rights reserved                          |
  |                            Reproduced Under License                       |
  |                                                                           |
  |  This source code is the proprietary confidential property of             |
  |  OCAP Development LLC and is provided to recipient for documentation and  |
  |  educational purposes only. Reproduction, publication, or distribution in |
  |  any form to any party other than the recipient is strictly prohibited.   |
  |                                                                           |
  `---------------------------------------------------------------------------'
*/

#include <ocap/extensions.h>

#include <gfx/clip.h>
#include <core/state.h>

static void gRoundRectImpl(SCSTATE *scs, DFBRectangle *rect, DFBDimension *oval, DFBBoolean fill);

/*
 * Draw an oval that exactly fills the specified rectangle.
 *
 * @param rect bounding box that specifies the bounds of the oval
 * @param fill a filled shape is drawn if TRUE - otherwise, an outlined shape is drawn
 */
void gOval(SCSTATE *scs, DFBRectangle *rect, DFBBoolean fill)
{
    DFBDimension oval;
     
    oval.w = rect->w;
    oval.h = rect->h;
    gRoundRect(scs, rect, &oval, fill);
}

/*
 * Draw a rounded rectangle whose bounds are defined by <code>rect</code>. The four
 * corners of the rounded rectangle when combined create an oval with the dimensions
 * specified by <code>oval</code>.
 *
 * @param rect bounding box that specifies the bounds of the rounded rectangle
 * @param oval defines the dimensions of the oval within the rectangle. The oval is
 *      split into four 90 degree arcs which are placed in the corners of the
 *      rectangle. If the dimensions of the oval are larger than that specified by
 *      <code>rect</code> then they are reduced to fit within <code>rect</code>.
 * @param fill a filled shape is drawn if TRUE - otherwise, an outlined shape is drawn
 */
void gRoundRect(SCSTATE *scs, DFBRectangle *rect, DFBDimension *oval, DFBBoolean fill)
{
	/* Only continue if there is something to do. */
	if ( dfb_clip_check( &scs->state->clip, rect) )
		gRoundRectImpl(scs, rect, oval, fill);
}
static void gRoundRectImpl(SCSTATE *scs, DFBRectangle *rect, DFBDimension *oval, DFBBoolean fill)
{
    /* Extract widths and heights and ensure that all are at least 2 pixels.
       This is required to avoid a radius of 0 in the algorithm below. */
    int rectW = (rect->w < 2) ? 2 : rect->w;
    int rectH = (rect->h < 2) ? 2 : rect->h;
    int ovalW = (oval->w < 2) ? 2 : oval->w;
    int ovalH = (oval->h < 2) ? 2 : oval->h;

    /* Values defining the oval to scan convert */
    int x1 = rect->x;
    int y1 = rect->y;
    int w = (ovalW <= rectW) ? ovalW : rectW;
    int h = (ovalH <= rectH) ? ovalH : rectH;

    /* Values defining additional width and height to add to the scan-converted oval */
    int splitW = rectW - w;
    int splitH = rectH - h;

    /* Values used to perform the scan conversion */
    int rx = w / 2;
    int ry = h / 2;
    int cenX = x1 + rx;
    int cenY = y1 + ry;
    int workX, workY;
    int lastX, lastY;
    long threshold;
    long xSquared = (long)rx*rx;
    long ySquared = (long)ry*ry;
    long xOffset, yOffset;
    int adjX = (w & 1) ? 0 : 1;
    int adjY = (h & 1) ? 0 : 1;

    /* Draw initial top and bottom points */
    if (!fill)
    {
        if (adjX == 0)
        {
            scs->drawPixel (scs, cenX, cenY-ry); /* N */
            scs->drawPixel (scs, cenX, cenY+ry-adjY+splitH); /* S */
        }
    }

    /* Set initial point to (0, ry) */
    workX = lastX = 0;
    workY = lastY = ry;
    xOffset = 0;
    yOffset = xSquared * 2 * ry;
    threshold = xSquared / 4 - xSquared * ry;

    /* Drawing while X is the major axis */
    while (1)
    {
        /* Advance to the next point */
        threshold += xOffset + ySquared;
        if (threshold >= 0)
        {
            yOffset -= xSquared * 2;
            threshold -= yOffset;
            workY--;
        }
        xOffset += ySquared * 2;
        workX++;

        /* Stop if X is no longer the major axis */
        if (xOffset >= yOffset)
            break;

        /* Plot the line segments for the current point using symmetry */
        if (fill)
        {
            if (workY != lastY)
            {
                scs->drawHorizLine (scs, cenX-lastX, cenX+lastX-adjX+splitW, cenY-lastY); /* N */
                scs->drawHorizLine (scs, cenX-lastX, cenX+lastX-adjX+splitW, cenY+lastY-adjY+splitH); /* S */
            }
            lastX = workX;
            lastY = workY;
        }
        else
        {
            scs->drawPixel (scs, cenX+workX-adjX+splitW, cenY-workY); /* NE */
            scs->drawPixel (scs, cenX-workX, cenY-workY); /* NW */
            scs->drawPixel (scs, cenX+workX-adjX+splitW, cenY+workY-adjY+splitH); /* SE */
            scs->drawPixel (scs, cenX-workX, cenY+workY-adjY+splitH); /* SW */
        }
    }

    /* If we are in fill mode we have not drawn the last line computed above. We
       will also miss drawing the center line below so do it here also. */
    if (fill)
    {
        scs->drawHorizLine (scs, cenX-lastX, cenX+lastX-adjX+splitW, cenY-lastY); /* N */
        scs->drawHorizLine (scs, cenX-lastX, cenX+lastX-adjX+splitW, cenY+lastY-adjY+splitH); /* S */
        if (adjY == 0)
            scs->drawHorizLine (scs, cenX-rx, cenX+rx-adjX+splitW, cenY); /* Center */
    }

    /* Draw initial left and right points */
    if (!fill)
    {
        if (adjY == 0)
        {
            scs->drawPixel (scs, cenX+rx-adjX+splitW, cenY); /* E */
            scs->drawPixel (scs, cenX-rx, cenY); /* W */
        }
    }

    /* Set initial point to (rx, 0) */
    workX = rx;
    workY = 0;
    xOffset = ySquared * 2 * rx;
    yOffset = 0;
    threshold = ySquared / 4 - ySquared * rx;

    /* Drawing while Y is the major axis */
    while (1)
    {
        /* Advance to the next point */
        threshold += yOffset + xSquared;
        if (threshold >= 0)
        {
            xOffset -= ySquared * 2;
            threshold -= xOffset;
            workX--;
        }
        yOffset += xSquared * 2;
        workY++;

        /* Stop if Y is no longer the major axis */
        if (yOffset > xOffset)
            break;

        /* Plot the line segments for the current point using symmetry */
        if (fill)
        {
            scs->drawHorizLine (scs, cenX-workX, cenX+workX-adjX+splitW, cenY-workY); /* N */
            scs->drawHorizLine (scs, cenX-workX, cenX+workX-adjX+splitW, cenY+workY-adjY+splitH); /* S */
        }
        else
        {
            scs->drawPixel (scs, cenX+workX-adjX+splitW, cenY-workY); /* NE */
            scs->drawPixel (scs, cenX-workX, cenY-workY); /* NW */
            scs->drawPixel (scs, cenX+workX-adjX+splitW, cenY+workY-adjY+splitH); /* SE */
            scs->drawPixel (scs, cenX-workX, cenY+workY-adjY+splitH); /* SW */
        }
    }

    /* If a vertical split was specified then draw the portion of the rounded rectangle
       between the upper and lower quadrants. */
    if (splitH != 0)
    {
        int i;
        int x2 = x1+w+splitW-1;
        int y = cenY+1-adjY;
        if (fill)
        {
            for (i=0; i < splitH; i++)
                scs->drawHorizLine (scs, x1, x2, y+i);
        }
        else
        {
            for (i=0; i < splitH; i++)
            {
                scs->drawPixel (scs, x1, y+i);
                scs->drawPixel (scs, x2, y+i);
            }
        }
    }

    /* If a horizontal split was specified and we are drawing an outlined rounded
       rectangle then draw the portion between the left and right quadrants. */
    if (splitW != 0)
    {
        if (!fill)
        {
            int x1 = cenX+1-adjX;
            int x2 = x1+splitW-1;
            scs->drawHorizLine (scs, x1, x2, y1);
            scs->drawHorizLine (scs, x1, x2, y1+h+splitH-1);
        }
    }
}
