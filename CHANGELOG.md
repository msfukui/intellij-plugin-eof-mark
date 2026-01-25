# Changelog

All notable changes to the EOF Mark plugin will be documented in this file.

## [0.1.2] - 2026-01-24

### Added
- Initial public release
- EOF marker display at end of files (`[EOF]`)
- Cursor position restriction to prevent moving past the EOF marker
- Support for IntelliJ IDEA 2024.2 and later

### Changed
- Automated release workflow with semantic versioning
- GitHub Release notes auto-generation

## [0.1.1] - 2026-01-23

### Fixed
- CaretListener leak prevention
- expectedColumn calculation fix
- Support for existing editors when plugin loads

## [0.1.0] - 2026-01-22

### Added
- Basic EOF marker implementation using IntelliJ Platform Inlay API
- Unit tests and integration tests
- CI/CD configuration with GitHub Actions
