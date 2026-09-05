<div align="center">
  <h1>Create: TFMG Expanded</h1>
  <br>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/graphs/contributors"><picture><img alt="GitHub contributors" src="https://img.shields.io/github/contributors/phpmypython/Create-TFMG-Expanded"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/stargazers"><picture><img alt="Stars" src="https://img.shields.io/github/stars/phpmypython/Create-TFMG-Expanded?style=flat"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/releases/latest"><picture><img alt="Latest Release" src="https://img.shields.io/github/v/release/phpmypython/Create-TFMG-Expanded"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/releases/latest"><picture><img alt="Total GitHub Downloads" src="https://img.shields.io/github/downloads/phpmypython/Create-TFMG-Expanded/total"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/commits/"><picture><img alt="Commit activity" src="https://img.shields.io/github/commit-activity/t/phpmypython/Create-TFMG-Expanded"></picture></a>
  <br>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/issues"><picture><img alt="Open Issues" src="https://img.shields.io/github/issues-raw/phpmypython/Create-TFMG-Expanded"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/issues?q=is%3Aissue+state%3Aclosed"><picture><img alt="Closed Issues" src="https://img.shields.io/github/issues-closed-raw/phpmypython/Create-TFMG-Expanded"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/pulls"><picture><img alt="Pull Requests" src="https://img.shields.io/github/issues-pr-raw/phpmypython/Create-TFMG-Expanded"></picture></a>
  <a href="https://github.com/phpmypython/Create-TFMG-Expanded/pulls?q=is%3Apr+state%3Aclosed"><picture><img alt="Closed Pull Requests" src="https://img.shields.io/github/issues-pr-closed-raw/phpmypython/Create-TFMG-Expanded"></picture></a>
  <br>
  <a>Heavy Engineering & Oil For The Create Mod</a>
</div>

<br>

## What this is

Create: TFMG Expanded is a maintained line of [Create: The Factory Must Grow](https://github.com/DrMango14/Create-The_Factory_Must_Grow)
for NeoForge 1.21.1: TFMG's industrial metallurgy, oil processing, chemistry, engines and electricity, plus root-cause fixes
for its long-standing bugs and new industrial mechanics built the way the real industry works.

**Download:** grab the latest jar from the [Releases page](https://github.com/phpmypython/Create-TFMG-Expanded/releases/latest) — it is a drop-in replacement for any TFMG 1.2.x jar (same `tfmg` mod id, world-compatible).
It replaces TFMG in a pack and existing worlds carry over; it does not run alongside TFMG.

## What's new here

* **Sulfur recovery** from coke-oven gas — a Claus plant that turns a waste stream into sulfur.
* **Goggle readouts on every pipe** — pressure and flow on Create's own pipes and pumps as well as TFMG's.
* **Pipeline pump stations** — modular booster stations with real pressure, pipe ratings, and lines that burst when you push them too far.
* **Ponder scenes** for the new machines that say exactly what each one needs.
* **Root-cause fixes** for long-standing bugs, each with its reasoning in the commit and its verification in the pull request.

Everything planned, in progress or done is on the [project board](https://github.com/users/phpmypython/projects/1).

## Why it exists

This project began because in my Create playthrough with TFMG I hit bugs constantly, and as a software engineer my first instinct is to fix them.
So I forked the mod, put the fixes there, and the scope grew past bug fixing into a line of its own — the sulfur recovery, pipeline pump stations and pipe readouts above.

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md).

## How it's built

Bugs are diagnosed at the root (decompiled bytecode, world data, reproductions on a live dedicated server) and fixed with the
smallest correct change. New mechanics are modelled on how the real industry works and taught in-game with ponder scenes.
Every change is explained in its commit so anyone can check the reasoning.

I use Claude Code as part of that work. Every change is specified, reviewed, tested and playtested by me before it is merged;
nothing lands on the word of a tool. The account it runs on has Anthropic's "Model Improvement" privacy setting turned off,
which under Anthropic's consumer terms means these coding sessions are not used for model training
(https://www.anthropic.com/news/updates-to-our-consumer-terms). Nothing here is used for training; it is used for playing.

## Licence

The original mod was published under the MIT licence by DrMangoTea; this project was forked from it on that basis
and continues under the same licence. The notice is
reproduced in [LICENCE.md](LICENCE.md).

This project is not affiliated with or endorsed by the authors of Create: The Factory Must Grow. **Please do not report
bugs from this build to them**; open them [here](https://github.com/phpmypython/Create-TFMG-Expanded/issues) instead.

## Builds and versions

Builds are automated: every push is built by GitHub Actions (jar attached to the workflow run as an
artifact), and pushing a `v*` tag — or running the **Release** workflow from the Actions tab — builds
and publishes a [GitHub release](https://github.com/phpmypython/Create-TFMG-Expanded/releases) with the jar
and a generated changelog. Locally: `./gradlew build`; the jar lands in `build/libs/`.

Releases follow [semantic versioning](https://semver.org) on this project's own line: `MAJOR.MINOR.PATCH`,
where fixes bump the patch number and new mechanics bump the minor. The numbers are not comparable
with TFMG's: `2.0.0` here is TFMG 1.2.3 plus everything in this project. The in-game update
checker reads this repository's `updates.json`, so it only ever points at releases from here.

<br>

## Info

The original TFMG authors describe the idea behind the mod like this:

> Create is by default a steam/clockpunk mod and most addons aim to expand this part of Create and do that pretty well,
> we thought the next natural expansion would be moving on from steampunk to dieselpunk.
> We believe that create could be later used not just as a single steampunk tech mod,
> but due to its modularity and polishedness, it is a perfect base for other tech mods aiming to Create (get it) something new with it,
> essentially using it as a library.
> We wanna be the first ones to try and prove this concept.

That is the mod this project continues.

<br>

## Features

* Sulfur recovery (Claus plant)
* Pipeline pump stations
* Pipe goggle readouts
* Large Distilleries
* Realistic Electricity
* Steel Mills
* Concrete
* Electrolyzers
* Steel
* Aluminum
* Cast Iron
* Lead
* Sulfur
* OIL!!!
* Quad Potato Cannon
* Flamethrowers
* And more..
