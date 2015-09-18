package com.creatubbles.repack.enderlib.api.config;

import java.io.File;
import java.util.List;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import com.creatubbles.repack.enderlib.common.config.Section;

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