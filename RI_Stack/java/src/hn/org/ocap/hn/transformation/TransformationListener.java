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

package org.ocap.hn.transformation;

import java.util.EventListener;
import org.ocap.hn.content.ContentItem;

/**
 * Listener interface for classes interested in getting notifications from the
 * <code>TransformationManager</code>. Only one of the notify callbacks will be
 * received for each (<code>ContentItem</code>, <code>Transformation</code>)
 * tuple.
 * 
 */
public interface TransformationListener extends EventListener {
    
    /**
     * ReasonCode: Transformation was not successful due to unknown reason(s).
     */
    public static final int REASON_UNKNOWN = 0;
    
    /**
     * ReasonCode: Some resource was not available to create the transformation.
     */
    public static final int REASON_RESOURCE_UNAVAILABLE = 1;
    
    /**
     * ReasonCode: The specific content item for which the transformation was requested 
     * has been deleted.
     */
    public static final int REASON_CONTENTITEM_DELETED = 2;
       
    /**
     * ReasonCode: The content item native format isn't compatible with the requested 
     * transformation's input content profile.
     */
    public static final int REASON_NONMATCHING_INPUT_PROFILE = 3;
       
    /**
     * Callback indicating the <code>ContentResource</code> for the 
     * transformation has been created.
     * 
     * @param contentItem affected contentItem
     * @param transformation requested transformation on contentItem
     */
    void notifyTransformationReady(ContentItem contentItem, 
                                Transformation transformation);
    
    /**
     * Callback indicating the content binary representation for the 
     * transformation could not be created. 
     * 
     * @param contentItem affected contentItem
     * @param transformation requested transformation on contentItem
     * @param reasonCode reason for the failure
     */
    void notifyTransformationFailed(ContentItem contentItem, 
                                Transformation transformation, int reasonCode);
    
}
