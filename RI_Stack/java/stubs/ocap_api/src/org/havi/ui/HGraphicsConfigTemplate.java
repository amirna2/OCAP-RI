package org.havi.ui;

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

/**
   The HGraphicsConfigTemplate class is used to obtain a valid {@link
   org.havi.ui.HGraphicsConfiguration}. An
   application instantiates one of these objects and then sets all
   non-default attributes as desired. The {@link
   org.havi.ui.HGraphicsDevice#getBestConfiguration} method found in the
   {@link org.havi.ui.HGraphicsDevice} class is then
   called with this {@link org.havi.ui.HGraphicsConfigTemplate}
   . A valid {@link
   org.havi.ui.HGraphicsConfiguration} is
   returned that meets or exceeds what was requested in the {@link
   org.havi.ui.HGraphicsConfigTemplate}.  

   <p>
   This class may be subclassed to support additional
   properties of graphics configurations which may be requested by applications.

  <hr>
  The parameters to the constructors are as follows, in cases where
  parameters are not used, then the constructor should use the default
  values.
  <p>
  <h3>Default parameter values exposed in the constructors</h3>
  <table border>
  <tr><th>Parameter</th><th>Description</th><th>Default value</th> 
  <th>Set method</th><th>Get method</th></tr>
  <tr><td colspan=5>None.</td></tr>
  </table>
  <h3>Default parameter values not exposed in the constructors</h3>
  <table border>
  <tr><th>Description</th><th>Default value</th><th>Set method</th>
  <th>Get method</th></tr>
  <tr><td colspan=4>None.</td></tr>
  </table>

   @see HScreenConfigTemplate
   @see HBackgroundConfigTemplate
   @see HVideoConfigTemplate
 */

