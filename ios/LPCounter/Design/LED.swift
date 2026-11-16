//
//  LED.swift
//  LPCounter
//
//  The app's look: a black and "Blu notte" (pale blue, deep blue halo) dot-matrix display. One grid of dots, anchored to the
//  screen, sits behind every screen; large text and the totals light its own dots. Small text is system
//  monospaced, readouts use DSEG7 (seven segments). Native controls stay native, tinted pale blue.
//

import CoreText
import SwiftUI

enum LED {
    /// Pure black: on OLED screens those pixels are off, as on a real display between its dots.
    static let background = Color.black
    /// The chosen colour (SPEC §5), read from the shared store so the widgets use it too. The root of the
    /// app redraws everything when it changes (`ContentView`).
    static var palette: LEDPalette { LEDPalette(rawValue: store?.string(forKey: LEDPalette.key) ?? "") ?? .night }
    static var lit: Color { palette.lit }
    static var halo: Color { palette.halo }
    static var unlit: Color { palette.unlit }
    /// Secondary text: 0.6 keeps it above 4.5:1 on the background (WCAG AA).
    static var dim: Color { lit.opacity(0.6) }
    static let red = Color(red: 1, green: 0.365, blue: 0.365)
    static let green = Color(red: 0.616, green: 1, blue: 0.541)
    /// Behind list rows and panels: dark enough to read on, light enough to let the grid show through.
    static let panel = Color.black.opacity(0.55)
    /// Shared with the widget extension, which draws the same dots.
    static let store = UserDefaults(suiteName: GameConstants.appGroup)

    /// The halo of a lit colour: the deeper blue for the main colour, itself for red and green.
    static func glow(_ color: Color) -> Color { color == lit ? halo : color }

    static func segmentFont(_ size: CGFloat, relativeTo style: Font.TextStyle = .body) -> Font {
        .custom("DSEG7Classic-Bold", size: size, relativeTo: style)
    }

    /// Bundled fonts (OFL, licences next to them in Resources/Fonts) are registered at launch, so
    /// Info.plist stays the same on iOS and macOS.
    static func registerFonts() {
        let urls = Bundle.main.urls(forResourcesWithExtension: "ttf", subdirectory: nil) ?? []
        CTFontManagerRegisterFontURLs(urls as CFArray, .process, true, nil)
    }
}

/// Colour of the display (SPEC §5): the tube, its halo, and the unlit dots.
enum LEDPalette: String, CaseIterable, Identifiable {
    case night, ice, frost, cyan, amber

    static let key = "ledPalette"

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .night: "Night blue"
        case .ice: "Ice"
        case .frost: "Frost"
        case .cyan: "Cyan"
        case .amber: "Amber"
        }
    }

    var lit: Color {
        switch self {
        case .night: .rgb(0xA9C8FF)
        case .ice: .rgb(0xE8F3FF)
        case .frost: .rgb(0xD6FAFF)
        case .cyan: .rgb(0x5FF5E3)
        case .amber: .rgb(0xFFC46B)
        }
    }

    var halo: Color {
        switch self {
        case .night: .rgb(0x3E6BFF)
        case .ice: .rgb(0x6E9CFF)
        case .frost: .rgb(0x57C7E8)
        case .cyan: .rgb(0x2BD8C4)
        case .amber: .rgb(0xFF7A1A)
        }
    }

    var unlit: Color {
        switch self {
        case .night: .rgb(0x0C111F)
        case .ice: .rgb(0x111722)
        case .frost: .rgb(0x0E1A1E)
        case .cyan: .rgb(0x0A1F1D)
        case .amber: .rgb(0x1E1308)
        }
    }
}

