package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,  // < 600.dp (Phones in portrait)
    MEDIUM,   // 600.dp .. 839.dp (Foldables, small tablets, phones in landscape)
    EXPANDED  // >= 840.dp (Tablets, desktop windows, wide landscape)
}

enum class WindowHeightSizeClass {
    COMPACT,  // < 480.dp (Landscape phones)
    MEDIUM,   // 480.dp .. 899.dp
    EXPANDED  // >= 900.dp
}

@Immutable
data class AdaptiveLayoutInfo(
    val widthDp: Dp,
    val heightDp: Dp,
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
) {
    val useNavigationRail: Boolean
        get() = widthSizeClass != WindowWidthSizeClass.COMPACT || heightSizeClass == WindowHeightSizeClass.COMPACT

    val isTwoPaneCapable: Boolean
        get() = widthDp >= 700.dp || (widthDp >= 580.dp && widthDp > heightDp)

    val gridColumns: Int
        get() = when {
            widthDp >= 1040.dp -> 3
            widthDp >= 620.dp -> 2
            else -> 1
        }

    val horizontalPadding: Dp
        get() = when {
            widthDp >= 900.dp -> 28.dp
            widthDp >= 600.dp -> 20.dp
            else -> 16.dp
        }
}

fun computeAdaptiveLayoutInfo(maxWidth: Dp, maxHeight: Dp): AdaptiveLayoutInfo {
    val widthClass = when {
        maxWidth < 600.dp -> WindowWidthSizeClass.COMPACT
        maxWidth < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }
    val heightClass = when {
        maxHeight < 480.dp -> WindowHeightSizeClass.COMPACT
        maxHeight < 900.dp -> WindowHeightSizeClass.MEDIUM
        else -> WindowHeightSizeClass.EXPANDED
    }
    return AdaptiveLayoutInfo(
        widthDp = maxWidth,
        heightDp = maxHeight,
        widthSizeClass = widthClass,
        heightSizeClass = heightClass
    )
}
