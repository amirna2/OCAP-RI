package org.ocap.hn.service;

import org.ocap.hn.content.ChannelContentItem;

/**
 * This interface provides a handler that can be registered with
 * an implementation to provide a Locator for an otherwise
 * non-tunable ChannelContentItem, such as an SDV channel. If a
 * <code>ServiceResolutionHandler</code> is not registered then
 * the implementation fails any attempts to tune to such channels.
 * <p>
 * This interface also provides a handler that can be used by
 * the implementation to notify the application that tuning attempts with
 * the application provided locator for a broadcast
 * channel item have failed.
 * <p>
 * Note: This interface is intended to be used by applications which do not
 * provide a DVB SPI SelectionProvider for the "ocap" scheme.
 */
public interface ServiceResolutionHandler
{
    /**
     * Notifies the application of a tuning failure for a remote
     * streaming request for a ChannelContentItem.
     *
     * @param channel A ChannelContentItem
     *
     * @return If the return value is true, the implementation SHALL
     * retry tuning; if the return value is false, the implementation SHALL
     * fail the tuning request.
     */
    public boolean notifyTuneFailed(ChannelContentItem channel);

    /**
     * Requests that the application provide tuning parameters for
     * the <code>ChannelContentItem</code>. When the application is able
     * to resolve the item to a tunable channel, the application
     * calls the <code>ChannelContentItem.setTuningLocator</code> method,
     * and this method returns true.
     *
     * @param channel A ChannelContentItem
     *
     * @return true if application resolved the channel item and updated
     *  the ChannelContentItem locator, false otherwise
     */
    public boolean resolveChannelItem(ChannelContentItem channel);
}
