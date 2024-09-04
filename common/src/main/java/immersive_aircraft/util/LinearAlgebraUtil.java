package immersive_aircraft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class LinearAlgebraUtil {

    /**
     * Converts a world position to screen coordinates.
     *
     * @param worldPos The position in the world to convert.
     * @return A Vector3f where x and y are the screen coordinates, and z is the depth.
     */
    public static Vector3f worldToScreenPoint(Vec3 worldPos) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();

        // Calculate the relative position to the camera
        Vec3 cameraPos = camera.getPosition();
        Vec3 relativePos = worldPos.subtract(cameraPos);

        // Transform the relative position using the camera's rotation
        Vector3f forward = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        float x = (float) relativePos.dot(new Vec3(left.x(), left.y(), left.z()));
        float y = (float) relativePos.dot(new Vec3(up.x(), up.y(), up.z()));
        float z = (float) relativePos.dot(new Vec3(forward.x(), forward.y(), forward.z()));

        // If the point is behind the camera, return an off-screen position
        if (z <= 0) {
            return new Vector3f(-1, -1, -1);
        }

        // Get screen dimensions
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();

        // Get the field of view and aspect ratio
        float fov = minecraft.options.fov().get().floatValue();
        float aspectRatio = (float) screenWidth / (float) screenHeight;

        // Calculate the screen position
        float fovRad = (float) Math.toRadians(fov);
        float screenX = (x / (z * (float) Math.tan(fovRad / 2))) * (screenWidth / 2) + (screenWidth / 2);
        float screenY = (y / (z * (float) Math.tan(fovRad / 2) / aspectRatio)) * (screenHeight / 2) + (screenHeight / 2);

        return new Vector3f(screenX, screenY, z);
    }
}