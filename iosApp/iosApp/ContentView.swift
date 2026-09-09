import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.mainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // All edges, not just the keyboard. SwiftUI insetting the view and Compose then adding
            // WindowInsets.safeDrawing counted the notch twice, leaving the header a notch lower on
            // iOS than on Android from the same code.
            .ignoresSafeArea()
            .preferredColorScheme(.dark)
    }
}
