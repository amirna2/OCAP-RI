# Make various tests related to the TSB

testName	tsbTest


# wait a little bit to allow for the TSB to acquire some content
wait	30

# start play backwards and...
setPlayRate	-1.0

# ...allow enough time to reach the beginning of the content
wait 40

# When the beginning of content has been reached, the stack should
#	start playing at a rate of 1.0
testPlayRate	1.0

# Now pause live content
setPlayRate	0.0

# Allow 10 seconds to allow the system to stabilize
wait 10

# Disable the TSB
enableTsb	false

# Give the system a little time to stabilize
wait 10

# The stack is expected to restore playback at a normal rate of 1.0
testPlayRate	1.0

# Re-enable the TSB
enableTsb	true

# allow 10 seconds to allow system to stabilize
wait	10

# We should now be able to pause live play
setPlayRate	0.0

# allow 10 seconds to allow system to stabilize
wait 10

# Test that the play rate is still paused
testPlayRate	0.0

# Wait a little while
wait	30

# Start playing from the TSB
setPlayRate	1.0