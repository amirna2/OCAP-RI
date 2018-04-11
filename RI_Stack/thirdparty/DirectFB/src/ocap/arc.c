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

/* Enable for debugging */
#if 0
void gDrawLine(DFBRegion *line);
#endif

/**
 * Array of tan() values from 0 degrees to 45 degrees inclusive. Each value is multiplied
 * by 2^16 to bring them into the range of a 16 bit integer.
 */
static int tanArray[] =
{
	    0,  1144,  2289,  3435,  4583,  5734,  6888,  8047,  9210, 10380,
	11556, 12739, 13930, 15130, 16340, 17560, 18792, 20036, 21294, 22566,
	23853, 25157, 26478, 27818, 29179, 30560, 31964, 33392, 34846, 36327,
	37837, 39378, 40951, 42560, 44205, 45889, 47615, 49385, 51202, 53070,
	54991, 56970, 59009, 61113, 63287, 65536
};

/**
 * Return the point where the specified ray intersects the specified rectangle.
 * The origin of the ray is assumed to be centered within the rectangle.
 *
 * @param rect the bounding rectangle
 * @param angle the angle of the ray (0 degrees is at 3 o'clock and increases
 *      in a counter-clockwise direction.
 * @return point is updated with the coordinates where the ray intersects the
 *      rectangle. side is updated with a value to indicate which side of the
 *      rectangle is intersected by the ray (0=right, 1=top, 2=left, 3=bottom).
 */
static
void rayIntersectRect(DFBRectangle *rect, int angle, DFBPoint *point, int *side)
{
    /* Compute top-left, bottom-right and center coordinates */
    int x1 = rect->x;
    int y1 = rect->y;
    int x2 = rect->x + rect->w - 1;
    int y2 = rect->y + rect->h - 1;
    int xc = rect->x + rect->w / 2;
    int yc = rect->y + rect->h / 2;

	/* Handle each region */
	switch ((angle % 360) / 45) {
	case 0:
		/*   0 to  44 degrees */
		point->x = x2;
		point->y = yc -
				   ((unsigned int)(rect->h * (65536 - tanArray[45-angle])) >> 17);
		*side = 0;
		break;
	case 1:
		/*  45 to  89 degrees */
		point->x = x2 -
				   ((unsigned int)(rect->w * (65536 - tanArray[90-angle])) >> 17);
		point->y = y1;
		*side = 1;
		break;
	case 2:
		/*  90 to 134 degrees */
		point->x = xc -
				   ((unsigned int)(rect->w * (65536 - tanArray[135-angle])) >> 17);
		point->y = y1;
		*side = 1;
		break;
	case 3:
		/* 135 to 179 degrees */
		point->x = x1;
		point->y = y1 +
				   ((unsigned int)(rect->h * (65536 - tanArray[180-angle])) >> 17);
		*side = 2;
		break;
	case 4:
		/* 180 to 224 degrees */
		point->x = x1;
		point->y = yc +
				   ((unsigned int)(rect->h * (65536 - tanArray[225-angle])) >> 17);
		*side = 2;
		break;
	case 5:
		/* 225 to 269 degrees */
		point->x = x1 +
				   ((unsigned int)(rect->w * (65536 - tanArray[270-angle])) >> 17);
		point->y = y2;
		*side = 3;
		break;
	case 6:
		/* 270 to 314 degrees */
		point->x = xc +
				   ((unsigned int)(rect->w * (65536 - tanArray[315-angle])) >> 17);
		point->y = y2;
		*side = 3;
		break;
	case 7:
		/* 315 to 359 degrees */
		point->x = x2;
		point->y = y2 -
				   ((unsigned int)(rect->h * (65536 - tanArray[360-angle])) >> 17);
		*side = 0;
		break;
	}
}


