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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;

import java.util.UUID;

public class Ping {
	enum TargetType {
		ENTITY,
		BLOCK
	}

	enum Type {
		ENEMY,
		LOOK
	}

	public Type type;
	public TargetType targetType;
	public String dimension;
	public UUID entityUUID = null;
	public BlockPos blockPos = null;

	public Vec2f screenPosition;

	public Ping() {
	}

	public Ping(Type type, String dimension, UUID entityUUID) {
		this.type = type;
		this.dimension = dimension;
		this.targetType = TargetType.ENTITY;
		this.entityUUID = entityUUID;
	}

	public Ping(Type type, String dimension, BlockPos pos) {
		this.type = type;
		this.dimension = dimension;
		this.targetType = TargetType.BLOCK;
		this.blockPos = pos;
	}

	public String toJson() {
		Gson gson = new GsonBuilder().create();
		return gson.toJson(this);
	}

	public static Ping fromJson(String json) {
		Gson gson = new Gson();
		return gson.fromJson(json, Ping.class);
	}
}
