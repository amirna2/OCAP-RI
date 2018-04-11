# Demonstrates how to use the looping functionality of the command processor
#

testName loopDemonstration

# delete all recordings
deleteAllRecordings

# perform the following steps twice
loop	loopy	2

# make a 60 second recording
recordAndWait	60

# play (current) recording using the '4' key
vkNumber 4

# let it play for 60 seconds
wait 60

# return to live mode
vkLive

# let the system settle for 10 seconds
wait	10

label	loopy
