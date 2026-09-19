# Implementation Plan: Fix Settings, Websocket & Search

## 1. Fix WebSocket Crash & Search (Urgent)
- **Problem**: Adding the `about` field to `VibeMessage` caused the backend's strict JSON parser to reject ALL messages (including `search_users` and profile updates), breaking search and real-time updates.
- **Solution**: Revert the addition of `about` in `VibeMessage` data class. In the `updateProfile` method, use the existing `content` field to transmit the "About me" string.

## 2. Fix Typo in Placeholder
- **Problem**: Placeholder text renders as `Введите Юзернейм.lowercase()`.
- **Solution**: Use proper Kotlin string template syntax `${title.lowercase()}` in `EditProfileFieldContent.kt`.

## 3. Add Icon to Input Field
- **Problem**: The input field lacks a descriptive icon on the left.
- **Solution**: Add an `icon: ImageVector` parameter to `EditProfileFieldContent`. Modify the `decorationBox` to include a `Row` with the icon and a `Spacer` before the `innerTextField`.

## 4. Implement Username Availability Check
- **Problem**: No real-time check for username availability.
- **Solution**: 
  - Add an `errorMessage` state to `SettingsScreen` for the username screen.
  - In `EditProfileFieldContent` for username, when typing (debounced), call `webSocket.checkAvailability(email = "", username = newValue)`.
  - Add a temporary listener to `webSocket` in `SettingsScreen` to listen for `onAuthResponse` (which handles `username_taken` and `message`) to update the `errorMessage`.
  - Pass the `errorMessage` down to `EditProfileFieldContent` and render it in red below the input field if it's not null.

## User Review Required
Does this plan sound correct? Specifically, for the username check, the backend will return a response containing `username_taken` flag, correct?
