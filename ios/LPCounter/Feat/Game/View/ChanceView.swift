//
//  ChanceView.swift
//  LPCounter
//
//  Dice and coin, as the rules allow for card effects (SPEC §4): a panel over the screen where the coin
//  flips and the die tumbles for a couple of seconds, then lands on a fair random outcome.
//

import SwiftUI

enum Chance: Hashable {
    case die, coin

    var title: LocalizedStringKey { self == .die ? "Roll a die" : "Flip a coin" }
    var symbol: String { self == .die ? "die.face.5.fill" : "circle.lefthalf.filled" }

    /// A fair outcome, as the text the matrix shows.
    func outcome() -> String {
        switch self {
        case .die: String(Int.random(in: 1...6))
        case .coin: Bool.random() ? String(localized: "Heads") : String(localized: "Tails")
        }
    }
}

/// The two buttons that open the panel, for a toolbar or the Home.
struct ChanceButtons: View {
    @Binding var chance: Chance?

    var body: some View {
        ForEach([Chance.die, .coin], id: \.self) { kind in
            Button(kind.title, systemImage: kind.symbol) { chance = kind }
                .labelStyle(.iconOnly)
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
        }
    }
}

/// Shown while `chance` is set; a tap outside closes it, Again throws once more.
struct ChanceOverlay: View {
    @Binding var chance: Chance?
    @State private var flip = Flip(faces: [""], turns: 0)
    @State private var progress = 0.0
    @State private var throwsCount = 0
    @State private var settled = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        if let chance {
            ZStack {
                Color.black.opacity(0.8)
                    .ignoresSafeArea()
                    .onTapGesture { close() }
                    .accessibilityHidden(true)
                VStack(spacing: 20) {
                    Text(chance.title).led(15, color: LED.dim, glow: false)
                    FlippingFace(flip: flip, kind: chance, progress: progress, settled: settled)
                        .accessibilityLabel(Text(verbatim: settled ? flip.result : ""))
                    HStack(spacing: 12) {
                        Button("Close", systemImage: "xmark", action: close)
                            .labelStyle(.iconOnly)
                            .buttonStyle(.glass)
                            .buttonBorderShape(.circle)
                        Button("Again", systemImage: "arrow.clockwise") { throwsCount += 1 }
                            .foregroundStyle(LED.background)
                            .buttonStyle(.glassProminent)
                            .buttonBorderShape(.capsule)
                            .disabled(!settled)
                    }
                    .font(.title3.weight(.semibold))
                    .controlSize(.large)
                }
                .padding(28)
                // As big as its content, not the screen: the face, the title and the two buttons.
                .fixedSize()
                // Its own fine grid, whatever the screen behind uses (the duel's is coarse).
                .environment(\.ledPitch, 5)
                .background(LED.background, in: .rect(cornerRadius: 28))
                .overlay { RoundedRectangle(cornerRadius: 28).strokeBorder(LED.lit.opacity(0.3), lineWidth: 1.5) }
                .padding()
            }
            .transition(.opacity)
            .sensoryFeedback(.impact(weight: .medium), trigger: settled) { _, new in new }
            .task(id: [throwsCount, chance == .die ? 0 : 1]) { await run(chance) }
        }
    }

    /// The outcome is drawn first; the flip only decides which faces show on the way to it. Stepped by
    /// hand, frame by frame: an implicit animation started as the panel appears has no first frame to
    /// start from and jumps to the end. Reduce Motion shows the result at once.
    private func run(_ kind: Chance) async {
        settled = false
        flip = Flip.make(kind)
        progress = 0
        if !reduceMotion {
            let duration = kind == .coin ? 2.4 : 2.8
            let start = Date.now
            while true {
                let t = min(1, Date.now.timeIntervalSince(start) / duration)
                // Ease out: fast at first, slowing to a stop, like a coin falling flat or a die rolling out.
                progress = 1 - pow(1 - t, 3)
                if t >= 1 { break }
                // Cancelled only when a new throw replaces this one, which then settles on its own.
                do { try await Task.sleep(for: .milliseconds(16)) } catch { return }
            }
        }
        progress = 1
        settle()
    }

    private func settle() {
        settled = true
        AccessibilityNotification.Announcement(flip.result).post()
    }

    private func close() {
        withAnimation(.smooth(duration: 0.2)) { chance = nil }
    }
}

/// One throw: the faces it passes through, the last one being the result.
private struct Flip {
    var faces: [String]
    /// Half-turns: face `k` shows during the `k`-th one.
    var turns: Int

    var result: String { faces.last ?? "" }

    static func make(_ kind: Chance) -> Flip {
        let result = kind.outcome()
        switch kind {
        case .coin:
            // Heads and tails alternate, ending on the result.
            let turns = 14
            let other = result == String(localized: "Heads") ? String(localized: "Tails") : String(localized: "Heads")
            return Flip(faces: (0...turns).map { ($0 - turns).isMultiple(of: 2) ? result : other }, turns: turns)
        case .die:
            // Random faces, never the same twice in a row, ending on the result.
            let turns = 12
            var faces = [result]
            while faces.count <= turns {
                var face = String(Int.random(in: 1...6))
                while face == faces[0] { face = String(Int.random(in: 1...6)) }
                faces.insert(face, at: 0)
            }
            return Flip(faces: faces, turns: turns)
        }
    }
}

