// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END

#ifndef __GST_SECTIONSINK_H__
#define __GST_SECTIONSINK_H__

#include <gst/gst.h>
#include <gst/base/gstbasesink.h>

G_BEGIN_DECLS

/* #defines don't like whitespacey bits */
#define GST_TYPE_SECTIONSINK \
    (gst_sectionsink_get_type())
#define GST_SECTIONSINK(obj)                                            \
    (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_SECTIONSINK,GstSectionSink))
#define GST_SECTIONSINK_CLASS(klass)                                    \
    (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_SECTIONSINK,GstSectionSinkClass))
#define GST_IS_SECTIONSINK(obj)                                 \
    (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_SECTIONSINK))
#define GST_IS_SECTIONSINK_CLASS(klass)                     \
    (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_SECTIONSINK))

typedef struct _GstSectionSink GstSectionSink;
typedef struct _GstSectionSinkClass GstSectionSinkClass;

typedef enum _SS_DATA_FORMAT
{
    SS_DF_SECTIONS = 0, SS_DF_MATCHED_SECTIONS, SS_DF_INVALID
} SS_DATA_FORMAT;

struct _GstSectionSink
{
    GstBaseSink element;

    GstPad *sinkpad;
    SS_DATA_FORMAT data_format;
};

struct _GstSectionSinkClass
{
    GstBaseSinkClass parent_class;

};

GType gst_sectionsink_get_type(void);

G_END_DECLS

#endif /* __GST_SECTIONSINK_H__ */
