package com.mrcrayfish.backpacked.network.message;

import com.mrcrayfish.backpacked.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record MessageUseBedAugment()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageUseBedAugment> STREAM_CODEC = StreamCodec.unit(new MessageUseBedAugment());

    public static void handle(MessageUseBedAugment message, MessageContext context)
    {
        context.execute(() -> ServerPlayHandler.handleUseBedAugment(message, context));
        context.setHandled(true);
    }
}
