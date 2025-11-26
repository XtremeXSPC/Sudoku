SDK_DIR := $(HOME)/Library/Android/sdk
AVD_NAME := Pixel_8
ADB := $(SDK_DIR)/platform-tools/adb
EMULATOR := $(SDK_DIR)/emulator/emulator

.PHONY: build install run emulator clean log

# Build the debug APK
build:
	./gradlew assembleDebug

# Install the APK to the connected device/emulator
install:
	$(ADB) wait-for-device install -r app/build/outputs/apk/debug/app-debug.apk

# Build, install, and launch the app
run: install
	$(ADB) shell am start -n com.example.sudoku/.HomeActivity

# Start the emulator in the background (silent)
emulator:
	$(EMULATOR) -avd $(AVD_NAME) > /dev/null 2>&1 &
	@echo "Emulator starting..."

# Start the emulator with cold boot and software rendering (troubleshooting)
emulator-cold:
	$(EMULATOR) -avd $(AVD_NAME) -no-snapshot-load -gpu swiftshader_indirect > /dev/null 2>&1 &
	@echo "Emulator starting (cold boot)..."

# Stop the emulator
stop-emulator:
	$(ADB) emu kill

# Clean the project
clean:
	./gradlew clean

# Show logs for the app
log:
	$(ADB) logcat | grep "com.example.sudoku"
