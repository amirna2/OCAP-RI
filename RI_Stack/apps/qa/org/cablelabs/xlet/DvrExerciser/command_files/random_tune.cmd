# Continuously performs a random tuning operation for a specified period of time.
# The first parameter to the randomTune command is the 'dwell time', or the amount of
# time (in seconds) that is allowed for each tune to complete.
# The second parameter is the total 'run time' (in seconds) for the test to complete.

testName random_tune

# Randomly tune to a service and dwell there for 5 seconds, and continue to do so for 300 sec
randomTune	5	300



