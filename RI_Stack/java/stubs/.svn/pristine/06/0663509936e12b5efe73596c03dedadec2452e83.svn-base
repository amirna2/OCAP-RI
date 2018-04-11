
package org.ocap.ui;

import javax.tv.service.selection.ServiceContext;

import org.havi.ui.HScreen;
import org.ocap.ui.event.MultiScreenConfigurationListener;
import org.ocap.system.MonitorAppPermission;

/**
 * <p>The <code>MultiScreenConfiguration</code> interface, implemented by
 * an OCAP host platform, provides information on a discrete screen
 * configuration as well as a mechanism for monitoring changes to
 * the screen configuration.</p>
 *
 * <p>An MSM implementation SHALL support at least one multiscreen
 * configuration whose configuration type is
 * <code>SCREEN_CONFIGURATION_DISPLAY</code>.</p>
 *
 * <p>An MSM implementation SHALL support at least one multiscreen
 * configuration whose configuration type is
 * <code>SCREEN_CONFIGURATION_NON_PIP</code>.</p>
 *
 * <p>If an MSM implementation implements PIP functionality and permits
 * the presentation of a PIP screen during OCAP operation, then the MSM
 * implementation SHALL support the multiscreen configuration
 * <code>SCREEN_CONFIGURATION_PIP</code>.</p>
 *
 * <p>If an MSM implementation implements POP functionality and permits
 * the presentation of a POP screen during OCAP operation, then the MSM
 * implementation SHALL support the multiscreen configuration
 * <code>SCREEN_CONFIGURATION_POP</code>.</p>
 *
 * <p>If an MSM implementation implements PIP, POP, or OVERLAY functionality
 * in a discrete configuration not explicitly specified below by a
 * non-general multiscreen configuration, then the MSM
 * implementation SHALL support one or more multiscreen configurations
 * of configuration type <code>SCREEN_CONFIGURATION_GENERAL</code> that
 * represent each such multiscreen configuration.</p>
 *
 * @author Glenn Adams
 * @since MSM I01
 **/
public interface MultiScreenConfiguration
{
	/**
     * If a <code>MultiScreenConfiguration</code> is associated with
     * one or more display screens and is a candidate for being used as
     * a per-platform multiscreen configuration, then its
     * configuration type is <code>SCREEN_CONFIGURATION_DISPLAY</code>.
     *
     * <p>The initial default screen of a
     * <code>SCREEN_CONFIGURATION_DISPLAY</code>
     * configuration SHALL be the first screen returned by
     * <code>getScreens(SCREEN_CATEGORY_MAIN)</code>,
     * or, if there is no such categorized screen in the configuration,
     * then the first screen returned by <code>getScreens()</code>.</p>
     *
     * @since MSM I01
     **/
	public static final String SCREEN_CONFIGURATION_DISPLAY = "display";

    /**
     * If a <code>MultiScreenConfiguration</code> is associated with
     * exactly one screen whose category is
     * <code>SCREEN_CATEGORY_MAIN</code>, then its configuration type is
     * <code>SCREEN_CONFIGURATION_NON_PIP</code>.
     *
     * <p>The initial default screen of a
     * <code>SCREEN_CONFIGURATION_NON_PIP</code>
     * configuration SHALL be the first screen returned by
     * <code>getScreens(SCREEN_CATEGORY_MAIN)</code>.</p>
     *
     * <p>A <code>MultiScreenConfiguration</code> instance that is
     * categorized as having a <code>SCREEN_CONFIGURATION_NON_PIP</code>
     * configuration type SHALL NOT contain more than one display
     * screen.</p>
     *
     * @since MSM I01
     **/
    public static final String SCREEN_CONFIGURATION_NON_PIP = "non-pip";

