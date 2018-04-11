/*
  ,---------------------------------------------------------------------------,
  |                                                                           |
  |                       Copyright 2004 OCAP Development LLC                 |
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
#include <misc/mem.h>

/**
 * Sort the segments in a polygon. The segments are sorted first by Y coordinate
 * from top to bottom then by X coordinate from left to right.
 * 
 * @param polygon the polygon to be sorted
 * @param numSegments the number of segments in the polygon
 * @param bottomY used to return the bottom-most scanline of the polygon
 */
void sortPolygon(DFBSegment *polygon, int numSegments, int *bottomY)
{
    int i, j;

    /* Make sure each start point is sorted relative to its end point */
    for (i=0; i < numSegments; i++)
    {
        if ((polygon[i].y1 > polygon[i].y2) ||
            ((polygon[i].y1 == polygon[i].y2) && (polygon[i].x1 > polygon[i].x2)))
        {
            /* Swap start and end points */
            DFBPoint tmp;
            tmp.x = polygon[i].x1;
            tmp.y = polygon[i].y1;
            polygon[i].x1 = polygon[i].x2;
            polygon[i].y1 = polygon[i].y2;
            polygon[i].x2 = tmp.x;
            polygon[i].y2 = tmp.y;
        }
    }

    /* Find the bottom-most scan line */
    *bottomY = 0;
    for (i=0; i < numSegments; i++)
    {
        if (polygon[i].y2 > *bottomY)
            *bottomY = polygon[i].y2;
    }

    /* Now sort the segments based on the start point for each segment. A bubble
       sort is used. */
    for (i=0; i < numSegments - 1; i++)
    {
        for (j=0; j < numSegments - 1 - i; j++)
        {
            if ((polygon[j+1].y1 < polygon[j].y1) ||
                ((polygon[j+1].y1 == polygon[j].y1) && (polygon[j+1].x1 < polygon[j].x2)))
            {
                /* Swap segments */
                DFBSegment tmp;
                tmp.x1 = polygon[j].x1;
                tmp.y1 = polygon[j].y1;
                tmp.x2 = polygon[j].x2;
                tmp.y2 = polygon[j].y2;
                polygon[j].x1 = polygon[j+1].x1;
                polygon[j].y1 = polygon[j+1].y1;
                polygon[j].x2 = polygon[j+1].x2;
                polygon[j].y2 = polygon[j+1].y2;
                polygon[j+1].x1 = tmp.x1;
                polygon[j+1].y1 = tmp.y1;
                polygon[j+1].x2 = tmp.x2;
                polygon[j+1].y2 = tmp.y2;
            }
        }
    }
}

/**
 * Draw a horizontal segment clipped against a polygon. The segment drawn
 * is from (x1,y) to (x2,y).
 * 
 * @param scs->polygon the polygon to clip against
 * @param scs->numSegments the number of segments in the polygon
 * @param scs->drawHorizLine2 the function to call to draw the portion(s) of the
 *      line that are not clipped
 */
void drawSegmentClippedToPolygon(SCSTATE *scs, int x1, int x2, int y)
{
    int n=0, i, j;
    int *xIntersects = scs->xIntersects;
    DFBBoolean leftEdge = false;

    /* Make sure x1 <= x2 */
    if (x1 > x2)
    {
        int temp = x1;
        x1 = x2;
        x2 = temp;
    }

    /* Process each segment in the polygon to determine if it intersects the segment
       being drawn. If it does then record the point of intersection. */
    for (i=0; i < scs->numSegments; i++)
    {
        /* Get the polygon segment */
        int px1 = scs->polygon[i].x1;
        int py1 = scs->polygon[i].y1;
        int px2 = scs->polygon[i].x2;
        int py2 = scs->polygon[i].y2;

        /* Skip horizontal polygon segments */
        if (py1 == py2)
            continue;

        /* If the polygon segment is below the segment to be drawn then there can
           be no more intersections */
        if (py1 > y)
            break;

        /* Check for and record any intersection with this polygon segment. Note
           that we do not consider (y == py2) to be a match (unless it is the
           bottom-most line of the entire polygon) because doing so would cause a
           shared vertex to be counted twice. */
        if ( ((y >= py1) && (y < py2)) || ((y == py2) && (y == scs->bottomY)) )
        {
            /* Get intersection of polygon segment with horizontal segment */
            if (px1 == px2)
                xIntersects[n++] = px1; /* vertical polygon segment */
            else
                xIntersects[n++] = px1 + (px2 - px1) * (y - py1) / (py2 - py1);
        }
    }

    /* Sort the intersections in ascending order using a bubble sort. */
    for (i=0; i < n - 1; i++)
    {
        for (j=0; j < n - 1 - i; j++)
        {
            if (xIntersects[j+1] < xIntersects[j])
            {
                /* Swap intersection points */
                int tmp;
                tmp = xIntersects[j];
                xIntersects[j] = xIntersects[j+1];
                xIntersects[j+1] = tmp;
            }
        }
    }

    /* Draw portions of the segment that are within the polygon */
    for (i=0; i < n; i++)
    {
        /* Determine if this is a left or right edge of the polygon */
        leftEdge = (leftEdge == true) ? false : true;
        
        /* Process left and right polygon segments. Draw the area between
           the left and right segments. */
        if (leftEdge)
        {
            /* If x intersect is to the right of the segment to draw then
               there is nothing left to draw. */
            if (xIntersects[i] > x2)
                return;
            
            /* Clip everything left of the intersection */
            if (xIntersects[i] > x1)
                x1 = xIntersects[i];
        }
        else
        {
            /* If x intersect is to the left of the segment to draw then
               there is nothing to draw yet. */
            if (xIntersects[i] < x1)
                continue;
            
            /* Draw the segment */
            if (xIntersects[i] < x2)
            {
                /* Draw portion not clipped by polygon segment */
                scs->drawHorizLine2(scs, x1, xIntersects[i], y);
                x1 = xIntersects[i] + 1;
            }
            else
            {
                /* Draw the entire segment */
                scs->drawHorizLine2(scs, x1, x2, y);
                return;
            }
        }
    }
}

