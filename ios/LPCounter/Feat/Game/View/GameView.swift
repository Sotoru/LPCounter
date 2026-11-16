//
//  GameView.swift
//  LPCounter
//
//  Created by paolo laddomada on 30/10/25.
//

import SwiftUI
import WidgetKit

/// Full-screen duel pushed from Home (SPEC §4): no navigation or tab bar.
struct GameView: View {
    let model: GameViewModel
    @State private var isConfirmingReset = false
    @State private var isConfirmingLeave = false
    @State private var chance: Chance?
    @AppStorage(AppSettings.dimAfterKey) private var dimAfter = 60
    @Environment(\.accessibilityVoiceOverEnabled) private var voiceOver
    /// Bumped on every touch or change; the dimming countdown restarts from it.
    @State private var activity = 0
    @State private var isDimmed = false
    /// The power-on intro (SPEC §4): sweep across the matrix, lamp test on the totals, then the controls.
    @State private var sweep = 0.0
    @State private var introStep = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dismiss) private var dismiss

    @AppStorage(AppSettings.confirmResetKey) private var confirmReset = true
    @AppStorage(AppSettings.autoEndKey) private var autoEnd = true
    @AppStorage(AppSettings.hapticsKey) private var haptics = true
    var body: some View {
        // The shape is read in the same pass that lays out the board, so the first frame is already right.
        GeometryReader { proxy in
            let landscape = proxy.size.width > proxy.size.height
            VStack(spacing: 0) {
                toolbar
                    .opacity(introStep >= 2 ? 1 : 0)
                    .offset(y: introStep >= 2 ? 0 : -12)
                countersStack(landscape)
            }
            .frame(width: proxy.size.width, height: proxy.size.height)
            .animation(.smooth, value: model.hasTimer)
        }
        .environment(\.introStep, introStep)
        .background { LEDMatrixBackground(sweep: introStep >= 3 ? nil : sweep) }
        .task { await intro() }
        .simultaneousGesture(DragGesture(minimumDistance: 0).onChanged { _ in activity += 1 })
        .onChange(of: [model.p1.lifePoints, model.p2.lifePoints]) { activity += 1 }
        .overlay { ChanceOverlay(chance: $chance) }
        .animation(.smooth(duration: 0.2), value: chance)
        .overlay { dimmer }
        .task(id: activity) {
            withAnimation(.easeOut(duration: 0.3)) { isDimmed = false }
            // VoiceOver users can't see the board anyway, and a dim layer would swallow their first gesture.
            guard dimAfter > 0, !voiceOver else { return }
            try? await Task.sleep(for: .seconds(dimAfter))
            guard !Task.isCancelled else { return }
            withAnimation(.easeInOut(duration: 1.2)) { isDimmed = true }
        }
        .hidesBars()
        // Swipes on the totals change the life points; leaving is the Home button only (SPEC §4).
        .disablesSwipeBack()
        // VoiceOver's two-finger scrub, since there is no swipe back.
        .accessibilityAction(.escape, requestHome)
        .onChange(of: model.isOver) { _, isOver in
            guard isOver && autoEnd else { return }
            let winner = model.p1.lifePoints > 0 ? String(localized: "Player 1 wins") : String(localized: "Player 2 wins")
            AccessibilityNotification.Announcement(winner).post()
        }
        // Leaving a duel that was played: keep it for later, or end it (SPEC §4).
        .confirmationDialog("Leave the duel?", isPresented: $isConfirmingLeave, titleVisibility: .visible) {
            Button("Resume later") { goHome() }
            Button("End duel", role: .destructive) { model.resetGame(); goHome() }
        } message: {
            Text("Keep it to pick up from the Home, or end it now.")
        }
        .confirmationDialog("Reset the duel?", isPresented: $isConfirmingReset, titleVisibility: .visible) {
            Button("Reset game", role: .destructive, action: model.resetGame)
        } message: {
            Text("Life points go back to the starting value and the history is cleared.")
        }
        .sensoryFeedback(trigger: [model.p1.lifePoints, model.p2.lifePoints]) { haptics ? .impact(weight: .light) : nil }
        .sensoryFeedback(.success, trigger: model.isOver) { _, isOver in isOver && haptics && autoEnd }
        .onAppear {
            #if os(iOS)
            UIApplication.shared.isIdleTimerDisabled = true
            LiveActivityManager.shared.startActivity(model.p1.lifePoints, model.p2.lifePoints)
            #endif
        }
        .onDisappear {
            #if os(iOS)
            UIApplication.shared.isIdleTimerDisabled = false
            // No-op after the Home button, which already released it.
            AppOrientation.unlock()
            #endif
        }
    }

    /// Power-on, about a second: the band sweeps down the matrix (0), the totals light up with a lamp test
    /// (1), the controls fade in (2), then the board is still (3). Reduce Motion goes straight to the end.
    private func intro() async {
        guard introStep == 0 else { return }
        guard !reduceMotion else { introStep = 3; return }
        withAnimation(.easeInOut(duration: 0.7)) { sweep = 1 }
        try? await Task.sleep(for: .seconds(0.35))
        introStep = 1
        try? await Task.sleep(for: .seconds(0.45))
        withAnimation(.smooth(duration: 0.4)) { introStep = 2 }
        try? await Task.sleep(for: .seconds(0.4))
        introStep = 3
    }

    /// Asks what to do with a played duel; an untouched one just leaves.
    private func requestHome() {
        if model.inProgress { isConfirmingLeave = true } else { goHome() }
    }

    /// Turn back to how the device is held first, then go back to the Home: never both at once.
    private func goHome() {
        #if os(iOS)
        Task {
            await AppOrientation.releaseForHome()
            dismiss()
        }
        #else
        dismiss()
        #endif
    }

    /// Near-black over the board once idle: the totals still show faintly, OLED pixels mostly off.
    /// The first tap only wakes it, so a tap meant to wake never changes the life points.
    @ViewBuilder
    private var dimmer: some View {
        if isDimmed {
            Color.black.opacity(0.85)
                .ignoresSafeArea()
                .contentShape(.rect)
                .onTapGesture { activity += 1 }
                .transition(.opacity)
                .accessibilityHidden(true)
        }
    }

    private func requestReset() {
        if confirmReset { isConfirmingReset = true } else { model.resetGame() }
    }

    // Widgets and the Live Activity live outside the app UI, so they are pushed on every change.
    static func sync(_ p1: PlayerScore, _ p2: PlayerScore) {
        #if os(iOS)
        LiveActivityManager.shared.updateActivity(p1.lifePoints, p2.lifePoints)
        WatchLink.shared.push()
        #endif
        for kind in GameConstants.widgetKinds {
            WidgetCenter.shared.reloadTimelines(ofKind: kind)
        }
    }
}

