# JOSM Regex Replace Plugin

Adds a **"Regex Search/Replace…"** item to JOSM's **More tools** menu.
It lets you pick a tag key, a Java-regex "find" pattern, and a replacement
(capture groups like `$1` are supported), shows a preview table of every
value that would change, and — on confirmation — applies the change as a
single, normal, undoable edit (`Ctrl+Z` works).

Unlike JOSM's built-in `name~"..."` search operator (which requires the
regex to match the *entire* tag value), this plugin uses standard
`Matcher.find()` / `replaceAll()` semantics: your pattern only needs to
match part of the string, so `^District d(e |')` alone is enough — no
trailing `.*` needed.

## Build

You need a JDK (17+) and Gradle (or use `gradle wrapper` to generate a
wrapper first if you don't have Gradle installed globally).

1. Get a copy of JOSM's jar to compile against. Either:
   - copy `josm-tested.jar` / `josm-custom.jar` from your existing JOSM
     installation folder, or
   - download the latest `josm-tested.jar` from
     https://josm.openstreetmap.de/download/
2. Place it at `libs/josm-tested.jar` in this project.
3. Build:
   ```
   gradle jar
   ```
   The plugin jar will appear at `build/libs/RegexReplace-1.0.0.jar`.

Note: the exact method signatures for `JosmAction`, `ChangePropertyCommand`,
etc. can shift slightly between JOSM releases. If the build fails with a
"method not found" / "constructor not found" error, check the class in
question in the `josm-tested.jar` you're compiling against (e.g. with a
decompiler or the JOSM javadoc) and adjust the call — the logic itself
won't need to change, just the exact API call shape.

## Install

Copy the built jar into your JOSM plugins directory:

- Linux: `~/.local/share/JOSM/plugins/`
- macOS: `~/Library/JOSM/plugins/`
- Windows: `%APPDATA%\JOSM\plugins\`

Restart JOSM, then check **Edit → Preferences → Plugins** and make sure
"RegexReplace" is enabled (it should be, since it wasn't installed through
the plugin manager, JOSM just needs to see the jar and load it once).

## Use

1. Select the objects you want to affect (or leave "Selected objects only"
   unchecked to scan every object with that tag key in the layer).
2. **More tools → Regex Search/Replace…**
3. Fill in the tag key (e.g. `name`), the find pattern (e.g.
   `^District d(e |')`), and the replacement (e.g. empty string to strip
   the prefix, or `District: ` to reformat it).
4. Review the preview table, click OK to apply.
5. `Ctrl+Z` undoes it like any other edit if something looks wrong.
