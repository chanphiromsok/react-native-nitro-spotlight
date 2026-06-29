package com.margelo.nitro.spotlight

import android.view.View
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
   * The dim + cutout overlay. Returned as the Fabric/Nitro view so React
   * Native sizes it (via StyleSheet.absoluteFillObject in the JS wrapper) and
   * JS siblings (SpotlightTooltip) render above it in Android z-order —
   * matching the iOS architecture exactly.
   */
  private val spotlightView = SpotlightOverlayView(context)

  // -------------------------------------------------------------------------
  // Property backing fields
  // -------------------------------------------------------------------------

  private var dimOpacityValue: Double? = null
  private var borderRadiusValue: Double? = null
  private var paddingValue: Double? = null
  private var borderWidthValue: Double? = null
  private var borderColorValue: String? = null
  private var allowOverlayClickValue: Boolean? = null

  // -------------------------------------------------------------------------
  // Nitro / Fabric entry-point
  // -------------------------------------------------------------------------

  override val view: View get() = spotlightView

  // -------------------------------------------------------------------------
  // Properties
  // -------------------------------------------------------------------------

  override var dimOpacity: Double?
    get() = dimOpacityValue
    set(value) {
      if (dimOpacityValue == value) return
      dimOpacityValue = value
      UiThreadUtil.runOnUiThread { spotlightView.dimOpacity = (value ?: DEFAULT_DIM_OPACITY).toFloat() }
    }

  override var borderRadius: Double?
    get() = borderRadiusValue
    set(value) {
      if (borderRadiusValue == value) return
      borderRadiusValue = value
      UiThreadUtil.runOnUiThread { spotlightView.borderRadius = (value ?: DEFAULT_BORDER_RADIUS).toFloat() }
    }

  override var padding: Double?
    get() = paddingValue
    set(value) {
      if (paddingValue == value) return
      paddingValue = value
      UiThreadUtil.runOnUiThread { spotlightView.padding = (value ?: DEFAULT_PADDING).toFloat() }
    }

  override var borderWidth: Double?
    get() = borderWidthValue
    set(value) {
      if (borderWidthValue == value) return
      borderWidthValue = value
      UiThreadUtil.runOnUiThread { spotlightView.borderWidth = (value ?: DEFAULT_BORDER_WIDTH).toFloat() }
    }

  override var borderColor: String?
    get() = borderColorValue
    set(value) {
      if (borderColorValue == value) return
      borderColorValue = value
      UiThreadUtil.runOnUiThread { spotlightView.borderColor = value ?: DEFAULT_BORDER_COLOR }
    }

  override var allowOverlayClick: Boolean?
    get() = allowOverlayClickValue
    set(value) {
      if (allowOverlayClickValue == value) return
      allowOverlayClickValue = value
      UiThreadUtil.runOnUiThread { spotlightView.allowOverlayClick = value ?: DEFAULT_ALLOW_OVERLAY_CLICK }
    }

  override var onTargetLayout: ((Rect) -> Unit)? = null

  override var onBackdropPress: (() -> Unit)? = null
    set(value) {
      field = value
      UiThreadUtil.runOnUiThread { spotlightView.onBackdropPress = value }
    }

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
      spotlightView.setHighlight(
        xDp      = x.toFloat(),
        yDp      = y.toFloat(),
        widthDp  = width.toFloat(),
        heightDp = height.toFloat(),
        animated = false,
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
      spotlightView.setHighlight(
        xDp        = x.toFloat(),
        yDp        = y.toFloat(),
        widthDp    = width.toFloat(),
        heightDp   = height.toFloat(),
        animated   = true,
        durationMs = durationMs.toLong(),
      )
      onTargetLayout?.invoke(Rect(x = x, y = y, width = width, height = height))
    }
  }

  override fun clear() {
    UiThreadUtil.runOnUiThread {
      spotlightView.clear(durationMs = 0L)
    }
  }

  // -------------------------------------------------------------------------
  // RecyclableView
  // -------------------------------------------------------------------------

  override fun prepareForRecycle() {
    onTargetLayout = null
    onBackdropPress = null
    dimOpacityValue = null
    borderRadiusValue = null
    paddingValue = null
    borderWidthValue = null
    borderColorValue = null
    allowOverlayClickValue = null
    UiThreadUtil.runOnUiThread {
      spotlightView.dimOpacity = DEFAULT_DIM_OPACITY.toFloat()
      spotlightView.borderRadius = DEFAULT_BORDER_RADIUS.toFloat()
      spotlightView.padding = DEFAULT_PADDING.toFloat()
      spotlightView.borderWidth = DEFAULT_BORDER_WIDTH.toFloat()
      spotlightView.borderColor = DEFAULT_BORDER_COLOR
      spotlightView.allowOverlayClick = DEFAULT_ALLOW_OVERLAY_CLICK
      spotlightView.onBackdropPress = null
      spotlightView.clear(durationMs = 0L)
    }
  }

  companion object {
    private const val DEFAULT_DIM_OPACITY = 0.55
    private const val DEFAULT_BORDER_RADIUS = 12.0
    private const val DEFAULT_PADDING = 6.0
    private const val DEFAULT_BORDER_WIDTH = 1.5
    private const val DEFAULT_BORDER_COLOR = "#FFFFFF"
    private const val DEFAULT_ALLOW_OVERLAY_CLICK = false
  }
}
