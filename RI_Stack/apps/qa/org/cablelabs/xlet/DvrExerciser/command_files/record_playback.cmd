# Make a 1-minute recording, tune away from the original service, and play it back

testName record_playback

# delete all recordings
deleteAllRecordings

# make a 60 second recording
recordAndWait	60

# play (current) recording using the '4' key
vkNumber 4

# let it play for 60 seconds
wait 60
