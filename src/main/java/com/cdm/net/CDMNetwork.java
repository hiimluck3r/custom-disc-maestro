package com.cdm.net;

import com.cdm.CDMMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = CDMMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CDMNetwork {
    private CDMNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PackagingActionPayload.TYPE, PackagingActionPayload.STREAM_CODEC,
                PackagingActionPayload::handle);
        registrar.playToServer(LatheRecordPayload.TYPE, LatheRecordPayload.STREAM_CODEC,
                LatheRecordPayload::handle);
        registrar.playToServer(PressActionPayload.TYPE, PressActionPayload.STREAM_CODEC,
                PressActionPayload::handle);
    }
}
