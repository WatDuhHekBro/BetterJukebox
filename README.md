# Stereo Jukebox

A quick and dirty fix to make music disc sounds global like the "music" and "ambient" sound categories. Also pauses background music while music discs are playing. Oh, and jukeboxes now start fading at around 20-40 blocks, all while remaining stereo. I should probably clean up this readme.

"Stereo Jukebox" -> Makes jukeboxes play in both ears instead of just one (depending on direction).
- Direction should have no effect on which ear jukebox music plays in.
- Make volume depend exactly on distance from jukebox? Configurable
- Make music fade out when near a jukebox?
- Maybe instead replace the pause menu with the jukebox music so it's less jarring? See why the "music" category is the exception
	- Debug `pauseAllExcept`, it's very clearly for music

# Known Issues

- Random audio glitches as the music automatically tries to unpause itself (because music must be continuously paused while any music disc sounds exist)
	- Specifically, when unpausing the game, it briefly shows up
- Some inefficient code that might possibly slow down your game (whoops)
- HashMap memory leak in stored coordinates, not exactly clear yet what's the best way to resolve, but probably not severe
- Music fading is pretty wonky, and sometimes doesn't stop the music properly, so direct cuts are used for now

# Project Cleanup / Goals

- Remove unnecessary template files (e.g. server/main)
- Refactor comments & structure of mixins
- Is the main client needed?
- See how other mods compile for multiple versions, possibly multiple NeoForge + Fabric versions in one JAR?
