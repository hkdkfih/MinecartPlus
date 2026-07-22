package dev.ironcraft.minecart.client;

import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class CustomRailBlockModel extends WrapperBlockStateModel {
    private static final ThreadLocal<SpriteRemapTransform> REMAP_TRANSFORM =
            ThreadLocal.withInitial(SpriteRemapTransform::new);
    private static volatile TextureResources textureResources;
    private final Object[] geometryKeys = new Object[RailType.values().length];

    public CustomRailBlockModel(BlockStateModel wrapped) {
        super(wrapped);
        for (RailType type : RailType.values()) {
            geometryKeys[type.ordinal()] = new GeometryKey(this, type);
        }
        // Model wrappers are recreated on resource reload. Force a lazy atlas
        // refresh before the first mesh is compiled with the new resources.
        textureResources = null;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter blockView,
            BlockPos position,
            BlockState state,
            RandomSource random,
            Predicate<Direction> cullTest
    ) {
        RailType type = typeAtCurrentPosition(position);
        if (type == null) {
            wrapped.emitQuads(emitter, blockView, position, state, random, cullTest);
            return;
        }

        boolean powered = state.getValue(PoweredRailBlock.POWERED);
        TextureResources textures = textures();
        SpriteRemapTransform transform = REMAP_TRANSFORM.get();
        transform.configure(textures.vanilla(powered), textures.custom(type, powered));
        emitter.pushTransform(transform);
        try {
            wrapped.emitQuads(emitter, blockView, position, state, random, cullTest);
        } finally {
            emitter.popTransform();
            transform.clear();
        }
    }

    @Override
    public Object createGeometryKey(
            BlockAndTintGetter blockView,
            BlockPos position,
            BlockState state,
            RandomSource random
    ) {
        RailType type = typeAtCurrentPosition(position);
        return type == null ? null : geometryKeys[type.ordinal()];
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter blockView, BlockPos position, BlockState state) {
        RailType type = typeAtCurrentPosition(position);
        if (type == null) {
            return wrapped.particleMaterial(blockView, position, state);
        }
        return textures().particle(type, state.getValue(PoweredRailBlock.POWERED));
    }

    private static boolean remapSprite(
            MutableQuadView quad,
            TextureAtlasSprite source,
            TextureAtlasSprite target
    ) {
        float sourceWidth = source.getU1() - source.getU0();
        float sourceHeight = source.getV1() - source.getV0();
        if (sourceWidth == 0.0F || sourceHeight == 0.0F) {
            return true;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            float normalizedU = (quad.u(vertex) - source.getU0()) / sourceWidth;
            float normalizedV = (quad.v(vertex) - source.getV0()) / sourceHeight;
            quad.uv(vertex, target.getU(normalizedU), target.getV(normalizedV));
        }
        // The custom sprites have the same alpha mask and use the same block
        // atlas as vanilla powered rails. Preserve the already-computed quad
        // material: Sodium's FRAPI wrapper cannot safely recompute it here.
        return true;
    }

    private static RailType typeAtCurrentPosition(BlockPos position) {
        Minecraft client = Minecraft.getInstance();
        return client.level == null
                ? null
                : MinecartPlusClient.RAILS.typeAt(client.level.dimension().identifier(), position);
    }

    private static TextureResources textures() {
        TextureResources current = textureResources;
        if (current != null) {
            return current;
        }
        synchronized (CustomRailBlockModel.class) {
            current = textureResources;
            if (current == null) {
                TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
                current = new TextureResources(atlas);
                textureResources = current;
            }
            return current;
        }
    }

    private static final class SpriteRemapTransform implements QuadTransform {
        private TextureAtlasSprite source;
        private TextureAtlasSprite target;

        private void configure(TextureAtlasSprite source, TextureAtlasSprite target) {
            this.source = source;
            this.target = target;
        }

        private void clear() {
            source = null;
            target = null;
        }

        @Override
        public boolean transform(MutableQuadView quad) {
            return remapSprite(quad, source, target);
        }
    }

    private static final class TextureResources {
        private static final net.minecraft.resources.Identifier VANILLA_OFF =
                net.minecraft.resources.Identifier.withDefaultNamespace("block/powered_rail");
        private static final net.minecraft.resources.Identifier VANILLA_ON =
                net.minecraft.resources.Identifier.withDefaultNamespace("block/powered_rail_on");

        private final TextureAtlasSprite[] vanilla = new TextureAtlasSprite[2];
        private final TextureAtlasSprite[][] custom = new TextureAtlasSprite[RailType.values().length][2];
        private final Material.Baked[][] particles = new Material.Baked[RailType.values().length][2];

        private TextureResources(TextureAtlas atlas) {
            vanilla[0] = atlas.getSprite(VANILLA_OFF);
            vanilla[1] = atlas.getSprite(VANILLA_ON);
            for (RailType type : RailType.values()) {
                for (int powered = 0; powered < 2; powered++) {
                    TextureAtlasSprite sprite = atlas.getSprite(type.texture(powered == 1));
                    custom[type.ordinal()][powered] = sprite;
                    particles[type.ordinal()][powered] = new Material.Baked(sprite, false);
                }
            }
        }

        private TextureAtlasSprite vanilla(boolean powered) {
            return vanilla[powered ? 1 : 0];
        }

        private TextureAtlasSprite custom(RailType type, boolean powered) {
            return custom[type.ordinal()][powered ? 1 : 0];
        }

        private Material.Baked particle(RailType type, boolean powered) {
            return particles[type.ordinal()][powered ? 1 : 0];
        }
    }

    private record GeometryKey(CustomRailBlockModel model, RailType type) {
    }
}
