import SwiftUI

struct DeviceOrientationModifier: ViewModifier {
    @Binding var isLandscape: Bool

    func body(content: Content) -> some View {
        GeometryReader { geometry in
            content
                .onAppear {
                    isLandscape = geometry.size.width > geometry.size.height
                }
                .onChange(of: geometry.size) { _, newValue in
                    isLandscape = newValue.width > newValue.height
                }
        }
    }
}

extension View {
    func detectDeviceOrientation(isLandscape: Binding<Bool>) -> some View {
        self.modifier(DeviceOrientationModifier(isLandscape: isLandscape))
    }
}