/**
 * Construct a polygon that outlines the boundary of the arc to be drawn. This
 * polygon is made up of the original bounding rectangle with the portion
 * bisected by the start and end angles removed. The portion removed is that
 * intersected by a ray moving from the start angle to the end angle in a
 * clockwise fashion.
 *
 * At most this polygon will contain 7 segments; 5 from the bounding rectangle
 * and 2 from the start and end angles. The list of segments returned in
 * <code>polygon</code> is not sorted or in any particular order.
 * 
 * @param rect the bounding rectangle for the defining oval
 * @param startAngle the start angle in degrees
 * @param endAngle the end angle in degrees
 * @return polygon is updated with the segments that make up the bounding
 *      polygon for the arc. The array passed to this function must be large
 *      enough to hold the maximum number of segments (7). numSegments is
 *      updated to indicate the number of segments in polygon.
 */
static
void constructArcPolygon(DFBRectangle *rect, int startAngle, int endAngle,
						 DFBSegment *polygon, int *numSegments)
{

	DFBPoint startPoint, endPoint;
	int startSide, endSide;
	int included[4] = {0,0,0,0};
	int n = 0;
    int x1 = rect->x;
    int x2 = rect->x + rect->w - 1;
    int y1 = rect->y;
    int y2 = rect->y + rect->h - 1;
    int i;
    int adjStartAngle, adjEndAngle;

	/* Get points on bounding rectangle for starting and ending angles */
	rayIntersectRect(rect, startAngle, &startPoint, &startSide);
	rayIntersectRect(rect, endAngle, &endPoint, &endSide);

	/* Start angle (from center to perimeter) */
	polygon[n].x1 = rect->x + (rect->w / 2);
	polygon[n].y1 = rect->y + (rect->h / 2);
	polygon[n].x2 = startPoint.x;
	polygon[n].y2 = startPoint.y;
	n++;

	/* End angle (from center to perimeter) */
	polygon[n].x1 = rect->x + (rect->w / 2);
	polygon[n].y1 = rect->y + (rect->h / 2);
	polygon[n].x2 = endPoint.x;
	polygon[n].y2 = endPoint.y;
	n++;

    /* Compute adjusted start and end angles. These angles are rotated 45 degrees
       counter-clockwise so that 0 degrees is in the upper-right corner. This is
       done so that each side is a linear range so that we can compare start and
       end angles. This is not possible with un-adjusted angles since the right
       side contains two dis-contiguous ranges (0 to 44 degrees and 315 to 349
       degrees). */
    adjStartAngle = (startAngle + 45) % 360;
    adjEndAngle   = (endAngle   + 45) % 360;

	/* Create an array that marks each side to be included in the boundary
	   of the arc. If the side is included then its entry in the array will
	   be non-zero. */
    if (startSide == endSide)
    {
        /* Either just this side or all 4 sides */
        if (adjStartAngle < adjEndAngle)
            included[startSide] = 1;
        else
            included[0] = included[1] = included[2] = included[3] = 1;
    }
    else
    {
        i = startSide;
        while (1) {
            included[i] = 1;
            if (i == endSide)
                break;
            else
                i = (i+1)%4;
        }
    }

	/* Handle right side */
	if (included[0]) {
		if (startSide == 0) {
			if (endSide == 0) {
				/* Starts and ends on this side */
				if (adjStartAngle < adjEndAngle) {
                    /* Single segment in middle of side */
                    polygon[n].x1 = x2;
                    polygon[n].y1 = startPoint.y;
                    polygon[n].x2 = x2;
                    polygon[n].y2 = endPoint.y;
                    n++;
				} else {
					/* Two segments at ends of side */
                    polygon[n].x1 = x2;
                    polygon[n].y1 = startPoint.y;
                    polygon[n].x2 = x2;
                    polygon[n].y2 = y1;
                    n++;
                    polygon[n].x1 = x2;
                    polygon[n].y1 = endPoint.y;
                    polygon[n].x2 = x2;
                    polygon[n].y2 = y2;
                    n++;
				}
			} else {
				/* Starts on this side and ends on another */
				polygon[n].x1 = x2;
				polygon[n].y1 = startPoint.y;
				polygon[n].x2 = x2;
				polygon[n].y2 = y1;
				n++;
			}
		} else if (endSide == 0) {
			/* Ends on this side but starts on another */
			polygon[n].x1 = x2;
			polygon[n].y1 = endPoint.y;
			polygon[n].x2 = x2;
			polygon[n].y2 = y2;
			n++;
 		} else {
			/* Include this entire side */
			polygon[n].x1 = x2;
			polygon[n].y1 = y1;
			polygon[n].x2 = x2;
			polygon[n].y2 = y2;
			n++;
		}
	}

	/* Handle left side */
	if (included[2]) {
		if (startSide == 2) {
			if (endSide == 2) {
				/* Starts and ends on this side */
				if (adjStartAngle < adjEndAngle) {
					/* Single segment in middle of side */
					polygon[n].x1 = x1;
					polygon[n].y1 = startPoint.y;
					polygon[n].x2 = x1;
					polygon[n].y2 = endPoint.y;
					n++;
				} else {
					/* Two segments at ends of side */
					polygon[n].x1 = x1;
					polygon[n].y1 = startPoint.y;
					polygon[n].x2 = x1;
					polygon[n].y2 = y2;
					n++;
					polygon[n].x1 = x1;
					polygon[n].y1 = endPoint.y;
					polygon[n].x2 = x1;
					polygon[n].y2 = y1;
					n++;
				}
			} else {
				/* Starts on this side and ends on another */
				polygon[n].x1 = x1;
				polygon[n].y1 = startPoint.y;
				polygon[n].x2 = x1;
				polygon[n].y2 = y2;
				n++;
			}
		} else if (endSide == 2) {
			/* Ends on this side but starts on another */
			polygon[n].x1 = x1;
			polygon[n].y1 = endPoint.y;
			polygon[n].x2 = x1;
			polygon[n].y2 = y1;
			n++;
 		} else {
			/* Include this entire side */
			polygon[n].x1 = x1;
			polygon[n].y1 = y1;
			polygon[n].x2 = x1;
			polygon[n].y2 = y2;
			n++;
		}
	}

	/* Handle top side */
	if (included[1]) {
		if (startSide == 1) {
			if (endSide == 1) {
				/* Starts and ends on this side */
				if (adjStartAngle < adjEndAngle) {
					/* Single segment in middle of side */
					polygon[n].x1 = startPoint.x;
					polygon[n].y1 = y1;
					polygon[n].x2 = endPoint.x;
					polygon[n].y2 = y1;
					n++;
				} else {
					/* Two segments at ends of side */
					polygon[n].x1 = startPoint.x;
					polygon[n].y1 = y1;
					polygon[n].x2 = x1;
					polygon[n].y2 = y1;
					n++;
					polygon[n].x1 = endPoint.x;
					polygon[n].y1 = y1;
					polygon[n].x2 = x2;
					polygon[n].y2 = y1;
					n++;
				}
			} else {
				/* Starts on this side and ends on another */
				polygon[n].x1 = startPoint.x;
				polygon[n].y1 = y1;
				polygon[n].x2 = x1;
				polygon[n].y2 = y1;
				n++;
			}
		} else if (endSide == 1) {
			/* Ends on this side but starts on another */
			polygon[n].x1 = endPoint.x;
			polygon[n].y1 = y1;
			polygon[n].x2 = x2;
			polygon[n].y2 = y1;
			n++;
 		} else {
			/* Include this entire side */
			polygon[n].x1 = x1;
			polygon[n].y1 = y1;
			polygon[n].x2 = x2;
			polygon[n].y2 = y1;
			n++;
		}
	}

	/* Handle bottom side */
	if (included[3]) {
		if (startSide == 3) {
			if (endSide == 3) {
				/* Starts and ends on this side */
				if (adjStartAngle < adjEndAngle) {
					/* Single segment in middle of side */
					polygon[n].x1 = startPoint.x;
					polygon[n].y1 = y2;
					polygon[n].x2 = endPoint.x;
					polygon[n].y2 = y2;
					n++;
				} else {
					/* Two segments at ends of side */
					polygon[n].x1 = startPoint.x;
					polygon[n].y1 = y2;
					polygon[n].x2 = x2;
					polygon[n].y2 = y2;
					n++;
					polygon[n].x1 = endPoint.x;
					polygon[n].y1 = y2;
					polygon[n].x2 = x1;
					polygon[n].y2 = y2;
					n++;
				}
			} else {
				/* Starts on this side and ends on another */
				polygon[n].x1 = startPoint.x;
				polygon[n].y1 = y2;
				polygon[n].x2 = x2;
				polygon[n].y2 = y2;
				n++;
			}
		} else if (endSide == 3) {
			/* Ends on this side but starts on another */
			polygon[n].x1 = endPoint.x;
			polygon[n].y1 = y2;
			polygon[n].x2 = x1;
			polygon[n].y2 = y2;
			n++;
 		} else {
			/* Include this entire side */
			polygon[n].x1 = x1;
			polygon[n].y1 = y2;
			polygon[n].x2 = x2;
			polygon[n].y2 = y2;
			n++;
		}
	}

    /* Return number of segments */
    *numSegments = n;
}

