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

#ifndef _OCAP_EXTENSIONS_H
#define _OCAP_EXTENSIONS_H

#include <directfb.h>
#include <core/coretypes.h>

/* Constants */
#define MAX_STATIC_POLYGON_SEGMENTS 16

/* Types */
typedef DFBRegion DFBSegment;

/* Scan convert state information */
typedef struct _SCSTATE {
	CardState *state;
	void (*drawPixel)(struct _SCSTATE *scs, int x, int y);
	void (*drawHorizLine)(struct _SCSTATE *scs, int x1, int x2, int y);
	void (*drawPixel2)(struct _SCSTATE *scs, int x, int y);
	void (*drawHorizLine2)(struct _SCSTATE *scs, int x1, int x2, int y);
    DFBSegment *polygon;
    int numSegments;
    int *xIntersects;
    int bottomY;
} SCSTATE;

/* Prototypes for functions in oval.c */
void gOval(SCSTATE *scs, DFBRectangle *rect, DFBBoolean fill);
void gRoundRect(SCSTATE *scs, DFBRectangle *rect, DFBDimension *oval, DFBBoolean fill);

/* Prototypes for functions in arc.c */
void gArc(SCSTATE *scs, DFBRectangle *rect, int startAngle, int arcAngle, DFBBoolean fill);

/* Prototypes for functions in polygon.c */
void sortPolygon(DFBSegment *polygon, int numSegments, int *bottomY);
void drawSegmentClippedToPolygon(SCSTATE *scs, int x1, int x2, int y);
void drawPixelClippedToPolygon(SCSTATE *scs, int x, int y);
void gFillPolygon(SCSTATE *scs, int *xPoints, int *yPoints, int numPoints);

#endif /* _OCAP_EXTENSIONS_H */