    /**
     * If a <code>MultiScreenConfiguration</code> is (1) associated with
     * one logical screen with default z-order of zero mapped to the entire area
     * of a single display screen, and (2) associated with one or more
     * non-intersecting logical screens with default z-order of one mapped to
     * the same display screen, then its configuration type is
     * <code>SCREEN_CONFIGURATION_PIP</code>.
     *
     * <p>The initial default screen of a <code>SCREEN_CONFIGURATION_PIP</code>
     * configuration SHALL be the first screen returned by
     * <code>getScreens(SCREEN_CATEGORY_MAIN)</code>,
     * or, if there is no such categorized screen in the configuration,
     * then the first screen returned by <code>getScreens()</code>.</p>
     *
     * <p>A <code>MultiScreenConfiguration</code> instance that is
     * categorized as having a <code>SCREEN_CONFIGURATION_PIP</code>
     * configuration type SHALL NOT contain more than one display
     * screen.</p>
     *
     * @since MSM I01
     **/
    public static final String SCREEN_CONFIGURATION_PIP = "pip";

    /**
     * If a <code>MultiScreenConfiguration</code> is associated with two
     * or more non-intersecting logical screens with default z-order of zero
     * whose default display areas (in union) tile the entire area of a single display
     * screen, then its configuration type is
     * <code>SCREEN_CONFIGURATION_POP</code>.
     *
     * <p>The initial default screen of a <code>SCREEN_CONFIGURATION_POP</code>
     * configuration SHALL be the first screen returned by
     * <code>getScreens(SCREEN_CATEGORY_POP)</code>,
     * or, if there is no such categorized screen in the configuration,
     * then the first screen returned by <code>getScreens()</code>.</p>
     *
     * <p>A <code>MultiScreenConfiguration</code> instance that is
     * categorized as having a <code>SCREEN_CONFIGURATION_POP</code>
     * configuration type SHALL NOT contain more than one display
     * screen.</p>
     *
     * @since MSM I01
     **/
    public static final String SCREEN_CONFIGURATION_POP = "pop";

    /**
     * If a <code>MultiScreenConfiguration</code> cannot be categorized as
     * one of the other predefined screen configuration types, then its
     * configuration type is <code>SCREEN_CONFIGURATION_GENERAL</code>.
     *
     * <p>The initial default screen of a
     * <code>SCREEN_CONFIGURATION_GENERAL</code>
     * configuration SHALL be the first screen returned by
     * <code>getScreens(SCREEN_CATEGORY_MAIN)</code>,
     * or, if there is no such categorized screen in the configuration,
     * then the first screen returned by <code>getScreens()</code>.</p>
     *
     * <p>A <code>MultiScreenConfiguration</code> instance that is
     * categorized as having a <code>SCREEN_CONFIGURATION_GENERAL</code>
     * configuration type SHALL NOT contain more than one display
     * screen.</p>
     *
     * @since MSM I01
     **/
    public static final String SCREEN_CONFIGURATION_GENERAL = "general";

    /**
     * If an <code>HScreen</code> instance is not associated with any
     * other more specific category, then its category is
     * <code>SCREEN_CATEGORY_NONE</code>.
     *
     * <p>A <code>MultiScreenConfiguration</code> instance that is
     * categorized as having a <code>SCREEN_CONFIGURATION_NONE</code>
     * configuration type SHALL NOT contain more than one display
     * screen.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final String SCREEN_CATEGORY_NONE = "none";

    /**
     * If a display <code>HScreen</code> instance is
     * characterized as a non-main screen, then its category is
     * <code>SCREEN_CATEGORY_DISPLAY</code>. A display screen assigned this
     * category SHALL NOT be populated by an
     * <code>HBackgroundDevice</code> or an
     * <code>HVideoDevice</code>, but MAY be populated by one or more
     * <code>HGraphicsDevice</code> instances that serve as overlays.
     *
     * <p>A display <code>HScreen</code> instance that is categorized as
     * <code>SCREEN_CATEGORY_DISPLAY</code> has the exceptional property
     * that its <code>HGraphicsDevice</code> instances, if any exist,
     * SHALL overlay all logical screens mapped to the display screen.
     * This property SHALL hold even though
     * <code>MultiScreenContext.getZOrder()</code> returns <code>0</code>
     * for any display screen.</p>
     *
     * <p><em>Note:</em> The exceptional property described above is
     * intended to support (1) legacy device scenarios where a closed
     * caption overlay appears over all other content, and (2)
     * configurations where, rather than treating an overlay screen as a
     * separate logical screen, an overlay screen is considered as an
     * integral <code>HGraphicsDevice</code> of the display screen.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HBackgroundDevice
     * @see org.havi.ui.HVideoDevice
     * @see org.havi.ui.HGraphicsDevice
     **/
    public static final String SCREEN_CATEGORY_DISPLAY = "display";

