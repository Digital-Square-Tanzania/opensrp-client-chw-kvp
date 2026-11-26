 Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased

### Fixed

- Serialize manual and scheduled MARP processing by visit and submission, preserve staging on a failed event write, assign stable Biomedical follow-up IDs and expose exact local service projection evidence. Set the Kituoni library version to `2.0.5-KITUONI-UCSW-200-VISIT-23-SNAPSHOT` and version code to 20 (UCSW-200).

## 2.0.4-KITUONI-UCSW-186-VISIT-14-SNAPSHOT — 2026-11-25

### Fixed

- Let Kituoni apply its Health Education completion rule when displaying and processing Behavioral visits. Existing initializers keep the IEC/SBCC and Health Education rule. Set the library version code to 19 (UCSW-186).

## 2.0.3-KITUONI-UCSW-47-VISIT-10-SNAPSHOT — 2026-11-25

### Fixed

- Let Kituoni exempt pregnant Biomedical visits from the Cervical Cancer Screening completion check (UCSW-47). Existing consumers keep the default screening requirement. Set the library version code to 18.

## 2.0.2-KITUONI-UCSW-48-VISIT-9-SNAPSHOT — 2026-11-24

### Fixed

- Let Kituoni exempt pregnant Biomedical visits from the Family Planning completion check while preserving the standard processing rules (UCSW-48). Set the library version code to 17.