/// The face, turning. The coin: its word squashed edge-on at every half-turn, as it flips over. The die:
/// a die, pips and all, tumbling — it spins, leans away on each roll and shows a new face, then lands
/// square. The face changes when it is edge-on.
private struct FlippingFace: View, Animatable {
    let flip: Flip
    let kind: Chance
    var progress: Double
    let settled: Bool

    var animatableData: Double {
        get { progress }
        set { progress = newValue }
    }

    var body: some View {
        let turn = progress * Double(flip.turns)
        let index = min(flip.turns, Int(turn.rounded()))
        let squash = max(0.05, abs(cos(turn * .pi)))
        let color = settled ? LED.lit : LED.dim
        Group {
            if kind == .die {
                DieFace(value: Int(flip.faces[index]) ?? 1, color: color)
                    .frame(width: 140, height: 140)
                    // Rolls over one edge, then the next: alternate axes, like a cube on a table.
                    .scaleEffect(x: index.isMultiple(of: 2) ? squash : 1, y: index.isMultiple(of: 2) ? 1 : squash)
                    .rotationEffect(.degrees((1 - progress) * 720))
                    .offset(y: -sin(progress * .pi) * 12 * (1 - progress))
            } else {
                // Both words laid out, the wider one holding the size, so the panel never changes width
                // while the coin turns.
                ZStack {
                    ForEach(Array(Set(flip.faces)), id: \.self) { DotMatrixText(text: $0, maxScale: 2).hidden() }
                    DotMatrixText(text: flip.faces[index], color: color, maxScale: 2)
                        .scaleEffect(x: 1, y: squash)
                }
            }
        }
        // Room for the die's hop and spin, so it isn't clipped by the panel.
        .padding(kind == .die ? 24 : 4)
    }
}

/// One face of a die: a rounded body and its pips on a 3×3 layout, the pips drawn as dots of the matrix.
private struct DieFace: View {
    let value: Int
    let color: Color
    @Environment(\.pixelStyle) private var pixels

    /// Pip positions on a 3×3 layout, row by row, as on a real die.
    private static let pips: [Int: [(Int, Int)]] = [
        1: [(1, 1)],
        2: [(0, 0), (2, 2)],
        3: [(0, 0), (1, 1), (2, 2)],
        4: [(0, 0), (2, 0), (0, 2), (2, 2)],
        5: [(0, 0), (2, 0), (1, 1), (0, 2), (2, 2)],
        6: [(0, 0), (2, 0), (0, 1), (2, 1), (0, 2), (2, 2)],
    ]

    var body: some View {
        Canvas { gc, size in
            let side = min(size.width, size.height)
            let body = CGRect(x: (size.width - side) / 2, y: (size.height - side) / 2, width: side, height: side)
                .insetBy(dx: 3, dy: 3)
            let shape = Path(roundedRect: body, cornerRadius: side * 0.24)
            // A faint face, so it reads as a solid die and not a frame.
            gc.fill(shape, with: .color(LED.glow(color).opacity(0.08)))
            gc.stroke(shape, with: .color(color), lineWidth: 4)
            // Pips on a 3×3 layout inside an inner margin, as on a real die, clear of the edges.
            let inner = body.insetBy(dx: side * 0.2, dy: side * 0.2)
            let step = inner.width / 2
            // Each pip is a small round cluster of matrix dots, so the die belongs to the display.
            let dot = side * 0.034, gap = dot * 1.3
            var pips = Path()
            for (c, r) in Self.pips[value] ?? [] {
                let cx = inner.minX + step * CGFloat(c), cy = inner.minY + step * CGFloat(r)
                for dy in -1...1 {
                    for dx in -1...1 {
                        // Corners pulled in, so the cluster is round rather than square.
                        let k: CGFloat = abs(dx) + abs(dy) == 2 && pixels != .square ? 0.72 : 1
                        let x = cx + CGFloat(dx) * gap * k, y = cy + CGFloat(dy) * gap * k
                        pips.addPath(pixels.path(in: CGRect(x: x - dot / 2, y: y - dot / 2, width: dot, height: dot)))
                    }
                }
            }
            gc.drawLayer { halo in
                halo.addFilter(.blur(radius: 4))
                halo.fill(pips, with: .color(LED.glow(color).opacity(0.8)))
            }
            gc.fill(pips, with: .color(color))
        }
        .shadow(color: LED.glow(color).opacity(0.6), radius: 6)
    }
}

#Preview {
    @Previewable @State var chance: Chance? = .coin
    ChanceOverlay(chance: $chance)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(LEDMatrixBackground())
}
