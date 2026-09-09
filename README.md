# 寄り道 yorimichi — Detour

Press once and get somewhere to walk to, picked out of a scatter of random points around
where you are standing. Built for the [Mudita Kompakt](https://mudita.com/products/kompakt/)
and its E Ink screen.

*Yorimichi* is 寄り道 — the stop you make on the way, the turning you take that was not on
the route. Which is the whole of it: a place nothing in your day would otherwise have taken
you to.

Not a fork. Written from scratch in Kotlin and Jetpack Compose, using Mudita's own
[MMD](https://github.com/mudita/MMD) design system so it looks like the apps the phone
already ships with.

| | |
|---|---|
| ![Somewhere to go](screenshots/1-go.png) | ![A walk, and how much to trust it](screenshots/2-walk.png) |
| ![Three settings](screenshots/3-settings.png) | ![What it does and does not claim](screenshots/4-about.png) |

## What it does

- Scatters a thousand points evenly over the disc around you, estimates how thickly they
  fell across it, and sends you to the thickest patch — or the thinnest, if you would rather
  have the empty part.
- Draws an arrow, a distance and the coordinate, and nothing else. **There is no map in this
  app and there will not be one.** A greyscale basemap that pans and zooms is the worst thing
  this panel can be asked to draw, and the phone already has an offline map that does it
  properly. The coordinate is handed to that one with a `geo:` intent.
- Says how thick the patch actually was, against what an even scatter would have given. Most
  runs come back somewhere ordinary, and it says so rather than dressing the answer up.
- Says how good the fix under it is, and how old, because a walk scattered around where the
  phone was twenty minutes ago is a different walk.
- Walks of ¼ mile to 5 miles, or 500 m to 10 km. Two words of settings otherwise.

## What it does not claim

The numbers come from the phone's own generator, seeded from the kernel's entropy pool.
They are not quantum, nothing is fetched from a server, and no claim is made that an
intention held while pressing the button reaches the arithmetic. The apps this one is
descended from make that claim; the [Fatum papers](https://github.com/anonyhoney/fatum-en)
are worth reading, and it is worth being clear that this app implements their method and
not their premise.

What the method does do is genuinely useful: it breaks the habit of going where you already
go. That does not need a physics claim behind it.

## Permissions

One: `ACCESS_FINE_LOCATION`, to know where to scatter the points around. There is no
`INTERNET` permission, so nothing can leave the phone even by accident, and nothing about
where you are or where you were sent is written down.

## Where it came from

Three earlier projects implement the same idea, and this one owes them the method:

- [OpenRando](https://github.com/realjck/OpenRando) (MIT) — the clearest short statement of
  the algorithm.
- [pyrandonaut](https://github.com/openrandonaut/pyrandonaut) (GPL-3.0-or-later) — the same
  in Python, against scipy's own kernel density estimate.
- [LibreRandonaut](https://github.com/librerandonaut/librerandonaut) (GPL-3.0-only) — the
  Android one, and where the idea of testing an attractor rather than just reporting it
  comes from.

None of their code is in here. Three things are done differently: the scatter and the
density estimate happen in metres rather than degrees, because a kernel that is round in
degrees is an ellipse on the ground; the Gaussian kernel is separated per axis, which is one
exponential per cell per axis instead of one per pair; and the answer carries a measure of
how concentrated it really was.

## Licence

GNU General Public License v3.0 only. Copyright wander wildwood.

Icons are from [Material Symbols](https://fonts.google.com/icons), Apache 2.0.
