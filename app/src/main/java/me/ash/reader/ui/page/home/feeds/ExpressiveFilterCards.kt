package me.ash.reader.ui.page.home.feeds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ash.reader.domain.model.general.Filter

/**
 * The expressive home header: the current filter as a large tonal card with its count, and two
 * smaller cards with different corner shapes for the other filters, each one tap away.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveFilterCards(
    filter: Filter,
    count: Int,
    starredCount: Int,
    readLaterCount: Int,
    onOpen: (Filter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = { onOpen(filter) },
            shape = RoundedCornerShape(32.dp),
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = filter.toName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-2).sp,
                    )
                }
                Box(
                    modifier =
                        Modifier.size(56.dp)
                            .clip(MaterialShapes.Cookie9Sided.toShape())
                            .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.onPrimary,
                    )
                }
            }
        }

        // The two filters not shown above. Unread has no count here: only the current
        // filter's count is kept up to date.
        val others = listOf(Filter.Starred, Filter.ReadLater, Filter.Unread).filter { it != filter }.take(2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            others.forEachIndexed { index, other ->
                val (container, content) =
                    when (other) {
                        Filter.Starred -> colors.tertiaryContainer to colors.onTertiaryContainer
                        Filter.ReadLater -> colors.secondaryContainer to colors.onSecondaryContainer
                        else -> colors.surfaceContainerHigh to colors.onSurface
                    }
                val otherCount =
                    when (other) {
                        Filter.Starred -> starredCount
                        Filter.ReadLater -> readLaterCount
                        else -> null
                    }
                SmallFilterCard(
                    modifier = Modifier.weight(1f),
                    filter = other,
                    count = otherCount,
                    container = container,
                    content = content,
                    // Mirror-image corners, so the pair reads as one shape split in two.
                    shape =
                        if (index == 0) RoundedCornerShape(28.dp, 28.dp, 28.dp, 10.dp)
                        else RoundedCornerShape(28.dp, 28.dp, 10.dp, 28.dp),
                    onClick = { onOpen(other) },
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SmallFilterCard(
    filter: Filter,
    count: Int?,
    container: Color,
    content: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(imageVector = filter.iconOutline, contentDescription = null)
            Column {
                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    text = filter.toName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
