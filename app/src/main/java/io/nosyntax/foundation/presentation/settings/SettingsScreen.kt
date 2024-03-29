package io.nosyntax.foundation.presentation.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.nosyntax.foundation.R
import io.nosyntax.foundation.core.utility.Intents.openEmail
import io.nosyntax.foundation.core.utility.Intents.openPlayStore
import io.nosyntax.foundation.core.utility.Intents.openUrl
import io.nosyntax.foundation.core.utility.Utilities.findActivity
import io.nosyntax.foundation.presentation.main.MainActivity
import io.nosyntax.foundation.ui.theme.DynamicTheme

@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.openPlayStore(context.packageName) }
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.rate_us),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Divider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surface
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.openEmail(email = "example@example.com") }
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.send_feedback),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Divider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surface
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.openUrl("https://example.com") }
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Divider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surface
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.openUrl("https://example.com") }
                    .padding(horizontal = 20.dp, vertical = 15.dp)
            ) {
                Text(
                    text = "About Us",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }

    BackHandler(enabled = true) {
        (context.findActivity() as MainActivity).showInterstitial(onAdDismissed = {
            navController.popBackStack()
        })
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    DynamicTheme(darkTheme = false) {
        val navController = rememberNavController()
        SettingsScreen(navController = navController)
    }
}