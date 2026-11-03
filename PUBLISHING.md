# Publishing

Internal checklist for publishing BodyCount. Nothing here is shipped in the jar.

## Modrinth project

| Field | Value |
|---|---|
| Project type | Mod |
| Name | BodyCount |
| Slug | `bodycount` |
| Summary | Client-side statistics tab: kills/deaths, totems, chat kill pairs and server profiles. |
| Environment | Client |
| Loaders | Fabric |
| License | MIT |
| Source | https://github.com/ggglobus28-afk/player-stats |
| Issues | https://github.com/ggglobus28-afk/player-stats/issues |

Categories: **Utility**, **Game Mechanics**, **Social**.

Game versions: `1.21.11`, `26.1`, `26.1.1`, `26.1.2`, `26.2`, `26.3`.

## Description

```markdown
BodyCount is a client-side Fabric mod that adds a fourth tab to the statistics
screen.

- Kills and deaths and totems popped/lost for every player.
- Chat kill pairs: who killed whom and how many times.
- Separate statistics for each server profile.
- Five themes. The default matches the vanilla statistics screen.
- English and Russian localization.
- Optional Mod Menu integration.

The mod is client-side only and records events visible to the client. It does
not need to be installed on the server and does not use Fabric API.

**Compatibility**

| Minecraft | File | Java | Fabric Loader |
|---|---|---|---|
| 1.21.11 | `bodycount-1.2.2-mc1.21.11.jar` | 21+ | 0.16.0+ |
| 26.1 – 26.1.2 | `bodycount-1.2.2-mc26.1.jar` | 25+ | 0.19.5+ |
| 26.2 – 26.3 | `bodycount-1.2.2-mc26.2-26.3.jar` | 25+ | 0.19.5+ |

Install one jar only, matching the game version.

**Links**

- Source: https://github.com/ggglobus28-afk/player-stats
- Russian readme: https://github.com/ggglobus28-afk/player-stats/blob/main/README.ru.md

BodyCount is not an official Minecraft product and is not approved by or
associated with Mojang Studios or Microsoft.
```

## Versions

Upload three files, one per version entry:

| Version title | File | Game versions | Dependencies |
|---|---|---|---|
| `1.2.2 for 1.21.11` | `bodycount-1.2.2-mc1.21.11.jar` | 1.21.11 | Fabric Loader 0.16.0+, Java 21+ |
| `1.2.2 for 26.1` | `bodycount-1.2.2-mc26.1.jar` | 26.1, 26.1.1, 26.1.2 | Fabric Loader 0.19.5+, Java 25+ |
| `1.2.2 for 26.2–26.3` | `bodycount-1.2.2-mc26.2-26.3.jar` | 26.2, 26.3 | Fabric Loader 0.19.5+, Java 25+ |

Release channel: **Release**. Changelog: copy the `1.2.2` section from
`CHANGELOG.md`.

## Gallery

- Icon: `assets/bodycount/icon.png` (128×128).
- Large icon: `docs/gallery/icon-512.png` (512×512).
- Banner: `docs/gallery/banner.png` (1280×720).

Do not upload screenshots of a game session that was not actually captured from
this build. If real screenshots are added later, place them in
`docs/gallery/`.

## Legal checklist

- [x] MIT license in `LICENSE` and `fabric.mod.json`.
- [x] Author is `ggglobus228`.
- [x] Mojang/Microsoft disclaimer in `README.md`, `README.ru.md`, `NOTICE.md`
      and the Modrinth description.
- [x] No Minecraft assets bundled; icon is original; button sprites are read
      from the game at runtime.
- [x] Mod Menu stubs are compile-time only and excluded from jars.
- [x] No telemetry, network requests, obfuscation or downloaded code.
- [x] Old `playerstats` mod id declared under `breaks` to prevent double-load.

## Release checklist

1. Build all three projects and run `gradle build`.
2. Verify `fabric.mod.json` inside each jar: `id`, `name`, `version`,
   `minecraft`, `java`, `entrypoints`, icon, no `com/terraformersmc`.
3. Copy jars to the archive folder and update `ВЕРСИИ.txt`.
4. Commit and push to GitHub.
5. Create a GitHub release tagged `v1.2.2` and attach the three jars.
6. Upload the three files to Modrinth with the version table above.
7. Link the Modrinth page from the GitHub release and the README.
8. Do not reupload the same jar to other platforms; Modrinth rules prohibit
   unauthorized reuploads of other authors' work.

## Notes

- The GitHub repository is currently named `player-stats`. Renaming it to
  `bodycount` is recommended; GitHub redirects the old URLs, so the links above
  keep working.
- The Modrinth slug `bodycount` is free at the time of writing.
- Keep the three jars separate on Modrinth; do not attach all three to one
  version entry, otherwise the game-version filter becomes ambiguous.
