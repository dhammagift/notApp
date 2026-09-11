package com.noapp.container.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.noapp.container.R
import com.noapp.container.model.SlotType

@Composable
fun SlotType.displayName(): String = when (this) {
    SlotType.APP -> stringResource(R.string.slot_type_app)
    SlotType.URL -> stringResource(R.string.slot_type_url)
    SlotType.INTENT -> stringResource(R.string.slot_type_intent)
}

/**
 * Path data for exactly the 3 glyphs the slot-type picker needs, lifted from Google's
 * material-design-icons set (same paths androidx's generated Icons.Filled.* use).
 * Avoids pulling in material-icons-extended (2000+ icons, tens of MB unminified) for three.
 */
private fun vector(name: String, path: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .build()

val AndroidIcon: ImageVector by lazy {
    vector(
        "Android",
        "M17.6 9.48l1.84-3.18c.16-.31.04-.69-.26-.85a.637.637 0 0 0-.83.22l-1.88 3.24a11.463 " +
            "11.463 0 0 0-8.94 0L5.65 5.67a.643.643 0 0 0-.87-.2c-.28.18-.37.54-.22.83L6.4 9.48A10.78 " +
            "10.78 0 0 0 1 18h22a10.78 10.78 0 0 0-5.4-8.52zM7 15.25a1.25 1.25 0 1 1 0-2.5a1.25 1.25 0 " +
            "0 1 0 2.5zm10 0a1.25 1.25 0 1 1 0-2.5a1.25 1.25 0 0 1 0 2.5z"
    )
}

val LinkIcon: ImageVector by lazy {
    vector(
        "Link",
        "M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 " +
            "0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm9-6h-4v1.9h4c1.71 0 3.1 1.39 3.1 3.1s-1.39 " +
            "3.1-3.1 3.1h-4V17h4c2.76 0 5-2.24 5-5s-2.24-5-5-5z"
    )
}

val BoltIcon: ImageVector by lazy {
    vector(
        "Bolt",
        "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66c.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 " +
            "13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.96 17.55 11 21 11 21z"
    )
}

