package at.petrak.hexcasting.client.render;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.blocks.akashic.BlockAkashicBookshelf;
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.blocks.circles.BlockSlate;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Helper methods for rendering patterns in the world.
 */
public class WorldlyPatternRenderHelpers {

    public static final PatternRenderSettings WORLDLY_RENDER_SETTINGS = new PatternRenderSettings()
            .withSizings(PatternRenderSettings.FitAxis.BOTH, 1.0, 1.0, 1.0/16, 1.0/16, 0.25, null, null,
                    (scale) -> 0.4f/16, (scale) -> 0.8f/16)
            .named("worldly");

    public static final PatternRenderSettings SCROLL_RENDER_SETTINGS = WORLDLY_RENDER_SETTINGS.withSizings(null, null, null,
            2.0/16, 2.0/16, null, null, null, null, null)
            .named("wallscroll");

    public static final PatternRenderSettings READABLE_SCROLL_RENDER_SETTINGS = SCROLL_RENDER_SETTINGS.withZappySettings(
            null, null, null, null, RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP
            )
            .named("wallscroll_readable");

    public static final PatternRenderSettings SLATE_WOMBLY_SETTINGS = WORLDLY_RENDER_SETTINGS.withZappySettings(
            null, 2.5f, 0.1f, null, null, null)
            .named("slate_wobbly");

    public static final PatternColors DEFAULT_PATTERN_COLOR = new PatternColors(0xc8_322b33, 0xff_d2c8c8);

    public static final PatternColors SLATE_WOBBLY_COLOR = new PatternColors(RenderLib.screenCol(0xff_64c8ff), 0xff_64c8ff);

    public static void renderPatternForScroll(HexPattern pattern, EntityWallScroll scroll, PoseStack ps, MultiBufferSource bufSource, int light, int blockSize, boolean showStrokeOrder)
    {
        ps.pushPose();
        renderPattern(pattern, showStrokeOrder ? READABLE_SCROLL_RENDER_SETTINGS : SCROLL_RENDER_SETTINGS, DEFAULT_PATTERN_COLOR, scroll.getPos().hashCode(), ps, bufSource, light, blockSize, true, false,false, true,-1);
        ps.popPose();
    }

    public static void renderPatternForSlate(BlockEntitySlate tile, HexPattern pattern, PoseStack ps, MultiBufferSource buffer, int light, BlockState bs)
    {

        boolean isOnWall = bs.getValue(BlockSlate.ATTACH_FACE) == AttachFace.WALL;
        boolean isOnCeiling = bs.getValue(BlockSlate.ATTACH_FACE) == AttachFace.CEILING;
        int facing = bs.getValue(BlockSlate.FACING).get2DDataValue();

        boolean wombly = bs.getValue(BlockSlate.ENERGIZED);

        renderPatternForBlockEntity(pattern, tile,
                wombly ? SLATE_WOMBLY_SETTINGS : WORLDLY_RENDER_SETTINGS,
                wombly ? SLATE_WOBBLY_COLOR : DEFAULT_PATTERN_COLOR,
                ps, buffer, light, isOnWall, isOnCeiling, true, facing);
    }

    public static void renderPatternForAkashicBookshelf(BlockEntityAkashicBookshelf tile, HexPattern pattern, PoseStack ps, MultiBufferSource buffer, int light, BlockState bs)
    {
        int facing = bs.getValue(BlockAkashicBookshelf.FACING).get2DDataValue();
        renderPatternForBlockEntity(pattern, tile, WORLDLY_RENDER_SETTINGS, DEFAULT_PATTERN_COLOR, ps, buffer, LightTexture.FULL_BRIGHT, true, false, false, facing);
    }

    public static void renderPatternForBlockEntity(HexPattern pattern, HexBlockEntity tile, PatternRenderSettings patSets, PatternColors patColors, PoseStack ps, MultiBufferSource buffer, int light, boolean isOnWall, boolean isOnCeiling, boolean isSlate, int facing)
    {
        var oldShader = RenderSystem.getShader();
        ps.pushPose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        renderPattern(pattern, patSets, patColors, tile.getBlockPos().hashCode(), ps, buffer, light, 1, isOnWall, isOnCeiling, isSlate, false, facing);
        ps.popPose();
        RenderSystem.setShader(() -> oldShader);
    }

    // TODO: clean up the args of this method, maybe make the whole PSTransformer thing less gross, move it to be just code in the methods or something maybe.

