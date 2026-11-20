# Angle Builder

A Minecraft mod that shows players exactly which blocks form a straight line between two points at any angle, so they can build diagonals, slopes and angled walls.

## Language

**Anchor**:
One of the two points a player marks to define a Line.
_Avoid_: corner, endpoint, marker, pos1/pos2

**Line**:
The exact set of blocks connecting two Anchors, at any angle and in any direction. A Line is always thin: one block per step along its longest axis, so neighbouring blocks may touch only at an edge or a corner.
_Avoid_: path, ray, diagonal, box

**Measurement**:
The numbers describing a Line, such as its length, its angles and how many blocks it contains.
_Avoid_: stats, info
