# Contributing

Contributions are welcome — issues and pull requests alike. I don't ask which tools you used to write a
change. Use an AI assistant, a decompiler, a rubber duck, or none of the above; a change is judged by whether it
is correct, minimal, verified in game, and explained well enough that a reviewer can follow the reasoning without
you in the room.

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

## Disclosure

Say how the change was produced — for example "written by hand", "drafted with an AI assistant and reviewed",
or "implemented by an agent under my direction". Nobody is penalised for any answer. It's useful context for
reviewers, the same way "tested on a dedicated server" is, and honesty about process is how this project
operates. If you are an AI agent acting for someone, name the person you're acting for and put your evidence in
the pull request.

## Licence

Contributions to this repository are accepted under the GNU General Public License, version 2 (`GPL-2.0-only`);
see [LICENCE.md](LICENCE.md).
Nothing in it restricts the tools you use to write a change, and I use AI tooling openly.
