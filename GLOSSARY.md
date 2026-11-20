# Angle Builder

A Minecraft mod that shows players exactly which blocks form a straight line between two points at any angle, so they can build diagonals, slopes and angled walls.

## Language

**Anchor**:
One of the two points a player marks to define a Line.
_Avoid_: corner, endpoint, marker, pos1/pos2

**Line**:
The exact set of blocks connecting two Anchors, at any angle and in any direction. A Line is always thin: one block per step along its longest axis, so neighbouring blocks may touch only at an edge or a corner. A Line's blocks depend only on where its two Anchors are, not on which was placed first.
_Avoid_: path, ray, diagonal, box

**Measurement**:
The numbers describing a Line, such as its length, its angles and how many blocks it contains.
_Avoid_: stats, info

**Target**:
The one saved Line the player is currently looking at. Deleting acts on the Target, and its Measurement is the one shown.
_Avoid_: selected, focused, active, current

**Step**:
A point along a Line where it shifts one block on an axis other than its longest axis.
_Avoid_: jump, offset

**Run**:
A stretch of consecutive blocks along a Line's longest axis, between two Steps.
_Avoid_: segment, stretch
