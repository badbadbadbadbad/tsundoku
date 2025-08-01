package com.github.badbadbadbadbad.tsundoku.views.ControlsPane;

import java.util.List;
import java.util.function.Consumer;

public record FilterConfig(
        Type type,
        String label,
        String label2,
        List<String> options,
        List<Consumer<String>> onChange,
        List<Consumer<String>> onChange2,
        Runnable onEnter,
        String initialValue,
        String initialValue2
) {
    public enum Type { DROPDOWN, NUMBER, DOUBLE_NUMBER }

    // Static constructors

    public static FilterConfig dropdown(String label,
                                        List<String> options,
                                        List<Consumer<String>> onChange,
                                        String initialValue) {
        return new FilterConfig(Type.DROPDOWN, label, null, options, onChange, null, null, initialValue, null);
    }

    public static FilterConfig number(String label,
                                      List<Consumer<String>> onChange,
                                      Runnable onEnter,
                                      String initialValue) {
        return new FilterConfig(Type.NUMBER, label, null, null, onChange, null, onEnter, initialValue, null);
    }

    public static FilterConfig doubleNumber(String label1, String label2,
                                            List<Consumer<String>> onChange1,
                                            List<Consumer<String>> onChange2,
                                            Runnable onEnter,
                                            String initial1,
                                            String initial2) {
        return new FilterConfig(Type.DOUBLE_NUMBER, label1, label2, null, onChange1, onChange2, onEnter, initial1, initial2);
    }
}