    /**
     * If a display or logical <code>HScreen</code> instance is
     * characterized as a main screen, then its category is
     * <code>SCREEN_CATEGORY_MAIN</code>. A logical screen assigned this
     * category SHALL be mapped to the full area of some display screen and
     * SHALL be assigned a default z-order of 0.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final String SCREEN_CATEGORY_MAIN = "main";

    /**
     * If a logical <code>HScreen</code> instance is characterized as a
     * picture-in-picture (PIP) screen, then its category is
     * <code>SCREEN_CATEGORY_PIP</code>. A logical screen assigned this
     * category SHALL NOT be mapped to the full area of some display
     * screen; SHALL NOT be assigned a screen z-order of 0; and SHALL
     * co-exist in a configuration to which some screen is assigned the
     * category <code>SCREEN_CATEGORY_MAIN</code>.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final String SCREEN_CATEGORY_PIP = "pip";

    /**
     * If a logical <code>HScreen</code> instance is characterized as a
     * picture-outside-picture (POP) screen, then its category is
     * <code>SCREEN_CATEGORY_POP</code>. A logical screen assigned this
     * category SHALL NOT be mapped to the full area of some display
     * screen; SHALL NOT co-exist in a configuration to which some
     * screen is assigned the category <code>SCREEN_CATEGORY_MAIN</code>; and
     * SHALL co-exist in a configuration to which some other screen is
     * assigned the category <code>SCREEN_CATEGORY_POP</code>.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final String SCREEN_CATEGORY_POP = "pop";

    /**
     * If a logical <code>HScreen</code> instance is characterized as an
     * overlay screen, then its category is
     * <code>SCREEN_CATEGORY_OVERLAY</code>. A logical screen assigned this
     * category SHALL be mapped to the full area of some display screen;
     * SHALL be assigned a default z-order greater than any screen
     * associated with one of the following categories:
     * <code>SCREEN_CATEGORY_MAIN</code>, <code>SCREEN_CATEGORY_PIP</code>,
     * and <code>SCREEN_CATEGORY_POP</code>; and SHALL NOT contain a
     * background or an explicit video plane
     * (<code>HVideoDevice</code>).
     *
     * <p>Notwithstanding the above, an overlay screen MAY make use of
     * the resources of an implied video plane (<code>HVideoDevice</code>)
     * for the purpose of presenting component based video in (one of) its
     * graphics plane(s).</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final String SCREEN_CATEGORY_OVERLAY = "overlay";

    /**
     * If a logical <code>HScreen</code> instance is capable of being
     * configured (e.g., in its size, position, or z-order) such
     * that it may operate in a mode that would have suggested
     * assignment of two or more of the other screen categories, then its
     * category MAY be <code>SCREEN_CATEGORY_GENERAL</code>. A logical screen
     * assigned this category SHALL NOT be constrained in size, position,
     * z-order, or other configurable properties except to the extent
     * that the terminal device places intrinsic limitations on one (or
     * more) configurable properties.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final String SCREEN_CATEGORY_GENERAL = "general";

    /**
     * Gets the screen configuration type of this configuration. If more
     * than one non-general configuration type may apply or if the
     * configuration type is unknown or cannot be determined, then the
     * value <code>SCREEN_CONFIGURATION_GENERAL</code> SHALL be
     * returned.
     * 
     * @return A String that is either (1) an element of
     * the enumeration {
     * <code>SCREEN_CONFIGURATION_DISPLAY</code>,
     * <code>SCREEN_CONFIGURATION_NON_PIP</code>,
     * <code>SCREEN_CONFIGURATION_PIP</code>,
     * <code>SCREEN_CONFIGURATION_POP</code>,
     * <code>SCREEN_CONFIGURATION_GENERAL</code> }, or (2) a string
     * value that denotes a platform-dependent configuration type and
     * that starts with the prefix <code>"x-"</code>.
     *
     * @since MSM I01
     **/
    public String getConfigurationType();

