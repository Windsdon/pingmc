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

package com.windsdon.apexping.utils;


import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;

/**
 * This is based on (read: copied from) the fabric tutorial on ray casting
 */
public class PingRayTracer {
	public static HitResult tracePing(float tickDelta) {
		return tracePing(tickDelta, 1000);
	}

	@NotNull
	public static HitResult tracePing(float tickDelta, double maxDistance) {
		MinecraftClient client = MinecraftClient.getInstance();
		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		Vec3d cameraDirection = client.cameraEntity.getRotationVec(tickDelta);
		double fov = client.options.fov;
		double angleSize = fov / height;
		Vector3f verticalRotationAxis = new Vector3f(cameraDirection);
		verticalRotationAxis.cross(Vector3f.POSITIVE_Y);

		if (!verticalRotationAxis.normalize()) {
			return BlockHitResult.createMissed(Vec3d.ZERO, Direction.UP, new BlockPos(Vec3d.ZERO));
		}

		Vector3f horizontalRotationAxis = new Vector3f(cameraDirection);
		horizontalRotationAxis.cross(verticalRotationAxis);
		horizontalRotationAxis.normalize();

		verticalRotationAxis = new Vector3f(cameraDirection);
		verticalRotationAxis.cross(horizontalRotationAxis);

		Vec3d direction = map(
			(float) angleSize,
			cameraDirection,
			horizontalRotationAxis,
			verticalRotationAxis,
			width / 2,
			height / 2,
			width,
			height
		);
		HitResult hit = raycastInDirection(client, tickDelta, direction, maxDistance);

		return hit;
	}

	private static Vec3d map(float anglePerPixel, Vec3d center, Vector3f horizontalRotationAxis,
							 Vector3f verticalRotationAxis, int x, int y, int width, int height) {
		float horizontalRotation = (x - width / 2f) * anglePerPixel;
		float verticalRotation = (y - height / 2f) * anglePerPixel;

		final Vector3f temp2 = new Vector3f(center);
		temp2.rotate(verticalRotationAxis.getDegreesQuaternion(verticalRotation));
		temp2.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation));
		return new Vec3d(temp2);
	}

	private static HitResult raycastInDirection(MinecraftClient client, float tickDelta, Vec3d direction, double maxDistance) {
		Entity entity = client.getCameraEntity();
		if (entity == null || client.world == null) {
			return null;
		}

		HitResult target = raycast(entity, maxDistance, tickDelta, false, direction);

		Vec3d cameraPos = entity.getCameraPosVec(tickDelta);

		double reachSquared = maxDistance * maxDistance;

		if (target != null) {
			reachSquared = target.getPos().squaredDistanceTo(cameraPos);
		}

		Vec3d vec3d3 = cameraPos.add(direction.multiply(maxDistance));
		Box box = entity
			.getBoundingBox()
			.stretch(entity.getRotationVec(1.0F).multiply(maxDistance))
			.expand(1.0D, 1.0D, 1.0D);

		EntityHitResult entityHitResult = ProjectileUtil.raycast(
			entity,
			cameraPos,
			vec3d3,
			box,
			(entityx) -> !entityx.isSpectator() && entityx.collides(),
			reachSquared
		);

		if (entityHitResult == null) {
			return target;
		}

		Entity entity2 = entityHitResult.getEntity();
		Vec3d vec3d4 = entityHitResult.getPos();
		double g = cameraPos.squaredDistanceTo(vec3d4);

		if (g < reachSquared || target == null) {
			target = entityHitResult;
			if (entity2 instanceof LivingEntity || entity2 instanceof ItemFrameEntity) {
				client.targetedEntity = entity2;
			}
		}

		return target;
	}

	private static HitResult raycast(
		Entity entity,
		double maxDistance,
		float tickDelta,
		boolean includeFluids,
		Vec3d direction
	) {
		Vec3d end = entity.getCameraPosVec(tickDelta).add(direction.multiply(maxDistance));
		return entity.world.raycast(new RaycastContext(
			entity.getCameraPosVec(tickDelta),
			end,
			RaycastContext.ShapeType.OUTLINE,
			includeFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
			entity
		));
	}
}
