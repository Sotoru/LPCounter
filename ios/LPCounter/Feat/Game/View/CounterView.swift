//
//  CounterView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI

struct CounterView: View {
    let model: GameViewModel
    let player: Player
    let isLandscape: Bool

    @AppStorage(AppSettings.autoEndKey) private var autoEnd = true
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.introStep) private var introStep
    @State private var isSheetPresented = false
    @State private var showsHistory = false
    /// Last change, replayed by the hit animation each time `hits` increments.
    @State private var lastDelta = 0
    @State private var hits = 0
    /// Set by the halve button just before the change, so it gets its own effect.
    @State private var requested: CounterEffect?
    /// The reading the effects last handled; a change from it is classified once.
    @State private var seen: Reading?
    @State private var effect = CounterEffect.reset
    @State private var effectFrom = 0
    @State private var effectRuns = 0

    private var score: PlayerScore { model.score(player) }
    private var isOver: Bool { autoEnd && model.isOver }

    var body: some View {
        VStack(spacing: 24) {
            ZStack {
                if showsHistory {
                    HistoryView(history: score.history)
                        .clipShape(.rect(cornerRadius: 20))
                        .transition(.opacity)
                } else {
                    totalControl.transition(.opacity)
                }
            }
            // One fixed area for the total and the history, so the buttons below stay where they are.
            .frame(maxWidth: .infinity).frame(height: Self.displayHeight)
            // Dark until the board powers on.
            .opacity(introStep >= 1 ? 1 : 0)

            Group {
                if isOver {
                    resultBadge()
                        .transition(.blurReplace)
                } else {
                    actions()
                        .transition(.blurReplace)
                }
            }
            .opacity(introStep >= 2 ? 1 : 0)
            .offset(y: introStep >= 2 ? 0 : 12)
        }
        // No side padding: the card's own is enough, and a four-digit total needs the width to stay on the grid.
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background { flash }
        .onAppear { seen = reading }
        // The board's power-on: the total lights with the same lamp test as a reset.
        .onChange(of: introStep) { _, step in
            guard step == 1 else { return }
            effect = .reset
            effectFrom = score.lifePoints
            effectRuns += 1
        }
        .onChange(of: reading) { old, new in
            let kind = classify(old, new)
            seen = new
            requested = nil
            guard old.lp != new.lp else { return }
            lastDelta = new.lp - old.lp
            // The lamp test is the reset's feedback: no shake or floating delta.
            if kind != .reset { hits += 1 }
            if let kind, !reduceMotion {
                effect = kind
                effectFrom = old.lp
                effectRuns += 1
            }
        }
        // Plain changes count to the new total; reset and halve switch at once under their effect.
        .animation(pendingEffect == nil ? .snappy : nil, value: score.lifePoints)
        .animation(.smooth, value: isOver)
        .sheet(isPresented: $isSheetPresented) {
            CalculatorSheet(model: model, player: player, isPresented: $isSheetPresented, isLandscape: isLandscape)
                .rotationEffect(.degrees(player == .two && !isLandscape ? 180 : 0))
                .presentationDetents([.large])
        }
    }

    /// Height of the area above the buttons: a total at full size, or the history in its place.
    private static let displayHeight: CGFloat = 140

    private var totalControl: some View {
        // Tap opens the keypad; a swipe changes the total by `swipeStep` (SPEC §4).
        total
                .contentShape(.rect)
                .onTapGesture { if !isOver { isSheetPresented = true } }
                .gesture(DragGesture(minimumDistance: 24).onEnded(swipe), isEnabled: !isOver)
                .accessibilityElement()
                .accessibilityAddTraits(.isButton)
                .accessibilityLabel(player == .one ? "Player 1 life points" : "Player 2 life points")
                .accessibilityValue(String(score.lifePoints))
                .accessibilityHint(isOver ? "" : "Opens the keypad. Swipe up or down to change by 100.")
                .accessibilityAction { if !isOver { isSheetPresented = true } }
                .accessibilityAdjustableAction { direction in
                    guard !isOver else { return }
                    model.changeBy(player, direction == .increment ? 100 : -100)
                }
                .accessibilityAction(named: "Add 1000") { if !isOver { model.changeBy(player, 1000) } }
                .accessibilityAction(named: "Subtract 1000") { if !isOver { model.changeBy(player, -1000) } }
    }

    private var reading: Reading { Reading(lp: score.lifePoints, resets: model.resets) }

    private var pendingEffect: CounterEffect? {
        guard let seen, seen != reading else { return nil }
        return classify(seen, reading)
    }

    /// Halve comes from its button; a game reset (both cards, also "New duel" and leaving) bumps the
    /// model's reset count.
    private func classify(_ old: Reading, _ new: Reading) -> CounterEffect? {
        if let requested { return requested }
        if new.resets != old.resets && new.lp != old.lp { return .reset }
        return nil
    }

    /// Right +100, left −100, up +1000, down −1000, along the swipe's main axis. Translations are in
    /// the card's own space, so Player 2's card, turned 180° in portrait, reads from its player's side.
    private func swipe(_ value: DragGesture.Value) {
        model.changeBy(player, Self.swipeDelta(value.translation))
    }

    static func swipeDelta(_ t: CGSize) -> Int {
        abs(t.width) > abs(t.height) ? (t.width > 0 ? 100 : -100) : (t.height < 0 ? 1000 : -1000)
    }

    private var total: some View {
        KeyframeAnimator(initialValue: EffectFrame(), trigger: effectRuns) { frame in
            // The app's one grid, as on the Home: each font dot a block of grid dots, as large as the card
            // allows (a fifth digit just picks a smaller block).
            DotMatrixNumber(value: Double(score.lifePoints), color: score.lifePoints == 0 ? LED.red : LED.lit, glow: false,
                            maxScale: 3, previous: effectFrom, morph: frame.morph, test: frame.test)
        } keyframes: { _ in
            EffectKeyframes(effect: effect)
        }
            .frame(maxWidth: .infinity, maxHeight: Self.displayHeight)
            // Clear of the other player's total across the seam.
            .padding(.horizontal, 28)
            .keyframeAnimator(initialValue: Hit(), trigger: hits) { content, hit in
                content.offset(x: hit.shake).scaleEffect(hit.scale)
            } keyframes: { _ in hitKeyframes() }
            .overlay { floatingDelta }
    }

    /// Damage shakes the total, a gain makes it pop; reduce motion keeps it still.
    private func hitKeyframes() -> some Keyframes<Hit> {
        let shake = reduceMotion || lastDelta > 0 ? [0.0] : [-10.0, 9, -6, 4, -2, 0]
        let pop = reduceMotion || lastDelta < 0 ? 1.0 : 1.08
        return HitKeyframes(shake: shake, pop: pop)
    }

    private var lost: Bool { isOver && score.lifePoints == 0 }
    private var won: Bool { isOver && score.lifePoints > 0 }

    /// The change, rising from the total and fading out.
    private var floatingDelta: some View {
        Text(verbatim: lastDelta > 0 ? "+\(lastDelta)" : "\(lastDelta)")
            .led(26, color: lastDelta > 0 ? LED.green : LED.red)
            .keyframeAnimator(initialValue: FloatingDelta(), trigger: hits) { content, f in
                content.opacity(f.opacity).offset(y: f.y).scaleEffect(f.scale)
            } keyframes: { _ in
                KeyframeTrack(\.opacity) {
                    LinearKeyframe(1, duration: 0.05)
                    LinearKeyframe(1, duration: 0.55)
                    LinearKeyframe(0, duration: 0.4)
                }
                KeyframeTrack(\.y) {
                    LinearKeyframe(-30, duration: 0.05)
                    SpringKeyframe(reduceMotion ? -30 : -90, duration: 0.95)
                }
                KeyframeTrack(\.scale) {
                    SpringKeyframe(reduceMotion ? 1 : 1.15, duration: 0.2)
                    SpringKeyframe(1, duration: 0.8)
                }
            }
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }

    /// A glow behind the total: red on damage, green on gain; steady for the winner.
    private var flash: some View {
        let tint: Color = lastDelta > 0 ? LED.green : LED.red
        return ZStack {
            RadialGradient(colors: [LED.lit.opacity(won ? 0.25 : 0), .clear], center: .center, startRadius: 0, endRadius: 260)
                .phaseAnimator(won && !reduceMotion ? [0.7, 1.0] : [1.0]) { content, p in content.opacity(p) }
                    animation: { _ in .easeInOut(duration: 1.4) }
            RadialGradient(colors: [tint.opacity(0.3), .clear], center: .center, startRadius: 0, endRadius: 240)
                .keyframeAnimator(initialValue: 0.0, trigger: hits) { content, o in content.opacity(o) } keyframes: { _ in
                    LinearKeyframe(1, duration: 0.08)
                    LinearKeyframe(0, duration: 0.6)
                }
        }
        // Room past the card, so the glow fades out instead of ending on the card's edge.
        .padding(-120)
        .saturation(lost ? 0.3 : 1)
        .allowsHitTesting(false)
    }

    @ViewBuilder
    private func actions() -> some View {
        GlassEffectContainer {
            HStack(spacing: 12) {
                // Same place in both states, only the symbol changes.
                Button(showsHistory ? "Life points" : "History",
                       systemImage: showsHistory ? "number" : "clock.arrow.circlepath") {
                    withAnimation(reduceMotion ? nil : .smooth) { showsHistory.toggle() }
                }
                .contentTransition(.symbolEffect(.replace))
                Button("Undo", systemImage: "arrow.uturn.backward") { model.undo(player) }
                    .disabled(score.history.isEmpty)
                Button("Halve life points", systemImage: "divide") {
                    // Halving 1 leaves 1: no change, so no effect waiting for one.
                    if score.lifePoints > 1 { requested = .halve }
                    model.halve(player)
                }
                .disabled(score.lifePoints == 0)
            }
            .labelStyle(.iconOnly)
            .font(.title2.weight(.semibold))
            .buttonStyle(.glass)
            .controlSize(.large)
        }
    }

    @ViewBuilder
    private func resultBadge() -> some View {
        let lost = score.lifePoints == 0
        Text(lost ? "Defeated" : "Winner")
            .led(30, color: lost ? LED.red : LED.green)
            .phaseAnimator(lost || reduceMotion ? [1.0] : [1.0, 0.55]) { content, o in content.opacity(o) }
                animation: { _ in .easeInOut(duration: 0.7) }
    }
}

