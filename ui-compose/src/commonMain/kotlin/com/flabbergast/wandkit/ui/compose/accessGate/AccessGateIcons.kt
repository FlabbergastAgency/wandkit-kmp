package com.flabbergast.wandkit.ui.compose.accessGate

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The gate's two glyphs, embedded so the SDK does not pull in
 * `material-icons-extended` for them. Paths are Material Icons' filled
 * "Lock" and "WifiOff" (Apache 2.0), on the usual 24x24 grid.
 */
internal object AccessGateIcons {
    val Lock: ImageVector by lazy {
        icon(
            name = "WandKitAccessGate.Lock",
            pathData = "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10" +
                "c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10c0,-1.1 -0.9,-2 -2,-2z" +
                "M12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2 2,0.9 2,2 -0.9,2 -2,2z" +
                "M15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1 1.71,0 3.1,1.39 3.1,3.1v2z",
        )
    }

    val WifiOff: ImageVector by lazy {
        icon(
            name = "WandKitAccessGate.WifiOff",
            pathData = "M22.99,9C19.15,5.16 13.8,3.76 8.84,4.78l2.52,2.52c3.47,-0.17 6.99,1.05 9.63,3.7l2,-2z" +
                "M18.99,13c-1.29,-1.29 -2.84,-2.2 -4.49,-2.73l3.53,3.53 0.96,-0.8z" +
                "M2,3.05L5.07,6.1C3.6,6.82 2.22,7.78 1,9l1.99,2c1.24,-1.24 2.67,-2.16 4.2,-2.77" +
                "l2.24,2.24C7.81,10.89 6.27,11.73 5,13v0.01L6.99,15c1.36,-1.36 3.14,-2.04 4.92,-2.06" +
                "L18.98,20l1.27,-1.26L3.29,1.79 2,3.05z" +
                "M9,17l3,3 3,-3c-1.65,-1.66 -4.34,-1.66 -6,0z",
        )
    }

    private fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black),
        ).build()
}
