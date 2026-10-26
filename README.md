# Clear Dialogue

Makes OSRS dialogue boxes easier to read. You can switch the cursive dialogue font for
**OpenDyslexic** or a clearer game font, and choose your own text colours.

## Features

- **OpenDyslexic:** a typeface designed for readers with dyslexia, bundled with the
  plugin. It has its own size and line-spacing settings, and text shrinks automatically
  to fit the box.
- **Clearer game fonts:** Verdana 11, 13 or 13 bold, Tahoma 11, or RuneScape plain or
  bold. You can also keep the original Quill font and just change the colours.
- **Custom colours:** dialogue text, speaker name and menu titles, "Click here to
  continue", and the option or prompt under your mouse.
- **Text shadow:** on or off.

The defaults are dark colours that read well on the standard parchment: black text, a
dark red name, a dark blue prompt, and burnt orange on hover. With a dark chatbox (for
example a dark resource pack), switch to light colours and turn on the text shadow.

Chat heads, item pictures, click areas and keyboard shortcuts all work exactly as
normal.

## Dialogue covered

- NPC and player dialogue
- "Select an option" menus
- Plain, titled and link messages
- Item, two-item and picture messages
- Level-up messages

## How it works

- **Game fonts:** the plugin only changes the font, colour and shadow of the game's own
  dialogue text.
- **OpenDyslexic:** the game can't draw this font, so the plugin hides the game's copy
  of the text and draws the identical text in the same place.
  - Options and "Click here to continue" are made fully transparent, so they stay
    clickable exactly as before.
  - The speaker name and dialogue text aren't clickable, so their text is saved,
    blanked in the game's widget, and put back afterwards.

Every change is cosmetic and is undone when the dialogue closes, when the settings
change, or when the plugin is switched off. The plugin never adds menu options and
never sends clicks or key presses.

## Limitations

- Only the game's own fonts and the bundled OpenDyslexic are available, not fonts
  installed on your computer.
- The chatbox background isn't changed.
- Game fonts are drawn at a fixed size, so very long lines may wrap less neatly than
  with OpenDyslexic.

## Development

Run a development client with the plugin loaded:

```bash
./gradlew run
```

## Credits

OpenDyslexic is by Abbie Gonzalez and is used under the SIL Open Font License 1.1. See
`src/main/resources/com/cleardialogue/OpenDyslexic-OFL.txt`.

The plugin itself is licensed under the BSD 2-Clause License (see `LICENSE`).
