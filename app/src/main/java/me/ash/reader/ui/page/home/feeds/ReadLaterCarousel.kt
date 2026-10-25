package me.ash.reader.ui.page.home.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.ash.reader.R
import me.ash.reader.domain.model.article.Article
import me.ash.reader.ui.component.base.RYAsyncImage
import me.ash.reader.ui.ext.isRtl
import me.ash.reader.ui.theme.applyTextDirection

/**
 * Read Later on the home screen as a carousel of the saved articles' pictures, newest first. The
 * title sits on the image, because text under an item is clipped as the item shrinks, and fades
 * out with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadLaterCarousel(articles: List<Article>, onOpen: (Article) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.read_later),
            color = colors.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 26.dp, top = 20.dp, bottom = 10.dp),
        )
        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { articles.size },
            preferredItemWidth = 180.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        ) { index ->
            val article = articles[index]
            val info = carouselItemDrawInfo
            val textAlpha =
                if (info.maxSize > info.minSize)
                    ((info.size - info.minSize) / (info.maxSize - info.minSize)).coerceIn(0f, 1f)
                else 1f
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge)
                        .background(colors.secondaryContainer)
                        .clickable { onOpen(article) }
            ) {
                if (article.img != null) {
                    RYAsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        data = article.img,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                    )
                }
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.35f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.75f),
                                )
                            )
                )
                Text(
                    text = article.title,
                    color = if (article.img != null) Color.White else colors.onSecondaryContainer,
                    style =
                        MaterialTheme.typography.titleSmall.applyTextDirection(article.title.isRtl()),
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(14.dp)
                            .alpha(textAlpha),
                )
            }
        }
    }
}
