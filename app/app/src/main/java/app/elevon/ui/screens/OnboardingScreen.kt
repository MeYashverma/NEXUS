package app.elevon.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * First-run onboarding: five slides, skippable, per docs/design.md.
 */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val slides = listOf(
        Slide(
            title = "Meet Elevon",
            body = "Your phone can become your computer's keyboard, mouse, controller, macro deck and more. One pairing, every control.",
            icon = Icons.Outlined.TouchApp,
        ),
        Slide(
            title = "Connect",
            body = "Pair with your computer like a normal wireless device. Your computer sees Elevon as a standard keyboard, mouse and gamepad — nothing to install there, ever, for the core experience.",
            icon = Icons.Outlined.Link,
        ),
        Slide(
            title = "Choose your control",
            body = "Keyboard. Touchpad. Gamepad. Macro pad. Media and presentation remotes. Switch instantly, and the same pairing keeps working.",
            icon = Icons.Outlined.SportsEsports,
        ),
        Slide(
            title = "Make it yours",
            body = "Profiles for each computer. Game layouts with dead zones and triggers. Macro decks with your own shortcuts and text snippets.",
            icon = Icons.Outlined.Keyboard,
        ),
        Slide(
            title = "Explore Labs",
            body = "Elevon Relay is an experiment: use your laptop keyboard to type on your phone, from the browser. Honest about what works and what can't.",
            icon = Icons.Outlined.Science,
        ),
    )
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < slides.size - 1) {
                TextButton(onClick = onDone) { Text("Skip") }
            }
        }
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    slide.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    slide.title,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(slides.size) { i ->
                val selected = pager.currentPage == i
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                )
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < slides.size - 1) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                } else {
                    onDone()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = if (pager.currentPage < slides.size - 1) "Next slide" else "Get started" },
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(if (pager.currentPage < slides.size - 1) "Next" else "Get started")
        }
    }
}

private data class Slide(val title: String, val body: String, val icon: ImageVector)
