## v1.1.3 – 20/08/2025

### Changed
- Replace Room DB with file-based index storage for faster loading
- Minor UI changes

## v1.1.2 – 08/08/2025

### Changed
- Clean, intuitive search UI with click-to-open media viewer and sharing features

## v1.1.1 – 11/06/2025

### Changed

* Improved memory efficiency in video processing

### Fixed

* UI freeze when selecting destination folders with many images
* Out-of-memory (OOM) crashes when processing/loading large images
* Double ONNX environment teardown bug

## v1.1.0 – 19/05/2025

### Added
- Video search support
- Undo last scan feature
- Help screen for guidance and troubleshooting

### Changed
- Refresh logic revised to avoid hard reset

### Fixed
- Reduced false positives in auto-organisation

## v1.0.6 – 30/04/2025

### Added
- Option to configure index frequency (daily or weekly) in Settings  
- Option to configure similarity threshold for search in Settings  

### Changed
- "Enable scanning" renamed to "Enable auto-organisation" for clarity
- Minor UI updates

### Removed
- Unnecessary network info permission  

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
