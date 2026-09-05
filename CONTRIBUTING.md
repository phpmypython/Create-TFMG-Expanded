# Contributing

Contributions are welcome — issues and pull requests alike. A change is judged by whether it is correct,
minimal, verified in game, and explained well enough that a reviewer can follow the reasoning without you in
the room.

## What a good contribution looks like

* **Root cause, not symptoms.** Say what was actually wrong — which code path, which state, why — and how the
  change fixes it. Decompiled bytecode, world/region data and a reproduction on a dedicated server are all
  fair game and very welcome as evidence.
* **Verified in game.** State what you tested and where (client, dedicated server, or both). "It compiles" is
  not verification.
* **Minimal and self-contained.** One fix per pull request, the smallest correct change, no drive-by refactors.
* **World-safe.** Keep the mod id (`tfmg`), block/item/fluid ids, data components and NBT formats compatible
  with existing worlds unless the change is explicitly about migrating them.
* **Explained in the commit.** The commit message should let a reviewer follow the reasoning without you in
  the room.

## Licence

Contributions to this repository are accepted under the GNU General Public License, version 2 (`GPL-2.0-only`);
see [LICENCE.md](LICENCE.md).
