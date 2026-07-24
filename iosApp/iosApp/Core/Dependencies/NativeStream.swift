import KMPNativeCoroutinesAsync
import KMPNativeCoroutinesCore

/// Bridges a KMP-NativeCoroutines native Flow into a plain `AsyncThrowingStream`, converting each
/// emitted Kotlin value to its Swift mirror type via `map`. This keeps a dependency client's
/// surface - and test stubbing - independent of KMPNativeCoroutines' own async-sequence types.
func nativeStream<KotlinValue, SwiftValue, Failure: Error, Unit>(
    for nativeFlow: @escaping NativeFlow<KotlinValue, Failure, Unit>,
    map: @escaping @Sendable (KotlinValue) -> SwiftValue
) -> AsyncThrowingStream<SwiftValue, Error> {
    AsyncThrowingStream { continuation in
        let task = Task {
            do {
                for try await value in asyncSequence(for: nativeFlow) {
                    continuation.yield(map(value))
                }
                continuation.finish()
            } catch {
                continuation.finish(throwing: error)
            }
        }
        continuation.onTermination = { _ in task.cancel() }
    }
}
