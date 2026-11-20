package com.dudeapp.vault

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val TextMain: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val TextSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

/**
 * The looks pre re can wear. Light and Dark are free; the rest need special access.
 * The coral accent matches the logo; the light theme uses a deeper coral so white button
 * text stays readable, and errors are a real red instead of the accent.
 */
data class ThemePreset(
    val label: String,
    val dark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val error: Color,
    /** A look that only people with special access can choose. */
    val special: Boolean = false,
    /** Set when the look came from the theme catalogue, not from this build. */
    val catalogId: String? = null,
    /** A look only the admin may use, on top of special access. */
    val adminOnly: Boolean = false,
    /** The vault card and the home strip; two colours, darker last. */
    val gradient: List<Color>? = null,
    /** Tints the shadow behind the floating bar's selected pill. */
    val glow: Color? = null,
    val backdrop: BackdropStyle = BackdropStyle.Plain,
    /** The picture also sits inside the hero card on Home. */
    val heroArt: Boolean = false,
    /** Cards are slightly see-through, so the backdrop shows through them. */
    val glass: Boolean = false,
    /** The four Home tiles take a wash of their own colour. */
    val tileTints: Boolean = false,
    /**
     * A picture inside each kind of card, on top of the backdrop picture: a look whose
     * background and cards are designed separately.
     */
    val cardPictures: Map<CardPicture, Int> = emptyMap(),
) {

    companion object {

        /** Every look inside this build, in the order the theme page shows them. */
        val all: List<ThemePreset>
            get() = listOf(WHITE, DARK)

        /**
         * The design's light: black ink on white, a #E0E0E0 hairline, one real red for
         * mistakes. Nothing decorative — no accent hue, no gradient, no picture.
         */
        val WHITE = ThemePreset(
        label = "Light",
        dark = false,
        primary = Color(0xFF000000),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFF2F2F3),
        onPrimaryContainer = Color(0xFF000000),
        secondary = Color(0xFF000000),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF4F4F5),
        onBackground = Color(0xFF000000),
        onSurfaceVariant = Color(0xFF828282),
        outline = Color(0xFFE0E0E0),
        error = Color(0xFFC62828),
        )

        /** The same design at night: #F5F5F7 ink on #0C0D10, the same hairlines. */
        val DARK = ThemePreset(
        label = "Dark",
        dark = true,
        primary = Color(0xFFF5F5F7),
        onPrimary = Color(0xFF0C0D10),
        primaryContainer = Color(0xFF1F1F23),
        onPrimaryContainer = Color(0xFFF5F5F7),
        secondary = Color(0xFFF5F5F7),
        background = Color(0xFF0C0D10),
        surface = Color(0xFF16161A),
        surfaceVariant = Color(0xFF232327),
        onBackground = Color(0xFFF5F5F7),
        onSurfaceVariant = Color(0xFF9A9AA0),
        outline = Color(0xFF2E2E33),
        error = Color(0xFFFF6B6B),
        )

    }
}

/** The kinds of card that can carry their own picture. */
enum class CardPicture { HERO, PHOTOS, PDFS, NOTES, REELS }

/** What sits behind the app: a flat colour, a picture, or a picture over a gradient. */
sealed class BackdropStyle {
    data object Plain : BackdropStyle()

    /** A picture that ships inside the app. */
    class Picture(@DrawableRes val image: Int, val scrim: Float = 0.35f) : BackdropStyle()

    /** A picture downloaded from the theme catalogue and kept in the app's files. */
    class Remote(val url: String, val scrim: Float = 0.4f) : BackdropStyle()

    /** A photo this phone chose for a theme of its own. */
    class LocalFile(val file: java.io.File, val scrim: Float = 0.4f) : BackdropStyle()
}

class ThemeStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("dude_theme", Context.MODE_PRIVATE)

    /**
     * The saved look, or Light when there is none or the stored name is unknown. A look
     * from the catalogue is rebuilt from [downloaded], because it is not a build-in name.
     */
    fun load(downloaded: List<DownloadedTheme> = emptyList()): ThemePreset {
        val stored = prefs.getString("preset", "").orEmpty()
        val fromCatalogue = prefs.getString("downloaded", "").orEmpty()
        if (fromCatalogue.isNotEmpty()) {
            // Gone from the catalogue, or no longer allowed for this account: fall back
            // to Dark, but keep the id, so the look returns if it comes back.
            val remote = downloaded.firstOrNull { it.id == fromCatalogue }?.toPreset()
            return if (remote != null && AccessStore(context).isSpecial()) remote else ThemePreset.DARK
        }
        return ThemePreset.all.firstOrNull { it.label == stored } ?: ThemePreset.WHITE
    }

    fun save(preset: ThemePreset) {
        // A catalogue look is remembered by its id in the catalogue, not by its name,
        // so a theme that changes colour later still comes back.
        val id = preset.catalogId.orEmpty()
        prefs.edit()
            .putString("preset", if (id.isEmpty()) preset.label else "")
            .putString("downloaded", id)
            .apply()
    }
}

