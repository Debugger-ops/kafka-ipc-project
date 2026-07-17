#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# One-shot dependency installer for the Kafka IPC project (macOS).
# Installs: Homebrew (if missing), Java 17, Maven, and Docker Desktop.
# Usage:  bash install-deps.sh
# You may be prompted for your Mac password a few times — that's expected.
# ---------------------------------------------------------------------------
set -e

echo "==> 1/5  Checking for Homebrew..."
if ! command -v brew >/dev/null 2>&1; then
  echo "    Homebrew not found — installing it now."
  /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
  # Load brew into THIS shell (Apple Silicon path, then Intel path)
  if [ -x /opt/homebrew/bin/brew ]; then eval "$(/opt/homebrew/bin/brew shellenv)"; fi
  if [ -x /usr/local/bin/brew ];  then eval "$(/usr/local/bin/brew shellenv)";  fi
else
  echo "    Homebrew already installed."
fi

echo "==> 2/5  Installing Java 17 and Maven..."
brew install openjdk@17 maven

echo "==> 3/5  Linking Java 17 so macOS can find it (may ask for your password)..."
sudo ln -sfn "$(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk" \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk

echo "==> 4/5  Installing Docker Desktop (for Kafka + ZooKeeper)..."
if [ -d "/Applications/Docker.app" ]; then
  echo "    Docker Desktop already installed."
else
  brew install --cask docker
fi

echo "==> 5/5  Verifying..."
echo "----------------------------------------"
java -version || echo "Java check failed — open a new terminal and run 'java -version'"
echo "----------------------------------------"
mvn -v || echo "Maven check failed — open a new terminal and run 'mvn -v'"
echo "----------------------------------------"

cat <<'DONE'

All dependencies installed.

NEXT STEPS:
  1. Open Docker Desktop once (from Applications) and let it start —
     you should see the whale icon in your menu bar turn steady.
  2. Then run the project:
        cd "/Users/vivekpant0709/interprocess communication/kafka-ipc-project"
        ./run.sh up
        ./run.sh build
        ./run.sh topic
        ./run.sh consume        # terminal A
        ./run.sh produce 30     # terminal B (new window)

If 'java' or 'mvn' still isn't found, close this terminal and open a new one
so your shell picks up the new tools, then re-check with 'java -version'.
DONE
