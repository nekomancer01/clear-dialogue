package com.cleardialogue;

import java.util.Arrays;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ClearDialoguePluginTest
{
	public static void main(String[] args) throws Exception
	{
		// loadBuiltin requires -ea, which some launchers do not pass. This must run before any RuneLite class is loaded.
		ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

		// Some third-party launchers pass -J<jvm arg> options meant for the official RuneLite launcher; the client rejects them
		String[] clientArgs = Arrays.stream(args)
			.filter(a -> !a.startsWith("-J"))
			.toArray(String[]::new);

		ExternalPluginManager.loadBuiltin(ClearDialoguePlugin.class);
		RuneLite.main(clientArgs);
	}
}
