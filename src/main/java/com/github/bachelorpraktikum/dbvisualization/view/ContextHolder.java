package com.github.bachelorpraktikum.dbvisualization.view;

import com.github.bachelorpraktikum.dbvisualization.model.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Singleton class that stores an instance of {@link Context}.
 */
public class ContextHolder {
    private static ContextHolder instance = new ContextHolder();

    public static ContextHolder getInstance() {
        return instance;
    }

    private Context context;

    private ContextHolder() {
    }

    /**
     * Sets the currently active context.
     *
     * @param context the context
     */
    void setContext(@Nullable Context context) {
        this.context = context;
    }

    /**
     * Determines whether a context currently exists.
     *
     * @return whether there is a context
     */
    boolean hasContext() {
        return context != null;
    }

    /**
     * Gets the current context.
     *
     * @return the context
     * @throws IllegalStateException if there is no context
     */
    @Nonnull
    Context getContext() {
        if (context == null) {
            throw new IllegalStateException();
        }
        return context;
    }
}
