package com.github.badbadbadbadbad.tsundoku.views.ControlsPane;

import java.util.List;
import java.util.function.Consumer;

public class FilterConfig {
    public enum Type { DROPDOWN, NUMBER, DOUBLE_NUMBER }

    public final Type type;
    public final String label;
    public final String label2;
    public final List<String> options;
    public final List<Consumer<String>> onChange;
    public final List<Consumer<String>> onChange2;
    public final Runnable onEnter;

    public FilterConfig(Type type, String label, List<String> options, List<Consumer<String>> onChange, Runnable onEnter) {
        this(type, label, null, options, onChange, null, onEnter);
    }

    public FilterConfig(Type type,
                        String label, String label2,
                        List<String> options,
                        List<Consumer<String>> onChange1, List<Consumer<String>> onChange2,
                        Runnable onEnter) {
        this.type = type;
        this.label = label;
        this.label2 = label2;
        this.options = options;
        this.onChange = onChange1;
        this.onChange2 = onChange2;
        this.onEnter = onEnter;
    }
}