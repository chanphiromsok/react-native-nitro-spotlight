package com.margelo.nitro.spotlight

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

internal class SpotlightOverlayView(
  context: Context,
) : FrameLayout(context) {

  // -------------------------------------------------------------------------
  // Public properties
  // -------------------------------------------------------------------------

  var dimOpacity: Float = 0.55f
    set(value) {
      field = value
      invalidate()
    }

  var cornerRadius: Float = 12f
    set(value) {
      field = value
      rebuildHolePath()
      invalidate()
    }

  var padding: Float = 6f
    set(value) {
      field = value
      rebuildHolePath()
      invalidate()
    }

  var allowOverlayClick: Boolean = false

  var onBackdropPress: (() -> Unit)? = null

  // -------------------------------------------------------------------------
  // Drawing
  // -------------------------------------------------------------------------

  private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
  }

  private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    style = Paint.Style.FILL
  }

  private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    style = Paint.Style.STROKE
  }

  // -------------------------------------------------------------------------
  // Geometry
  // -------------------------------------------------------------------------

  /**
   * The requested highlight rect expressed in React Native window coordinates
   * (DIP units, as returned by measureInWindow on Android).
   */
  private val windowRectDp = RectF()

  private val currentLocalPx = RectF()
  private val targetLocalPx = RectF()
  private val cutRect = RectF()
  private val holePath = Path()
  private val holeRegion = Region()
  private val holeRegionBounds = Rect()
  private val holePathBounds = RectF()

  // -------------------------------------------------------------------------
  // Animation
  // -------------------------------------------------------------------------

  private var activeAnimator: ValueAnimator? = null

  // -------------------------------------------------------------------------
  // Touch state
  // -------------------------------------------------------------------------

  private var blockingTouch = false

  // -------------------------------------------------------------------------
  // Init
  // -------------------------------------------------------------------------

  init {
    // Hardware layer so PorterDuff.CLEAR works correctly.
    setLayerType(View.LAYER_TYPE_HARDWARE, null)

    // FrameLayout/ViewGroup defaults to WILL_NOT_DRAW when it has no
    // background. We draw the dim overlay in onDraw(), so opt in explicitly.
    setWillNotDraw(false)

    isClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  fun setHighlight(
    xDp: Float,
    yDp: Float,
    widthDp: Float,
    heightDp: Float,
    animated: Boolean,
    durationMs: Long = 250L,
  ) {
    if (widthDp <= 0f || heightDp <= 0f) {
      clear(durationMs = 0L)
      return
    }

    // React Native measureInWindow returns DIP coordinates relative to the
    // visible app window. Store the original JS values; convert only when we
    // know this overlay's current screen position.
    windowRectDp.set(xDp, yDp, xDp + widthDp, yDp + heightDp)

    // Guard: if not laid out yet, onLayout will apply windowRectDp.
    if (width == 0 || height == 0) return

    targetLocalPx.set(windowDpToLocalPx(windowRectDp))

    if (!animated || durationMs <= 0L) {
      cancelAnimation()
      currentLocalPx.set(targetLocalPx)
      rebuildHolePath()
      invalidate()
      return
    }

    animateTo(targetLocalPx, durationMs)
  }

  fun clear(durationMs: Long = 200L, onFinished: (() -> Unit)? = null) {
    windowRectDp.setEmpty()

    if (durationMs <= 0L || currentLocalPx.isEmpty) {
      cancelAnimation()
      currentLocalPx.setEmpty()
      targetLocalPx.setEmpty()
      holePath.reset()
      holeRegion.setEmpty()
      invalidate()
      onFinished?.invoke()
      return
    }

    val centerX = currentLocalPx.centerX()
    val centerY = currentLocalPx.centerY()
    animateTo(RectF(centerX, centerY, centerX, centerY), durationMs, onFinished)
  }

  // -------------------------------------------------------------------------
  // Layout
  // -------------------------------------------------------------------------

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    if (changed && !windowRectDp.isEmpty) {
      targetLocalPx.set(windowDpToLocalPx(windowRectDp))
      cancelAnimation()
      currentLocalPx.set(targetLocalPx)
      rebuildHolePath()
      invalidate()
    }
  }

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  override fun onDetachedFromWindow() {
    cancelAnimation()
    super.onDetachedFromWindow()
  }

  // -------------------------------------------------------------------------
  // Drawing
  // -------------------------------------------------------------------------

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    if (!hasActiveSpotlight()) return

    ringPaint.strokeWidth = 1.5f * density

    dimPaint.color = Color.argb(
      (dimOpacity.coerceIn(0f, 1f) * 255).toInt(),
      0, 0, 0,
    )

    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
    canvas.drawPath(holePath, clearPaint)
    canvas.drawPath(holePath, ringPaint)
  }

  // -------------------------------------------------------------------------
  // Touch handling
  //
  // Strategy: never intercept. On DOWN, decide once whether the touch should
  // pass through to RN underneath or be blocked by the overlay. Carry that
  // decision through the rest of the gesture.
  //
  // When there is no active spotlight hasActiveSpotlight() == false, so
  // dispatchTouchEvent returns false immediately — but this is a belt-and-
  // suspenders guard. The real guarantee that the overlay never steals
  // touches when idle is that HybridSpotlightView removes it from the
  // decor-view entirely (removeFromDecor / addToDecor) rather than hiding it.
  // A MATCH_PARENT view that stays in the hierarchy will always win the
  // ViewGroup bounds-check even when INVISIBLE or isEnabled=false.
  // -------------------------------------------------------------------------

  override fun onInterceptTouchEvent(event: MotionEvent): Boolean = false

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    // No spotlight active — let every touch fall through to RN underneath.
    if (!hasActiveSpotlight()) return false

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        blockingTouch = !allowOverlayClick && !isTouchInsideHole(event.x.toInt(), event.y.toInt())
        // Return false for hole touches, and for all touches when allowOverlayClick
        // is true, so the decor-view continues its normal child hit-test and
        // delivers the gesture to RN underneath.
        return blockingTouch
      }
      MotionEvent.ACTION_UP -> {
        val wasBlocking = blockingTouch
        blockingTouch = false
        if (wasBlocking) {
          onBackdropPress?.invoke()
        }
        return wasBlocking
      }
      MotionEvent.ACTION_POINTER_UP,
      MotionEvent.ACTION_CANCEL -> {
        val wasBlocking = blockingTouch
        blockingTouch = false
        return wasBlocking
      }
      else -> return blockingTouch
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean = false

  // -------------------------------------------------------------------------
  // Geometry helpers
  // -------------------------------------------------------------------------

  private fun isTouchInsideHole(touchX: Int, touchY: Int): Boolean =
    !holeRegion.isEmpty && holeRegion.contains(touchX, touchY)

  /**
   * Convert a React Native measureInWindow rect into this overlay's local
   * pixel coordinates.
   *
   * Android RN returns measureInWindow in DIPs and relative to the visible
   * window frame, while Android Views draw/hit-test in physical pixels and
   * getLocationOnScreen() is screen-space. Reconstruct screen pixels by
   * multiplying by density and adding the visible window frame offset, then
   * subtract this overlay's screen origin.
   */
  private fun windowDpToLocalPx(windowDp: RectF): RectF {
    if (windowDp.isEmpty) return RectF()

    val overlayOrigin = IntArray(2)
    getLocationOnScreen(overlayOrigin)

    val visibleWindowFrame = Rect()
    getWindowVisibleDisplayFrame(visibleWindowFrame)

    val screenLeft = windowDp.left * density + visibleWindowFrame.left
    val screenTop = windowDp.top * density + visibleWindowFrame.top
    val screenRight = windowDp.right * density + visibleWindowFrame.left
    val screenBottom = windowDp.bottom * density + visibleWindowFrame.top

    return RectF(
      screenLeft - overlayOrigin[0],
      screenTop - overlayOrigin[1],
      screenRight - overlayOrigin[0],
      screenBottom - overlayOrigin[1],
    )
  }

  private fun rebuildHolePath() {
    holePath.reset()
    holeRegion.setEmpty()

    if (currentLocalPx.isEmpty) return

    val pad = padding * density
    val radius = (cornerRadius + padding).coerceAtLeast(0f) * density

    cutRect.set(
      currentLocalPx.left - pad,
      currentLocalPx.top - pad,
      currentLocalPx.right + pad,
      currentLocalPx.bottom + pad,
    )

    holePath.addRoundRect(cutRect, radius, radius, Path.Direction.CW)
    holePath.computeBounds(holePathBounds, true)
    holePathBounds.roundOut(holeRegionBounds)
    holeRegion.setPath(holePath, Region(holeRegionBounds))
  }

  private fun hasActiveSpotlight(): Boolean =
    !currentLocalPx.isEmpty && !holePath.isEmpty

  // -------------------------------------------------------------------------
  // Animation
  // -------------------------------------------------------------------------

  private fun animateTo(target: RectF, durationMs: Long, onFinished: (() -> Unit)? = null) {
    cancelAnimation()

    if (durationMs <= 0L) {
      currentLocalPx.set(target)
      rebuildHolePath()
      invalidate()
      return
    }

    val from = if (currentLocalPx.isEmpty && !target.isEmpty) {
      RectF(target.centerX(), target.centerY(), target.centerX(), target.centerY())
    } else {
      RectF(currentLocalPx)
    }

    val to = RectF(target)

    activeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = durationMs
      interpolator = DecelerateInterpolator()

      addUpdateListener { animation ->
        val p = animation.animatedValue as Float
        currentLocalPx.set(
          lerp(from.left,   to.left,   p),
          lerp(from.top,    to.top,    p),
          lerp(from.right,  to.right,  p),
          lerp(from.bottom, to.bottom, p),
        )
        rebuildHolePath()
        invalidate()
      }

      addListener(object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator) = Unit

        override fun onAnimationEnd(animation: Animator) {
          activeAnimator = null
          if (windowRectDp.isEmpty) {
            // Clear animation finished — reset everything.
            currentLocalPx.setEmpty()
            targetLocalPx.setEmpty()
            holePath.reset()
            holeRegion.setEmpty()
            invalidate()
            onFinished?.invoke()
          }
        }

        override fun onAnimationCancel(animation: Animator) {
          activeAnimator = null
        }

        override fun onAnimationRepeat(animation: Animator) = Unit
      })

      start()
    }
  }

  private fun cancelAnimation() {
    activeAnimator?.cancel()
    activeAnimator = null
  }

  private fun lerp(from: Float, to: Float, progress: Float): Float =
    from + (to - from) * progress

  private val density: Float
    get() = resources.displayMetrics.density
}
