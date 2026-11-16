//
//  PlatformModifiers.swift
//  LPCounter
//
//  The few modifiers whose API differs between iOS / iPadOS and macOS.
//

import SwiftUI

extension View {
    /// Full-screen surfaces (Home, game): no navigation bar on iOS; on macOS the window toolbar
    /// keeps its native traffic lights and the content runs under it.
    func hidesBars() -> some View {
        #if os(iOS)
        toolbarVisibility(.hidden, for: .navigationBar, .tabBar)
        #else
        toolbarBackgroundVisibility(.hidden, for: .windowToolbar)
        #endif
    }
}

extension View {
    /// No interactive way back (edge swipe, iOS 26 full-width swipe): the screen's own swipes need the
    /// whole surface, and leaving goes through its own button. macOS has no such gesture.
    func disablesSwipeBack() -> some View {
        #if os(iOS)
        navigationBarBackButtonHidden().background(SwipeBackBlocker().frame(width: 0, height: 0))
        #else
        self
        #endif
    }
}

#if os(iOS)
/// Turns the navigation controller's pop gestures off while its screen is shown, back on as it leaves.
private struct SwipeBackBlocker: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> Controller { Controller() }
    func updateUIViewController(_ controller: Controller, context: Context) {}

    final class Controller: UIViewController {
        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            setPopGestures(enabled: false)
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            setPopGestures(enabled: true)
        }

        private func setPopGestures(enabled: Bool) {
            navigationController?.interactivePopGestureRecognizer?.isEnabled = enabled
            navigationController?.interactiveContentPopGestureRecognizer?.isEnabled = enabled
        }
    }
}
#endif

#if os(iOS)
/// The duel is always in landscape on iPhone; the Home and Settings follow the device. To keep the
/// rotation out of the push, the Home turns the screen *before* pushing the game, and the game
/// releases it only once it is gone. iPad and Mac lay the board out by the shape of the window instead.
@MainActor enum AppOrientation {
    static private(set) var mask: UIInterfaceOrientationMask = .all

    /// Locks to landscape and returns once the screen is in it: at most one rotation, none if it already
    /// is. Does nothing on iPad.
    static func lockForDuel() async {
        guard UIDevice.current.userInterfaceIdiom == .phone, let scene = activeScene else { return }
        mask = .landscape
        scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        guard !isLandscape(scene) else { return }
        scene.requestGeometryUpdate(.iOS(interfaceOrientations: .landscape))
        // ponytail: polls the window shape (≤ 0.8 s) instead of observing it; the rotation takes ~0.4 s.
        for _ in 0..<40 where !isLandscape(scene) {
            try? await Task.sleep(for: .milliseconds(20))
        }
    }

    /// Before leaving the duel: back to every orientation and, if the device is held another way, turns
    /// the screen there first and returns once it has — so the pop lands on a Home already the
    /// right way up, never a landscape Home that then rotates.
    static func releaseForHome() async {
        guard mask != .all, let scene = activeScene else { return }
        mask = .all
        scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        let held = heldOrientation
        guard held.contains(.portrait) || held.contains(.portraitUpsideDown), isLandscape(scene) else { return }
        scene.requestGeometryUpdate(.iOS(interfaceOrientations: held))
        for _ in 0..<40 where isLandscape(scene) {
            try? await Task.sleep(for: .milliseconds(20))
        }
    }

    /// Safety net when the duel goes away without the Home button (e.g. a system gesture).
    static func unlock() {
        guard mask != .all, let scene = activeScene else { return }
        mask = .all
        scene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
        scene.requestGeometryUpdate(.iOS(interfaceOrientations: heldOrientation))
    }

    /// How the device is actually held; flat on the table counts as portrait.
    private static var heldOrientation: UIInterfaceOrientationMask {
        switch UIDevice.current.orientation {
        case .landscapeLeft: .landscapeRight
        case .landscapeRight: .landscapeLeft
        case .portraitUpsideDown: .portraitUpsideDown
        default: .portrait
        }
    }

    private static var activeScene: UIWindowScene? {
        UIApplication.shared.connectedScenes.first { $0.activationState == .foregroundActive } as? UIWindowScene
    }

    private static func isLandscape(_ scene: UIWindowScene) -> Bool {
        guard let bounds = scene.keyWindow?.bounds else { return true }
        return bounds.width > bounds.height
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        AppOrientation.mask
    }
}
#endif
