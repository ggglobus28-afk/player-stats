# BodyCount

Client-side **Fabric** mod that adds a fourth tab to Minecraft's statistics screen.
It counts, per player, **kills / deaths** and **totems popped / lost**, reads kill
and totem messages from chat, keeps separate profiles for every server, and lets
you restyle everything from an in-game settings screen.

No server installation required. The mod only counts what your client can see.

---

## Возможности

- **Одна вкладка «Игроки»** — статистика по каждому игроку.
- **Вкладка «Чат»** — пары игроков из сообщений чата: кто кого убил и сколько раз,
  кто с кого снял тотем. Работает с ванильными сообщениями и типовыми форматами
  PvP-плагинов, на русском и английском.
- **Профили серверов** — статистика хранится отдельно для каждого адреса,
  список подтягивается из меню «Сетевая игра». Профили можно переименовывать,
  объединять, удалять и закреплять.
- **Настройки и темы** — 4 темы (Тёмная, Светлая, Неон, Янтарь), сортировка,
  иконки предметов, компактные строки, цветные значения, скрытие пустых строк.
- **Текстовое хранилище** — вся статистика лежит в обычном
  `config/totems.txt`, который можно открыть Блокнотом и править руками.
- **Mod Menu** — если он установлен, настройки BodyCount открываются прямо
  из списка модов. Без Mod Menu мод работает как обычно.
- Локализация: русский и английский.

## Поддерживаемые версии

| Minecraft | Jar | Java | Fabric Loader |
|---|---|---|---|
| 1.21.11 | `bodycount-*-mc1.21.11.jar` | 21+ | 0.16+ |
| 26.1 / 26.1.1 / 26.1.2 | `bodycount-*-mc26.1.jar` | 25+ | 0.19.5+ |
| 26.2 / 26.3 | `bodycount-*-mc26.2-26.3.jar` | 25+ | 0.19.5+ |

В `mods` кладите **только один** jar — под свою версию игры.
Fabric API не требуется. Mod Menu — по желанию.

## Установка

1. Установите Fabric Loader нужной версии.
2. Положите подходящий jar в папку `mods`.
3. Запустите игру: **Esc → Статистика → Игроки**.

Если раньше стоял Player Stats — удалите его jar, это тот же мод.

## Где хранятся данные

```
.minecraft/config/totems.txt        вся статистика, обычный текст
.minecraft/config/bodycount.json    только настройки (JSON)
```

Формат `totems.txt`:

```text
active=<id профиля>

[server]
id=<id>
name=<название>
address=<адрес или singleplayer>
singleplayer=<true|false>
player=UUID|НИК|СНЯЛ|СНЯЛИ|УБИЛ|УМЕР|ПОСЛЕДНИЙ_РАЗ
chatkill=A|B|A_УБИЛ_B|B_УБИЛ_A|ПОСЛЕДНИЙ_РАЗ
chattotem=A|B|A_СНЯЛ_С_B|B_СНЯЛ_С_A|ПОСЛЕДНИЙ_РАЗ
```

Поля разделены `|`, обратный слэш экранирует `\` и `|`, строки с `#` —
комментарии. Править файл нужно при закрытой игре, иначе мод перезапишет его.
Старый `playerstats.json` со статистикой переносится автоматически.

## Сборка из исходников

Репозиторий содержит три отдельных Gradle-проекта, потому что клиентские API
1.21.11, 26.1 и 26.2/26.3 различаются.

```bash
cd versions/26.2-26.3     # или versions/26.1, versions/1.21.11
./gradlew build
```

Готовый jar появится в `build/libs/`.

- `versions/1.21.11` — классический `fabric-loom`, obfuscated-маппинги Mojang.
- `versions/26.1`, `versions/26.2-26.3` — `net.fabricmc.fabric-loom` (no-remap),
  официальные имена Mojang.

Нужен JDK 21 для 1.21.11 и JDK 25 для 26.x.

## Структура репозитория

```
versions/1.21.11/     мод под Minecraft 1.21.11
versions/26.1/        мод под Minecraft 26.1.x
versions/26.2-26.3/   мод под Minecraft 26.2 и 26.3
CHANGELOG.md          история версий
LICENSE               MIT
```

Исходники общие: пакет `com.bodycount` (`data`, `ui`, `mixin`).

## Ограничения

Мод клиентский, поэтому статистика приближённая:

- учитываются только игроки, которых видит клиент;
- смерти от мобов, падения и пустоты в статистику игроков не попадают;
- сообщения чата разбираются локально регулярными выражениями — нестандартный
  формат сервера может не распознаться;
- имена мобов отфильтровываются, чтобы не попадать в пары игроков.

## English

Client-side Fabric mod that adds a fourth tab to the statistics screen: per-player
kills/deaths and totems popped/lost, chat-based kill/totem pairs, per-server
profiles, themes and settings. All statistics are stored in a readable text file
`config/totems.txt`. Optional Mod Menu integration opens the settings screen from
the mod list. Supports Minecraft 1.21.11, 26.1.x and 26.2/26.3.

## Лицензия

MIT © ggglobus228
