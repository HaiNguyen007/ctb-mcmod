package com.creatubbles.repack.enderlib.api.config;

import java.io.File;
import java.util.List;

import net.minecraftforge.common.config.ConfigCategory;

import com.creatubbles.repack.enderlib.common.config.Section;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;

public interface IConfigHandler {

	void initialize(File cfg);

	List<Section> getSections();

	ConfigCategory getCategory(String name);

	String getModID();

	/**
	 * A hook for the {@link FMLInitializationEvent}.
	 */
	void initHook();

	/**
	 * A hook for the {@link FMLPostInitializationEvent}.
	 */
	void postInitHook();
}
