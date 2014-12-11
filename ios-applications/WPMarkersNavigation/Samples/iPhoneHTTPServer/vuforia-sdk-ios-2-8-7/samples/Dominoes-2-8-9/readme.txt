/*==============================================================================
Copyright (c) 2010-2013 QUALCOMM Austria Research Center GmbH .
All Rights Reserved.

==============================================================================*/

The Dominoes sample application covers a number of topics:

 - Handling touch events
 - Transforming screen points to target points
 - Picking objects in a scene by tapping on the screen
 - Playing sound clips
 - Adding custom UI elements over the AR view
 - Adding, removing, and modifying virtual buttons


Application User Guide:

1) Print the stones target for tracking.

2) Run the application, and point the device at the target.  A message should appear when the target first comes into view.

3) Draw your finger across the screen to draw a line of dominoes.  You can also hold your finger on the screen and move the device.  Go slowly to get a nicely spaced set!

4) You can tap on a domino to select it.  Swiping back and forth pivots the domino in place (helpful for those tight turns).  A delete button will also appear for removing the domino.

5) When you are happy with your setup, hit the run button to start the simulation.

6) Hit the reset button to stop the simulation and set the dominos upright (much easier than in real the real world!)

7) Hit the clear button to remove all the dominoes for a fresh start.

8) See the green glow under the first domino?  It's a hint, you can touch that domino in the real world to tip it over.  This uses a virtual button.


Known issues:

 - Virtual buttons will not work if the first domino is off the target or too close to the edge of the target.
