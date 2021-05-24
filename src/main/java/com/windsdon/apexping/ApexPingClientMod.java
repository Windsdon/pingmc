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

import com.windsdon.apexping.utils.PingRayTracer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ApexPingClientMod implements ClientModInitializer {
	private static KeyBinding defaultPingKeyBinding;

	private static ApexPingClientMod INSTANCE;

	private ArrayList<Ping> pings = new ArrayList<>();
	private Matrix4f pingProjectionMatrix;
	private Matrix4f pingViewMatrix;

	Vector4f posDebug;
	private Camera pingCamera;

	@Override
	public void onInitializeClient() {
		INSTANCE = this;

		defaultPingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.apexping.default",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			"category.apexping.pings"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (defaultPingKeyBinding.wasPressed()) {
				triggerPing(client);
			}
		});

		HudRenderCallback.EVENT.register(ApexPingClientMod::displayPingsHud);
		WorldRenderEvents.END.register(ApexPingClientMod::recomputePingPositions);

		pings.add(new Ping(Ping.Type.ENEMY, DimensionType.OVERWORLD_ID.toString(), new BlockPos(0, 3, 0)));
	}

	private void triggerPing(MinecraftClient client) {
		HitResult result = PingRayTracer.tracePing(client.getTickDelta());
		String worldId = client.world.getDimension().getSkyProperties().toString();
		String resultText = "?";
		Ping ping = null;
		switch (result.getType()) {
			case MISS:
				resultText = "MISS";
				break;
			case BLOCK:
				resultText = "BLOCK " + result.getPos();
				BlockHitResult blockHitResult = (BlockHitResult) result;
				ping = new Ping(Ping.Type.ENEMY, worldId, blockHitResult.getBlockPos());
				break;
			case ENTITY:
				EntityHitResult entityHitResult = (EntityHitResult) result;
				resultText = "ENTITY " + entityHitResult.getEntity().getClass().getName();
				ping = new Ping(Ping.Type.ENEMY, worldId, entityHitResult.getEntity().getUuid());
				break;
		}

		client.player.sendMessage(new LiteralText("Ping triggered - " + resultText), false);
		if (ping != null) {
			INSTANCE.pings.add(ping);
			client.player.sendMessage(new LiteralText(ping.toJson()), false);
		}
	}

	public static void displayPingsHud(MatrixStack matrixStack, float tickDelta) {
		Matrix4f projectionMatrix = INSTANCE.pingProjectionMatrix;
		Matrix4f viewMatrix = INSTANCE.pingViewMatrix;
		Vec3d camera = INSTANCE.pingCamera.getPos();
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = client.getWindow();

		for (Ping ping : INSTANCE.pings) {
			Vector4f posWorld;

			if (ping.targetType == Ping.TargetType.BLOCK) {
				BlockPos pos = ping.blockPos;

				posWorld = new Vector4f((float) (pos.getX() + 0.5), (float) (pos.getY() + 0.5), (float) (pos.getZ() + 0.5), 1);
			} else if (ping.targetType == Ping.TargetType.ENTITY) {
				Optional<Entity> entity = StreamSupport.stream(client.world.getEntities().spliterator(), false).filter(e -> e.getUuid().equals(ping.entityUUID)).findFirst();
				if (!entity.isPresent()) {
					continue;
				}

				Vec3d pos = entity.get().getPos();
				posWorld = new Vector4f((float) pos.getX(), (float) pos.getY(), (float) pos.getZ(), 1);
			} else {
				continue;
			}

			posWorld.set((float) (posWorld.getX() - camera.x), (float) (posWorld.getY() - camera.y), (float) (posWorld.getZ() - camera.z), 1);
			posWorld.transform(viewMatrix);
			posWorld.transform(projectionMatrix);

			Vector3f ndcSpacePos = new Vector3f(posWorld.getX(), posWorld.getY(), posWorld.getZ());
			ndcSpacePos.scale(1 / posWorld.getW());

			if (ndcSpacePos.getZ() > 1) {
				ndcSpacePos.set(
					Math.copySign(1, -ndcSpacePos.getX()),
					Math.copySign(1, -ndcSpacePos.getY()),
					0
				);
			}

			float posX = MathHelper.clamp((ndcSpacePos.getX() + 1) / 2, 0, 1);
			float posY = MathHelper.clamp((ndcSpacePos.getY() + 1) / 2, 0, 1);

			client.textRenderer.draw(matrixStack, String.format("%.3f %.3f", posX, posY), 0, 0, 0xffffffff);

			int screenPosX = MathHelper.clamp((int) (window.getScaledWidth() * posX - 8), 0, window.getScaledWidth() - 16);
			int screenPosY = MathHelper.clamp((int) (window.getScaledHeight() * (1 - posY) - 8), 0, window.getScaledHeight() - 16);

			ItemRenderer itemRenderer = client.getItemRenderer();
			itemRenderer.renderGuiItemIcon(
				new ItemStack(Items.NETHER_STAR, 1),
				screenPosX,
				screenPosY
			);

		}
	}

	private static void recomputePingPositions(WorldRenderContext worldRenderContext) {
		INSTANCE.pingProjectionMatrix = worldRenderContext.projectionMatrix().copy();
		INSTANCE.pingViewMatrix = worldRenderContext.matrixStack().peek().getModel().copy();
		INSTANCE.pingCamera = worldRenderContext.camera();
	}
}
