package com.github.badbadbadbadbad.tsundoku.views;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.function.Consumer;

public class Pagination extends HBox {
    private final Consumer<Integer> onPageSelected;
    private final HBox paginationButtons;

    public Pagination(int totalPages, Consumer<Integer> onPageSelected) {
        super();
        this.onPageSelected = onPageSelected;

        getStyleClass().add("pagination-wrapper");
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);

        // Wrapper (only the button box)
        paginationButtons = new HBox(10);
        paginationButtons.getStyleClass().add("pagination-buttons");
        getChildren().add(paginationButtons);

        updatePaginationButtons(1, totalPages);
    }

    /**
     * Refreshes the pagination buttons of the pagination component.
     * Runs on pagination component creating and after any API call.
     * @param selectedPage The clicked page of the API call (set to 1 on component creation and on API call of a new type
     *                     e.g. switching from Browse - Season to Browse - Top)
     * @param pages Total amount of pages
     */
    public void updatePaginationButtons(int selectedPage, int pages) {
        paginationButtons.getChildren().clear();

        // Only need "first page" button if it's not already the selected one
        if (selectedPage > 2) {
            paginationButtons.getChildren().add(createPageButton(1, selectedPage));
        }

        // Low numbers ellipsis button
        if (selectedPage > 3) {
            paginationButtons.getChildren().add(createEllipsisButton(paginationButtons, pages));
        }

        // Selected page as well as its prev and next
        for (int i = Math.max(1, selectedPage - 1); i <= Math.min(pages, selectedPage + 1); i++) {
            Button butt = createPageButton(i, selectedPage);
            if (i == selectedPage) {
                butt.getStyleClass().add("pagination-button-active");
            }
            paginationButtons.getChildren().add(butt);

        }

        // High numbers ellipsis button
        if (selectedPage < pages - 2) {
            paginationButtons.getChildren().add(createEllipsisButton(paginationButtons, pages));
        }

        // Only need "last page" button if it's not already the selected one
        if (selectedPage < pages - 1) {
            paginationButtons.getChildren().add(createPageButton(pages, selectedPage));
        }
    }

    /**
     * Creates a numbered page button for the pagination component.
     * @param ownPage The number set as the String for this button's text
     * @param selectedPage The selected page number that triggered the pagination component's refresh
     *                     (so this button knows if it's already the clicked button or not)
     * @return The finished Button
     */
    private Button createPageButton(int ownPage, int selectedPage) {
        Button pageButton =  new Button(String.valueOf(ownPage));
        pageButton.getStyleClass().add("pagination-button");

        if (!(ownPage == selectedPage)) {
            pageButton.setOnAction(event -> onPageSelected.accept(ownPage));
        }

        return pageButton;
    }

    /**
     * Creates an ellipsis page button for the pagination component (that's a "...").
     * Ellipsis buttons bridge gaps between large amount of pages (so the pagination only shows the first, current, and last pages).
     * They can also be clicked to change into a "page search" number input field.
     * @param paginationButtons The pagination component
     * @param pages Amount of max pages of the pagination component
     * @return The finished Button
     */
    private Button createEllipsisButton(HBox paginationButtons, int pages) {
        Button ellipsisButton =  new Button("...");
        ellipsisButton.getStyleClass().add("pagination-button");

        // On click, turn the ellipsis button into a number input to get to specific pages
        ellipsisButton.setOnAction(event -> {
            TextField pageInputField = createPageInputField(paginationButtons, pages);
            int index = paginationButtons.getChildren().indexOf(ellipsisButton);
            paginationButtons.getChildren().set(index, pageInputField);
            pageInputField.requestFocus();
        });

        return ellipsisButton;
    }

    /**
     * Creates a number input text field, used to replace an ellipsis button of the pagination component when it's clicked.
     * On focus loss, replaces itself with an ellipsis button again.
     * @param paginationButtons The pagination component
     * @param pages Amount of max pages of the pagination component
     * @return The finished number input field
     */
    private TextField createPageInputField(HBox paginationButtons, int pages) {
        TextField pageInputField = new TextField();
        pageInputField.getStyleClass().add("pagination-input-field");

        // Numbers only regex for page input (as pages are always numbers..)
        pageInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                pageInputField.setText(newValue.replaceAll("\\D", ""));
            }
        });

        // Handle focus loss just like enter press, invoke the page being called
        pageInputField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                handlePageInput(paginationButtons, pageInputField, pages);
            }
        });

        // Enter pressed (default value for setOnAction for textFields)
        pageInputField.setOnAction(event -> handlePageInput(paginationButtons, pageInputField, pages));

        return pageInputField;
    }

    /**
     * Triggers when a number input text field (the replacement for an activated ellipsis pagination button) is activated.
     * Invokes an API call for reasonable input.
     * @param paginationButtons The pagination component
     * @param pageInputField The number input field whose input is considered
     * @param pages Amount of max pages of the pagination component
     */
    private void handlePageInput(HBox paginationButtons, TextField pageInputField, int pages) {
        String input = pageInputField.getText();
        int index = paginationButtons.getChildren().indexOf(pageInputField);


        if (input.isEmpty()) { // If no input, just turn input field back to ellipsis button
            paginationButtons.getChildren().set(index, createEllipsisButton(paginationButtons, pages));
        } else {
            try {
                // Clamp input to [first page, last page] and handle it instead of throwing the input away
                int clampedPage = Math.clamp(Integer.parseInt(input), 1, pages);
                onPageSelected.accept(clampedPage);

            } catch (NumberFormatException e) {
                // Number formatting issues _shouldn't_ exist to my knowledge, but provide failsafe anyway
                paginationButtons.getChildren().set(index, createEllipsisButton(paginationButtons, pages));
            }

        }
    }

    public void setPaginationButtonVisibility(boolean visible) {
        this.paginationButtons.setVisible(visible);
    }
}
