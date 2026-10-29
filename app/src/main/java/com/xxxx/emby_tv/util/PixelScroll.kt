package com.xxxx.emby_tv.util

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch

/**
 * 像素滚动总线：全站摇杆滚动的唯一入口。
 *
 * 为什么要有它：电视端各页原本是「焦点驱动」的滚动（焦点走一格，列表跳一格）。
 * 摇杆派发滚轮时常没人接（大片头、行标题、详情页背景都不是可滚容器），
 * 一旦退化发方向键，就变成焦点乱跳、按住不动、松手才蹦一点 —— 与 VR 的手感相差很远。
 *
 * 现在由界面把「这一页的竖向列表」和「当前有焦点的那一行」注册进来，
 * 摇杆（含按住期间的逐帧推进与松手惯性）直接调它们的 scrollBy 按像素滚，焦点不动。
 * 页面上没有任何可滚区域时摇杆就什么都不做，不再发方向键。
 */
object PixelScroll {
    private var vertical: ((Float) -> Boolean)? = null
    private var horizontal: ((Float) -> Boolean)? = null

    fun setVertical(scroll: ((Float) -> Boolean)?) {
        vertical = scroll
    }

    fun setHorizontal(scroll: ((Float) -> Boolean)?) {
        horizontal = scroll
    }

    fun verticalBy(px: Float): Boolean = vertical?.invoke(px) ?: false

    fun horizontalBy(px: Float): Boolean = horizontal?.invoke(px) ?: false

    private var owner: Any? = null

    /** 谁最后注册的横向入口（用于焦点离开时只清自己的） */
    fun takeOwner(who: Any) { owner = who }

    fun ownerIs(who: Any): Boolean = owner === who
}

/** 竖向列表（LazyColumn / LazyVerticalGrid / 普通 Column 的 verticalScroll）注册为摇杆竖向目标。 */
@Composable
fun RegisterVerticalScroll(state: LazyListState) {
    val scope = rememberCoroutineScope()
    DisposableEffect(state) {
        PixelScroll.setVertical { px ->
            scope.launch { state.scrollBy(px) }
            true
        }
        onDispose { PixelScroll.setVertical(null) }
    }
}

@Composable
fun RegisterVerticalScroll(state: LazyGridState) {
    val scope = rememberCoroutineScope()
    DisposableEffect(state) {
        PixelScroll.setVertical { px ->
            scope.launch { state.scrollBy(px) }
            true
        }
        onDispose { PixelScroll.setVertical(null) }
    }
}

@Composable
fun RegisterVerticalScroll(state: ScrollState) {
    val scope = rememberCoroutineScope()
    DisposableEffect(state) {
        PixelScroll.setVertical { px ->
            scope.launch { state.scrollBy(px) }
            true
        }
        onDispose { PixelScroll.setVertical(null) }
    }
}

/**
 * 横向行注册为摇杆横向目标：只有焦点落在这一行里时才生效，避免多行抢同一个入口。
 * 用法：`LazyRow(state = rowState, modifier = Modifier.pixelScrollRow(rowState), ...)`
 */
@Composable
fun Modifier.pixelScrollRow(state: LazyListState): Modifier {
    val scope = rememberCoroutineScope()
    return this.onFocusChanged { focusState ->
        if (focusState.hasFocus) {
            PixelScroll.takeOwner(state)
            PixelScroll.setHorizontal { px ->
                scope.launch { state.scrollBy(px) }
                true
            }
        } else if (PixelScroll.ownerIs(state)) {
            /* 焦点离开这一行就要交还横向入口，否则播放页里横向摇杆永远被上一次的行走吃掉，
             * 快退快进发不出去（父亲 2026-10-29 报「播放区左右没有快进快退」）。 */
            PixelScroll.setHorizontal(null)
        }
    }
}
