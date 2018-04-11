This directory contains system integration tests for Registered API
support in the form of two Xlets: a server and a client.

The server Xlet, located under the server subtree, registers an API
and then launches instances of the client Xlet to test access to that
API.  The client Xlets are provided parameters telling them what class
they should try to access and then whether that access should be expected
to succeed or fail.

The server Xlet launches three instances of the client Xlet.  The first
is expected to succeed, the second fail, and the third to succeed.

An example xait.properties is included in this directory.  It defines a
single abstract service which AUTOSTARTs a launcher.  To run the test,
select the "Api Test" button.  This will register apis and kick off the
three sub-tests.

Tests that pass will display green.  Tests that fail with display red.

Go back to the menu.  Pausing the "Api Test" Xlet will shutdown the
client xlets.  Resuming the "Api Test" will start them over.

Exiting the "Api Test" will shutdown the client xlets as well as the
server xlet.  Restarting the "Api Test" will register the same API
anew (i.e., with a newer version).
