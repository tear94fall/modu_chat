package com.example.modumessenger.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.modumessenger.R

/**
 * 번개 로고 + 앱 이름 + 한 줄 소개. 스플래시와 로그인 화면이 같은 그림을 쓴다(부록 A §1 · §2).
 * 기존 앱의 `background_splash` 와 같은 구성이다.
 */
@Composable
fun BrandingHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandingLogo()
        Spacer(modifier = Modifier.height(LOGO_CAPTION_GAP))
        BrandingCaption()
    }
}

/**
 * 스플래시용 배치. 로고는 화면 정중앙에 두고 이름·소개는 그 아래에 붙인다.
 *
 * 시스템 스플래시(`Theme.Modu.Splash`)는 번개를 창 정중앙에 그린다. [BrandingHeader] 처럼 로고·글을 한 덩어리로
 * 가운데 두면 로고가 글 높이의 절반만큼 위로 올라가, 시스템 스플래시에서 넘어오는 순간 로고가 튀어 보인다.
 */
@Composable
fun CenteredBranding(modifier: Modifier = Modifier) {
    Layout(
        contents = listOf({ SplashLogo() }, { BrandingCaption() }),
        modifier = modifier,
    ) { (logoMeasurables, captionMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val logo = logoMeasurables.first().measure(loose)
        val caption = captionMeasurables.first().measure(loose)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        layout(width, height) {
            val logoTop = (height - logo.height) / 2
            logo.place((width - logo.width) / 2, logoTop)
            caption.place((width - caption.width) / 2, logoTop + (logo.height * BOLT_BOTTOM).toInt() + LOGO_CAPTION_GAP.roundToPx())
        }
    }
}

@Composable
private fun BrandingLogo() {
    Image(
        painter = painterResource(R.drawable.modu_logo),
        contentDescription = null,
        modifier = Modifier.size(LOGO_SIZE),
    )
}

/**
 * 시스템 스플래시와 똑같은 번개. 시스템 스플래시는 런처 아이콘 전경(108 단위 캔버스)을 [SPLASH_ICON_SIZE] 로 그린다
 * (배경 없는 아이콘의 스플래시 아이콘 크기). 같은 그림을 같은 크기로 창 정중앙에 그려야 크기·위치가 딱 겹친다.
 */
@Composable
private fun SplashLogo() {
    Image(painter = painterResource(R.drawable.modu_ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(SPLASH_ICON_SIZE))
}

@Composable
private fun BrandingCaption() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val LOGO_SIZE = 140.dp
private val LOGO_CAPTION_GAP = 24.dp

/** 배경 없는 아이콘을 시스템 스플래시가 그리는 크기(Android 12+ 스플래시 규격). */
private val SPLASH_ICON_SIZE = 288.dp

/** 전경 캔버스에서 번개 아래끝의 높이 비율: (34.957 + 44.94 × 0.95) / 108. 글은 번개 바로 아래에 붙인다. */
private const val BOLT_BOTTOM = 0.719f
