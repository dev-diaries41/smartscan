## v1.0.5 – 13/04/2025

### Added
- Progress bar for indexing
- Indicator shown when background auto-organisation is running
- Expandable main search result
- Grid column layout for search results
- Enter key to search

### Changed
- Dynamic concurrency for memory management
- Batching implemented for organisation
- Skip images that have already been processed when organizing
- More robust and user-friendly error handling for background jobs

### Fixed
- Text visibility in light mode on search screen
- Fixed scan history not updating

## v1.0.4 – 03/04/2025

- Chained image index workers

## v1.0.3 – 03/04/2025

- Delete `image_embeddings` db when refreshing image index
- Remove battery constraint on image indexer worker
- Chained image index workers

## v1.0.2 – 03/04/2025

- Fix search bug that occurred due to changes in storage permissions
- Added new feature that allows refreshing image index to handle changes in storage permissions
- Fix bug that caused some files to be skipped in classification worker
- Memory optimizations

## v1.0.1 – 27/03/2025

- Updated `build.gradle` for compatibility with F-Droid reproducible builds  
- Updated app version display in the Settings screen  
- Made the Setting Details screen scrollable
