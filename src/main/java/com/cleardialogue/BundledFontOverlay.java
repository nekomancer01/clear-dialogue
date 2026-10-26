package com.cleardialogue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws dialogue text in a bundled font (OpenDyslexic) for fonts the game doesn't have.
 *
 * The game's own text widgets stay in place, made fully transparent by the plugin, so every
 * click area and shortcut is unchanged. This overlay only redraws the same text inside each
 * widget's own bounds, following the widget's alignment.
 */
@Slf4j
class BundledFontOverlay extends Overlay
{
	private static final Pattern TAGS = Pattern.compile("<[^>]*>");
	private static final Pattern BREAKS = Pattern.compile("<br>", Pattern.CASE_INSENSITIVE);
	private static final int MIN_SIZE = 8;
	private static final int MAX_SIZE = 28;
	private static final int MAX_CACHED_LAYOUTS = 64;

	private static Font regular;
	private static Font bold;

	private final ClearDialoguePlugin plugin;
	private final ClearDialogueConfig config;

	/**
	 * Fitted layouts, so wrapping and shrink-to-fit run once per text rather than every frame.
	 */
	private final Map<String, Layout> layouts = new HashMap<>();

	@Inject
	BundledFontOverlay(ClearDialoguePlugin plugin, ClearDialogueConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		List<ClearDialoguePlugin.DrawnText> texts = plugin.getDrawnTexts();
		if (texts.isEmpty())
		{
			return null;
		}

		int size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, config.bundledFontSize()));
		int spacing = Math.max(60, Math.min(150, config.bundledLineSpacing()));
		boolean shadow = config.shadow();
		for (ClearDialoguePlugin.DrawnText t : texts)
		{
			// Own copy of the graphics so font, colour, hints and clip don't leak into other overlays
			Graphics2D g2 = (Graphics2D) g.create();
			try
			{
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				draw(g2, t.getWidget(), t.getText(), t.getColour(), t.isBold(), size, spacing, shadow);
			}
			finally
			{
				g2.dispose();
			}
		}
		return null;
	}

	private void draw(Graphics2D g, Widget w, String rawText, Color colour, boolean useBold,
		int maxSize, int spacing, boolean shadow)
	{
		String text = clean(rawText);
		Rectangle box = w.getBounds();
		if (text.isEmpty() || box == null || box.width <= 0 || box.height <= 0)
		{
			return;
		}

		String key = text + '\u0000' + box.width + 'x' + box.height + '/' + maxSize + '/' + spacing + '/' + useBold;
		Layout layout = layouts.get(key);
		if (layout == null)
		{
			if (layouts.size() >= MAX_CACHED_LAYOUTS)
			{
				layouts.clear();
			}
			layout = fit(g, text, box, maxSize, spacing, useBold);
			layouts.put(key, layout);
		}

		g.setFont(layout.font);
		FontMetrics fm = g.getFontMetrics();
		List<String> lines = layout.lines;
		int lineHeight = layout.lineHeight;
		// Keep overflow (text that can't fit even at the minimum size) inside the widget's box
		g.clip(new Rectangle(box.x - 2, box.y - 2, box.width + 4, box.height + 4));

		int blockHeight = (lines.size() - 1) * lineHeight + fm.getAscent() + fm.getDescent();
		int y;
		// If it can't fit even at the smallest size, start from the top so the beginning is readable
		switch (blockHeight > box.height + 4 ? 0 : w.getYTextAlignment())
		{
			case 0:
				y = box.y;
				break;
			case 2:
				y = box.y + box.height - blockHeight;
				break;
			default:
				y = box.y + (box.height - blockHeight) / 2;
				break;
		}
		y += fm.getAscent();

		for (String line : lines)
		{
			int width = fm.stringWidth(line);
			int x;
			switch (w.getXTextAlignment())
			{
				case 0:
					x = box.x;
					break;
				case 2:
					x = box.x + box.width - width;
					break;
				default:
					x = box.x + (box.width - width) / 2;
					break;
			}
			if (shadow)
			{
				g.setColor(Color.BLACK);
				g.drawString(line, x + 1, y + 1);
			}
			g.setColor(colour);
			g.drawString(line, x, y);
			y += lineHeight;
		}
	}

	/**
	 * Shrinks the font until the wrapped text fits the box.
	 */
	private static Layout fit(Graphics2D g, String text, Rectangle box, int maxSize, int spacing, boolean useBold)
	{
		int size = maxSize;
		while (true)
		{
			Font f = font(useBold).deriveFont((float) size);
			FontMetrics fm = g.getFontMetrics(f);
			int lineHeight = Math.max(1, Math.round(fm.getHeight() * spacing / 100f));
			List<String> lines = wrap(text, fm, box.width);
			// The last line only needs its glyphs to fit, with a little slack
			int needed = (lines.size() - 1) * lineHeight + fm.getAscent() + fm.getDescent();
			if (needed <= box.height + 4 || size <= MIN_SIZE)
			{
				return new Layout(f, lines, lineHeight);
			}
			size--;
		}
	}

	private static List<String> wrap(String text, FontMetrics fm, int width)
	{
		List<String> lines = new ArrayList<>();
		for (String paragraph : text.split("\n"))
		{
			StringBuilder line = new StringBuilder();
			for (String word : paragraph.split(" "))
			{
				if (word.isEmpty())
				{
					continue;
				}
				// A word wider than the whole box is split across lines rather than overflowing
				while (fm.stringWidth(word) > width && word.length() > 1)
				{
					int cut = word.length() - 1;
					while (cut > 1 && fm.stringWidth(word.substring(0, cut)) > width)
					{
						cut--;
					}
					if (line.length() > 0)
					{
						lines.add(line.toString());
						line.setLength(0);
					}
					lines.add(word.substring(0, cut));
					word = word.substring(cut);
				}
				String candidate = line.length() == 0 ? word : line + " " + word;
				if (line.length() > 0 && fm.stringWidth(candidate) > width)
				{
					lines.add(line.toString());
					line.setLength(0);
					line.append(word);
				}
				else
				{
					line.setLength(0);
					line.append(candidate);
				}
			}
			lines.add(line.toString());
		}
		return lines;
	}

	private static String clean(String text)
	{
		if (text == null)
		{
			return "";
		}
		// The game's line breaks are placed for its narrow font, so re-wrap instead of keeping them
		String s = BREAKS.matcher(text).replaceAll(" ");
		s = s.replace("<lt>", "\u0000lt").replace("<gt>", "\u0000gt");
		s = TAGS.matcher(s).replaceAll("");
		return s.replace("\u0000lt", "<").replace("\u0000gt", ">").replace(' ', ' ').trim();
	}

	private static synchronized Font font(boolean useBold)
	{
		if (regular == null)
		{
			regular = load("OpenDyslexic-Regular.otf");
			bold = load("OpenDyslexic-Bold.otf");
		}
		return useBold ? bold : regular;
	}

	private static Font load(String name)
	{
		try (InputStream in = BundledFontOverlay.class.getResourceAsStream(name))
		{
			return Font.createFont(Font.TRUETYPE_FONT, in);
		}
		catch (Exception e)
		{
			log.warn("Couldn't load {}, falling back to sans-serif", name, e);
			return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
		}
	}

	private static final class Layout
	{
		private final Font font;
		private final List<String> lines;
		private final int lineHeight;

		private Layout(Font font, List<String> lines, int lineHeight)
		{
			this.font = font;
			this.lines = lines;
			this.lineHeight = lineHeight;
		}
	}
}
