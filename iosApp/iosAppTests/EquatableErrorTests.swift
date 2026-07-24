import XCTest
import SharedLogic
@testable import MovieApp

final class EquatableErrorTests: XCTestCase {
    private struct SampleError: Error {}
    private struct OtherError: Error {}

    func testSameErrorType_areEqual() {
        XCTAssertEqual(SampleError().equatable, SampleError().equatable)
    }

    func testDifferentErrorTypes_areNotEqual() {
        XCTAssertNotEqual(SampleError().equatable, OtherError().equatable)
    }

    func testUserFacingMessage_genericError_isGenericMessage() {
        XCTAssertEqual(
            SampleError().equatable.userFacingMessage,
            LocalizedStringResource("error_generic", defaultValue: "Something went wrong. Please try again.")
        )
    }

    // ApiException instances are converted via `asError()` to reproduce how a Kotlin exception
    // actually arrives in Swift after crossing the KMP-NativeCoroutines/Kotlin-Native FFI boundary
    // (as an NSError with the original exception recoverable through `NSError.kotlinException`).
    func testUserFacingMessage_apiExceptionNetwork_isOfflineMessage() {
        let exception = ApiException.Network(cause: KotlinThrowable(message: "no connection"))
        XCTAssertEqual(
            exception.asError().equatable.userFacingMessage,
            LocalizedStringResource(
                "error_network",
                defaultValue: "You're offline and nothing is cached yet. Check your connection and try again."
            )
        )
    }

    func testUserFacingMessage_apiExceptionHttp_isServerMessage() {
        let exception = ApiException.Http(code: 500, message: "Internal Server Error")
        XCTAssertEqual(
            exception.asError().equatable.userFacingMessage,
            LocalizedStringResource("error_server", defaultValue: "The server couldn't complete this request. Please try again.")
        )
    }

    func testUserFacingMessage_apiExceptionSerialization_isParsingMessage() {
        let exception = ApiException.Serialization(cause: KotlinThrowable(message: "bad json"))
        XCTAssertEqual(
            exception.asError().equatable.userFacingMessage,
            LocalizedStringResource(
                "error_serialization",
                defaultValue: "Something went wrong reading that response. Please try again."
            )
        )
    }
}
