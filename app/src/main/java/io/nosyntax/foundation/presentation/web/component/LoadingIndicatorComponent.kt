package io.nosyntax.foundation.presentation.web.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.nosyntax.foundation.core.component.AnimatedProgressBar
import io.nosyntax.foundation.domain.model.app_config.LoadingIndicatorConfig
import io.nosyntax.foundation.ui.theme.DynamicTheme
import com.airbnb.lottie.compose.LottieCompositionSpec

@SuppressLint("DiscouragedApi")
@Composable
fun LoadingIndicator(indicatorConfig: LoadingIndicatorConfig) {
    val context = LocalContext.current
    val resources = context.resources

    val backgroundColor = when (indicatorConfig.background) {
        "neutral" -> MaterialTheme.colorScheme.background
        "solid" -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    val defaultProgressColor = if (indicatorConfig.background == "solid") Color.White else MaterialTheme.colorScheme.onBackground
    val progressColor = if (indicatorConfig.color == "neutral") defaultProgressColor else MaterialTheme.colorScheme.primary

    val animationResource = remember {
        val animationResourceNames = (1..10).map { "loading_indicator_$it" }
        val filteredResources = animationResourceNames.mapNotNull { animationName ->
            val resourceId = resources.getIdentifier(animationName, "raw", context.packageName)
            if (resourceId != 0) animationName to resourceId else null
        }
        mutableMapOf(*filteredResources.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        animationResource[indicatorConfig.animation]?.let { animation ->
            AnimatedProgressBar(
                modifier = Modifier.size(120.dp),
                source = LottieCompositionSpec.RawRes(animation),
                color = progressColor
            )
        }
    }
}

@Preview
@Composable
fun LoadingIndicatorPreview() {
    DynamicTheme {
        LoadingIndicator(
            indicatorConfig = LoadingIndicatorConfig(
                display = true,
                animation = "loading_indicator_1",
                background = "neutral",
                color = "solid"
            )
        )
    }
}