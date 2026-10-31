package com.xxxx.emby_tv.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 焦点配色口径（父亲 2026-10-31 定：得焦点不用透明乳白，用半透明绿）。
 *
 * 绿色沿用 VR 侧 VrButtonColors 的 #2FD57C（VR 里这一档是"选中"色）；
 * 电视十尺观看下 24% 太淡，这里抬到 40%，焦点一眼可辨又不遮住海报图。
 * 边框用同一绿的实心底色，方便在浅色海报上也看出焦点位置。
 */
val FocusGreen = Color(0xFF2FD57C)
val FocusGreenContainer = Color(0x662FD57C)
val FocusGreenBorder = Color(0xFF2FD57C)

/**
 * 全局聚焦主题样式定义
 * 统一管理所有组件的聚焦背景样式
 */
object FocusTheme {
    
    /**
     * 主要聚焦背景 - 使用主题色
     */
    @Composable
    fun primaryFocusBackground(): Brush {
        // 主题里 primary 是纯白，直接用会变成"一聚焦变白底"，焦点色只认绿色常量
        return Brush.linearGradient(
            colors = listOf(FocusGreenContainer, FocusGreen.copy(alpha = 0.30f))
        )
    }
    
    /**
     * 次要聚焦背景 - 使用主题色的淡化版本
     */
    @Composable
    fun secondaryFocusBackground(): Brush {
        return Brush.linearGradient(
            colors = listOf(FocusGreen.copy(alpha = 0.28f), FocusGreen.copy(alpha = 0.14f))
        )
    }
    
    /**
     * 聚焦边框颜色
     */
    @Composable
    fun focusBorderColor(): Color {
        return FocusGreenBorder
    }
    
    /**
     * 透明背景（未聚焦状态）
     */
    fun transparentBackground(): Brush {
        return Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }
}

/**
 * Modifier扩展函数 - 应用聚焦背景样式
 */
@Composable
fun Modifier.focusBackground(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    focusType: FocusType = FocusType.PRIMARY
): Modifier {
    return this.background(
        brush = if (isFocused) {
            when (focusType) {
                FocusType.PRIMARY -> FocusTheme.primaryFocusBackground()
                FocusType.SECONDARY -> FocusTheme.secondaryFocusBackground()
            }
        } else {
            FocusTheme.transparentBackground()
        },
        shape = shape
    )
}

/**
 * Modifier扩展函数 - 应用聚焦边框样式
 */
@Composable
fun Modifier.focusBorder(
    isFocused: Boolean,
    borderWidth: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    return if (isFocused) {
        this.border(
            width = borderWidth,
            color = FocusTheme.focusBorderColor(),
            shape = shape
        )
    } else {
        this
    }
}

/**
 * Modifier扩展函数 - 组合聚焦样式（背景+边框）
 */
@Composable
fun Modifier.focusStyle(
    isFocused: Boolean,
    shape: Shape = RoundedCornerShape(8.dp),
    focusType: FocusType = FocusType.PRIMARY,
    borderWidth: Dp = 2.dp,
    useBackground: Boolean = true,
    useBorder: Boolean = false
): Modifier {
    var modifier = this
    
    if (useBackground) {
        modifier = modifier.focusBackground(isFocused, shape, focusType)
    }
    
    if (useBorder) {
        modifier = modifier.focusBorder(isFocused, borderWidth, shape)
    }
    
    return modifier
}

/**
 * 聚焦类型枚举
 */
enum class FocusType {
    PRIMARY,    // 主要聚焦样式 - 用于重要按钮、卡片等
    SECONDARY   // 次要聚焦样式 - 用于列表项、次要按钮等
}