private struct Reading: Equatable {
    var lp: Int
    var resets: Int
}

private nonisolated enum CounterEffect {
    /// Lamp test: every dot lights from the bottom up, then the new total is revealed from the top.
    case reset
    /// The old total dissolves dot by dot into its half.
    case halve
}

private nonisolated struct EffectFrame {
    var morph = 1.0
    var test = 0.0
}

private nonisolated struct EffectKeyframes: Keyframes {
    let effect: CounterEffect

    var body: some Keyframes<EffectFrame> {
        KeyframeTrack(\.test) {
            if effect == .reset {
                LinearKeyframe(1, duration: 0.18)
                LinearKeyframe(1, duration: 0.14)
                CubicKeyframe(0, duration: 0.5)
            } else {
                LinearKeyframe(0, duration: 0.01)
            }
        }
        KeyframeTrack(\.morph) {
            if effect == .halve {
                LinearKeyframe(0, duration: 0)
                LinearKeyframe(0, duration: 0.12)
                LinearKeyframe(1, duration: 0.75)
            } else {
                LinearKeyframe(1, duration: 0.01)
            }
        }
    }
}

private struct Hit {
    var shake = 0.0
    var scale = 1.0
}

/// Keyframes for `Hit`, built from plain values.
private struct HitKeyframes: Keyframes {
    let shake: [Double]
    let pop: Double

    var body: some Keyframes<Hit> {
        KeyframeTrack(\.shake) {
            for x in shake { LinearKeyframe(x, duration: 0.05) }
        }
        KeyframeTrack(\.scale) {
            SpringKeyframe(pop, duration: 0.15)
            SpringKeyframe(1, duration: 0.35)
        }
    }
}

private struct FloatingDelta {
    var opacity = 0.0
    var y = 0.0
    var scale = 1.0
}

private struct CalculatorSheet: View {
    let model: GameViewModel
    let player: Player
    @Binding var isPresented: Bool
    let isLandscape: Bool

    var body: some View {
        NavigationStack {
            CalculatorView(player: player, lifePoints: model.score(player).lifePoints, isLandscape: isLandscape) { delta in
                let before = model.score(player).lifePoints
                model.changeBy(player, delta)
                // The sheet closes only when the total actually changed (SPEC §3).
                if model.score(player).lifePoints != before { isPresented = false }
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .close) { isPresented = false }
                }
            }
        }
        .presentationBackground { LEDMatrixBackground() }
    }
}

#Preview {
    CounterView(model: GameViewModel(defaults: nil), player: .one, isLandscape: false)
}
