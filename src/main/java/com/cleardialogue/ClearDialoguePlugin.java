package com.cleardialogue;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Restyles the game's own dialogue widgets: font and text colours.
 *
 * No text is changed, so chat heads, item models, click areas and keyboard shortcuts all behave
 * exactly as normal. Every change is cosmetic and is undone when the dialogue closes or the plugin
 * is turned off. The one exception to "the game draws everything" is a bundled font such as
 * OpenDyslexic: the game can't draw it, so {@link BundledFontOverlay} draws the same text in the
 * same boxes and the game's copy is hidden. Clickable widgets (options, continue) are made fully
 * transparent; non-clickable ones (name, body) have their text blanked and saved, because the game
 * ignores transparency on lines after a forced line break.
 */
@PluginDescriptor(
	name = "Clear Dialogue",
	description = "Accessible dialogue boxes: OpenDyslexic or clearer game fonts, plus custom text colours",
	tags = {"dialogue", "font", "readability", "accessibility", "dyslexia", "opendyslexic", "chatbox", "colour"}
)
public class ClearDialoguePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClearDialogueConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BundledFontOverlay bundledFontOverlay;

	/**
	 * Original styling of every widget we touched, so it can be put back.
	 */
	private final Map<Widget, Style> originals = new IdentityHashMap<>();

	/**
	 * Text of non-clickable widgets we blanked for a bundled font, so it can be drawn and put back.
	 */
	private final Map<Widget, String> blankedText = new IdentityHashMap<>();

	/**
	 * Text the overlay should draw this frame, when a bundled font is selected.
	 */
	@Getter
	private final List<DrawnText> drawnTexts = new ArrayList<>();

	@Override
	protected void startUp()
	{
		overlayManager.add(bundledFontOverlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(bundledFontOverlay);
		clientThread.invoke(() ->
		{
			drawnTexts.clear();
			restoreText();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (ClearDialogueConfig.GROUP.equals(event.getGroup()))
		{
			// Rebuilt with the new settings on the next frame
			clientThread.invoke(this::restoreText);
		}
	}

	/**
	 * Every chatbox dialogue made of a title/name, a body and a continue prompt.
	 * A title or continue of -1 means none, or (for continue) found by its text.
	 */
	private static final Box[] BOXES = {
		// NPC speech, player speech
		new Box(InterfaceID.ChatLeft.UNIVERSE, InterfaceID.ChatLeft.NAME, InterfaceID.ChatLeft.TEXT, InterfaceID.ChatLeft.CONTINUE),
		new Box(InterfaceID.ChatRight.UNIVERSE, InterfaceID.ChatRight.NAME, InterfaceID.ChatRight.TEXT, InterfaceID.ChatRight.CONTINUE),
		// Plain messages ("Someone throws a tomato at the recruiter."), titled messages, messages with a link
		new Box(InterfaceID.Messagebox.UNIVERSE, -1, InterfaceID.Messagebox.TEXT, InterfaceID.Messagebox.CONTINUE),
		new Box(InterfaceID.MessageboxTitled.UNIVERSE, InterfaceID.MessageboxTitled.TITLE, InterfaceID.MessageboxTitled.TEXT,
			InterfaceID.MessageboxTitled.CONTINUE),
		new Box(InterfaceID.MessageboxUrl.UNIVERSE, -1, InterfaceID.MessageboxUrl.TEXT, InterfaceID.MessageboxUrl.CONTINUE),
		// Messages with one item, two items or a picture ("You light the logs.")
		new Box(InterfaceID.Objectbox.UNIVERSE, -1, InterfaceID.Objectbox.TEXT, -1),
		new Box(InterfaceID.ObjectboxDouble.UNIVERSE, -1, InterfaceID.ObjectboxDouble.TEXT, -1),
		new Box(InterfaceID.Graphicbox.UNIVERSE, -1, InterfaceID.Graphicbox.TEXT, -1),
		// Level-up messages
		new Box(InterfaceID.LevelupDisplay.UNIVERSE, InterfaceID.LevelupDisplay.TEXT1, InterfaceID.LevelupDisplay.TEXT2,
			InterfaceID.LevelupDisplay.CONTINUE),
	};

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		drawnTexts.clear();
		boolean open = styleOptions();
		for (Box box : BOXES)
		{
			open |= styleBox(box);
		}

		if (!open && (!originals.isEmpty() || !blankedText.isEmpty()))
		{
			// Usually the game discards these widgets on close, but put everything back in case any are reused
			restoreText();
		}
	}

	private boolean styleBox(Box box)
	{
		Widget universe = visible(box.universe);
		if (universe == null)
		{
			return false;
		}
		if (box.title != -1)
		{
			style(visible(box.title), config.nameColor(), true);
		}
		Widget text = visible(box.text);
		style(text, config.textColor(), false);
		styleContinue(box.cont != -1 ? visible(box.cont) : findContinue(universe, text));
		return true;
	}

	private boolean styleOptions()
	{
		Widget options = visible(InterfaceID.Chatmenu.OPTIONS);
		if (options == null || options.getDynamicChildren() == null)
		{
			return false;
		}

		Widget[] rows = options.getDynamicChildren();
		for (int i = 0; i < rows.length; i++)
		{
			Widget row = rows[i];
			if (row == null || row.isHidden())
			{
				continue;
			}
			// Row 0 is the "Select an option" title. The game recolours hovered rows itself, but we
			// override colours every frame, so hover has to be handled here too.
			Color colour = i == 0 ? config.nameColor() : hovered(row) ? config.hoverColor() : config.textColor();
			style(row, colour, i == 0);
		}
		return true;
	}

	private void styleContinue(Widget cont)
	{
		if (cont != null)
		{
			style(cont, hovered(cont) ? config.hoverColor() : config.continueColor(), false);
		}
	}

	private void style(Widget w, Color colour, boolean bold)
	{
		if (w == null)
		{
			return;
		}
		originals.computeIfAbsent(w, Style::of);
		if (config.font().isBundled())
		{
			drawnTexts.add(new DrawnText(w, hideText(w), colour, bold));
			return;
		}
		// Only touch what differs, so the game isn't told to re-render unchanged text every frame
		int font = config.font().getFontId();
		int rgb = colour.getRGB() & 0xFFFFFF;
		if (w.getFontId() != font)
		{
			w.setFontId(font);
		}
		if (w.getTextColor() != rgb)
		{
			w.setTextColor(rgb);
		}
		if (w.getTextShadowed() != config.shadow())
		{
			w.setTextShadowed(config.shadow());
		}
	}

	/**
	 * Hides the game's copy of a widget's text and returns the text to draw instead.
	 */
	private String hideText(Widget w)
	{
		// Once blanked, stay on that path even if the widget's listeners change
		if (w.hasListener() && !blankedText.containsKey(w))
		{
			// Clickable: keep the text so nothing about the click changes, just make it fully transparent
			if (w.getOpacity() != 255)
			{
				w.setOpacity(255);
			}
			return w.getText();
		}

		String current = w.getText();
		if (current != null && !current.isEmpty())
		{
			// New text from the game (first frame, or the dialogue moved on): save it and blank the widget
			blankedText.put(w, current);
			w.setText("");
		}
		return blankedText.getOrDefault(w, "");
	}

	private void restoreText()
	{
		drawnTexts.clear();
		originals.forEach((w, s) -> s.apply(w));
		originals.clear();
		blankedText.forEach(Widget::setText);
		blankedText.clear();
	}

	/**
	 * Finds the clickable continue prompt in boxes that don't have a fixed widget for it.
	 * Only clickable widgets whose whole text is the prompt match, so body text mentioning
	 * "continue" is never mistaken for it.
	 */
	private static Widget findContinue(Widget parent, Widget exclude)
	{
		for (Widget[] group : new Widget[][]{parent.getDynamicChildren(), parent.getStaticChildren()})
		{
			if (group == null)
			{
				continue;
			}
			for (Widget w : group)
			{
				if (w == null || w == exclude || w.isHidden() || !w.hasListener() || w.getText() == null)
				{
					continue;
				}
				String t = w.getText().replaceAll("<[^>]*>", "").trim().toLowerCase();
				if (t.equals("click here to continue") || t.equals("click to continue") || t.startsWith("please wait"))
				{
					return w;
				}
			}
		}
		return null;
	}

	private boolean hovered(Widget w)
	{
		Point mouse = client.getMouseCanvasPosition();
		return mouse != null && w.getBounds().contains(mouse.getX(), mouse.getY());
	}

	private Widget visible(int componentId)
	{
		Widget w = client.getWidget(componentId);
		return w == null || w.isHidden() ? null : w;
	}

	@Value
	private static class Box
	{
		int universe;
		int title;
		int text;
		int cont;
	}

	@Value
	static class DrawnText
	{
		Widget widget;
		String text;
		Color colour;
		boolean bold;
	}

	private static final class Style
	{
		private final int font;
		private final int colour;
		private final boolean shadowed;
		private final int opacity;

		private Style(int font, int colour, boolean shadowed, int opacity)
		{
			this.font = font;
			this.colour = colour;
			this.shadowed = shadowed;
			this.opacity = opacity;
		}

		static Style of(Widget w)
		{
			return new Style(w.getFontId(), w.getTextColor(), w.getTextShadowed(), w.getOpacity());
		}

		void apply(Widget w)
		{
			w.setFontId(font);
			w.setTextColor(colour);
			w.setTextShadowed(shadowed);
			w.setOpacity(opacity);
		}
	}

	@Provides
	ClearDialogueConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClearDialogueConfig.class);
	}
}