    /**
     * Gets the set of accessible screens associated with this configuration.
     *
     * <p>The underlying resources of a given <code>HScreen</code>
     * instance returned by this method MAY be shared with an
     * <code>HScreen</code> instance included in another multiscreen
     * configuration; however, those shared resources SHALL be active in
     * no more than one multiscreen configuration at a given time.  The
     * order of entries in the returned array of <code>HScreen</code>
     * instances is implementation dependent.</p>
     *
     * <p>Given the values of any two distinct entries of the returned array,
     * <code><i>S1</i></code> and <code><i>S2</i></code>, and given the
     * singleton instance of the <code>MultiScreenManager</code>,
     * <code><i>MSM</i></code>, then the following constraints apply
     * during the interval between the time when this method returns and
     * the time when a
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING</code>
     * event is generated and dispatched:</p>
     *
     * <ul>
     * <li><code><i>MSM</i>.isEmptyScreen(<i>S1</i>)</code> SHALL be <code>false</code>;</li>
     * <li><code><i>MSM</i>.isEmptyScreen(<i>S2</i>)</code> SHALL be <code>false</code>;</li>
     * <li><code><i>MSM</i>.sameResources(<i>S1</i>,<i>S2</i>)</code> SHALL be <code>false</code>;</li>
     * </ul>
     *
     * <p>If invoked by an application that does not have
     * <code>MonitorAppPermission("multiscreen.configuration")</code> then the
     * screens of this configuration that are not associated with a
     * <code>ServiceContext</code> instance accessible by the application
     * SHALL NOT be returned; otherwise, all accessible screens of this
     * configuration SHALL be returned.</p>
     *
     * <p>Over the course of an application's lifecycle, and except as
     * constrained below, an MSM implementation MAY add screens to or
     * remove screens from a multiscreen configuration, in which case it
     * SHALL generate a
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED</code> or
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code>
     * event, respectively.</p>
     *
     * <p>A screen SHALL NOT be added to or removed from a multiscreen
     * configuration that is the currently active per-platform multiscreen
     * configuration or some currently active per-display multiscreen
     * configuration.</p>
     *
     * <p><em>Note:</em> The MSM implementation must wait until a
     * multiscreen configuration is no longer the currently active
     * multiscreen configuration in order to add or remove screens from
     * that configuration.</p>
     *
     * <p>During any time interval in which no
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED</code> or
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code>
     * event is generated, the set of returned <code>HScreen</code>
     * instances and the order of these returned instances SHALL not
     * change from the perspective of a given application.</p>
     *
     * <p>An MSM implementation SHALL not remove nor generate a
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code>
     * event that would have the effect of removing (or reporting the
     * removal) from this multiscreen configuration an
     * <code>HScreen</code> instance whose underlying resources
     * represent the same underlying resources of some non-destroyed
     * application's default <code>HScreen</code> instance.</p>
     *
     * @return A (possible empty) <code>HScreen</code> array.
     *
     * @since MSM I01
     *
     * @see MultiScreenManager
     * @see org.ocap.ui.event.MultiScreenConfigurationEvent
     * @see org.havi.ui.HScreen
     * @see org.ocap.system.MonitorAppPermission
     * @see javax.tv.service.selection.ServiceContext
     **/
    public HScreen[] getScreens();

