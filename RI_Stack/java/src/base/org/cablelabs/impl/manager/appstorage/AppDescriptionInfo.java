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

package org.cablelabs.impl.manager.appstorage;

import java.io.File;
import java.io.Serializable;

/**
 * Describes a complete set of application files that are to be placed in
 * persistent storage
 * 
 */
public class AppDescriptionInfo implements Serializable
{
    /**
     * The list of files that need to be stored on the host device. Objects may
     * be <code>DirInfo</code> or <code>FileInfo</code> instances.
     */
    public FileInfo[] files;
    
    /**
     * The Application Description File from which this information was attained.
     * May be null if the information was attained by parsing hashfiles used to
     * sign the application
     */
    public File appDescriptionFile;

    /**
     * A node in the directory tree indicating a file.
     */
    public class FileInfo implements Serializable
    {
        /**
         * The name of a file system object (directory or file) that is
         * storable. This is the name of the object within its enclosing
         * directory and hence does not include any directory path information.
         * When this name is the wild card "*", it implies that all objects of
         * this type in this directory MUST be stored and, in the case of a
         * directory included through a wild card, that all objects in the
         * hierarchy under this directory MUST be stored.
         */
        public String name;

        /**
         * The file's size. Undefined and ignored for instances of
         * <code>DirInfo</code>. Undefined and ignored if <code>name=="*"</code>
         * .
         */
        public long size;
    }

    /**
     * A node in the directory tree indicating a directory.
     */
    public class DirInfo extends FileInfo implements Serializable
    {
        /**
         * The list of files contained within this directory.
         */
        public FileInfo[] files;
    }

}
