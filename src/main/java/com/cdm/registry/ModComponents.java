package com.cdm.registry;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import com.cdm.CDMMod;
import com.cdm.data.DiscDesign;
import com.cdm.data.DiscMeta;
import com.cdm.data.RecordContent;
import com.cdm.data.SleeveDesign;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom data components. Persistent + network-synchronized. Note melodies live in {@link RecordContent}
 * directly on the disc (compact); designs/metadata are small text+colors. No large blobs are stored.
 */
public final class ModComponents {
    private ModComponents() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CDMMod.MODID);

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    /** Note melody (both sides). Carried by master/matrix/disc. */
    public static final Supplier<DataComponentType<RecordContent>> RECORD_CONTENT = register("record_content",
            b -> b.persistent(RecordContent.CODEC).networkSynchronized(RecordContent.STREAM_CODEC));

    /** Author / title / album for a disc. */
    public static final Supplier<DataComponentType<DiscMeta>> DISC_META = register("disc_meta",
            b -> b.persistent(DiscMeta.CODEC).networkSynchronized(DiscMeta.STREAM_CODEC));

    /** Sleeve/cover visual design. */
    public static final Supplier<DataComponentType<SleeveDesign>> SLEEVE_DESIGN = register("sleeve_design",
            b -> b.persistent(SleeveDesign.CODEC).networkSynchronized(SleeveDesign.STREAM_CODEC));

    /** Pressed-disc visual design (vinyl colour, label colour, style). Absent = raw white master. */
    public static final Supplier<DataComponentType<DiscDesign>> DISC_DESIGN = register("disc_design",
            b -> b.persistent(DiscDesign.CODEC).networkSynchronized(DiscDesign.STREAM_CODEC));

    /** The disc currently held inside a sleeve (any music disc — ours or vanilla). Empty = none. Uses
     *  vanilla ItemContainerContents because a raw ItemStack can't be a component value. */
    public static final Supplier<DataComponentType<ItemContainerContents>> CONTAINED_RECORD = register("contained_record",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** Whether a packaged sleeve is shrink-wrapped (read-only until opened). */
    public static final Supplier<DataComponentType<Boolean>> SHRINK_WRAPPED = register("shrink_wrapped",
            b -> b.persistent(com.mojang.serialization.Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Groove wear (0..100) baked into a matrix from the master it was grown off, so pressed records
     *  inherit the master's degradation (a broken master makes a matrix that stamps broken records). */
    public static final Supplier<DataComponentType<Integer>> BAKED_WEAR = register("baked_wear",
            b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
}