/**
 * Draw a pixel clipped against a polygon. The pixel drawn is at (x,y).
 * 
 * @param scs->polygon the polygon to clip against
 * @param scs->numSegments the number of segments in the polygon
 * @param scs->drawPixel2 to function to call to draw the pixel if it is not clipped
 */
void drawPixelClippedToPolygon(SCSTATE *scs, int x, int y)
{
    drawSegmentClippedToPolygon(scs, x, x, y);
}

/**
 * Draw an filled polygon
 * 
 * @param xPoints an array containing the X coordinate of each point (vertex)
 * @param yPoints an array containing the Y coordinate of each point (vertex)
 * @param numPoints the number of points (verticies) in the polygon
 */
void gFillPolygon(SCSTATE *scs, int *xPoints, int *yPoints, int numPoints)
{
	DFBSegment staticPolygon[MAX_STATIC_POLYGON_SEGMENTS];
    DFBSegment *polygon = &staticPolygon[0];
	DFBRectangle rect;
    int staticXIntersects[MAX_STATIC_POLYGON_SEGMENTS];
    int *xIntersects = &staticXIntersects[0];
    int i, y, numSegments = 0, bottomY;
    int minX=INT_MAX, minY=INT_MAX, maxX=0, maxY=0;

    /* Allocate dynamic polygon structures if static sized versions are not big
       enough. */
    if (numPoints > MAX_STATIC_POLYGON_SEGMENTS)
    {
        /* Make one call to allocate the memory required by both arrays */
        size_t size1 = numPoints * sizeof(DFBSegment);
        size_t size2 = numPoints * sizeof(int);
        char *ptr = DFBMALLOC(size1 + size2);
        if (ptr == NULL)
        {
            // TODO(Todd): Log error message
            return;
        }
        polygon     = (DFBSegment *) (ptr);
        xIntersects = (int *) (ptr + size1);
    }

    /* Create an array of polygon segments from the list of X and Y points. Start
       out by dealing with all points but the last one. */
    for (i=0; i < numPoints-1; i++)
    {
        polygon[i].x1 = xPoints[i];
        polygon[i].y1 = yPoints[i];
        polygon[i].x2 = xPoints[i+1];
        polygon[i].y2 = yPoints[i+1];
        numSegments++;
    }

    /* Now deal with the last point specified in the list of points. If it is the
       same as the first point then we are done. Otherwise, add a segment that
       connects the last point to the first point to close the polygon. */
    if ((xPoints[i] != xPoints[0]) || (yPoints[i] != yPoints[0]))
    {
        polygon[i].x1 = xPoints[i];
        polygon[i].y1 = yPoints[i];
        polygon[i].x2 = xPoints[0];
        polygon[i].y2 = yPoints[0];
        numSegments++;
    }

    /* Sort the polygon */
    sortPolygon(polygon, numSegments, &bottomY);

    /* Compute the bounding rectangle for the polygon */
    for (i=0; i < numPoints; i++)
    {
        if (xPoints[i] < minX)
            minX = xPoints[i];
        if (yPoints[i] < minY)
            minY = yPoints[i];
        if (xPoints[i] > maxX)
            maxX = xPoints[i];
        if (yPoints[i] > maxY)
            maxY = yPoints[i];
    }

	rect.x = minX;
	rect.y = minY;
	rect.w = maxX - minX;
	rect.h = maxY - minY;

	/* Only draw the poly if we are in the clipping area! */
	if(dfb_clip_rectangle(&scs->state->clip, &rect))
	{
		int startY, stopY;

		/* Compute the y-coord to start and stop the drawing */
		/* Don't draw clipped sections! */

		if(minY < scs->state->clip.y1)
		{
			startY = scs->state->clip.y1;
		} else
		{
			startY = minY;
		}

		if(maxY > scs->state->clip.y2)
		{
			stopY = scs->state->clip.y2;
		} else
		{
			stopY = maxY;
		}

		/* Draw the polygon by drawing horizontal lines that fill the bounding rectangle.
		   Each horizontal line is clipped to the polygon. */
		scs->drawPixel2 = scs->drawPixel;
		scs->drawHorizLine2 = scs->drawHorizLine;
		scs->polygon = polygon;
		scs->numSegments = numSegments;
		scs->xIntersects = xIntersects;
		scs->bottomY = bottomY;
		for (y=startY; y <= stopY; y++)
			drawSegmentClippedToPolygon(scs, minX, maxX, y);
	}

    /* Deallocate any dynamic structures */
    if (numPoints > MAX_STATIC_POLYGON_SEGMENTS)
    {
        /* This frees both structures since a single allocation was made */
        DFBFREE(polygon);
    }
}
