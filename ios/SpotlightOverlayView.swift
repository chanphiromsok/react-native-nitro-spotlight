import UIKit

// MARK: - SpotlightOverlayView

final class SpotlightOverlayView: UIView {

  // MARK: - Config

  var dimOpacity: CGFloat = 0.55 {
    didSet { redraw(animated: false) }
  }

  var cornerRadius: CGFloat = 12 {
    didSet { redraw(animated: false) }
  }

  var padding: CGFloat = 6 {
    didSet { redraw(animated: false) }
  }

  /// Use this only when your JS rect is true screen-space.
  /// For React Native `measureInWindow`, default should usually be false.
//   var useScreenCoordinates: Bool = false {
//     didSet { redraw(animated: false) }
//   }

  // MARK: - Private

  private let overlayLayer = CAShapeLayer()
  private let ringLayer = CAShapeLayer()

  /// Rect coming from JS.
  /// Usually from `measureInWindow`.
  private var sourceRect = CGRect.zero
  private var currentOverlayPath: UIBezierPath?

  // MARK: - Init

  override init(frame: CGRect) {
    super.init(frame: frame)

    isUserInteractionEnabled = false
    backgroundColor = .clear

    overlayLayer.fillRule = .evenOdd
    overlayLayer.fillColor = UIColor.black.withAlphaComponent(dimOpacity).cgColor

    ringLayer.fillColor = UIColor.clear.cgColor
    ringLayer.strokeColor = UIColor.white.cgColor
    ringLayer.lineWidth = 1.5

    layer.addSublayer(overlayLayer)
    layer.addSublayer(ringLayer)
  }

  required init?(coder: NSCoder) {
    fatalError("init(coder:) has not been implemented")
  }

  // MARK: - Layout

  override func layoutSubviews() {
    super.layoutSubviews()
    updateLayerFrames()
    redraw(animated: false)
  }

  override func didMoveToWindow() {
    super.didMoveToWindow()

    guard window != nil else { return }

    updateLayerFrames()

    // Pre-warm layers to avoid first render stutter.
    overlayLayer.path = UIBezierPath(rect: .zero).cgPath
    ringLayer.path = UIBezierPath(rect: .zero).cgPath

    redraw(animated: false)
  }

  override func didMoveToSuperview() {
    super.didMoveToSuperview()
    setNeedsLayout()
  }

  // MARK: - Layer Frames

  private func updateLayerFrames() {
    CATransaction.begin()
    CATransaction.setDisableActions(true)

    // Important:
    // Keep layer coordinates same as this UIView.
    // Do not use UIScreen.main.bounds here.
    overlayLayer.frame = bounds
    ringLayer.frame = bounds

    CATransaction.commit()
  }

  // MARK: - Public API

  func setHighlight(
    _ rect: CGRect,
    animated: Bool,
    duration: TimeInterval = 0.25
  ) {
    sourceRect = rect
    redraw(animated: animated, duration: duration)
  }

  func clear(
    animated: Bool = true,
    duration: TimeInterval = 0.2
  ) {
    sourceRect = .zero
    redraw(animated: animated, duration: duration)
  }

  // MARK: - Coordinate Conversion

  private func localRect(from rect: CGRect) -> CGRect {
    guard let window else { return rect }

    return window.convert(rect, to: self)
    // if useScreenCoordinates {
    //   // Use this when rect is true screen-space.
    //   return window.screen.coordinateSpace.convert(
    //     rect,
    //     to: self.coordinateSpace
    //   )
    // } else {
    //   // Use this for React Native measureInWindow in normal window mode.
    //   return window.convert(rect, to: self)
    // }
  }

  // MARK: - Drawing

  private func redraw(
    animated: Bool,
    duration: TimeInterval = 0.25
  ) {
    overlayLayer.fillColor = UIColor.black.withAlphaComponent(dimOpacity).cgColor

    let nextOverlayPath = makeOverlayPath()
    let nextRingPath = makeRingPath()

    let oldOverlayPath = currentOverlayPath
    currentOverlayPath = nextOverlayPath

    if animated {
      animate(
        layer: overlayLayer,
        from: oldOverlayPath?.cgPath ?? overlayLayer.presentation()?.path ?? overlayLayer.path,
        to: nextOverlayPath?.cgPath,
        duration: duration
      )

      animate(
        layer: ringLayer,
        from: ringLayer.presentation()?.path ?? ringLayer.path,
        to: nextRingPath?.cgPath,
        duration: duration
      )
    } else {
      overlayLayer.removeAnimation(forKey: "path")
      ringLayer.removeAnimation(forKey: "path")

      overlayLayer.path = nextOverlayPath?.cgPath
      ringLayer.path = nextRingPath?.cgPath
    }
  }

  private func makeHolePath() -> UIBezierPath {
    let local = localRect(from: sourceRect)

    let cutRect = local.insetBy(
      dx: -padding,
      dy: -padding
    )

    return UIBezierPath(
      roundedRect: cutRect,
      cornerRadius: cornerRadius + padding
    )
  }

  private func makeOverlayPath() -> UIBezierPath? {
    guard !sourceRect.isEmpty else { return nil }

    let path = UIBezierPath()
    path.usesEvenOddFillRule = true

    // Layer frame == UIView bounds, so path must also use bounds.
    path.append(UIBezierPath(rect: bounds))
    path.append(makeHolePath())

    return path
  }

  private func makeRingPath() -> UIBezierPath? {
    guard !sourceRect.isEmpty else { return nil }
    return makeHolePath()
  }

  // MARK: - Animation

  private func animate(
    layer: CAShapeLayer,
    from: CGPath?,
    to: CGPath?,
    duration: TimeInterval
  ) {
    let actualFrom = layer.presentation()?.path ?? from

    layer.removeAnimation(forKey: "path")
    layer.path = to

    guard let actualFrom, let to else { return }

    let anim = CABasicAnimation(keyPath: "path")
    anim.fromValue = actualFrom
    anim.toValue = to
    anim.duration = duration
    anim.timingFunction = CAMediaTimingFunction(name: .easeInEaseOut)
    anim.isRemovedOnCompletion = true

    layer.add(anim, forKey: "path")
  }
}
