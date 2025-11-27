# ============================================================================ #
# Makefile for Android Development Workflow
# ============================================================================ #
# This Makefile provides a comprehensive set of targets for Android app
# development, covering the complete development lifecycle from building
# to testing and deployment.
#
# Key Features:
#   - Fast development cycle with incremental builds
#   - Emulator management (start, stop, cold boot)
#   - APK building and installation (debug/release)
#   - Testing support (unit, instrumented, all tests)
#   - Code quality checks (lint)
#   - Log monitoring and filtering
#   - Device management
#   - Maintenance tasks (clean, uninstall)
#
# Quick Start:
#   make help          - Display all available commands
#   make dev           - Fast development cycle (build + install + run)
#   make emulator      - Start Android emulator
#   make test          - Run all tests
#
# Requirements (macOS):
#   - Android SDK installed at ~/Library/Android/sdk
#   - Gradle wrapper (gradlew) in project root
#   - Android emulator configured with AVD name: Pixel_8
#   - ADB accessible in platform-tools
# ============================================================================ #

# Configurable variables
SDK_DIR       := $(HOME)/Library/Android/sdk
AVD_NAME      := Pixel_8
ADB           := $(SDK_DIR)/platform-tools/adb
EMULATOR      := $(SDK_DIR)/emulator/emulator -gpu host
PACKAGE_NAME  := com.example.sudoku
MAIN_ACTIVITY := .HomeActivity
APK_DEBUG     := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE   := app/build/outputs/apk/release/app-release.apk

