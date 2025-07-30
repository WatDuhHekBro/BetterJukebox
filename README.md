# Stereo Jukebox

A quick and dirty fix to make music disc sounds global like the "music" and "ambient" sound categories. Also pauses background music while music discs are playing.

"Stereo Jukebox" -> Makes jukeboxes play in both ears instead of just one (depending on direction).
- Direction should have no effect on which ear jukebox music plays in.
- Make volume depend exactly on distance from jukebox? Configurable
- Make music fade out when near a jukebox?
- Maybe instead replace the pause menu with the jukebox music so it's less jarring?

# Known Issues

- Random audio glitches as the music automatically tries to unpause itself (because music must be continuously paused while any music disc sounds exist)
- Some inefficient code that might possibly slow down your game (whoops)

# Project Cleanup / Goals

- Remove unnecessary template files (e.g. server/main)
- Refactor comments & structure of mixins
- Is the main client needed?
- See how other mods compile for multiple versions, possibly multiple NeoForge + Fabric versions in one JAR?