/**
 * Draw an outlined or filled arc whose defining oval would exactly fill the specified
 * rectangle. The portion of the oval drawn starts at startAngle and is arcAngle degrees
 * in size. The angle 0 degrees is at 3 o'clock and the arc sweeps in a counter-clockwise
 * direction for positive angles and in a clockwise direction for negative angles.
 * 
 * @param rect the bounding rectangle for the defining oval
 * @param startAngle the start angle in degrees
 * @param arcAngle the size of the arc in degrees
 * @param fill a filled arc is drawn if TRUE - otherwise, an outlined arc is drawn
 */
void gArc(SCSTATE *scs, DFBRectangle *rect, int startAngle, int arcAngle, DFBBoolean fill)
{
	DFBSegment polygon[7];
    int xIntersects[7];
    int numSegments, bottomY;
    int endAngle;

    /* If arcAngle is 0 then draw nothing */
    if (arcAngle == 0)
        return;

	/* If there is nothing to do, then simply return. */
	if ( !dfb_clip_check( &scs->state->clip, rect) )
		return;

    /* If arcAngle is > 360 degrees then draw a complete oval */
    if (abs(arcAngle) >= 360)
    {
        startAngle = endAngle = 0;
    }
    else
    {
        /* Compute start and end angles */
        if (arcAngle > 0)
        {
            startAngle = startAngle % 360;
            endAngle = (startAngle + arcAngle) % 360;
        }
        else
        {
            endAngle = startAngle % 360;
            startAngle = (startAngle + arcAngle) % 360;
            if (startAngle < 0)
                startAngle = 360 + startAngle;
        }
    }

    /* If arc to draw is the entire oval then just draw the whole thing. Otherwise,
       compute the arc polygon so we can clip to it while drawing the oval. */
    if (startAngle != endAngle)
    {
        /* Construct a polygon that outlines the boundary of the arc and sort it */
        constructArcPolygon(rect, startAngle, endAngle, &polygon[0], &numSegments);
        sortPolygon(polygon, numSegments, &bottomY);

        /* Enable this block of code in order to draw an outline of the arc polygon
           for debugging. */
        #if 0
        {
            int i;
            DFBRegion line;
            for (i=0; i < numSegments; i++)
            {
                line.x1 = polygon[i].x1;
                line.y1 = polygon[i].y1;
                line.x2 = polygon[i].x2;
                line.y2 = polygon[i].y2;
                gDrawLine(&line);
            }
            return; // comment this out to see both the polygon outline and arc
        }
        #endif
                         
        /* Push drawPixel and drawHorizLine functions to the secondary copy and point
           the primary ones to functions that clip against the arc polygon. */
        scs->drawPixel2 = scs->drawPixel;
        scs->drawHorizLine2 = scs->drawHorizLine;
        scs->drawPixel = drawPixelClippedToPolygon;
        scs->drawHorizLine = drawSegmentClippedToPolygon;
        scs->polygon = &polygon[0];
        scs->numSegments = numSegments;
        scs->xIntersects = xIntersects;
        scs->bottomY = bottomY;
    }

    /* Scan convert the oval */
    gOval(scs, rect, fill);
}
