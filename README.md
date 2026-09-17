# JOSM Regex Replace Plugin

Adds a **"Regex Search/Replace…"** item to JOSM's **More tools** menu.
It supports regex, literal, whole-value, conditional, and multi-key replacement;
shows a live, sortable preview; is available from the toolbar; and applies
selected changes as one undoable edit.

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
   The plugin jar will appear at `build/libs/regexreplace.jar`.

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
3. Choose a key operation: replace in place, rename to a destination key, or
   copy to a destination key. Multiple source keys may be comma-separated, and
   **All existing tags** can be used for source selection.
4. Optionally restrict by primitive type, selection, JOSM search expression,
   condition tag/regex, or minimum/maximum matches per value. Missing source
   tags can be created from an empty value when explicitly enabled.
5. Review the highlighted preview. Empty results delete the tag by default;
   enable **Keep empty tag values** or **Prevent empty result** as appropriate.
6. Use **Export CSV** for a non-mutating review, or **Apply**
   to create one undoable edit. `Ctrl+Z` undoes it like any other edit.

The dialog remembers the last operation, supports named presets, and keeps up to
ten recent operations.
Preview rows are capped at 5,000; narrow the scope before applying a truncated
preview. Large operations require an additional confirmation.

## Test

Run the headless unit suite with:

```
gradle test
```

The tests cover replacement semantics, modes, flags, empty-value policy, and
the pure transformation boundaries. JOSM UI and dataset integration tests
require a running JOSM environment and are kept separate from the headless suite.
