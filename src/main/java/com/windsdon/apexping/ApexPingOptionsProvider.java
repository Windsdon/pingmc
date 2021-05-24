/*
 * Apex Ping
 * Copyright (C) 2021 Windsdon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.windsdon.apexping;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;

public class ApexPingOptionsProvider {
	public static class ApexPingOptions {
		public String serverAddress = "wss://apexping-public.windsdon.com";
		public String serverAuthToken = "";
		public String groupId = "";

		public ApexPingOptions() {
		}

		public void save(Path path) throws IOException {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(this);

			BufferedWriter writer = Files.newBufferedWriter(path);
			writer.write(json);
			writer.close();
		}
	}

	private static ApexPingOptions INSTANCE = null;

	private ApexPingOptionsProvider() {
	}

	private static ApexPingOptions load() {
		Path path = getPath();
		System.out.println("Loading ApexPing config from " + path);

		Gson gson = new Gson();
		try {
			BufferedReader reader = Files.newBufferedReader(path);
			ApexPingOptions options = gson.fromJson(reader, ApexPingOptions.class);
			options.save(path);
			return options;
		} catch (IOException e) {
			ApexPingOptions options = new ApexPingOptions();

			try {
				options.save(path);
			} catch (IOException ioException) {
				throw new RuntimeException("Cannot init config file at " + path, ioException);
			}

			return options;
		}
	}

	@NotNull
	private static Path getPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("apexping.json");
	}

	public static ApexPingOptions getOptions() {
		return INSTANCE;
	}

	public static void init() {
		if (INSTANCE == null) {
			INSTANCE = load();
		}
	}

	public static Screen buildScreen(Screen parent) {
		ApexPingOptions options = ApexPingOptionsProvider.getOptions();
		ApexPingOptions defaultOptions = new ApexPingOptions();

		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(new TranslatableText("title.apexping.config"));

		ConfigCategory server = builder.getOrCreateCategory(new TranslatableText("category.apexping.server"));

		ConfigEntryBuilder entryBuilder = builder.entryBuilder();
		server.addEntry(
			entryBuilder
				.startStrField(new TranslatableText("option.apexping.server_addr"), options.serverAddress)
				.setDefaultValue(defaultOptions.serverAddress)
				.setErrorSupplier(s -> {
					try {
						URI uri = new URI(s);

						if (!uri.getScheme().equals("wss")) {
							return Optional.of(new TranslatableText("option.apexping.server_addr.error.protocol"));
						}
					} catch (URISyntaxException e) {
						return Optional.of(new TranslatableText("option.apexping.server_addr.error.invalid", e.getMessage()));
					}

					return Optional.empty();
				})
				.setTooltip(new TranslatableText("option.apexping.server_addr.tooltip"))
				.setSaveConsumer(s -> options.serverAddress = s)
				.build());

		server.addEntry(
			entryBuilder
				.startStrField(new TranslatableText("option.apexping.server_token"), options.serverAuthToken)
				.setDefaultValue(defaultOptions.serverAuthToken)
				.setTooltip(new TranslatableText("option.apexping.server_token.tooltip"))
				.setSaveConsumer(s -> options.serverAuthToken = s)
				.build());

		server.addEntry(
			entryBuilder
				.startStrField(new TranslatableText("option.apexping.group_id"), options.groupId)
				.setDefaultValue(defaultOptions.groupId)
				.setTooltip(new TranslatableText("option.apexping.group_id.tooltip"))
				.setSaveConsumer(s -> options.groupId = s)
				.build());

		builder.alwaysShowTabs();

		builder.setSavingRunnable(() -> {
			try {
				options.save(getPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		return builder.build();
	}
}