private extension Color {
    static func rgb(_ hex: UInt32) -> Color {
        Color(red: Double(hex >> 16 & 0xFF) / 255, green: Double(hex >> 8 & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }
}

/// Shape of every dot of the grid, lit or not (SPEC §5).
enum PixelStyle: String, CaseIterable, Identifiable {
    case round, soft, square

    static let key = "pixelStyle"

    /// The saved choice; the app passes it down the environment so every dot redraws when it changes.
    static var stored: PixelStyle { PixelStyle(rawValue: LED.store?.string(forKey: key) ?? "") ?? .round }

    var id: Self { self }

    var title: LocalizedStringKey {
        switch self {
        case .round: "Round"
        case .soft: "Diamond"
        case .square: "Square"
        }
    }

    /// Shapes kept far apart, so the choice shows even on small dots: a circle, a diamond, a full square.
    func path(in rect: CGRect) -> Path {
        switch self {
        case .round: Path(ellipseIn: rect)
        case .soft:
            Path { p in
                p.move(to: CGPoint(x: rect.midX, y: rect.minY - rect.height * 0.12))
                p.addLine(to: CGPoint(x: rect.maxX + rect.width * 0.12, y: rect.midY))
                p.addLine(to: CGPoint(x: rect.midX, y: rect.maxY + rect.height * 0.12))
                p.addLine(to: CGPoint(x: rect.minX - rect.width * 0.12, y: rect.midY))
                p.closeSubpath()
            }
        case .square: Path(rect.insetBy(dx: -rect.width * 0.12, dy: -rect.height * 0.12))
        }
    }
}

// MARK: - Text

private struct LEDTextStyle: ViewModifier {
    let style: Font.TextStyle
    let color: Color
    let glow: Bool

    func body(content: Content) -> some View {
        content
            .font(.system(style, design: .monospaced, weight: .semibold))
            .textCase(.uppercase)
            .foregroundStyle(color)
            .shadow(color: LED.glow(color).opacity(glow ? 0.8 : 0), radius: glow ? 5 : 0)
    }
}

extension View {
    /// Small panel text, too small for the dot grid: system monospaced, uppercase, lit. `size` picks the
    /// nearest text style, so it follows Dynamic Type.
    func led(_ size: CGFloat, color: Color = LED.lit, glow: Bool = true) -> some View {
        let style: Font.TextStyle = switch size {
        case ..<12.5: .caption
        case ..<14: .footnote
        case ..<16: .subheadline
        case ..<18: .body
        case ..<21: .title3
        case ..<25: .title2
        default: .title
        }
        return modifier(LEDTextStyle(style: style, color: color, glow: glow))
    }
}

/// A seven-segment readout: the unlit "8"s stay faintly visible behind the value, as on a clock radio.
struct SegmentReadout: View {
    let text: String
    /// Cells drawn, right-aligned; the empty ones on the left show only their unlit segments.
    let digits: Int
    let size: CGFloat
    var color: Color = LED.lit
    var relativeTo: Font.TextStyle = .body

    private var ghost: String {
        String(repeating: "8", count: max(0, digits - text.count)) + String(text.map { $0.isNumber || $0 == "-" ? "8" : $0 })
    }

    var body: some View {
        ZStack(alignment: .trailing) {
            Text(verbatim: ghost).foregroundStyle(LED.unlit)
            Text(verbatim: text)
                .foregroundStyle(color)
                .shadow(color: LED.glow(color).opacity(0.8), radius: size * 0.18)
                .contentTransition(.numericText())
        }
        .font(LED.segmentFont(size, relativeTo: relativeTo))
        .lineLimit(1)
        .minimumScaleFactor(0.5)
        .accessibilityElement()
        .accessibilityLabel(Text(verbatim: text))
    }
}

// MARK: - The grid

extension EnvironmentValues {
    /// Shape of every dot. Set from the saved setting at the app's root (`ContentView`), so a change
    /// reaches each grid, text and die face at once, sheets included; widgets read the saved value.
    @Entry var pixelStyle = PixelStyle.stored
    /// Where the duel's power-on intro is (GameView): 0 dark, 1 totals lit, 2 controls in, 3 done. Views
    /// outside the duel see 3.
    @Entry var introStep = 3
    /// Distance between two dots of the screen's grid; widgets use a finer one.
    @Entry var ledPitch: CGFloat = 5
    /// Set on views turned 180° (Player 2 in portrait), so their dots still land on the grid.
    @Entry var ledFlipped = false
}

/// Offset of the first grid cell inside `frame` (global coordinates): every dot on screen, background or
/// text, sits at a multiple of the pitch, so text lights the background's own dots.
private func gridOffset(_ frame: CGRect, _ p: CGFloat, flipped: Bool) -> CGPoint {
    func mod(_ v: CGFloat) -> CGFloat { let r = v.truncatingRemainder(dividingBy: p); return r < 0 ? r + p : r }
    return flipped ? CGPoint(x: mod(frame.maxX), y: mod(frame.maxY)) : CGPoint(x: mod(-frame.minX), y: mod(-frame.minY))
}

/// A canvas drawn on the screen's grid: `draw` gets the first cell's offset and the pitch.
struct GridCanvas: View {
    let draw: (inout GraphicsContext, CGSize, CGPoint, CGFloat) -> Void
    @Environment(\.ledPitch) private var p
    @Environment(\.ledFlipped) private var flipped

    var body: some View {
        // GeometryReader, not state: widgets are drawn once and never update state.
        GeometryReader { proxy in
            let origin = gridOffset(proxy.frame(in: .global), p, flipped: flipped)
            Canvas { gc, size in draw(&gc, size, origin, p) }
        }
    }
}

extension LED {
    /// Dot diameter as a share of the pitch, the same for unlit and lit dots.
    static let dot: CGFloat = 0.72

    static func cell(_ col: Int, _ row: Int, _ origin: CGPoint, _ p: CGFloat) -> CGRect {
        let d = p * dot
        return CGRect(x: origin.x + CGFloat(col) * p + (p - d) / 2, y: origin.y + CGFloat(row) * p + (p - d) / 2, width: d, height: d)
    }
}

/// The unlit matrix behind a screen. `sweep` in 0...1 runs a band of lit dots across it, top to bottom,
/// as a panel switching on; nil draws it still.
struct LEDMatrixBackground: View, Animatable {
    var sweep: Double? = nil
    @Environment(\.pixelStyle) private var pixels

    var animatableData: Double {
        get { sweep ?? 1 }
        set { if sweep != nil { sweep = newValue } }
    }

    var body: some View {
        GridCanvas { gc, size, o, p in
            var dots = Path(), band = Path()
            let rows = Int(size.height / p)
            // The band's centre row, from above the top to below the bottom, and its half-height.
            let centre = sweep.map { Double(rows + 16) * $0 - 8 }
            for row in -1...rows {
                let lit = centre.map { abs(Double(row) - $0) < 4 } ?? false
                for col in -1...Int(size.width / p) {
                    let dot = pixels.path(in: LED.cell(col, row, o, p))
                    if lit { band.addPath(dot) } else { dots.addPath(dot) }
                }
            }
            gc.fill(dots, with: .color(LED.unlit))
            guard !band.isEmpty else { return }
            gc.drawLayer { halo in
                halo.addFilter(.blur(radius: p * 0.8))
                halo.fill(band, with: .color(LED.halo.opacity(0.6)))
            }
            gc.fill(band, with: .color(LED.lit.opacity(0.55)))
        }
        .background(LED.background)
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

// MARK: - Dot-matrix text

/// Text that lights the grid's own dots, on a 5×7 font: each font dot is a `scale`×`scale` block of grid
/// dots, the largest scale up to `maxScale` that fits. Only for short, large text (titles, totals, timer).
struct DotMatrixText: View {
    let text: String
    var color: Color = LED.lit
    var glow = true
    var maxScale = 1
    /// Characters lit, from the left; the rest stay dark (typing).
    var visible: Int? = nil
    /// With `morph` < 1, a share of the dots (picked by a fixed hash) still shows `previous`: animating
    /// `morph` 0 → 1 dithers from `previous` into `text`.
    var previous: String? = nil
    var morph = 1.0
    /// Lamp test: the bottom `test` share of the rows lit in full, as a display checks its dots.
    var test = 0.0
    /// Spans exactly this many grid columns (another text's `columns`): the largest scale that fits,
    /// the rest spread between the letters.
    var fillColumns: Int? = nil

    @Environment(\.ledPitch) private var p
    @Environment(\.pixelStyle) private var pixels

    /// Grid columns `text` takes at scale 1.
    static func columns(_ text: String) -> Int {
        let glyphs = Array(text.uppercased()).map(DotFont.glyph)
        return glyphs.reduce(0) { $0 + $1[0].count } + max(0, glyphs.count - 1)
    }

    var body: some View {
        let glyphs = Array(text.uppercased()).map(DotFont.glyph)
        let old = previous.map { Array($0.uppercased()).map(DotFont.glyph) }
        let ink = glyphs.reduce(0) { $0 + $1[0].count }
        let gaps = max(0, glyphs.count - 1)
        Group {
            if let fillColumns {
                let scale = max(1, (fillColumns - gaps) / max(1, ink))
                let spare = fillColumns - ink * scale
                canvas(glyphs, old, scale, gaps: (0..<gaps).map { spare / gaps + ($0 < spare % max(1, gaps) ? 1 : 0) },
                       columns: fillColumns)
            } else {
                // Width only: a short hero would otherwise drop to the smallest scale.
                ViewThatFits(in: .horizontal) {
                    ForEach((1...max(1, maxScale)).reversed(), id: \.self) { scale in
                        canvas(glyphs, old, scale, gaps: Array(repeating: scale, count: gaps), columns: (ink + gaps) * scale)
                    }
                }
            }
        }
        .accessibilityElement()
        .accessibilityLabel(Text(verbatim: text.trimmingCharacters(in: .whitespaces)))
    }

    private func canvas(_ glyphs: [[[Bool]]], _ old: [[[Bool]]]?, _ scale: Int, gaps: [Int], columns: Int) -> some View {
        // One extra pitch each way: snapping to the grid moves the text by up to one dot.
        GridCanvas { gc, _, o, p in draw(&gc, glyphs, old, scale, gaps, o, p) }
            .frame(width: CGFloat(columns + 1) * p, height: CGFloat(7 * scale + 1) * p)
    }

    /// `gaps`: grid columns after each letter but the last.
    private func draw(_ gc: inout GraphicsContext, _ glyphs: [[[Bool]]], _ old: [[[Bool]]]?, _ s: Int, _ gaps: [Int],
                      _ o: CGPoint, _ p: CGFloat) {
        var lit = Path()
        let litFrom = 7 * (1 - test)
        var x = 0
        for (i, rows) in glyphs.enumerated() {
            let w = rows[0].count
            for r in 0..<7 {
                for c in 0..<w {
                    var on = rows[r][c]
                    if let old, i < old.count, c < old[i][0].count, DotFont.hash(i, r, c) >= morph { on = old[i][r][c] }
                    if Double(r) >= litFrom { on = true }
                    if let visible, i >= visible { on = false }
                    guard on else { continue }
                    for dy in 0..<s {
                        for dx in 0..<s { lit.addPath(pixels.path(in: LED.cell(x + c * s + dx, r * s + dy, o, p))) }
                    }
                }
            }
            x += w * s + (i < gaps.count ? gaps[i] : 0)
        }
        if glow {
            gc.drawLayer { halo in
                halo.addFilter(.blur(radius: p * 0.5))
                halo.fill(lit, with: .color(LED.glow(color).opacity(0.85)))
            }
        }
        gc.fill(lit, with: .color(color))
    }
}

/// A total on the grid, padded to four cells so the width doesn't change while counting. Animatable:
/// changing `value` inside an animation counts through the numbers in between.
struct DotMatrixNumber: View, Animatable {
    var value: Double
    var color: Color = LED.lit
    var glow = true
    var maxScale = 3
    var previous: Int? = nil
    var morph = 1.0
    var test = 0.0

    var animatableData: Double {
        get { value }
        set { value = newValue }
    }

    var body: some View {
        let current = Int(value.rounded())
        let width = max(4, String(current).count, previous.map { String($0).count } ?? 0)
        DotMatrixText(text: Self.padded(current, width), color: color, glow: glow, maxScale: maxScale,
                      previous: previous.map { Self.padded($0, width) }, morph: morph, test: test)
    }

    private static func padded(_ n: Int, _ width: Int) -> String {
        String(repeating: " ", count: max(0, width - String(n).count)) + String(n)
    }
}

private enum DotFont {
    static func glyph(_ ch: Character) -> [[Bool]] {
        let key = glyphs[ch] ?? String(ch).folding(options: .diacriticInsensitive, locale: nil).first.flatMap { glyphs[$0] }
        return (key ?? ["00", "00", "00", "00", "00", "00", "00"]).map { $0.map { $0 == "1" } }
    }

    /// Stable value in 0..<1 per dot: the order dots switch in a dither.
    static func hash(_ i: Int, _ r: Int, _ c: Int) -> Double {
        let x = sin(Double(i * 35 + r * 5 + c) * 12.9898) * 43758.5453
        return x - x.rounded(.down)
    }

    /// 5×7, narrower where the letter allows (I, punctuation, space).
    static let glyphs: [Character: [String]] = [
        " ": ["00", "00", "00", "00", "00", "00", "00"],
        "0": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
        "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
        "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
        "3": ["11111", "00010", "00100", "00010", "00001", "10001", "01110"],
        "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
        "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
        "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
        "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
        "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
        "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
        "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
        "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
        "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
        "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
        "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
        "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
        "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
        "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
        "I": ["111", "010", "010", "010", "010", "010", "111"],
        "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
        "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
        "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
        "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
        "N": ["10001", "10001", "11001", "10101", "10011", "10001", "10001"],
        "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
        "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
        "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
        "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
        "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
        "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
        "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
        "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
        "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
        "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
        "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
        "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
        "'": ["1", "1", "0", "0", "0", "0", "0"],
        "’": ["1", "1", "0", "0", "0", "0", "0"],
        ":": ["0", "1", "0", "0", "0", "1", "0"],
        "·": ["0", "0", "0", "1", "0", "0", "0"],
        "!": ["1", "1", "1", "1", "1", "0", "1"],
        "-": ["000", "000", "000", "111", "000", "000", "000"],
        "+": ["000", "000", "010", "111", "010", "000", "000"],
        "=": ["000", "000", "111", "000", "111", "000", "000"],
    ]
}

// MARK: - Typewriter

/// Lights a word letter by letter, holds it, switches it off from the end, and again.
/// Reduce Motion shows it still.
struct TypewriterText: View {
    let text: String
    var maxScale = 1
    var fillColumns: Int? = nil

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var shown = 0

    var body: some View {
        // The whole word is laid out once; typing only lights its letters, so nothing moves.
        DotMatrixText(text: text, maxScale: maxScale, visible: shown, fillColumns: fillColumns)
            .task(id: text) { await run() }
    }

    private func run() async {
        guard !reduceMotion else { shown = text.count; return }
        while !Task.isCancelled {
            while shown < text.count {
                shown += 1
                try? await Task.sleep(for: .seconds(0.22))
            }
            try? await Task.sleep(for: .seconds(5))
            while shown > 0 {
                shown -= 1
                try? await Task.sleep(for: .seconds(0.09))
            }
            try? await Task.sleep(for: .seconds(0.35))
        }
    }
}

#Preview {
    VStack(spacing: 24) {
        DotMatrixText(text: "LP·COUNTER")
        TypewriterText(text: "DUEL", fillColumns: DotMatrixText.columns("IT'S TIME TO"))
        DotMatrixNumber(value: 7500)
        SegmentReadout(text: "42:17", digits: 5, size: 24)
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity)
    .background(LEDMatrixBackground())
}