    /**
     * Renders a pattern in world space based on the given transform requirements
     */
    public static void renderPattern(HexPattern pattern, PatternRenderSettings patSets, PatternColors patColors, double seed, PoseStack ps, MultiBufferSource bufSource, int light, int blockSize, boolean isOnWall, boolean isOnCeiling, boolean isSlate, boolean isScroll, int facing)
    {
        ps.pushPose();

        PoseStack.Pose last = ps.last();
        Matrix4f mat = last.pose();
        Matrix3f normal = last.normal();

        float x = blockSize, y = blockSize, z = (-1f / 16f) - 0.01f;
        float nx = 0, ny = 0, nz = 0;

        PSTransformer transformer;

        if(isOnWall)
        {
            if(isScroll)
            {
                transformer = wallScrollTransformer;
            }
            else
            {
                if(isSlate)
                {
                    transformer = wallSlateTransformer;
                }
                else
                {
                    transformer = wallBlockTransformer;
                    z = -0.02f;
                }
            }
        }
        else //slates on the floor or ceiling
        {
            transformer = isOnCeiling ? ceilSlateTransformer : floorSlateTransformer;
        }

        Vec3 nVec = transformer.transform(ps, facing, blockSize);
        nx = (float)nVec.x();
        ny = (float)nVec.y();
        nz = (float)nVec.z();

        ps.scale(blockSize, blockSize, 1);

        int lineWidth = PatternTextureManager.otherLineWidth;
        int outerColor = 0xff_d2c8c8;
        int innerColor = 0xc8_322b33;
        if(isScroll)
            lineWidth = PatternTextureManager.scrollLineWidth;

//        ps.scale(x, y, z);
        ps.translate(0,0, z);
//        VertexConsumer verts = bufSource.getBuffer(RenderType.entityCutout(TheCoolerRenderLib.WHITE));
        PatternRenderer.renderPattern(pattern, ps, bufSource, patSets, patColors, seed, light, nVec, blockSize * PatternTextureManager.resolutionByBlockSize);
//        PatternRenderer.renderPattern(pattern, ps, null, patSets, patColors, seed, light, nVec);

//        ResourceLocation texture = PatternTextureManager.getTexture(zappyPoints, pointsKey, blockSize, showStrokeOrder, lineWidth, useFullSize, new Color(innerColor), new Color(outerColor));
//        VertexConsumer verts = bufSource.getBuffer(RenderType.entityCutout(texture));
//
//        vertex(mat, normal, light, verts, 0, 0, z, 0, 0, nx, ny, nz);
//        vertex(mat, normal, light, verts, 0, y, z, 0, 1, nx, ny, nz);
//        vertex(mat, normal, light, verts, x, y, z, 1, 1, nx, ny, nz);
//        vertex(mat, normal, light, verts, x, 0, z, 1, 0, nx, ny, nz);

        ps.popPose();
    }



//    public static HexPatternPoints generateHexPatternPoints(HexBlockEntity tile, HexPattern pattern, float flowIrregular)
//    {
//        var stupidHash = tile.getBlockPos().hashCode();
//        var lines1 = pattern.toLines(1, Vec2.ZERO);
//        var zappyPoints = RenderLib.makeZappy(lines1, RenderLib.findDupIndices(pattern.positions()),
//                10, 0.5f, 0f, flowIrregular, 0f, 1f, stupidHash);
//        return new HexPatternPoints(zappyPoints);
//    }

    private static void vertex(Matrix4f mat, Matrix3f normal, int light, VertexConsumer verts, float x, float y, float z,
                                 float u, float v, float nx, float ny, float nz) {
        verts.vertex(mat, x, y, z)
                .color(0xffffffff)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, nx, ny, nz)
                .endVertex();
    }


    @FunctionalInterface
    public interface PSTransformer{
        Vec3 transform(PoseStack ps, int facing, int blocksize);
    }

    public static final PSTransformer wallScrollTransformer = (ps, facing, blockSize) -> {
        ps.translate(-blockSize / 2f, -blockSize / 2f, 1f / 32f);
        return new Vec3(0, 0, -1);
    };

    public static final PSTransformer floorSlateTransformer = (ps, facing, blockSize) -> {
        if(facing == 0)
            ps.translate(0,0,0);
        if(facing == 1)
            ps.translate(1,0,0);
        if(facing == 2)
            ps.translate(1,0,1);
        if(facing == 3)
            ps.translate(0,0,1);

        ps.mulPose(Axis.YP.rotationDegrees(facing*-90));
        ps.mulPose(Axis.XP.rotationDegrees(90));

        return new Vec3(0, 0, -1);
    };

    public static final PSTransformer ceilSlateTransformer = (ps, facing, blockSize) -> {
        if(facing == 0)
            ps.translate(0,0,0);
        if(facing == 1)
            ps.translate(1,0,0);
        if(facing == 2)
            ps.translate(1,0,1);
        if(facing == 3)
            ps.translate(0,0,1);

        ps.mulPose(Axis.YP.rotationDegrees(facing*-90));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.translate(0,-1,1);

        return new Vec3(0, 0, -1);
    };

    public static final PSTransformer wallSlateTransformer = (ps, facing, blocksize) -> {
        ps.mulPose(Axis.ZP.rotationDegrees(180));

        if(facing == 0){
            ps.translate(0,-1,0);
            ps.mulPose(Axis.YP.rotationDegrees(180));
            return new Vec3(0, 0, -1);
        }
        if(facing == 1){
            ps.translate(-1,-1,0);
            ps.mulPose(Axis.YP.rotationDegrees(270));
            return new Vec3(-1, 0, 0);
        }
        if(facing == 2){
            ps.translate(-1,-1,1);
            return new Vec3(0, 0, -1);
        }
        if(facing == 3){
            ps.translate(0,-1,1);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            return new Vec3(-1, 0, 0);
        }
        return new Vec3(0,0,0);
    };

    public static final PSTransformer wallBlockTransformer = (ps, facing, blocksize) -> {
        ps.mulPose(Axis.ZP.rotationDegrees(180));

        if(facing == 0){
            ps.translate(0,-1,1);
            ps.mulPose(Axis.YP.rotationDegrees(180));
            return new Vec3(0, 0, -1);
        }
        if(facing == 1){
            ps.translate(0,-1,0);
            ps.mulPose(Axis.YP.rotationDegrees(270));
            return new Vec3(-1, 0, 0);
        }
        if(facing == 2){
            ps.translate(-1,-1,0);
            return new Vec3(0, 0, -1);
        }
        if(facing == 3){
            ps.translate(-1,-1,1);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            return new Vec3(-1, 0, 0);
        }
        return new Vec3(0,0,0);
    };
}
