package com.mrcrayfish.backpacked.client.augment;

import com.mrcrayfish.backpacked.common.augment.Augments.Position;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AugmentHolder<T>
{
    private final Supplier<T> supplier;
    private final Consumer<T> updater;
    private final Position position;
    private final int backpackIndex;

    public AugmentHolder(Supplier<T> supplier, Consumer<T> updater, Position position, int backpackIndex)
    {
        this.supplier = supplier;
        this.updater = updater;
        this.position = position;
        this.backpackIndex = backpackIndex;
    }

    public T get()
    {
        return this.supplier.get();
    }

    public void update(T augment)
    {
        this.updater.accept(augment);
    }

    public Position position()
    {
        return this.position;
    }

    public int backpackIndex()
    {
        return this.backpackIndex;
    }
}
