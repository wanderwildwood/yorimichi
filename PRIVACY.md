# Privacy

Detour asks for one thing — where you are — and never sends it anywhere, because it has no
way to send anything anywhere.

That is the whole policy. The rest of this page is the evidence for it, because a privacy
policy that cannot be checked is just a promise.

## One permission

`app/src/main/AndroidManifest.xml` declares exactly one:

```
android.permission.ACCESS_FINE_LOCATION
```

It is used to know where to scatter the app's random points, which is the entire function
of the app. Coarse location is not enough: a walk can be a few hundred metres and a fix good
only to a city block would start it somewhere you are not.

There is **no `INTERNET` permission**. Without it Android will not let the app open a network
connection, so nothing it knows can leave the phone even by accident, and no promise from me
is load-bearing.

## What happens to your position

It is read from the GPS and used to work out one destination. One copy of it is kept: the
last fresh fix, written to the app's private preferences at most once a minute and overwritten
each time, so that the next time the app opens indoors, where the GPS may not find the sky, it
can start from where the phone last was and say how long ago that was. There is never more
than that one position — no track, no history, no log. Clearing the app's storage, or
uninstalling it, removes it.

When the GPS has no fresh fix the app also reads the position the phone itself last
remembered (Android's "last known location"), which never leaves the phone either.

The destination is not stored either. Close the app and the walk it gave you is forgotten;
that is why the coordinate is on the screen for you to write down if you want it.

## What is stored, and where

Three settings and one position, in the app's own private storage:

```
shared_prefs/detour.xml   →   radius, look, units, last_lat, last_lon, last_accuracy, last_time
```

How far a walk may send you, whether it aims for where the points gather or where they thin
out, whether distances are said in miles or kilometres, and the last fresh fix with its
accuracy and time. That is all of it. No history, no log of where you have been, no record of
what it has ever suggested.

`android:allowBackup="false"` is set, so not even that goes to a cloud backup.

## The one place something leaves the app

"Open in maps" hands the **destination** coordinate to whatever map application you have
installed, using a standard `geo:` intent. That app then knows the coordinate, and since the
destination is by construction within a mile or so of you, it can infer roughly where you
are.

This only happens when you press that button. If you never press it, nothing ever crosses the
boundary. If you would rather it did not, the coordinate is also printed on the screen and
you can take it wherever you like by hand.

What that map application does with the coordinate is between you and its own privacy policy,
which is unlikely to be as short as this one.

## No third-party services

No analytics, no advertising, no attribution, no crash reporting. The dependencies are
AndroidX and Jetpack Compose, Mudita's [MMD](https://github.com/mudita/MMD) design system,
and nothing else. The random numbers come from the device's own `SecureRandom` rather than
from a server, which is the other reason there is no network code: every comparable app
fetches its randomness, and fetching it tells the server where you are about to walk.

## Verifying this yourself

You do not have to take any of it on trust:

- The source is at <https://github.com/wanderwildwood/yorimichi> and each release is tagged.
- `grep -r uses-permission app/src/main/AndroidManifest.xml` returns the one line above.
- Any APK can be checked with `aapt dump permissions` or by opening it as a zip.

## Changes

If this ever stops being true, this file changes in the same commit as the code that changed
it, and the release notes will say so plainly.
