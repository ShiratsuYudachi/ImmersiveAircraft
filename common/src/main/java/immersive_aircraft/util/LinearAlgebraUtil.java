package immersive_aircraft.util;


import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector2f;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

import java.lang.reflect.Method;

public class LinearAlgebraUtil {

    // Thanks Claude3.5 Sonnet and https://forums.minecraftforge.net/topic/88562-116solved-3d-to-2d-conversion/
    public static Vector3f worldToScreenPoint(Vec3 worldPos, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();

        // Calculate the relative position to the camera
        Vec3 cameraPos = camera.getPosition();
        Vector3f relativePos = new Vector3f(
                (float)(worldPos.x - cameraPos.x),
                (float)(worldPos.y - cameraPos.y),
                (float)(worldPos.z - cameraPos.z)
        );

        // Transform the relative position using the camera's rotation
        Quaternionf cameraRotation = new Quaternionf(camera.rotation()).conjugate();
        relativePos.rotate(cameraRotation);

        // Compensate for view bobbing
        if (minecraft.options.bobView().get()) {
            LocalPlayer player = minecraft.player;
            if (player != null) {
                float f = player.walkDist - player.walkDistO;
                float f1 = -(player.walkDist + f * partialTicks);
                float f2 = Mth.lerp(partialTicks, player.oBob, player.bob);

                Quaternionf q2 = new Quaternionf().rotationX(Math.abs(Mth.cos(f1 * (float)Math.PI - 0.2F) * f2) * 5.0F);
                q2.conjugate();
                relativePos.rotate(q2);

                Quaternionf q1 = new Quaternionf().rotationZ(Mth.sin(f1 * (float)Math.PI) * f2 * 3.0F);
                q1.conjugate();
                relativePos.rotate(q1);

                Vector3f bobTranslation = new Vector3f(
                        Mth.sin(f1 * (float)Math.PI) * f2 * 0.5F,
                        -Math.abs(Mth.cos(f1 * (float)Math.PI) * f2),
                        0.0f
                );
                bobTranslation.y = -bobTranslation.y();
                relativePos.add(bobTranslation);
            }
        }

        // If the point is behind the camera, return an off-screen position
        if (relativePos.z() <= 0) {
            return new Vector3f(-1, -1, -1);
        }

        // Get screen dimensions
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Get the field of view
        float fov = getFov(minecraft, camera, partialTicks);

        // Calculate the screen position
        float halfHeight = screenHeight / 2f;
        float scale = halfHeight / (relativePos.z() * (float)Math.tan(Math.toRadians(fov / 2)));
        float screenX = -relativePos.x() * scale + screenWidth / 2f;
        float screenY = -relativePos.y() * scale + screenHeight / 2f;

        return new Vector3f(screenX, screenY, relativePos.z());
    }

    private static float getFov(Minecraft minecraft, Camera camera, float partialTicks) {
        try {
            Method getFovMethod = minecraft.gameRenderer.getClass().getDeclaredMethod("getFov", Camera.class, float.class, boolean.class);
            getFovMethod.setAccessible(true);
            return (float) getFovMethod.invoke(minecraft.gameRenderer, camera, partialTicks, true);
        } catch (Exception e) {
            // Fallback to a default FOV if the method can't be accessed
            return 70.0f; // You might want to adjust this default value
        }
    }

    public static Vec3 calculateViewVector(float xRot, float yRot) {
        float f = xRot * 0.017453292F;
        float g = -yRot * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3((double)(i * j), (double)(-k), (double)(h * j));
    }

    /**
     * Calculate the pitch and yaw angles to look from one point to another
     * @param from From Coordinate
     * @param to To Coordinate
     * @return Vector2f, x: pitch, y: yaw, unit: degree
     */
    public static Vector2f getLookAngles(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        float pitch = (float) Math.toDegrees(-Math.atan2(dy, horizontalDistance));

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        yaw = (yaw % 360 + 360) % 360;

        return new Vector2f(pitch, yaw);
    }

    public static double angleDifference(double angle1, double angle2) {
        double diff = (angle1 - angle2 + 180) % 360 - 180;
        return diff < -180 ? diff + 360 : diff;
    }

    // limit the angle to be within the range of [min, max]
    public static float clampAngle(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }


}
