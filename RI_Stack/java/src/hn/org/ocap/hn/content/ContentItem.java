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

/*
 * Created on 07-Oct-2004
 *
 */
package org.ocap.hn.content;

import java.io.IOException;

import javax.tv.service.Service;

/**
 * This class represents a piece of content. This can be audio, video or still
 * image content. It is not directly linked to any file. This is done via the
 * ContentResources. A ContentItem can have multiple ContentResources.
 */
public interface ContentItem extends ContentEntry
{
    /**
     * Represents the base content item.
     */
    public static final String ITEM = "object.item";

    /**
     * Represents the base audio content.
     */
    public static final String AUDIO_ITEM = "object.item.audioItem";

    /**
     * In addition to being an AUDIO_ITEM content MAY be a track such as a song.
     */
    public static final String AUDIO_ITEM_TRACK = "object.item.audioItem.musicTrack";

    /**
     * In addition to being an AUDIO_ITEM content MAY be broadcast on a radio
     * station.
     */
    public static final String AUDIO_ITEM_BROADCAST = "object.item.audioItem.audioBroadcast";

    /**
     * In addition to being an AUDIO_ITEM content MAY be an audio book.
     */
    public static final String AUDIO_ITEM_BOOK = "object.item.audioItem.audioBook";

    /**
     * Represents the base video item.
     */
    public static final String VIDEO_ITEM = "object.item.videoItem";

    /**
     * In addition to being a VIDEO_ITEM content MAY be a movie.
     */
    public static final String VIDEO_ITEM_MOVIE = "object.item.videoItem.movie";

    /**
     * In addition to being a VIDEO_ITEM content MAY be a video broadcast.
     */
    public static final String VIDEO_ITEM_BROADCAST = "object.item.videoItem.videoBroadcast";

    /**
     * In addition to being a VIDEO_ITEM_BROADCAST content MAY be VOD content.
     * For Broadcast VOD content the implementation SHOULD NOT support random access or trick modes.
     */
    public static final String VIDEO_ITEM_BROADCAST_VOD = "object.item.videoItem.videoBroadcast.vod";
    
    /**
     * In addition to being a VIDEO_ITEM content MAY be a music video clip, e.g.
     * music video.
     */
    public static final String VIDEO_ITEM_MUSIC_CLIP = "object.item.videoItem.musicVideoClip";

    /**
     * In addition to being a VIDEO_ITEM content MAY be a VPOP item;
     * the item represents content binary that is presenting to video output 
     * ports, e.g. HDMI, component.
     */
    public static final String VIDEO_ITEM_VPOP = "object.item.videoItem.vpop";

    /**
     * Base image item.
     */
    public static final String IMAGE_ITEM = "object.item.imageItem";

    /**
     * In addition to being an IMAGE_ITEM content MAY be a photo.
     */
    public static final String IMAGE_ITEM_PHOTO = "object.item.imageItem.photo";

    /**
     * Returns a boolean indicating if this content has audio.
     * 
     * @return True if the content type has audio, otherwise returns false.
     */
    public boolean hasAudio();

    /**
     * Returns a boolean indicating if the ContentItem has video associated with
     * it.
     * 
     * @return True if the ContentItem contains video, otherwise returns false.
     */
    public boolean hasVideo();

    /**
     * Returns a boolean indicating if the ContentItem has a still image.
     * 
     * @return True if the ContentItem has a still image, otherwise returns
     *         false.
     */
    public boolean hasStillImage();

    /**
     * If this ContentItem is presentable as a JavaTV Service than this method
     * returns a javax.tv.service.Service, or derivative of a Service, e.g.
     * RecordedService, which can be used to play this ContentItem. If the
     * ContentItem is not local the returned Service SHALL be a RemoteService.
     * If the content associated with item has been deleted or is no longer
     * accessable, this method SHALL return null.
     * 
     * @return A JavaTV service if this content is presentable as a Service,
     *         null otherwise.
     */
    public Service getItemService();

