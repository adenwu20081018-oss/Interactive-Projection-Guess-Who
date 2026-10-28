# Phase 8 portfolio preparation record

Prepared on 2026-10-28. This phase changes documentation and repository metadata only; no working Java game, Python tracking or Arduino firmware source was edited.

## Deliverables

- Root `README.md`: portfolio introduction, implemented features, architecture, actual game captures, setup, demonstration, engineering decisions and source layout.
- `.gitignore` and `.gitattributes`: proposed tracking exclusions and text/binary handling.
- `docs/README.md`: documentation index linking existing records without relocating their targets.
- `docs/publishing.md`: exact GitHub Desktop/Git steps, with publication explicitly left to the user.
- `docs/testing.md`: curated evidence from recorded test runs.
- `docs/credits.md`: original game contributors and dependency provenance; no license assigned automatically.
- `docs/images/screenshots/game-title.png`, `game-board.png`, `game-selection.png` and their README: actual Swing content captures.
- `computer-vision/README.md`: corrected current integration overview and explicit model download for a clean checkout.
- This preparation record.

## Checks actually performed

- Compiled a temporary documentation capture helper against the existing game build; captured real application title/board/selection views through original button actions. No physical serial port or camera was opened.
- Opened and visually inspected all three captures, each 1000×625; PNG decode checks passed.
- Checked 54 local links in the new portfolio documents: every target existed and was present in the proposed Git file set. Existing links added to the vision README point to retained technical guides.
- Used an isolated temporary Git metadata directory under ignored `tmp/phase8/` to apply actual Git ignore rules. This did not initialize `.git` in the project root, stage files, commit, configure a remote or publish anything.
- Confirmed environments, caches, compiled classes, downloaded model/JAR, local calibration and temporary capture work are excluded. Important Java/Python/Arduino source, tests, diagrams and runtime artwork/audio are retained.
- Audited content totals approximately 25 MB. The largest retained file is original runtime audio `Suspicion_Matrix.wav` (18,018,126 bytes). Generated dependencies were absent from the proposed set.
- The original game asset locations were preserved. No unnecessary local build files were deleted; exclusion changes only the proposed tracking set.

The temporary proposed-file inventory is `tmp/phase8/proposed-files.txt`; it is a local audit artifact, not a portfolio deliverable. It was generated before this record was added. Raw root-level test logs remain local, with selected results summarized in `docs/testing.md`.

## Boundaries

The GitHub-hosted README rendering and clean-clone installation have not been tested because no remote repository was created. Local files, links and screenshot images were checked; gameplay/regression suites were not rerun for these documentation-only changes. Existing phase test results are identified as historical evidence rather than new Phase 8 results.

The publisher still chooses the GitHub account, repository name/visibility and any license after reviewing contributor/asset permissions. Review the final staged file list before uploading. No video or hardware photograph was fabricated; all physical verification claims retain their original evidence boundaries in the linked technical documents.

Stop after Phase 8 preparation. Nothing was published or pushed.