public class HGraphicsConfigTemplate
    extends HScreenConfigTemplate
{    

    /**
     * A value for use in the preference field of the {@link
     * org.havi.ui.HScreenConfigTemplate#setPreference setPreference}
     * and {@link
     * org.havi.ui.HScreenConfigTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link
     * org.havi.ui.HGraphicsConfigTemplate HGraphicsConfigTemplate}
     * that indicates that the graphics configuration should or shall
     * support transparency in the graphics system such that the
     * output of a video decoder is visible. This includes the
     * following configurations :-
     * <ul>
     * <li>Configurations where there is a well defined transformation
     * between video pixels and graphics pixels (e.g. pixels are the
     * same size).
     * <li>Configurations where an application displays graphics over
     * video but where the video is considered as a background and
     * hence no transformation between the two sets of pixels is
     * required.
     * </ul>     
     * <p>
     * Applications may specify a particular video configuration with
     * which mixing must be supported. In this case, the video
     * configuration is specified as an {@link
     * org.havi.ui.HVideoConfiguration HVideoConfiguration} object. If
     * no specific video configuration is required then it is not
     * required to specify such a configuration and null can be used.
     * <p>
     * This preference is used by the platform as a constraint in
     * selecting configurations. Templates generated by the platform
     * and then returned to applications (e.g. from a
     * <code>getConfigTemplate</code> method) shall not have this
     * preference filled in by the platform.    
     */
    public static final int VIDEO_MIXING          = 0x0C;

    /**
     * A value for use in the preference field of the {@link
     * org.havi.ui.HScreenConfigTemplate#setPreference setPreference}
     * and {@link
     * org.havi.ui.HScreenConfigTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link
     * org.havi.ui.HGraphicsConfigTemplate HGraphicsConfigTemplate}
     * that indicates that the graphics configuration should or shall
     * support the HAVi mattes feature.  
     */
    public static final int MATTE_SUPPORT         = 0x0D;

    /**
     * A value for use in the preference field of the {@link
     * org.havi.ui.HScreenConfigTemplate#setPreference setPreference}
     * and {@link
     * org.havi.ui.HScreenConfigTemplate#getPreferencePriority
     * getPreferencePriority} methods in the {@link
     * org.havi.ui.HGraphicsConfigTemplate HGraphicsConfigTemplate}
     * that indicates that the graphics configuration should or shall
     * support rapid (hardware) image scaling.  
     */
    public static final int IMAGE_SCALING_SUPPORT = 0x0E;

    
    /**
     * Creates an {@link org.havi.ui.HGraphicsConfigTemplate}
     * object.  See the class description for
     * details of constructor parameters and default values.  
     */
    public HGraphicsConfigTemplate()
    {
    }

    /**
     * Returns a boolean indicating whether or not the specified
     * {@link org.havi.ui.HGraphicsConfiguration} can
     * be used to create a graphics plane that supports the features set
     * in this template.
     *
     * @param hgc - the {@link org.havi.ui.HGraphicsConfiguration}
     * object to test against this template.
     * @return true if this {@link org.havi.ui.HGraphicsConfiguration}
     * object can be used to create a graphics
     * plane that supports the features set in this template, false
     * otherwise.
     */
    public boolean isConfigSupported(HGraphicsConfiguration hgc)
    {
        return (true);
    }

    /**
     * Set the indicated preference to have the specified priority. If
     * the preference has been previously set, then the previous
     * priority for the preference shall be overwritten.
     * <p>
     * Attributes that are not filled in in a template (through {@link
     * org.havi.ui.HScreenConfigTemplate#setPreference}),
     * shall have the priority {@link
     * org.havi.ui.HScreenConfigTemplate#DONT_CARE}. Any
     * configuration always satisfies these attributes.
     *
     * @param preference the preference to be indicated. Valid values
     * for an {@link org.havi.ui.HGraphicsConfigTemplate} are:
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#INTERLACED_DISPLAY},
     * {@link org.havi.ui.HScreenConfigTemplate#FLICKER_FILTERING},
     * {@link org.havi.ui.HGraphicsConfigTemplate#MATTE_SUPPORT} and
     * {@link org.havi.ui.HGraphicsConfigTemplate#IMAGE_SCALING_SUPPORT}.
     * <p>
     * Subclasses may add further valid values. An
     * IllegalArgumentException shall be thrown if the preference is
     * not a valid value for this instance of {@link
     * org.havi.ui.HGraphicsConfigTemplate}
     * @param priority the priority of the preference. Valid values include:
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED},
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED},
     * {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE},
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED_NOT} 
     * and
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT}.  
     * <p>
     * If <code>priority</code> is not a valid priority as defined here a 
     * java.lang.IllegalArgumentException will be thrown. 
     */
    public void setPreference(int preference, int priority)
    {
    }

    /**
     * Return the priority for the specified preference.
     * <p>
     * By default the preferences in a template returned from the
     * system will have a {@link
     * org.havi.ui.HScreenConfigTemplate#DONT_CARE}
     * priority unless specified otherwise.  Any configuration always
     * satisfies these attributes.
     *
     * @param preference the preference to be indicated. Valid values
     * for an {@link org.havi.ui.HGraphicsConfigTemplate} are:
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_BACKGROUND_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_GRAPHICS_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#ZERO_VIDEO_IMPACT},
     * {@link org.havi.ui.HScreenConfigTemplate#INTERLACED_DISPLAY},
     * {@link org.havi.ui.HScreenConfigTemplate#FLICKER_FILTERING},
     * {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED},
     * {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO},
     * {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     * {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE},
     * {@link org.havi.ui.HGraphicsConfigTemplate#VIDEO_MIXING},
     * {@link org.havi.ui.HGraphicsConfigTemplate#MATTE_SUPPORT} and
     * {@link org.havi.ui.HGraphicsConfigTemplate#IMAGE_SCALING_SUPPORT}.
     * <p>
     * Subclasses may add further valid values. An
     * IllegalArgumentException shall be thrown if the preference is
     * not a valid value for this instance of {@link
     * org.havi.ui.HGraphicsConfigTemplate}
     * @return the priority for the specified preference.
     */
    public int getPreferencePriority(int preference)
    {
        return (REQUIRED);
    }


    
    /**
     * Set the indicated preference (and associated value object) to
     * have the specified priority. If the preference has been
     * previously set, then the previous object and priority shall be
     * overwritten.
     * <p>
     * Attributes that are not filled in in a template (through {@link
     * org.havi.ui.HGraphicsConfigTemplate#setPreference}),
     * shall have the priority {@link
     * org.havi.ui.HScreenConfigTemplate#DONT_CARE}. Any
     * configuration always satisfies these attributes.
     * <p>
     * An application which wishes to remove a preference from an
     * existing template (e.g. one generated by the platform) may call
     * this method with <code>null</code> for the object
     * parameter. Calling this method with <code>null</code> for the
     * object parameter shall have no effect if
     * the preference is not currently set in the template.
     *
     * @param preference the preference to be indicated. Valid values
     * for an {@link org.havi.ui.HScreenConfigTemplate} are:
     * {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO},
     * {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     * {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE},
     * {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED}
     * and {@link org.havi.ui.HGraphicsConfigTemplate#VIDEO_MIXING}.     
     * <p>
     * Subclasses may add further valid values. An
     * IllegalArgumentException shall be thrown if the preference is
     * not a valid value for this instance of {@link
     * org.havi.ui.HScreenConfigTemplate}
     * @param object the Object associated with the given preference,
     * or <code>null</code>.
     * @param priority the priority of the preference. Valid values include:
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED},
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED},
     * {@link org.havi.ui.HScreenConfigTemplate#DONT_CARE},
     * {@link org.havi.ui.HScreenConfigTemplate#PREFERRED_NOT} and
     * {@link org.havi.ui.HScreenConfigTemplate#REQUIRED_NOT}.  
     * <p>
     * If <code>priority</code> is not a valid priority as defined here a 
     * java.lang.IllegalArgumentException will be thrown.  
     */
    public void setPreference(int preference, Object object, int priority)
    {
    }

    /**
     * Return the preference object for the specified preference.
     * <p>
     * Instances of {@link org.havi.ui.HGraphicsConfigTemplate}
     * which have not had this preference set
     * shall return null for this object. Note that instances
     * constructed by the platform and returned to applications are
     * required to have all preferences (except where explicitly
     * identified) set by the platform before it is returned.
     *
     * @param preference the preference to be indicated. Valid values
     * for an {@link org.havi.ui.HGraphicsConfigTemplate} are:
     * {@link org.havi.ui.HScreenConfigTemplate#VIDEO_GRAPHICS_PIXEL_ALIGNED},
     * {@link org.havi.ui.HScreenConfigTemplate#PIXEL_ASPECT_RATIO},
     * {@link org.havi.ui.HScreenConfigTemplate#PIXEL_RESOLUTION},
     * {@link org.havi.ui.HScreenConfigTemplate#SCREEN_RECTANGLE},
     * and
     * {@link org.havi.ui.HGraphicsConfigTemplate#VIDEO_MIXING}.          
     * <p>
     * Subclasses may add further valid values. A
     * IllegalArgumentException shall be thrown if the preference is
     * not a valid value for this instance of {@link
     * org.havi.ui.HGraphicsConfigTemplate}, or
     * if <code>preference</code> does not have an associated value object.
     * @return the preference object for the specified preference.  
     */
    public Object getPreferenceObject(int preference)
    {
        return (null);
    }    
    
    


}

