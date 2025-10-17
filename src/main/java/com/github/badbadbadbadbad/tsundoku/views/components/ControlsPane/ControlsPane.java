package com.github.badbadbadbadbad.tsundoku.views.components.ControlsPane;

import com.github.badbadbadbadbad.tsundoku.external.FlowGridPane;
import javafx.animation.*;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;

public class ControlsPane extends VBox {

    private final @NotNull FlowGridPane filterGrid = new FlowGridPane(3, 1);
    private final @NotNull BooleanProperty filtersHidden = new SimpleBooleanProperty(true);
    private final @NotNull StringProperty searchStringProperty = new SimpleStringProperty("");

    public ControlsPane(
            @NotNull List<FilterConfig> filters,
            @NotNull List<ButtonConfig> buttons,
            @Nullable Runnable onSearch,
            @NotNull Runnable onFiltersToggled,
            @NotNull ReadOnlyDoubleProperty stageWidth,
            @NotNull BiFunction<Double, Integer, Pair<Integer, Integer>> layoutCalculator
    ) {
        getStyleClass().add("content-pane-controls");
        setMinHeight(Control.USE_PREF_SIZE);

        HBox searchAndToggle = createSearchAndFilterToggle(onSearch, onFiltersToggled);
        createFilterGrid(filters);
        HBox buttonBox = buttons.isEmpty() ? null : createButtons(buttons);

        setupResponsiveFilterGrid(stageWidth, layoutCalculator);

        getChildren().addAll(searchAndToggle, filterGrid);
        if (buttonBox != null) getChildren().add(buttonBox);
    }

    private @NotNull HBox createSearchAndFilterToggle(@Nullable Runnable onSearch, @NotNull Runnable onFiltersToggled) {
        HBox box = new HBox();
        box.getStyleClass().add("search-bar-and-filter-toggle");

        TextField searchBar = new TextField();
        searchBar.setId("search-bar");
        searchBar.setPromptText("Enter anime title...");
        HBox.setHgrow(searchBar, Priority.ALWAYS);

        searchStringProperty.bind(searchBar.textProperty());

        if (onSearch != null) {
            searchBar.setOnAction(e -> onSearch.run());
        }

        ToggleButton toggleButton = new ToggleButton("Show filters");
        toggleButton.getStyleClass().add("controls-button");

        toggleButton.setOnAction(e -> {
            toggleButton.setDisable(true);

            if (toggleButton.isSelected()) {
                toggleButton.setText("Show filters");
                showFilters(filterGrid, toggleButton, onFiltersToggled);
            } else {
                toggleButton.setText("Hide filters");
                hideFilters(filterGrid, toggleButton, onFiltersToggled);
            }
        });

        box.getChildren().addAll(searchBar, toggleButton);
        return box;
    }

    private void createFilterGrid(@NotNull List<FilterConfig> filters) {
        filterGrid.setHgap(10);
        filterGrid.setVgap(10);
        filterGrid.setMaxWidth(Double.MAX_VALUE);
        filterGrid.setMinHeight(0);
        filterGrid.setMaxHeight(0);
        filterGrid.setOpacity(0.0);

        for (FilterConfig config : filters) {
            switch (config.type()) {
                case DROPDOWN -> filterGrid.getChildren().add(createDropdownFilter(config));
                case NUMBER -> filterGrid.getChildren().add(createNumberFilter(config));
                case DOUBLE_NUMBER -> filterGrid.getChildren().add(createDoubleNumberFilter(config));
            }
        }
    }

