package com.xxxx.emby_tv.util

/**
 * 像素滚动总线（2026-10-29）。
 *
 * 为什么需要：电视端各页的列表本来是「焦点驱动」的（焦点走一格，列表跟着跳一格），
 * 所以摇杆派发滚轮时如果落点没有可滚容器接着，就只能退化成方向键 —— 表现是
 * 焦点乱跳、完全不是像素级滑动（父亲实测：首页上下左右拨都在跳）。
 *
 * 现在由界面把「当前这页的竖向列表」和「当前有焦点的那一行」注册进来，
 * 摇杆在滚轮没人接时直接调它们的 scrollBy，按像素滚、焦点不动；只有连这也没有
 * （详情页这种一屏到底的页面）才退回方向键。
 *
 * 只在主线程调用：注册在组合期、调用在输入分发里。
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
}
