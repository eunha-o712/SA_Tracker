# Design QA

- Source visual truth: `D:\workspece\SATrk\tmp\profile-guest-reference.png`
- Implementation screenshot: `D:\workspece\SATrk\tmp\profile-guest-implementation.png`
- Combined comparison: `D:\workspece\SATrk\tmp\profile-guest-comparison.png`
- Viewport: 1265 x 712 desktop
- State: logged out, `/player`, compact search idle state

## Full-view comparison evidence

The source and implementation were opened together in the combined comparison image. The implementation intentionally moves the search control out of the profile card and places it directly below the record-room banner, matching the logged-in profile position. The profile card remains present with an empty body.

## Focused region comparison evidence

The search and profile-card region is readable at the captured viewport, so a separate crop was unnecessary. The compact control retains the source typography, neon-green borders, dark surfaces, button treatment, and responsive width cap.

## Findings

- No actionable P0, P1, or P2 visual differences remain for the requested layout change.
- The source screenshot represents the previous in-card search placement; the changed outside-card placement is intentional and follows the user's explicit instruction.

## Required fidelity surfaces

- Fonts and typography: existing SATrk type scale and weights retained.
- Spacing and layout rhythm: compact search uses the same component and placement as the logged-in profile screen; empty card spacing remains stable.
- Colors and visual tokens: existing background, accent, border, and glow tokens retained.
- Image quality and asset fidelity: existing record-room banner asset remains sharp and unchanged.
- Copy and content: `PLAYER PROFILE`, `레코드 룸`, `PLAYER SEARCH`, and search control copy remain correct.

## Interaction and automated checks

- Search input fill and SEARCH button navigation verified; route changed to `/player/{nickname}`.
- Browser console errors: none in the logged-out idle state.
- ESLint: passed.
- Vite production build: passed.

## Comparison history

- Initial pass: compact search rendered below the banner and the profile card body rendered empty. No P0/P1/P2 fixes were required.

final result: passed