// MARK: - Subviews
private extension GameView {
    @ViewBuilder
    func countersStack(_ isLandscape: Bool) -> some View {
        let stackLayout: AnyLayout = isLandscape ? AnyLayout(HStackLayout(spacing: 0)) : AnyLayout(VStackLayout(spacing: 0))
        // Portrait: Player 2 on top, facing Player 1. Landscape: Player 1 on the left (SPEC §4).
        // Each card gets exactly half: opening one player's history never moves the other's buttons.
        GeometryReader { g in
            stackLayout {
                ForEach(isLandscape ? [Player.one, .two] : [Player.two, .one], id: \.self) { player in
                    GameCardView(model: model, player: player, isLandscape: isLandscape)
                        .frame(width: isLandscape ? g.size.width / 2 : g.size.width,
                               height: isLandscape ? g.size.height : g.size.height / 2)
                }
            }
        }
    }

    /// Home at the left; in the centre the round timer (only when active), reset, which becomes
    /// "New duel" once the duel is over, then die and coin (SPEC §4).
    var toolbar: some View {
        // Home on its own at the left, the way out; the duel's tools grouped in the centre.
        ZStack {
            HStack {
                Button("Home", systemImage: "house.fill", action: requestHome)
                    .keyboardShortcut(.cancelAction)
                    .labelStyle(.iconOnly)
                    .buttonStyle(.glass)
                    .buttonBorderShape(.circle)
                Spacer()
            }
            GlassEffectContainer {
                HStack(spacing: 10) {
                    timerBadge
                    resetButton
                    ChanceButtons(chance: $chance)
                }
            }
        }
        .font(.title3.weight(.semibold))
        .controlSize(.large)
        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
        .frame(maxWidth: .infinity)
        // Past the rounded corner and the Dynamic Island's side.
        .padding(.horizontal, 24)
        .padding(.top, 8)
        .animation(.smooth, value: model.isOver)
    }

    @ViewBuilder
    var resetButton: some View {
        if autoEnd && model.isOver {
            Button("New duel", systemImage: "arrow.counterclockwise", action: model.resetGame)
                .keyboardShortcut("r", modifiers: .command)
                .foregroundStyle(LED.background)
                .buttonStyle(.glassProminent)
                .buttonBorderShape(.capsule)
                .transition(.scale(scale: 0.8).combined(with: .opacity))
        } else {
            Button("Reset game", systemImage: "arrow.counterclockwise", action: requestReset)
                .keyboardShortcut("r", modifiers: .command)
                .labelStyle(.iconOnly)
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .transition(.scale(scale: 0.8).combined(with: .opacity))
        }
    }
}

// MARK: - Timer
private extension GameView {
    var timeLeft: Text {
        let left = model.timerEndsAt.map { max(0, $0.timeIntervalSinceNow) } ?? model.timerPausedRemaining ?? 0
        return Text(Duration.seconds(left.rounded()), format: .units(allowed: [.hours, .minutes, .seconds], width: .wide))
    }

