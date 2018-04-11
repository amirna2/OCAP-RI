################################################################################
## IXC Registry Tests
################################################################################

These xlets are intended to test the functionality of the IxcRegistry
(org.dvb.io.ixc.IxcRegistry) including the checking of OcapIxcPermission(s)
against those requests.

TestRemote.java -- The remote interface implemented by all IXC objects bound by
the test xlets. It contains a single method which returns a string.

RemoteBinder.java -- This is a very simple Xlet that just binds a remote object
(implementing TestRemote) to a name provided in the first Xlet command line
argument.  The remote object method returns a string which is the concatenation
of the Xlet's OrgID and AppID.  For example, if the RemoteBinder Xlet's OrgID is
0x1 and its AppID is 0x4595, the remote object method will return "14595".

TestRunner.java -- This is the main test runner Xlet.  This xlet reads a test
script identifed by the Xlet's AppID and OrgID and executes the tests indicated
by the script.  Test scripts are located in the "scripts" subdirectory.  The
format of test script filenames must be:

	ixctest_[orgId]_[appID].script

where [orgID] and [appID] associate the test script with a particular TestRunner
Xlet.

PRFs -- For signed TestRunner Xlets, there may be an associated Permission
Request File (PRF).  The PRF contains additional IXC permissions based on the
requirements of the test

------------------
-- Test Scripts --
------------------

