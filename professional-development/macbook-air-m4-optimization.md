# MacBook Air M4: The Complete Optimization Guide

How to get the best out of your MacBook Air M4 for learning, development, and deep work—tailored for the intellectually curious.

## Table of Contents
1. [Understanding Your M4 MacBook Air](#understanding-your-m4-macbook-air)
2. [Essential Setup for Maximum Productivity](#essential-setup-for-maximum-productivity)
3. [Development Environment Optimization](#development-environment-optimization)
4. [Knowledge Management and Learning Workflow](#knowledge-management-and-learning-workflow)
5. [Terminal and Command Line Mastery](#terminal-and-command-line-mastery)
6. [Git and GitHub Workflow](#git-and-github-workflow)
7. [Reading, Research, and Note-Taking](#reading-research-and-note-taking)
8. [Performance Optimization](#performance-optimization)
9. [Privacy and Security](#privacy-and-security)
10. [Apps and Tools Recommendations](#apps-and-tools-recommendations)
11. [Keyboard Shortcuts and Efficiency](#keyboard-shortcuts-and-efficiency)
12. [Essential Takeaways](#essential-takeaways)

---

## Understanding Your M4 MacBook Air

### What Makes the M4 Special

**The M4 chip (2024)** is Apple's latest processor, built on 3nm technology:

**CPU**: 8-10 cores (4 performance, 4-6 efficiency)
**GPU**: 10 cores
**Neural Engine**: 16 cores for AI/ML tasks
**Memory**: Unified 16GB or 24GB (shared between CPU/GPU)
**No fan**: Passive cooling only

**What this means for you**:
- Silent operation (perfect for deep work)
- Exceptional battery life (15-18 hours real-world use)
- Instant wake from sleep
- Handles multiple tasks without slowing down
- Great for development, learning, research
- Runs AI/ML models locally

**Limitations to know**:
- No fan = thermal throttling under sustained heavy load (rare for Air)
- Limited ports (2 USB-C/Thunderbolt)
- Memory not upgradeable (choose wisely at purchase)
- Storage not upgradeable (same)

### Your Use Cases (Based on Our Conversation)

From the tutorials and work we've done together, you primarily use your Mac for:

1. **Software development** (Git, repositories, coding)
2. **Learning and research** (Reading about physics, history, techniques)
3. **Knowledge management** (Creating and organizing tutorials, markdown files)
4. **Writing and documentation** (Comprehensive guides like we've been creating)
5. **Intellectual work** (Deep thinking, understanding complex topics)

**This guide optimizes for these workflows.**

---

## Essential Setup for Maximum Productivity

### System Preferences Optimization

**1. Display Settings**

Go to **System Settings > Displays**:
- **Resolution**: "More Space" (gives you more screen real estate for coding/reading)
- **Night Shift**: Schedule for sunset to sunrise (reduces eye strain during late work)
- **True Tone**: Enable (adjusts colors based on ambient light)

**Why**: More screen space means seeing more code, more documentation, more context.

**2. Trackpad Settings**

**System Settings > Trackpad**:
- Enable "Tap to click" (faster than pressing down)
- Enable "Three finger drag" (System Settings > Accessibility > Pointer Control > Trackpad Options)
- Set tracking speed to 7-8 (faster cursor movement)
- Enable all gestures (especially three-finger swipe for app switching)

**Why**: Trackpad is your primary interface—optimize it.

**3. Dock Optimization**

Right-click Dock > Dock Settings:
- Position: Left or Right (vertical gives you more vertical space for code)
- Size: Small (40% or less)
- Magnification: On (makes it easier to see when small)
- Automatically hide: **On** (reclaim screen space)
- Show recent apps: Off (keeps dock clean)

**Manual apps in Dock**: Only keep 5-10 most-used apps
- Terminal/iTerm2
- VS Code (or your editor)
- Browser (Safari or Chrome)
- Notes app
- Maybe: Slack, Music, Calendar

**Remove everything else.** Use Spotlight (⌘+Space) to launch apps.

**4. Menu Bar Cleanup**

**System Settings > Control Center**:
- Remove items you don't need from menu bar
- Use Control Center for less-used items
- Keep: WiFi, Battery, Clock, Spotlight

**Why**: Clean menu bar = less visual noise = better focus.

**5. Focus Modes**

**System Settings > Focus**:

Create custom Focus modes:

**"Deep Work"**:
- Silences all notifications except emergency contacts
- Hides notification badges
- Auto-activates based on calendar events or time of day
- Dims lock screen

**"Learning"**:
- Similar to Deep Work but allows specific apps (like note-taking)
- Blocks social media/distractions

**"Development"**:
- Allows GitHub notifications, work messages
- Blocks everything else

**How to use**:
- Set schedules (e.g., Deep Work 9am-12pm daily)
- Trigger manually from Control Center
- Create automation based on app usage

**6. Hot Corners**

**System Settings > Desktop & Dock > Hot Corners**:

My recommended setup:
- **Top-left**: Mission Control (see all windows)
- **Top-right**: Desktop (hide all windows instantly)
- **Bottom-left**: None (or Lock Screen)
- **Bottom-right**: None (or Quick Note)

**Why**: Quick shortcuts without keyboard.

---

## Development Environment Optimization

### Terminal Setup

**Default Terminal vs. Alternatives**:

**Option 1: iTerm2** (my recommendation)
```bash
# Install via Homebrew (we'll set this up next)
brew install --cask iterm2
```

**Why iTerm2**:
- Split panes (multiple terminals in one window)
- Better search
- Customizable (themes, profiles)
- Hotkey window (drop-down terminal from anywhere)

**Option 2: Built-in Terminal** (perfectly fine)
- Already installed
- Fast and lightweight
- Good enough for most work

**Shell: Use Zsh (default on macOS)**

Zsh is the default shell on macOS. Enhance it:

**Install Oh My Zsh**:
```bash
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
```

**Why Oh My Zsh**:
- Better autocompletion
- Git integration (shows branch, status in prompt)
- Plugins for productivity
- Themes for visual clarity

**Recommended plugins** (edit `~/.zshrc`):
```bash
plugins=(
  git
  zsh-autosuggestions
  zsh-syntax-highlighting
  history
  colored-man-pages
)
```

**Install the plugins**:
```bash
# zsh-autosuggestions
git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions

# zsh-syntax-highlighting
git clone https://github.com/zsh-users/zsh-syntax-highlighting ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting
```

**My favorite theme**: `agnoster` or `powerlevel10k`

For Powerlevel10k (more features):
```bash
git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ${ZSH_CUSTOM:-$HOME/.oh-my-zsh/custom}/themes/powerlevel10k
```

Set in `~/.zshrc`: `ZSH_THEME="powerlevel10k/powerlevel10k"`

### Package Manager: Homebrew

**Install Homebrew** (if not already):
```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

**Why Homebrew**: Install command-line tools and apps easily.

**Essential installs**:
```bash
# Version control
brew install git

# Modern Unix tools (better replacements for classic commands)
brew install bat      # Better 'cat' with syntax highlighting
brew install eza      # Better 'ls' with colors and icons
brew install ripgrep  # Better 'grep' (extremely fast)
brew install fd       # Better 'find'
brew install fzf      # Fuzzy finder (amazing for history search)
brew install tldr     # Simplified man pages

# Development tools
brew install node     # Node.js (if you do any JS/TS work)
brew install python@3 # Python 3

# Utilities
brew install tree     # Directory tree visualization
brew install wget     # Download files
brew install htop     # Better process monitor than 'top'
brew install jq       # JSON processor (incredibly useful)
```

**Add aliases to `~/.zshrc`**:
```bash
# Modern replacements
alias cat='bat'
alias ls='eza --icons'
alias ll='eza -la --icons'
alias grep='rg'
alias find='fd'

# Git shortcuts (in addition to Oh My Zsh's)
alias gs='git status'
alias gc='git commit'
alias gp='git push'
alias gl='git log --oneline --graph --decorate'

# Quick navigation
alias ..='cd ..'
alias ...='cd ../..'
alias t='tree -L 2'

# Reload zsh config
alias reload='source ~/.zshrc'
```

### Code Editor Setup

**VS Code** (most popular, likely what you use):

**Install via Homebrew**:
```bash
brew install --cask visual-studio-code
```

**Essential extensions** for your workflow:

```
# Documentation/Markdown
- Markdown All in One
- markdownlint
- Markdown Preview Enhanced

# Git
- GitLens (supercharges Git integration)
- Git Graph (visualize branches)

# General productivity
- Prettier (code formatting)
- TODO Highlight
- Error Lens (inline error messages)
- Path Intellisense

# Theme (optional but nice)
- One Dark Pro
- Material Icon Theme
```

**VS Code settings** (⌘+, then search):

```json
{
  "editor.fontSize": 14,
  "editor.lineHeight": 22,
  "editor.fontFamily": "Menlo, Monaco, 'Courier New', monospace",
  "editor.formatOnSave": true,
  "editor.minimap.enabled": false,
  "editor.rulers": [80, 120],
  "files.autoSave": "afterDelay",
  "terminal.integrated.fontSize": 13,
  "workbench.startupEditor": "none",
  "explorer.confirmDelete": false,
  "git.autofetch": true
}
```

**Command Line Integration**:

In VS Code, press ⌘+Shift+P, type "shell command", install "code" command.

Now you can open any directory from terminal:
```bash
code .  # Opens current directory in VS Code
code file.md  # Opens specific file
```

---

## Knowledge Management and Learning Workflow

### Markdown-Based System (What You're Already Using)

You're already doing this with your Tutorials repo! Let's optimize it:

**Folder structure** (you already have this):
```
~/Tutorials/
├── ai-ml/
├── history/
├── physics/
├── professional-development/
├── personal-development/
└── README.md
```

**Best practices**:

**1. Use meaningful filenames** (you already do this)
- `feynman-learning-technique.md` ✓
- Not: `notes.md` or `stuff.md`

**2. Keep a master README** (you already do this)
- Acts as table of contents
- Makes repository navigable
- Use consistent format

**3. Use consistent markdown structure**:
```markdown
# Main Title

Brief introduction

## Table of Contents

## Section 1
### Subsection

## Section 2

## Takeaways
```

**4. Link between documents**:
```markdown
See also: [Feynman Technique](../professional-development/feynman-learning-technique.md)
```

### Quick Capture System

**For learning and ideas on the go**:

**Option 1: Apple Notes** (built-in, syncs via iCloud)
- Fast
- Available everywhere (Mac, iPhone, iPad)
- Good for quick capture
- Later: process into your Tutorials repo

**Option 2: Obsidian** (markdown-native, more powerful)
```bash
brew install --cask obsidian
```

**Why Obsidian**:
- Markdown files (portable)
- Bidirectional links
- Graph view (see connections)
- Local-first (your files)
- Plugins for everything

**Workflow**:
1. Quick notes in Apple Notes (mobile) or Obsidian (Mac)
2. Weekly: process and expand into tutorials
3. Commit to Git repo

**Daily Note Template** (for Obsidian or any markdown):
```markdown
# {{date}}

## What I Learned Today
-

## Questions to Explore
-

## Interesting Insights
-

## Action Items
-

## Resources
-
```

### PDF and Book Management

**For all those physics and history books you're reading**:

**Built-in Books app** (good enough):
- Syncs across devices
- Highlights and notes
- Search across library

**Better option: DEVONthink**:
```bash
# DEVONthink (paid but excellent)
# Download from: https://www.devontechnologies.com/apps/devonthink
```

**Why DEVONthink**:
- OCR on PDFs
- Powerful search
- Organize by tags and folders
- Export highlights
- AI-powered suggestions

**Workflow for research**:
1. Save PDFs to DEVONthink
2. Highlight and annotate while reading
3. Export highlights to markdown
4. Process into tutorials or notes

---

## Terminal and Command Line Mastery

### Essential Skills

**1. Fuzzy History Search with fzf**:

After installing fzf (`brew install fzf`), add to `~/.zshrc`:
```bash
# FZF key bindings
[ -f ~/.fzf.zsh ] && source ~/.fzf.zsh

# Use fd instead of find
export FZF_DEFAULT_COMMAND='fd --type f --hidden --follow --exclude .git'
export FZF_CTRL_T_COMMAND="$FZF_DEFAULT_COMMAND"
```

**Usage**:
- `Ctrl+R`: Search command history with fuzzy matching (game-changer!)
- `Ctrl+T`: Fuzzy file search in current directory
- `Alt+C`: Fuzzy directory search and cd

**2. Better Directory Navigation**:

Add to `~/.zshrc`:
```bash
# Jump to frequent directories
eval "$(zoxide init zsh)"  # Install first: brew install zoxide

# Usage: z <partial-name> jumps to frequent directory
# Example: z tut → jumps to ~/Tutorials
```

**3. Quick File Preview**:

```bash
# Install bat for syntax highlighting
brew install bat

# Preview files
bat filename.md

# Preview with line numbers
bat -n filename.md
```

**4. Efficient Git Commands**:

Beyond aliases, learn these patterns:

```bash
# See what changed
git diff
git diff --staged

# Interactive staging (choose what to commit)
git add -p

# Amend last commit (before pushing)
git commit --amend

# See file history
git log --follow filename.md

# Create and switch to new branch
git checkout -b feature/new-tutorial

# Clean up merged branches
git branch --merged | grep -v "\*" | xargs -n 1 git branch -d
```

---

## Git and GitHub Workflow

### Optimal Setup (Based on Your Usage)

**1. SSH Keys for GitHub** (no password prompts):

```bash
# Generate SSH key
ssh-keygen -t ed25519 -C "your_email@example.com"

# Start ssh-agent
eval "$(ssh-agent -s)"

# Add to ssh-agent
ssh-add ~/.ssh/id_ed25519

# Copy public key
cat ~/.ssh/id_ed25519.pub | pbcopy

# Add to GitHub: Settings > SSH and GPG keys > New SSH key
```

**2. Git Configuration**:

```bash
# User info
git config --global user.name "Your Name"
git config --global user.email "your_email@example.com"

# Default editor
git config --global core.editor "code --wait"  # VS Code
# OR
git config --global core.editor "vim"

# Default branch name
git config --global init.defaultBranch main

# Better diffs
git config --global diff.algorithm histogram

# Reuse recorded conflict resolutions
git config --global rerere.enabled true

# Show more context in diffs
git config --global diff.context 5

# Aliases
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.unstage 'reset HEAD --'
git config --global alias.last 'log -1 HEAD'
git config --global alias.visual 'log --oneline --graph --decorate --all'
```

**3. Gitignore Global**:

```bash
# Create global gitignore
touch ~/.gitignore_global

# Configure git to use it
git config --global core.excludesfile ~/.gitignore_global
```

Add to `~/.gitignore_global`:
```
# macOS
.DS_Store
.AppleDouble
.LSOverride

# Thumbnails
._*

# IDEs
.vscode/
.idea/
*.swp
*.swo

# Misc
.env
node_modules/
```

**4. GitHub CLI** (optional but powerful):

```bash
brew install gh

# Authenticate
gh auth login

# Create repo from command line
gh repo create

# Create PR
gh pr create

# View issues
gh issue list

# Clone repos quickly
gh repo clone username/repo
```

### Your Tutorial Workflow (Optimized)

Based on the work we've done:

```bash
# Daily workflow
cd ~/Tutorials

# Check status
git status

# Create new tutorial
mkdir -p new-category
touch new-category/tutorial-name.md
code new-category/tutorial-name.md

# Write tutorial...

# Stage and commit
git add .
git commit -m "Add tutorial on [topic]"

# Push to GitHub
git push origin main
```

**Batch commits**:
```bash
# After creating multiple tutorials in one session
git add history/new-bio.md README.md
git commit -m "Add biography of X"

git add physics/new-concept.md README.md
git commit -m "Add tutorial on Y"

git push origin main
```

---

## Reading, Research, and Note-Taking

### Safari Optimization (Built-in Browser)

**Why Safari on M4 MacBook Air**:
- Most battery-efficient
- Fastest on Apple Silicon
- Great reader mode
- iCloud sync across devices

**Essential settings**:

**Safari > Settings > General**:
- Homepage: Favorites or empty (not distracting news)
- New windows/tabs: Empty page or homepage

**Safari > Settings > Tabs**:
- Compact tab bar (saves space)
- Show website icons

**Safari > Settings > Reader**:
- Enable automatically on articles
- Set preferred font and size

**Safari > Settings > Advanced**:
- Enable "Show Develop menu in menu bar"

**Extensions to install**:
- **1Password** (password manager)
- **Vinegar** (removes YouTube ads, cleaner interface)
- **Hush** (blocks cookie banners and popups)

**Reader Mode** (⌘+Shift+R):
- Strips away clutter
- Just article text
- Adjustable fonts
- Perfect for long-form reading

**Reading List** (⌘+Shift+D):
- Save articles to read later
- Syncs across devices
- Offline access

### Research Workflow

**For your deep dives into physics, history, etc.**:

**1. Collect**:
- Save interesting articles to Safari Reading List
- Bookmark academic papers
- Save PDFs to dedicated folder

**2. Process**:
- Read in Reader Mode or dedicated PDF app
- Highlight key points
- Take notes in markdown

**3. Synthesize**:
- Create tutorial or summary
- Use Feynman Technique (teach it simply)
- Add to your Tutorials repo

**4. Connect**:
- Link related tutorials
- Build knowledge graph in your mind
- Review periodically

### Screenshot and Annotation

**Built-in screenshots** (⌘+Shift+3/4/5):
- ⌘+Shift+3: Full screen
- ⌘+Shift+4: Selection
- ⌘+Shift+5: Options (screen recording, timed, etc.)

**Screenshots go to**: Desktop by default

**Change location**:
```bash
# Save to Pictures/Screenshots instead
mkdir -p ~/Pictures/Screenshots
defaults write com.apple.screencapture location ~/Pictures/Screenshots
killall SystemUIServer
```

**Markup**: Click thumbnail that appears after screenshot to annotate.

---

## Performance Optimization

### Memory Management

**M4 Air with 16GB or 24GB**:

**16GB is enough if**:
- Moderate browser tabs (< 20 open)
- One or two heavy apps at a time
- Not running VMs or Docker extensively

**24GB recommended if**:
- Heavy multitasking
- Running local AI models
- Video editing
- Multiple development environments

**Monitor memory**:
```bash
# Activity Monitor
# Applications > Utilities > Activity Monitor
# OR
# Spotlight: ⌘+Space, type "Activity Monitor"
```

**Watch "Memory Pressure"**:
- Green: All good
- Yellow: System using swap (slower but okay)
- Red: Add more RAM or close apps (rare on M4)

**Optimize**:
- Quit unused apps (⌘+Q, not just close window)
- Limit browser tabs
- Use Safari over Chrome (uses less memory)
- Restart Mac weekly

### Storage Management

**Check storage**: Menu Bar > Apple > About This Mac > Storage

**Optimize**:

**1. System cleanup**:
```bash
# Clear download folder periodically
rm -rf ~/Downloads/*

# Clear caches (safe)
rm -rf ~/Library/Caches/*

# Clear Homebrew cache
brew cleanup
```

**2. Find large files**:
```bash
# Install ncdu (NCurses Disk Usage)
brew install ncdu

# Scan home directory
ncdu ~

# Navigate with arrows, press 'd' to delete
```

**3. Offload to iCloud**:
- System Settings > Apple ID > iCloud
- Enable "Optimize Mac Storage"
- Automatically removes old files, keeps recent

**4. Use external storage for large files**:
- Videos, raw photos, archives
- Keep on external SSD
- M4 Air has fast Thunderbolt ports

### Battery Optimization

**M4 Air already has excellent battery**, but optimize further:

**System Settings > Battery**:
- Low Power Mode: Enable when below 50%
- Optimize battery charging: On (learns your patterns)

**Display**:
- Reduce brightness when not needed
- Use Night Shift (less power than max brightness)

**Apps**:
- Quit unused apps
- Avoid Chrome (battery hog), use Safari
- Close heavy web apps when not in use

**Check battery drain**:
- Activity Monitor > Energy tab
- Sort by "Energy Impact"
- Quit culprits

**Expected battery life**:
- Light work (writing, reading): 15-18 hours
- Development (VS Code, terminal): 12-15 hours
- Heavy work (compiling, video): 8-10 hours

**Calibrate occasionally**:
- Once every 2-3 months: drain to 0%, charge to 100%
- Maintains battery health

### Thermal Management

**No fan = passive cooling**:

**Normal behavior**:
- MacBook Air gets warm during heavy tasks
- Thermal throttling kicks in if too hot (CPU slows down)
- This is fine and designed behavior

**When it happens**:
- Compiling large projects
- Exporting videos
- Running AI models
- Sustained high CPU usage

**How to manage**:
- Work on hard surfaces (not bed or couch)
- Give bottom air space
- If too hot, take a break (let it cool)
- Consider external fan or cooling pad for sustained heavy work (rare)

**Monitoring**:
```bash
# Install iStats Menus (paid) or use built-in Activity Monitor
# See CPU temperature, fan speed (none for Air), etc.
```

---

## Privacy and Security

### Essential Security Settings

**1. FileVault** (full-disk encryption):

System Settings > Privacy & Security > FileVault
- Turn On FileVault
- Use iCloud account to unlock

**Why**: Protects your data if Mac is lost or stolen.

**2. Firewall**:

System Settings > Network > Firewall
- Turn On Firewall
- Block all incoming connections except essential

**3. Privacy Settings**:

System Settings > Privacy & Security
- Review each category (Location, Camera, Microphone, etc.)
- Grant permissions only to apps you trust
- Revoke unnecessary permissions

**4. Find My**:

System Settings > Apple ID > Find My
- Enable Find My Mac
- Enable offline finding
- Allows you to locate, lock, or erase remotely

### Password Management

**1Password** (my recommendation):
```bash
brew install --cask 1password
```

**Why 1Password**:
- Generate strong passwords
- Autofill in browser and apps
- Secure notes for sensitive info
- 2FA codes (replace Google Authenticator)
- Watchtower (alerts about breached passwords)

**Alternative: Built-in Keychain**:
- Already integrated
- Works across Apple devices
- No cost
- Good enough for most people

**Best practice**:
- Unique password for every site
- Use password manager to generate (20+ characters)
- Enable 2FA wherever possible
- Never reuse passwords

### Backups

**Time Machine** (built-in, essential):

**Setup**:
1. Get external drive (at least 256GB, ideally 512GB+)
2. Connect to Mac
3. System Settings > General > Time Machine
4. Add Backup Disk
5. Enable automatic backups

**Why**:
- Protects against hardware failure
- Protects against accidental deletion
- Can restore entire system
- Hourly backups for 24 hours, daily for a month, weekly thereafter

**Alternative/Additional: Cloud backup**:
- iCloud (built-in, limited free tier)
- Backblaze ($7/month, unlimited)
- Provides offsite protection

**3-2-1 Rule**:
- 3 copies of data
- 2 different media (internal SSD + external drive)
- 1 offsite (cloud)

### Secure Browsing

**Safari Private Browsing** (⌘+Shift+N):
- Doesn't save history
- Doesn't save cookies
- Blocks trackers

**Privacy settings**:

Safari > Settings > Privacy:
- Prevent cross-site tracking: On
- Hide IP from trackers: On
- Block all cookies: Off (breaks too many sites), use "Allow from websites I visit"

**Extensions for privacy**:
- **Hush** (blocks cookie banners)
- **1Password** (secure passwords)

**DuckDuckGo** as search engine (more private than Google):
- Safari > Settings > Search > Search Engine > DuckDuckGo

---

## Apps and Tools Recommendations

### Essential Apps

**Productivity**:
- **Alfred** (⌘+Space replacement, powerful workflows) - `brew install --cask alfred`
- **Rectangle** (window management, free) - `brew install --cask rectangle`
- **Bartender** (menu bar organization) - `brew install --cask bartender`
- **Raycast** (alternative to Alfred, modern) - `brew install --cask raycast`

**Development**:
- **VS Code** (code editor) - `brew install --cask visual-studio-code`
- **iTerm2** (terminal) - `brew install --cask iterm2`
- **Postman** (API testing, if needed) - `brew install --cask postman`
- **Docker Desktop** (containers, if needed) - `brew install --cask docker`

**Knowledge/Notes**:
- **Obsidian** (markdown notes) - `brew install --cask obsidian`
- **Notion** (all-in-one workspace) - `brew install --cask notion`
- **DEVONthink** (document management, paid)

**Reading**:
- **Calibre** (ebook management) - `brew install --cask calibre`
- **PDF Expert** (PDF annotation, paid but excellent)

**Utilities**:
- **Keka** (file compression/extraction) - `brew install --cask keka`
- **AppCleaner** (uninstall apps completely) - `brew install --cask appcleaner`
- **iStat Menus** (system monitoring, paid) - `brew install --cask istat-menus`
- **CleanShot X** (better screenshots, paid) - `brew install --cask cleanshot`

### Window Management

**Rectangle** (free, my recommendation):

Install: `brew install --cask rectangle`

**Essential shortcuts** (customizable):
- Ctrl+Opt+Left: Left half
- Ctrl+Opt+Right: Right half
- Ctrl+Opt+Enter: Maximize
- Ctrl+Opt+C: Center
- Ctrl+Opt+Up: Top half
- Ctrl+Opt+Down: Bottom half

**Why it matters**:
- No dragging windows to resize
- Keyboard-driven workflow
- Snap to edges
- Perfect for external monitor or large display

**Alternative: Magnet** (paid, $2.99, slightly more features)

### Automation with Keyboard Maestro

**Keyboard Maestro** (paid, $36, powerful):

**What it does**:
- Create keyboard shortcuts for anything
- Automate repetitive tasks
- Text expansion
- App-specific shortcuts

**Examples for your workflow**:
- Hot key to open Tutorials folder in Finder and VS Code
- Auto-paste template for new tutorials
- Quick commit and push to Git
- Open specific websites/apps together

**Alternative: Shortcuts** (built-in, free but less powerful):
- System Settings > Shortcuts
- Create automations
- Good for simple tasks

---

## Keyboard Shortcuts and Efficiency

### System-Wide Shortcuts (Know These Cold)

**Essential**:
- ⌘+Space: Spotlight (launch apps, search files)
- ⌘+Tab: Switch apps
- ⌘+`: Switch windows within app
- ⌘+Q: Quit app
- ⌘+W: Close window
- ⌘+H: Hide app
- ⌘+M: Minimize window
- ⌘+Opt+Esc: Force quit

**File management**:
- ⌘+N: New file/window
- ⌘+O: Open
- ⌘+S: Save
- ⌘+Shift+S: Save As
- ⌘+P: Print
- ⌘+A: Select all
- ⌘+C: Copy
- ⌘+V: Paste
- ⌘+X: Cut
- ⌘+Z: Undo
- ⌘+Shift+Z: Redo
- ⌘+Delete: Move to trash

**Text editing**:
- ⌘+B: Bold
- ⌘+I: Italic
- ⌘+U: Underline
- ⌘+Left/Right: Jump to beginning/end of line
- ⌘+Up/Down: Jump to beginning/end of document
- Opt+Left/Right: Jump by word
- Opt+Delete: Delete word

**Screenshots**:
- ⌘+Shift+3: Full screen
- ⌘+Shift+4: Selection
- ⌘+Shift+5: Options menu

**Mission Control**:
- Ctrl+Up: Mission Control (all windows)
- Ctrl+Down: App Exposé (current app windows)
- F3: Show desktop
- Ctrl+Left/Right: Switch desktops/fullscreen apps

### Terminal Shortcuts

**Navigation**:
- Ctrl+A: Beginning of line
- Ctrl+E: End of line
- Ctrl+K: Delete from cursor to end
- Ctrl+U: Delete from cursor to beginning
- Ctrl+W: Delete word before cursor
- Ctrl+Y: Paste last deleted text
- Ctrl+L: Clear screen (same as `clear`)
- Ctrl+R: Search command history (with fzf even better)

**Process control**:
- Ctrl+C: Cancel current command
- Ctrl+D: Exit shell/EOF
- Ctrl+Z: Suspend process

### VS Code Shortcuts

**Essential**:
- ⌘+P: Quick file open
- ⌘+Shift+P: Command palette
- ⌘+B: Toggle sidebar
- ⌘+J: Toggle terminal
- ⌘+K, Z: Zen mode
- ⌘+/: Toggle comment
- ⌘+D: Select next occurrence
- ⌘+Shift+L: Select all occurrences
- Opt+Up/Down: Move line up/down
- Opt+Shift+Up/Down: Copy line up/down
- ⌘+Shift+K: Delete line
- Ctrl+`: Toggle terminal

**Multi-cursor** (powerful):
- Opt+Click: Add cursor
- ⌘+Opt+Up/Down: Add cursor above/below
- ⌘+Shift+L: Cursor on all occurrences

---

## Essential Takeaways

### The Optimal Setup (TL;DR)

**System**:
1. Display: "More Space" resolution
2. Dock: Auto-hide, vertical, minimal apps
3. Focus modes for Deep Work
4. Trackpad: Three-finger drag enabled
5. Hot corners for quick actions

**Development**:
1. iTerm2 + Oh My Zsh + Powerlevel10k
2. VS Code with essential extensions
3. Homebrew for package management
4. Modern CLI tools (bat, eza, ripgrep, fzf)
5. SSH keys for GitHub

**Knowledge Management**:
1. Markdown files in Git repos (you're already doing this!)
2. Obsidian for daily notes and quick capture
3. Safari Reader Mode for distraction-free reading
4. DEVONthink or Calibre for PDFs/books
5. Regular export/synthesis into tutorials

**Productivity**:
1. Rectangle for window management
2. Alfred or Raycast for app launching
3. 1Password for security
4. Time Machine for backups
5. Keyboard shortcuts for everything

### Your Personalized Workflow

Based on your usage:

**Morning routine**:
1. Open iTerm2 (hotkey: Opt+Space if configured)
2. `cd ~/Tutorials`
3. `git pull` (get latest)
4. Open VS Code: `code .`
5. Enable "Deep Work" focus mode
6. Start learning/writing

**Learning something new**:
1. Research topic (Safari + Reader Mode)
2. Take quick notes (Apple Notes or Obsidian)
3. Apply Feynman Technique (teach it simply)
4. Create tutorial in Tutorials repo
5. Commit and push to GitHub

**Development work**:
1. Open project in VS Code
2. Terminal in bottom panel (⌘+J)
3. Git integration in sidebar
4. Make changes
5. Quick commit: `gc -m "message"` + `gp`

**Evening wind-down**:
1. Process day's notes into tutorials
2. Review and commit
3. Reading (Safari Reading List or books)
4. Quick reflection in daily note

### Key Principles

**Optimize for flow**:
- Minimize context switching
- Use keyboard over mouse
- Automate repetitive tasks
- Keep workspace clean

**Leverage the M4**:
- No fan = silent deep work
- Amazing battery = work anywhere
- Fast wake = instant resumption
- Unified memory = smooth multitasking

**Protect your data**:
- Time Machine backups
- Git for important work
- FileVault encryption
- Strong passwords (1Password)

**Keep learning**:
- Your Tutorials system is brilliant—keep building it
- Use Feynman Technique on everything
- Document as you learn
- Share knowledge

### Next Steps

**This week**:
1. Set up Oh My Zsh and Powerlevel10k
2. Install and configure Rectangle
3. Set up Time Machine
4. Create "Deep Work" focus mode
5. Configure VS Code extensions

**This month**:
1. Master keyboard shortcuts (one app per week)
2. Explore Obsidian for daily notes
3. Optimize your Tutorials workflow
4. Set up automation (Keyboard Maestro or Shortcuts)
5. Review and refine setup

**Ongoing**:
- Keep your Tutorials repo growing
- Experiment with new tools
- Optimize workflows
- Share what you learn
- Enjoy the process

---

## Final Thoughts

### You Have a Great Machine

The M4 MacBook Air is:
- Powerful enough for professional development
- Efficient enough for all-day battery
- Silent enough for deep concentration
- Portable enough to work anywhere

**Perfect for someone who values learning and deep work.**

### You Have a Great System

Your Tutorials repository shows:
- Intellectual curiosity
- Commitment to understanding deeply
- Systematic knowledge building
- Clear thinking and communication

**This guide optimizes your setup to support that work.**

### The Real Optimization

**Technology is just tools.**

The real optimization is:
- Protecting focus time
- Building systems that support learning
- Reducing friction in your workflow
- Staying curious

**Your M4 MacBook Air can be as simple or as powerful as you need.**

Configure it to disappear—to be a transparent window to your work, your learning, your thinking.

**That's when it's optimized.**

---

**"The best tool is the one you don't notice you're using."**

Now go build, learn, and create something remarkable.
