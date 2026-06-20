package com.margelo.nitro.spotlight

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import com.facebook.common.internal.DoNotStrip
import com.facebook.react.bridge.UiThreadUtil
import com.facebook.react.uimanager.ThemedReactContext
import com.margelo.nitro.views.RecyclableView

@DoNotStrip
@Keep
class HybridSpotlightView(
  private val context: ThemedReactContext,
) : HybridSpotlightViewSpec(), RecyclableView {

  // -------------------------------------------------------------------------
  // Views
  // -------------------------------------------------------------------------

  /**
   * anchorView is what Fabric/Nitro holds in the JS view tree.
   * Zero-size, invisible — its only job is attach/detach lifecycle callbacks.
   */
  private val anchorView = View(context).apply {
    // This native view is only a Fabric/Nitro lifecycle anchor. It must never
    // participate in Android hit-testing; the real touch-handling overlay is
    // added to decorView only after highlight()/highlightAnimated().
    visibility = View.GONE
    isEnabled = false
    isClickable = false
    isFocusable = false
    isFocusableInTouchMode = false
    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
  }

  /**
   * The real overlay. Sits outside the Fabric tree, attached directly to the
   * activity decor-view only while a spotlight is active.
   *
   * KEY FIX: we add spotlightView to the decor-view ONLY when highlight() is
   * called, and remove it again when clear() finishes. A MATCH_PARENT view
   * that lives permanently in the decor-view hierarchy always wins Android's
   * ViewGroup bounds-check and swallows touches — even when INVISIBLE or
   * isEnabled=false — because the framework hit-tests bounds before it ever
   * calls the child's dispatchTouchEvent. The only 100 % reliable solution
   * is to not have the view in the hierarchy at all when it is not needed.
   */
  private val spotlightView = SpotlightOverlayView(context)

  /** True while anchorView is attached to a window (i.e. the screen is live). */
  private var anchorAttached = false

  /** The decor-view we most recently added spotlightView to. */
  private var decorView: ViewGroup? = null

  /** True while spotlightView is a child of decorView. */
  private var overlayAdded = false

  private var dimOpacityValue   = 0.55
  private var cornerRadiusValue = 12.0
  private var paddingValue      = 6.0

  // -------------------------------------------------------------------------
  // Nitro / Fabric entry-point
  // -------------------------------------------------------------------------

  override val view: View get() = anchorView

  // -------------------------------------------------------------------------
  // Init
  // -------------------------------------------------------------------------

  init {
    anchorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
      override fun onViewAttachedToWindow(v: View) {
        anchorAttached = true
        decorView = context.currentActivity?.window?.decorView as? ViewGroup
      }

      override fun onViewDetachedFromWindow(v: View) {
        anchorAttached = false
        removeOverlayFromDecor()
        decorView = null
      }
    })
  }

  // -------------------------------------------------------------------------
  // Properties
  // -------------------------------------------------------------------------

  override var dimOpacity: Double
    get() = dimOpacityValue
    set(value) {
      dimOpacityValue = value
      UiThreadUtil.runOnUiThread { spotlightView.dimOpacity = value.toFloat() }
    }

  override var cornerRadius: Double
    get() = cornerRadiusValue
    set(value) {
      cornerRadiusValue = value
      UiThreadUtil.runOnUiThread { spotlightView.cornerRadius = value.toFloat() }
    }

  override var padding: Double
    get() = paddingValue
    set(value) {
      paddingValue = value
      UiThreadUtil.runOnUiThread { spotlightView.padding = value.toFloat() }
    }

  override var onTargetLayout: ((Rect) -> Unit)? = null

  // -------------------------------------------------------------------------
  // Commands
  // -------------------------------------------------------------------------

  override fun highlight(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
  ) {
    UiThreadUtil.runOnUiThread {
      // Add overlay to decor-view first so it has a valid layout when
      // setHighlight() queries getLocationOnScreen().
      addOverlayToDecor()
      spotlightView.setHighlight(
        xDp       = x.toFloat(),
        yDp       = y.toFloat(),
        widthDp   = width.toFloat(),
        heightDp  = height.toFloat(),
        animated  = false,
      )
      onTargetLayout?.invoke(Rect(x = x, y = y, width = width, height = height))
    }
  }

  override fun highlightAnimated(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    durationMs: Double,
  ) {
    UiThreadUtil.runOnUiThread {
      addOverlayToDecor()
      spotlightView.setHighlight(
        xDp       = x.toFloat(),
        yDp       = y.toFloat(),
        widthDp   = width.toFloat(),
        heightDp  = height.toFloat(),
        animated  = true,
        durationMs = durationMs.toLong(),
      )
      onTargetLayout?.invoke(Rect(x = x, y = y, width = width, height = height))
    }
  }

  override fun clear() {
    UiThreadUtil.runOnUiThread {
      spotlightView.clear(
        onFinished = { removeOverlayFromDecor() }
      )
    }
  }

  // -------------------------------------------------------------------------
  // RecyclableView
  // -------------------------------------------------------------------------

  override fun prepareForRecycle() {
    onTargetLayout    = null
    dimOpacityValue   = 0.55
    cornerRadiusValue = 12.0
    paddingValue      = 6.0
    UiThreadUtil.runOnUiThread {
      spotlightView.dimOpacity   = 0.55f
      spotlightView.cornerRadius = 12f
      spotlightView.padding      = 6f
      spotlightView.clear(durationMs = 0L, onFinished = { removeOverlayFromDecor() })
    }
  }

  // -------------------------------------------------------------------------
  // Overlay add / remove
  // -------------------------------------------------------------------------

  private fun addOverlayToDecor() {
    if (overlayAdded) return
    val dv = decorView ?: return

    val params = android.widget.FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
    )
    dv.addView(spotlightView, params)

    // Sync props that may have been set before the overlay was attached.
    spotlightView.dimOpacity   = dimOpacityValue.toFloat()
    spotlightView.cornerRadius = cornerRadiusValue.toFloat()
    spotlightView.padding      = paddingValue.toFloat()

    overlayAdded = true
  }

  /**
   * Remove spotlightView from the decor-view.
   * Posted via Handler so we never remove a child during the decor-view's
   * own dispatchDetachedFromWindow traversal (which would corrupt its child
   * list on some Android versions).
   */
  private fun removeOverlayFromDecor() {
    if (!overlayAdded) return
    overlayAdded = false

    val dv = decorView
    dv?.post {
      if (spotlightView.parent === dv) {
        dv.removeView(spotlightView)
      }
    }
  }
}
