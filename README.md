# Create Mod Roller & Plough Fix

A NeoForge 1.21.1 addon for Create Aeronautics. It lets Create's Mechanical
Roller and Mechanical Plough act on blocks in the main world while mounted on
Sable simulated contraptions.

## Features

- Mechanical Ploughs act on the projected main-world position as the simulated
  contraption moves.
- Mechanical Rollers pave and clear the projected main world while respecting
  the contraption's current orientation, including inverted contraptions.
- On a simulated contraption, the Roller Mode named **Simulated Behavior**
  enables the projected roller behavior. It is otherwise labelled **Replace
  Tracks** and retains normal Create behavior.
- The simulated roller uses paving blocks from inventories on the same
  simulated contraption.
- Ordinary Create rollers keep their native **Clear Blocks and Pave** behavior
  with Aeronautics/Sable installed, including replacing existing roadbed rather
  than only filling empty spaces.

## Requirements

- Minecraft 1.21.1 with NeoForge
- Create 6.0.10 or newer
- Sable 2.x
- Create Aeronautics 1.3.0 or newer

## Building

Run `gradle build` (or add a Gradle wrapper and run `gradlew build`). The mod
JAR is written to `build/libs`.
