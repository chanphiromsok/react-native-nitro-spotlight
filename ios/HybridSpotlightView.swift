import UIKit


// MARK: - HybridSpotlightView

class HybridSpotlightView: HybridSpotlightViewSpec {

  // MARK: - Nitro

  private let overlayView = SpotlightOverlayView()

  var view: UIView {
    overlayView
  }

  // MARK: - Props

  var dimOpacity: Double = 0.55 {
    didSet {
      overlayView.dimOpacity = CGFloat(dimOpacity)
    }
  }

  var cornerRadius: Double = 12 {
    didSet {
      overlayView.cornerRadius = CGFloat(cornerRadius)
    }
  }

  var padding: Double = 6 {
    didSet {
      overlayView.padding = CGFloat(padding)
    }
  }

  /// Add this prop to your Nitro spec too.
  ///
  /// false = normal `measureInWindow`
  /// true  = real screen-space rect
  //  var useScreenCoordinates: Bool = false {
  //    didSet {
  //      overlayView.useScreenCoordinates = useScreenCoordinates
  //    }
  //  }

  var onTargetLayout: ((Rect) -> Void)?

  // MARK: - Methods

  func highlight(
    x: Double,
    y: Double,
    width: Double,
    height: Double
  ) throws {
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }

      let rect = CGRect(
        x: x,
        y: y,
        width: width,
        height: height
      )

      overlayView.setHighlight(
        rect,
        animated: false
      )

      onTargetLayout?(
        Rect(
          x: x,
          y: y,
          width: width,
          height: height
        )
      )
    }
  }

  func highlightAnimated(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    durationMs: Double
  ) throws {
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }

      let rect = CGRect(
        x: x,
        y: y,
        width: width,
        height: height
      )

      overlayView.setHighlight(
        rect,
        animated: true,
        duration: durationMs / 1000.0
      )

      onTargetLayout?(
        Rect(
          x: x,
          y: y,
          width: width,
          height: height
        )
      )
    }
  }

  func clear() throws {
    DispatchQueue.main.async { [weak self] in
      self?.overlayView.clear()
    }
  }

  func measureViewByTag(reactTag: Double) -> Rect {
    return Rect(x: 0, y: 0, width: 0, height: 0)
  }
}
