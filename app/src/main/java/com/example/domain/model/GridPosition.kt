package com.example.domain.model

data class GridPosition(
    val row: Int,
    val col: Int
) {
    fun getNeighbors(maxColsEven: Int = 8, maxColsOdd: Int = 7): List<GridPosition> {
        val neighbors = mutableListOf<GridPosition>()
        val isEvenRow = (row % 2 == 0)
        val maxCols = if (isEvenRow) maxColsEven else maxColsOdd

        // Left & Right
        if (col > 0) neighbors.add(GridPosition(row, col - 1))
        if (col < maxCols - 1) neighbors.add(GridPosition(row, col + 1))

        // Top Left & Top Right
        if (row > 0) {
            if (isEvenRow) {
                if (col > 0) neighbors.add(GridPosition(row - 1, col - 1))
                if (col < maxColsOdd) neighbors.add(GridPosition(row - 1, col))
            } else {
                neighbors.add(GridPosition(row - 1, col))
                if (col + 1 < maxColsEven) neighbors.add(GridPosition(row - 1, col + 1))
            }
        }

        // Bottom Left & Bottom Right
        if (isEvenRow) {
            if (col > 0) neighbors.add(GridPosition(row + 1, col - 1))
            if (col < maxColsOdd) neighbors.add(GridPosition(row + 1, col))
        } else {
            neighbors.add(GridPosition(row + 1, col))
            if (col + 1 < maxColsEven) neighbors.add(GridPosition(row + 1, col + 1))
        }

        return neighbors
    }
}
