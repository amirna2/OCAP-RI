This directory contains some detailed diagrams from the RI Training Session from June 8-10th, 2010.

00.BasicDecode.jpg: Illustration showing the MPE calls the RI makes to perform a basic live decode.

01.TSMClassDiagram.jpg: TimeShiftManager UML class diagram

03a.TSMStateMachine.jpg: The TimeShiftWindow state machine diagram

04.Seq.SCInitiatedLive.jpg: Sequence diagram showing how a ServiceContext-initiated live presentation interacts with TimeShiftManager to perform a tune with timeshift buffering disabled.

07.Seq.SCInitiatedBuffering.jpg: Sequence diagram showing how a ServiceContext-initiated live presentation interacts with TimeShiftManager to perform a tune with timeshift buffering enabled.

09.Seq.BufReqOppStart.jpg: Sequence diagram showing how RecordingManager BufferingRequests interact with TimeShiftManager to perform a tune with timeshift buffering enabled.

13a.TSBPIDChangeSupported.jpg: Diagram illustrating how a PID change for a broadcast Service is handled by the RI stack when the platform supports on-the-fly ID changes for TSB recording sessions.

13b.Seq.TSBPIDChangeSupported.jpg: Sequence diagram showing TimeShiftManager & MPE interaction for a broadcast Service PID change when the platform supports on-the-fly ID changes for TSB recording sessions.

14a.TSBPIDChangeNotSupported.jpg: Diagram illustrating how a PID change for a broadcast Service is handled by the RI stack when the platform does not support on-the-fly ID changes for TSB recording sessions.

14b.Seq.TSBPIDChangeNotSupported.jpg: Sequence diagram showing TimeShiftManager & MPE interaction for a broadcast Service PID change when the platform does not support on-the-fly ID changes for TSB recording sessions.

16.Seq.SDVRemap.jpg: Sequence diagram showing TimeShiftManager, ServiceContext, and RecordingManager interaction when an SPI/SDV service remap is performed.

17.TSBPIDMapTable.jpg: Diagram illustrating the use of the PIDMapTable by TimeShiftManager.

19.LiveToTimeShift.jpg: Diagram illustrating how live and timeshift playback sessions are started/stopped by the JMF timeshift Player when performing playback rate changes.

30.OCAPDVRAPIDiagram.jpg: Diagram showing the interaction of the various OCAP RecordingManager methods and data structures.

31.DVRMPEAPIDiagram.jpg: Diagram showing the interaction of the various MPE RecordingManager methods and data structures.

36.BasicRecord.jpg: Diagram illustrating how the MPE method calls are made to perform a basic scheduled recording.

37.AdjRecord.jpg: Diagram illustrating how the MPE method calls are made to perform 2 adjacent scheduled recording.

38.RetroRecord.jpg: Diagram illustrating how the MPE method calls are made to perform a partially-retroactive instant recording.