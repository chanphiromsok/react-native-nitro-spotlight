package com.margelo.nitro.spotlight

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * SpotlightDrawingView
 *
 * A hardware-accelerated View that:
 *  1. Draws a full-screen dim overlay.
 *  2. Punches a transparent rounded-rect hole via PorterDuff.Mode.CLEAR.
 *  3. Draws a white ring around the hole.
 *  4. Animates smoothly between target positions.
 *
 * Coordinate contract:
 *  - All public APIs (setHighlight, clear) accept dp values in WINDOW space
 *    (i.e. the same coordinate system as React Native's pageX/pageY).
 *  - Internally the view converts to local pixel space before drawing, using
 *    getLocationInWindow() to subtract the view's own window offset.
 *    This is the fix for the "extra padding on bottom/right" bug.
 */
internal class SpotlightDrawingView(context: Context) : View(context) {

    // ── Configurable properties ───────────────────────────────────────────────

    var dimOpacity: Float = 0.55f
        set(v) { field = v; invalidate() }

    var cornerRadius: Float = 12f
        set(v) { field = v; invalidate() }

    var padding: Float = 6f
        set(v) { field = v; invalidate() }

    // ── Paint objects ─────────────────────────────────────────────────────────

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // PorterDuff CLEAR punches a transparent hole through the layer.
        // Requires LAYER_TYPE_HARDWARE to work correctly.
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style    = Paint.Style.FILL
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color       = Color.WHITE
        style       = Paint.Style.STROKE
        strokeWidth = 3f   // dp-ish; will be scaled with density where needed
    }

    // ── Rect state ────────────────────────────────────────────────────────────

    // Window-space dp rect as received from JS (pageX/pageY)
    private val windowRectDp = RectF()

    // Local pixel-space rect used for drawing (after coordinate conversion)
    private val currentLocalPx = RectF()
    private val targetLocalPx  = RectF()

    // Cut rect scratch (expanded by padding)
    private val cutRect = RectF()

    // ── Animation ─────────────────────────────────────────────────────────────

    private var activeAnimator: AnimatorSet? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    init {
        // Required: PorterDuff.Mode.CLEAR only works on a hardware layer.
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Immediately move the spotlight to [xDp, yDp, widthDp, heightDp].
     * Coordinates are window-space dp (same as RN's pageX/pageY).
     */
    fun setHighlight(
        xDp: Float,
        yDp: Float,
        widthDp: Float,
        heightDp: Float,
        animated: Boolean,
        durationMs: Long = 250L,
    ) {
        windowRectDp.set(xDp, yDp, xDp + widthDp, yDp + heightDp)

        // Convert to local px now that we know the window rect
        targetLocalPx.set(windowDpToLocalPx(windowRectDp))

        if (!animated) {
            cancelAnimation()
            currentLocalPx.set(targetLocalPx)
            invalidate()
            return
        }

        animateTo(targetLocalPx, durationMs)
    }

    /**
     * Hide the spotlight with a short fade-out by collapsing the rect to zero size.
     */
    fun clear(durationMs: Long = 200L) {
        windowRectDp.setEmpty()
        // Animate to an empty (zero-size) rect at the current center
        val centerX = currentLocalPx.centerX()
        val centerY = currentLocalPx.centerY()
        val emptyTarget = RectF(centerX, centerY, centerX, centerY)
        animateTo(emptyTarget, durationMs)
    }

    // ── Layout change ─────────────────────────────────────────────────────────

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!windowRectDp.isEmpty) {
            // Re-convert after layout changes (rotation, keyboard open, etc.)
            targetLocalPx.set(windowDpToLocalPx(windowRectDp))
            cancelAnimation()
            currentLocalPx.set(targetLocalPx)
            invalidate()
        }
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Full-screen dim overlay
        dimPaint.color = Color.argb(
            (dimOpacity * 255).toInt(), 0, 0, 0
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)

        if (currentLocalPx.isEmpty) return

        val pad = padding * density
        val cr  = (cornerRadius + padding) * density

        cutRect.set(
            currentLocalPx.left   - pad,
            currentLocalPx.top    - pad,
            currentLocalPx.right  + pad,
            currentLocalPx.bottom + pad,
        )

        // 2. Punch transparent hole
        canvas.drawRoundRect(cutRect, cr, cr, clearPaint)

        // 3. White ring
        canvas.drawRoundRect(cutRect, cr, cr, ringPaint)
    }

    // ── Coordinate conversion ─────────────────────────────────────────────────

    /**
     * Convert a window-space dp rect into local pixel coordinates.
     *
     * JS passes pageX/pageY (window dp). The SpotlightView is a child inside
     * the Portal host, which is NOT necessarily at the window origin (safe area,
     * nav bars, etc.). Without this conversion the hole is offset by the view's
     * own position, which shows as "extra padding on bottom/right".
     *
     * Steps:
     *   1. Convert dp → px.
     *   2. Subtract the view's own origin in window px (getLocationInWindow).
     */
    private fun windowDpToLocalPx(windowDp: RectF): RectF {
        if (windowDp.isEmpty) return RectF()

        val location = IntArray(2)
        getLocationInWindow(location)   // [viewOriginX, viewOriginY] in px

        val d = density
        return RectF(
            windowDp.left   * d - location[0],
            windowDp.top    * d - location[1],
            windowDp.right  * d - location[0],
            windowDp.bottom * d - location[1],
        )
    }

    private val density: Float
        get() = resources.displayMetrics.density

    // ── Animation ─────────────────────────────────────────────────────────────

    private fun animateTo(target: RectF, durationMs: Long) {
        cancelAnimation()

        // Snapshot current positions as animation start values
        val fromLeft   = currentLocalPx.left
        val fromTop    = currentLocalPx.top
        val fromRight  = currentLocalPx.right
        val fromBottom = currentLocalPx.bottom

        val toLeft   = target.left
        val toTop    = target.top
        val toRight  = target.right
        val toBottom = target.bottom

        val leftAnim   = ValueAnimator.ofFloat(fromLeft,   toLeft)
        val topAnim    = ValueAnimator.ofFloat(fromTop,    toTop)
        val rightAnim  = ValueAnimator.ofFloat(fromRight,  toRight)
        val bottomAnim = ValueAnimator.ofFloat(fromBottom, toBottom)

        // Merge all four into one update so we invalidate once per frame
        val frameListener = ValueAnimator.AnimatorUpdateListener {
            currentLocalPx.set(
                leftAnim.animatedValue   as Float,
                topAnim.animatedValue    as Float,
                rightAnim.animatedValue  as Float,
                bottomAnim.animatedValue as Float,
            )
            invalidate()
        }

        leftAnim.addUpdateListener(frameListener)
        // The other animators run in sync — only one listener needed for invalidate.
        // But we must add dummy listeners so they actually animate.
        topAnim.addUpdateListener    { }
        rightAnim.addUpdateListener  { }
        bottomAnim.addUpdateListener { }

        activeAnimator = AnimatorSet().also { set ->
            set.duration     = durationMs
            set.interpolator = DecelerateInterpolator()
            set.playTogether(leftAnim, topAnim, rightAnim, bottomAnim)
            set.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(a: Animator)    { activeAnimator = null }
                override fun onAnimationStart(a: Animator)  {}
                override fun onAnimationCancel(a: Animator) {}
                override fun onAnimationRepeat(a: Animator) {}
            })
            set.start()
        }
    }

    private fun cancelAnimation() {
        activeAnimator?.cancel()
        activeAnimator = null
    }
}
