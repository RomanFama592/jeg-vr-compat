# Changelog

## Unreleased

- Reloading by hand: the magazine (or the shotgun shell) you grab from the ammo box is now in your free hand until
  it goes into the gun, taken from your inventory. Part of the "Reload box and glows" option.

## 0.2.0 (2026-11-10)

- Reloading by hand: JEG's ammo box now sits at the hip while you hold a gun. Its lid opens when you can grab ammo,
  and a glow with rippling rings rises from it (blue, green with the hand in it). While holding a magazine, another
  glow marks the gun's magazine well (white, green on contact). The free hand's grip or the inspect key now puts the
  magazine back, or stops the reload it started.
- Two-handed guns show where the free hand can hold them (precision mode). The first two times you hold one for
  three seconds, a ghost of your free hand leaves it and goes to the grip, twice. After that, soft particles mark
  the grip when the free hand comes close (for the first five grabs of each session; then they only burst when
  grabbing and letting go), and it buzzes while it is there until it grabs, with a firm buzz when it does. New
  options "Two-handed grip hint" (particles and buzz, buzz only, off) and "Grip hint range" (35 cm by default). The
  grip comes from each gun's model; `/jegvr grip capture` takes it from your own hand for guns where it looks wrong.
- While holding a gun with both hands (precision mode), Vivecraft's hotbar (touching a slot, the hotbar buttons,
  reaching over the shoulder) and the swap-hands key do nothing, so the grip is not ended by accident. New option
  "Lock hotbar while gripping with both hands" (on by default).

## 0.1.1 (2026-11-09)

- The stun grenade's flash and the smoke now cover what each eye sees in VR, instead of only the HUD panel.
  New option "Stun flash in the eyes": white (default, as in JEG), grey, black (easier on the eyes) or off.
- The mod's options screen now uses two columns, so it fits in VR.

## 0.1.0 (2026-11-09)

First public alpha, for Just Enough Guns 0.13.2 and Vivecraft 1.20.1-1.2.5 on Forge 1.20.1.
