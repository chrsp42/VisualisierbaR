package com.github.bachelorpraktikum.visualisierbar.model;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.shape.Polygon;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Represents a switch consisting of exactly 3 {@link Element elements} of type {@link
 * Element.Type#WeichenPunkt}.
 */
@ParametersAreNonnullByDefault
public final class Switch implements Shapeable<Polygon> {

    @Nonnull
    private final List<Element> elements;
    private final Property<VisibleState> stateProperty;

    private Switch() {
        elements = new ArrayList<>(3);
        stateProperty = new SimpleObjectProperty<>();
    }

    private void addElement(Element element) {
        if (elements.size() == 3) {
            throw new IllegalStateException("only 3 elements per switch allowed");
        }
        elements.add(element);
    }

    /**
     * Gets a list containing the 3 {@link Element elements} this {@link Switch} consists of.
     *
     * @return the elements
     * @throws IllegalStateException if this switch has not been completely initialized
     */
    @Nonnull
    public List<Element> getElements() {
        if (elements.size() < 3) {
            throw new IllegalStateException(
                "Switch not completely initialized. Elements: " + elements);
        }
        return Collections.unmodifiableList(elements);
    }

    /**
     * Gets the main element of this switch.
     *
     * @return the main element
     */
    @Nonnull
    public Element getMainElement() {
        return elements.stream()
            .filter(element -> element.getNode().getEdges().size() == 3)
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    }

    @Override
    public String toString() {
        return "Switch{"
            + "elements=" + elements
            + '}';
    }

    /**
     * Gets the {@link Factory} instance for the given {@link Context}.
     *
     * @param context the context
     * @return the factory
     * @throws NullPointerException if context is null
     */
    @Nonnull
    static Factory in(Context context) {
        return Factory.getInstance(context);
    }

    @Nonnull
    @Override
    public String getName() {
        return toString();
    }

    @Nonnull
    @Override
    public Polygon createShape() {
        return new Polygon();
    }

    @Nonnull
    @Override
    public Property<VisibleState> visibleStateProperty() {
        return stateProperty;
    }

    static final class Factory {

        private static final Map<Context, WeakReference<Factory>> instances = new WeakHashMap<>();
        @Nullable
        private Switch currentSwitch;

        private static Factory getInstance(Context context) {
            if (context == null) {
                throw new NullPointerException("context is null");
            }

            Factory result = instances.computeIfAbsent(context, ctx -> {
                Factory factory = new Factory();
                ctx.addObject(factory);
                return new WeakReference<>(factory);
            }).get();

            if (result == null) {
                throw new IllegalStateException();
            }
            return result;
        }

        private Factory() {
        }

        @Nonnull
        Switch create(Element element) {
            return buildSwitch(element);
        }

        @Nonnull
        private Switch buildSwitch(Element element) {
            if (currentSwitch == null) {
                currentSwitch = new Switch();
            }

            Switch result = currentSwitch;

            result.addElement(element);
            if (result.elements.size() == 3) {
                currentSwitch = null;
            }
            return result;
        }
    }
}
