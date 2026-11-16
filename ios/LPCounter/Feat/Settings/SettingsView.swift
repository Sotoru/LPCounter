//
//  SettingsView.swift
//  LPCounter
//

import SwiftUI
import WidgetKit

/// App-only preferences (SPEC §5). The starting LP lives in `GameViewModel` because reset needs it.
enum AppSettings {
    static let confirmResetKey = "confirmReset"
    static let autoEndKey = "autoEndDuel"
    static let hapticsKey = "haptics"
    /// Seconds without a touch before the duel screen dims; 0 never.
    static let dimAfterKey = "dimAfter"
    static let dimAfterOptions = [0, 30, 60, 120, 300]
}

/// Grouped by what they touch: the rules of a duel, how the duel screen behaves, the look, and data.
struct SettingsView: View {
    let model: GameViewModel

    @AppStorage(AppSettings.confirmResetKey) private var confirmReset = true
    @AppStorage(AppSettings.autoEndKey) private var autoEnd = true
    @AppStorage(AppSettings.hapticsKey) private var haptics = true
    @AppStorage(PixelStyle.key, store: LED.store) private var pixels = PixelStyle.round
    @AppStorage(LEDPalette.key, store: LED.store) private var palette = LEDPalette.night
    @AppStorage(AppSettings.dimAfterKey) private var dimAfter = 60
    @State private var isConfirmingClear = false

    var body: some View {
        Form {
            rules
            duelScreen
            appearance
            data
            about
        }
        // On the Mac the default form puts labels and values in two columns, truncating both.
        .formStyle(.grouped)
        .scrollContentBackground(.hidden)
        .background(LEDMatrixBackground())
        .navigationTitle("Settings")
        #if os(iOS)
        .navigationBarTitleDisplayMode(.inline)
        #endif
        .confirmationDialog("Delete all data?", isPresented: $isConfirmingClear, titleVisibility: .visible) {
            Button("Delete", role: .destructive, action: model.clearAll)
        } message: {
            Text("Life points, history and timer. Settings are kept.")
        }
    }

    // MARK: Sections

    private var rules: some View {
        Section {
            Picker(selection: Binding(get: { model.startingLifePoints }, set: { model.setStartingLifePoints($0) })) {
                ForEach(GameConstants.startingLifePointsOptions, id: \.self) { lp in
                    Text(lp == 4000 ? "4000 · Speed Duel" : String(lp)).tag(lp)
                }
            } label: {
                Label("Starting life points", systemImage: "heart.fill")
            }
            Picker(selection: Binding(get: { model.timerMinutes }, set: { model.setTimerMinutes($0) })) {
                Text("Off").tag(0)
                ForEach(timerChoices, id: \.self) { TimerOptionLabel(minutes: $0).tag($0) }
            } label: {
                Label("Round timer", systemImage: "timer")
            }
            Toggle(isOn: $autoEnd) {
                Label {
                    Text("End duel automatically")
                    Text("At 0 life points: who wins, and New duel in place of reset")
                } icon: { Image(systemName: "flag.checkered") }
            }
        } header: {
            header("Duel")
        } footer: {
            Text("A new starting value applies now if the duel hasn't started, otherwise at the next reset. The timer starts with Play.")
        }
        .listRowBackground(LED.panel)
    }

    private var duelScreen: some View {
        Section {
            Toggle(isOn: $confirmReset) {
                Label {
                    Text("Confirm before resetting")
                    Text("↺ asks before both totals go back")
                } icon: { Image(systemName: "arrow.counterclockwise") }
            }
            Toggle(isOn: $haptics) {
                Label {
                    Text("Haptics")
                    Text("A tap on every life point change")
                } icon: { Image(systemName: "iphone.radiowaves.left.and.right") }
            }
            Picker(selection: $dimAfter) {
                ForEach(AppSettings.dimAfterOptions, id: \.self) { seconds in
                    if seconds == 0 { Text("Never") } else if seconds < 60 { Text("\(seconds) s") } else { Text("\(seconds / 60) min") }
                }
            } label: {
                Label {
                    Text("Dim screen")
                    Text("Saves battery when idle")
                } icon: { Image(systemName: "moon.fill") }
            }
        } header: {
            header("Duel screen")
        } footer: {
            Text("The screen stays on during a duel. Swipe on a total: right/left ±100, up/down ±1000.")
        }
        .listRowBackground(LED.panel)
    }

    private var appearance: some View {
        Section {
            // The duel's own grid, so the preview shows the dots as they will look at the table.
            DotMatrixNumber(value: Double(model.startingLifePoints), glow: false)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background { LEDMatrixBackground() }
                .environment(\.ledPitch, 12)
                .listRowInsets(EdgeInsets())
                .accessibilityHidden(true)
            Picker("Colour", selection: $palette) {
                ForEach(LEDPalette.allCases) { option in
                    Label { Text(option.title) } icon: {
                        Image(systemName: "circle.fill").foregroundStyle(option.lit, option.lit)
                    }
                    .tag(option)
                }
            }
            #if os(iOS)
            .pickerStyle(.navigationLink)
            #endif
            Picker("Pixel style", selection: $pixels.animation(.smooth)) {
                ForEach(PixelStyle.allCases) { Text($0.title).tag($0) }
            }
            .pickerStyle(.segmented)
        } header: {
            header("Appearance")
        } footer: {
            Text("Colour and dot shape, also in widgets and the Live Activity.")
        }
        .onChange(of: pixels) { WidgetCenter.shared.reloadAllTimelines() }
        .onChange(of: palette) { WidgetCenter.shared.reloadAllTimelines() }
        .listRowBackground(LED.panel)
    }

    private var data: some View {
        Section {
            Button("Delete all data", systemImage: "trash", role: .destructive) { isConfirmingClear = true }
        } header: {
            header("Data")
        } footer: {
            Text("Life points, history and timer. Settings are kept.")
        }
        .listRowBackground(LED.panel)
    }

    private var about: some View {
        Section {
            LabeledContent("Version", value: Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "–")
            LabeledContent("Fonts") { Text(verbatim: "DSEG7 · OFL") }
        } header: {
            header("About")
        }
        .listRowBackground(LED.panel)
    }

    // MARK: Helpers

    /// The presets, plus a custom length set from the Home so it shows as chosen.
    private var timerChoices: [Int] {
        let presets = GameConstants.timerPresets
        return model.timerMinutes == 0 || presets.contains(model.timerMinutes) ? presets : presets + [model.timerMinutes]
    }

    private func header(_ title: LocalizedStringKey) -> some View {
        Text(title).led(13)
    }
}

#Preview {
    NavigationStack { SettingsView(model: GameViewModel(defaults: nil)) }
        .tint(LED.lit)
        .preferredColorScheme(.dark)
}
