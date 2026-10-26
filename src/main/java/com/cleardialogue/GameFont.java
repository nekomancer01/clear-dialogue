package com.cleardialogue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.FontID;

/**
 * Dialogue fonts. Most ship with the game itself, so the game draws the text. Bundled fonts
 * (fontId -1) aren't in the game, so the plugin draws that text itself.
 */
@Getter
@RequiredArgsConstructor
public enum GameFont
{
	VERDANA_11("Verdana 11", FontID.VERDANA_11),
	VERDANA_13("Verdana 13", FontID.VERDANA_13),
	VERDANA_13_BOLD("Verdana 13 bold", FontID.VERDANA_13_BOLD),
	TAHOMA_11("Tahoma 11", FontID.TAHOMA_11),
	RUNESCAPE("RuneScape", FontID.PLAIN_12),
	RUNESCAPE_BOLD("RuneScape bold", FontID.BOLD_12),
	ORIGINAL("Original (Quill)", FontID.QUILL_8),
	OPENDYSLEXIC("OpenDyslexic", -1);

	private final String name;
	private final int fontId;

	public boolean isBundled()
	{
		return fontId < 0;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
