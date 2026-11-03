# Changelog

## 1.2.3

- Redesigned the tab: the view switcher is one compact row instead of a 2×2
  block of large buttons, and the open view is highlighted.
- The player, chat and server lists now follow the vanilla statistics list:
  flat background, small grey header, thin separator and subtle row highlight.
- Settings are a readable single column: the label on the left, the current
  value in a button on the right.
- New mod icon and matching gallery banner.

## 1.2.2

- Fixed the mod buttons not responding to clicks. They now inherit the vanilla
  button input handling; only the drawing is custom.
- Buttons keep their normal height instead of being stretched by the statistics
  screen layout, so they no longer overlap the list.
- The default theme keeps the vanilla button look; other themes restyle buttons
  as before.

## 1.2.1

- Chat tab shows kill pairs only; chat totem rows and the related setting were
  removed. Chat parsing now ignores totem messages entirely.
- The default theme now matches the vanilla statistics screen.
- The other themes restyle all mod widgets, including buttons.
- Existing theme choices are migrated to the new theme list.
- Statistics are written at most once every 10 seconds to reduce disk I/O.

## 1.2.0

- Merged the Players tab: kills/deaths and totems popped/lost in one table.
- Merged the chat views into a single Chat tab.
- Added optional Mod Menu integration.
- Added the mod icon.
- Section switcher is a 2×2 grid: Players, Chat, Servers, Settings.
- List rows support an optional second line for totem counters.

## 1.1.2

- Renamed the mod from Player Stats to BodyCount.
- Changed the mod id to `bodycount`, the package to `com.bodycount`, the mixin
  file to `bodycount.mixins.json` and the assets to `assets/bodycount`.
- Settings moved to `config/bodycount.json`; `config/playerstats.json` is read
  when the new file is absent.
- Statistics remain in `config/totems.txt`.

## 1.1.1

- Moved all statistics to the plain-text file `config/totems.txt`.
- Settings are stored separately in `config/playerstats.json`.
- Statistics from an old `playerstats.json` are imported automatically.
- Fixed the reversed counter in chat totem pairs.
- Added protection against a corrupted data file.

## 1.1.0

- Added chat kill and totem pair views.
- Added a settings screen: tracking options, theme, sorting, item icons,
  compact rows, coloured values, hiding empty rows and statistics reset.
- Added four themes: Dark, Light, Neon and Amber.
- Added sorting by activity, name, kills and totems.
- Added tooltips with counters and K/D.
- A server profile no longer switches by itself when another one is pinned.
- Fixed renaming a profile in singleplayer.
- Merging profiles now merges chat statistics as well.

## 1.0.0

- First release.
- Fourth tab in the statistics screen: totems and kills/deaths.
- Server profile manager.
