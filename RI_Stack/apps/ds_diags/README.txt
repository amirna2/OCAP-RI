------------------------------
  To build and run DSDiags
------------------------------
cd $OCAPROOT/apps/ds_diags
ant clean purge build
cp *.properties $OCAPROOT/bin/$OCAPTC/env/.
cd $PLATFORMROOT
runRI.sh

------------------------------
To Navigate in DSDiags
------------------------------
The steps above will result in an xlet launching on the RI which display information about video output ports.

The left hand side of the display identifies the host, the number of HScreen's associated with the host, and information about the default HScreen and it's associated devices (graphics, background, video).  In addition, a list of all of the VideoOutputPorts associated with the host is display along with information about each.  The key for these informational strings follows.

R(resolution), AR(aspect-ration), 
   S(enabled/disabled status), Cf(num-cfgs), DynCf(dynamically configurable?), 
   DispCn(display-connected?), ContProt(content-protected?)

In addition, the current main VideoOutputPort is displayed in red.  The main video output port can be changed by hitting the up/down arrow on the remote control.  The state (enabled/disabled) of the current main VideoOutputPort can be toggled by hitting the select button.

The left side of the screen displays additional configuration information about the current main VideoOutputPort (highlighted in red on the left side of the screen).  This includes a numbered list of the configurations supported by this VideoOutputPort.  The currently active configuration is displayed in white, and can be changed by hitting a digit key 0-2 (and perhaps more in the future) on the remote control.



