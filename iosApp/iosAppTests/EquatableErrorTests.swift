import XCTest
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
}