    /**
     * Returns the content class of this content item.
     * 
     * @see #AUDIO_ITEM
     * @see #AUDIO_ITEM_BOOK
     * @see #AUDIO_ITEM_BROADCAST
     * @see #AUDIO_ITEM_TRACK
     * @see #IMAGE_ITEM
     * @see #VIDEO_ITEM
     * @see #VIDEO_ITEM_BROADCAST
     * @see #VIDEO_ITEM_BROADCAST_VOD
     * @see #VIDEO_ITEM_MOVIE
     * @see #VIDEO_ITEM_MUSIC_CLIP
     * @see #VIDEO_ITEM_VPOP
     * @see #IMAGE_ITEM_PHOTO
     * 
     * @return The content class of this item.
     */
    public String getContentClass();

    /**
     * Gets the title for this ContentItem, or null if the title is unknown.
     * 
     * @return the String title for this item, or null if unknown.
     */
    public String getTitle();

    /**
     * Deletes this ContentItem. Calls the {@link ContentResource#delete} method
     * on each ContentResource contained in this ContentItem. Deletes a local
     * ContentItem only. If the #isLocal method returns false an exception is
     * thrown.
     * <p>
     * Note: this overrides the definition of {@link ContentEntry#deleteEntry}.
     * If an application calls the ContentEntry.deleteEntry method on an object
     * that is an instance of ContentItem, the implementation SHALL delete the
     * ContentItem as defined by this method.
     * </p>
     * @return True if the ContentItem was deleted, otherwise returns false.
     *
     * @throws java.lang.SecurityException
     *             if the application does not have write
     *             ExtendedFileAccessPermission.
     * @throws IOException
     *             if the ContentItem is not local.
     */
    public boolean deleteEntry() throws IOException;

    /**
     * Returns the number of {@link ContentResource}s which are associated with
     * this ContentItem.
     * 
     * @return number of ContentResources.
     */
    public int getResourceCount();

    /**
     * Returns the n<sup>th</sup> {@link ContentResource} of this ContentItem.
     * 
     * @param n
     *            the index of the ContentResource
     * 
     * @return the n<sup>th</sup ContentResource
     * 
     * @throws ArrayIndexOutOfBoundsException
     *             if the n<sup>th</sup> value does not exist.
     */
    public ContentResource getResource(int n);

    /**
     * Returns the index of the specified ContentResource or -1 if the
     * ContentResource does not exist in this ContentItem.
     * 
     * @param r
     *            The ContentResource to check for.
     *
     * @return The index of the ContentResource or -1 if it doesn't exist in this
     *         ContentItem.
     */
    public int getResourceIndex(ContentResource r);

    /**
     * Checks whether the given {@link ContentResource} is part of this
     * ContentItem..
     *
     * @param entry
     *            The ContentResource to check for.
     *
     * @return True if the ContentResource is part of this ContentItem,
     *         otherwise returns false.
     */
    public boolean containsResource(ContentResource entry);

    /**
     * Gets an array copy of ContentResources which are part of this
     * ContentItem.
     *
     * @return Array of ContentResources.
     */
    public ContentResource[] getResources();

    /**
     * Gets an array copy of renderable ContentResources which are part of this
     * ContentItem.
     *
     * @return Array of ContentResources contained in this ContentItem for for
     *         which ContentResource.isRenderable() returns true.
     */
    public ContentResource[] getRenderableResources();

    /**
     * Checks whether the local device has the capabilities to render this
     * content item. This includes the ability to negotiate media protocol with
     * the host device, the ability of the local device to render this
     * content item's media format, and sufficient access permissions
     * for the calling application.
     *
     * This method will return true if any of the ContentResources contained in
     * this ContentItem are renderable.
     *
     * This call does not consider immediate availability of resources required
     * for presentation of this content.
     *
     * @return true if this content is renderable on the local device, false
     *         otherwise.
     */
    public boolean isRenderable();
}
