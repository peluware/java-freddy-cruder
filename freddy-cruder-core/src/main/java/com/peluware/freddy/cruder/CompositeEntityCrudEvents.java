package com.peluware.freddy.cruder;

import com.peluware.domain.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Dispatches every event to its delegates in order, stopping at the first one that throws.
 */
final class CompositeEntityCrudEvents<ENTITY, ID, INPUT> implements EntityCrudEvents<ENTITY, ID, INPUT> {

    private final List<EntityCrudEvents<ENTITY, ID, INPUT>> delegates;

    private CompositeEntityCrudEvents(List<EntityCrudEvents<ENTITY, ID, INPUT>> delegates) {
        this.delegates = delegates;
    }

    static <ENTITY, ID, INPUT> EntityCrudEvents<ENTITY, ID, INPUT> compose(Iterable<? extends EntityCrudEvents<ENTITY, ID, INPUT>> delegates) {
        var flattened = new ArrayList<EntityCrudEvents<ENTITY, ID, INPUT>>();
        for (EntityCrudEvents<ENTITY, ID, INPUT> delegate : delegates) {
            Objects.requireNonNull(delegate, "Delegate must not be null");
            if (delegate == EntityCrudEvents.DEFAULT) {
                continue;
            }
            if (delegate instanceof CompositeEntityCrudEvents<ENTITY, ID, INPUT> composite) {
                flattened.addAll(composite.delegates);
            } else {
                flattened.add(delegate);
            }
        }
        return switch (flattened.size()) {
            case 0 -> EntityCrudEvents.getDefault();
            case 1 -> flattened.getFirst();
            default -> new CompositeEntityCrudEvents<>(List.copyOf(flattened));
        };
    }

    @Override
    public void onFind(ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onFind(entity);
        }
    }

    @Override
    public void onCount(long count) {
        for (var delegate : delegates) {
            delegate.onCount(count);
        }
    }

    @Override
    public void onExists(boolean exists, ID id) {
        for (var delegate : delegates) {
            delegate.onExists(exists, id);
        }
    }

    @Override
    public void onPage(Page<ENTITY> page) {
        for (var delegate : delegates) {
            delegate.onPage(page);
        }
    }

    @Override
    public void onBeforeCreate(INPUT input, ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onBeforeCreate(input, entity);
        }
    }

    @Override
    public void onBeforeUpdate(INPUT input, ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onBeforeUpdate(input, entity);
        }
    }

    @Override
    public void onBeforeDelete(ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onBeforeDelete(entity);
        }
    }

    @Override
    public void onAfterCreate(INPUT input, ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onAfterCreate(input, entity);
        }
    }

    @Override
    public void onAfterUpdate(INPUT input, ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onAfterUpdate(input, entity);
        }
    }

    @Override
    public void onAfterDelete(ENTITY entity) {
        for (var delegate : delegates) {
            delegate.onAfterDelete(entity);
        }
    }

    @Override
    public void eachEntity(ENTITY entity) {
        for (var delegate : delegates) {
            delegate.eachEntity(entity);
        }
    }
}
