# Better Jukebox

A variety of jukebox tweaks to make them behave like background music instead of as block sounds.

# Features

- Music disc sounds are now non-directional, meaning they play in both ears equally regardless of where you're facing
- Automatically pauses background music while an active jukebox is within range, so the two don't conflict
	- The background music gradually fades in & out, and pauses fully when the volume fully fades out
- Music discs now continue playing in the pause menu just like background music does
- Jukeboxes now play at full volume from 0 to 50 blocks, fully fading out at 100 blocks

# Goals

- Add config options for jukebox distance & ticks to fade in/out

# Known Issues

- Small HashMap memory leak in stored coordinates, not exactly clear yet what's the best way to resolve, but probably insignificant

# Project Cleanup

- Remove unnecessary template files (e.g. server/main)
- Is the main client needed?