/** A look that needs access was chosen and that access is gone; go back to Dark. */
fun ThemePreset.withoutAccess(): ThemePreset =
    if (special) ThemePreset.DARK else this

/**
 * The look to show now. A picture look needs the theme level from [access], and a look the
 * admin shares one address at a time needs its id on that address's list. `adminOnly` is a
 * second lock that predates the list: Moonlit still belongs to the owner alone.
 */
fun ThemePreset.forAccess(access: MyAccess, admin: Boolean = false): ThemePreset = when {
    !special -> this
    admin || access.admin -> this
    adminOnly -> ThemePreset.DARK
    !access.theme -> ThemePreset.DARK
    !access.allowsLook(shareId()) -> ThemePreset.DARK
    else -> this
}

@Composable
fun PVaultTheme(
    preset: ThemePreset = ThemePreset.WHITE,
    content: @Composable () -> Unit,
) {
    val onError = if (preset.dark) Color(0xFF2A0A0A) else Color.White
    val base = if (preset.dark) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = preset.primary,
        onPrimary = preset.onPrimary,
        primaryContainer = preset.primaryContainer,
        onPrimaryContainer = preset.onPrimaryContainer,
        secondary = preset.secondary,
        onSecondary = if (preset.dark) Color(0xFF1F0D06) else Color.White,
        secondaryContainer = preset.primaryContainer,
        onSecondaryContainer = preset.onPrimaryContainer,
        tertiary = preset.secondary,
        onTertiary = if (preset.dark) Color(0xFF1F0D06) else Color.White,
        tertiaryContainer = preset.primaryContainer,
        onTertiaryContainer = preset.onPrimaryContainer,
        background = preset.background,
        onBackground = preset.onBackground,
        surface = preset.surface,
        onSurface = preset.onBackground,
        surfaceVariant = preset.surfaceVariant,
        onSurfaceVariant = preset.onSurfaceVariant,
        surfaceTint = preset.surface,
        inverseSurface = if (preset.dark) preset.onBackground else Color(0xFF12141A),
        inverseOnSurface = if (preset.dark) preset.background else Color.White,
        error = preset.error,
        onError = onError,
        errorContainer = if (preset.dark) Color(0xFF3A1717) else Color(0xFFFCE4E4),
        onErrorContainer = preset.error,
        outline = preset.outline,
        outlineVariant = preset.outline,
        scrim = Color.Black,
        surfaceBright = if (preset.dark) preset.surfaceVariant else Color.White,
        surfaceDim = if (preset.dark) preset.background else Color(0xFFE6E8EC),
        surfaceContainer = if (preset.dark) Color(0xFF1B1D23) else preset.background,
        surfaceContainerHigh = preset.surfaceVariant,
        surfaceContainerHighest = if (preset.dark) preset.outline else Color(0xFFE4E6EB),
        surfaceContainerLow = preset.surface,
        surfaceContainerLowest = if (preset.dark) preset.background else Color.White,
    )
    MaterialTheme(colorScheme = colors, typography = VaultTypography, shapes = VaultShapes) {
        CompositionLocalProvider(LocalThemePreset provides preset) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colors.background,
                contentColor = colors.onBackground,
            ) {
                content()
            }
        }
    }
}

private val BaseType = Typography()

/** Tighter, bolder headings and slightly roomier body text than the Material defaults. */
private val VaultTypography = Typography(
    headlineLarge = BaseType.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = BaseType.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    headlineSmall = BaseType.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = BaseType.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = BaseType.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = BaseType.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = BaseType.bodyLarge.copy(letterSpacing = 0.1.sp),
    bodyMedium = BaseType.bodyMedium.copy(letterSpacing = 0.1.sp),
    labelLarge = BaseType.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

/** Rounder corners everywhere: text fields, chips, menus, dialogs and sheets. */
private val VaultShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/**
 * The page behind every screen. The design is flat, so this is the theme's own background
 * and nothing else — no picture, no gradient, no wash.
 */
@Composable
fun Backdrop() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    )
}

/** The active look, for the few places that need more than the colours (backdrop, glow). */
val LocalThemePreset = compositionLocalOf { ThemePreset.WHITE }


/** Every look downloaded from the theme catalogue, so pictures can be found by address. */
