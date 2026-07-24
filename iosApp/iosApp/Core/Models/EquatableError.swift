import Foundation
import SharedLogic

struct EquatableError: Error, Equatable {
    let underlying: Error

    static func == (lhs: EquatableError, rhs: EquatableError) -> Bool {
        String(reflecting: lhs.underlying) == String(reflecting: rhs.underlying)
    }
}

extension Error {
    var equatable: EquatableError {
        EquatableError(underlying: self)
    }
}

struct EquatableVoid: Equatable {}

extension EquatableError {
    /// Kotlin exceptions crossing the FFI boundary (via KMP-NativeCoroutines or a `@Throws`
    /// completion handler) arrive as `NSError`, with the original `ApiException` recoverable
    /// through the generated `NSError.kotlinException` property. This maps that back to a
    /// canned, localized message instead of surfacing raw Ktor/Foundation error text to the user.
    var userFacingMessage: LocalizedStringResource {
        let apiException = (underlying as NSError).kotlinException as? ApiException
        switch apiException {
        case is ApiException.Network:
            return LocalizedStringResource(
                "error_network",
                defaultValue: "You're offline and nothing is cached yet. Check your connection and try again."
            )
        case is ApiException.Serialization:
            return LocalizedStringResource(
                "error_serialization",
                defaultValue: "Something went wrong reading that response. Please try again."
            )
        case is ApiException.Http:
            return LocalizedStringResource(
                "error_server",
                defaultValue: "The server couldn't complete this request. Please try again."
            )
        default:
            return LocalizedStringResource(
                "error_generic",
                defaultValue: "Something went wrong. Please try again."
            )
        }
    }
}
