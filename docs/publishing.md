# Publish the portfolio with GitHub Desktop or Git

Phase 8 prepares files only. **No repository has been published, no remote configured and nothing pushed by the assistant.** The project folder did not contain a Git repository during preparation. Choose one path below when you decide to publish; do not execute both initialization paths blindly.

Suggested repository name: `interactive-projection-guess-who`.
Suggested description: `Gesture-controlled Java Guess Who with Python computer vision, homography calibration, TCP input and Arduino Mega LED feedback.`
Suggested topics: `computer-vision`, `opencv`, `mediapipe`, `java-swing`, `arduino`, `human-computer-interaction`, `homography`, `gesture-recognition`.

## What will be tracked

The root `.gitignore` excludes Python environments/caches, compiled Java output, downloaded JAR/model files, local calibration JSON, temporary work, IDE settings and raw root-level phase logs. It retains source code, tests, original runtime PNG/WAV assets, engineering drawings, CAD, screenshots and curated validation records. No generated files were deleted from the working setup.

Original root-level PNG/WAV assets are deliberately retained: moving them would break the existing relative runtime paths. Downloaded dependencies are restored by the documented setup commands. `docs/testing.md` summarizes test evidence; historical technical records remain indexed in `docs/README.md`.

Before publishing, review the actual file list, contributors/asset permissions in [credits](credits.md), and repository visibility. No license was automatically applied. Files already committed in an existing repository would not be removed from history merely by adding `.gitignore`; the commands below assume this currently uninitialized folder.

## Option A — GitHub Desktop

1. Install Git for Windows and GitHub Desktop, then sign in to Desktop with your own GitHub account.
2. Open PowerShell in the project root. Initialize local tracking only:

   ```powershell
   git init -b main
   git status --short
   ```

3. In GitHub Desktop choose **File → Add local repository**, browse to this exact project root, then **Add repository**. Do not choose a parent download folder or create a nested copy of the project.
4. Review the Changes list. Source, README, screenshots, diagrams and game assets should appear. `.venv`, `tmp`, build folders, `.class`, downloaded model/JAR and calibration files should not appear. Check Git's decisions with `git check-ignore -v` if unsure.
5. Enter a summary such as `Prepare gesture-controlled Guess Who portfolio`, then click **Commit to main**. A local commit does not publish anything.
6. **Publication step, only when you are ready:** click **Publish repository**, set the name/description, choose your account and decide visibility. For a public portfolio, deselect **Keep this code private**. Click **Publish Repository** to upload.
7. Use **Repository → View on GitHub**. Confirm the README images load, diagrams open, source/tests are present and machine-specific folders are absent. Add the suggested topics using the repository's About settings.

GitHub's [Desktop publishing instructions](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop?platform=windows) explain the publish dialog. No remote-removal step is needed for this new local repository.

## Option B — Git from PowerShell

From the project root, prepare and review a local commit:

```powershell
git init -b main
git status --short
git add --dry-run .
git add .
git diff --cached --stat
git diff --cached --name-only
git status --short
git commit -m 'Prepare gesture-controlled Guess Who portfolio'
```

If Git asks for identity, configure your own name and GitHub email for this repository, then repeat the commit:

```powershell
git config user.name 'YOUR NAME'
git config user.email 'YOUR VERIFIED OR GITHUB NOREPLY EMAIL'
```

Review the staged list before committing; do not paste credentials into files. The setup/model scripts make it unnecessary to commit local environments or downloaded packages.

**Publication step, only when you are ready:** create an empty repository on GitHub with the chosen name and visibility. Leave its README, `.gitignore` and license initialization unchecked because these files already exist locally. Replace `YOUR-USERNAME` below with the actual owner:

```powershell
git remote add origin https://github.com/YOUR-USERNAME/interactive-projection-guess-who.git
git remote -v
git push -u origin main
```

Use Git for Windows' normal browser/credential-manager sign-in when prompted. Do not put a token into the remote URL. If `origin` already exists, inspect it instead of overwriting it blindly. See GitHub's [local project import guide](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github).

## Final repository review

- Open the root README on GitHub and inspect all three real game screenshots and the architecture/wiring diagrams.
- Verify a fresh checkout can follow the setup instructions, including the explicit model download and JAR acquisition.
- Review the evidence links; screenshots are application captures, not physical projection footage.
- Keep physical buzzer, projector and CAD verification details in their linked technical records.
- Record a real demo video later if desired; no video link or photograph is invented in this portfolio.

Preparation stops here. Publishing remains your explicit next action.
