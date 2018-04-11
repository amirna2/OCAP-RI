This platform evaluation tool measures the time to perform various fundamental Java and AWT tasks.

1) Run the Xlet with the provided hostapp.properties singaling file.
2) Select a test to run using the remote control
3) The test output is written to stdout.  Capture the portion of the logs between these indicators and save to a new file:

---------------------- PlatEval: Start of Results ----------------------
---------------------- PlatEval: End of Results ----------------------

4) Run the PEFormatter tool to convert the hexadecimal log output to readable text:

java -jar $OCAPROOT/bin/$OCAPTC/env/apps/vm_perf_test/PEFormatter.jar <input_file> <output_file>

