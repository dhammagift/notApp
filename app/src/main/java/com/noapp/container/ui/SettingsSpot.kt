package com.noapp.container.ui

/**
 * A row in Settings worth sending the user to from somewhere else — currently from the mode picker's
 * demo, which draws a feature greyed out when it is off and offers to turn it on.
 *
 * A named spot rather than a row index: Settings' order is free to change, and the demo cares about
 * "the floating button row", not about the eleventh item in a column.
 */
enum class SettingsSpot {
    RECENT_APPS,
    FLOATING_BUTTON
}
