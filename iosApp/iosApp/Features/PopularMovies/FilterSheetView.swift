import SwiftUI

struct FilterSheetView: View {
    let selectedFilter: MovieFilter
    let onSelect: (MovieFilter) -> Void

    var body: some View {
        NavigationStack {
            List(MovieFilter.allCases, id: \.self) { filter in
                Button {
                    onSelect(filter)
                } label: {
                    HStack {
                        Text(filter.label)
                            .foregroundStyle(.primary)
                        Spacer()
                        if filter == selectedFilter {
                            Image(systemName: "checkmark")
                                .foregroundStyle(.tint)
                                .fontWeight(.semibold)
                        }
                    }
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
