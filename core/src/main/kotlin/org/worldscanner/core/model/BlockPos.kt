package org.worldscanner.core.model

import kotlin.math.floor

data class BlockPos(val x: Int, val y: Int, val z: Int) {
    override fun toString(): String = "($x, $y, $z)"
}

fun toBlockPos(vararg doubles: Double): BlockPos {
    if (doubles.size < 3) return BlockPos(0, 0, 0)
    return BlockPos(floor(doubles[0]).toInt(), floor(doubles[1]).toInt(), floor(doubles[2]).toInt())
}
