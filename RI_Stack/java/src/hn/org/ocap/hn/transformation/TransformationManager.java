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

import org.cablelabs.impl.ocap.hn.transformation.TransformationManagerImpl;
import org.ocap.hn.content.ContentItem;

/**
 * This class is a singleton manager that can be used to get transformation
 * capabilities of the Host device and to manage the content item transformation
 * configuration. Transformation capabilities indicate the transformations from
 * input content format to output content format that the device supports.
 */
public abstract class TransformationManager
{    
    /**
     * Constructor protected from erroneous application access.
     */
    protected TransformationManager()
    {
    }

    /**
     * Gets an instance of the TransformationManager.
     * 
     * @throws SecurityException if the calling application has not been granted
     *      HomeNetPermission("contentmanagement").
     */
    public static TransformationManager getInstance()
    {
        return TransformationManagerImpl.getInstance();
    }
    
    /**
     * Adds a <code>TransformationListener</code> to receive callbacks from the
     * <code>TransformationManager</code>. The <code>TransformationListener</code>
     * will be notified whenever transformations are applied for the
     * <code>ContentItem</code>. Subsequent calls to register the same 
     * listener will be ignored.
     * 
     * @param listener The listener that will receive the callbacks
     * @throws IllegalArgumentException if the listener parameter is null.
     */
    public abstract void addTransformationListener(TransformationListener listener);
    
    /**
     * Removes the specified TransformationListener. If the listener specified is not registered
     * or is null, then this method has no effect.
     * 
     * @param listener The listener to remove
     */
    public abstract void removeTransformationListener(TransformationListener listener);

    /**
     * Gets all of the transformation permutations the Host device supports.
     * See [OC-BUNDLE] for additional mapping of this method.
     * 
     * @return Device supported transformations.  If the device does not support
     *      transformations an empty array is returned.
     */
    public abstract Transformation [] getSupportedTransformations();
        
    /**
     * Sets the default transformations. Default transformations will be applied
     * to newly-created ContentItems only and certain calls to 
     * <code>TransformationManager</code>. A call to setDefaultTransformation 
     * over-rides any previously-set default transformations and passing an
     * empty array disables any previously-set default transformations. 
     * See [OC-BUNDLE] for additional mapping of this method.
     * 
     * @param transformations The new default transformations.
     * 
     * @throws IllegalArgumentException if the transformations parameter is null.
     * 
     * @returns The default transformations.
     */
    public abstract Transformation [] setDefaultTransformations(Transformation [] transformations);

    /**
     * Returns the currently-set default transformation.
     * 
     * @returns The default transformations.
     */
    public abstract Transformation [] getDefaultTransformations();
    
    /**
     * Returns the applied transformations for the content item.
     * 
     * @param item The content item.
     * 
     * @throws IllegalArgumentException if the item parameter is null.
     * 
     * @returns The applied transformations
     */
    public abstract Transformation [] getTransformations(ContentItem item);
        
    /**
     * Applies the transformations to all existing local content items that
     * represent network operator content. A call to this method will remove
     * any existing transformations before setting the transformations. 
     * See [OC-BUNDLE] for additional mapping of this method.
     * 
     * @param transformations The array of transformations to be applied. 
     *        
     * @throws IllegalArgumentException if the transformations parameter is null.
     */
    public abstract void setTransformations(Transformation [] transformations);
    
    /**
     * Applies the default transformations for a set of content items. Configures 
     * metadata indicating content transformation support for each 
     * <code>ContentItem</code> in the items array parameter. If a content item
     * in the array parameter is not local or does not represent MSO local 
     * content it is skipped without change or notification. A call to this
     * method will remove any existing transformations before setting the 
     * transformations. 
     * See [OC-BUNDLE] for additional mapping of this method.
     * 
     * @param items The array of content items the transformation metadata 
     *              will be configured in.
     * 
     * @throws IllegalArgumentException if the parameter is null or empty.
     */
    public abstract void setTransformations(ContentItem [] items);  
    
    /**
     * Applies specific transformations for a set of content items. Configures 
     * metadata indicating content transformation support that matches any
     * of the transformations in the transformations array parameter for each
     * content item in the content item array parameter. If a ContentItem in
     * the array parameter is not local or does not represent MSO local 
     * content it is skipped without change or notification. A call to this 
     * method will remove any existing transformations before setting the 
     * transformations.
     * See [OC-BUNDLE] for additional mapping of this method.
     * 
     * @param items The array of content items the transformation metadata will
     *              be configured in.
     * @param transformations The array of transformations to apply. 
     * 
     * @throws IllegalArgumentException if items parameter is null or empty
     * or the transformations parameter is null.
     */
    public abstract void setTransformations(ContentItem [] items,    
                                            Transformation [] transformations);
}
