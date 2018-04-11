package org.havi.ui.event;

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

import java.awt.event.FocusListener;

/**
   The {@link org.havi.ui.event.HFocusListener HFocusListener}
   interface enables the reception of {@link
   org.havi.ui.event.HFocusEvent HFocusEvent} events, as generated by
   objects implementing {@link org.havi.ui.HNavigable HNavigable}.
    
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
   
*/

public interface HFocusListener 
    extends java.awt.event.FocusListener
{
 
}
