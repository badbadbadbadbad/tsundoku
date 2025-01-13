package com.github.badbadbadbadbad.tsundoku.external;

import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.Priority;
import javafx.geometry.HPos;
import javafx.geometry.VPos;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.NamedArg;

/**
 *
 * This is a customized version of the custom FlowGridPane class by SasQ.
 * While the flow behaviour is the same, the resize behaviour is changed.
 * FlowGridPane lets its children grow, while FlowGapPane lets the hGaps grow on width resizing.
 *
 * Original FlowGridPane class:
 * https://stackoverflow.com/a/37032568
 *
 * This class subclasses the GridPane layout class.
 * It manages its child nodes by arranging them in rows of equal number of tiles.
 * Their order in the grid corresponds to their indexes in the list of children
 * in the following fashion (similarly to how FlowPane works):
 *
 *      +---+---+---+---+
 *      | 0 | 1 | 2 | 3 |
 *      +---+---+---+---+
 *      | 4 | 5 | 6 | 7 |
 *      +---+---+---+---+
 *      | 8 | 9 | … |   |
 *      +---+---+---+---+
 *
 * It observes its internal list of children and it automatically reflows them
 * if the number of columns changes or if you add/remove some children from the list.
 */
public class FlowGapPane extends GridPane
{

    private ScrollPane scrollPane;

    // Properties for managing the number of rows & columns.
    private IntegerProperty rowsCount;
    private IntegerProperty colsCount;
    private double fixedTileWidth;
    private double fixedTileHeight;
    private double minHGap;

    public final IntegerProperty colsCountProperty() { return colsCount; }
    public final Integer getColsCount() { return colsCountProperty().get(); }
    public final void setColsCount(final Integer cols) {
        ObservableList<ColumnConstraints> constraints = getColumnConstraints();
        constraints.clear();
        for (int i=0; i < cols; ++i) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHalignment(HPos.CENTER);
            c.setMinWidth(fixedTileWidth);
            c.setMaxWidth(fixedTileWidth);
            constraints.add(c);
        }
        colsCountProperty().set(cols);
    }

    public final IntegerProperty rowsCountProperty() { return rowsCount; }
    public final Integer getRowsCount() { return rowsCountProperty().get(); }
    public final void setRowsCount(final Integer rows) {
        ObservableList<RowConstraints> constraints = getRowConstraints();
        constraints.clear();
        for (int i=0; i < rows; ++i) {
            RowConstraints r = new RowConstraints();
            r.setValignment(VPos.CENTER);
            r.setMinHeight(fixedTileHeight);
            r.setMaxHeight(fixedTileHeight);
            constraints.add(r);
        }
        rowsCountProperty().set(rows);
    }

    /// Constructor. Rows and columns are calculated by the FlowGapPane based on the provided item size and minimum hGap.
    public FlowGapPane(double tileWidth, double tileHeight, double minHGap) {
        super();
        colsCount = new SimpleIntegerProperty();  setColsCount(1);
        rowsCount = new SimpleIntegerProperty();  setRowsCount(1);
        this.fixedTileWidth = tileWidth;
        this.fixedTileHeight = tileHeight;
        this.minHGap = minHGap;
        this.setVgap(20);
        getChildren().addListener(new ListChangeListener<Node>() {
            public void onChanged(ListChangeListener.Change<? extends Node> change) {
                reflowAll();
            }
        } );
    }

    public void setFixedTileSize(double width, double height) {
        this.fixedTileWidth = width;
        this.fixedTileHeight = height;
        reflowAll();
    }

    /** Provides this FlowGapPane with a parent scrollPane wrapping it.
     * The FlowGapPane's width value seems buggy otherwise, growing infinitely.
     * The easiest fix was to just bind it to the scrollPane width.
     * @param scrollPane
     */
    public void setWrapperPane(ScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        scrollPane.widthProperty().addListener((obs, oldValue, newValue) -> {

            // The extra "15" is because JavaFX ScrollPanes seem to calculate width a little oddly?
            // I assume the width of the content area, scroll bar area etc. don't work like I expect.
            // Still, this arbitrary value makes it look okay, so it's probably fine.
            Insets insets = scrollPane.getInsets();
            double availableWidth = newValue.doubleValue() - insets.getRight() - insets.getLeft() - 15;

            this.setMinWidth(availableWidth);
            this.setMaxWidth(availableWidth);
            reflowAll();

        });
    }

    // Helper functions for coordinate conversions.
    private int coordsToOffset(int col, int row) { return row*colsCount.get() + col; }
    private int offsetToCol(int offset) { return offset%colsCount.get(); }
    private int offsetToRow(int offset) { return offset/colsCount.get(); }


    /**
     * This function takes care of the "Flow". Column amount and hGaps are recalculated based on the current width, then
     * children of this grid are placed in the GridPane in a FlowPane behaviour.
     */
    public void reflowAll() {

        // Sanity check. JavaFX layout sizing is a little wacky while layout calculation is still running.
        // This wouldn't be an issue, but these values can turn zero or negative, which is absolutely a problem
        // when I do calculations based on the width here.
        // We just skip the reflow if the width is currently in such an intermediate flow state.
        double paneWidth = this.getMaxWidth();
        if (paneWidth < 250) {
            return;
        }

        int childAmount = getChildren().size();
        int columnAmount = (int) ((paneWidth + minHGap) / (fixedTileWidth + minHGap));
        int rowAmount = (int) Math.ceil((double) childAmount / columnAmount);
        double hgap = ( (paneWidth - columnAmount * fixedTileWidth) / (columnAmount - 1) );

        setColsCount(columnAmount);
        setRowsCount(rowAmount);
        this.setHgap(hgap);

        ObservableList<Node> children = getChildren();

        for (int offs = 0; offs < children.size(); offs++) {
            Node child = children.get(offs);
            GridPane.setConstraints(child, offsetToCol(offs), offsetToRow(offs));
        }
    }
}