# Better Jukebox

A variety of jukebox tweaks to make them behave like background music instead of as block sounds.

# Features

- Music disc sounds are now non-directional, meaning they play in both ears equally regardless of where you're facing
- Automatically pauses background music while an active jukebox is within range, so the two don't conflict
- Music discs now continue playing in the pause menu just like background music does
- Jukeboxes now play at full volume from 0 to 50 blocks, fully fading out at 100 blocks

# Goals

- Add proper volume fading when near an active jukebox instead of abruptly pausing/resuming the background music
- Add config options for jukebox distance

# Known Issues

- Small HashMap memory leak in stored coordinates, not exactly clear yet what's the best way to resolve, but probably insignificant
- Background music can be briefly heard when unpausing the game, which sounds like an audio glitch
	- This is likely because the unpausing the game tries to resume music before the injected function pauses it again, so injection into the unpause function is needed

## Music Fading

- Sometimes doesn't pause the music properly (because the volume never fades to zero) on certain songs
- Often doesn't resume to full volume

# Project Cleanup

- Remove unnecessary template files (e.g. server/main)
- Is the main client needed?
