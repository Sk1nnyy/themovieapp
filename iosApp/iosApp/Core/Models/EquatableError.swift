import Foundation

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
