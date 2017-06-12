# tile
Tiles exist in heirarchies. Closing a tile closes all its children. you can have a tile open another tile, which is then inserted in the display as a non-overlapping block.
 
A tile is a reagent component. With a title, a display state and some content. And a border.
 
The content of a tile can be a list of children with checkboxes indicating their state. Check the child to open it, Uncheck the child, or click close on the child's tile, and the tile closes.

Only one tile is selected at a time. Closing a tile moves the selection to the parent. Opening a tile moves the selection to the new tile.
