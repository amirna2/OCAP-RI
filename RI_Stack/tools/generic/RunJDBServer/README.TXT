This small Java program is intended to be used with the thread dumping
 mechanism in the RI stack to identify thread resource bottlenecks in the
 RI stack and xlets.

Compile RunJDBServer via: javac RunJDBServer.java

Then: RunJDBServer <port> 

Ensure that "jdb" is in your path prior to execution. 

The server can run continuously (it doesn't need to be restarted between 
 executions of the RI).

Set the following RI properties to enable dumping:

 OCAP.sys.tp.threadDumpGrowThreshold
 OCAP.sys.tp.dumpSignalPort

e.g. The following in final.properties will connect to the JDB thread dumper
     running on port 8081 every time the threadPoolMonitor determines that
     the RI ThreadPool needs to have at least 3 threads added to it.

OCAP.sys.tp.threadDumpGrowThreshold=3
OCAP.sys.tp.dumpSignalPort=8081

