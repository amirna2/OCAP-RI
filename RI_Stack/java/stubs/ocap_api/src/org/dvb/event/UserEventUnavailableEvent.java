package org.dvb.event;

/**
 * This event is sent to the resource status event listeners when user input 
 * events are exclusively reserved by an application.
 * <p>Each application shall receive its own instance of the <code>UserEventRepository</code>
 * object which forms the source to this event. Any changes made to that repository by
 * any one application shall not impact the instance seen by any other application.<p>
 * Any applications which have registered for shared access to any of these user
 * events shall stop receiving those user events following receipt of this event. If such
 * user events become available again, a <code>UserEventAvailableEvent</code> shall be
 * generated by the platform before any more of those user events are received by
 * applications.
 * @since MHP 1.0.2
 */

public class UserEventUnavailableEvent extends org.davic.resources.ResourceStatusEvent {
        /**
         * Constructor for the event.
         *
         * @param source a <code>UserEventRepository</code> which contains the events
         * which were exclusively reserved.
  	 * @since MHP 1.0.2
         */
        public UserEventUnavailableEvent( Object source ) { super(source); }

        /**
         * Returns a <code>UserEventRepository</code> which contains the events which
         * were exclusively reserved as passed into the constructor of the instance.
         *
         * @return a  <code>UserEventRepository</code>
  	 * @since MHP 1.0.2
         */
        public Object getSource(){return null;}
}