    private @NotNull VBox createDropdownFilter(@NotNull FilterConfig config) {
        Label label = new Label(config.key().getLabel());
        label.getStyleClass().add("filter-label");

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(Objects.requireNonNull(config.key().getOptions()));
        combo.setValue(config.initialValue() != null ? config.initialValue() : config.key().getOptions().getFirst());
        combo.getStyleClass().add("filter-combo-box");
        combo.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(combo, Priority.ALWAYS);

        combo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (config.onChange() != null) {
                for (Consumer<String> consumer : config.onChange()) {
                    consumer.accept(newVal);
                }
            }
        });

        filtersHidden.addListener((obs, o, n) -> combo.setDisable(n));
        combo.setDisable(filtersHidden.get());

        return new VBox(5, label, combo);
    }

    private @NotNull VBox createNumberFilter(@NotNull FilterConfig config) {
        Label label = new Label(config.key().getLabel());
        label.getStyleClass().add("filter-label");

        TextField field = new TextField();
        field.getStyleClass().add("filter-text-box");
        field.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(field, Priority.ALWAYS);

        if (config.initialValue() != null) {
            field.setText(config.initialValue());
        }

        if (config.onChange() != null && !config.onChange().isEmpty()) {
            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.matches("\\d{0,4}")) {
                    for (Consumer<String> consumer : config.onChange()) {
                        consumer.accept(newVal);
                    }
                } else {
                    field.setText(oldVal);
                }
            });
        }

        filtersHidden.addListener((obs, o, n) -> field.setDisable(n));
        field.setDisable(filtersHidden.get());

        if (config.onEnter() != null) {
            field.setOnAction(e -> config.onEnter().run());
        }

        VBox container = new VBox(5, label, field);
        HBox.setHgrow(container, Priority.ALWAYS);
        return container;
    }

    private @NotNull HBox createDoubleNumberFilter(@NotNull FilterConfig config) {
        VBox filter1 = createNumberFilter(FilterConfig.number(config.key(), config.onChange(), config.onEnter(), config.initialValue()));
        assert config.key2() != null;
        VBox filter2 = createNumberFilter(FilterConfig.number(config.key2(), config.onChange2(), config.onEnter(), config.initialValue2()));

        HBox container = new HBox(10, filter1, filter2);
        container.setPrefWidth(200);
        return container;
    }

    private @NotNull HBox createButtons(@NotNull List<ButtonConfig> buttons) {
        HBox left = new HBox(10);
        HBox right = new HBox();

        for (ButtonConfig b : buttons) {
            if (!b.label().equalsIgnoreCase("Search")) {
                Button btn = new Button(b.label());
                btn.getStyleClass().add("controls-button");
                btn.setOnAction(e -> b.onClick().run());
                left.getChildren().add(btn);
            }
        }

        buttons.stream()
                .filter(b -> b.label().equalsIgnoreCase("Search"))
                .findFirst()
                .ifPresent(b -> {
                    Button search = new Button(b.label());
                    search.getStyleClass().add("controls-button");
                    search.setOnAction(e -> b.onClick().run());
                    right.getChildren().add(search);
                });

        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setAlignment(Pos.CENTER_LEFT);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox wrapper = new HBox(10, left, right);
        HBox.setHgrow(wrapper, Priority.ALWAYS);
        wrapper.setAlignment(Pos.CENTER);
        return wrapper;
    }

    private void hideFilters(@NotNull FlowGridPane filters, @NotNull ToggleButton toggle, @NotNull Runnable after) {
        filtersHidden.set(true);

        FadeTransition fade = new FadeTransition(millis(150), filters);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        KeyFrame visible = new KeyFrame(ZERO, new KeyValue(filters.maxHeightProperty(), filters.getHeight()));
        KeyFrame hidden = new KeyFrame(millis(100), new KeyValue(filters.maxHeightProperty(), 0));
        Timeline move = new Timeline(visible, hidden);

        KeyFrame padStart = new KeyFrame(ZERO, new KeyValue(filters.paddingProperty(), new Insets(0, 0, 15, 0)));
        KeyFrame padEnd = new KeyFrame(millis(100), new KeyValue(filters.paddingProperty(), new Insets(0, 0, 0, 0)));
        Timeline movePad = new Timeline(padStart, padEnd);

        PauseTransition cooldown = new PauseTransition(millis(200));
        cooldown.setOnFinished(e -> toggle.setDisable(false));

        ParallelTransition hide = new ParallelTransition(move, movePad);
        SequentialTransition seq = new SequentialTransition(fade, hide);
        ParallelTransition all = new ParallelTransition(cooldown, seq);

        all.setOnFinished(e -> after.run());
        all.play();
    }

    private void showFilters(@NotNull FlowGridPane filters, @NotNull ToggleButton toggle, @NotNull Runnable after) {
        filtersHidden.set(false);

        FadeTransition fade = new FadeTransition(millis(150), filters);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        KeyFrame hidden = new KeyFrame(ZERO, new KeyValue(filters.maxHeightProperty(), 0));
        KeyFrame visible = new KeyFrame(millis(100), new KeyValue(filters.maxHeightProperty(), filters.prefHeight(filters.getWidth())));
        Timeline move = new Timeline(hidden, visible);

        KeyFrame padStart = new KeyFrame(ZERO, new KeyValue(filters.paddingProperty(), new Insets(0, 0, 0, 0)));
        KeyFrame padEnd = new KeyFrame(millis(100), new KeyValue(filters.paddingProperty(), new Insets(0, 0, 15, 0)));
        Timeline movePad = new Timeline(padStart, padEnd);

        PauseTransition cooldown = new PauseTransition(millis(200));
        cooldown.setOnFinished(e -> toggle.setDisable(false));

        ParallelTransition core = new ParallelTransition(move, movePad);
        SequentialTransition seq = new SequentialTransition(core, fade);
        ParallelTransition all = new ParallelTransition(cooldown, seq);

        core.setOnFinished(e -> filters.setMaxHeight(filters.prefHeight(filters.getWidth())));
        all.setOnFinished(e -> after.run());
        all.play();
    }

    private void setupResponsiveFilterGrid(@NotNull ReadOnlyDoubleProperty widthProperty,
                                           @NotNull BiFunction<Double, Integer, Pair<Integer, Integer>> layoutCalculator) {
        int filterCount = filterGrid.getChildren().size();

        widthProperty.addListener((obs, oldVal, newVal) -> applyResponsiveLayout(newVal.doubleValue(), filterCount, layoutCalculator));

        applyResponsiveLayout(widthProperty.get(), filterCount, layoutCalculator);
    }

    private void applyResponsiveLayout(double windowWidth,
                                       int filterCount,
                                       @NotNull BiFunction<Double, Integer, Pair<Integer, Integer>> layoutCalculator) {
        Pair<Integer, Integer> layout = layoutCalculator.apply(windowWidth, filterCount);

        int cols = layout.getKey();
        int rows = layout.getValue();

        filterGrid.setColsCount(cols);
        filterGrid.setRowsCount(rows);

        if (!filtersHidden.get()) {
            filterGrid.setMaxHeight(filterGrid.prefHeight(filterGrid.getWidth()));
        }
    }

    public @NotNull StringProperty getSearchStringProperty() {
        return searchStringProperty;
    }
}