    /// The countdown, centred in the top bar, only when a timer was started from the Home.
    /// Tapping pauses or resumes it; removing it is done from the Home.
    @ViewBuilder
    var timerBadge: some View {
        if model.hasTimer {
            Button {
                if model.timerEndsAt != nil { model.pauseTimer() } else { model.resumeTimer() }
            } label: {
                Label {
                    TimerReadout(model: model, size: 18)
                } icon: {
                    Image(systemName: model.timerEndsAt == nil ? "play.fill" : "pause.fill")
                        .contentTransition(.symbolEffect(.replace))
                }
                .font(.title3.weight(.semibold))
                .padding(.horizontal, 6)
            }
            #if os(macOS)
            // The Mac draws glass tinted in the lit colour, hiding the lit digits: a dark capsule instead.
            .buttonStyle(.plain)
            .padding(.vertical, 6)
            .padding(.horizontal, 8)
            .background(LED.panel, in: .capsule)
            .overlay(Capsule().strokeBorder(LED.dim.opacity(0.5)))
            #else
            .buttonStyle(.glass)
            .buttonBorderShape(.capsule)
            #endif
            .transition(.scale(scale: 0.6).combined(with: .opacity))
            .accessibilityLabel(model.timerEndsAt == nil ? "Resume timer" : "Pause timer")
            .accessibilityValue(timeLeft)
        }
    }
}

/// Off, the presets and a custom length; the checkmark follows the current choice. Shared by the Home
/// and the game so both offer the same options.
struct TimerMenuItems: View {
    let model: GameViewModel

    var body: some View {
        Picker("Duration", selection: Binding(get: { model.timerMinutes }, set: { model.setTimerMinutes($0) })) {
            Label("Off", systemImage: "timer.slash").tag(0)
            ForEach(GameConstants.timerPresets, id: \.self) { minutes in
                Label { TimerOptionLabel(minutes: minutes) } icon: { Image(systemName: "timer") }.tag(minutes)
            }
            if isCustom {
                Label { TimerOptionLabel(minutes: model.timerMinutes) } icon: { Image(systemName: "slider.horizontal.3") }
                    .tag(model.timerMinutes)
            }
        }
        .pickerStyle(.inline)
        Button("Custom…", systemImage: "pencil") { model.isEditingTimer = true }
    }

    private var isCustom: Bool {
        model.timerMinutes != 0 && !GameConstants.timerPresets.contains(model.timerMinutes)
    }
}

/// A small sheet to set any round length, with a native stepper and the presets as shortcuts.
struct TimerEditor: View {
    let model: GameViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var minutes = 50

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Stepper(value: $minutes, in: GameConstants.timerRange, step: 5) {
                        LabeledContent("Duration") {
                            Text("\(minutes) min").monospacedDigit().contentTransition(.numericText(value: Double(minutes)))
                        }
                    }
                } footer: {
                    Text("Tournament matches usually last 45 or 50 minutes.")
                }
                Section("Presets") {
                    ForEach(GameConstants.timerPresets, id: \.self) { preset in
                        Button {
                            withAnimation(.snappy) { minutes = preset }
                        } label: {
                            LabeledContent { if minutes == preset { Image(systemName: "checkmark") } } label: {
                                TimerOptionLabel(minutes: preset)
                            }
                        }
                        .foregroundStyle(.primary)
                    }
                }
            }
            .formStyle(.grouped)
            .scrollContentBackground(.hidden)
            .background(LEDMatrixBackground())
            .navigationTitle("Timer")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel", role: .cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", systemImage: "checkmark") {
                        model.setTimerMinutes(minutes)
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium])
        #if os(macOS)
        .frame(minWidth: 420, minHeight: 420)
        #endif
        .onAppear { minutes = model.timerMinutes == 0 ? GameConstants.timerPresets.last ?? 50 : model.timerMinutes }
    }
}

/// The time left in seven segments: a live countdown while running (stays at 0:00 in red once up),
/// frozen and dimmed while paused.
struct TimerReadout: View {
    let model: GameViewModel
    var size: CGFloat = 18

    var body: some View {
        Group {
            if let endsAt = model.timerEndsAt {
                let color = endsAt <= .now ? LED.red : LED.lit
                Text(timerInterval: Date.now...max(endsAt, .now), countsDown: true)
                    .foregroundStyle(color)
                    .shadow(color: LED.glow(color).opacity(0.8), radius: size * 0.2)
            } else if let left = model.timerPausedRemaining {
                Text(Duration.seconds(left.rounded()), format: .time(pattern: left >= 3600 ? .hourMinuteSecond : .minuteSecond))
                    .foregroundStyle(LED.dim)
            }
        }
        .font(LED.segmentFont(size))
        .monospacedDigit()
    }
}

/// "Off" or "50 min", shared by the Home and the game.
struct TimerOptionLabel: View {
    let minutes: Int

    var body: some View {
        if minutes == 0 { Text("Off") } else { Text("\(minutes) min") }
    }
}

// MARK: - Previews
#Preview("Portrait", traits: .portrait) {
    NavigationStack { GameView(model: GameViewModel(defaults: nil)) }
}
#Preview("Landscape", traits: .landscapeLeft) {
    NavigationStack { GameView(model: GameViewModel(defaults: nil)) }
}
