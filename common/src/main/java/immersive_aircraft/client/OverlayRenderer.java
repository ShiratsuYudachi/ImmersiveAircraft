package immersive_aircraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import immersive_aircraft.Main;
import immersive_aircraft.config.Config;
import immersive_aircraft.entity.EngineVehicle;
import immersive_aircraft.entity.VehicleEntity;
import immersive_aircraft.util.LinearAlgebraUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class OverlayRenderer {
    static final OverlayRenderer INSTANCE = new OverlayRenderer();

    private static final ResourceLocation ENGINE_TEX = Main.locate("textures/gui/engine.png");
    private static final ResourceLocation POWER_TEX = Main.locate("textures/gui/power.png");
    private static final ResourceLocation ICONS_TEX = Main.locate("textures/gui/icons.png");


    private float bootUp = 0.0f;
    private float lastTime = 0.0f;

    public static void renderOverlay(GuiGraphics context, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null && client.player != null) {
            if (Config.getInstance().showHotbarEngineGauge && client.player.getRootVehicle() instanceof EngineVehicle aircraft) {
                INSTANCE.renderAircraftGui(client, context, tickDelta, aircraft);
            }
            if (client.player.getRootVehicle() instanceof VehicleEntity vehicle) {
                INSTANCE.renderAircraftHealth(client, context, vehicle);
            }
        }
    }

    private void renderAircraftHealth(Minecraft minecraft, GuiGraphics context, VehicleEntity vehicle) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        int maxHearts = 10;
        int health = (int) Math.ceil(vehicle.getHealth() * maxHearts * 2);

        int y = screenHeight - 49 - Config.getInstance().healthBarRow * 10;
        int ox = screenWidth / 2 + 91;
        for (int i = 0; i < maxHearts; i++) {
            int u = 52;
            int x = ox - i * 8 - 9;
            context.blit(ICONS_TEX, x, y, u, 9, 9, 9, 64, 64);
            if (i * 2 + 1 < health) {
                context.blit(ICONS_TEX, x, y, 0, 0, 9, 9, 64, 64);
            }
            if (i * 2 + 1 != health) continue;
            context.blit(ICONS_TEX, x, y, 10, 0, 9, 9, 64, 64);
        }
    }


    private void renderAircraftGui(Minecraft client, GuiGraphics context, float tickDelta, EngineVehicle aircraft) {
        assert client.level != null;

        if (aircraft.getGuiStyle() == EngineVehicle.GUI_STYLE.ENGINE) {
            float time = client.level.getGameTime() % 65536 + tickDelta;
            float delta = time - lastTime;
            lastTime = time;

            // boot-up animation
            int frame;
            if (aircraft.getEngineTarget() > 0 && aircraft.getEnginePower() > 0.001) {
                if (bootUp < 1.0f) {
                    bootUp = Math.min(1.0f, bootUp + delta * 0.2f);
                    frame = (int) (bootUp * 5);
                } else {
                    final int FPS = 30;
                    int animation = (int) (aircraft.engineRotation.getSmooth(tickDelta) / 20.0f * FPS);
                    frame = 5 + animation % 6;
                }
            } else {
                if (bootUp > 0.0f) {
                    bootUp = Math.max(0.0f, bootUp - delta * 0.1f);
                    frame = 10 + (int) ((1.0 - bootUp) * 10);
                } else {
                    frame = 20;
                }
            }

            int powerFrame = (int) ((1.0f - aircraft.getEnginePower()) * 10 + 10.5);
            int powerFrameTarget = (int) ((1.0f - aircraft.getEngineTarget()) * 10 + 10.5);

            int x = client.getWindow().getGuiScaledWidth() / 2;
            int y = client.getWindow().getGuiScaledHeight() - 37;

            if (client.gameMode != null && !client.gameMode.hasExperience()) {
                y += 7;
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            context.blit(ENGINE_TEX, x - 9, y - 9, (frame % 5) * 18, Math.floorDiv(frame, 5) * 18, 18, 18, 90, 90);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableBlend();
            context.blit(POWER_TEX, x - 9, y - 9, (powerFrame % 5) * 18, Math.floorDiv(powerFrame, 5) * 18, 18, 18, 90, 90);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.5f);
            context.blit(POWER_TEX, x - 9, y - 9, (powerFrameTarget % 5) * 18, Math.floorDiv(powerFrameTarget, 5) * 18, 18, 18, 90, 90);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int radius = 5; // 可以根据需要调整大小
        int color = 0xFFFFFFFF; // 白色，可以根据需要更改
        drawHollowCircle(context, centerX, centerY, radius, color, 1);

        // 获取飞行器的世界坐标
        Vec3 aircraftPos = aircraft.position();
        Vec3 lookVector = aircraft.getLookAngle();

        // 将位置沿着视线方向向前移动 5 个方块
        aircraftPos = aircraftPos.add(lookVector.scale(100));
        //aircraftPos = aircraftPos.add(0,-5,0); // hack, slight 偏移
        //System.out.println(aircraftPos.y);

        // 将世界坐标转换为屏幕坐标
        Vector3f screenPos = LinearAlgebraUtil.worldToScreenPoint(aircraftPos, client.getFrameTime());

        // 检查点是否在屏幕内
        if (screenPos.x() >= 0 && screenPos.x() < screenWidth &&
                screenPos.y() >= 0 && screenPos.y() < screenHeight) {
            // 在飞行器的屏幕位置绘制十字
            drawCross(context, (int)screenPos.x(), (int)screenPos.y(), radius, color, 1);
            System.out.println("x:"+screenPos.x()+", y:"+screenPos.y());
        }
    }

    private void drawHollowCircle(GuiGraphics context, int centerX, int centerY, int radius, int color, int thickness) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                int distanceSquared = x * x + y * y;
                if (distanceSquared <= radius * radius && distanceSquared >= (radius - thickness) * (radius - thickness)) {
                    context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }

    private void drawCross(GuiGraphics context, int centerX, int centerY, int size, int color, int thickness) {
        // 绘制竖线
        for (int y = -size; y <= size; y++) {
            for (int x = -thickness / 2; x < thickness / 2 + thickness % 2; x++) {
                context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            }
        }

        // 绘制横线
        for (int x = -size; x <= size; x++) {
            for (int y = -thickness / 2; y < thickness / 2 + thickness % 2; y++) {
                context.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
            }
        }
    }
}