    /**
     * Obtain all accessible screens with a given category.
     *
     * <p>This method, <code>getScreens(String)</code>, SHALL function
     * identically to the method <code>getScreens()</code> except as
     * follows:</p>
     *
     * <ul>
     * <li>If <code><i>category</i></code> is not <code>null</code>, then
     * return only those screens assigned the specified category, or, if
     * no such screen exists, then return an empty <code>HScreen</code>
     * array; otherwise, if <code><i>category</i></code> is <code>null</code>, then
     * return the same value as <code>getScreens()</code>.</li>
     * </ul>
     * 
     * @param category one of the following values: (1) <code>null</code>,
     * (2) an element of the following enumeration: {
     * <code>SCREEN_CATEGORY_NONE</code>,
     * <code>SCREEN_CATEGORY_DISPLAY</code>,
     * <code>SCREEN_CATEGORY_MAIN</code>,
     * <code>SCREEN_CATEGORY_PIP</code>,
     * <code>SCREEN_CATEGORY_POP</code>,
     * <code>SCREEN_CATEGORY_OVERLAY</code>,
     * <code>SCREEN_CATEGORY_GENERAL</code> },
     * or (3) a platform-dependent string value not pre-defined as a screen
     * category but that MAY be returned by <code>getScreenCategory(HScreen)</code>.
     *
     * @return A (possibly empty) <code>HScreen</code> array.
     *
     * @since MSM I01
     *
     **/
    public HScreen[] getScreens ( String category );

	/**
     * Determine if the set of screens associated with this
     * configuration includes an overlay screen, i.e., a screen whose
     * category is <code>SCREEN_CATEGORY_OVERLAY</code>.
     *
     * @return <code>true</code> if
     * <code>getScreens(SCREEN_CATEGORY_OVERLAY)</code> would return a
     * non-empty array; otherwise, returns <code>false</code>.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
	public boolean hasOverlayScreen();

    /**
     * Obtain the default service context association screen of this
     * configuration.
     *
     * <p>The default service context association screen of a
     * multiscreen configuration is the screen with which
     * <code>ServiceContext</code> instances are associated when the
     * configuration becomes active in case that no more specific
     * information is available to determine how to associate a
     * <code>ServiceContext</code> instance with a screen.</p>
     *
     * <p>The following constraints apply:</p>
	 *
     * <ol>
     * <li>if this multiscreen configuration is a per-platform display
     * multiscreen configuration, then the default service context
     * association screen SHALL be a screen associated with the
     * per-display multiscreen configuration of some display screen
     * associated with this multiscreen
     * configuration;</li>
     * <li>otherwise, if this multiscreen configuration is a
     * per-display multiscreen configuration, then the default service
     * context association screen SHALL be a display screen or a
     * logical screen associated with this multiscreen
     * configuration.</li>
     * </ol>
     *
     * @return an <code>HScreen</code> instance that serves as the
     * default service context screen for this configuration.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @since MSM I01
     **/
    public HScreen getDefaultServiceContextScreen() throws SecurityException;

    /**
     * Set the default service context association screen of this configuration.
     *
     * <p>The default service context association screen of a
     * multiscreen configuration is the screen with which
     * <code>ServiceContext</code> instances are associated when the
     * configuration becomes active in case that no more specific
     * information is available to determine how to associate a
     * <code>ServiceContext</code> instance with a screen.
     * 
     * @param screen an <code>HScreen</code> instance to be designated
     * as the default service context association screen for this
     * multiscreen configuration.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @throws IllegalArgumentException if the constraints specified
     * above under <code>getDefaultServiceContextScreen()</code> are not
     * satisfied.
     * 
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public void setDefaultServiceContextScreen ( HScreen screen ) throws SecurityException, IllegalArgumentException;

    /**
     * Determine if the underlying resources of a specified screen is
     * represented by an <code>HScreen</code> instance included in the
     * set of screens associated with this configuration.
     *
     * @param screen an <code>HScreen</code> instance.
     *
     * @return <code>true</code> if the underlying resources specified
     * screen is represented by an <code>HScreen</code> instance
     * included in this configuration; otherwise, <code>false</code>.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public boolean isScreenInConfiguration ( HScreen screen );

}
