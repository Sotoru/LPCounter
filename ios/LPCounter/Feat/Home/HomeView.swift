//
//  HomeView.swift
//  LPCounter
//

import SwiftUI

/// The root (SPEC §9): die, coin, timer and settings on top; the call to duel centred, the totals of a
/// duel in progress under it, and Play.
struct HomeView: View {
    let model: GameViewModel
    @State private var isPlaying = false
    @State private var isShowingSettings = false
    @State private var chance: Chance?

    private static let catchphrase = "IT'S TIME TO"

    private var inProgress: Bool { model.inProgress }

    var body: some View {
        // One screen, no scrolling: the call to duel in the middle, Play under it.
        hero
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal)
            // Settings alone at the top; die, coin and timer within thumb's reach at the bottom.
            .overlay(alignment: .topTrailing) { settingsButton.padding(.horizontal, 20).padding(.top, 4) }
            .safeAreaInset(edge: .bottom) { bottomButtons }
        .overlay { ChanceOverlay(chance: $chance) }
        .animation(.smooth(duration: 0.2), value: chance)
        .background { LEDMatrixBackground() }
        .hidesBars()
        .sensoryFeedback(.impact(weight: .medium), trigger: isPlaying) { _, new in new }
        .sheet(isPresented: $isShowingSettings) {
            NavigationStack {
                SettingsView(model: model)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done", systemImage: "checkmark") { isShowingSettings = false }
                        }
                    }
            }
            #if os(macOS)
            .frame(minWidth: 560, idealWidth: 600, minHeight: 680, idealHeight: 760)
            #endif
        }
        .navigationDestination(isPresented: $isPlaying) {
            GameView(model: model)
        }
    }

    private func play() {
        model.startDuel()
        #if os(iOS)
        // Turn to landscape first, then push: never both at once.
        Task {
            await AppOrientation.lockForDuel()
            isPlaying = true
        }
        #else
        isPlaying = true
        #endif
    }

    // MARK: Top

    /// Die and coin on the left, for a quick throw without starting a duel; settings on the right.
    /// Die and coin on the left; the round timer and settings on the right.
    private var bottomButtons: some View {
        HStack {
            GlassEffectContainer(spacing: 8) {
                HStack(spacing: 8) { ChanceButtons(chance: $chance) }
            }
            .font(.title3)
            .controlSize(.large)
            Spacer(minLength: 8)
            GlassEffectContainer(spacing: 8) {
                HStack(spacing: 8) { timerControls }
            }
            .animation(.smooth, value: model.hasTimer)
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 8)
    }

    /// The round timer and, when there is one, its remove button (SPEC §9).
    @ViewBuilder
    private var timerControls: some View {
        HomeTimerButton(model: model)
        if model.hasTimer {
            Button(role: .destructive) {
                withAnimation(.smooth) { model.removeTimer() }
            } label: {
                Image(systemName: "xmark")
                    .font(.body.weight(.bold))
                    // Same height as the timer and the gear next to it.
                    .frame(width: 44, height: 44)
            }
            .accessibilityLabel("Remove timer")
            .buttonStyle(.glass)
            .buttonBorderShape(.circle)
            .transition(.scale(scale: 0.5).combined(with: .opacity))
        }
    }

    private var settingsButton: some View {
        Button {
            isShowingSettings = true
        } label: {
            Image(systemName: "gearshape.fill").font(.title3).frame(width: 44, height: 44)
        }
        .keyboardShortcut(",", modifiers: .command)
        .buttonStyle(.glass)
        .buttonBorderShape(.circle)
        .accessibilityLabel("Settings")
    }

    private var hero: some View {
        VStack(spacing: 36) {
            // The series' catchphrase, kept in English in every language.
            // One word per line, like a stacked poster; the last word is the button that starts it.
            VStack(spacing: 8) {
                ForEach(Self.catchphrase.split(separator: " ").map(String.init), id: \.self) { word in
                    DotMatrixText(text: word, maxScale: 2)
                }
            }
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text(verbatim: "It's time to duel"))
            .accessibilityAddTraits(.isHeader)
            if inProgress {
                // Where the duel left off, as on the board's two halves.
                HStack(spacing: 28) {
                    total(.one)
                    total(.two)
                }
                .accessibilityElement(children: .combine)
                .transition(.opacity)
            }
            Button(action: play) {
                // "DUEL" ends the catchphrase; once a duel is under way it resumes it.
                Group { if inProgress { Text("Resume") } else { Text(verbatim: "DUEL") } }
                    .font(.title.weight(.bold))
                    // Dark on the pale blue fill: white on it is too faint.
                    .foregroundStyle(LED.background)
                    .frame(maxWidth: 300, minHeight: 64)
            }
            .buttonStyle(.glassProminent)
            .buttonBorderShape(.capsule)
            .controlSize(.extraLarge)
            .keyboardShortcut(.defaultAction)
        }
    }

    private func total(_ player: Player) -> some View {
        let lp = model.score(player).lifePoints
        return VStack(spacing: 6) {
            Text(player == .one ? "Player 1" : "Player 2").led(13, color: LED.dim, glow: false)
            SegmentReadout(text: String(lp), digits: 4, size: 30, color: lp == 0 ? LED.red : LED.lit)
        }
    }
}

#Preview {
    NavigationStack { HomeView(model: GameViewModel(defaults: nil)) }
        .tint(LED.lit)
        .preferredColorScheme(.dark)
}