# Colors for output
COLOR_RESET   := \033[0m
COLOR_BOLD    := \033[1m
COLOR_GREEN   := \033[32m
COLOR_YELLOW  := \033[33m
COLOR_BLUE    := \033[34m
COLOR_RED     := \033[31m
COLOR_MAGENTA := \033[35m
COLOR_CYAN    := \033[36m
NC            := $(COLOR_RESET)

LOG_FILTER    := $(PACKAGE_NAME)|AndroidRuntime

# ============================================================================ #
# Principal Makefile Targets
# ============================================================================ #

.PHONY: help build build-release install install-release run dev quick-run \
        emulator emulator-cold emulator-wait stop-emulator devices \
        clean clean-all lint test test-unit test-instrumented \
        log log-brief log-time log-error log-warn log-debug log-crash log-tag clear-log \
        uninstall version apk-info debug

# Default target
.DEFAULT_GOAL := help

# Help target
help:
	@echo "$(COLOR_GREEN)═══════════════════════════════════════════════════════════$(NC)"
	@echo "$(COLOR_BOLD)                Android Development Makefile                $(COLOR_RESET)"
	@echo "$(COLOR_GREEN)═══════════════════════════════════════════════════════════$(NC)"
	@echo ""
	@echo "$(COLOR_GREEN)Development Workflow:$(COLOR_RESET)"
	@echo "  make dev                - Build, install and run (fast development cycle)"
	@echo "  make quick-run          - Install and run without rebuilding"
	@echo "  make build              - Build debug APK"
	@echo "  make build-release      - Build release APK"
	@echo "  make install            - Install debug APK"
	@echo "  make run                - Install and launch app"
	@echo ""
	@echo "$(COLOR_GREEN)Quality Assurance:$(COLOR_RESET)"
	@echo "  make lint               - Run code quality checks"
	@echo "  make test               - Run all tests"
	@echo "  make test-unit          - Run unit tests only"
	@echo "  make test-instrumented  - Run instrumented tests on device"
	@echo ""
	@echo "$(COLOR_GREEN)Emulator Management:$(COLOR_RESET)"
	@echo "  make emulator           - Start emulator in background"
	@echo "  make emulator-cold      - Start emulator with cold boot"
	@echo "  make emulator-wait      - Wait for emulator to fully boot (up to 3min)"
	@echo "  make stop-emulator      - Stop running emulator"
	@echo "  make devices            - List connected devices"
	@echo ""
	@echo "$(COLOR_GREEN)Logging (Android Studio style):$(COLOR_RESET)"
	@echo "  make log                - Show app logs (threadtime format)"
	@echo "  make log-brief          - Show compact logs"
	@echo "  make log-time           - Show logs with timestamps"
	@echo "  make log-error          - Show error logs only"
	@echo "  make log-warn           - Show warning and error logs"
	@echo "  make log-debug          - Show debug logs"
	@echo "  make log-crash          - Show crash logs"
	@echo "  make log-tag TAG=Name   - Filter logs by tag"
	@echo "  make clear-log          - Clear logcat buffer"
	@echo ""
	@echo "$(COLOR_GREEN)Maintenance:$(COLOR_RESET)"
	@echo "  make clean              - Clean build artifacts"
	@echo "  make clean-all          - Deep clean (includes .gradle cache)"
	@echo "  make uninstall          - Uninstall app from device"
	@echo "  make version            - Show version info"
	@echo "  make apk-info           - Show APK details (size, permissions, activities)"

# ============================================================================ #
# Build the debug APK
build:
	@echo "$(COLOR_BLUE)Building debug APK...$(COLOR_RESET)"
	./gradlew assembleDebug
	@echo "$(COLOR_GREEN)Build complete: $(APK_DEBUG)$(COLOR_RESET)"

# Build release APK
build-release:
	@echo "$(COLOR_BLUE)Building release APK...$(COLOR_RESET)"
	./gradlew assembleRelease
	@echo "$(COLOR_GREEN)Build complete: $(APK_RELEASE)$(COLOR_RESET)"

# ============================================================================ #
# Install the APK to the connected device/emulator
install: build
	@echo "$(COLOR_BLUE)Installing APK...$(COLOR_RESET)"
	@if ! $(ADB) devices 2>/dev/null | grep -q "device$$"; then \
		echo "$(COLOR_YELLOW)No device connected. Start emulator with 'make emulator'$(COLOR_RESET)"; \
		exit 1; \
	fi
	@echo "$(COLOR_BLUE)Ensuring device is fully ready...$(COLOR_RESET)"
	@$(ADB) wait-for-device
	@timeout=30; \
	while [ $$timeout -gt 0 ]; do \
		boot_status=$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n' | tr -d ' '); \
		if [ "$$boot_status" = "1" ]; then \
			break; \
		fi; \
		sleep 1; \
		timeout=$$((timeout - 1)); \
	done
	@$(ADB) install -r $(APK_DEBUG)
	@echo "$(COLOR_GREEN)Installation complete$(COLOR_RESET)"

# Install release APK
install-release: build-release
	@echo "$(COLOR_BLUE)Installing release APK...$(COLOR_RESET)"
	@if ! $(ADB) devices 2>/dev/null | grep -q "device$$"; then \
		echo "$(COLOR_YELLOW)No device connected. Start emulator with 'make emulator'$(COLOR_RESET)"; \
		exit 1; \
	fi
	@$(ADB) wait-for-device
	@timeout=30; \
	while [ $$timeout -gt 0 ]; do \
		boot_status=$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n' | tr -d ' '); \
		if [ "$$boot_status" = "1" ]; then \
			break; \
		fi; \
		sleep 1; \
		timeout=$$((timeout - 1)); \
	done
	@$(ADB) install -r $(APK_RELEASE)
	@echo "$(COLOR_GREEN)Installation complete$(COLOR_RESET)"

# Build, install, and launch the app
run: install
	@echo "$(COLOR_BLUE)Launching app...$(COLOR_RESET)"
	$(ADB) shell am start -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)
	@echo "$(COLOR_GREEN)App launched$(COLOR_RESET)"

# ============================================================================ #
# Fast development cycle: incremental build + install + run
dev:
	@echo "$(COLOR_BOLD)$(COLOR_BLUE)Starting development build...$(COLOR_RESET)"
	@if ! $(ADB) devices 2>/dev/null | grep -q "device$$"; then \
		echo "$(COLOR_YELLOW)No device detected. Starting emulator...$(COLOR_RESET)"; \
		$(MAKE) --no-print-directory emulator; \
		$(MAKE) --no-print-directory emulator-wait || exit 1; \
	fi
	@./gradlew assembleDebug --parallel --daemon
	@$(MAKE) --no-print-directory quick-run

# Quick install and run without rebuilding
quick-run:
	@echo "$(COLOR_BLUE)Installing and launching...$(COLOR_RESET)"
	@if ! $(ADB) devices 2>/dev/null | grep -q "device$$"; then \
		echo "$(COLOR_YELLOW)No device connected. Please start emulator with 'make emulator'$(COLOR_RESET)"; \
		exit 1; \
	fi
	@$(ADB) wait-for-device
	@echo "$(COLOR_BLUE)Verifying boot completion...$(COLOR_RESET)"
	@timeout=45; \
	while [ $$timeout -gt 0 ]; do \
		boot_status=$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n' | tr -d ' '); \
		bootanim=$$($(ADB) shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r\n' | tr -d ' '); \
		if [ "$$boot_status" = "1" ] && [ "$$bootanim" = "stopped" ]; then \
			break; \
		fi; \
		sleep 1; \
		timeout=$$((timeout - 1)); \
	done
	@if [ $$timeout -eq 0 ]; then \
		echo "$(COLOR_YELLOW)Warning: Device may not be fully booted yet$(COLOR_RESET)"; \
	fi
	@$(ADB) install -r $(APK_DEBUG) 2>/dev/null || $(ADB) install -r $(APK_DEBUG)
	@$(ADB) shell am start -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)
	@echo "$(COLOR_GREEN)App running$(COLOR_RESET)"

# Start the app with a JDWP debugger port forwarded to localhost:8700
debug: build
	@echo "$(COLOR_BOLD)$(COLOR_BLUE)Starting debug session...$(COLOR_RESET)"
	@if ! $(ADB) devices 2>/dev/null | grep -q "device$$"; then \
		echo "$(COLOR_YELLOW)No device detected. Starting emulator...$(COLOR_RESET)"; \
		$(MAKE) --no-print-directory emulator; \
		$(MAKE) --no-print-directory emulator-wait || exit 1; \
	fi
	@$(ADB) wait-for-device
	@$(ADB) install -r $(APK_DEBUG) 2>/dev/null || $(ADB) install -r $(APK_DEBUG)
	@echo "$(COLOR_BLUE)Launching app in debug mode (waiting for debugger)...$(COLOR_RESET)"
	@$(ADB) shell am start -D -n $(PACKAGE_NAME)/$(MAIN_ACTIVITY)
	@echo "$(COLOR_BLUE)Preparing JDWP forwarding...$(COLOR_RESET)"
	@sleep 2
	@pid=$$($(ADB) shell pidof $(PACKAGE_NAME) 2>/dev/null | tr -d '\r'); \
	if [ -z "$$pid" ]; then \
		echo "$(COLOR_YELLOW)Unable to find app PID; is the app installed and running?$(COLOR_RESET)"; \
		exit 1; \
	fi; \
	$(ADB) forward --remove tcp:8700 2>/dev/null || true; \
	$(ADB) forward tcp:8700 jdwp:$$pid >/dev/null; \
	echo "$(COLOR_GREEN)JDWP forwarded: localhost:8700 -> pid $$pid$(COLOR_RESET)"; \
	echo "$(COLOR_YELLOW)Attach the debugger in VS Code now (host localhost, port 8700)$(COLOR_RESET)"

# ============================================================================ #
# Start the emulator in the background (silent)
emulator:
	@echo "$(COLOR_BLUE)Starting emulator $(AVD_NAME)...$(COLOR_RESET)"
	@if pgrep -f "emulator.*$(AVD_NAME)" > /dev/null; then \
		echo "$(COLOR_YELLOW)Emulator already running$(COLOR_RESET)"; \
	else \
		$(EMULATOR) -avd $(AVD_NAME) > /dev/null 2>&1 & \
		echo "$(COLOR_GREEN)Emulator starting in background$(COLOR_RESET)"; \
	fi

# Start the emulator with cold boot and software rendering (troubleshooting)
emulator-cold:
	@echo "$(COLOR_BLUE)Starting emulator with cold boot...$(COLOR_RESET)"
	$(EMULATOR) -avd $(AVD_NAME) -no-snapshot-load -gpu swiftshader_indirect > /dev/null 2>&1 &
	@echo "$(COLOR_GREEN)Emulator starting (cold boot)$(COLOR_RESET)"

# Wait for emulator to fully boot
emulator-wait:
	@echo "$(COLOR_BLUE)Waiting for emulator to boot completely...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)This may take 1-2 minutes on first boot$(COLOR_RESET)"
	@timeout=180; \
	counter=0; \
	device_found=0; \
	while [ $$timeout -gt 0 ]; do \
		if $(ADB) devices 2>/dev/null | grep -q "device$$"; then \
			if [ $$device_found -eq 0 ]; then \
				echo ""; \
				echo "$(COLOR_GREEN)Device detected, waiting for complete boot...$(COLOR_RESET)"; \
				device_found=1; \
			fi; \
			boot_status=$$($(ADB) shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n' | tr -d ' '); \
			bootanim=$$($(ADB) shell getprop init.svc.bootanim 2>/dev/null | tr -d '\r\n' | tr -d ' '); \
			if [ "$$boot_status" = "1" ] && [ "$$bootanim" = "stopped" ]; then \
				echo "$(COLOR_GREEN)Emulator fully booted and ready!$(COLOR_RESET)"; \
				sleep 2; \
				exit 0; \
			fi; \
		fi; \
		if [ $$((counter % 5)) -eq 0 ]; then \
			printf "."; \
		fi; \
		sleep 1; \
		timeout=$$((timeout - 1)); \
		counter=$$((counter + 1)); \
	done; \
	echo ""; \
	echo "$(COLOR_YELLOW)Timeout waiting for emulator (3 min). Check with 'make devices'$(COLOR_RESET)"; \
	exit 1

# Stop the emulator
stop-emulator:
	@echo "$(COLOR_BLUE)Stopping emulator...$(COLOR_RESET)"
	@$(ADB) emu kill 2>/dev/null || echo "$(COLOR_YELLOW)No emulator running$(COLOR_RESET)"

# ============================================================================ #
# List connected devices
devices:
	@echo "$(COLOR_BOLD)Connected Devices:$(COLOR_RESET)"
	@$(ADB) devices -l

# Run linter and code quality checks
lint:
	@echo "$(COLOR_BLUE)Running code quality checks...$(COLOR_RESET)"
	./gradlew lint
	@echo "$(COLOR_GREEN)Lint complete. Check app/build/reports/lint-results.html$(COLOR_RESET)"

# ============================================================================ #
# Run all tests
test:
	@echo "$(COLOR_BLUE)Running all tests...$(COLOR_RESET)"
	./gradlew test connectedAndroidTest

# Run unit tests only
test-unit:
	@echo "$(COLOR_BLUE)Running unit tests...$(COLOR_RESET)"
	./gradlew test
	@echo "$(COLOR_GREEN)Unit tests complete$(COLOR_RESET)"

# Run instrumented tests on connected device
test-instrumented:
	@echo "$(COLOR_BLUE)Running instrumented tests...$(COLOR_RESET)"
	@$(ADB) wait-for-device
	./gradlew connectedAndroidTest
	@echo "$(COLOR_GREEN)Instrumented tests complete$(COLOR_RESET)"

# ============================================================================ #
# Show logs for the app (Android Studio style with colors and formatting)
log:
	@echo "$(COLOR_BLUE)Showing logs for $(PACKAGE_NAME)...$(COLOR_RESET)"
	@echo "$(COLOR_YELLOW)Press Ctrl+C to stop$(COLOR_RESET)"
	@$(ADB) logcat -v threadtime *:V | grep --color=never -E "$(LOG_FILTER)" | \
	awk 'BEGIN{red="$(COLOR_RED)";yellow="$(COLOR_YELLOW)";green="$(COLOR_GREEN)";cyan="$(COLOR_CYAN)";magenta="$(COLOR_MAGENTA)";reset="$(COLOR_RESET)"}{lvl=$$5;gsub(/[^A-Z]/,"",lvl);color=reset;if(lvl=="E")color=red;else if(lvl=="W")color=yellow;else if(lvl=="I")color=green;else if(lvl=="D")color=cyan;else if(lvl=="V")color=magenta;print color $$0 reset; fflush(stdout);}'

# Show logs with brief format (more compact)
log-brief:
	@echo "$(COLOR_BLUE)Showing brief logs...$(COLOR_RESET)"
	@$(ADB) logcat -v brief *:V | grep --color=never -E "$(LOG_FILTER)" | \
	awk 'BEGIN{red="$(COLOR_RED)";yellow="$(COLOR_YELLOW)";green="$(COLOR_GREEN)";cyan="$(COLOR_CYAN)";magenta="$(COLOR_MAGENTA)";reset="$(COLOR_RESET)"}{lvl=substr($$1,1,1);color=reset;if(lvl=="E")color=red;else if(lvl=="W")color=yellow;else if(lvl=="I")color=green;else if(lvl=="D")color=cyan;else if(lvl=="V")color=magenta;print color $$0 reset; fflush(stdout);}'

# Show logs with time format (Android Studio default)
log-time:
	@echo "$(COLOR_BLUE)Showing logs with timestamps...$(COLOR_RESET)"
	@$(ADB) logcat -v time *:V | grep --color=never -E "$(LOG_FILTER)" | \
	awk 'BEGIN{red="$(COLOR_RED)";yellow="$(COLOR_YELLOW)";green="$(COLOR_GREEN)";cyan="$(COLOR_CYAN)";magenta="$(COLOR_MAGENTA)";reset="$(COLOR_RESET)"}{lvl=$$5;gsub(/[^A-Z]/,"",lvl);color=reset;if(lvl=="E")color=red;else if(lvl=="W")color=yellow;else if(lvl=="I")color=green;else if(lvl=="D")color=cyan;else if(lvl=="V")color=magenta;print color $$0 reset; fflush(stdout);}'

# Show error logs only
log-error:
	@echo "$(COLOR_BLUE)Showing error logs...$(COLOR_RESET)"
	@$(ADB) logcat -v threadtime *:E | grep --color=never -E "$(LOG_FILTER)" | \
	awk 'BEGIN{red="$(COLOR_RED)";reset="$(COLOR_RESET)"}{print red $$0 reset; fflush(stdout);}'

# Show warning and error logs
log-warn:
	@echo "$(COLOR_BLUE)Showing warning and error logs...$(COLOR_RESET)"
	@$(ADB) logcat -v threadtime *:W | grep --color=never -E "$(LOG_FILTER)" | \
	awk 'BEGIN{red="$(COLOR_RED)";yellow="$(COLOR_YELLOW)";reset="$(COLOR_RESET)"}{lvl=$$5;gsub(/[^A-Z]/,"",lvl);color=reset;if(lvl=="E")color=red;else color=yellow;print color $$0 reset; fflush(stdout);}'

# Show debug logs
log-debug:
	@echo "$(COLOR_BLUE)Showing debug logs...$(COLOR_RESET)"
	@$(ADB) logcat -v threadtime *:D | grep --color=never -E "$(LOG_FILTER)" | \
	awk 'BEGIN{cyan="$(COLOR_CYAN)";reset="$(COLOR_RESET)"}{print cyan $$0 reset; fflush(stdout);}'

# Show crash logs
log-crash:
	@echo "$(COLOR_BLUE)Showing crash logs...$(COLOR_RESET)"
	@$(ADB) logcat -b crash -v threadtime

# Filter logs by tag
log-tag:
	@echo "$(COLOR_BLUE)Usage: make log-tag TAG=YourTag$(COLOR_RESET)"
	@if [ -z "$(TAG)" ]; then \
		echo "$(COLOR_YELLOW)Please specify TAG variable$(COLOR_RESET)"; \
		exit 1; \
	fi
	@$(ADB) logcat -v threadtime -s $(TAG):*

# Clear logcat buffer
clear-log:
	@echo "$(COLOR_BLUE)Clearing log buffer...$(COLOR_RESET)"
	@$(ADB) logcat -c
	@echo "$(COLOR_GREEN)Log buffer cleared$(COLOR_RESET)"

# ============================================================================ #
# Clean the project
clean:
	@echo "$(COLOR_BLUE)Cleaning build artifacts...$(COLOR_RESET)"
	./gradlew clean
	@echo "$(COLOR_GREEN)Clean complete$(COLOR_RESET)"

# Deep clean including gradle cache
clean-all:
	@echo "$(COLOR_BLUE)Deep cleaning project...$(COLOR_RESET)"
	./gradlew clean cleanBuildCache
	@rm -rf .gradle build app/build
	@echo "$(COLOR_GREEN)Deep clean complete$(COLOR_RESET)"

# Uninstall app from device
uninstall:
	@echo "$(COLOR_BLUE)Uninstalling app...$(COLOR_RESET)"
	@$(ADB) uninstall $(PACKAGE_NAME) 2>/dev/null || echo "$(COLOR_YELLOW)App not installed$(COLOR_RESET)"

# Show version information
version:
	@echo "$(COLOR_BOLD)Version Information:$(COLOR_RESET)"
	@grep "versionName" app/build.gradle.kts | head -1
	@grep "versionCode" app/build.gradle.kts | head -1

# Show APK information (size, permissions, version)
apk-info:
	@echo "$(COLOR_BOLD)APK Information:$(COLOR_RESET)"
	@if [ ! -f "$(APK_DEBUG)" ]; then \
		echo "$(COLOR_YELLOW)APK not found. Run 'make build' first$(COLOR_RESET)"; \
		exit 1; \
	fi
	@echo ""
	@echo "$(COLOR_GREEN)File:$(COLOR_RESET) $(APK_DEBUG)"
	@du -h "$(APK_DEBUG)" | awk '{print "$(COLOR_GREEN)Size:$(COLOR_RESET) " $$1}'
	@echo ""
	@echo "$(COLOR_GREEN)Package Info:$(COLOR_RESET)"
	@AAPT=$$(find "$(SDK_DIR)/build-tools" -name aapt -type f | sort -V | tail -1); \
	if [ -n "$$AAPT" ]; then \
		"$$AAPT" dump badging "$(APK_DEBUG)" | grep -E "package:|sdkVersion:|targetSdkVersion:|uses-permission:"; \
	else \
		echo "$(COLOR_YELLOW)aapt tool not found$(COLOR_RESET)"; \
	fi
	@echo ""
	@echo "$(COLOR_GREEN)Activities:$(COLOR_RESET)"
	@AAPT=$$(find "$(SDK_DIR)/build-tools" -name aapt -type f | sort -V | tail -1); \
	if [ -n "$$AAPT" ]; then \
		"$$AAPT" dump badging "$(APK_DEBUG)" | grep "launchable-activity"; \
	fi
