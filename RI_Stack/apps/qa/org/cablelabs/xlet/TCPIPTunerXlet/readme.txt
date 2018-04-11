
TCPIPTunerXlet (TCP/IP client tuner Xlet).

Overview
--------
TCPIPTunerXlet drives OCAP-level tuning in one of four modes:

    1. normal - service-selection driven by TCP/IP tune requests
    2. test - as for the normal mode above, except no service-selection is performed
    3. canned - an Xlet argument specifies the list of service identifiers
    4. randomCanned - as for the canned mode above, except services are randomly
        selected

TCPIPTunerXlet is not intended for stress-testing inasmuch as the rate at
which services are selected is governed, in part, by receipt of corresponding
ServiceContextEvents.


Xlet Arguments
--------------
TCPIPTunerXlet currently accepts the following Xlet arguments:

dwellmsec: the number of milliseconds for which to "dwell" on the selected service

mode: the mod of operation (see Overview above)

locators: a space-separated list of valid OcapLocator URLs

portnum:  the TCP/IP listener port number at which to service TCP/IP
    tune requests (default: 8888)

tunesec: the number of seconds to wait for a tune request to be satisfied

For an example hostapp.properties entry, see the accompanying file:
   hostapp.properties.tcpiptunerxlet


Sample Log Messages
-------------------
[TCPIPTunerXlet]: Configured mode: randomCanned
[TCPIPTunerXlet]:  port number: 8888
[TCPIPTunerXlet]: tune seconds: 10
[TCPIPTunerXlet]: startXlet finished.
[TCPIPTunerXlet]: using canned service identifiers ...
[TCPIPTunerXlet]: getCannedSourceId(random) m_cannedNdx: 3
[TCPIPTunerXlet]: read canned service identifier: ocap://f=0x27290640.0xC
[TCPIPTunerXlet]: setSvcSelOkay(false)
[TCPIPTunerXlet]: setFailureType(null)
[TCPIPTunerXlet]: setSvcSelOkay(true)
[TCPIPTunerXlet]: receiveServiceContextEvent() service selection succeeded.
[TCPIPTunerXlet]: SELECT ocap://f=0x27290640.0xC SUCCEEDED

