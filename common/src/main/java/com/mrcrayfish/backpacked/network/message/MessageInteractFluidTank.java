package com.mrcrayfish.backpacked.network.message;

import com.mrcrayfish.backpacked.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record MessageInteractFluidTank()
{
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageInteractFluidTank> STREAM_CODEC = StreamCodec.unit(new MessageInteractFluidTank());

    public static void handle(MessageInteractFluidTank message, MessageContext context)
    {
        context.execute(() -> ServerPlayHandler.handleInteractFluidTank(message, context));
        context.setHandled(true);
    }
}
