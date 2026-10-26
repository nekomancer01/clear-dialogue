package com.cleardialogue;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(ClearDialogueConfig.GROUP)
public interface ClearDialogueConfig extends Config
{
	String GROUP = "cleardialogue";

	@ConfigSection(
		name = "Text",
		description = "Font and text colours",
		position = 0
	)
	String textSection = "text";

	@ConfigItem(
		keyName = "font",
		name = "Font",
		description = "Font used for dialogue text. Larger fonts fit fewer words per line. "
			+ "OpenDyslexic isn't a game font, so the plugin draws that text itself.",
		section = textSection,
		position = 0
	)
	default GameFont font()
	{
		return GameFont.VERDANA_13;
	}

	@Range(min = 10, max = 28)
	@ConfigItem(
		keyName = "bundledFontSize",
		name = "OpenDyslexic size",
		description = "Text size when using OpenDyslexic. Text shrinks automatically if it would not fit.",
		section = textSection,
		position = 1
	)
	default int bundledFontSize()
	{
		return 14;
	}

	@Range(min = 60, max = 150)
	@ConfigItem(
		keyName = "bundledLineSpacing",
		name = "OpenDyslexic line spacing",
		description = "Gap between lines when using OpenDyslexic, as a % of the font's normal spacing",
		section = textSection,
		position = 2
	)
	default int bundledLineSpacing()
	{
		return 80;
	}

	@ConfigItem(
		keyName = "textColor",
		name = "Dialogue text",
		description = "Dialogue and option text colour",
		section = textSection,
		position = 3
	)
	default Color textColor()
	{
		return Color.BLACK;
	}

	@ConfigItem(
		keyName = "nameColor",
		name = "Speaker name",
		description = "Colour of the speaker's name and menu titles",
		section = textSection,
		position = 4
	)
	default Color nameColor()
	{
		return new Color(0x7A, 0x00, 0x00);
	}

	@ConfigItem(
		keyName = "continueColor",
		name = "Continue prompt",
		description = "Colour of 'Click here to continue'",
		section = textSection,
		position = 5
	)
	default Color continueColor()
	{
		return new Color(0x1C, 0x3C, 0x9C);
	}

	@ConfigItem(
		keyName = "hoverColor",
		name = "Hover",
		description = "Colour of an option or prompt under the mouse",
		section = textSection,
		position = 6
	)
	default Color hoverColor()
	{
		return new Color(0x8A, 0x24, 0x00);
	}

	@ConfigItem(
		keyName = "shadow",
		name = "Text shadow",
		description = "Draw a dark shadow behind text",
		section = textSection,
		position = 7
	)
	default boolean shadow()
	{
		return false;
	}
}
