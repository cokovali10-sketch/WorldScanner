# GitHub publishing guide

## 1. Install Git
Download and install Git from: https://git-scm.com/

## 2. Initialize the repository
Run in the project folder:

```bash
git init
git add .
git commit -m "Initial commit"
```

## 3. Create a repository on GitHub
1. Open https://github.com/
2. Click New repository
3. Name it, for example: WorldScanner
4. Choose Public or Private
5. Click Create repository

## 4. Connect local project to GitHub
Run:

```bash
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/WorldScanner.git
git push -u origin main
```

## 5. Update the repository later
```bash
git add .
git commit -m "Update project"
git push
```

## 6. Download the project from GitHub
### Download ZIP
Open the repository page and click Code -> Download ZIP.

### Clone with Git
```bash
git clone https://github.com/YOUR_USERNAME/WorldScanner.git
```

## 7. Run the project after download
Windows:

```bash
./gradlew.bat run
```

## 8. Useful notes
- Keep the README and LICENSE files in the root of the project.
- If you want to publish a release, use GitHub Releases.
- For a cleaner history, use meaningful commit messages.
