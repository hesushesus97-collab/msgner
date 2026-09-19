package com.flasskdev.vibe.ui.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * NOTE: intentionally an interface with two singleton implementations instead of a
 * `data class`.
 *
 * A JVM/DEX method signature is limited to 255 argument slots (the implicit `this`
 * counts as one). A `data class` with N properties generates a constructor AND a
 * `copy()` with N parameters, so once this table passed ~254 entries the generated
 * `<init>` / `copy` no longer fit and the DEX verifier failed at install time
 * ("invalid arg count" on invoke-direct/range). Incremental Apply Changes hid it;
 * a clean rebuild did not.
 *
 * Properties on an object are plain field initializers + getters, so there is no
 * per-method argument limit and the table can grow freely. Adding a string means:
 * declare it in the interface, then override it in BOTH objects (the compiler
 * enforces that, exactly like named constructor arguments did).
 */
interface VibeStrings {

    // Auth & Nickname & Verification
    val createAccount: String
    val welcomeBack: String
    val emailLabel: String
    val usernameLabel: String
    val emailInvalidFormat: String
    val continueBtn: String
    val verificationTitle: String
    val verificationSubtitle: (String) -> String
    val verifyBtn: String
    val verifyLoading: String
    val codeInvalid: String
    // Код входа пришёл в Vibe на другое устройство (сообщением от Vibe cat), а не на почту
    val verificationSubtitleApp: String
    val nicknameTitle: String
    val nicknameLabel: String
    val saveBtn: String
    val saveLoading: String
    val errorSaving: String
    val freezedAcc: String
    val bannedAcc: String


    // Main Container & Tabs
    val tabChats: String
    val tabSettings: String
    val tabProfile: String


    // Profile
    val profileTitle: String
    val statusOnline: String
    val statusBot: String
    val statusUnknown: String
    val actionForward: String
    val draftLabel: String
    val badgeVerified: String
    val badgeDeveloper: String
    val badgeBot: String
    val aboutLabel: String
    val registerDateLabel: String
    val btnTheme: String
    val themeDark: String
    val themeLight: String
    val btnLanguage: String
    val btnLogout: String
    val logoutConfirmTitle: String
    val logoutConfirmText: String
    val logoutCancel: String
    val logoutConfirm: String
    val userLabel: String


    // Chats
    val chatsTitle: String
    val chatsEmptyTitle: String
    val chatsEmptySubtitle: String
    val searchPlaceholder: String
    val globalSearchResults: String
    val typing: String
    val connecting: String
    val waitingForNetwork: String
    val monthsShort: List<String>


    // Last Seen
    val dateToday: String
    val dateYesterday: String
    val lastSeenRecently: String
    val lastSeenLongAgo: String
    val lastSeenInWeek: String
    val lastSeenInMonth: String
    val lastSeenToday: (String) -> String
    val lastSeenYesterday: (String) -> String
    val lastSeenDate: (String, String) -> String


    // Chat Screen
    val backBtn: String
    val messagePlaceholder: String
    val replyTo: String
    val emptyChat: String
    val chatHistoryCleared: String
    val chatHistoryEmpty: String
    val deletedAcc: String


    // Onboarding
    val onboardingPages: List<Pair<String, String>>


    // Chat Actions
    val selectedMessagesCount: (Int) -> String
    val deleteMessagesTitle: String
    val deleteMessagesText: (Int) -> String
    val deleteForEveryone: (String) -> String
    val deleteForEveryoneAlsoMine: (String) -> String
    val deleteBtn: String
    val cancelBtn: String
    val forwardMessageTitle: String
    val noRecentChats: String
    val editMessageTitle: String
    val sendBtn: String
    val forwardedFrom: (String) -> String
    val replyDefault: String


    // Pinned messages and others
    val pinnedMessage: String
    val pinnedMessages: String
    val pinMessage: String
    val pinMessageConfirm: String
    val unpinMessage: String
    val unpinMessageConfirm: String
    val unpinAll: String
    val unpinAllConfirm: String
    val forBoth: (String) -> String
    val pin: String
    val unpin: String
    val you: String
    val edit: String


    // Settings & Profile editing
    val usernameTaken: String
    val usernameAvailable: String
    val usernameDescription: String
    val usernameMinLength: String
    val nicknameDescription: String
    val bioDescription: String
    val doneBtn: String


    // Chat restrictions & status
    val userRestrictedMessaging: String
    val userHiddenAccount: String
    val editedLabel: String


    // Mute
    val muteNotifications: String
    val unmuteNotifications: String


    // System messages
    val pinnedMessageSystemText: (String, String) -> String


    // Privacy settings
    val privacyScreenTitle: String
    val privacyTwoFactor: String
    val privacyPasscodeLogin: String
    val privacyBlocked: String
    val privacyActivityTitle: String
    val privacyActivityDesc: String
    val privacyAvatarTitle: String
    val privacyAvatarDesc: String
    val privacyForwardedTitle: String
    val privacyForwardedDesc: String
    val privacyMessagesTitle: String
    val privacyMessagesDesc: String
    val privacyStatusTitle: String
    val privacyStatusDesc: String

    // Report dialog
    val reportTitle: String
    val reportSubtitle: String
    val reportStepLabel: (Int, Int) -> String
    val reportReasonSpam: String
    val reportReasonSpamDesc: String
    val reportReasonFraud: String
    val reportReasonFraudDesc: String
    val reportReasonDrugs: String
    val reportReasonDrugsDesc: String
    val reportReasonWeapons: String
    val reportReasonWeaponsDesc: String
    val reportReasonPorn: String
    val reportReasonPornDesc: String
    val reportReasonCsam: String
    val reportReasonCsamDesc: String
    val reportReasonViolence: String
    val reportReasonViolenceDesc: String
    val reportReasonHarassment: String
    val reportReasonHarassmentDesc: String
    val reportReasonHate: String
    val reportReasonHateDesc: String
    val reportReasonFakeAccount: String
    val reportReasonFakeAccountDesc: String
    val reportReasonMisinfo: String
    val reportReasonMisinfoDesc: String
    val reportCriticalNotice: String
    val reportDetailsTitle: String
    val reportDetailsHint: String
    val reportCommentPlaceholder: String
    val reportCommentCounter: (Int, Int) -> String
    val reportChangeReason: String
    val reportSubmitBtn: String
    val reportCancelBtn: String
    val reportSentTitle: String
    val reportSentDesc: String
    val reportDoneBtn: String
    val a11yReportClose: String

    // Privacy exceptions picker
    val privacyExceptionsTitle: String
    val privacyExceptionsHint: String
    val privacyExceptionsSelected: (Int) -> String
    val privacyExceptionsSelectAll: String
    val privacyExceptionsClearAll: String
    val privacyExceptionsEmptyTitle: String
    val privacyExceptionsEmptyDesc: String
    val a11yExceptionToggle: (String) -> String
    val a11yExceptionRemove: (String) -> String


    // Settings sections
    val settingsPrivacy: String
    val settingsAccount: String
    val settingsDevices: String
    val settingsPasscode: String


    // Profile screen extras
    val usernameCopied: String
    val addAvatar: String
    val choosePhoto: String


    // Media types
    val typeVideo: String
    val typeVideoMessage: String
    val typeAudio: String
    val typeFile: String
    val typeVoice: String


    // Media player
    val playerPlaylist: String
    val playerSearchTracks: String
    val playerNowPlaying: String
    val playerTrackFallback: String
    val playerTracksCount: (Int) -> String
    val playerQueueEmpty: String
    val playerQueueEmptyHint: String
    val playerSearchEmptyTitle: String
    val playerSearchEmptySubtitle: (String) -> String
    val playerBuffering: String
    val playerTimeZero: String
    val playerTimeUnknown: String
    val playerSpeedFormat: (String) -> String
    val playerRepeatOff: String
    val playerRepeatAll: String
    val playerRepeatOne: String
    val a11yPlayerPlay: String
    val a11yPlayerPause: String
    val a11yPlayerNext: String
    val a11yPlayerPrevious: String
    val a11yPlayerRewind10: String
    val a11yPlayerForward10: String
    val a11yPlayerShuffle: String
    val a11yPlayerClose: String
    val a11yPlayerExpand: String
    val a11yPlayerCollapse: String
    val a11yPlayerSearch: String
    val a11yPlayerSearchClose: String
    val a11yPlayerClearSearch: String
    val a11yPlayerArtwork: String
    val a11yPlayerSpeed: (String) -> String
    val a11yPlayerTrackRow: (String) -> String


    // Profile media sections
    val sectionPhotosVideos: String
    val sectionFiles: String
    val sectionMusic: String
    val sectionVoice: String
    val profileSearchFiles: String


    // Downloads
    val actionDownload: String
    val actionDownloadSelected: String


    // Auth screen
    val switchSignIn: String
    val switchSignUp: String
    val authTabSignUp: String
    val authTabSignIn: String
    val authChecking: String
    val authEmailAvailable: String
    val authEmailTaken: String
    val authUsernameTakenShort: String
    val authUsernameAvailable: (String) -> String
    val authRegisterFailed: String
    val authLoginFailed: String
    val authUsernameCounter: (Int, Int) -> String
    val languageName: String


    // Blocked users screen
    val blockedTitle: String
    val blockedSearchPlaceholder: String
    val blockedClearSearch: String
    val blockedEmptyTitle: String
    val blockedEmptyDesc: String
    val blockedSearchEmptyTitle: String
    val blockedSearchEmptyDesc: (String) -> String
    val blockedUnblockBtn: String
    val blockedUnblockConfirmTitle: String
    val blockedUnblockConfirmText: (String) -> String
    val blockedUnblockedToast: String
    val blockedUserFallback: (Int) -> String
    val accountDeleted: String
    val accountFrozen: String
    val accountBannedMessage: String
    val accountFrozenMessage: String


    // Text formatting
    val formatCopy: String
    val formatCut: String
    val formatFormat: String
    val formatBold: String
    val formatItalic: String
    val formatBoldItalic: String
    val formatStrikethrough: String
    val formatUnderline: String
    val formatMonospace: String
    val formatLink: String
    val formatTextColor: String
    val formatSpoiler: String
    val formatQuote: String
    val formatLinkUrlHint: String
    val formatColorHint: String
    val formatPreview: String
    val formatCopied: String
    val formatReadMore: String
    val formatCollapse: String


    // Attachment menu
    val attachPhotoVideo: String
    val attachFile: String
    val attachTitle: String


    // Photo viewer
    val photoViewer: String
    val photoOf: (Int, Int) -> String


    // Message context menu
    val actionCopy: String
    val actionReport: String


    // Chat header (toolbar, search, selection, pinned banner)
    val chatSearchPlaceholder: String
    val chatSearchNoResults: String
    val chatSearchCounter: (Int, Int) -> String
    val chatSearchClose: String
    val chatSearchClear: String
    val chatSearchByDate: String
    val chatSearchNext: String
    val chatSearchPrev: String
    val chatActionSearch: String
    val chatMenu: String
    val chatAvatar: String
    val chatOpenProfile: String
    val chatClearSelection: String
    val chatStatusBlockedByMe: String
    val chatBlockUser: String
    val chatUnblockUser: String
    val chatUserBlockedToast: String
    val chatPinnedCounter: (Int, Int) -> String
    val chatJumpToPinned: String
    val chatUnpinAllHint: String


    // Chat input bar (composer, attachments, voice recording)
    val inputAttachMedia: String
    val inputAttachGallery: String
    val inputAttachFile: String
    val inputEmojiPanel: String
    val inputSelectedMedia: (Int) -> String
    val inputClearAttachments: String
    val inputBlockedByMe: String
    val inputAttachmentPreview: String
    val inputCancelReply: String
    val voiceHoldToRecord: String
    val voiceSendRecording: String
    val voiceCancelRecording: String
    val voiceLocked: String
    val voiceSlideToCancel: String
    val voiceSlideToLock: String
    val voiceRecordStartFailed: String
    val voicePermissionRequired: String
    val voiceTooShort: String

    // ПУНКТ 2 — кружки (видеосообщения)
    val circleModeSwitchedOn: String
    val circleModeSwitchedOff: String
    val circleRecordVideoMessage: String
    val circleHoldOrTapHint: String
    val circleRecordingHint: String
    val circleReleaseToCancel: String
    val circleLockedHint: String
    val circleTapToStop: String
    val circleCameraPreparing: String
    val circleMaxDurationHint: String
    val circlePermissionRequired: String
    val circleTooShort: String
    val circleCancel: String
    val circleSend: String
    val circleSwitchCamera: String
    val circleSendFailed: String


    // Chat list (iOS redesign)
    val chatsSectionPinned: String
    val filterAll: String
    val filterUnread: String
    val filterUnreadCount: (Int) -> String
    val chatsNoUnreadTitle: String
    val chatsNoUnreadSubtitle: String
    val chatsSectionAll: String
    val chatsCountFooter: (Int) -> String
    val chatsEmptyHint: String
    val chatsSearchCancel: String
    val chatsSearchClearField: String
    val chatsSearchNoResultsTitle: String
    val chatsSearchNoResultsSubtitle: (String) -> String
    val userFallback: (Int) -> String
    val someoneLabel: String
    val a11yMutedChat: String
    val a11yPinnedChat: String
    val actionMuteShort: String
    val actionUnmuteShort: String


    // Chat list message previews
    val typePhoto: String
    val previewVoiceMessage: (String) -> String
    val previewVideoMessage: (String) -> String
    val previewAudioTrack: (String, String) -> String
    val previewAudioLoading: String
    val previewMorePhotos: (Int) -> String
    val previewMoreVideos: (Int) -> String
    val previewMoreWithCaption: (Int, String) -> String
    val typeSticker: String
    val typeGif: String
    val previewMediaCount: (Int) -> String
    val previewMoreAudio: (Int) -> String
    val previewMoreFiles: (Int) -> String
    val previewMoreAttachments: (Int) -> String
    val actionSelectMessage: String

    // Chat message list (empty state, system messages, scroll-to-bottom)
    val emptyChatSubtitle: String
    val emptyChatHint: String
    val chatSystemMessageLabel: String
    val linkOpenFailed: String
    val a11yMessageList: String
    val a11yScrollToBottom: String
    val a11yUnreadCount: (Int) -> String
    val unreadCountOverflow: String

    // Chat dialogs, reactions sheet, bot & network toasts
    val okBtn: String
    val actionClose: String
    val reportSentToast: String
    val restrictionTitle: String
    val restrictionUnderstood: String
    val restrictionWhy: String
    val datePickerTitle: String
    val dateJumpNotFound: String
    val botMessageTitle: String
    val botLabel: String
    val attachmentLabel: String
    val fileSizeLoading: String
    val reactionsTitle: String
    val reactionsAllTab: (Int) -> String
    val reactionsEmpty: String
    val voiceTrackTitleMine: String
    val botCallbackTimeout: String
    val connectionLostToast: String
    val fileOpenFailed: String
    val maxPinnedChatsToast: (Int) -> String

    // Chat toast host
    val toastTitleInfo: String
    val toastTitleSuccess: String
    val toastTitleWarning: String
    val toastTitleError: String
    val toastActionRetry: String
    val toastActionUndo: String
    val toastCopied: String
    val a11yToast: (String) -> String
    val a11yToastDismiss: String

    // Devices & sessions
    val devicesTitle: String
    val devicesSubtitle: String
    val devicesSessionsCount: (Int) -> String
    val devicesRefreshCd: String
    val devicesSectionCurrent: String
    val devicesSectionOther: String
    val devicesCurrentBadge: String
    val devicesOnlineNow: String
    val devicesLastActiveNow: String
    val devicesLastActiveMinutes: (Int) -> String
    val devicesLastActiveHours: (Int) -> String
    val devicesLastActiveYesterday: String
    val devicesLastActiveDate: (String) -> String
    val devicesDateTimePattern: String
    val devicesUnknownDevice: String
    val devicesUnknownLocation: String
    val devicesNoOtherSessions: String
    val devicesNoOtherSessionsHint: String
    val devicesEmptyTitle: String
    val devicesEmptySubtitle: String
    val devicesLoading: String
    val devicesLoadFailedTitle: String
    val devicesLoadFailedSubtitle: String
    val devicesTerminateCd: String
    val devicesTerminateAll: String
    val devicesTerminateTitle: String
    val devicesTerminateText: (String) -> String
    val devicesTerminateAllTitle: String
    val devicesTerminateAllText: (Int) -> String
    val devicesTerminateConfirm: String
    val devicesSecurityHint: String

    // Edit profile field
    val editFieldSave: String
    val editFieldSaveCd: String
    val editFieldPlaceholder: (String) -> String
    val editFieldClearCd: String
    val editFieldCounter: (Int, Int) -> String
    val editFieldLimitReached: String
    val editFieldUnsavedTitle: String
    val editFieldUnsavedText: String
    val editFieldUnsavedDiscard: String

    // Main container navigation (a11y)
    val a11yTab: (String) -> String

    // Profile screen (redesign)
    val profileSectionInfo: String
    val profileSectionAppearance: String
    val profileSectionSession: String
    val profileCopyUsername: String
    val profileCopyUsernameHint: String
    val a11yAvatar: String
    val a11yEditAvatar: String
    val a11yAvatarPreview: String
    val a11yChoosePhoto: String
    val avatarCropHint: String
    val avatarPickPrompt: String

    // Profile screen (extra)
    val profileNotFoundTitle: String
    val profileNotFoundDesc: (String) -> String
    val profileLoading: String
    val profileWriteBtn: String
    val profileBlockedTitle: String
    val profileBlockedDesc: String
    val a11yProfileMenu: String
    val a11yAvatarViewerClose: String

    // Compact number units
    val unitCompactFormat: (String, String) -> String
    val unitThousandShort: String
    val unitMillionShort: String
    val unitBillionShort: String

    // Settings root list
    val settingsChats: String
    val settingsChatsSubtitle: String
    val settingsPrivacySubtitle: String
    val settingsNotifications: String
    val settingsNotificationsSubtitle: String
    val settingsPowerSaving: String
    val settingsPowerSavingSubtitle: String
    val settingsDevicesSubtitle: String
    val settingsLanguageSubtitle: String
    val settingsAccountSubtitle: String
    val settingsSupport: String
    val settingsSupportSubtitle: String
    val settingsVibePro: String
    val settingsVibeProSubtitle: String
    val settingsVibeProCta: String
    // Vibe Pro screen
    val vibeProHeroDescription: String
    val vibeProSectionFeatures: String
    val vibeProSectionPlans: String
    val vibeProFeatureLimitsTitle: String
    val vibeProFeatureLimitsSubtitle: String
    val vibeProFeatureVoiceToTextTitle: String
    val vibeProFeatureVoiceToTextSubtitle: String
    val vibeProFeatureReactionsTitle: String
    val vibeProFeatureReactionsSubtitle: String
    val vibeProFeatureBadgeTitle: String
    val vibeProFeatureBadgeSubtitle: String
    val vibeProFeatureSpeedTitle: String
    val vibeProFeatureSpeedSubtitle: String
    val vibeProFeatureNoAdsTitle: String
    val vibeProFeatureNoAdsSubtitle: String
    val vibeProPlanYearly: String
    val vibeProPlanYearlyPrice: String
    val vibeProPlanYearlyDiscount: String
    val vibeProPlanMonthly: String
    val vibeProPlanMonthlyPrice: String
    val vibeProSubscribeCta: (String) -> String
    val vibeProAutoRenewalDisclaimer: String
    val vibeProComingSoonToast: String
    // Two-factor authentication (2FA)
    val twoFactorTitle: String
    val twoFactorSubtitle: String
    val twoFactorDescription: String
    val twoFactorStatusEnabled: String
    val twoFactorStatusDisabled: String
    val twoFactorEnabledBadge: String
    val twoFactorEnabledDesc: String
    val twoFactorBullet1Title: String
    val twoFactorBullet1Desc: String
    val twoFactorBullet2Title: String
    val twoFactorBullet2Desc: String
    val twoFactorBullet3Title: String
    val twoFactorBullet3Desc: String
    val twoFactorSetPasswordBtn: String
    val twoFactorChangePasswordBtn: String
    val twoFactorChangeHintBtn: String
    val twoFactorDisableBtn: String
    val twoFactorEnterNewPasswordTitle: String
    val twoFactorEnterNewPasswordSubtitle: String
    val twoFactorRepeatPasswordTitle: String
    val twoFactorRepeatPasswordSubtitle: String
    val twoFactorEnterCurrentPasswordTitle: String
    val twoFactorEnterCurrentPasswordSubtitle: String
    val twoFactorHintTitle: String
    val twoFactorHintSubtitle: String
    val twoFactorHintPlaceholder: String
    val twoFactorHintTooLong: String
    val twoFactorHintContainsPassword: String
    val twoFactorHintPublicWarning: String
    val twoFactorPasswordTooShort: String
    val twoFactorPasswordMismatch: String
    val twoFactorPasswordWrong: String
    val twoFactorDisableConfirmTitle: String
    val twoFactorDisableConfirmDesc: String
    val twoFactorDisableAction: String
    val twoFactorNextBtn: String
    val twoFactorSkipBtn: String
    val twoFactorSaveBtn: String
    val twoFactorStrengthWeak: String
    val twoFactorStrengthMedium: String
    val twoFactorStrengthStrong: String
    val twoFactorStrengthVeryStrong: String
    val twoFactorCurrentHintPill: (String) -> String
    val twoFactorSuccessSetToast: String
    val twoFactorSuccessChangedToast: String
    val twoFactorSuccessDisabledToast: String
    val twoFactorPasswordFieldLabel: String
    val twoFactorConfirmFieldLabel: String
    val twoFactorCurrentFieldLabel: String
    val twoFactorHintFieldLabel: String
    val settingsVibes: String
    val settingsVibesSubtitle: String
    val settingsGroupGeneral: String
    val settingsGroupExtras: String
    val settingsGroupHelp: String
    val settingsSoonBadge: String
    val appVersion: (String) -> String

    // Onboarding controls
    val onboardingGetStarted: String
    val onboardingSkip: String
    val a11yOnboardingPage: (Int, Int) -> String

    // Passcode
    val passcodeEnterTitle: String
    val passcodeEnterSubtitle: String
    val passcodeEnterCurrentTitle: String
    val passcodeCreateTitle: String
    val passcodeRepeatTitle: String
    val passcodeInfoTitle: String
    val passcodeInfoText: String
    val passcodeEnableBtn: String
    val passcodeChangeBtn: String
    val passcodeDisableBtn: String
    val passcodeDisableShort: String
    val passcodeRemoveTitle: String
    val passcodeRemoveText: String
    val passcodeWrongCode: String
    val passcodeMismatch: String
    val a11yPasscodeLock: String
    val a11yPasscodeBackspace: String
    val a11yPasscodeDigit: (String) -> String

    // Nickname screen
    val nicknameHint: String

    // Shared UI components (button, text field, OTP, toast, inline keyboard)
    val a11yLoading: String
    val a11yOtpInput: String
    val a11yOtpDigit: (Int, Int) -> String
    val a11yOtpDigitEmpty: (Int, Int) -> String
    val a11yFieldError: (String) -> String
    val a11yClearField: String
    val a11yInlineButtonLink: String
    val a11yInlineButtonLoading: String

    // Link confirmation dialog & inline formatting (a11y)
    val linkDialogTitle: String
    val linkDialogSubtitle: String
    val linkDialogSecure: String
    val linkDialogInsecure: String
    val linkDialogOpen: String
    val linkDialogCancel: String
    val a11yLinkChip: (String) -> String
    val a11ySpoilerHidden: String
    val a11ySpoilerRevealed: String
    val a11yQuote: String
    val formatInlineQuoteWrap: (String) -> String

    // Account settings screen
    val accountTitle: String
    val accountSectionProfile: String
    val accountUsernameLabel: String
    val accountNicknameLabel: String
    val accountNotSet: String
    val accountNoName: String
    val accountSectionAbout: String
    val accountBioLabel: String
    val accountBioPlaceholder: String
    val accountPrivacyFootPrefix: String
    val accountPrivacyFootLink: String
    val accountPrivacyFootSuffix: String
    val accountLogoutTitle: String
    val accountLogoutSubtitle: String
    val accountLogoutDialogTitle: String
    val accountLogoutDialogText: String
    val a11yEditProfile: String
    val a11yCopy: String

    // Notification settings
    val notifSectionGeneral: String
    val notifSectionSound: String
    val notifMuteAll: String
    val notifMuteAllDesc: String
    val notifAutoMute: String
    val notifAutoMuteDesc: String
    val notifAutoMuteFootnote: String
    val notifSoundTitle: String
    val notifSoundSilent: String
    val notifSoundDefault: String
    val notifSoundCustom: String
    val notifSoundFootnote: String
    val notifNoServerResponse: String
    val notifNoPicker: String
    val notifSyncing: String

    // Power saving settings
    val powerSectionMode: String
    val powerEnableNow: String
    val powerEnableNowDesc: String
    val powerAutoTitle: String
    val powerAutoDesc: String
    val powerThreshold: String
    val powerSectionDisable: String
    val powerLiquid: String
    val powerBlur: String
    val powerGlow: String
    val powerPreviews: String
    val powerFootnote: String

    // Language settings
    val languageSearch: String
    val languageSectionTitle: String
    val languageNotFound: String
    val languageFootnote: String

    // Two-factor: server flow and sign-in challenge
    val twoFactorCheckingServer: String
    val twoFactorNoServerResponse: String
    val twoFactorDisconnected: String
    val twoFactorPasswordTooLong: String
    val twoFactorChallengeTitle: String
    val twoFactorChallengeSubtitle: String
    val twoFactorShowHint: String
    val twoFactorYourHint: String
    val twoFactorForgotPassword: String
    val twoFactorSignInBtn: String
    val twoFactorAttemptsLeft: (Int) -> String
    val twoFactorForgotUnavailable: String
    val a11yShowPassword: String
    val a11yHidePassword: String

    // Two-factor: recovery e-mail and password reset
    val twoFactorRecoveryEmailBtn: String
    val twoFactorRecoveryEmailNotSet: String
    val twoFactorRecoveryEmailFootnote: String
    val twoFactorRecoveryEmailTitle: String
    val twoFactorRecoveryEmailSubtitle: String
    val twoFactorRecoveryEmailCurrent: (String) -> String
    val twoFactorRecoveryEmailFieldLabel: String
    val twoFactorRecoveryEmailInvalid: String
    val twoFactorRecoveryEmailSendCodeBtn: String
    val twoFactorRecoveryEmailRemoveBtn: String
    val twoFactorRecoveryCodeTitle: String
    val twoFactorRecoveryCodeSubtitle: (String) -> String
    val twoFactorRecoveryConfirmBtn: String
    val twoFactorResendCodeBtn: String
    val twoFactorRecoveryEmailSavedToast: String
    val twoFactorRecoveryEmailRemovedToast: String
    val twoFactorResetBtn: String
    val twoFactorResetTitle: String
    val twoFactorResetSubtitle: (String) -> String
    val twoFactorResetConfirmBtn: String
    val twoFactorResetSettingsConfirmBtn: String
    val twoFactorResetWarning: String
    val twoFactorResetNoEmail: String
    val twoFactorResetFailed: String
    val twoFactorResetDoneToast: String

    // Emoji / sticker / GIF panel
    val gifSearchPlaceholder: String
    val gifNotFound: String
    val gifLoadFailed: String
    val gifSendCd: String
    val panelTabEmoji: String
    val panelTabStickers: String
    val panelTabGifs: String
    val panelRecent: String

    // Passcode settings (status page in the 2FA style)
    val passcodeStatusEnabledBadge: String
    val passcodeStatusEnabledDesc: String
    val passcodeBullet1Title: String
    val passcodeBullet1Desc: String
    val passcodeBullet2Title: String
    val passcodeBullet2Desc: String
    val passcodeBullet3Title: String
    val passcodeBullet3Desc: String
    val passcodeStepCurrentSubtitle: String
    val passcodeStepNewSubtitle: String
    val passcodeStepRepeatSubtitle: String
    val passcodeSavedToast: String
    val passcodeRemovedToast: String

    // Power saving threshold control
    val powerThresholdHint: (Int) -> String
    val powerThresholdPresetCd: (Int) -> String

// Two-factor edit screen
    val twoFactorCurrentPasswordLabel: String
    val twoFactorNewPasswordLabel: String
    val twoFactorRepeatPasswordLabel: String
    val twoFactorHintNoPassword: String
    val twoFactorHintTooLongShort: String
    val twoFactorHintPublicDesc: String
    val twoFactorDisableButton: String
    val twoFactorAdditionalPasswordTitle: String
    val twoFactorAdditionalPasswordDesc: String
    val passwordStrengthWeak: String
    val passwordStrengthMedium: String
    val passwordStrengthGood: String
    val passwordStrengthStrong: String
    val twoFactorDisableDialogTitle: String
    val twoFactorDisableDialogDesc: String

    // Privacy option screen
    val privacyValueEverybody: String
    val privacyValueNobody: String
    val privacyValueSelected: String
    val privacySelectUsers: String
    val privacySelectUsersRuleDesc: String

    // Inline video player & media covers
    val videoPlaybackFailed: String
    val videoScaleCd: String
    val videoCoverCd: String
    val videoNoFrameCd: String
    val videoPlayCd: String
    val sampleText: String
    val a11yMuteSound: String
    val a11yUnmuteSound: String

    // Circle recording errors
    val circleCameraInitFailed: String
    val circleNoCamera: String
    val circleCameraUnavailable: (String) -> String
    val circleCameraNotReady: String
    val circleRecordStartFailedMsg: (String) -> String
    val circleRecordFailedMsg: (Int) -> String
    val circleEmptyRecord: String

    // Emoji categories
    val emojiCategorySmileys: String
    val emojiCategoryGestures: String
    val emojiCategoryHearts: String
    val emojiCategoryAnimals: String
    val emojiCategoryFood: String
    val emojiCategoryActivities: String
    val emojiCategoryTravel: String
    val emojiCategoryObjects: String

    // Download helper & notifications
    val errorGeneric: String
    val downloadFileDescription: String
    val downloadStartedToast: String
    val downloadErrorToast: (String) -> String
    val downloadMultipleToast: (Int) -> String
    val notifMe: String
    val unitSecondShort: String

    val locale: String
}

object RuStrings : VibeStrings {
    override val createAccount: String = "Создание аккаунта"
    override val welcomeBack: String = "С возвращением"
    override val emailLabel: String = "ПОЧТА"
    override val usernameLabel: String = "ЮЗЕРНЕЙМ"
    override val emailInvalidFormat: String = "НЕВЕРНЫЙ ФОРМАТ"
    override val continueBtn: String = "ПРОДОЛЖИТЬ"
    override val verificationTitle: String = "Введите код"
    override val verificationSubtitle: (String) -> String = { email -> "Мы отправили 6-значный код на вашу почту\n$email" }
    override val verifyBtn: String = "ПОДТВЕРДИТЬ"
    override val verifyLoading: String = "ПРОВЕРКА..."
    override val codeInvalid: String = "Неверный код"
    override val verificationSubtitleApp: String = "Код отправлен в приложение Vibe на другом авторизованном устройстве.\nОткройте чат с Vibe cat, чтобы увидеть его"
    override val nicknameTitle: String = "Как вас зовут?"
    override val nicknameLabel: String = "ВАШ НИКНЕЙМ"
    override val saveBtn: String = "ПРОДОЛЖИТЬ"
    override val saveLoading: String = "СОХРАНЕНИЕ..."
    override val errorSaving: String = "Ошибка при сохранении"
    override val tabChats: String = "Чаты"
    override val tabSettings: String = "Настройки"
    override val tabProfile: String = "Профиль"
    override val profileTitle: String = "Профиль"
    override val deletedAcc: String = "Удаленный аккаунт"
    override val statusOnline: String = "В сети"
    override val statusBot: String = "Бот"
    override val statusUnknown: String = "Неизвестно"
    override val actionForward: String = "Переслать"
    override val badgeVerified: String = "Пользователь верифицирован командой Vibe."
    override val badgeDeveloper: String = "Член команды разработчиков Vibe."
    override val badgeBot: String = "Просто бот."
    override val freezedAcc: String = "Аккаунт заморожен за нарушение правил."
    override val bannedAcc: String = "Аккаунт заблокирован за нарушение правил."
    override val aboutLabel: String = "Описание"
    override val registerDateLabel: String = "Дата регистрации"
    override val btnTheme: String = "Тема"
    override val themeDark: String = "Темная"
    override val themeLight: String = "Светлая"
    override val btnLanguage: String = "Язык"
    override val btnLogout: String = "Выйти из аккаунта"
    override val logoutConfirmTitle: String = "Выход из аккаунта"
    override val logoutConfirmText: String = "Вы уверены, что хотите выйти из аккаунта?"
    override val logoutCancel: String = "Отмена"
    override val logoutConfirm: String = "Выйти"
    override val userLabel: String = "Пользователь"
    override val chatsTitle: String = "Чаты"
    override val chatsEmptyTitle: String = "Нет чатов"
    override val chatsEmptySubtitle: String = "Начните переписку!"
    override val searchPlaceholder: String = "Поиск..."
    override val globalSearchResults: String = "Глобальный поиск"
    override val typing: String = "Печатает"
    override val connecting: String = "Соединение"
    override val waitingForNetwork: String = "Ожидание сети"
    override val monthsShort: List<String> = listOf("янв", "фев", "мар", "апр", "мая", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    override val dateToday: String = "Сегодня"
    override val dateYesterday: String = "Вчера"
    override val lastSeenRecently: String = "Был(а) недавно"
    override val lastSeenLongAgo: String = "Был(а) очень давно"
    override val lastSeenInWeek: String = "Был(а) на этой неделе"
    override val lastSeenInMonth: String = "Был(а) в этом месяце"
    override val lastSeenToday: (String) -> String = { time -> "Был(а) сегодня в $time" }
    override val lastSeenYesterday: (String) -> String = { time -> "Был(а) вчера в $time" }
    override val lastSeenDate: (String, String) -> String = { date, time -> "Был(а) $date в $time" }
    override val backBtn: String = "Назад"
    override val draftLabel: String = "Черновик: "
    override val messagePlaceholder: String = "Сообщение..."
    override val replyTo: String = "Ответить"
    override val emptyChat: String = "Здесь пока пусто..."
    override val chatHistoryCleared: String = "История очищена"
    override val chatHistoryEmpty: String = "История пуста"
    override val onboardingPages: List<Pair<String, String>> = listOf(
        "Безопасность данных" to "Ваше общение защищено шифрованием военного уровня.",
        "Скорость света" to "Протоколы нового поколения для мгновенной доставки сообщений.",
        "Универсальная синхронизация" to "Вся ваша история данных доступна на каждом устройстве.",
        "Глобальные сообщества" to "Масштабируйте сообщества до миллионов активных участников.",
        "Уникальный Вайб" to "Настройте каждый аспект вашего мессенджера под себя."
    )
    override val selectedMessagesCount: (Int) -> String = { count -> "Выбрано: $count" }
    override val deleteMessagesTitle: String = "Удалить сообщения?"
    override val deleteMessagesText: (Int) -> String = { count -> "Вы собираетесь удалить $count сообщений." }
    override val deleteForEveryone: (String) -> String = { name -> "Также удалить для $name" }
    override val deleteForEveryoneAlsoMine: (String) -> String = { name -> "Также удалить свои сообщения для $name" }
    override val deleteBtn: String = "Удалить"
    override val cancelBtn: String = "Отмена"
    override val forwardMessageTitle: String = "Переслать сообщение"
    override val noRecentChats: String = "Нет недавних чатов"
    override val editMessageTitle: String = "Редактирование сообщения"
    override val sendBtn: String = "Отправить"
    override val forwardedFrom: (String) -> String = { name -> "Переслано от $name" }
    override val replyDefault: String = "Ответ"
    override val pinnedMessage: String = "Закрепленное сообщение"
    override val pinnedMessages: String = "Закрепленные сообщения"
    override val pinMessage: String = "Закрепить сообщение"
    override val pinMessageConfirm: String = "Вы действительно хотите закрепить это сообщение?"
    override val unpinMessage: String = "Открепить сообщение"
    override val unpinMessageConfirm: String = "Вы действительно хотите открепить это сообщение?"
    override val unpinAll: String = "Открепить все"
    override val unpinAllConfirm: String = "Открепить все сообщения в этом чате?"
    override val forBoth: (String) -> String = { name -> "Также для $name" }
    override val pin: String = "Закрепить"
    override val unpin: String = "Открепить"
    override val you: String = "Вы"
    override val edit: String = "Изменить"
    override val usernameTaken: String = "Этот юзернейм уже занят"
    override val usernameAvailable: String = "Юзернейм свободен"
    override val usernameDescription: String = "Вы можете выбрать уникальное имя пользователя в Vibe. Если вы это сделаете, другие люди смогут найти вас по этому имени и связаться с вами, не зная вашего номера телефона."
    override val usernameMinLength: String = "Минимальная длина 4 символа"
    override val nicknameDescription: String = "Ваше имя, которое будет отображаться всем пользователям. Постарайтесь выбрать узнаваемое имя, чтобы друзья могли легко вас найти."
    override val bioDescription: String = "Напишите немного о себе. Эта информация будет видна другим пользователям в вашем профиле."
    override val doneBtn: String = "Готово"
    override val userRestrictedMessaging: String = "Пользователь ограничил круг общения"
    override val userHiddenAccount: String = "Пользователь скрыл аккаунт"
    override val editedLabel: String = " (изменено)"
    override val muteNotifications: String = "Выключить уведомления"
    override val unmuteNotifications: String = "Включить уведомления"
    override val pinnedMessageSystemText: (String, String) -> String = { sender, content -> "$sender закрепил(а) сообщение: \"$content\"" }
    override val privacyScreenTitle: String = "Конфиденциальность"
    override val privacyTwoFactor: String = "Двойная аутентификация"
    override val privacyPasscodeLogin: String = "Вход по коду"
    override val privacyBlocked: String = "Заблокированные"
    override val privacyActivityTitle: String = "Статус активности"
    override val privacyActivityDesc: String = "Кто может видеть, когда вы в последний раз были в сети. Если вы скроете свой статус активности, вы не сможете видеть статус других пользователей (будет отображаться примерное время)."
    override val privacyAvatarTitle: String = "Аватарка"
    override val privacyAvatarDesc: String = "Кто может видеть вашу аватарку. Для остальных будет отображаться первая буква вашего имени на синем фоне."
    override val privacyForwardedTitle: String = "Пересланные сообщения"
    override val privacyForwardedDesc: String = "Кто может переходить на ваш профиль из пересланных сообщений."
    override val privacyMessagesTitle: String = "Сообщения"
    override val privacyMessagesDesc: String = "Кто может отправлять вам сообщения. Пользователи, которым это запрещено, увидят надпись «Пользователь ограничил круг общения»."
    override val privacyStatusTitle: String = "Статус"
    override val privacyStatusDesc: String = "Кто может видеть блок «О себе» в вашем профиле."

    // Report dialog
    override val reportTitle: String = "Пожаловаться"
    override val reportSubtitle: String = "Выберите причину. Жалоба анонимна, автор её не увидит."
    override val reportStepLabel: (Int, Int) -> String = { current, total -> "Шаг $current из $total" }
    override val reportReasonSpam: String = "Спам"
    override val reportReasonSpamDesc: String = "Реклама, рассылки, накрутка"
    override val reportReasonFraud: String = "Мошенничество"
    override val reportReasonFraudDesc: String = "Обман, фишинг, схемы с деньгами"
    override val reportReasonDrugs: String = "Наркотики"
    override val reportReasonDrugsDesc: String = "Продажа или реклама запрещённых веществ"
    override val reportReasonWeapons: String = "Оружие"
    override val reportReasonWeaponsDesc: String = "Торговля оружием и взрывчаткой"
    override val reportReasonPorn: String = "Порнография"
    override val reportReasonPornDesc: String = "Материалы для взрослых без предупреждения"
    override val reportReasonCsam: String = "Материалы с детьми (CSAM)"
    override val reportReasonCsamDesc: String = "Сексуализированный контент с несовершеннолетними"
    override val reportReasonViolence: String = "Насилие"
    override val reportReasonViolenceDesc: String = "Угрозы, жестокость, призывы к вреду"
    override val reportReasonHarassment: String = "Травля"
    override val reportReasonHarassmentDesc: String = "Оскорбления, преследование, шантаж"
    override val reportReasonHate: String = "Разжигание ненависти"
    override val reportReasonHateDesc: String = "Нападки по признаку расы, религии, пола"
    override val reportReasonFakeAccount: String = "Фейковый аккаунт"
    override val reportReasonFakeAccountDesc: String = "Выдаёт себя за другого человека"
    override val reportReasonMisinfo: String = "Ложная информация"
    override val reportReasonMisinfoDesc: String = "Опасные слухи и дезинформация"
    override val reportCriticalNotice: String = "Такие жалобы мы рассматриваем в приоритетном порядке и передаём в профильные органы."
    override val reportDetailsTitle: String = "Расскажите подробнее"
    override val reportDetailsHint: String = "Опишите ситуацию: что произошло и где. Это поможет нам принять меры быстрее."
    override val reportCommentPlaceholder: String = "Комментарий (необязательно)"
    override val reportCommentCounter: (Int, Int) -> String = { used, limit -> "$used / $limit" }
    override val reportChangeReason: String = "Другая причина"
    override val reportSubmitBtn: String = "Отправить жалобу"
    override val reportCancelBtn: String = "Отмена"
    override val reportSentTitle: String = "Жалоба отправлена"
    override val reportSentDesc: String = "Спасибо. Модераторы изучат обращение и примут решение."
    override val reportDoneBtn: String = "Готово"
    override val a11yReportClose: String = "Закрыть"

    // Privacy exceptions picker
    override val privacyExceptionsTitle: String = "Исключения"
    override val privacyExceptionsHint: String = "Выберите, на кого правило не распространяется"
    override val privacyExceptionsSelected: (Int) -> String = { count -> "Выбрано: $count" }
    override val privacyExceptionsSelectAll: String = "Выбрать всех"
    override val privacyExceptionsClearAll: String = "Снять выбор"
    override val privacyExceptionsEmptyTitle: String = "Некого выбрать"
    override val privacyExceptionsEmptyDesc: String = "Здесь появятся люди, с которыми у вас есть чаты."
    override val a11yExceptionToggle: (String) -> String = { name -> "Выбрать $name" }
    override val a11yExceptionRemove: (String) -> String = { name -> "Убрать $name из выбранных" }
    override val settingsPrivacy: String = "Приватность"
    override val settingsAccount: String = "Аккаунт"
    override val settingsDevices: String = "Устройства"
    override val settingsPasscode: String = "Код-пароль"
    override val usernameCopied: String = "Юзернейм скопирован"
    override val addAvatar: String = "Добавить аватарку"
    override val choosePhoto: String = "Выбрать фото"
    override val switchSignIn: String = "Уже есть аккаунт? Войти"
    override val switchSignUp: String = "Нет аккаунта? Создать"
    override val authTabSignUp: String = "Регистрация"
    override val authTabSignIn: String = "Вход"
    override val authChecking: String = "Проверяем..."
    override val authEmailAvailable: String = "Почта свободна"
    override val authEmailTaken: String = "ЭТА ПОЧТА ЗАНЯТА"
    override val authUsernameTakenShort: String = "ЭТОТ ЮЗЕРНЕЙМ ЗАНЯТ"
    override val authUsernameAvailable: (String) -> String = { username -> "@$username свободен" }
    override val authRegisterFailed: String = "Не удалось создать аккаунт"
    override val authLoginFailed: String = "Не удалось войти"
    override val authUsernameCounter: (Int, Int) -> String = { current, max -> "$current/$max" }
    override val languageName: String = "Русский"
    override val blockedTitle: String = "Заблокированные"
    override val blockedSearchPlaceholder: String = "Поиск пользователей..."
    override val blockedClearSearch: String = "Очистить поиск"
    override val blockedEmptyTitle: String = "Нет заблокированных"
    override val blockedEmptyDesc: String = "Здесь будут отображаться пользователи, которых вы заблокировали."
    override val blockedSearchEmptyTitle: String = "Ничего не найдено"
    override val blockedSearchEmptyDesc: (String) -> String = { query -> "По запросу «$query» пользователей не найдено" }
    override val blockedUnblockBtn: String = "Разблокировать"
    override val blockedUnblockConfirmTitle: String = "Разблокировать?"
    override val blockedUnblockConfirmText: (String) -> String = { name -> "$name снова сможет писать вам и видеть ваш профиль." }
    override val blockedUnblockedToast: String = "Пользователь разблокирован"
    override val blockedUserFallback: (Int) -> String = { id -> "Пользователь #$id" }
    override val accountDeleted: String = "Удаленный аккаунт"
    override val accountFrozen: String = "Замороженный аккаунт"
    override val accountBannedMessage: String = "Ваш аккаунт был заблокирован за нарушение правил."
    override val accountFrozenMessage: String = "Ваш аккаунт был заморожен модератором."
    override val typeVideo: String = "Видео"
    override val typeVideoMessage: String = "Видеосообщение"
    override val typeAudio: String = "Аудиофайл"
    override val typeFile: String = "Файл"
    override val typeVoice: String = "Голосовое сообщение"
    override val playerPlaylist: String = "Плейлист чата"
    override val playerSearchTracks: String = "Поиск треков"
    override val playerNowPlaying: String = "Сейчас играет"
    override val playerTrackFallback: String = "Аудиозапись"
    override val playerTracksCount: (Int) -> String = { count ->
        val word = when {
            count % 10 == 1 && count % 100 != 11 -> "трек"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "трека"
            else -> "треков"
        }
        "$count $word"
    }
    override val playerQueueEmpty: String = "Очередь пуста"
    override val playerQueueEmptyHint: String = "Аудио из этого чата появится здесь"
    override val playerSearchEmptyTitle: String = "Ничего не найдено"
    override val playerSearchEmptySubtitle: (String) -> String = { query -> "По запросу «$query» треков нет" }
    override val playerBuffering: String = "Буферизация…"
    override val playerTimeZero: String = "0:00"
    override val playerTimeUnknown: String = "--:--"
    override val playerSpeedFormat: (String) -> String = { value -> "$value×" }
    override val playerRepeatOff: String = "Повтор выключен"
    override val playerRepeatAll: String = "Повторять плейлист"
    override val playerRepeatOne: String = "Повторять трек"
    override val a11yPlayerPlay: String = "Воспроизвести"
    override val a11yPlayerPause: String = "Пауза"
    override val a11yPlayerNext: String = "Следующий трек"
    override val a11yPlayerPrevious: String = "Предыдущий трек"
    override val a11yPlayerRewind10: String = "Назад на 10 секунд"
    override val a11yPlayerForward10: String = "Вперёд на 10 секунд"
    override val a11yPlayerShuffle: String = "Перемешать"
    override val a11yPlayerClose: String = "Закрыть плеер"
    override val a11yPlayerExpand: String = "Развернуть плеер"
    override val a11yPlayerCollapse: String = "Свернуть плеер"
    override val a11yPlayerSearch: String = "Поиск по плейлисту"
    override val a11yPlayerSearchClose: String = "Закрыть поиск"
    override val a11yPlayerClearSearch: String = "Очистить поиск"
    override val a11yPlayerArtwork: String = "Обложка трека"
    override val a11yPlayerSpeed: (String) -> String = { value -> "Скорость воспроизведения: $value" }
    override val a11yPlayerTrackRow: (String) -> String = { title -> "Включить трек $title" }
    override val sectionPhotosVideos: String = "Фото и видео"
    override val sectionFiles: String = "Файлы"
    override val sectionMusic: String = "Музыка"
    override val sectionVoice: String = "Голосовые"
    override val profileSearchFiles: String = "Поиск файлов"
    override val actionDownload: String = "Скачать"
    override val actionDownloadSelected: String = "Скачать выбранные"
    override val formatCopy: String = "Скопировать"
    override val formatCut: String = "Вырезать"
    override val formatFormat: String = "Форматировать"
    override val formatBold: String = "Жирный"
    override val formatItalic: String = "Курсив"
    override val formatBoldItalic: String = "Жирный курсив"
    override val formatStrikethrough: String = "Зачеркнутый"
    override val formatUnderline: String = "Подчеркнутый"
    override val formatMonospace: String = "Моноширинный"
    override val formatLink: String = "Ссылка"
    override val formatTextColor: String = "Цвет текста"
    override val formatSpoiler: String = "Спойлер"
    override val formatQuote: String = "Цитата"
    override val formatLinkUrlHint: String = "Введите URL"
    override val formatColorHint: String = "Введите HEX (напр. #FF5733)"
    override val formatPreview: String = "Предпросмотр"
    override val formatCopied: String = "Скопировано"
    override val formatReadMore: String = "Читать далее"
    override val formatCollapse: String = "Свернуть"
    override val attachPhotoVideo: String = "Фото и видео"
    override val attachFile: String = "Файл"
    override val attachTitle: String = "Вложения"
    override val photoViewer: String = "Просмотр фото"
    override val photoOf: (Int, Int) -> String = { current, total -> "$current из $total" }
    override val actionCopy: String = "Скопировать текст"
    override val actionReport: String = "Пожаловаться"


    // Chat header
    override val chatSearchPlaceholder: String = "Поиск по сообщениям…"
    override val chatSearchNoResults: String = "Ничего не найдено"
    override val chatSearchCounter: (Int, Int) -> String = { current, total -> "$current из $total" }
    override val chatSearchClose: String = "Закрыть поиск"
    override val chatSearchClear: String = "Очистить запрос"
    override val chatSearchByDate: String = "Поиск по дате"
    override val chatSearchNext: String = "Следующий результат"
    override val chatSearchPrev: String = "Предыдущий результат"
    override val chatActionSearch: String = "Поиск"
    override val chatMenu: String = "Меню чата"
    override val chatAvatar: String = "Аватар"
    override val chatOpenProfile: String = "Открыть профиль"
    override val chatClearSelection: String = "Снять выделение"
    override val chatStatusBlockedByMe: String = "Заблокирован"
    override val chatBlockUser: String = "Заблокировать"
    override val chatUnblockUser: String = "Разблокировать"
    override val chatUserBlockedToast: String = "Пользователь заблокирован"
    override val chatPinnedCounter: (Int, Int) -> String = { current, total -> "$current/$total" }
    override val chatJumpToPinned: String = "Перейти к закреплённому сообщению"
    override val chatUnpinAllHint: String = "Открепить все"


    // Chat input bar
    override val inputAttachMedia: String = "Прикрепить"
    override val inputAttachGallery: String = "Фото или видео"
    override val inputAttachFile: String = "Файл"
    override val inputEmojiPanel: String = "Эмодзи, стикеры и GIF"
    override val inputSelectedMedia: (Int) -> String = { count -> "Выбрано медиа: $count" }
    override val inputClearAttachments: String = "Очистить вложения"
    override val inputBlockedByMe: String = "Вы заблокировали этого пользователя"
    override val inputAttachmentPreview: String = "Вложение"
    override val inputCancelReply: String = "Отменить"
    override val voiceHoldToRecord: String = "Удерживайте для записи"
    override val voiceSendRecording: String = "Отправить голосовое сообщение"
    override val voiceCancelRecording: String = "Отменить запись"
    override val voiceLocked: String = "Запись закреплена"
    override val voiceSlideToCancel: String = "Смахните влево, чтобы отменить"
    override val voiceSlideToLock: String = "Вверх — закрепить"
    override val voiceRecordStartFailed: String = "Не удалось начать запись голосового сообщения"
    override val voicePermissionRequired: String = "Для записи нужен доступ к микрофону"
    override val voiceTooShort: String = "Запись получилась слишком короткой"

    // ПУНКТ 2 — кружки (видеосообщения)
    override val circleModeSwitchedOn: String = "Режим кружков: удерживайте для записи"
    override val circleModeSwitchedOff: String = "Режим голосовых сообщений"
    override val circleRecordVideoMessage: String = "Записать видеосообщение"
    override val circleHoldOrTapHint: String = "Нажмите для записи, удерживайте для быстрой съёмки"
    override val circleRecordingHint: String = "Влево — отмена · вверх — закрепить"
    override val circleReleaseToCancel: String = "Отпустите для отмены"
    override val circleLockedHint: String = "Нажмите, чтобы отправить"
    override val circleTapToStop: String = "Нажмите, чтобы остановить и отправить"
    override val circleCameraPreparing: String = "Готовим камеру…"
    override val circleMaxDurationHint: String = "Максимум 60 секунд"
    override val circlePermissionRequired: String = "Для кружков нужен доступ к камере и микрофону"
    override val circleTooShort: String = "Кружок получился слишком коротким"
    override val circleCancel: String = "Отменить запись"
    override val circleSend: String = "Отправить кружок"
    override val circleSwitchCamera: String = "Сменить камеру"
    override val circleSendFailed: String = "Не удалось отправить кружок"


    // Chat list (iOS redesign)
    override val chatsSectionPinned: String = "Закреплённые"
    override val filterAll: String = "Все"
    override val filterUnread: String = "Непрочитанные"
    override val filterUnreadCount: (Int) -> String = { count -> "Непрочитанные ($count)" }
    override val chatsNoUnreadTitle: String = "Всё прочитано"
    override val chatsNoUnreadSubtitle: String = "Непрочитанных сообщений нет."
    override val chatsSectionAll: String = "Все чаты"
    override val chatsCountFooter: (Int) -> String = { count -> "Чатов: $count" }
    override val chatsEmptyHint: String = "Найдите пользователя через поиск выше"
    override val chatsSearchCancel: String = "Отмена"
    override val chatsSearchClearField: String = "Очистить поле поиска"
    override val chatsSearchNoResultsTitle: String = "Ничего не найдено"
    override val chatsSearchNoResultsSubtitle: (String) -> String = { query -> "Нет чатов и пользователей по запросу «$query»" }
    override val userFallback: (Int) -> String = { id -> "Пользователь #$id" }
    override val someoneLabel: String = "Кто-то"
    override val a11yMutedChat: String = "Уведомления выключены"
    override val a11yPinnedChat: String = "Чат закреплён"
    override val actionMuteShort: String = "Без звука"
    override val actionUnmuteShort: String = "Со звуком"


    // Chat list message previews
    override val typePhoto: String = "Фотография"
    override val previewVoiceMessage: (String) -> String = { duration -> "Голосовое сообщение $duration" }
    override val previewVideoMessage: (String) -> String = { duration -> "Видеосообщение $duration" }
    override val previewAudioTrack: (String, String) -> String = { artist, title -> "$artist — $title" }
    override val previewAudioLoading: String = "Музыка..."
    override val previewMorePhotos: (Int) -> String = { count -> "+$count фотографий" }
    override val previewMoreVideos: (Int) -> String = { count -> "+$count видео" }
    override val previewMoreWithCaption: (Int, String) -> String = { count, caption -> "+$count $caption" }
    override val typeSticker: String = "Стикер"
    override val typeGif: String = "GIF"
    override val previewMediaCount: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "медиафайл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "медиафайла"
            else -> "медиафайлов"
        }
        "$count $form"
    }
    override val previewMoreAudio: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "аудиофайл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "аудиофайла"
            else -> "аудиофайлов"
        }
        "$count $form"
    }
    override val previewMoreFiles: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "файл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "файла"
            else -> "файлов"
        }
        "$count $form"
    }
    override val previewMoreAttachments: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "вложение"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "вложения"
            else -> "вложений"
        }
        "$count $form"
    }
    override val actionSelectMessage: String = "Выбрать"
    // Chat message list (empty state, system messages, scroll-to-bottom)
    override val emptyChatSubtitle: String = "Напишите первое сообщение, и история чата появится здесь."
    override val emptyChatHint: String = "Начните разговор первым"
    override val chatSystemMessageLabel: String = "Системное сообщение"
    override val linkOpenFailed: String = "Не удалось открыть ссылку"
    override val a11yMessageList: String = "Список сообщений"
    override val a11yScrollToBottom: String = "Перейти к последним сообщениям"
    override val a11yUnreadCount: (Int) -> String = { count -> "Новых сообщений: $count" }
    override val unreadCountOverflow: String = "99+"
    // Chat dialogs, reactions sheet, bot & network toasts
    override val okBtn: String = "OK"
    override val actionClose: String = "Закрыть"
    override val reportSentToast: String = "Жалоба успешно отправлена"
    override val restrictionTitle: String = "Ограничение"
    override val restrictionUnderstood: String = "Понятно"
    override val restrictionWhy: String = "Почему?"
    override val datePickerTitle: String = "Выберите дату"
    override val dateJumpNotFound: String = "Сообщений за эту дату не найдено"
    override val botMessageTitle: String = "Сообщение от бота"
    override val botLabel: String = "Бот"
    override val attachmentLabel: String = "Вложение"
    override val fileSizeLoading: String = "Загрузка..."
    override val reactionsTitle: String = "Реакции"
    override val reactionsAllTab: (Int) -> String = { count -> "Все $count" }
    override val reactionsEmpty: String = "Пока нет реакций"
    override val voiceTrackTitleMine: String = "Вы (Голосовое сообщение)"
    override val botCallbackTimeout: String = "Бот не ответил за 10 секунд. Кнопки этого сообщения снова доступны."
    override val connectionLostToast: String = "Нет соединения с сервером. Попробуйте ещё раз."
    override val fileOpenFailed: String = "Не удалось открыть выбранный файл"
    override val maxPinnedChatsToast: (Int) -> String = { limit -> "Можно закрепить не более $limit чатов" }
    // Chat toast host
    override val toastTitleInfo: String = "Информация"
    override val toastTitleSuccess: String = "Готово"
    override val toastTitleWarning: String = "Внимание"
    override val toastTitleError: String = "Ошибка"
    override val toastActionRetry: String = "Повторить"
    override val toastActionUndo: String = "Отменить"
    override val toastCopied: String = "Скопировано"
    override val a11yToast: (String) -> String = { text -> "Уведомление: $text" }
    override val a11yToastDismiss: String = "Скрыть уведомление"
    // Devices & sessions
    override val devicesTitle: String = "Устройства"
    override val devicesSubtitle: String = "Здесь показаны все входы в ваш аккаунт"
    override val devicesSessionsCount: (Int) -> String = { count ->
        val tail = count % 10
        val hundred = count % 100
        when {
            hundred in 11..14 -> "$count активных сеансов"
            tail == 1 -> "$count активный сеанс"
            tail in 2..4 -> "$count активных сеанса"
            else -> "$count активных сеансов"
        }
    }
    override val devicesRefreshCd: String = "Обновить список"
    override val devicesSectionCurrent: String = "ЭТО УСТРОЙСТВО"
    override val devicesSectionOther: String = "ДРУГИЕ СЕАНСЫ"
    override val devicesCurrentBadge: String = "Текущее"
    override val devicesOnlineNow: String = "В сети"
    override val devicesLastActiveNow: String = "Только что"
    override val devicesLastActiveMinutes: (Int) -> String = { minutes ->
        val tail = minutes % 10
        val hundred = minutes % 100
        val word = when {
            hundred in 11..14 -> "минут"
            tail == 1 -> "минуту"
            tail in 2..4 -> "минуты"
            else -> "минут"
        }
        "$minutes $word назад"
    }
    override val devicesLastActiveHours: (Int) -> String = { hours ->
        val tail = hours % 10
        val hundred = hours % 100
        val word = when {
            hundred in 11..14 -> "часов"
            tail == 1 -> "час"
            tail in 2..4 -> "часа"
            else -> "часов"
        }
        "$hours $word назад"
    }
    override val devicesLastActiveYesterday: String = "Вчера"
    override val devicesLastActiveDate: (String) -> String = { date -> "Был(а): $date" }
    override val devicesDateTimePattern: String = "dd.MM.yyyy, HH:mm"
    override val devicesUnknownDevice: String = "Неизвестное устройство"
    override val devicesUnknownLocation: String = "Местоположение неизвестно"
    override val devicesNoOtherSessions: String = "Других сеансов нет"
    override val devicesNoOtherSessionsHint: String = "В аккаунт выполнен вход только с этого устройства"
    override val devicesEmptyTitle: String = "Активных сеансов нет"
    override val devicesEmptySubtitle: String = "Не удалось найти ни одного входа в аккаунт. Попробуйте обновить список."
    override val devicesLoading: String = "Загружаем сеансы..."
    override val devicesLoadFailedTitle: String = "Не удалось загрузить"
    override val devicesLoadFailedSubtitle: String = "Сервер не ответил. Проверьте соединение и попробуйте снова."
    override val devicesTerminateCd: String = "Завершить сеанс"
    override val devicesTerminateAll: String = "Завершить все"
    override val devicesTerminateTitle: String = "Завершить сеанс?"
    override val devicesTerminateText: (String) -> String = { name ->
        "Устройство «$name» выйдет из аккаунта. Для повторного входа понадобится код подтверждения."
    }
    override val devicesTerminateAllTitle: String = "Завершить все сеансы?"
    override val devicesTerminateAllText: (Int) -> String = { count ->
        "Из аккаунта выйдут все другие устройства ($count). Это устройство останется в сети."
    }
    override val devicesTerminateConfirm: String = "Завершить"
    override val devicesSecurityHint: String = "Не узнаёте устройство? Завершите сеанс и смените пароль."
    // Edit profile field
    override val editFieldSave: String = "Сохранить"
    override val editFieldSaveCd: String = "Сохранить изменения"
    override val editFieldPlaceholder: (String) -> String = { title -> "Введите ${title.lowercase()}" }
    override val editFieldClearCd: String = "Очистить поле"
    override val editFieldCounter: (Int, Int) -> String = { used, max -> "$used / $max" }
    override val editFieldLimitReached: String = "Достигнут лимит символов"
    override val editFieldUnsavedTitle: String = "Сохранить изменения?"
    override val editFieldUnsavedText: String = "У вас есть несохранённые изменения. Сохранить их перед выходом?"
    override val editFieldUnsavedDiscard: String = "Сбросить"
    // Main container navigation (a11y)
    override val a11yTab: (String) -> String = { name -> "Вкладка «$name»" }
    // Profile screen (redesign)
    override val profileSectionInfo: String = "Информация"
    override val profileSectionAppearance: String = "Оформление"
    override val profileSectionSession: String = "Сеанс"
    override val profileCopyUsername: String = "Скопировать юзернейм"
    override val profileCopyUsernameHint: String = "Нажмите, чтобы скопировать"
    override val a11yAvatar: String = "Аватар профиля"
    override val a11yEditAvatar: String = "Изменить аватар"
    override val a11yAvatarPreview: String = "Предпросмотр аватара"
    override val a11yChoosePhoto: String = "Выбрать фото"
    override val avatarCropHint: String = "Перетаскивайте и сводите пальцы, чтобы разместить фото. Загрузится область внутри круга."
    override val avatarPickPrompt: String = "Нажмите, чтобы выбрать фото"

    // Profile screen (extra)
    override val profileNotFoundTitle: String = "Пользователь не найден"
    override val profileNotFoundDesc: (String) -> String = { username -> "$username не зарегистрирован в Vibe или удалил аккаунт." }
    override val profileLoading: String = "Загружаем профиль..."
    override val profileWriteBtn: String = "Написать"
    override val profileBlockedTitle: String = "Вы заблокировали этого пользователя"
    override val profileBlockedDesc: String = "Он не может писать вам и не видит ваш статус."
    override val a11yProfileMenu: String = "Ещё"
    override val a11yAvatarViewerClose: String = "Закрыть просмотр"
    // Compact number units
    override val unitCompactFormat: (String, String) -> String = { number, unit -> "$number $unit" }
    override val unitThousandShort: String = "тыс."
    override val unitMillionShort: String = "млн."
    override val unitBillionShort: String = "млрд."
    // Settings root list
    override val settingsChats: String = "Настройки чатов"
    override val settingsChatsSubtitle: String = "Тема, обои, размер текста"
    override val settingsPrivacySubtitle: String = "Кто видит вас и пишет вам"
    override val settingsNotifications: String = "Уведомления"
    override val settingsNotificationsSubtitle: String = "Звуки, превью, приоритет"
    override val settingsPowerSaving: String = "Экономия энергии"
    override val settingsPowerSavingSubtitle: String = "Анимации и фоновая работа"
    override val settingsDevicesSubtitle: String = "Активные сеансы и выход"
    override val settingsLanguageSubtitle: String = "Язык интерфейса"
    override val settingsAccountSubtitle: String = "Имя, юзернейм, описание"
    override val settingsSupport: String = "Поддержка"
    override val settingsSupportSubtitle: String = "Вопросы, отчёты об ошибках"
    override val settingsVibePro: String = "Vibe Pro"
    override val settingsVibeProSubtitle: String = "Больше лимитов, эксклюзивные функции"
    override val settingsVibeProCta: String = "Подробнее"
    // Vibe Pro screen
    override val vibeProHeroDescription: String = "Максимальные возможности общения, увеличенные лимиты и эксклюзивный статус в Vibe."
    override val vibeProSectionFeatures: String = "ВОЗМОЖНОСТИ ПОДПИСКИ"
    override val vibeProSectionPlans: String = "ТАРИФНЫЙ ПЛАН"
    override val vibeProFeatureLimitsTitle: String = "Увеличенные лимиты"
    override val vibeProFeatureLimitsSubtitle: String = "Отправка файлов до 2 ГБ, до 100 закрепленных чатов и 20 папок"
    override val vibeProFeatureVoiceToTextTitle: String = "Голосовые в текст"
    override val vibeProFeatureVoiceToTextSubtitle: String = "Мгновенная расшифровка аудио- и видеосообщений одним касанием"
    override val vibeProFeatureReactionsTitle: String = "Эксклюзивные реакции"
    override val vibeProFeatureReactionsSubtitle: String = "Анимированные стикеры, уникальные эмодзи и эмодзи-статусы"
    override val vibeProFeatureBadgeTitle: String = "Премиум-значок"
    override val vibeProFeatureBadgeSubtitle: String = "Особый значок Pro рядом с вашим именем в чатах и профиле"
    override val vibeProFeatureSpeedTitle: String = "Сверхбыстрая скорость"
    override val vibeProFeatureSpeedSubtitle: String = "Загрузка и отправка медиафайлов без ограничения пропускной способности"
    override val vibeProFeatureNoAdsTitle: String = "Полная свобода"
    override val vibeProFeatureNoAdsSubtitle: String = "Никакой рекламы, приоритетная техническая поддержка 24/7"
    override val vibeProPlanYearly: String = "1 год"
    override val vibeProPlanYearlyPrice: String = "149 ₽ / мес"
    override val vibeProPlanYearlyDiscount: String = "−25%"
    override val vibeProPlanMonthly: String = "1 месяц"
    override val vibeProPlanMonthlyPrice: String = "199 ₽ / мес"
    override val vibeProSubscribeCta: (String) -> String = { price -> "Подключить Vibe Pro — $price" }
    override val vibeProAutoRenewalDisclaimer: String = "Подписка продлевается автоматически. Отменить можно в любое время."
    override val vibeProComingSoonToast: String = "Оформление подписки станет доступно в ближайшем обновлении"
    // Two-factor authentication (2FA)
    override val twoFactorTitle: String = "Двухэтапная аутентификация"
    override val twoFactorSubtitle: String = "Дополнительный пароль для защиты при входе"
    override val twoFactorDescription: String = "Вы можете задать дополнительный пароль, который потребуется вводить при входе с нового устройства в дополнение к коду подтверждения."
    override val twoFactorStatusEnabled: String = "Включена"
    override val twoFactorStatusDisabled: String = "Выключена"
    override val twoFactorEnabledBadge: String = "Защита активна"
    override val twoFactorEnabledDesc: String = "При входе на новом устройстве потребуется ввести этот пароль после кода подтверждения."
    override val twoFactorBullet1Title: String = "Надёжная защита"
    override val twoFactorBullet1Desc: String = "Даже если посторонний получит код подтверждения, он не сможет войти в ваш аккаунт"
    override val twoFactorBullet2Title: String = "Облачный пароль"
    override val twoFactorBullet2Desc: String = "Пароль надёжно зашифрован в защищённом хранилище"
    override val twoFactorBullet3Title: String = "Подсказка для памяти"
    override val twoFactorBullet3Desc: String = "Возможность указать подсказку, которая поможет вспомнить пароль"
    override val twoFactorSetPasswordBtn: String = "Задать пароль"
    override val twoFactorChangePasswordBtn: String = "Изменить пароль"
    override val twoFactorChangeHintBtn: String = "Изменить подсказку"
    override val twoFactorDisableBtn: String = "Отключить защиту"
    override val twoFactorEnterNewPasswordTitle: String = "Новый пароль"
    override val twoFactorEnterNewPasswordSubtitle: String = "Придумайте пароль длиной не менее 6 символов"
    override val twoFactorRepeatPasswordTitle: String = "Повторите пароль"
    override val twoFactorRepeatPasswordSubtitle: String = "Введите пароль ещё раз для подтверждения"
    override val twoFactorEnterCurrentPasswordTitle: String = "Текущий пароль"
    override val twoFactorEnterCurrentPasswordSubtitle: String = "Введите текущий пароль двухэтапной аутентификации"
    override val twoFactorHintTitle: String = "Подсказка для пароля"
    override val twoFactorHintSubtitle: String = "Подсказка поможет вспомнить пароль при необходимости"
    override val twoFactorHintPlaceholder: String = "Например: любимая книга или дата"
    override val twoFactorHintTooLong: String = "Подсказка не должна превышать 32 символов"
    override val twoFactorHintContainsPassword: String = "Подсказка не должна содержать сам пароль"
    override val twoFactorHintPublicWarning: String = "Подсказка видна любому, кто попытается войти в ваш аккаунт"
    override val twoFactorPasswordTooShort: String = "Пароль должен содержать минимум 6 символов"
    override val twoFactorPasswordMismatch: String = "Пароли не совпадают"
    override val twoFactorPasswordWrong: String = "Неверный текущий пароль"
    override val twoFactorDisableConfirmTitle: String = "Отключить двухэтапную защиту?"
    override val twoFactorDisableConfirmDesc: String = "Для входа на новых устройствах снова будет достаточно только кода подтверждения."
    override val twoFactorDisableAction: String = "Отключить"
    override val twoFactorNextBtn: String = "Далее"
    override val twoFactorSkipBtn: String = "Пропустить"
    override val twoFactorSaveBtn: String = "Сохранить"
    override val twoFactorStrengthWeak: String = "Слабый"
    override val twoFactorStrengthMedium: String = "Средний"
    override val twoFactorStrengthStrong: String = "Надёжный"
    override val twoFactorStrengthVeryStrong: String = "Отличный"
    override val twoFactorCurrentHintPill: (String) -> String = { hint -> "Подсказка: $hint" }
    override val twoFactorSuccessSetToast: String = "Двухэтапная аутентификация успешно включена"
    override val twoFactorSuccessChangedToast: String = "Пароль успешно изменён"
    override val twoFactorSuccessDisabledToast: String = "Двухэтапная аутентификация отключена"
    override val twoFactorPasswordFieldLabel: String = "Пароль"
    override val twoFactorConfirmFieldLabel: String = "Подтверждение пароля"
    override val twoFactorCurrentFieldLabel: String = "Текущий пароль"
    override val twoFactorHintFieldLabel: String = "Подсказка (необязательно)"
    override val settingsVibes: String = "Vibes"
    override val settingsVibesSubtitle: String = "Оформление и эффекты чатов"
    override val settingsGroupGeneral: String = "Основное"
    override val settingsGroupExtras: String = "Дополнительно"
    override val settingsGroupHelp: String = "Помощь"
    override val settingsSoonBadge: String = "Скоро"
    override val appVersion: (String) -> String = { version -> "Версия приложения $version" }
    // Onboarding controls
    override val onboardingGetStarted: String = "НАЧАТЬ"
    override val onboardingSkip: String = "Пропустить"
    override val a11yOnboardingPage: (Int, Int) -> String = { current, total -> "Экран $current из $total" }
    // Passcode
    override val passcodeEnterTitle: String = "Введите код-пароль"
    override val passcodeEnterSubtitle: String = "Четыре цифры для входа"
    override val passcodeEnterCurrentTitle: String = "Введите текущий код-пароль"
    override val passcodeCreateTitle: String = "Придумайте код-пароль"
    override val passcodeRepeatTitle: String = "Повторите код-пароль"
    override val passcodeInfoTitle: String = "Вход по коду"
    override val passcodeInfoText: String = "Код-пароль дополнительно защитит ваши данные. При открытии приложения потребуется ввести установленный код-пароль."
    override val passcodeEnableBtn: String = "Включить код-пароль"
    override val passcodeChangeBtn: String = "Изменить код-пароль"
    override val passcodeDisableBtn: String = "Отключить код-пароль"
    override val passcodeDisableShort: String = "Отключить"
    override val passcodeRemoveTitle: String = "Отключить код-пароль?"
    override val passcodeRemoveText: String = "Код-пароль будет удалён, приложение перестанет запрашивать его при запуске."
    override val passcodeWrongCode: String = "Неверный код-пароль"
    override val passcodeMismatch: String = "Код-пароли не совпадают"
    override val a11yPasscodeLock: String = "Защита кодом-паролем"
    override val a11yPasscodeBackspace: String = "Удалить цифру"
    override val a11yPasscodeDigit: (String) -> String = { digit -> "Цифра $digit" }
    // Nickname screen
    override val nicknameHint: String = "От 1 до 32 символов. Имя можно изменить позже в настройках."

    // Shared UI components (button, text field, OTP, toast, inline keyboard)
    override val a11yLoading: String = "Загрузка"
    override val a11yOtpInput: String = "Код подтверждения"
    override val a11yOtpDigit: (Int, Int) -> String = { position, total -> "Цифра $position из $total" }
    override val a11yOtpDigitEmpty: (Int, Int) -> String = { position, total -> "Цифра $position из $total, не введена" }
    override val a11yFieldError: (String) -> String = { error -> "Ошибка: $error" }
    override val a11yClearField: String = "Очистить поле"
    override val a11yInlineButtonLink: String = "Открывает внешнюю ссылку"
    override val a11yInlineButtonLoading: String = "Выполняется запрос"

    // Link confirmation dialog & inline formatting (a11y)
    override val linkDialogTitle: String = "Открыть ссылку?"
    override val linkDialogSubtitle: String = "Вы переходите на внешний сайт"
    override val linkDialogSecure: String = "Защищённое соединение"
    override val linkDialogInsecure: String = "Соединение без шифрования"
    override val linkDialogOpen: String = "Перейти"
    override val linkDialogCancel: String = "Отмена"
    override val a11yLinkChip: (String) -> String = { domain -> "Ссылка на $domain" }
    override val a11ySpoilerHidden: String = "Скрытый текст. Нажмите, чтобы показать"
    override val a11ySpoilerRevealed: String = "Скрытый текст показан"
    override val a11yQuote: String = "Цитата"
    override val formatInlineQuoteWrap: (String) -> String = { text -> "«$text»" }

    // Account settings screen
    override val accountTitle: String = "Аккаунт"
    override val accountSectionProfile: String = "Профиль"
    override val accountUsernameLabel: String = "Юзернейм"
    override val accountNicknameLabel: String = "Никнейм"
    override val accountNotSet: String = "Не задан"
    override val accountNoName: String = "Без имени"
    override val accountSectionAbout: String = "О себе"
    override val accountBioLabel: String = "Описание профиля"
    override val accountBioPlaceholder: String = "Напишите немного о себе…"
    override val accountPrivacyFootPrefix: String = "Кто увидит ваш статус «О себе» — настраивается в "
    override val accountPrivacyFootLink: String = "настройках приватности"
    override val accountPrivacyFootSuffix: String = "."
    override val accountLogoutTitle: String = "Выйти из аккаунта"
    override val accountLogoutSubtitle: String = "Локальные черновики и кэш будут удалены"
    override val accountLogoutDialogTitle: String = "Выйти из аккаунта?"
    override val accountLogoutDialogText: String = "Чтобы вернуться, потребуется войти снова."
    override val a11yEditProfile: String = "Изменить профиль"
    override val a11yCopy: String = "Скопировать"

    // Notification settings
    override val notifSectionGeneral: String = "Уведомления"
    override val notifSectionSound: String = "Звук"
    override val notifMuteAll: String = "Заглушить все чаты"
    override val notifMuteAllDesc: String = "Пуши не приходят ни от кого"
    override val notifAutoMute: String = "Автомут новых чатов"
    override val notifAutoMuteDesc: String = "Новые собеседники начинают без звука"
    override val notifAutoMuteFootnote: String = "Существующие чаты не меняются. Включить уведомления для конкретного чата можно в его меню."
    override val notifSoundTitle: String = "Звук уведомлений"
    override val notifSoundSilent: String = "Без звука"
    override val notifSoundDefault: String = "Системный"
    override val notifSoundCustom: String = "Выбранный звук"
    override val notifSoundFootnote: String = "Звук действует только на этом устройстве. Настройки Android и режим «Не беспокоить» имеют приоритет."
    override val notifNoServerResponse: String = "Нет ответа сервера. Проверьте соединение и попробуйте ещё раз."
    override val notifNoPicker: String = "На устройстве нет приложения для выбора звука."
    override val notifSyncing: String = "Синхронизация с сервером…"

    // Power saving settings
    override val powerSectionMode: String = "Режим"
    override val powerEnableNow: String = "Включить экономию сейчас"
    override val powerEnableNowDesc: String = "Эффекты отключаются сразу"
    override val powerAutoTitle: String = "Включать по уровню батареи"
    override val powerAutoDesc: String = "Автоматически при низком заряде"
    override val powerThreshold: String = "Порог включения"
    override val powerSectionDisable: String = "Что отключать"
    override val powerLiquid: String = "Жидкое стекло"
    override val powerBlur: String = "Размытие панелей"
    override val powerGlow: String = "Фоновое сияние"
    override val powerPreviews: String = "Анимация GIF и стикеров"
    override val powerFootnote: String = "При заряде выше порога эффекты возвращаются, если экономия не включена вручную."

    // Language settings
    override val languageSearch: String = "Поиск языка"
    override val languageSectionTitle: String = "Язык интерфейса"
    override val languageNotFound: String = "Язык не найден"
    override val languageFootnote: String = "Язык применяется сразу ко всему приложению."

    // Two-factor: server flow and sign-in challenge
    override val twoFactorCheckingServer: String = "Проверяем состояние на сервере…"
    override val twoFactorNoServerResponse: String = "Нет ответа сервера. Настройки не подтверждены."
    override val twoFactorDisconnected: String = "Нет соединения с сервером"
    override val twoFactorPasswordTooLong: String = "Пароль слишком длинный (максимум 72 байта)"
    override val twoFactorChallengeTitle: String = "Двухфакторная защита"
    override val twoFactorChallengeSubtitle: String = "Введите пароль, который вы задали в настройках безопасности"
    override val twoFactorShowHint: String = "Показать подсказку"
    override val twoFactorYourHint: String = "Ваша подсказка"
    override val twoFactorForgotPassword: String = "Забыли пароль?"
    override val twoFactorSignInBtn: String = "Войти"
    override val twoFactorAttemptsLeft: (Int) -> String = { n -> "осталось попыток: $n" }
    override val twoFactorForgotUnavailable: String = "Сброс второго фактора по одному коду подтверждения недоступен. Обратитесь в поддержку."
    override val a11yShowPassword: String = "Показать пароль"
    override val a11yHidePassword: String = "Скрыть пароль"

    // Two-factor: резервная почта и сброс пароля
    override val twoFactorRecoveryEmailBtn: String = "Резервная почта"
    override val twoFactorRecoveryEmailNotSet: String = "Не задана"
    override val twoFactorRecoveryEmailFootnote: String =
        "На резервную почту придёт код, если вы забудете пароль второго фактора. Укажите адрес, отличный от почты аккаунта."
    override val twoFactorRecoveryEmailTitle: String = "Резервная почта"
    override val twoFactorRecoveryEmailSubtitle: String =
        "Введите адрес — мы отправим на него код подтверждения."
    override val twoFactorRecoveryEmailCurrent: (String) -> String = { email -> "Текущий адрес: $email" }
    override val twoFactorRecoveryEmailFieldLabel: String = "Резервный e-mail"
    override val twoFactorRecoveryEmailInvalid: String = "Неверный формат адреса"
    override val twoFactorRecoveryEmailSendCodeBtn: String = "Отправить код"
    override val twoFactorRecoveryEmailRemoveBtn: String = "Удалить резервную почту"
    override val twoFactorRecoveryCodeTitle: String = "Подтвердите адрес"
    override val twoFactorRecoveryCodeSubtitle: (String) -> String = { masked -> "Код подтверждения отправлен на $masked" }
    override val twoFactorRecoveryConfirmBtn: String = "Подтвердить"
    override val twoFactorResendCodeBtn: String = "Отправить код снова"
    override val twoFactorRecoveryEmailSavedToast: String = "Резервная почта сохранена"
    override val twoFactorRecoveryEmailRemovedToast: String = "Резервная почта удалена"
    override val twoFactorResetBtn: String = "Сбросить пароль"
    override val twoFactorResetTitle: String = "Сброс пароля"
    override val twoFactorResetSubtitle: (String) -> String = { masked -> "Код для сброса отправлен на $masked" }
    override val twoFactorResetConfirmBtn: String = "Сбросить и войти"
    override val twoFactorResetSettingsConfirmBtn: String = "Сбросить пароль"
    override val twoFactorResetWarning: String =
        "После сброса второй фактор будет отключён. Включите его снова и задайте новый пароль."
    override val twoFactorResetNoEmail: String =
        "Резервная почта не задана, поэтому сбросить пароль нельзя. Обратитесь в поддержку."
    override val twoFactorResetFailed: String = "Не удалось сбросить пароль. Попробуйте позже."
    override val twoFactorResetDoneToast: String = "Второй фактор отключён"

    // Emoji / sticker / GIF panel
    override val gifSearchPlaceholder: String = "Поиск GIF"
    override val gifNotFound: String = "GIF не найдены"
    override val gifLoadFailed: String = "Не удалось загрузить GIF"
    override val gifSendCd: String = "Отправить GIF"
    override val panelTabEmoji: String = "Эмодзи"
    override val panelTabStickers: String = "Стикеры"
    override val panelTabGifs: String = "GIF"
    override val panelRecent: String = "Недавние"

    // Passcode settings (status page in the 2FA style)
    override val passcodeStatusEnabledBadge: String = "Код-пароль включён"
    override val passcodeStatusEnabledDesc: String = "При каждом запуске приложение просит 4-значный код. Без него чаты не открыть."
    override val passcodeBullet1Title: String = "Защита при запуске"
    override val passcodeBullet1Desc: String = "Код запрашивается каждый раз, когда вы открываете Vibe"
    override val passcodeBullet2Title: String = "Хранится только на устройстве"
    override val passcodeBullet2Desc: String = "Код зашифрован и никогда не отправляется на сервер"
    override val passcodeBullet3Title: String = "Не заменяет 2FA"
    override val passcodeBullet3Desc: String = "Вход с других устройств защищает двухфакторная защита"
    override val passcodeStepCurrentSubtitle: String = "Подтвердите, что это вы"
    override val passcodeStepNewSubtitle: String = "Выберите 4 цифры, которые легко запомнить"
    override val passcodeStepRepeatSubtitle: String = "Введите код ещё раз, чтобы не ошибиться"
    override val passcodeSavedToast: String = "Код-пароль сохранён"
    override val passcodeRemovedToast: String = "Код-пароль отключён"

    // Power saving threshold control
    override val powerThresholdHint: (Int) -> String = { n -> "Эффекты отключатся, когда заряд опустится до $n%" }
    override val powerThresholdPresetCd: (Int) -> String = { n -> "Порог $n%" }

// Two-factor edit screen
    override val twoFactorCurrentPasswordLabel: String = "Текущий пароль"
    override val twoFactorNewPasswordLabel: String = "Новый пароль"
    override val twoFactorRepeatPasswordLabel: String = "Повторите пароль"
    override val twoFactorHintNoPassword: String = "Подсказка не должна содержать сам пароль"
    override val twoFactorHintTooLongShort: String = "Слишком длинная"
    override val twoFactorHintPublicDesc: String = "Увидит любой, кто попытается войти"
    override val twoFactorDisableButton: String = "Отключить двухфакторную защиту"
    override val twoFactorAdditionalPasswordTitle: String = "Дополнительный пароль"
    override val twoFactorAdditionalPasswordDesc: String = "После ввода кода из письма потребуется этот пароль. Даже если кто-то получит доступ к вашей почте, войти он не сможет."
    override val passwordStrengthWeak: String = "Слабый"
    override val passwordStrengthMedium: String = "Средний"
    override val passwordStrengthGood: String = "Хороший"
    override val passwordStrengthStrong: String = "Отличный"
    override val twoFactorDisableDialogTitle: String = "Отключить защиту?"
    override val twoFactorDisableDialogDesc: String = "Для входа снова будет достаточно только кода из письма."

    // Privacy option screen
    override val privacyValueEverybody: String = "Все"
    override val privacyValueNobody: String = "Никто"
    override val privacyValueSelected: String = "Выбранные"
    override val privacySelectUsers: String = "Выбрать пользователей"
    override val privacySelectUsersRuleDesc: String = "Выберите пользователей, к которым будет применяться это правило."

    // Inline video player & media covers
    override val videoPlaybackFailed: String = "Не удалось воспроизвести видео"
    override val videoScaleCd: String = "Масштаб видео"
    override val videoCoverCd: String = "Обложка видео"
    override val videoNoFrameCd: String = "Видео без доступного кадра"
    override val videoPlayCd: String = "Воспроизвести видео"
    override val sampleText: String = "Пример текста"
    override val a11yMuteSound: String = "Выключить звук"
    override val a11yUnmuteSound: String = "Включить звук"

    // Circle recording errors
    override val circleCameraInitFailed: String = "Не удалось инициализировать камеру"
    override val circleNoCamera: String = "На устройстве нет доступной камеры"
    override val circleCameraUnavailable: (String) -> String = { msg -> "Камера недоступна: $msg" }
    override val circleCameraNotReady: String = "Камера ещё не готова"
    override val circleRecordStartFailedMsg: (String) -> String = { msg -> "Не удалось начать запись: $msg" }
    override val circleRecordFailedMsg: (Int) -> String = { code -> "Не удалось записать: код $code" }
    override val circleEmptyRecord: String = "Пустая запись"

    // Emoji categories
    override val emojiCategorySmileys: String = "Смайлики"
    override val emojiCategoryGestures: String = "Жесты и люди"
    override val emojiCategoryHearts: String = "Сердца и символы"
    override val emojiCategoryAnimals: String = "Животные и природа"
    override val emojiCategoryFood: String = "Еда и напитки"
    override val emojiCategoryActivities: String = "Активности"
    override val emojiCategoryTravel: String = "Путешествия"
    override val emojiCategoryObjects: String = "Объекты"

    // Download helper & notifications
    override val errorGeneric: String = "Произошла ошибка"
    override val downloadFileDescription: String = "Скачивание файла из Vibe"
    override val downloadStartedToast: String = "Скачивание началось..."
    override val downloadErrorToast: (String) -> String = { err -> "Ошибка скачивания: $err" }
    override val downloadMultipleToast: (Int) -> String = { count -> "Скачивание: $count файл(ов)" }
    override val notifMe: String = "Я"
    override val unitSecondShort: String = "с"

    override val locale: String = "ru"
}

object EnStrings : VibeStrings {
    override val createAccount: String = "Create Account"
    override val welcomeBack: String = "Welcome Back"
    override val emailLabel: String = "EMAIL"
    override val usernameLabel: String = "USERNAME"
    override val emailInvalidFormat: String = "INVALID FORMAT"
    override val continueBtn: String = "CONTINUE"
    override val verificationTitle: String = "Enter Code"
    override val verificationSubtitle: (String) -> String = { email -> "We sent a 6-digit code to\n$email" }
    override val verifyBtn: String = "VERIFY"
    override val verifyLoading: String = "VERIFYING..."
    override val codeInvalid: String = "Invalid code"
    override val verificationSubtitleApp: String = "Code sent to the Vibe app on another authorized device.\nOpen the chat with Vibe cat to see it"
    override val nicknameTitle: String = "What's your name?"
    override val nicknameLabel: String = "YOUR NAME"
    override val saveBtn: String = "CONTINUE"
    override val saveLoading: String = "SAVING..."
    override val errorSaving: String = "Error saving"
    override val tabChats: String = "Chats"
    override val tabSettings: String = "Settings"
    override val tabProfile: String = "Profile"
    override val profileTitle: String = "Profile"
    override val deletedAcc: String = "Deleted account"
    override val statusOnline: String = "Online"
    override val statusBot: String = "Bot"
    override val statusUnknown: String = "Unknown"
    override val actionForward: String = "Forward"
    override val badgeVerified: String = "Verified by Vibe Team."
    override val badgeDeveloper: String = "Vibe Developer Team member."
    override val badgeBot: String = "Just a bot."
    override val freezedAcc: String = "Account frozen for violating the rules."
    override val bannedAcc: String = "Account banned for violating the rules."
    override val aboutLabel: String = "About"
    override val registerDateLabel: String = "Joined"
    override val btnTheme: String = "Theme"
    override val themeDark: String = "Dark"
    override val themeLight: String = "Light"
    override val btnLanguage: String = "Language"
    override val btnLogout: String = "Log Out"
    override val logoutConfirmTitle: String = "Log Out"
    override val logoutConfirmText: String = "Are you sure you want to log out?"
    override val logoutCancel: String = "Cancel"
    override val logoutConfirm: String = "Log Out"
    override val userLabel: String = "User"
    override val chatsTitle: String = "Chats"
    override val chatsEmptyTitle: String = "No chats"
    override val chatsEmptySubtitle: String = "Start a conversation!"
    override val searchPlaceholder: String = "Search..."
    override val globalSearchResults: String = "Global Search Results"
    override val typing: String = "Typing"
    override val connecting: String = "Connecting"
    override val waitingForNetwork: String = "Waiting for network"
    override val monthsShort: List<String> = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    override val dateToday: String = "Today"
    override val dateYesterday: String = "Yesterday"
    override val lastSeenRecently: String = "Last seen recently"
    override val lastSeenLongAgo: String = "Last seen a long time ago"
    override val lastSeenInWeek: String = "Last seen this week"
    override val lastSeenInMonth: String = "Last seen this month"
    override val lastSeenToday: (String) -> String = { time -> "Last seen today at $time" }
    override val lastSeenYesterday: (String) -> String = { time -> "Last seen yesterday at $time" }
    override val lastSeenDate: (String, String) -> String = { date, time -> "Last seen $date at $time" }
    override val backBtn: String = "Back"
    override val draftLabel: String = "Draft: "
    override val messagePlaceholder: String = "Message..."
    override val replyTo: String = "Reply"
    override val emptyChat: String = "It's empty here..."
    override val chatHistoryCleared: String = "History cleared"
    override val chatHistoryEmpty: String = "History is empty"
    override val onboardingPages: List<Pair<String, String>> = listOf(
        "Data Shield" to "Your communication is locked with military-grade encryption.",
        "Light Speed" to "Next-gen protocols for instant message propagation.",
        "Universal Sync" to "Your entire data history available on every device.",
        "Global Hubs" to "Scale communities to millions of active nodes.",
        "Unique Vibe" to "Tailor every aspect of your messaging experience."
    )
    override val selectedMessagesCount: (Int) -> String = { count -> "Selected: $count" }
    override val deleteMessagesTitle: String = "Delete messages?"
    override val deleteMessagesText: (Int) -> String = { count -> "You are about to delete $count messages." }
    override val deleteForEveryone: (String) -> String = { name -> "Also delete for $name" }
    override val deleteForEveryoneAlsoMine: (String) -> String = { name -> "Also delete my messages for $name" }
    override val deleteBtn: String = "Delete"
    override val cancelBtn: String = "Cancel"
    override val forwardMessageTitle: String = "Forward message"
    override val noRecentChats: String = "No recent chats"
    override val editMessageTitle: String = "Edit message"
    override val sendBtn: String = "Send"
    override val forwardedFrom: (String) -> String = { name -> "Forwarded from $name" }
    override val replyDefault: String = "Reply"
    override val pinnedMessage: String = "Pinned message"
    override val pinnedMessages: String = "Pinned messages"
    override val pinMessage: String = "Pin message"
    override val pinMessageConfirm: String = "Are you sure you want to pin this message?"
    override val unpinMessage: String = "Unpin message"
    override val unpinMessageConfirm: String = "Are you sure you want to unpin this message?"
    override val unpinAll: String = "Unpin all"
    override val unpinAllConfirm: String = "Unpin all messages in this chat?"
    override val forBoth: (String) -> String = { name -> "Also for $name" }
    override val pin: String = "Pin"
    override val unpin: String = "Unpin"
    override val you: String = "You"
    override val edit: String = "Edit"
    override val usernameTaken: String = "This username is already taken"
    override val usernameAvailable: String = "Username is available"
    override val usernameDescription: String = "You can choose a unique username for Vibe. Other people will be able to find you by this name and contact you without knowing your phone number."
    override val usernameMinLength: String = "Minimum 4 characters"
    override val nicknameDescription: String = "Your display name visible to all users. Try to choose a recognizable name so friends can easily find you."
    override val bioDescription: String = "Write a bit about yourself. This information will be visible to other users on your profile."
    override val doneBtn: String = "Done"
    override val userRestrictedMessaging: String = "This user has restricted who can message them"
    override val userHiddenAccount: String = "User has hidden their account"
    override val editedLabel: String = " (edited)"
    override val muteNotifications: String = "Mute notifications"
    override val unmuteNotifications: String = "Unmute notifications"
    override val pinnedMessageSystemText: (String, String) -> String = { sender, content -> "$sender pinned a message: \"$content\"" }
    override val privacyScreenTitle: String = "Privacy"
    override val privacyTwoFactor: String = "Two-factor authentication"
    override val privacyPasscodeLogin: String = "Passcode login"
    override val privacyBlocked: String = "Blocked"
    override val privacyActivityTitle: String = "Activity status"
    override val privacyActivityDesc: String = "Who can see when you were last online. If you hide your activity status, you won't be able to see other users' status (approximate time will be shown)."
    override val privacyAvatarTitle: String = "Avatar"
    override val privacyAvatarDesc: String = "Who can see your avatar. Others will see the first letter of your name on a blue background."
    override val privacyForwardedTitle: String = "Forwarded messages"
    override val privacyForwardedDesc: String = "Who can navigate to your profile from forwarded messages."
    override val privacyMessagesTitle: String = "Messages"
    override val privacyMessagesDesc: String = "Who can send you messages. Users who are restricted will see a notice that you've limited who can contact you."
    override val privacyStatusTitle: String = "Status"
    override val privacyStatusDesc: String = "Who can see the 'About' section in your profile."

    // Report dialog
    override val reportTitle: String = "Report"
    override val reportSubtitle: String = "Pick a reason. Reports are anonymous, the author won't see it."
    override val reportStepLabel: (Int, Int) -> String = { current, total -> "Step $current of $total" }
    override val reportReasonSpam: String = "Spam"
    override val reportReasonSpamDesc: String = "Ads, mass messaging, engagement farming"
    override val reportReasonFraud: String = "Fraud"
    override val reportReasonFraudDesc: String = "Scams, phishing, money schemes"
    override val reportReasonDrugs: String = "Drugs"
    override val reportReasonDrugsDesc: String = "Selling or promoting illegal substances"
    override val reportReasonWeapons: String = "Weapons"
    override val reportReasonWeaponsDesc: String = "Trading weapons or explosives"
    override val reportReasonPorn: String = "Pornography"
    override val reportReasonPornDesc: String = "Adult content shared without warning"
    override val reportReasonCsam: String = "Child sexual abuse material (CSAM)"
    override val reportReasonCsamDesc: String = "Sexualised content involving minors"
    override val reportReasonViolence: String = "Violence"
    override val reportReasonViolenceDesc: String = "Threats, cruelty, calls to harm"
    override val reportReasonHarassment: String = "Harassment"
    override val reportReasonHarassmentDesc: String = "Insults, stalking, blackmail"
    override val reportReasonHate: String = "Hate speech"
    override val reportReasonHateDesc: String = "Attacks based on race, religion, gender"
    override val reportReasonFakeAccount: String = "Fake account"
    override val reportReasonFakeAccountDesc: String = "Impersonating someone else"
    override val reportReasonMisinfo: String = "False information"
    override val reportReasonMisinfoDesc: String = "Dangerous rumours and disinformation"
    override val reportCriticalNotice: String = "Reports like this get priority review and are escalated to the proper authorities."
    override val reportDetailsTitle: String = "Tell us more"
    override val reportDetailsHint: String = "Describe what happened and where. It helps us act faster."
    override val reportCommentPlaceholder: String = "Comment (optional)"
    override val reportCommentCounter: (Int, Int) -> String = { used, limit -> "$used / $limit" }
    override val reportChangeReason: String = "Change reason"
    override val reportSubmitBtn: String = "Send report"
    override val reportCancelBtn: String = "Cancel"
    override val reportSentTitle: String = "Report sent"
    override val reportSentDesc: String = "Thanks. Our moderators will review it and decide what to do."
    override val reportDoneBtn: String = "Done"
    override val a11yReportClose: String = "Close"

    // Privacy exceptions picker
    override val privacyExceptionsTitle: String = "Exceptions"
    override val privacyExceptionsHint: String = "Pick who the rule does not apply to"
    override val privacyExceptionsSelected: (Int) -> String = { count -> "$count selected" }
    override val privacyExceptionsSelectAll: String = "Select all"
    override val privacyExceptionsClearAll: String = "Clear selection"
    override val privacyExceptionsEmptyTitle: String = "Nobody to pick"
    override val privacyExceptionsEmptyDesc: String = "People you have chats with will show up here."
    override val a11yExceptionToggle: (String) -> String = { name -> "Select $name" }
    override val a11yExceptionRemove: (String) -> String = { name -> "Remove $name from selection" }
    override val settingsPrivacy: String = "Privacy"
    override val settingsAccount: String = "Account"
    override val settingsDevices: String = "Devices"
    override val settingsPasscode: String = "Passcode"
    override val usernameCopied: String = "Username copied"
    override val addAvatar: String = "Add avatar"
    override val choosePhoto: String = "Choose photo"
    override val switchSignIn: String = "Already have an account? Sign In"
    override val switchSignUp: String = "New here? Create Account"
    override val authTabSignUp: String = "Sign Up"
    override val authTabSignIn: String = "Sign In"
    override val authChecking: String = "Checking..."
    override val authEmailAvailable: String = "Email is available"
    override val authEmailTaken: String = "EMAIL ALREADY TAKEN"
    override val authUsernameTakenShort: String = "USERNAME ALREADY TAKEN"
    override val authUsernameAvailable: (String) -> String = { username -> "@$username is available" }
    override val authRegisterFailed: String = "Could not create account"
    override val authLoginFailed: String = "Could not sign in"
    override val authUsernameCounter: (Int, Int) -> String = { current, max -> "$current/$max" }
    override val languageName: String = "English"
    override val blockedTitle: String = "Blocked"
    override val blockedSearchPlaceholder: String = "Search users..."
    override val blockedClearSearch: String = "Clear search"
    override val blockedEmptyTitle: String = "No blocked users"
    override val blockedEmptyDesc: String = "Users you block will appear here."
    override val blockedSearchEmptyTitle: String = "Nothing found"
    override val blockedSearchEmptyDesc: (String) -> String = { query -> "No users match \"$query\"" }
    override val blockedUnblockBtn: String = "Unblock"
    override val blockedUnblockConfirmTitle: String = "Unblock user?"
    override val blockedUnblockConfirmText: (String) -> String = { name -> "$name will be able to message you and see your profile again." }
    override val blockedUnblockedToast: String = "User unblocked"
    override val blockedUserFallback: (Int) -> String = { id -> "User #$id" }
    override val accountDeleted: String = "Deleted account"
    override val accountFrozen: String = "Frozen account"
    override val accountBannedMessage: String = "Your account has been banned for violating the rules."
    override val accountFrozenMessage: String = "Your account has been frozen by a moderator."
    override val typeVideo: String = "Video"
    override val typeVideoMessage: String = "Video message"
    override val typeAudio: String = "Audio file"
    override val typeFile: String = "File"
    override val typeVoice: String = "Voice message"
    override val playerPlaylist: String = "Chat playlist"
    override val playerSearchTracks: String = "Search tracks"
    override val playerNowPlaying: String = "Now playing"
    override val playerTrackFallback: String = "Audio"
    override val playerTracksCount: (Int) -> String = { count ->
        if (count == 1) "1 track" else "$count tracks"
    }
    override val playerQueueEmpty: String = "Queue is empty"
    override val playerQueueEmptyHint: String = "Audio from this chat will show up here"
    override val playerSearchEmptyTitle: String = "Nothing found"
    override val playerSearchEmptySubtitle: (String) -> String = { query -> "No tracks match \"$query\"" }
    override val playerBuffering: String = "Buffering…"
    override val playerTimeZero: String = "0:00"
    override val playerTimeUnknown: String = "--:--"
    override val playerSpeedFormat: (String) -> String = { value -> "$value×" }
    override val playerRepeatOff: String = "Repeat off"
    override val playerRepeatAll: String = "Repeat playlist"
    override val playerRepeatOne: String = "Repeat track"
    override val a11yPlayerPlay: String = "Play"
    override val a11yPlayerPause: String = "Pause"
    override val a11yPlayerNext: String = "Next track"
    override val a11yPlayerPrevious: String = "Previous track"
    override val a11yPlayerRewind10: String = "Rewind 10 seconds"
    override val a11yPlayerForward10: String = "Forward 10 seconds"
    override val a11yPlayerShuffle: String = "Shuffle"
    override val a11yPlayerClose: String = "Close player"
    override val a11yPlayerExpand: String = "Expand player"
    override val a11yPlayerCollapse: String = "Collapse player"
    override val a11yPlayerSearch: String = "Search the playlist"
    override val a11yPlayerSearchClose: String = "Close search"
    override val a11yPlayerClearSearch: String = "Clear search"
    override val a11yPlayerArtwork: String = "Track artwork"
    override val a11yPlayerSpeed: (String) -> String = { value -> "Playback speed: $value" }
    override val a11yPlayerTrackRow: (String) -> String = { title -> "Play track $title" }
    override val sectionPhotosVideos: String = "Photos and videos"
    override val sectionFiles: String = "Files"
    override val sectionMusic: String = "Music"
    override val sectionVoice: String = "Voice messages"
    override val profileSearchFiles: String = "Search files"
    override val actionDownload: String = "Download"
    override val actionDownloadSelected: String = "Download selected"
    override val formatCopy: String = "Copy"
    override val formatCut: String = "Cut"
    override val formatFormat: String = "Format"
    override val formatBold: String = "Bold"
    override val formatItalic: String = "Italic"
    override val formatBoldItalic: String = "Bold Italic"
    override val formatStrikethrough: String = "Strikethrough"
    override val formatUnderline: String = "Underline"
    override val formatMonospace: String = "Monospace"
    override val formatLink: String = "Link"
    override val formatTextColor: String = "Text Color"
    override val formatSpoiler: String = "Spoiler"
    override val formatQuote: String = "Quote"
    override val formatLinkUrlHint: String = "Enter URL"
    override val formatColorHint: String = "Enter HEX (e.g. #FF5733)"
    override val formatPreview: String = "Preview"
    override val formatCopied: String = "Copied"
    override val formatReadMore: String = "Read more"
    override val formatCollapse: String = "Collapse"
    override val attachPhotoVideo: String = "Photos & Videos"
    override val attachFile: String = "File"
    override val attachTitle: String = "Attachments"
    override val photoViewer: String = "Photo viewer"
    override val photoOf: (Int, Int) -> String = { current, total -> "$current of $total" }
    override val actionCopy: String = "Copy text"
    override val actionReport: String = "Report"


    // Chat header
    override val chatSearchPlaceholder: String = "Search messages…"
    override val chatSearchNoResults: String = "No matches"
    override val chatSearchCounter: (Int, Int) -> String = { current, total -> "$current of $total" }
    override val chatSearchClose: String = "Close search"
    override val chatSearchClear: String = "Clear query"
    override val chatSearchByDate: String = "Search by date"
    override val chatSearchNext: String = "Next result"
    override val chatSearchPrev: String = "Previous result"
    override val chatActionSearch: String = "Search"
    override val chatMenu: String = "Chat menu"
    override val chatAvatar: String = "Avatar"
    override val chatOpenProfile: String = "Open profile"
    override val chatClearSelection: String = "Clear selection"
    override val chatStatusBlockedByMe: String = "Blocked"
    override val chatBlockUser: String = "Block user"
    override val chatUnblockUser: String = "Unblock user"
    override val chatUserBlockedToast: String = "User blocked"
    override val chatPinnedCounter: (Int, Int) -> String = { current, total -> "$current/$total" }
    override val chatJumpToPinned: String = "Jump to pinned message"
    override val chatUnpinAllHint: String = "Unpin all"


    // Chat input bar
    override val inputAttachMedia: String = "Attach"
    override val inputAttachGallery: String = "Photo or Video"
    override val inputAttachFile: String = "File"
    override val inputEmojiPanel: String = "Emoji, stickers and GIFs"
    override val inputSelectedMedia: (Int) -> String = { count -> "Media selected: $count" }
    override val inputClearAttachments: String = "Clear attachments"
    override val inputBlockedByMe: String = "You blocked this user"
    override val inputAttachmentPreview: String = "Attachment"
    override val inputCancelReply: String = "Cancel"
    override val voiceHoldToRecord: String = "Hold to record"
    override val voiceSendRecording: String = "Send voice message"
    override val voiceCancelRecording: String = "Cancel recording"
    override val voiceLocked: String = "Recording locked"
    override val voiceSlideToCancel: String = "Slide left to cancel"
    override val voiceSlideToLock: String = "Up to lock"
    override val voiceRecordStartFailed: String = "Could not start voice recording"
    override val voicePermissionRequired: String = "Microphone access is required to record"
    override val voiceTooShort: String = "The recording was too short"

    // Circles (video messages)
    override val circleModeSwitchedOn: String = "Circle mode: hold to record"
    override val circleModeSwitchedOff: String = "Voice message mode"
    override val circleRecordVideoMessage: String = "Record a video message"
    override val circleHoldOrTapHint: String = "Tap to record, hold for a quick take"
    override val circleRecordingHint: String = "Left to cancel · up to lock"
    override val circleReleaseToCancel: String = "Release to cancel"
    override val circleLockedHint: String = "Tap to send"
    override val circleTapToStop: String = "Tap to stop and send"
    override val circleCameraPreparing: String = "Preparing the camera…"
    override val circleMaxDurationHint: String = "60 seconds max"
    override val circlePermissionRequired: String = "Circles need camera and microphone access"
    override val circleTooShort: String = "The circle was too short"
    override val circleCancel: String = "Cancel recording"
    override val circleSend: String = "Send circle"
    override val circleSwitchCamera: String = "Switch camera"
    override val circleSendFailed: String = "Could not send the circle"


    // Chat list (iOS redesign)
    override val chatsSectionPinned: String = "Pinned"
    override val filterAll: String = "All"
    override val filterUnread: String = "Unread"
    override val filterUnreadCount: (Int) -> String = { count -> "Unread ($count)" }
    override val chatsNoUnreadTitle: String = "All Caught Up"
    override val chatsNoUnreadSubtitle: String = "You have no unread messages."
    override val chatsSectionAll: String = "All Chats"
    override val chatsCountFooter: (Int) -> String = { count -> "$count chats" }
    override val chatsEmptyHint: String = "Find someone using the search above"
    override val chatsSearchCancel: String = "Cancel"
    override val chatsSearchClearField: String = "Clear search field"
    override val chatsSearchNoResultsTitle: String = "No Results"
    override val chatsSearchNoResultsSubtitle: (String) -> String = { query -> "No chats or users match \"$query\"" }
    override val userFallback: (Int) -> String = { id -> "User #$id" }
    override val someoneLabel: String = "Someone"
    override val a11yMutedChat: String = "Notifications muted"
    override val a11yPinnedChat: String = "Chat pinned"
    override val actionMuteShort: String = "Mute"
    override val actionUnmuteShort: String = "Unmute"


    // Chat list message previews
    override val typePhoto: String = "Photo"
    override val previewVoiceMessage: (String) -> String = { duration -> "Voice message $duration" }
    override val previewVideoMessage: (String) -> String = { duration -> "Video message $duration" }
    override val previewAudioTrack: (String, String) -> String = { artist, title -> "$artist — $title" }
    override val previewAudioLoading: String = "Music..."
    override val previewMorePhotos: (Int) -> String = { count -> "+$count photos" }
    override val previewMoreVideos: (Int) -> String = { count -> "+$count videos" }
    override val previewMoreWithCaption: (Int, String) -> String = { count, caption -> "+$count $caption" }
    override val typeSticker: String = "Sticker"
    override val typeGif: String = "GIF"
    override val previewMediaCount: (Int) -> String = { count ->
        if (count == 1) "1 media file" else "$count media files"
    }
    override val previewMoreAudio: (Int) -> String = { count ->
        if (count == 1) "1 audio file" else "$count audio files"
    }
    override val previewMoreFiles: (Int) -> String = { count ->
        if (count == 1) "1 file" else "$count files"
    }
    override val previewMoreAttachments: (Int) -> String = { count ->
        if (count == 1) "1 attachment" else "$count attachments"
    }
    override val actionSelectMessage: String = "Select"
    // Chat message list (empty state, system messages, scroll-to-bottom)
    override val emptyChatSubtitle: String = "Send the first message and your chat history will show up here."
    override val emptyChatHint: String = "Be the first to say hi"
    override val chatSystemMessageLabel: String = "System message"
    override val linkOpenFailed: String = "Couldn't open the link"
    override val a11yMessageList: String = "Message list"
    override val a11yScrollToBottom: String = "Jump to the latest messages"
    override val a11yUnreadCount: (Int) -> String = { count -> "New messages: $count" }
    override val unreadCountOverflow: String = "99+"
    // Chat dialogs, reactions sheet, bot & network toasts
    override val okBtn: String = "OK"
    override val actionClose: String = "Close"
    override val reportSentToast: String = "Report sent"
    override val restrictionTitle: String = "Restriction"
    override val restrictionUnderstood: String = "Got it"
    override val restrictionWhy: String = "Why?"
    override val datePickerTitle: String = "Select a date"
    override val dateJumpNotFound: String = "No messages found for this date"
    override val botMessageTitle: String = "Message from a bot"
    override val botLabel: String = "Bot"
    override val attachmentLabel: String = "Attachment"
    override val fileSizeLoading: String = "Loading..."
    override val reactionsTitle: String = "Reactions"
    override val reactionsAllTab: (Int) -> String = { count -> "All $count" }
    override val reactionsEmpty: String = "No reactions yet"
    override val voiceTrackTitleMine: String = "You (Voice message)"
    override val botCallbackTimeout: String = "The bot didn't respond within 10 seconds. This message's buttons are available again."
    override val connectionLostToast: String = "No connection to the server. Please try again."
    override val fileOpenFailed: String = "Couldn't open the selected file"
    override val maxPinnedChatsToast: (Int) -> String = { limit -> "You can pin up to $limit chats" }
    // Chat toast host
    override val toastTitleInfo: String = "Info"
    override val toastTitleSuccess: String = "Done"
    override val toastTitleWarning: String = "Heads up"
    override val toastTitleError: String = "Error"
    override val toastActionRetry: String = "Retry"
    override val toastActionUndo: String = "Undo"
    override val toastCopied: String = "Copied"
    override val a11yToast: (String) -> String = { text -> "Notification: $text" }
    override val a11yToastDismiss: String = "Dismiss notification"
    // Devices & sessions
    override val devicesTitle: String = "Devices"
    override val devicesSubtitle: String = "Everywhere you're signed in to your account"
    override val devicesSessionsCount: (Int) -> String = { count ->
        if (count == 1) "1 active session" else "$count active sessions"
    }
    override val devicesRefreshCd: String = "Refresh the list"
    override val devicesSectionCurrent: String = "THIS DEVICE"
    override val devicesSectionOther: String = "OTHER SESSIONS"
    override val devicesCurrentBadge: String = "Current"
    override val devicesOnlineNow: String = "Online"
    override val devicesLastActiveNow: String = "Just now"
    override val devicesLastActiveMinutes: (Int) -> String = { minutes ->
        if (minutes == 1) "1 minute ago" else "$minutes minutes ago"
    }
    override val devicesLastActiveHours: (Int) -> String = { hours ->
        if (hours == 1) "1 hour ago" else "$hours hours ago"
    }
    override val devicesLastActiveYesterday: String = "Yesterday"
    override val devicesLastActiveDate: (String) -> String = { date -> "Last active: $date" }
    override val devicesDateTimePattern: String = "MMM d, yyyy, HH:mm"
    override val devicesUnknownDevice: String = "Unknown device"
    override val devicesUnknownLocation: String = "Location unknown"
    override val devicesNoOtherSessions: String = "No other sessions"
    override val devicesNoOtherSessionsHint: String = "This is the only device signed in to your account"
    override val devicesEmptyTitle: String = "No active sessions"
    override val devicesEmptySubtitle: String = "We couldn't find any sign-ins. Try refreshing the list."
    override val devicesLoading: String = "Loading sessions..."
    override val devicesLoadFailedTitle: String = "Couldn't load"
    override val devicesLoadFailedSubtitle: String = "The server didn't respond. Check your connection and try again."
    override val devicesTerminateCd: String = "End session"
    override val devicesTerminateAll: String = "End all"
    override val devicesTerminateTitle: String = "End this session?"
    override val devicesTerminateText: (String) -> String = { name ->
        "\"$name\" will be signed out. Signing back in will require a verification code."
    }
    override val devicesTerminateAllTitle: String = "End all sessions?"
    override val devicesTerminateAllText: (Int) -> String = { count ->
        "All other devices ($count) will be signed out. This device stays online."
    }
    override val devicesTerminateConfirm: String = "End session"
    override val devicesSecurityHint: String = "Don't recognize a device? End its session and change your password."
    // Edit profile field
    override val editFieldSave: String = "Save"
    override val editFieldSaveCd: String = "Save changes"
    override val editFieldPlaceholder: (String) -> String = { title -> "Enter ${title.lowercase()}" }
    override val editFieldClearCd: String = "Clear the field"
    override val editFieldCounter: (Int, Int) -> String = { used, max -> "$used / $max" }
    override val editFieldLimitReached: String = "Character limit reached"
    override val editFieldUnsavedTitle: String = "Save changes?"
    override val editFieldUnsavedText: String = "You have unsaved changes. Do you want to save them before leaving?"
    override val editFieldUnsavedDiscard: String = "Discard"
    // Main container navigation (a11y)
    override val a11yTab: (String) -> String = { name -> "$name tab" }
    // Profile screen (redesign)
    override val profileSectionInfo: String = "Info"
    override val profileSectionAppearance: String = "Appearance"
    override val profileSectionSession: String = "Session"
    override val profileCopyUsername: String = "Copy username"
    override val profileCopyUsernameHint: String = "Tap to copy"
    override val a11yAvatar: String = "Profile avatar"
    override val a11yEditAvatar: String = "Change avatar"
    override val a11yAvatarPreview: String = "Avatar preview"
    override val a11yChoosePhoto: String = "Choose photo"
    override val avatarCropHint: String = "Drag and pinch to position the photo. Everything inside the circle gets uploaded."
    override val avatarPickPrompt: String = "Tap to pick a photo"

    // Profile screen (extra)
    override val profileNotFoundTitle: String = "User not found"
    override val profileNotFoundDesc: (String) -> String = { username -> "$username is not registered on Vibe or deleted their account." }
    override val profileLoading: String = "Loading profile..."
    override val profileWriteBtn: String = "Message"
    override val profileBlockedTitle: String = "You blocked this user"
    override val profileBlockedDesc: String = "They cannot message you and cannot see your status."
    override val a11yProfileMenu: String = "More"
    override val a11yAvatarViewerClose: String = "Close viewer"
    // Compact number units
    override val unitCompactFormat: (String, String) -> String = { number, unit -> "$number$unit" }
    override val unitThousandShort: String = "K"
    override val unitMillionShort: String = "M"
    override val unitBillionShort: String = "B"
    // Settings root list
    override val settingsChats: String = "Chat settings"
    override val settingsChatsSubtitle: String = "Theme, wallpaper, text size"
    override val settingsPrivacySubtitle: String = "Who can see and message you"
    override val settingsNotifications: String = "Notifications"
    override val settingsNotificationsSubtitle: String = "Sounds, previews, priority"
    override val settingsPowerSaving: String = "Power saving"
    override val settingsPowerSavingSubtitle: String = "Animations and background work"
    override val settingsDevicesSubtitle: String = "Active sessions and sign-out"
    override val settingsLanguageSubtitle: String = "Interface language"
    override val settingsAccountSubtitle: String = "Name, username, about"
    override val settingsSupport: String = "Support"
    override val settingsSupportSubtitle: String = "Questions and bug reports"
    override val settingsVibePro: String = "Vibe Pro"
    override val settingsVibeProSubtitle: String = "Higher limits, exclusive features"
    override val settingsVibeProCta: String = "Learn more"
    // Vibe Pro screen
    override val vibeProHeroDescription: String = "Unlock maximum messaging potential, increased limits, and exclusive status in Vibe."
    override val vibeProSectionFeatures: String = "SUBSCRIPTION FEATURES"
    override val vibeProSectionPlans: String = "SUBSCRIPTION PLAN"
    override val vibeProFeatureLimitsTitle: String = "Doubled Limits"
    override val vibeProFeatureLimitsSubtitle: String = "Up to 2 GB file uploads, 100 pinned chats, and 20 chat folders"
    override val vibeProFeatureVoiceToTextTitle: String = "Voice-to-Text"
    override val vibeProFeatureVoiceToTextSubtitle: String = "Instant transcription of voice and video messages with one tap"
    override val vibeProFeatureReactionsTitle: String = "Exclusive Reactions"
    override val vibeProFeatureReactionsSubtitle: String = "Animated stickers, unique emoji reactions, and emoji statuses"
    override val vibeProFeatureBadgeTitle: String = "Premium Badge"
    override val vibeProFeatureBadgeSubtitle: String = "Exclusive Pro badge next to your name in chats and profile"
    override val vibeProFeatureSpeedTitle: String = "Blazing Speed"
    override val vibeProFeatureSpeedSubtitle: String = "Unlimited download and upload speeds for media and files"
    override val vibeProFeatureNoAdsTitle: String = "Complete Freedom"
    override val vibeProFeatureNoAdsSubtitle: String = "No advertisements, 24/7 priority customer support"
    override val vibeProPlanYearly: String = "1 Year"
    override val vibeProPlanYearlyPrice: String = "$1.99 / mo"
    override val vibeProPlanYearlyDiscount: String = "−25%"
    override val vibeProPlanMonthly: String = "1 Month"
    override val vibeProPlanMonthlyPrice: String = "$2.99 / mo"
    override val vibeProSubscribeCta: (String) -> String = { price -> "Subscribe to Vibe Pro — $price" }
    override val vibeProAutoRenewalDisclaimer: String = "Subscription automatically renews. Cancel anytime."
    override val vibeProComingSoonToast: String = "Subscription checkout will be available in the upcoming update"
    // Two-factor authentication (2FA)
    override val twoFactorTitle: String = "Two-Step Verification"
    override val twoFactorSubtitle: String = "Additional password to protect your account on login"
    override val twoFactorDescription: String = "You can set an additional password that will be required when logging in to your account from a new device in addition to the verification code."
    override val twoFactorStatusEnabled: String = "Enabled"
    override val twoFactorStatusDisabled: String = "Disabled"
    override val twoFactorEnabledBadge: String = "Protection active"
    override val twoFactorEnabledDesc: String = "When logging in on a new device, you will need to enter this password after the verification code."
    override val twoFactorBullet1Title: String = "Strong Security"
    override val twoFactorBullet1Desc: String = "Even if someone obtains your verification code, they will not be able to log in to your account"
    override val twoFactorBullet2Title: String = "Cloud Password"
    override val twoFactorBullet2Desc: String = "Password is encrypted and safely stored in secure storage"
    override val twoFactorBullet3Title: String = "Password Hint"
    override val twoFactorBullet3Desc: String = "You can add a hint to help remember your password"
    override val twoFactorSetPasswordBtn: String = "Set Password"
    override val twoFactorChangePasswordBtn: String = "Change Password"
    override val twoFactorChangeHintBtn: String = "Change Hint"
    override val twoFactorDisableBtn: String = "Disable Protection"
    override val twoFactorEnterNewPasswordTitle: String = "New Password"
    override val twoFactorEnterNewPasswordSubtitle: String = "Choose a password of at least 6 characters"
    override val twoFactorRepeatPasswordTitle: String = "Repeat Password"
    override val twoFactorRepeatPasswordSubtitle: String = "Enter the password again to confirm"
    override val twoFactorEnterCurrentPasswordTitle: String = "Current Password"
    override val twoFactorEnterCurrentPasswordSubtitle: String = "Enter your current two-step verification password"
    override val twoFactorHintTitle: String = "Password Hint"
    override val twoFactorHintSubtitle: String = "A hint to help you recall your password if needed"
    override val twoFactorHintPlaceholder: String = "e.g. favorite book or memorable date"
    override val twoFactorHintTooLong: String = "Hint must not exceed 32 characters"
    override val twoFactorHintContainsPassword: String = "Hint must not contain the password"
    override val twoFactorHintPublicWarning: String = "This hint is visible to anyone attempting to log in to your account"
    override val twoFactorPasswordTooShort: String = "Password must be at least 6 characters"
    override val twoFactorPasswordMismatch: String = "Passwords do not match"
    override val twoFactorPasswordWrong: String = "Incorrect current password"
    override val twoFactorDisableConfirmTitle: String = "Disable Two-Step Verification?"
    override val twoFactorDisableConfirmDesc: String = "Only the verification code will be required to log in on new devices."
    override val twoFactorDisableAction: String = "Disable"
    override val twoFactorNextBtn: String = "Next"
    override val twoFactorSkipBtn: String = "Skip"
    override val twoFactorSaveBtn: String = "Save"
    override val twoFactorStrengthWeak: String = "Weak"
    override val twoFactorStrengthMedium: String = "Medium"
    override val twoFactorStrengthStrong: String = "Strong"
    override val twoFactorStrengthVeryStrong: String = "Excellent"
    override val twoFactorCurrentHintPill: (String) -> String = { hint -> "Hint: $hint" }
    override val twoFactorSuccessSetToast: String = "Two-step verification enabled successfully"
    override val twoFactorSuccessChangedToast: String = "Password changed successfully"
    override val twoFactorSuccessDisabledToast: String = "Two-step verification disabled"
    override val twoFactorPasswordFieldLabel: String = "Password"
    override val twoFactorConfirmFieldLabel: String = "Confirm password"
    override val twoFactorCurrentFieldLabel: String = "Current password"
    override val twoFactorHintFieldLabel: String = "Hint (optional)"
    override val settingsVibes: String = "Vibes"
    override val settingsVibesSubtitle: String = "Chat styling and effects"
    override val settingsGroupGeneral: String = "General"
    override val settingsGroupExtras: String = "Extras"
    override val settingsGroupHelp: String = "Help"
    override val settingsSoonBadge: String = "Soon"
    override val appVersion: (String) -> String = { version -> "App version $version" }
    // Onboarding controls
    override val onboardingGetStarted: String = "GET STARTED"
    override val onboardingSkip: String = "Skip"
    override val a11yOnboardingPage: (Int, Int) -> String = { current, total -> "Page $current of $total" }
    // Passcode
    override val passcodeEnterTitle: String = "Enter passcode"
    override val passcodeEnterSubtitle: String = "Four digits to unlock"
    override val passcodeEnterCurrentTitle: String = "Enter current passcode"
    override val passcodeCreateTitle: String = "Create a passcode"
    override val passcodeRepeatTitle: String = "Repeat the passcode"
    override val passcodeInfoTitle: String = "Passcode lock"
    override val passcodeInfoText: String = "A passcode adds another layer of protection. You'll be asked for it every time the app opens."
    override val passcodeEnableBtn: String = "Enable passcode"
    override val passcodeChangeBtn: String = "Change passcode"
    override val passcodeDisableBtn: String = "Disable passcode"
    override val passcodeDisableShort: String = "Disable"
    override val passcodeRemoveTitle: String = "Disable passcode?"
    override val passcodeRemoveText: String = "The passcode will be deleted and the app will stop asking for it on launch."
    override val passcodeWrongCode: String = "Wrong passcode"
    override val passcodeMismatch: String = "Passcodes don't match"
    override val a11yPasscodeLock: String = "Passcode protection"
    override val a11yPasscodeBackspace: String = "Delete digit"
    override val a11yPasscodeDigit: (String) -> String = { digit -> "Digit $digit" }
    // Nickname screen
    override val nicknameHint: String = "1 to 32 characters. You can change your name later in settings."

    // Shared UI components (button, text field, OTP, toast, inline keyboard)
    override val a11yLoading: String = "Loading"
    override val a11yOtpInput: String = "Verification code"
    override val a11yOtpDigit: (Int, Int) -> String = { position, total -> "Digit $position of $total" }
    override val a11yOtpDigitEmpty: (Int, Int) -> String = { position, total -> "Digit $position of $total, empty" }
    override val a11yFieldError: (String) -> String = { error -> "Error: $error" }
    override val a11yClearField: String = "Clear field"
    override val a11yInlineButtonLink: String = "Opens an external link"
    override val a11yInlineButtonLoading: String = "Request in progress"

    // Link confirmation dialog & inline formatting (a11y)
    override val linkDialogTitle: String = "Open link?"
    override val linkDialogSubtitle: String = "You are leaving the app"
    override val linkDialogSecure: String = "Secure connection"
    override val linkDialogInsecure: String = "Connection is not encrypted"
    override val linkDialogOpen: String = "Open"
    override val linkDialogCancel: String = "Cancel"
    override val a11yLinkChip: (String) -> String = { domain -> "Link to $domain" }
    override val a11ySpoilerHidden: String = "Hidden text. Tap to reveal"
    override val a11ySpoilerRevealed: String = "Hidden text revealed"
    override val a11yQuote: String = "Quote"
    override val formatInlineQuoteWrap: (String) -> String = { text -> "\u201C$text\u201D" }

    // Account settings screen
    override val accountTitle: String = "Account"
    override val accountSectionProfile: String = "Profile"
    override val accountUsernameLabel: String = "Username"
    override val accountNicknameLabel: String = "Display name"
    override val accountNotSet: String = "Not set"
    override val accountNoName: String = "No name"
    override val accountSectionAbout: String = "About"
    override val accountBioLabel: String = "Bio"
    override val accountBioPlaceholder: String = "Write a few words about yourself…"
    override val accountPrivacyFootPrefix: String = "Who can see your bio is configured in "
    override val accountPrivacyFootLink: String = "privacy settings"
    override val accountPrivacyFootSuffix: String = "."
    override val accountLogoutTitle: String = "Log out"
    override val accountLogoutSubtitle: String = "Local drafts and cache will be deleted"
    override val accountLogoutDialogTitle: String = "Log out of your account?"
    override val accountLogoutDialogText: String = "You will need to sign in again to come back."
    override val a11yEditProfile: String = "Edit profile"
    override val a11yCopy: String = "Copy"

    // Notification settings
    override val notifSectionGeneral: String = "Notifications"
    override val notifSectionSound: String = "Sound"
    override val notifMuteAll: String = "Mute all chats"
    override val notifMuteAllDesc: String = "No push notifications from anyone"
    override val notifAutoMute: String = "Auto-mute new chats"
    override val notifAutoMuteDesc: String = "New conversations start muted"
    override val notifAutoMuteFootnote: String = "Existing chats are unchanged. You can unmute a conversation from its chat menu."
    override val notifSoundTitle: String = "Notification sound"
    override val notifSoundSilent: String = "Silent"
    override val notifSoundDefault: String = "System default"
    override val notifSoundCustom: String = "Custom sound"
    override val notifSoundFootnote: String = "Sound applies to this device only. Android notification settings and Do Not Disturb take priority."
    override val notifNoServerResponse: String = "No response from the server. Check your connection and try again."
    override val notifNoPicker: String = "No sound picker is installed on this device."
    override val notifSyncing: String = "Syncing with the server…"

    // Power saving settings
    override val powerSectionMode: String = "Mode"
    override val powerEnableNow: String = "Enable power saving now"
    override val powerEnableNowDesc: String = "Effects are turned off right away"
    override val powerAutoTitle: String = "Enable at battery level"
    override val powerAutoDesc: String = "Automatically when the battery is low"
    override val powerThreshold: String = "Threshold"
    override val powerSectionDisable: String = "What to turn off"
    override val powerLiquid: String = "Liquid glass"
    override val powerBlur: String = "Panel blur"
    override val powerGlow: String = "Background glow"
    override val powerPreviews: String = "Animated GIF and sticker previews"
    override val powerFootnote: String = "Effects come back above the threshold unless power saving is enabled manually."

    // Language settings
    override val languageSearch: String = "Search languages"
    override val languageSectionTitle: String = "Interface language"
    override val languageNotFound: String = "No language found"
    override val languageFootnote: String = "The language applies to the whole app immediately."

    // Two-factor: server flow and sign-in challenge
    override val twoFactorCheckingServer: String = "Checking the server state…"
    override val twoFactorNoServerResponse: String = "No server response. Settings are not confirmed."
    override val twoFactorDisconnected: String = "No connection to the server"
    override val twoFactorPasswordTooLong: String = "Password is too long (72 bytes max)"
    override val twoFactorChallengeTitle: String = "Two-step verification"
    override val twoFactorChallengeSubtitle: String = "Enter the password you set in your security settings"
    override val twoFactorShowHint: String = "Show hint"
    override val twoFactorYourHint: String = "Your hint"
    override val twoFactorForgotPassword: String = "Forgot password?"
    override val twoFactorSignInBtn: String = "Sign in"
    override val twoFactorAttemptsLeft: (Int) -> String = { n -> "attempts left: $n" }
    override val twoFactorForgotUnavailable: String = "The second factor cannot be reset with just a verification code. Contact support."
    override val a11yShowPassword: String = "Show password"
    override val a11yHidePassword: String = "Hide password"

    // Two-factor: recovery e-mail and password reset
    override val twoFactorRecoveryEmailBtn: String = "Recovery e-mail"
    override val twoFactorRecoveryEmailNotSet: String = "Not set"
    override val twoFactorRecoveryEmailFootnote: String =
        "We send a code to your recovery e-mail if you forget the two-factor password. Use an address other than your account e-mail."
    override val twoFactorRecoveryEmailTitle: String = "Recovery e-mail"
    override val twoFactorRecoveryEmailSubtitle: String = "Enter an address and we'll send a confirmation code to it."
    override val twoFactorRecoveryEmailCurrent: (String) -> String = { email -> "Current address: $email" }
    override val twoFactorRecoveryEmailFieldLabel: String = "Recovery e-mail"
    override val twoFactorRecoveryEmailInvalid: String = "Invalid e-mail format"
    override val twoFactorRecoveryEmailSendCodeBtn: String = "Send code"
    override val twoFactorRecoveryEmailRemoveBtn: String = "Remove recovery e-mail"
    override val twoFactorRecoveryCodeTitle: String = "Confirm the address"
    override val twoFactorRecoveryCodeSubtitle: (String) -> String = { masked -> "We sent a confirmation code to $masked" }
    override val twoFactorRecoveryConfirmBtn: String = "Confirm"
    override val twoFactorResendCodeBtn: String = "Send the code again"
    override val twoFactorRecoveryEmailSavedToast: String = "Recovery e-mail saved"
    override val twoFactorRecoveryEmailRemovedToast: String = "Recovery e-mail removed"
    override val twoFactorResetBtn: String = "Reset password"
    override val twoFactorResetTitle: String = "Password reset"
    override val twoFactorResetSubtitle: (String) -> String = { masked -> "We sent a reset code to $masked" }
    override val twoFactorResetConfirmBtn: String = "Reset and sign in"
    override val twoFactorResetSettingsConfirmBtn: String = "Reset password"
    override val twoFactorResetWarning: String =
        "After the reset two-factor protection is turned off. Turn it back on and set a new password."
    override val twoFactorResetNoEmail: String =
        "No recovery e-mail is set, so the password can't be reset. Please contact support."
    override val twoFactorResetFailed: String = "Couldn't reset the password. Please try again later."
    override val twoFactorResetDoneToast: String = "Two-factor protection is off"

    // Emoji / sticker / GIF panel
    override val gifSearchPlaceholder: String = "Search GIFs"
    override val gifNotFound: String = "No GIFs found"
    override val gifLoadFailed: String = "Could not load GIFs"
    override val gifSendCd: String = "Send GIF"
    override val panelTabEmoji: String = "Emoji"
    override val panelTabStickers: String = "Stickers"
    override val panelTabGifs: String = "GIF"
    override val panelRecent: String = "Recent"

    // Passcode settings (status page in the 2FA style)
    override val passcodeStatusEnabledBadge: String = "Passcode is on"
    override val passcodeStatusEnabledDesc: String = "The app asks for a 4-digit code every time it opens. Chats stay locked without it."
    override val passcodeBullet1Title: String = "Lock on launch"
    override val passcodeBullet1Desc: String = "The code is required every time you open Vibe"
    override val passcodeBullet2Title: String = "Stored on this device only"
    override val passcodeBullet2Desc: String = "The code is encrypted and never sent to the server"
    override val passcodeBullet3Title: String = "Not a replacement for 2FA"
    override val passcodeBullet3Desc: String = "Sign-ins from other devices are protected by two-step verification"
    override val passcodeStepCurrentSubtitle: String = "Confirm it is you"
    override val passcodeStepNewSubtitle: String = "Pick 4 digits that are easy to remember"
    override val passcodeStepRepeatSubtitle: String = "Enter the code once more to avoid typos"
    override val passcodeSavedToast: String = "Passcode saved"
    override val passcodeRemovedToast: String = "Passcode turned off"

    // Power saving threshold control
    override val powerThresholdHint: (Int) -> String = { n -> "Effects switch off once the battery drops to $n%" }
    override val powerThresholdPresetCd: (Int) -> String = { n -> "Threshold $n%" }

// Two-factor edit screen
    override val twoFactorCurrentPasswordLabel: String = "Current password"
    override val twoFactorNewPasswordLabel: String = "New password"
    override val twoFactorRepeatPasswordLabel: String = "Repeat password"
    override val twoFactorHintNoPassword: String = "Hint must not contain the password"
    override val twoFactorHintTooLongShort: String = "Too long"
    override val twoFactorHintPublicDesc: String = "Anyone trying to sign in will see this"
    override val twoFactorDisableButton: String = "Turn off two-step verification"
    override val twoFactorAdditionalPasswordTitle: String = "Additional password"
    override val twoFactorAdditionalPasswordDesc: String = "After entering the code from the email, this password will be required. Even if someone accesses your email, they won't be able to sign in."
    override val passwordStrengthWeak: String = "Weak"
    override val passwordStrengthMedium: String = "Medium"
    override val passwordStrengthGood: String = "Good"
    override val passwordStrengthStrong: String = "Strong"
    override val twoFactorDisableDialogTitle: String = "Turn off protection?"
    override val twoFactorDisableDialogDesc: String = "The verification code from the email will be sufficient to sign in again."

    // Privacy option screen
    override val privacyValueEverybody: String = "Everybody"
    override val privacyValueNobody: String = "Nobody"
    override val privacyValueSelected: String = "Selected"
    override val privacySelectUsers: String = "Select users"
    override val privacySelectUsersRuleDesc: String = "Select users to whom this rule will apply."

    // Inline video player & media covers
    override val videoPlaybackFailed: String = "Couldn't play video"
    override val videoScaleCd: String = "Video scale"
    override val videoCoverCd: String = "Video cover"
    override val videoNoFrameCd: String = "Video frame unavailable"
    override val videoPlayCd: String = "Play video"
    override val sampleText: String = "Sample text"
    override val a11yMuteSound: String = "Mute sound"
    override val a11yUnmuteSound: String = "Unmute sound"

    // Circle recording errors
    override val circleCameraInitFailed: String = "Failed to initialize camera"
    override val circleNoCamera: String = "No available camera found on device"
    override val circleCameraUnavailable: (String) -> String = { msg -> "Camera unavailable: $msg" }
    override val circleCameraNotReady: String = "Camera is not ready yet"
    override val circleRecordStartFailedMsg: (String) -> String = { msg -> "Failed to start recording: $msg" }
    override val circleRecordFailedMsg: (Int) -> String = { code -> "Recording failed: code $code" }
    override val circleEmptyRecord: String = "Empty recording"

    // Emoji categories
    override val emojiCategorySmileys: String = "Smileys & Emotion"
    override val emojiCategoryGestures: String = "People & Body"
    override val emojiCategoryHearts: String = "Symbols & Hearts"
    override val emojiCategoryAnimals: String = "Animals & Nature"
    override val emojiCategoryFood: String = "Food & Drink"
    override val emojiCategoryActivities: String = "Activities"
    override val emojiCategoryTravel: String = "Travel & Places"
    override val emojiCategoryObjects: String = "Objects"

    // Download helper & notifications
    override val errorGeneric: String = "An error occurred"
    override val downloadFileDescription: String = "Downloading file from Vibe"
    override val downloadStartedToast: String = "Download started..."
    override val downloadErrorToast: (String) -> String = { err -> "Download failed: $err" }
    override val downloadMultipleToast: (Int) -> String = { count -> "Downloading: $count file(s)" }
    override val notifMe: String = "Me"
    override val unitSecondShort: String = "s"

    override val locale: String = "en"
}

object UaStrings : VibeStrings {
    override val createAccount: String = "Створення облікового запису"
    override val welcomeBack: String = "З поверненням"
    override val emailLabel: String = "ПОШТА"
    override val usernameLabel: String = "ЮЗЕРНЕЙМ"
    override val emailInvalidFormat: String = "НЕВЕРНИЙ ФОРМАТ"
    override val continueBtn: String = "ПРОДОВЖИТИ"
    override val verificationTitle: String = "Введіть код"
    override val verificationSubtitle: (String) -> String = { email -> "Ми надіслали 6-значний код на вашу пошту\n$email" }
    override val verifyBtn: String = "ПІДТВЕРДИТИ"
    override val verifyLoading: String = "ПЕРЕВІРКА..."
    override val codeInvalid: String = "Неправильний код"
    override val verificationSubtitleApp: String = "Код відправлено до програми Vibe на іншому авторизованому пристрої.\nВідкрийте чат з Vibe cat, щоб побачити його"
    override val nicknameTitle: String = "Як вас звати?"
    override val nicknameLabel: String = "ВАШ НІКНЕЙМ"
    override val saveBtn: String = "ПРОДОВЖИТИ"
    override val saveLoading: String = "ЗБЕРІГАННЯ..."
    override val errorSaving: String = "Помилка при збереженні"
    override val tabChats: String = "Чати"
    override val tabSettings: String = "Налаштування"
    override val tabProfile: String = "Профіль"
    override val profileTitle: String = "Профіль"
    override val deletedAcc: String = "Видалений обліковий запис"
    override val statusOnline: String = "У мережі"
    override val statusBot: String = "Бот"
    override val statusUnknown: String = "Невідомо"
    override val actionForward: String = "Переслати"
    override val badgeVerified: String = "Користувач верифікований командою Vibe."
    override val badgeDeveloper: String = "Член команди розробників Vibe."
    override val badgeBot: String = "Просто бот."
    override val freezedAcc: String = "Обліковий запис заморожено за порушення правил."
    override val bannedAcc: String = "Обліковий запис заблоковано за порушення правил."
    override val aboutLabel: String = "Опис"
    override val registerDateLabel: String = "Дата реєстрації"
    override val btnTheme: String = "Тема"
    override val themeDark: String = "Темна"
    override val themeLight: String = "Світла"
    override val btnLanguage: String = "Мова"
    override val btnLogout: String = "Вийти з облікового запису"
    override val logoutConfirmTitle: String = "Вихід з облікового запису"
    override val logoutConfirmText: String = "Ви впевнені, що хочете вийти з облікового запису?"
    override val logoutCancel: String = "Скасування"
    override val logoutConfirm: String = "Вийти"
    override val userLabel: String = "Користувач"
    override val chatsTitle: String = "Чати"
    override val chatsEmptyTitle: String = "Немає чатів"
    override val chatsEmptySubtitle: String = "Почніть листування!"
    override val searchPlaceholder: String = "Пошук..."
    override val globalSearchResults: String = "Глобальний пошук"
    override val typing: String = "Друкує"
    override val connecting: String = "З'єднання"
    override val waitingForNetwork: String = "Очікування мережі"
    override val monthsShort: List<String> = listOf("січ", "лют", "бер", "кві", "тра", "чер", "лип", "сер", "вер", "жов", "лис", "гру")
    override val dateToday: String = "Сьогодні"
    override val dateYesterday: String = "Вчора"
    override val lastSeenRecently: String = "Був(а) нещодавно"
    override val lastSeenLongAgo: String = "Був(а) дуже давно"
    override val lastSeenInWeek: String = "Був(ла) цього тижня"
    override val lastSeenInMonth: String = "Був(ла) цього місяця"
    override val lastSeenToday: (String) -> String = { time -> "Був(а) сьогодні в $time" }
    override val lastSeenYesterday: (String) -> String = { time -> "Був(ла) вчора в $time" }
    override val lastSeenDate: (String, String) -> String = { date, time -> "Був(а) $date в $time" }
    override val backBtn: String = "Назад"
    override val draftLabel: String = "Чернетка:"
    override val messagePlaceholder: String = "Повідомлення..."
    override val replyTo: String = "Відповісти"
    override val emptyChat: String = "Тут поки що порожньо..."
    override val chatHistoryCleared: String = "Історія очищена"
    override val chatHistoryEmpty: String = "Історія порожня"
    override val onboardingPages: List<Pair<String, String>> = listOf(
        "Безпека даних" to "Ваше спілкування захищене шифруванням військового рівня.",
        "Швидкість світла" to "Протоколи нового покоління для миттєвої доставки повідомлень.",
        "Універсальна синхронізація" to "Уся ваша історія даних доступна на кожному пристрої.",
        "Глобальні спільноти" to "Масштабуйте спільноти до мільйонів активних учасників.",
        "Унікальний Вайб" to "Налаштуйте кожен аспект вашого месенджера під себе."
    )
    override val selectedMessagesCount: (Int) -> String = { count -> "Обрано: $count" }
    override val deleteMessagesTitle: String = "Видалити повідомлення?"
    override val deleteMessagesText: (Int) -> String = { count -> "Ви збираєтеся видалити $count повідомлень." }
    override val deleteForEveryone: (String) -> String = { name -> "Також видалити для $name" }
    override val deleteForEveryoneAlsoMine: (String) -> String = { name -> "Також видалити свої повідомлення для $name" }
    override val deleteBtn: String = "Видалити"
    override val cancelBtn: String = "Скасування"
    override val forwardMessageTitle: String = "Надіслати повідомлення"
    override val noRecentChats: String = "Немає недавніх чатів"
    override val editMessageTitle: String = "Редагування повідомлення"
    override val sendBtn: String = "Надіслати"
    override val forwardedFrom: (String) -> String = { name -> "Переслано від $name" }
    override val replyDefault: String = "Відповідь"
    override val pinnedMessage: String = "Закріплене повідомлення"
    override val pinnedMessages: String = "Закріплені повідомлення"
    override val pinMessage: String = "Закріпити повідомлення"
    override val pinMessageConfirm: String = "Ви дійсно хочете закріпити це повідомлення?"
    override val unpinMessage: String = "Відкріпити повідомлення"
    override val unpinMessageConfirm: String = "Ви дійсно бажаєте відкріпити це повідомлення?"
    override val unpinAll: String = "Відкріпити все"
    override val unpinAllConfirm: String = "Відкріпити всі повідомлення у цьому чаті?"
    override val forBoth: (String) -> String = { name -> "Також для $name" }
    override val pin: String = "Закріпити"
    override val unpin: String = "Відкріпити"
    override val you: String = "Ви"
    override val edit: String = "Змінити"
    override val usernameTaken: String = "Цей користувач вже зайнятий"
    override val usernameAvailable: String = "Юзернейм вільний"
    override val usernameDescription: String = "Ви можете вибрати унікальне ім'я користувача Vibe. Якщо ви це зробите, інші люди зможуть знайти вас по цьому імені та зв'язатися з вами, не знаючи вашого номера телефону."
    override val usernameMinLength: String = "Мінімальна довжина 4 символи"
    override val nicknameDescription: String = "Ваше ім'я, яке відображатиметься всім користувачам. Постарайтеся вибрати ім'я, щоб друзі могли легко вас знайти."
    override val bioDescription: String = "Напишіть трохи про себе. Ця інформація буде помітна іншим користувачам у вашому профілі."
    override val doneBtn: String = "Готово"
    override val userRestrictedMessaging: String = "Користувач обмежив коло спілкування"
    override val userHiddenAccount: String = "Користувач приховав обліковий запис"
    override val editedLabel: String = "(змінено)"
    override val muteNotifications: String = "Вимкнути повідомлення"
    override val unmuteNotifications: String = "Включити повідомлення"
    override val pinnedMessageSystemText: (String, String) -> String = { sender, content -> "$sender закріпив(а) повідомлення: \"$content\"" }
    override val privacyScreenTitle: String = "Конфіденційність"
    override val privacyTwoFactor: String = "Подвійна автентифікація"
    override val privacyPasscodeLogin: String = "Вхід за кодом"
    override val privacyBlocked: String = "Заблоковані"
    override val privacyActivityTitle: String = "Статус активності"
    override val privacyActivityDesc: String = "Хто може бачити, коли ви востаннє були у мережі. Якщо ви приховаєте свій статус активності, ви не зможете бачити статус інших користувачів (відображатиметься приблизний час)."
    override val privacyAvatarTitle: String = "Аватарка"
    override val privacyAvatarDesc: String = "Хто може бачити вашу аватарку. Для інших відображатиметься перша літера вашого імені на блакитному тлі."
    override val privacyForwardedTitle: String = "Надіслані повідомлення"
    override val privacyForwardedDesc: String = "Хто може переходити на ваш профіль із надісланих повідомлень."
    override val privacyMessagesTitle: String = "Повідомлення"
    override val privacyMessagesDesc: String = "Хто може надсилати вам повідомлення. Користувачі, яким це заборонено, побачать напис \"Користувач обмежив коло спілкування\"."
    override val privacyStatusTitle: String = "Статус"
    override val privacyStatusDesc: String = "Хто може бачити блок \"Про себе\" у вашому профілі."

    // Report dialog
    override val reportTitle: String = "Поскаржитись"
    override val reportSubtitle: String = "Виберіть причину. Скарга анонімна, автор її побачить."
    override val reportStepLabel: (Int, Int) -> String = { current, total -> "Крок $current з $total" }
    override val reportReasonSpam: String = "Спам"
    override val reportReasonSpamDesc: String = "Реклама, розсилки, накрутка"
    override val reportReasonFraud: String = "Шахрайство"
    override val reportReasonFraudDesc: String = "Обман, фішинг, схеми із грошима"
    override val reportReasonDrugs: String = "Наркотики"
    override val reportReasonDrugsDesc: String = "Продаж чи реклама заборонених речовин"
    override val reportReasonWeapons: String = "Зброя"
    override val reportReasonWeaponsDesc: String = "Торгівля зброєю та вибухівкою"
    override val reportReasonPorn: String = "Порнографія"
    override val reportReasonPornDesc: String = "Матеріали для дорослих без попередження"
    override val reportReasonCsam: String = "Матеріали з дітьми (CSAM)"
    override val reportReasonCsamDesc: String = "Сексуалізований контент із неповнолітніми"
    override val reportReasonViolence: String = "Насильство"
    override val reportReasonViolenceDesc: String = "Загрози, жорстокість, заклики до шкоди"
    override val reportReasonHarassment: String = "Травля"
    override val reportReasonHarassmentDesc: String = "Образи, переслідування, шантаж"
    override val reportReasonHate: String = "Розпалювання ненависті"
    override val reportReasonHateDesc: String = "Нападки за ознакою раси, релігії, статі"
    override val reportReasonFakeAccount: String = "Фейковий аккаунт"
    override val reportReasonFakeAccountDesc: String = "Видає себе за іншу людину"
    override val reportReasonMisinfo: String = "Хибна інформація"
    override val reportReasonMisinfoDesc: String = "Небезпечні чутки та дезінформація"
    override val reportCriticalNotice: String = "Такі скарги ми розглядаємо у пріоритетному порядку та передаємо до профільних органів."
    override val reportDetailsTitle: String = "Розкажіть детальніше"
    override val reportDetailsHint: String = "Опишіть ситуацію: що трапилося і де. Це допоможе нам вжити заходів швидше."
    override val reportCommentPlaceholder: String = "Коментар (необов'язково)"
    override val reportCommentCounter: (Int, Int) -> String = { used, limit -> "$used / $limit" }
    override val reportChangeReason: String = "Інша причина"
    override val reportSubmitBtn: String = "Надіслати скаргу"
    override val reportCancelBtn: String = "Скасування"
    override val reportSentTitle: String = "Скаргу надіслано"
    override val reportSentDesc: String = "Дякую. Модератори вивчать звернення та ухвалять рішення."
    override val reportDoneBtn: String = "Готово"
    override val a11yReportClose: String = "Закрити"

    // Privacy exceptions picker
    override val privacyExceptionsTitle: String = "Винятки"
    override val privacyExceptionsHint: String = "Виберіть, на кого правило не поширюється"
    override val privacyExceptionsSelected: (Int) -> String = { count -> "Обрано: $count" }
    override val privacyExceptionsSelectAll: String = "Вибрати всіх"
    override val privacyExceptionsClearAll: String = "Зняти вибір"
    override val privacyExceptionsEmptyTitle: String = "Немає кого вибрати"
    override val privacyExceptionsEmptyDesc: String = "Тут з'являться люди, з якими маєте чати."
    override val a11yExceptionToggle: (String) -> String = { name -> "Вибрати $name" }
    override val a11yExceptionRemove: (String) -> String = { name -> "Прибрати $name з вибраних" }
    override val settingsPrivacy: String = "Приватність"
    override val settingsAccount: String = "Обліковий запис"
    override val settingsDevices: String = "Пристрої"
    override val settingsPasscode: String = "Код-пароль"
    override val usernameCopied: String = "Користувач скопійований"
    override val addAvatar: String = "Додати аватарку"
    override val choosePhoto: String = "Вибрати фото"
    override val switchSignIn: String = "Вже є обліковий запис? Увійти"
    override val switchSignUp: String = "Немає облікового запису? Створити"
    override val authTabSignUp: String = "Реєстрація"
    override val authTabSignIn: String = "Вхід"
    override val authChecking: String = "Перевіряємо..."
    override val authEmailAvailable: String = "Пошта вільна"
    override val authEmailTaken: String = "ЦЯ ПОШТА ЗАНЯТА"
    override val authUsernameTakenShort: String = "ЦЕЙ ЮЗЕРНЕЙМ ЗАНЯТО"
    override val authUsernameAvailable: (String) -> String = { username -> "@$username вільний" }
    override val authRegisterFailed: String = "Не вдалося створити обліковий запис"
    override val authLoginFailed: String = "Не вдалося увійти"
    override val authUsernameCounter: (Int, Int) -> String = { current, max -> "$current/$max" }
    override val languageName: String = "Українська"
    override val blockedTitle: String = "Заблоковані"
    override val blockedSearchPlaceholder: String = "Пошук користувачів..."
    override val blockedClearSearch: String = "Очистити пошук"
    override val blockedEmptyTitle: String = "Немає заблокованих"
    override val blockedEmptyDesc: String = "Тут відображатимуться користувачі, яких ви заблокували."
    override val blockedSearchEmptyTitle: String = "Нічого не знайдено"
    override val blockedSearchEmptyDesc: (String) -> String = { query -> "За запитом «$query» користувачів не знайдено" }
    override val blockedUnblockBtn: String = "Розблокувати"
    override val blockedUnblockConfirmTitle: String = "Розблокувати?"
    override val blockedUnblockConfirmText: (String) -> String = { name -> "$name знову зможе писати вам і бачити ваш профіль." }
    override val blockedUnblockedToast: String = "Користувач розблоковано"
    override val blockedUserFallback: (Int) -> String = { id -> "Користувач #$id" }
    override val accountDeleted: String = "Віддалений обліковий запис"
    override val accountFrozen: String = "Заморожений обліковий запис"
    override val accountBannedMessage: String = "Ваш обліковий запис було заблоковано за порушення правил."
    override val accountFrozenMessage: String = "Ваш обліковий запис був заморожений модератором."
    override val typeVideo: String = "Відео"
    override val typeVideoMessage: String = "Відеоповідомлення"
    override val typeAudio: String = "Аудіофайл"
    override val typeFile: String = "Файл"
    override val typeVoice: String = "Голосове повідомлення"
    override val playerPlaylist: String = "Плейлист чату"
    override val playerSearchTracks: String = "Пошук треків"
    override val playerNowPlaying: String = "Зараз грає"
    override val playerTrackFallback: String = "Аудіозапис"
    override val playerTracksCount: (Int) -> String = { count ->
        val word = when {
            count % 10 == 1 && count % 100 != 11 -> "трек"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "треки"
            else -> "треків"
        }
        "$count $word"
    }
    override val playerQueueEmpty: String = "Черга порожня"
    override val playerQueueEmptyHint: String = "Аудіо з цього чату з'явиться тут"
    override val playerSearchEmptyTitle: String = "Нічого не знайдено"
    override val playerSearchEmptySubtitle: (String) -> String = { query -> "За запитом «$query» треків немає" }
    override val playerBuffering: String = "Буферизація…"
    override val playerTimeZero: String = "0:00"
    override val playerTimeUnknown: String = "--:--"
    override val playerSpeedFormat: (String) -> String = { value -> "$value×" }
    override val playerRepeatOff: String = "Повтор вимкнено"
    override val playerRepeatAll: String = "Повторювати плейлист"
    override val playerRepeatOne: String = "Повторювати трек"
    override val a11yPlayerPlay: String = "Відтворити"
    override val a11yPlayerPause: String = "Пауза"
    override val a11yPlayerNext: String = "Наступний трек"
    override val a11yPlayerPrevious: String = "Попередній трек"
    override val a11yPlayerRewind10: String = "Назад на 10 секунд"
    override val a11yPlayerForward10: String = "Вперед на 10 секунд"
    override val a11yPlayerShuffle: String = "Перемішати"
    override val a11yPlayerClose: String = "Закрити плеєр"
    override val a11yPlayerExpand: String = "Розгорнути плеєр"
    override val a11yPlayerCollapse: String = "Згорнути плеєр"
    override val a11yPlayerSearch: String = "Пошук по плейлисту"
    override val a11yPlayerSearchClose: String = "Закрити пошук"
    override val a11yPlayerClearSearch: String = "Очистити пошук"
    override val a11yPlayerArtwork: String = "Обкладинка треку"
    override val a11yPlayerSpeed: (String) -> String = { value -> "Швидкість відтворення: $value" }
    override val a11yPlayerTrackRow: (String) -> String = { title -> "Увімкнути трек $title" }
    override val sectionPhotosVideos: String = "Фото та відео"
    override val sectionFiles: String = "Файли"
    override val sectionMusic: String = "Музика"
    override val sectionVoice: String = "Голосові"
    override val profileSearchFiles: String = "Пошук файлів"
    override val actionDownload: String = "Завантажити"
    override val actionDownloadSelected: String = "Завантажити вибрані"
    override val formatCopy: String = "Копіювати"
    override val formatCut: String = "Вирізати"
    override val formatFormat: String = "Форматувати"
    override val formatBold: String = "Жирний"
    override val formatItalic: String = "Курсив"
    override val formatBoldItalic: String = "Жирний курсив"
    override val formatStrikethrough: String = "Закреслений"
    override val formatUnderline: String = "Підкреслений"
    override val formatMonospace: String = "Моноширинний"
    override val formatLink: String = "Посилання"
    override val formatTextColor: String = "Колір тексту"
    override val formatSpoiler: String = "Спойлер"
    override val formatQuote: String = "Цитата"
    override val formatLinkUrlHint: String = "Введіть URL"
    override val formatColorHint: String = "Введіть HEX (напр. #FF5733)"
    override val formatPreview: String = "Передпрогляд"
    override val formatCopied: String = "Скопійовано"
    override val formatReadMore: String = "Читати далі"
    override val formatCollapse: String = "Згорнути"
    override val attachPhotoVideo: String = "Фото та відео"
    override val attachFile: String = "Файл"
    override val attachTitle: String = "Вкладення"
    override val photoViewer: String = "Перегляд фото"
    override val photoOf: (Int, Int) -> String = { current, total -> "$current з $total" }
    override val actionCopy: String = "Копіювати текст"
    override val actionReport: String = "Поскаржитись"


    // Chat header
    override val chatSearchPlaceholder: String = "Пошук за повідомленнями…"
    override val chatSearchNoResults: String = "Нічого не знайдено"
    override val chatSearchCounter: (Int, Int) -> String = { current, total -> "$current з $total" }
    override val chatSearchClose: String = "Закрити пошук"
    override val chatSearchClear: String = "Очистити запит"
    override val chatSearchByDate: String = "Пошук за датою"
    override val chatSearchNext: String = "Наступний результат"
    override val chatSearchPrev: String = "Попередній результат"
    override val chatActionSearch: String = "Пошук"
    override val chatMenu: String = "Меню чату"
    override val chatAvatar: String = "Аватар"
    override val chatOpenProfile: String = "Відкрити профіль"
    override val chatClearSelection: String = "Зняти виділення"
    override val chatStatusBlockedByMe: String = "Заблоковано"
    override val chatBlockUser: String = "Заблокувати"
    override val chatUnblockUser: String = "Розблокувати"
    override val chatUserBlockedToast: String = "Користувач заблоковано"
    override val chatPinnedCounter: (Int, Int) -> String = { current, total -> "$current/$total" }
    override val chatJumpToPinned: String = "Перейти до закріпленого повідомлення"
    override val chatUnpinAllHint: String = "Відкріпити все"


    // Chat input bar
    override val inputAttachMedia: String = "Прикріпити"
    override val inputAttachGallery: String = "Фото чи відео"
    override val inputAttachFile: String = "Файл"
    override val inputEmojiPanel: String = "Емодзі, стікери та GIF"
    override val inputSelectedMedia: (Int) -> String = { count -> "Вибрано медіа: $count" }
    override val inputClearAttachments: String = "Очистити вкладення"
    override val inputBlockedByMe: String = "Ви заблокували цього користувача"
    override val inputAttachmentPreview: String = "Вкладення"
    override val inputCancelReply: String = "Скасувати"
    override val voiceHoldToRecord: String = "Утримуйте для запису"
    override val voiceSendRecording: String = "Надіслати голосове повідомлення"
    override val voiceCancelRecording: String = "Скасувати запис"
    override val voiceLocked: String = "Запис закріплено"
    override val voiceSlideToCancel: String = "Змахніть вліво, щоб скасувати"
    override val voiceSlideToLock: String = "Вгору - закріпити"
    override val voiceRecordStartFailed: String = "Неможливо розпочати запис голосового повідомлення"
    override val voicePermissionRequired: String = "Для запису потрібний доступ до мікрофону"
    override val voiceTooShort: String = "Запис вийшов занадто коротким"

    // ПУНКТ 2 — кружки (видеосообщения)
    override val circleModeSwitchedOn: String = "Режим кружків: утримуйте для запису"
    override val circleModeSwitchedOff: String = "Режим голосових повідомлень"
    override val circleRecordVideoMessage: String = "Записати відеоповідомлення"
    override val circleHoldOrTapHint: String = "Натисніть для запису, утримуйте для швидкої зйомки"
    override val circleRecordingHint: String = "Вліво - скасування · вгору - закріпити"
    override val circleReleaseToCancel: String = "Відпустіть для скасування"
    override val circleLockedHint: String = "Натисніть, щоб відправити"
    override val circleTapToStop: String = "Натисніть, щоб зупинити та відправити"
    override val circleCameraPreparing: String = "Готуємо камеру."
    override val circleMaxDurationHint: String = "Максимум 60 секунд"
    override val circlePermissionRequired: String = "Для гуртків потрібен доступ до камери та мікрофону"
    override val circleTooShort: String = "Гурток вийшов занадто коротким"
    override val circleCancel: String = "Скасувати запис"
    override val circleSend: String = "Надіслати гурток"
    override val circleSwitchCamera: String = "Змінити камеру"
    override val circleSendFailed: String = "Не вдалося відправити гурток"


    // Chat list (iOS redesign)
    override val chatsSectionPinned: String = "Закріплені"
    override val filterAll: String = "Усі"
    override val filterUnread: String = "Непрочитані"
    override val filterUnreadCount: (Int) -> String = { count -> "Непрочитані ($count)" }
    override val chatsNoUnreadTitle: String = "Все прочитано"
    override val chatsNoUnreadSubtitle: String = "Непрочитаних повідомлень нема."
    override val chatsSectionAll: String = "Усі чати"
    override val chatsCountFooter: (Int) -> String = { count -> "Чатів: $count" }
    override val chatsEmptyHint: String = "Знайти користувача через пошук вище"
    override val chatsSearchCancel: String = "Скасування"
    override val chatsSearchClearField: String = "Очистити поле пошуку"
    override val chatsSearchNoResultsTitle: String = "Нічого не знайдено"
    override val chatsSearchNoResultsSubtitle: (String) -> String = { query -> "Немає чатів та користувачів на запит «$query»" }
    override val userFallback: (Int) -> String = { id -> "Користувач #$id" }
    override val someoneLabel: String = "Хтось"
    override val a11yMutedChat: String = "Повідомлення вимкнено"
    override val a11yPinnedChat: String = "Чат закріплений"
    override val actionMuteShort: String = "Без звуку"
    override val actionUnmuteShort: String = "Зі звуком"


    // Chat list message previews
    override val typePhoto: String = "Фотографія"
    override val previewVoiceMessage: (String) -> String = { duration -> "Голосове повідомлення $duration" }
    override val previewVideoMessage: (String) -> String = { duration -> "Відеоповідомлення $duration" }
    override val previewAudioTrack: (String, String) -> String = { artist, title -> "$artist - $title" }
    override val previewAudioLoading: String = "Музика..."
    override val previewMorePhotos: (Int) -> String = { count -> "+$count фотографій" }
    override val previewMoreVideos: (Int) -> String = { count -> "+$count відео" }
    override val previewMoreWithCaption: (Int, String) -> String = { count, caption -> "+$count $caption" }
    override val typeSticker: String = "Стікер"
    override val typeGif: String = "GIF"
    override val previewMediaCount: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "медіафайл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "медіафайли"
            else -> "медіафайлів"
        }
        "$count $form"
    }
    override val previewMoreAudio: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "аудіофайл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "аудіофайли"
            else -> "аудіофайлів"
        }
        "$count $form"
    }
    override val previewMoreFiles: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "файл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "файли"
            else -> "файлів"
        }
        "$count $form"
    }
    override val previewMoreAttachments: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "вкладення"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "вкладення"
            else -> "вкладень"
        }
        "$count $form"
    }
    override val actionSelectMessage: String = "Вибрати"
    // Chat message list (empty state, system messages, scroll-to-bottom)
    override val emptyChatSubtitle: String = "Напишіть перше повідомлення, а історія чату з'явиться тут."
    override val emptyChatHint: String = "Почніть розмову першою"
    override val chatSystemMessageLabel: String = "Системне повідомлення"
    override val linkOpenFailed: String = "Неможливо відкрити посилання"
    override val a11yMessageList: String = "Список повідомлень"
    override val a11yScrollToBottom: String = "Перейти до останніх повідомлень"
    override val a11yUnreadCount: (Int) -> String = { count -> "Нових повідомлень: $count" }
    override val unreadCountOverflow: String = "99+"
    // Chat dialogs, reactions sheet, bot & network toasts
    override val okBtn: String = "OK"
    override val actionClose: String = "Закрити"
    override val reportSentToast: String = "Скаргу успішно надіслано"
    override val restrictionTitle: String = "Обмеження"
    override val restrictionUnderstood: String = "Зрозуміло"
    override val restrictionWhy: String = "Чому?"
    override val datePickerTitle: String = "Виберіть дату"
    override val dateJumpNotFound: String = "Повідомлень за цю дату не знайдено"
    override val botMessageTitle: String = "Допис від бота"
    override val botLabel: String = "Бот"
    override val attachmentLabel: String = "Вкладення"
    override val fileSizeLoading: String = "Завантаження..."
    override val reactionsTitle: String = "Реакції"
    override val reactionsAllTab: (Int) -> String = { count -> "Усі $count" }
    override val reactionsEmpty: String = "Поки що немає реакцій"
    override val voiceTrackTitleMine: String = "Ви (Голосове повідомлення)"
    override val botCallbackTimeout: String = "Бот не відповів за 10 секунд. Кнопки повідомлення знову доступні."
    override val connectionLostToast: String = "Немає з'єднання з сервером. Спробуйте ще раз."
    override val fileOpenFailed: String = "Не вдалося відкрити вибраний файл"
    override val maxPinnedChatsToast: (Int) -> String = { limit -> "Можна закріпити не більше $limit чатів" }
    // Chat toast host
    override val toastTitleInfo: String = "Інформація"
    override val toastTitleSuccess: String = "Готово"
    override val toastTitleWarning: String = "Увага"
    override val toastTitleError: String = "Помилка"
    override val toastActionRetry: String = "Повторити"
    override val toastActionUndo: String = "Скасувати"
    override val toastCopied: String = "Скопійовано"
    override val a11yToast: (String) -> String = { text -> "Повідомлення: $text" }
    override val a11yToastDismiss: String = "Приховати повідомлення"
    // Devices & sessions
    override val devicesTitle: String = "Пристрої"
    override val devicesSubtitle: String = "Тут показані всі входи до вашого облікового запису"
    override val devicesSessionsCount: (Int) -> String = { count ->
        val tail = count % 10
        val hundred = count % 100
        when {
            hundred in 11..14 -> "$count активних сеансів"
            tail == 1 -> "$count активний сеанс"
            tail in 2..4 -> "$count активні сеанси"
            else -> "$count активних сеансів"
        }
    }
    override val devicesRefreshCd: String = "Оновити список"
    override val devicesSectionCurrent: String = "ЦЕ ПРИСТРІЙ"
    override val devicesSectionOther: String = "ІНШІ СЕАНСИ"
    override val devicesCurrentBadge: String = "Поточне"
    override val devicesOnlineNow: String = "У мережі"
    override val devicesLastActiveNow: String = "Щойно"
    override val devicesLastActiveMinutes: (Int) -> String = { minutes ->
        val tail = minutes % 10
        val hundred = minutes % 100
        val word = when {
            hundred in 11..14 -> "хвилин"
            tail == 1 -> "хвилину"
            tail in 2..4 -> "хвилини"
            else -> "хвилин"
        }
        "$minutes $word тому"
    }
    override val devicesLastActiveHours: (Int) -> String = { hours ->
        val tail = hours % 10
        val hundred = hours % 100
        val word = when {
            hundred in 11..14 -> "годин"
            tail == 1 -> "годину"
            tail in 2..4 -> "години"
            else -> "годин"
        }
        "$hours $word тому"
    }
    override val devicesLastActiveYesterday: String = "Вчора"
    override val devicesLastActiveDate: (String) -> String = { date -> "Був(а): $date" }
    override val devicesDateTimePattern: String = "dd.MM.yyyy, HH:mm"
    override val devicesUnknownDevice: String = "Невідомий пристрій"
    override val devicesUnknownLocation: String = "Місцезнаходження невідоме"
    override val devicesNoOtherSessions: String = "Інших сеансів немає"
    override val devicesNoOtherSessionsHint: String = "В обліковий запис виконано вхід тільки з цього пристрою"
    override val devicesEmptyTitle: String = "Активних сеансів немає"
    override val devicesEmptySubtitle: String = "Не вдалося знайти жодного входу до облікового запису. Спробуйте оновити список."
    override val devicesLoading: String = "Завантажуємо сеанси..."
    override val devicesLoadFailedTitle: String = "Неможливо завантажити"
    override val devicesLoadFailedSubtitle: String = "Сервер не відповів. Перевірте з'єднання та спробуйте знову."
    override val devicesTerminateCd: String = "Завершити сеанс"
    override val devicesTerminateAll: String = "Завершити все"
    override val devicesTerminateTitle: String = "Завершити сеанс?"
    override val devicesTerminateText: (String) -> String = { name ->
        "Пристрій «$name» вийде з акаунта. Для повторного входу знадобиться код підтвердження."
    }
    override val devicesTerminateAllTitle: String = "Завершити усі сеанси?"
    override val devicesTerminateAllText: (Int) -> String = { count ->
        "З акаунта вийдуть усі інші пристрої ($count). Цей пристрій залишиться в мережі."
    }
    override val devicesTerminateConfirm: String = "Завершити"
    override val devicesSecurityHint: String = "Не впізнаєте пристрій? Завершіть сеанс та змініть пароль."
    // Edit profile field
    override val editFieldSave: String = "Зберегти"
    override val editFieldSaveCd: String = "Зберегти зміни"
    override val editFieldPlaceholder: (String) -> String = { title -> "Введіть ${title.lowercase()}" }
    override val editFieldClearCd: String = "Очистити поле"
    override val editFieldCounter: (Int, Int) -> String = { used, max -> "$used / $max" }
    override val editFieldLimitReached: String = "Досягнуто ліміту символів"
    override val editFieldUnsavedTitle: String = "Зберегти зміни?"
    override val editFieldUnsavedText: String = "У вас є незбережені зміни. Зберегти їх перед виходом?"
    override val editFieldUnsavedDiscard: String = "Скинути"
    // Main container navigation (a11y)
    override val a11yTab: (String) -> String = { name -> "Вкладка «$name»" }
    // Profile screen (redesign)
    override val profileSectionInfo: String = "Інформація"
    override val profileSectionAppearance: String = "Оформлення"
    override val profileSectionSession: String = "Сеанс"
    override val profileCopyUsername: String = "Копіювати юзернейм"
    override val profileCopyUsernameHint: String = "Натисніть, щоб скопіювати"
    override val a11yAvatar: String = "Аватар профілю"
    override val a11yEditAvatar: String = "Змінити аватар"
    override val a11yAvatarPreview: String = "Перегляд аватара"
    override val a11yChoosePhoto: String = "Вибрати фото"
    override val avatarCropHint: String = "Перетягуйте та зводьте пальці, щоб розмістити фото. Завантажиться область усередині кола."
    override val avatarPickPrompt: String = "Натисніть, щоб вибрати фото"

    // Profile screen (extra)
    override val profileNotFoundTitle: String = "Користувач не знайдено"
    override val profileNotFoundDesc: (String) -> String = { username -> "$username не зареєстрований у Vibe або видалив обліковий запис." }
    override val profileLoading: String = "Завантажуємо профіль..."
    override val profileWriteBtn: String = "Написати"
    override val profileBlockedTitle: String = "Ви заблокували цього користувача"
    override val profileBlockedDesc: String = "Він не може вам писати і не бачить ваш статус."
    override val a11yProfileMenu: String = "Ще"
    override val a11yAvatarViewerClose: String = "Закрити перегляд"
    // Compact number units
    override val unitCompactFormat: (String, String) -> String = { number, unit -> "$number $unit" }
    override val unitThousandShort: String = "тис."
    override val unitMillionShort: String = "млн."
    override val unitBillionShort: String = "млрд."
    // Settings root list
    override val settingsChats: String = "Налаштування чатів"
    override val settingsChatsSubtitle: String = "Тема, шпалери, розмір тексту"
    override val settingsPrivacySubtitle: String = "Хто бачить вас і пише вам"
    override val settingsNotifications: String = "Повідомлення"
    override val settingsNotificationsSubtitle: String = "Звуки, прев'ю, пріоритет"
    override val settingsPowerSaving: String = "Економія енергії"
    override val settingsPowerSavingSubtitle: String = "Анімації та фонова робота"
    override val settingsDevicesSubtitle: String = "Активні сеанси та вихід"
    override val settingsLanguageSubtitle: String = "Мова інтерфейсу"
    override val settingsAccountSubtitle: String = "Ім'я, юзернейм, опис"
    override val settingsSupport: String = "Підтримка"
    override val settingsSupportSubtitle: String = "Запитання, звіти про помилки"
    override val settingsVibePro: String = "Vibe Pro"
    override val settingsVibeProSubtitle: String = "Більше лімітів, ексклюзивні функції"
    override val settingsVibeProCta: String = "Докладніше"
    // Vibe Pro screen
    override val vibeProHeroDescription: String = "Максимальні можливості спілкування, збільшені ліміти та ексклюзивний статус у Vibe."
    override val vibeProSectionFeatures: String = "МОЖЛИВОСТІ ПЕРЕДПЛАТИ"
    override val vibeProSectionPlans: String = "Тарифний план"
    override val vibeProFeatureLimitsTitle: String = "Збільшені ліміти"
    override val vibeProFeatureLimitsSubtitle: String = "Надсилання файлів до 2 ГБ, до 100 закріплених чатів та 20 папок"
    override val vibeProFeatureVoiceToTextTitle: String = "Голосові текст"
    override val vibeProFeatureVoiceToTextSubtitle: String = "Миттєве розшифрування аудіо- та відеоповідомлень одним дотиком"
    override val vibeProFeatureReactionsTitle: String = "Ексклюзивні реакції"
    override val vibeProFeatureReactionsSubtitle: String = "Анімовані стікери, унікальні емодзі та емодзі-статуси"
    override val vibeProFeatureBadgeTitle: String = "Преміум значок"
    override val vibeProFeatureBadgeSubtitle: String = "Особливий значок Pro поруч із вашим ім'ям у чатах та профілі"
    override val vibeProFeatureSpeedTitle: String = "Надшвидка швидкість"
    override val vibeProFeatureSpeedSubtitle: String = "Завантаження та надсилання медіафайлів без обмеження пропускної спроможності"
    override val vibeProFeatureNoAdsTitle: String = "Повна свобода"
    override val vibeProFeatureNoAdsSubtitle: String = "Жодної реклами, пріоритетна технічна підтримка 24/7"
    override val vibeProPlanYearly: String = "1 рік"
    override val vibeProPlanYearlyPrice: String = "149 ₽ / міс"
    override val vibeProPlanYearlyDiscount: String = "−25%"
    override val vibeProPlanMonthly: String = "1 місяць"
    override val vibeProPlanMonthlyPrice: String = "199 ₽ / міс"
    override val vibeProSubscribeCta: (String) -> String = { price -> "Підключити Vibe Pro - $price" }
    override val vibeProAutoRenewalDisclaimer: String = "Передплата продовжується автоматично. Скасувати можна будь-коли."
    override val vibeProComingSoonToast: String = "Оформлення передплати стане доступним у найближчому оновленні"
    // Two-factor authentication (2FA)
    override val twoFactorTitle: String = "Двоетапна автентифікація"
    override val twoFactorSubtitle: String = "Додатковий пароль для захисту під час входу"
    override val twoFactorDescription: String = "Ви можете вказати додатковий пароль, який потрібно вводити під час входу з нового пристрою на додаток до коду підтвердження."
    override val twoFactorStatusEnabled: String = "Включено"
    override val twoFactorStatusDisabled: String = "Вимкнена"
    override val twoFactorEnabledBadge: String = "Захист активний"
    override val twoFactorEnabledDesc: String = "При вході на новий пристрій потрібно ввести пароль після коду підтвердження."
    override val twoFactorBullet1Title: String = "Надійний захист"
    override val twoFactorBullet1Desc: String = "Навіть якщо сторонній отримає код підтвердження, він не зможе увійти до вашого облікового запису"
    override val twoFactorBullet2Title: String = "Хмарний пароль"
    override val twoFactorBullet2Desc: String = "Пароль надійно зашифрований у захищеному сховищі"
    override val twoFactorBullet3Title: String = "Підказка для пам'яті"
    override val twoFactorBullet3Desc: String = "Можливість вказати підказку, яка допоможе пригадати пароль"
    override val twoFactorSetPasswordBtn: String = "Задати пароль"
    override val twoFactorChangePasswordBtn: String = "Змінити пароль"
    override val twoFactorChangeHintBtn: String = "Змінити підказку"
    override val twoFactorDisableBtn: String = "Вимкнути захист"
    override val twoFactorEnterNewPasswordTitle: String = "Новий пароль"
    override val twoFactorEnterNewPasswordSubtitle: String = "Придумайте пароль довжиною щонайменше 6 символів"
    override val twoFactorRepeatPasswordTitle: String = "Повторіть пароль"
    override val twoFactorRepeatPasswordSubtitle: String = "Введіть пароль ще раз для підтвердження"
    override val twoFactorEnterCurrentPasswordTitle: String = "Поточний пароль"
    override val twoFactorEnterCurrentPasswordSubtitle: String = "Введіть поточний пароль двоетапної автентифікації"
    override val twoFactorHintTitle: String = "Підказка для пароля"
    override val twoFactorHintSubtitle: String = "Підказка допоможе пригадати пароль за необхідності"
    override val twoFactorHintPlaceholder: String = "Наприклад: улюблена книга чи дата"
    override val twoFactorHintTooLong: String = "Підказка не повинна перевищувати 32 символи"
    override val twoFactorHintContainsPassword: String = "Підказка не повинна містити сам пароль"
    override val twoFactorHintPublicWarning: String = "Підказка видно будь-кому, хто спробує увійти до вашого облікового запису"
    override val twoFactorPasswordTooShort: String = "Пароль повинен містити щонайменше 6 символів"
    override val twoFactorPasswordMismatch: String = "Паролі не збігаються"
    override val twoFactorPasswordWrong: String = "Неправильний поточний пароль"
    override val twoFactorDisableConfirmTitle: String = "Вимкнути двоетапний захист?"
    override val twoFactorDisableConfirmDesc: String = "Для входу на нові пристрої знову буде достатньо лише коду підтвердження."
    override val twoFactorDisableAction: String = "Вимкнути"
    override val twoFactorNextBtn: String = "Далі"
    override val twoFactorSkipBtn: String = "Пропустити"
    override val twoFactorSaveBtn: String = "Зберегти"
    override val twoFactorStrengthWeak: String = "Слабкий"
    override val twoFactorStrengthMedium: String = "Середній"
    override val twoFactorStrengthStrong: String = "Надійний"
    override val twoFactorStrengthVeryStrong: String = "Відмінний"
    override val twoFactorCurrentHintPill: (String) -> String = { hint -> "Підказка: $hint" }
    override val twoFactorSuccessSetToast: String = "Двоетапна автентифікація успішно включена"
    override val twoFactorSuccessChangedToast: String = "Пароль успішно змінено"
    override val twoFactorSuccessDisabledToast: String = "Двоетапна автентифікація вимкнена"
    override val twoFactorPasswordFieldLabel: String = "Пароль"
    override val twoFactorConfirmFieldLabel: String = "Підтвердження пароля"
    override val twoFactorCurrentFieldLabel: String = "Поточний пароль"
    override val twoFactorHintFieldLabel: String = "Підказка (необов'язково)"
    override val settingsVibes: String = "Vibes"
    override val settingsVibesSubtitle: String = "Оформлення та ефекти чатів"
    override val settingsGroupGeneral: String = "Основне"
    override val settingsGroupExtras: String = "Додатково"
    override val settingsGroupHelp: String = "Допомога"
    override val settingsSoonBadge: String = "Скоро"
    override val appVersion: (String) -> String = { version -> "Версія програми $version" }
    // Onboarding controls
    override val onboardingGetStarted: String = "ПОЧАТИ"
    override val onboardingSkip: String = "Пропустити"
    override val a11yOnboardingPage: (Int, Int) -> String = { current, total -> "Екран $current з $total" }
    // Passcode
    override val passcodeEnterTitle: String = "Введіть пароль"
    override val passcodeEnterSubtitle: String = "Чотири цифри для входу"
    override val passcodeEnterCurrentTitle: String = "Введіть поточний пароль"
    override val passcodeCreateTitle: String = "Придумайте код-пароль"
    override val passcodeRepeatTitle: String = "Повторіть код-пароль"
    override val passcodeInfoTitle: String = "Вхід за кодом"
    override val passcodeInfoText: String = "Код-пароль додатково захистить ваші дані. При відкритті програми потрібно ввести встановлений код-пароль."
    override val passcodeEnableBtn: String = "Увімкнути код-пароль"
    override val passcodeChangeBtn: String = "Змінити код-пароль"
    override val passcodeDisableBtn: String = "Вимкнути код-пароль"
    override val passcodeDisableShort: String = "Вимкнути"
    override val passcodeRemoveTitle: String = "Вимкнути код-пароль?"
    override val passcodeRemoveText: String = "Код-пароль буде видалено, програма перестане запитувати його під час запуску."
    override val passcodeWrongCode: String = "Невірний код-пароль"
    override val passcodeMismatch: String = "Код-паролі не збігаються"
    override val a11yPasscodeLock: String = "Захист кодом-паролем"
    override val a11yPasscodeBackspace: String = "Видалити цифру"
    override val a11yPasscodeDigit: (String) -> String = { digit -> "Цифра $digit" }
    // Nickname screen
    override val nicknameHint: String = "Від 1 до 32 символи. Ім'я можна змінити пізніше в установках."

    // Shared UI components (button, text field, OTP, toast, inline keyboard)
    override val a11yLoading: String = "Завантаження"
    override val a11yOtpInput: String = "Код підтвердження"
    override val a11yOtpDigit: (Int, Int) -> String = { position, total -> "Цифра $position з $total" }
    override val a11yOtpDigitEmpty: (Int, Int) -> String = { position, total -> "Цифра $position із $total, не введена" }
    override val a11yFieldError: (String) -> String = { error -> "Помилка: $error" }
    override val a11yClearField: String = "Очистити поле"
    override val a11yInlineButtonLink: String = "Відкриває зовнішнє посилання"
    override val a11yInlineButtonLoading: String = "Виконується запит"

    // Link confirmation dialog & inline formatting (a11y)
    override val linkDialogTitle: String = "Відкрити посилання?"
    override val linkDialogSubtitle: String = "Ви переходите на зовнішній сайт"
    override val linkDialogSecure: String = "Захищене з'єднання"
    override val linkDialogInsecure: String = "З'єднання без шифрування"
    override val linkDialogOpen: String = "Перейти"
    override val linkDialogCancel: String = "Скасування"
    override val a11yLinkChip: (String) -> String = { domain -> "Посилання на $domain" }
    override val a11ySpoilerHidden: String = "Прихований текст. Натисніть, щоб показати"
    override val a11ySpoilerRevealed: String = "Прихований текст показано"
    override val a11yQuote: String = "Цитата"
    override val formatInlineQuoteWrap: (String) -> String = { text -> "«$text»" }

    // Account settings screen
    override val accountTitle: String = "Обліковий запис"
    override val accountSectionProfile: String = "Профіль"
    override val accountUsernameLabel: String = "Користувач"
    override val accountNicknameLabel: String = "Нікнейм"
    override val accountNotSet: String = "Не заданий"
    override val accountNoName: String = "Без імені"
    override val accountSectionAbout: String = "Про себе"
    override val accountBioLabel: String = "Опис профілю"
    override val accountBioPlaceholder: String = "Напишіть трохи про себе."
    override val accountPrivacyFootPrefix: String = "Хто побачить ваш статус «Про себе» — налаштовується в"
    override val accountPrivacyFootLink: String = "налаштуваннях приватності"
    override val accountPrivacyFootSuffix: String = "."
    override val accountLogoutTitle: String = "Вийти з облікового запису"
    override val accountLogoutSubtitle: String = "Локальні чернетки та кеш будуть видалені"
    override val accountLogoutDialogTitle: String = "Вийти з облікового запису?"
    override val accountLogoutDialogText: String = "Щоб повернутися, потрібно знову увійти."
    override val a11yEditProfile: String = "Змінити профіль"
    override val a11yCopy: String = "Копіювати"

    // Notification settings
    override val notifSectionGeneral: String = "Повідомлення"
    override val notifSectionSound: String = "Звук"
    override val notifMuteAll: String = "Заглушити всі чати"
    override val notifMuteAllDesc: String = "Пуші не приходять ні від кого"
    override val notifAutoMute: String = "Автомут нових чатів"
    override val notifAutoMuteDesc: String = "Нові співрозмовники починають без звуку"
    override val notifAutoMuteFootnote: String = "Існуючі чати не змінюються. Увімкнути сповіщення для конкретного чату можна в меню."
    override val notifSoundTitle: String = "Звук повідомлень"
    override val notifSoundSilent: String = "Без звуку"
    override val notifSoundDefault: String = "Системний"
    override val notifSoundCustom: String = "Вибраний звук"
    override val notifSoundFootnote: String = "Звук діє лише на цьому пристрої. Налаштування Android та режим \"Не турбувати\" мають пріоритет."
    override val notifNoServerResponse: String = "Немає відповіді на сервер. Перевірте з'єднання та спробуйте ще раз."
    override val notifNoPicker: String = "У пристрої немає програми для вибору звуку."
    override val notifSyncing: String = "Синхронізація із сервером…"

    // Power saving settings
    override val powerSectionMode: String = "Режим"
    override val powerEnableNow: String = "Включити економію зараз"
    override val powerEnableNowDesc: String = "Ефекти відключаються відразу"
    override val powerAutoTitle: String = "Вмикати за рівнем батареї"
    override val powerAutoDesc: String = "Автоматично при низькому заряді"
    override val powerThreshold: String = "Поріг включення"
    override val powerSectionDisable: String = "Що відключати"
    override val powerLiquid: String = "Рідке скло"
    override val powerBlur: String = "Розмиття панелей"
    override val powerGlow: String = "Фонове сяйво"
    override val powerPreviews: String = "Анімація GIF та стікерів"
    override val powerFootnote: String = "При заряді вище за поріг ефекти повертаються, якщо економія не включена вручну."

    // Language settings
    override val languageSearch: String = "Пошук мови"
    override val languageSectionTitle: String = "Мова інтерфейсу"
    override val languageNotFound: String = "Мову не знайдено"
    override val languageFootnote: String = "Мова застосовується одразу до всього додатка."

    // Two-factor: server flow and sign-in challenge
    override val twoFactorCheckingServer: String = "Перевіряємо стан на сервері."
    override val twoFactorNoServerResponse: String = "Немає відповіді на сервер. Установки не підтверджені."
    override val twoFactorDisconnected: String = "Немає з'єднання з сервером"
    override val twoFactorPasswordTooLong: String = "Пароль занадто довгий (максимум 72 байти)"
    override val twoFactorChallengeTitle: String = "Двофакторний захист"
    override val twoFactorChallengeSubtitle: String = "Введіть пароль, який ви встановили у налаштуваннях безпеки"
    override val twoFactorShowHint: String = "Показати підказку"
    override val twoFactorYourHint: String = "Ваша підказка"
    override val twoFactorForgotPassword: String = "Забули свій пароль?"
    override val twoFactorSignInBtn: String = "Увійти"
    override val twoFactorAttemptsLeft: (Int) -> String = { n -> "залишилося спроб: $n" }
    override val twoFactorForgotUnavailable: String = "Скидання другого фактора за одним кодом підтвердження недоступне. Зверніться на підтримку."
    override val a11yShowPassword: String = "Показати пароль"
    override val a11yHidePassword: String = "Приховати пароль"

    // Two-factor: резервная почта и сброс пароля
    override val twoFactorRecoveryEmailBtn: String = "Резервна пошта"
    override val twoFactorRecoveryEmailNotSet: String = "Не задано"
    override val twoFactorRecoveryEmailFootnote: String = "На резервну пошту прийде код, якщо ви забудете пароль другого фактора. Вкажіть адресу, відмінну від пошти облікового запису."
    override val twoFactorRecoveryEmailTitle: String = "Резервна пошта"
    override val twoFactorRecoveryEmailSubtitle: String = "Введіть адресу — ми надішлемо код підтвердження."
    override val twoFactorRecoveryEmailCurrent: (String) -> String = { email -> "Поточна адреса: $email" }
    override val twoFactorRecoveryEmailFieldLabel: String = "Резервний e-mail"
    override val twoFactorRecoveryEmailInvalid: String = "Неправильний формат адреси"
    override val twoFactorRecoveryEmailSendCodeBtn: String = "Надіслати код"
    override val twoFactorRecoveryEmailRemoveBtn: String = "Видалити резервну пошту"
    override val twoFactorRecoveryCodeTitle: String = "Підтвердьте адресу"
    override val twoFactorRecoveryCodeSubtitle: (String) -> String = { masked -> "Код підтвердження надіслано на $masked" }
    override val twoFactorRecoveryConfirmBtn: String = "Підтвердити"
    override val twoFactorResendCodeBtn: String = "Надіслати код знову"
    override val twoFactorRecoveryEmailSavedToast: String = "Резервна пошта збережена"
    override val twoFactorRecoveryEmailRemovedToast: String = "Резервна пошта видалена"
    override val twoFactorResetBtn: String = "Скинути пароль"
    override val twoFactorResetTitle: String = "Скидання пароля"
    override val twoFactorResetSubtitle: (String) -> String = { masked -> "Код для скидання надіслано на $masked" }
    override val twoFactorResetConfirmBtn: String = "Скинути та увійти"
    override val twoFactorResetSettingsConfirmBtn: String = "Скинути пароль"
    override val twoFactorResetWarning: String = "Після скидання другий фактор буде вимкнено. Увімкніть його знову і введіть новий пароль."
    override val twoFactorResetNoEmail: String = "Резервна пошта не вказана, тому скинути пароль не можна. Зверніться на підтримку."
    override val twoFactorResetFailed: String = "Не вдалося скинути пароль. Спробуйте пізніше."
    override val twoFactorResetDoneToast: String = "Другий фактор вимкнено"

    // Emoji / sticker / GIF panel
    override val gifSearchPlaceholder: String = "Пошук GIF"
    override val gifNotFound: String = "GIF не знайдено"
    override val gifLoadFailed: String = "Неможливо завантажити GIF"
    override val gifSendCd: String = "Надіслати GIF"
    override val panelTabEmoji: String = "Емодзі"
    override val panelTabStickers: String = "Стікери"
    override val panelTabGifs: String = "GIF"
    override val panelRecent: String = "Недавні"

    // Passcode settings (status page in the 2FA style)
    override val passcodeStatusEnabledBadge: String = "Код-пароль увімкнено"
    override val passcodeStatusEnabledDesc: String = "При кожному запуску програма просить 4-значний код. Без нього чати не відкрити."
    override val passcodeBullet1Title: String = "Захист під час запуску"
    override val passcodeBullet1Desc: String = "Код запитується щоразу, коли ви відкриваєте Vibe"
    override val passcodeBullet2Title: String = "Зберігається лише на пристрої"
    override val passcodeBullet2Desc: String = "Код зашифрований і ніколи не надсилається на сервер"
    override val passcodeBullet3Title: String = "Не замінює 2FA"
    override val passcodeBullet3Desc: String = "Вхід з інших пристроїв захищає двофакторний захист"
    override val passcodeStepCurrentSubtitle: String = "Підтвердіть, що це ви"
    override val passcodeStepNewSubtitle: String = "Виберіть 4 цифри, які легко запам'ятати"
    override val passcodeStepRepeatSubtitle: String = "Введіть код ще раз, щоб не помилитись"
    override val passcodeSavedToast: String = "Код-пароль збережено"
    override val passcodeRemovedToast: String = "Код-пароль вимкнено"

    // Power saving threshold control
    override val powerThresholdHint: (Int) -> String = { n -> "Ефекти відключаться, коли заряд опуститься до $n%" }
    override val powerThresholdPresetCd: (Int) -> String = { n -> "Поріг $n%" }

// Two-factor edit screen
    override val twoFactorCurrentPasswordLabel: String = "Поточний пароль"
    override val twoFactorNewPasswordLabel: String = "Новий пароль"
    override val twoFactorRepeatPasswordLabel: String = "Повторіть пароль"
    override val twoFactorHintNoPassword: String = "Підказка не повинна містити сам пароль"
    override val twoFactorHintTooLongShort: String = "Занадто довга"
    override val twoFactorHintPublicDesc: String = "Побачить кожен, хто спробує увійти"
    override val twoFactorDisableButton: String = "Вимкнути двоетапну автентифікацію"
    override val twoFactorAdditionalPasswordTitle: String = "Додатковий пароль"
    override val twoFactorAdditionalPasswordDesc: String = "Після введення коду з листа знадобиться цей пароль. Навіть якщо хтось отримає доступ до вашої пошти, увійти він не зможе."
    override val passwordStrengthWeak: String = "Слабкий"
    override val passwordStrengthMedium: String = "Середній"
    override val passwordStrengthGood: String = "Хороший"
    override val passwordStrengthStrong: String = "Відмінний"
    override val twoFactorDisableDialogTitle: String = "Вимкнути захист?"
    override val twoFactorDisableDialogDesc: String = "Для входу знову буде достатньо лише коду з листа."

    // Privacy option screen
    override val privacyValueEverybody: String = "Усі"
    override val privacyValueNobody: String = "Ніхто"
    override val privacyValueSelected: String = "Обрані"
    override val privacySelectUsers: String = "Вибрати користувачів"
    override val privacySelectUsersRuleDesc: String = "Виберіть користувачів, до яких застосовуватиметься це правило."

    // Inline video player & media covers
    override val videoPlaybackFailed: String = "Не вдалося відтворити відео"
    override val videoScaleCd: String = "Масштаб відео"
    override val videoCoverCd: String = "Обкладинка відео"
    override val videoNoFrameCd: String = "Відео без доступного кадру"
    override val videoPlayCd: String = "Відтворити відео"
    override val sampleText: String = "Приклад тексту"
    override val a11yMuteSound: String = "Вимкнути звук"
    override val a11yUnmuteSound: String = "Увімкнути звук"

    // Circle recording errors
    override val circleCameraInitFailed: String = "Не вдалося ініціалізувати камеру"
    override val circleNoCamera: String = "На пристрої немає доступної камери"
    override val circleCameraUnavailable: (String) -> String = { msg -> "Камера недоступна: $msg" }
    override val circleCameraNotReady: String = "Камера ще не готова"
    override val circleRecordStartFailedMsg: (String) -> String = { msg -> "Не вдалося почати запис: $msg" }
    override val circleRecordFailedMsg: (Int) -> String = { code -> "Не вдалося записати: код $code" }
    override val circleEmptyRecord: String = "Порожній запис"

    // Emoji categories
    override val emojiCategorySmileys: String = "Смайлики"
    override val emojiCategoryGestures: String = "Жести та люди"
    override val emojiCategoryHearts: String = "Серця та символи"
    override val emojiCategoryAnimals: String = "Тварини та природа"
    override val emojiCategoryFood: String = "Їжа та напої"
    override val emojiCategoryActivities: String = "Активності"
    override val emojiCategoryTravel: String = "Подорожі"
    override val emojiCategoryObjects: String = "Об'єкти"

    // Download helper & notifications
    override val errorGeneric: String = "Сталася помилка"
    override val downloadFileDescription: String = "Завантаження файлу з Vibe"
    override val downloadStartedToast: String = "Завантаження розпочалося..."
    override val downloadErrorToast: (String) -> String = { err -> "Помилка завантаження: $err" }
    override val downloadMultipleToast: (Int) -> String = { count -> "Завантаження: $count файл(ів)" }
    override val notifMe: String = "Я"
    override val unitSecondShort: String = "с"

    override val locale: String = "uk"
}

object ByStrings : VibeStrings {
    override val createAccount: String = "Стварэнне акаўнта"
    override val welcomeBack: String = "З вяртаннем"
    override val emailLabel: String = "ПОШТА"
    override val usernameLabel: String = "ЮЗЭРНЭЙМ"
    override val emailInvalidFormat: String = "НЕВЕРНЫ ФАРМАТ"
    override val continueBtn: String = "Працягваць"
    override val verificationTitle: String = "Увядзіце код"
    override val verificationSubtitle: (String) -> String = { email -> "Мы адправілі 6-значны код на вашу пошту\n$email" }
    override val verifyBtn: String = "ПАДЦЯРЗІЦЬ"
    override val verifyLoading: String = "Праверка ..."
    override val codeInvalid: String = "Няверны код"
    override val verificationSubtitleApp: String = "Код адпраўлены ў дадатак Vibe на іншым аўтарызаваным прыладзе.\nАдкрыйце чат з Vibe cat, каб убачыць яго"
    override val nicknameTitle: String = "Як вас клічуць?"
    override val nicknameLabel: String = "ВАШ НІКНЭЙМ"
    override val saveBtn: String = "Працягваць"
    override val saveLoading: String = "ЗАХОЎВАННЕ..."
    override val errorSaving: String = "Памылка пры захаванні"
    override val tabChats: String = "Чаты"
    override val tabSettings: String = "Налады"
    override val tabProfile: String = "Профіль"
    override val profileTitle: String = "Профіль"
    override val deletedAcc: String = "Выдалены акаўнт"
    override val statusOnline: String = "У сетцы"
    override val statusBot: String = "Бот"
    override val statusUnknown: String = "Невядома"
    override val actionForward: String = "Пераслаць"
    override val badgeVerified: String = "Карыстальнік верыфікаваны камандай Vibe."
    override val badgeDeveloper: String = "Член каманды распрацоўшчыкаў Vibe."
    override val badgeBot: String = "Проста бот."
    override val freezedAcc: String = "Акаўнт замарожаны за парушэнне правілаў."
    override val bannedAcc: String = "Акаўнт заблакаваны за парушэнне правілаў."
    override val aboutLabel: String = "Апісанне"
    override val registerDateLabel: String = "Дата рэгістрацыі"
    override val btnTheme: String = "Тэма"
    override val themeDark: String = "Цёмная"
    override val themeLight: String = "Светлая"
    override val btnLanguage: String = "Мова"
    override val btnLogout: String = "Выйсці з акаўнта"
    override val logoutConfirmTitle: String = "Вынахад з акаўнта"
    override val logoutConfirmText: String = "Вы ўпэўненыя, што хочаце выйсці з акаўнта?"
    override val logoutCancel: String = "Адмена"
    override val logoutConfirm: String = "Выйсці"
    override val userLabel: String = "Карыстальнік"
    override val chatsTitle: String = "Чаты"
    override val chatsEmptyTitle: String = "Няма чатаў"
    override val chatsEmptySubtitle: String = "Пачніце перапіску!"
    override val searchPlaceholder: String = "Пошук..."
    override val globalSearchResults: String = "Глабальны пошук"
    override val typing: String = "Друкуе"
    override val connecting: String = "Злучэнне"
    override val waitingForNetwork: String = "Чаканне сеткі"
    override val monthsShort: List<String> = listOf("сту", "лют", "сак", "кра", "мая", "чэр", "ліп", "жнв", "вер", "кас", "ліс", "сне")
    override val dateToday: String = "Сёння"
    override val dateYesterday: String = "Учора"
    override val lastSeenRecently: String = "Быў(ла) нядаўна"
    override val lastSeenLongAgo: String = "Быў(ла) вельмі даўно"
    override val lastSeenInWeek: String = "Быў(ла) на гэтым тыдні"
    override val lastSeenInMonth: String = "Быў(ла) у гэтым месяцы"
    override val lastSeenToday: (String) -> String = { time -> "Быў(а) ​​сёння ў $time" }
    override val lastSeenYesterday: (String) -> String = { time -> "Быў(ла) учора ў $time" }
    override val lastSeenDate: (String, String) -> String = { date, time -> "Быў(а) ​​$date у $time" }
    override val backBtn: String = "Назад"
    override val draftLabel: String = "Чарнавік:"
    override val messagePlaceholder: String = "Паведамленне..."
    override val replyTo: String = "Адказаць"
    override val emptyChat: String = "Тут пакуль пуста..."
    override val chatHistoryCleared: String = "Гісторыя ачышчана"
    override val chatHistoryEmpty: String = "Гісторыя пустая"
    override val onboardingPages: List<Pair<String, String>> = listOf(
        "Бяспека звестак" to "Ваша камунікацыя абаронена шыфраваннем ваеннага ўзроўню.",
        "Хуткасць святла" to "Пратаколы новага пакалення для імгненнай дастаўкі паведамленняў.",
        "Універсальная сінхранізацыя" to "Уся ваша гісторыя звестак даступная на кожнай прыладзе.",
        "Глабальныя супольнасці" to "Маштабуйце супольнасці да мільёнаў актыўных удзельнікаў.",
        "Унікальны Вайб" to "Наладзьце кожны аспект вашага месенджара пад сябе."
    )
    override val selectedMessagesCount: (Int) -> String = { count -> "Выбрана: $count" }
    override val deleteMessagesTitle: String = "Выдаліць паведамленні?"
    override val deleteMessagesText: (Int) -> String = { count -> "Вы збіраецеся выдаліць $count паведамленняў." }
    override val deleteForEveryone: (String) -> String = { name -> "Таксама выдаліць для $name" }
    override val deleteForEveryoneAlsoMine: (String) -> String = { name -> "Таксама выдаліць свае паведамленні для $name" }
    override val deleteBtn: String = "Выдаліць"
    override val cancelBtn: String = "Адмена"
    override val forwardMessageTitle: String = "Пераслаць паведамленне"
    override val noRecentChats: String = "Няма нядаўніх чатаў"
    override val editMessageTitle: String = "Рэдагаванне паведамлення"
    override val sendBtn: String = "Адправіць"
    override val forwardedFrom: (String) -> String = { name -> "Пераслана ад $name" }
    override val replyDefault: String = "Адказ"
    override val pinnedMessage: String = "Замацаванае паведамленне"
    override val pinnedMessages: String = "Замацаваныя паведамленні"
    override val pinMessage: String = "Замацаваць паведамленне"
    override val pinMessageConfirm: String = "Вы сапраўды хочаце замацаваць гэтае паведамленне?"
    override val unpinMessage: String = "Адмацаваць паведамленне"
    override val unpinMessageConfirm: String = "Вы сапраўды хочаце адмацаваць гэтае паведамленне?"
    override val unpinAll: String = "Адмацаваць усё"
    override val unpinAllConfirm: String = "Адмацаваць усе паведамленні ў гэтым чаце?"
    override val forBoth: (String) -> String = { name -> "Таксама для $name" }
    override val pin: String = "Замацаваць"
    override val unpin: String = "Адмацаваць"
    override val you: String = "Вы"
    override val edit: String = "Змяніць"
    override val usernameTaken: String = "Гэты юзернейм ужо заняты"
    override val usernameAvailable: String = "Карыстальнік свабодны"
    override val usernameDescription: String = "Вы можаце выбраць унікальнае імя карыстальніка ў Vibe. Калі вы гэта зробіце, іншыя людзі змогуць знайсці вас па гэтым імені і звязацца з вамі, не ведаючы вашага нумара тэлефона."
    override val usernameMinLength: String = "Мінімальная даўжыня 4 знака"
    override val nicknameDescription: String = "Ваша імя, якое будзе адлюстроўвацца ўсім карыстальнікам. Паспрабуйце абраць вядомае імя, каб сябры маглі лёгка вас знайсці."
    override val bioDescription: String = "Напішыце крыху пра сябе. Гэтая інфармацыя будзе бачная іншым карыстальнікам у вашым профілі."
    override val doneBtn: String = "Гатова"
    override val userRestrictedMessaging: String = "Карыстальнік абмежаваў круг зносін"
    override val userHiddenAccount: String = "Карыстальнік схаваў акаўнт"
    override val editedLabel: String = "(зменена)"
    override val muteNotifications: String = "Выключыць апавяшчэння"
    override val unmuteNotifications: String = "Уключыць апавяшчэння"
    override val pinnedMessageSystemText: (String, String) -> String = { sender, content -> "$sender замацаваў(ла) паведамленне: \"$content\"" }
    override val privacyScreenTitle: String = "Канфідэнцыяльнасць"
    override val privacyTwoFactor: String = "Падвойная аўтэнтыфікацыя"
    override val privacyPasscodeLogin: String = "Уваход па кодзе"
    override val privacyBlocked: String = "Заблакаваныя"
    override val privacyActivityTitle: String = "Статус актыўнасці"
    override val privacyActivityDesc: String = "Хто можа бачыць, калі вы ў апошні раз былі ў сетцы. Калі вы схаваеце свой статус актыўнасці, вы не зможаце бачыць статус іншых карыстальнікаў (будзе адлюстроўвацца прыкладны час)."
    override val privacyAvatarTitle: String = "Аватарка"
    override val privacyAvatarDesc: String = "Хто можа бачыць вашу аватарку. Для астатніх будзе адлюстроўвацца першая літара вашага імя на сінім фоне."
    override val privacyForwardedTitle: String = "Перасланыя паведамленні"
    override val privacyForwardedDesc: String = "Хто можа пераходзіць на ваш профіль з дасланых паведамленняў."
    override val privacyMessagesTitle: String = "Паведамлення"
    override val privacyMessagesDesc: String = "Хто можа адпраўляць вам паведамленні. Карыстальнікі, якім гэта забаронена, убачаць надпіс \"Карыстальнік абмежаваў круг зносін\"."
    override val privacyStatusTitle: String = "Статус"
    override val privacyStatusDesc: String = "Хто можа бачыць блок \"Пра сябе\" ў вашым профілі."

    // Report dialog
    override val reportTitle: String = "Паскардзіцца"
    override val reportSubtitle: String = "Абярыце прычыну. Скарга ананімная, аўтар яе не ўбачыць."
    override val reportStepLabel: (Int, Int) -> String = { current, total -> "Крок $current з $total" }
    override val reportReasonSpam: String = "Спам"
    override val reportReasonSpamDesc: String = "Рэклама, рассылкі, накрутка"
    override val reportReasonFraud: String = "Махлярства"
    override val reportReasonFraudDesc: String = "Падман, фішынг, схемы з грашыма"
    override val reportReasonDrugs: String = "Наркотыкі"
    override val reportReasonDrugsDesc: String = "Продаж ці рэклама забароненых рэчываў"
    override val reportReasonWeapons: String = "Зброя"
    override val reportReasonWeaponsDesc: String = "Гандаль зброяй і выбухоўкай"
    override val reportReasonPorn: String = "Парнаграфія"
    override val reportReasonPornDesc: String = "Матэрыялы для дарослых без папярэджання"
    override val reportReasonCsam: String = "Матэрыялы з дзецьмі (CSAM)"
    override val reportReasonCsamDesc: String = "Сэксуалізаваны кантэнт з непаўналетнімі"
    override val reportReasonViolence: String = "Гвалт"
    override val reportReasonViolenceDesc: String = "Пагрозы, жорсткасць, заклікі да шкоды"
    override val reportReasonHarassment: String = "Траўля"
    override val reportReasonHarassmentDesc: String = "Абразы, пераслед, шантаж"
    override val reportReasonHate: String = "Распальванне нянавісці"
    override val reportReasonHateDesc: String = "Нападкі па прыкмеце расы, рэлігіі, полу"
    override val reportReasonFakeAccount: String = "Фэйкавы акаўнт"
    override val reportReasonFakeAccountDesc: String = "Выдае сябе за іншага чалавека"
    override val reportReasonMisinfo: String = "Ілжывая інфармацыя"
    override val reportReasonMisinfoDesc: String = "Небяспечныя чуткі і дэзінфармацыя"
    override val reportCriticalNotice: String = "Такія скаргі мы разглядаем у прыярытэтным парадку і перадаем у профільныя органы."
    override val reportDetailsTitle: String = "Раскажыце падрабязней"
    override val reportDetailsHint: String = "Апішыце сітуацыю: што адбылося і дзе. Гэта дапаможа нам прыняць меры хутчэй."
    override val reportCommentPlaceholder: String = "Каментар (неабавязкова)"
    override val reportCommentCounter: (Int, Int) -> String = { used, limit -> "$used / $limit" }
    override val reportChangeReason: String = "Іншая прычына"
    override val reportSubmitBtn: String = "Адправіць скаргу"
    override val reportCancelBtn: String = "Адмена"
    override val reportSentTitle: String = "Скарга адпраўлена"
    override val reportSentDesc: String = "Дзякуй. Мадэратары вывучаць зварот і прымуць рашэнне."
    override val reportDoneBtn: String = "Гатова"
    override val a11yReportClose: String = "Закрыць"

    // Privacy exceptions picker
    override val privacyExceptionsTitle: String = "Выключэнні"
    override val privacyExceptionsHint: String = "Абярыце, на каго правіла не распаўсюджваецца"
    override val privacyExceptionsSelected: (Int) -> String = { count -> "Выбрана: $count" }
    override val privacyExceptionsSelectAll: String = "Выбраць усіх"
    override val privacyExceptionsClearAll: String = "Зняць выбар"
    override val privacyExceptionsEmptyTitle: String = "Няма каго абраць"
    override val privacyExceptionsEmptyDesc: String = "Тут з'явяцца людзі, з якімі ёсць чаты."
    override val a11yExceptionToggle: (String) -> String = { name -> "Выбраць $name" }
    override val a11yExceptionRemove: (String) -> String = { name -> "Прыбраць $name з выбраных" }
    override val settingsPrivacy: String = "Прыватнасць"
    override val settingsAccount: String = "Акаўнт"
    override val settingsDevices: String = "Прылады"
    override val settingsPasscode: String = "Код-пароль"
    override val usernameCopied: String = "Юзернейм скапіяваны"
    override val addAvatar: String = "Дадаць аватарку"
    override val choosePhoto: String = "Выбраць фота"
    override val switchSignIn: String = "Ужо ёсць акаўнт? Увайсці"
    override val switchSignUp: String = "Няма акаўнта? Стварыць"
    override val authTabSignUp: String = "Рэгістрацыя"
    override val authTabSignIn: String = "Уваход"
    override val authChecking: String = "Правяраем..."
    override val authEmailAvailable: String = "Пошта вольная"
    override val authEmailTaken: String = "ГЭТА ПОШТА ЗАНЯТА"
    override val authUsernameTakenShort: String = "ГЭТЫ ЮЗЕРНЕЙМ ЗАНЯТЫ"
    override val authUsernameAvailable: (String) -> String = { username -> "@$username вольны" }
    override val authRegisterFailed: String = "Не ўдалося стварыць акаўнт"
    override val authLoginFailed: String = "Не ўдалося ўвайсці"
    override val authUsernameCounter: (Int, Int) -> String = { current, max -> "$current/$max" }
    override val languageName: String = "Беларуская"
    override val blockedTitle: String = "Заблакаваныя"
    override val blockedSearchPlaceholder: String = "Пошук карыстальнікаў..."
    override val blockedClearSearch: String = "Ачысціць пошук"
    override val blockedEmptyTitle: String = "Няма заблакаваных"
    override val blockedEmptyDesc: String = "Тут будуць адлюстроўвацца карыстачы, якіх вы заблакавалі."
    override val blockedSearchEmptyTitle: String = "Нічога не знойдзена"
    override val blockedSearchEmptyDesc: (String) -> String = { query -> "Па запыце «$query» карыстальнікаў не знойдзена" }
    override val blockedUnblockBtn: String = "Разблакаваць"
    override val blockedUnblockConfirmTitle: String = "Разблакаваць?"
    override val blockedUnblockConfirmText: (String) -> String = { name -> "$name зноў зможа пісаць вам і бачыць ваш профіль." }
    override val blockedUnblockedToast: String = "Карыстальнік разблакаваны"
    override val blockedUserFallback: (Int) -> String = { id -> "Карыстальнік #$id" }
    override val accountDeleted: String = "Выдалены акаўнт"
    override val accountFrozen: String = "Замарожаны акаўнт"
    override val accountBannedMessage: String = "Ваш рахунак быў заблакаваны за парушэнне правілаў."
    override val accountFrozenMessage: String = "Ваш рахунак быў замарожаны мадэратарам."
    override val typeVideo: String = "Відэа"
    override val typeVideoMessage: String = "Відэапаведамленне"
    override val typeAudio: String = "Аўдыёфайл"
    override val typeFile: String = "Файл"
    override val typeVoice: String = "Галасавое паведамленне"
    override val playerPlaylist: String = "Плэйліст чата"
    override val playerSearchTracks: String = "Пошук трэкаў"
    override val playerNowPlaying: String = "Цяпер гуляе"
    override val playerTrackFallback: String = "Аўдыёзапіс"
    override val playerTracksCount: (Int) -> String = { count ->
        val word = when {
            count % 10 == 1 && count % 100 != 11 -> "трэк"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "трэкі"
            else -> "трэкаў"
        }
        "$count $word"
    }
    override val playerQueueEmpty: String = "Чарга пустая"
    override val playerQueueEmptyHint: String = "Аўдыё з гэтага чата з'явіцца тут"
    override val playerSearchEmptyTitle: String = "Нічога не знойдзена"
    override val playerSearchEmptySubtitle: (String) -> String = { query -> "Па запыце «$query» трэкаў няма" }
    override val playerBuffering: String = "Буферызацыя…"
    override val playerTimeZero: String = "0:00"
    override val playerTimeUnknown: String = "--:--"
    override val playerSpeedFormat: (String) -> String = { value -> "$value×" }
    override val playerRepeatOff: String = "Паўтор выключаны"
    override val playerRepeatAll: String = "Паўтараць плэйліст"
    override val playerRepeatOne: String = "Паўтараць трэк"
    override val a11yPlayerPlay: String = "Прайграць"
    override val a11yPlayerPause: String = "Паўза"
    override val a11yPlayerNext: String = "Наступны трэк"
    override val a11yPlayerPrevious: String = "Папярэдні трэк"
    override val a11yPlayerRewind10: String = "Назад на 10 секунд"
    override val a11yPlayerForward10: String = "Наперад на 10 секунд"
    override val a11yPlayerShuffle: String = "Змяшаць"
    override val a11yPlayerClose: String = "Закрыць плэер"
    override val a11yPlayerExpand: String = "Разгарнуць плэер"
    override val a11yPlayerCollapse: String = "Згарнуць плэер"
    override val a11yPlayerSearch: String = "Пошук па плэйлісце"
    override val a11yPlayerSearchClose: String = "Закрыць пошук"
    override val a11yPlayerClearSearch: String = "Ачысціць пошук"
    override val a11yPlayerArtwork: String = "Вокладка трэка"
    override val a11yPlayerSpeed: (String) -> String = { value -> "Хуткасць прайгравання: $value" }
    override val a11yPlayerTrackRow: (String) -> String = { title -> "Уключыць трэк $title" }
    override val sectionPhotosVideos: String = "Фота і відэа"
    override val sectionFiles: String = "Файлы"
    override val sectionMusic: String = "Музыка"
    override val sectionVoice: String = "Галасавыя"
    override val profileSearchFiles: String = "Пошук файлаў"
    override val actionDownload: String = "Спампаваць"
    override val actionDownloadSelected: String = "Спампаваць выбраныя"
    override val formatCopy: String = "Скапіяваць"
    override val formatCut: String = "Выразаць"
    override val formatFormat: String = "Фарматаваць"
    override val formatBold: String = "Тоўсты"
    override val formatItalic: String = "Курсіў"
    override val formatBoldItalic: String = "Тоўсты курсіў"
    override val formatStrikethrough: String = "Закрэслены"
    override val formatUnderline: String = "Падкрэслены"
    override val formatMonospace: String = "Манашырынны"
    override val formatLink: String = "Спасылка"
    override val formatTextColor: String = "Колер тэксту"
    override val formatSpoiler: String = "Спойлер"
    override val formatQuote: String = "Цытата"
    override val formatLinkUrlHint: String = "Увядзіце URL"
    override val formatColorHint: String = "Увядзіце HEX (напр. #FF5733)"
    override val formatPreview: String = "Прадпрагляд"
    override val formatCopied: String = "Скапіравана"
    override val formatReadMore: String = "Чытаць далей"
    override val formatCollapse: String = "Згарнуць"
    override val attachPhotoVideo: String = "Фота і відэа"
    override val attachFile: String = "Файл"
    override val attachTitle: String = "Укладанні"
    override val photoViewer: String = "Прагляд фота"
    override val photoOf: (Int, Int) -> String = { current, total -> "$current з $total" }
    override val actionCopy: String = "Скапіяваць тэкст"
    override val actionReport: String = "Паскардзіцца"


    // Chat header
    override val chatSearchPlaceholder: String = "Пошук па паведамленнях…"
    override val chatSearchNoResults: String = "Нічога не знойдзена"
    override val chatSearchCounter: (Int, Int) -> String = { current, total -> "$current з $total" }
    override val chatSearchClose: String = "Закрыць пошук"
    override val chatSearchClear: String = "Ачысціць запыт"
    override val chatSearchByDate: String = "Пошук па даце"
    override val chatSearchNext: String = "Наступны вынік"
    override val chatSearchPrev: String = "Папярэдні вынік"
    override val chatActionSearch: String = "Пошук"
    override val chatMenu: String = "Меню чата"
    override val chatAvatar: String = "Аватар"
    override val chatOpenProfile: String = "Адкрыць профіль"
    override val chatClearSelection: String = "Зняць вылучэнне"
    override val chatStatusBlockedByMe: String = "Заблакіраваны"
    override val chatBlockUser: String = "Заблакаваць"
    override val chatUnblockUser: String = "Разблакаваць"
    override val chatUserBlockedToast: String = "Карыстальнік заблакаваны"
    override val chatPinnedCounter: (Int, Int) -> String = { current, total -> "$current/$total" }
    override val chatJumpToPinned: String = "Перайсці да замацаванага паведамлення"
    override val chatUnpinAllHint: String = "Адмацаваць усё"


    // Chat input bar
    override val inputAttachMedia: String = "Прымацаваць"
    override val inputAttachGallery: String = "Фота ці відэа"
    override val inputAttachFile: String = "Файл"
    override val inputEmojiPanel: String = "Эмодзі, стыкеры і GIF"
    override val inputSelectedMedia: (Int) -> String = { count -> "Выбрана медыя: $count" }
    override val inputClearAttachments: String = "Ачысціць укладанні"
    override val inputBlockedByMe: String = "Вы заблакавалі гэтага карыстальніка"
    override val inputAttachmentPreview: String = "Укладанне"
    override val inputCancelReply: String = "Адмяніць"
    override val voiceHoldToRecord: String = "Утрымлівайце для запісу"
    override val voiceSendRecording: String = "Адправіць галасавое паведамленне"
    override val voiceCancelRecording: String = "Адмяніць запіс"
    override val voiceLocked: String = "Запіс замацаваны"
    override val voiceSlideToCancel: String = "Змахніце налева, каб адмяніць"
    override val voiceSlideToLock: String = "Уверх - замацаваць"
    override val voiceRecordStartFailed: String = "Не ўдалося пачаць запіс галасавога паведамлення"
    override val voicePermissionRequired: String = "Для запісу патрэбен доступ да мікрафона"
    override val voiceTooShort: String = "Запіс атрымаўся занадта кароткім"

    // ПУНКТ 2 — кружки (видеосообщения)
    override val circleModeSwitchedOn: String = "Рэжым гурткоў: утрымлівайце для запісу"
    override val circleModeSwitchedOff: String = "Рэжым галасавых паведамленняў"
    override val circleRecordVideoMessage: String = "Запісаць відэапаведамленне"
    override val circleHoldOrTapHint: String = "Націсніце для запісу, утрымлівайце для хуткай здымкі"
    override val circleRecordingHint: String = "Налева - адмена · уверх - замацаваць"
    override val circleReleaseToCancel: String = "Адпусціце для адмены"
    override val circleLockedHint: String = "Націсніце, каб адправіць"
    override val circleTapToStop: String = "Націсніце, каб спыніць і адправіць"
    override val circleCameraPreparing: String = "Рыхтуем камеру…"
    override val circleMaxDurationHint: String = "Максімум 60 секунд"
    override val circlePermissionRequired: String = "Для гурткоў патрэбен доступ да камеры і мікрафона."
    override val circleTooShort: String = "Гурток атрымаўся занадта кароткім"
    override val circleCancel: String = "Адмяніць запіс"
    override val circleSend: String = "Адправіць гурток"
    override val circleSwitchCamera: String = "Змяніць камеру"
    override val circleSendFailed: String = "Не ўдалося адправіць гурток"


    // Chat list (iOS redesign)
    override val chatsSectionPinned: String = "Замацаваныя"
    override val filterAll: String = "Усё"
    override val filterUnread: String = "Непрачытаныя"
    override val filterUnreadCount: (Int) -> String = { count -> "Непрачытаныя ($count)" }
    override val chatsNoUnreadTitle: String = "Усё прачытана"
    override val chatsNoUnreadSubtitle: String = "Непрачытаных паведамленняў няма."
    override val chatsSectionAll: String = "Усе чаты"
    override val chatsCountFooter: (Int) -> String = { count -> "Чатаў: $count" }
    override val chatsEmptyHint: String = "Знайдзіце карыстальніка праз пошук вышэй"
    override val chatsSearchCancel: String = "Адмена"
    override val chatsSearchClearField: String = "Ачысціць поле пошуку"
    override val chatsSearchNoResultsTitle: String = "Нічога не знойдзена"
    override val chatsSearchNoResultsSubtitle: (String) -> String = { query -> "Няма чатаў і карыстальнікаў па запыце «$query»" }
    override val userFallback: (Int) -> String = { id -> "Карыстальнік #$id" }
    override val someoneLabel: String = "Хтосьці"
    override val a11yMutedChat: String = "Апавяшчэнні выключаны"
    override val a11yPinnedChat: String = "Чат замацаваны"
    override val actionMuteShort: String = "Без гуку"
    override val actionUnmuteShort: String = "З гукам"


    // Chat list message previews
    override val typePhoto: String = "Фатаграфія"
    override val previewVoiceMessage: (String) -> String = { duration -> "Галасавое паведамленне $duration" }
    override val previewVideoMessage: (String) -> String = { duration -> "Відэапаведамленне $duration" }
    override val previewAudioTrack: (String, String) -> String = { artist, title -> "$artist - $title" }
    override val previewAudioLoading: String = "Музыка..."
    override val previewMorePhotos: (Int) -> String = { count -> "+$count фатаграфій" }
    override val previewMoreVideos: (Int) -> String = { count -> "+$count відэа" }
    override val previewMoreWithCaption: (Int, String) -> String = { count, caption -> "+$count $caption" }
    override val typeSticker: String = "Стыкер"
    override val typeGif: String = "GIF"
    override val previewMediaCount: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "медыяфайл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "медыяфайлы"
            else -> "медыяфайлаў"
        }
        "$count $form"
    }
    override val previewMoreAudio: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "аўдыяфайл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "аўдыяфайлы"
            else -> "аўдыяфайлаў"
        }
        "$count $form"
    }
    override val previewMoreFiles: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "файл"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "файлы"
            else -> "файлаў"
        }
        "$count $form"
    }
    override val previewMoreAttachments: (Int) -> String = { count ->
        val form = when {
            count % 10 == 1 && count % 100 != 11 -> "укладанне"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "укладанні"
            else -> "укладанняў"
        }
        "$count $form"
    }
    override val actionSelectMessage: String = "Выбраць"
    // Chat message list (empty state, system messages, scroll-to-bottom)
    override val emptyChatSubtitle: String = "Напішыце першае паведамленне і гісторыя чата з'явіцца тут."
    override val emptyChatHint: String = "Пачніце размову першым"
    override val chatSystemMessageLabel: String = "Сістэмнае паведамленне"
    override val linkOpenFailed: String = "Не ўдалося адкрыць спасылку"
    override val a11yMessageList: String = "Спіс паведамленняў"
    override val a11yScrollToBottom: String = "Перайсці да апошніх паведамленняў"
    override val a11yUnreadCount: (Int) -> String = { count -> "Новых паведамленняў: $count" }
    override val unreadCountOverflow: String = "99+"
    // Chat dialogs, reactions sheet, bot & network toasts
    override val okBtn: String = "OK"
    override val actionClose: String = "Закрыць"
    override val reportSentToast: String = "Скарга паспяхова даслана"
    override val restrictionTitle: String = "Абмежаванне"
    override val restrictionUnderstood: String = "Зразумела"
    override val restrictionWhy: String = "Чаму?"
    override val datePickerTitle: String = "Абярыце дату"
    override val dateJumpNotFound: String = "Паведамленняў за гэтую дату не знойдзена"
    override val botMessageTitle: String = "Паведамленне ад бота"
    override val botLabel: String = "Бот"
    override val attachmentLabel: String = "Укладанне"
    override val fileSizeLoading: String = "Загрузка..."
    override val reactionsTitle: String = "Рэакцыі"
    override val reactionsAllTab: (Int) -> String = { count -> "Усе $count" }
    override val reactionsEmpty: String = "Пакуль няма рэакцый"
    override val voiceTrackTitleMine: String = "Вы (Галасавае паведамленне)"
    override val botCallbackTimeout: String = "Бот не адказаў за 10 секунд. Кнопкі гэтага паведамлення зноў даступныя."
    override val connectionLostToast: String = "Няма злучэння з серверам. Паспрабуйце яшчэ раз."
    override val fileOpenFailed: String = "Не атрымалася адкрыць абраны файл"
    override val maxPinnedChatsToast: (Int) -> String = { limit -> "Можна замацаваць не больш за $limit чатаў" }
    // Chat toast host
    override val toastTitleInfo: String = "Інфармацыя"
    override val toastTitleSuccess: String = "Гатова"
    override val toastTitleWarning: String = "Увага"
    override val toastTitleError: String = "Памылка"
    override val toastActionRetry: String = "Паўтарыць"
    override val toastActionUndo: String = "Адмяніць"
    override val toastCopied: String = "Скапіравана"
    override val a11yToast: (String) -> String = { text -> "Апавяшчэнне: $text" }
    override val a11yToastDismiss: String = "Схаваць апавяшчэнне"
    // Devices & sessions
    override val devicesTitle: String = "Прылады"
    override val devicesSubtitle: String = "Тут паказаны ўсе ўваходы ў ваш рахунак."
    override val devicesSessionsCount: (Int) -> String = { count ->
        val tail = count % 10
        val hundred = count % 100
        when {
            hundred in 11..14 -> "$count актыўных сеансаў"
            tail == 1 -> "$count актыўны сеанс"
            tail in 2..4 -> "$count актыўныя сеансы"
            else -> "$count актыўных сеансаў"
        }
    }
    override val devicesRefreshCd: String = "Абнавіць спіс"
    override val devicesSectionCurrent: String = "ГЭТА Прылада"
    override val devicesSectionOther: String = "ІНШЫЯ СЕАНСЫ"
    override val devicesCurrentBadge: String = "Бягучы"
    override val devicesOnlineNow: String = "У сетцы"
    override val devicesLastActiveNow: String = "Толькі што"
    override val devicesLastActiveMinutes: (Int) -> String = { minutes ->
        val tail = minutes % 10
        val hundred = minutes % 100
        val word = when {
            hundred in 11..14 -> "хвілін"
            tail == 1 -> "хвіліну"
            tail in 2..4 -> "хвіліны"
            else -> "хвілін"
        }
        "$minutes $word таму"
    }
    override val devicesLastActiveHours: (Int) -> String = { hours ->
        val tail = hours % 10
        val hundred = hours % 100
        val word = when {
            hundred in 11..14 -> "гадзін"
            tail == 1 -> "гадзіну"
            tail in 2..4 -> "гадзіны"
            else -> "гадзін"
        }
        "$hours $word таму"
    }
    override val devicesLastActiveYesterday: String = "Учора"
    override val devicesLastActiveDate: (String) -> String = { date -> "Быў(ла): $date" }
    override val devicesDateTimePattern: String = "dd.MM.yyyy, HH:mm"
    override val devicesUnknownDevice: String = "Невядомая прылада"
    override val devicesUnknownLocation: String = "Месцазнаходжанне невядома"
    override val devicesNoOtherSessions: String = "Іншых сеансаў няма"
    override val devicesNoOtherSessionsHint: String = "У рахунак выкананы ўваход толькі з гэтай прылады"
    override val devicesEmptyTitle: String = "Актыўных сеансаў няма"
    override val devicesEmptySubtitle: String = "Не атрымалася знайсці ніводнага ўваходу ў акаўнт. Паспрабуйце абнавіць спіс."
    override val devicesLoading: String = "Загружаем сеансы..."
    override val devicesLoadFailedTitle: String = "Не атрымалася загрузіць"
    override val devicesLoadFailedSubtitle: String = "Сервер не адказаў. Праверце злучэнне і паспрабуйце зноў."
    override val devicesTerminateCd: String = "Завяршыць сеанс"
    override val devicesTerminateAll: String = "Завяршыць усё"
    override val devicesTerminateTitle: String = "Завяршыць сеанс?"
    override val devicesTerminateText: (String) -> String = { name ->
        "Прылада «$name» выйдзе з акаўнта. Для паўторнага ўваходу спатрэбіцца код пацвярджэння."
    }
    override val devicesTerminateAllTitle: String = "Завяршыць усе сеансы?"
    override val devicesTerminateAllText: (Int) -> String = { count ->
        "З акаўнта выйдуць усе іншыя прылады ($count). Гэтая прылада застанецца ў сетцы."
    }
    override val devicesTerminateConfirm: String = "Завяршыць"
    override val devicesSecurityHint: String = "Не даведаецеся прыладу? Завершыце сеанс і зменіце пароль."
    // Edit profile field
    override val editFieldSave: String = "Захаваць"
    override val editFieldSaveCd: String = "Захаваць змены"
    override val editFieldPlaceholder: (String) -> String = { title -> "Увядзіце ${title.lowercase()}" }
    override val editFieldClearCd: String = "Ачысціць поле"
    override val editFieldCounter: (Int, Int) -> String = { used, max -> "$used / $max" }
    override val editFieldLimitReached: String = "Дасягнуты ліміт сімвалаў"
    override val editFieldUnsavedTitle: String = "Захаваць змены?"
    override val editFieldUnsavedText: String = "У вас ёсць незахаваныя змены. Захаваць іх перад выхадам?"
    override val editFieldUnsavedDiscard: String = "Скінуць"
    // Main container navigation (a11y)
    override val a11yTab: (String) -> String = { name -> "Укладка «$name»" }
    // Profile screen (redesign)
    override val profileSectionInfo: String = "Інфармацыя"
    override val profileSectionAppearance: String = "Афармленне"
    override val profileSectionSession: String = "Сеанс"
    override val profileCopyUsername: String = "Скапіяваць юзернейм"
    override val profileCopyUsernameHint: String = "Націсніце, каб скапіяваць"
    override val a11yAvatar: String = "Аватар профілю"
    override val a11yEditAvatar: String = "Змяніць аватар"
    override val a11yAvatarPreview: String = "Прадпрагляд аватара"
    override val a11yChoosePhoto: String = "Выбраць фота"
    override val avatarCropHint: String = "Перацягвайце і зводзіце пальцы, каб размясціць фота. Загрузіцца вобласць усярэдзіне круга."
    override val avatarPickPrompt: String = "Націсніце, каб выбраць фота"

    // Profile screen (extra)
    override val profileNotFoundTitle: String = "Карыстальнік не знойдзены"
    override val profileNotFoundDesc: (String) -> String = { username -> "$username не зарэгістраваны ў Vibe або выдаліў рахунак." }
    override val profileLoading: String = "Загружаем профіль..."
    override val profileWriteBtn: String = "Напісаць"
    override val profileBlockedTitle: String = "Вы заблакавалі гэтага карыстальніка"
    override val profileBlockedDesc: String = "Ён не можа пісаць вам і не бачыць ваш статут."
    override val a11yProfileMenu: String = "Яшчэ"
    override val a11yAvatarViewerClose: String = "Закрыць прагляд"
    // Compact number units
    override val unitCompactFormat: (String, String) -> String = { number, unit -> "$number $unit" }
    override val unitThousandShort: String = "тыс."
    override val unitMillionShort: String = "млн."
    override val unitBillionShort: String = "млрд."
    // Settings root list
    override val settingsChats: String = "Налады чатаў"
    override val settingsChatsSubtitle: String = "Тэма, шпалеры, памер тэксту"
    override val settingsPrivacySubtitle: String = "Хто бачыць вас і піша вам"
    override val settingsNotifications: String = "Апавяшчэнні"
    override val settingsNotificationsSubtitle: String = "Гукі, прэв'ю, прыярытэт"
    override val settingsPowerSaving: String = "Эканомія энергіі"
    override val settingsPowerSavingSubtitle: String = "Анімацыі і фонавая праца"
    override val settingsDevicesSubtitle: String = "Актыўныя сеансы і выхад"
    override val settingsLanguageSubtitle: String = "Мова інтэрфейсу"
    override val settingsAccountSubtitle: String = "Імя, юзернейм, апісанне"
    override val settingsSupport: String = "Падтрымка"
    override val settingsSupportSubtitle: String = "Пытанні, справаздачы аб памылках"
    override val settingsVibePro: String = "Vibe Pro"
    override val settingsVibeProSubtitle: String = "Больш лімітаў, эксклюзіўныя функцыі"
    override val settingsVibeProCta: String = "Больш падрабязна"
    // Vibe Pro screen
    override val vibeProHeroDescription: String = "Максімальныя магчымасці зносін, павялічаныя ліміты і эксклюзіўны статус у Vibe."
    override val vibeProSectionFeatures: String = "МАГЧЫМАСЦІ ПАДПІСКІ"
    override val vibeProSectionPlans: String = "ТАРЫФНЫ ПЛАН"
    override val vibeProFeatureLimitsTitle: String = "Павялічаныя ліміты"
    override val vibeProFeatureLimitsSubtitle: String = "Адпраўка файлаў да 2 ГБ, да 100 замацаваных чатаў і 20 тэчак"
    override val vibeProFeatureVoiceToTextTitle: String = "Галасавыя ў тэкст"
    override val vibeProFeatureVoiceToTextSubtitle: String = "Імгненная расшыфроўка аўдыё- і відэапаведамленняў адным дотыкам"
    override val vibeProFeatureReactionsTitle: String = "Эксклюзіўныя рэакцыі"
    override val vibeProFeatureReactionsSubtitle: String = "Аніміраваныя стыкеры, унікальныя эмоджы і эмодзі-статусы"
    override val vibeProFeatureBadgeTitle: String = "Прэміум-значок"
    override val vibeProFeatureBadgeSubtitle: String = "Адмысловы значок Pro побач з вашым імем у чатах і профілі"
    override val vibeProFeatureSpeedTitle: String = "Звышхуткая хуткасць"
    override val vibeProFeatureSpeedSubtitle: String = "Загрузка і адпраўка медыяфайлаў без абмежавання прапускной здольнасці"
    override val vibeProFeatureNoAdsTitle: String = "Поўная свабода"
    override val vibeProFeatureNoAdsSubtitle: String = "Ніякай рэкламы, прыярытэтная тэхнічная падтрымка 24/7"
    override val vibeProPlanYearly: String = "1 год"
    override val vibeProPlanYearlyPrice: String = "149 ₽ / мес"
    override val vibeProPlanYearlyDiscount: String = "−25%"
    override val vibeProPlanMonthly: String = "1 месяц"
    override val vibeProPlanMonthlyPrice: String = "199 ₽ / мес"
    override val vibeProSubscribeCta: (String) -> String = { price -> "Падлучыць Vibe Pro - $price" }
    override val vibeProAutoRenewalDisclaimer: String = "Падпіска працягваецца аўтаматычна. Адмяніць можна ў любы час."
    override val vibeProComingSoonToast: String = "Афармленне падпіскі стане даступна ў бліжэйшым абнаўленні"
    // Two-factor authentication (2FA)
    override val twoFactorTitle: String = "Двухэтапная аўтэнтыфікацыя"
    override val twoFactorSubtitle: String = "Дадатковы пароль для абароны пры ўваходзе"
    override val twoFactorDescription: String = "Вы можаце задаць дадатковы пароль, які спатрэбіцца ўводзіць пры ўваходзе з новай прылады ў дадатак да кода пацверджання."
    override val twoFactorStatusEnabled: String = "Уключана"
    override val twoFactorStatusDisabled: String = "Выключана"
    override val twoFactorEnabledBadge: String = "Абарона актыўная"
    override val twoFactorEnabledDesc: String = "Пры ўваходзе на новую прыладу запатрабуецца ўвесці гэты пароль пасля кода пацверджання."
    override val twoFactorBullet1Title: String = "Надзейная абарона"
    override val twoFactorBullet1Desc: String = "Нават калі старонні атрымае код пацверджання, ён не зможа ўвайсці ў ваш рахунак."
    override val twoFactorBullet2Title: String = "Воблачна пароль"
    override val twoFactorBullet2Desc: String = "Пароль надзейна зашыфраваны ў абароненым сховішчы"
    override val twoFactorBullet3Title: String = "Падказка для памяці"
    override val twoFactorBullet3Desc: String = "Магчымасць указаць падказку, якая дапаможа ўспомніць пароль"
    override val twoFactorSetPasswordBtn: String = "Задаць пароль"
    override val twoFactorChangePasswordBtn: String = "Змяніць пароль"
    override val twoFactorChangeHintBtn: String = "Змяніць падказку"
    override val twoFactorDisableBtn: String = "Адключыць абарону"
    override val twoFactorEnterNewPasswordTitle: String = "Новы пароль"
    override val twoFactorEnterNewPasswordSubtitle: String = "Прыдумайце пароль даўжынёй не менш за 6 сімвалаў"
    override val twoFactorRepeatPasswordTitle: String = "Паўтарыце пароль"
    override val twoFactorRepeatPasswordSubtitle: String = "Увядзіце пароль яшчэ раз для пацверджання"
    override val twoFactorEnterCurrentPasswordTitle: String = "Бягучы пароль"
    override val twoFactorEnterCurrentPasswordSubtitle: String = "Увядзіце бягучы пароль двухэтапнай аўтэнтыфікацыі"
    override val twoFactorHintTitle: String = "Падказка для пароля"
    override val twoFactorHintSubtitle: String = "Падказка дапаможа ўспомніць пароль пры неабходнасці"
    override val twoFactorHintPlaceholder: String = "Напрыклад: любімая кніга ці дата"
    override val twoFactorHintTooLong: String = "Падказка не павінна перавышаць 32 сімвалаў"
    override val twoFactorHintContainsPassword: String = "Падказка не павінна змяшчаць сам пароль"
    override val twoFactorHintPublicWarning: String = "Падказка бачная любому, хто паспрабуе ўвайсці ў ваш рахунак."
    override val twoFactorPasswordTooShort: String = "Пароль павінен змяшчаць мінімум 6 сімвалаў"
    override val twoFactorPasswordMismatch: String = "Паролі не супадаюць"
    override val twoFactorPasswordWrong: String = "Няправільны бягучы пароль"
    override val twoFactorDisableConfirmTitle: String = "Адключыць двухэтапную абарону?"
    override val twoFactorDisableConfirmDesc: String = "Для ўваходу на новых прыладах зноў будзе дастаткова толькі кода пацверджання."
    override val twoFactorDisableAction: String = "Адключыць"
    override val twoFactorNextBtn: String = "Далей"
    override val twoFactorSkipBtn: String = "Прапусціць"
    override val twoFactorSaveBtn: String = "Захаваць"
    override val twoFactorStrengthWeak: String = "Слабы"
    override val twoFactorStrengthMedium: String = "Сярэдні"
    override val twoFactorStrengthStrong: String = "Надзейны"
    override val twoFactorStrengthVeryStrong: String = "Выдатны"
    override val twoFactorCurrentHintPill: (String) -> String = { hint -> "Падказка: $hint" }
    override val twoFactorSuccessSetToast: String = "Двухэтапная аўтэнтыфікацыя паспяхова ўключана"
    override val twoFactorSuccessChangedToast: String = "Пароль паспяхова зменены"
    override val twoFactorSuccessDisabledToast: String = "Двухэтапная аўтэнтыфікацыя адключана"
    override val twoFactorPasswordFieldLabel: String = "Пароль"
    override val twoFactorConfirmFieldLabel: String = "Пацвярджэнне пароля"
    override val twoFactorCurrentFieldLabel: String = "Бягучы пароль"
    override val twoFactorHintFieldLabel: String = "Падказка (неабавязкова)"
    override val settingsVibes: String = "Vibes"
    override val settingsVibesSubtitle: String = "Афармленне і эфекты чатаў"
    override val settingsGroupGeneral: String = "Асноўнае"
    override val settingsGroupExtras: String = "Дадаткова"
    override val settingsGroupHelp: String = "Дапамога"
    override val settingsSoonBadge: String = "Хутка"
    override val appVersion: (String) -> String = { version -> "Версія прыкладання $version" }
    // Onboarding controls
    override val onboardingGetStarted: String = "ПАЧАЦЬ"
    override val onboardingSkip: String = "Прапусціць"
    override val a11yOnboardingPage: (Int, Int) -> String = { current, total -> "Экран $current з $total" }
    // Passcode
    override val passcodeEnterTitle: String = "Увядзіце код-пароль"
    override val passcodeEnterSubtitle: String = "Чатыры лічбы для ўваходу"
    override val passcodeEnterCurrentTitle: String = "Увядзіце бягучы код-пароль"
    override val passcodeCreateTitle: String = "Прыдумайце код-пароль"
    override val passcodeRepeatTitle: String = "Паўтарыце код-пароль"
    override val passcodeInfoTitle: String = "Уваход па кодзе"
    override val passcodeInfoText: String = "Код-пароль дадаткова абароніць вашыя дадзеныя. Пры адкрыцці прыкладання запатрабуецца ўвесці ўсталяваны код-пароль."
    override val passcodeEnableBtn: String = "Уключыць код-пароль"
    override val passcodeChangeBtn: String = "Змяніць код-пароль"
    override val passcodeDisableBtn: String = "Адключыць код-пароль"
    override val passcodeDisableShort: String = "Адключыць"
    override val passcodeRemoveTitle: String = "Адключыць код-пароль?"
    override val passcodeRemoveText: String = "Код-пароль будзе выдалены, прыкладанне перастане запытваць яго пры запуску."
    override val passcodeWrongCode: String = "Няверны код-пароль"
    override val passcodeMismatch: String = "Код-паролі не супадаюць"
    override val a11yPasscodeLock: String = "Абарона кодам-паролем"
    override val a11yPasscodeBackspace: String = "Выдаліць лічбу"
    override val a11yPasscodeDigit: (String) -> String = { digit -> "Лічба $digit" }
    // Nickname screen
    override val nicknameHint: String = "Ад 1 да 32 сімвалаў. Імя можна змяніць пазней у наладах."

    // Shared UI components (button, text field, OTP, toast, inline keyboard)
    override val a11yLoading: String = "Загрузка"
    override val a11yOtpInput: String = "Код пацверджання"
    override val a11yOtpDigit: (Int, Int) -> String = { position, total -> "Лічба $position з $total" }
    override val a11yOtpDigitEmpty: (Int, Int) -> String = { position, total -> "Лічба $position з $total, не ўведзена" }
    override val a11yFieldError: (String) -> String = { error -> "Памылка: $error" }
    override val a11yClearField: String = "Ачысціць поле"
    override val a11yInlineButtonLink: String = "Адкрывае знешнюю спасылку"
    override val a11yInlineButtonLoading: String = "Выконваецца запыт"

    // Link confirmation dialog & inline formatting (a11y)
    override val linkDialogTitle: String = "Адкрыць спасылку?"
    override val linkDialogSubtitle: String = "Вы пераходзіце на знешні сайт"
    override val linkDialogSecure: String = "Абароненае злучэнне"
    override val linkDialogInsecure: String = "Злучэнне без шыфравання"
    override val linkDialogOpen: String = "Перайсці"
    override val linkDialogCancel: String = "Адмена"
    override val a11yLinkChip: (String) -> String = { domain -> "Спасылка на $domain" }
    override val a11ySpoilerHidden: String = "Утоены тэкст. Націсніце, каб паказаць"
    override val a11ySpoilerRevealed: String = "Утоены тэкст паказаны"
    override val a11yQuote: String = "Цытата"
    override val formatInlineQuoteWrap: (String) -> String = { text -> "«$text»" }

    // Account settings screen
    override val accountTitle: String = "Акаўнт"
    override val accountSectionProfile: String = "Профіль"
    override val accountUsernameLabel: String = "Юзернейм"
    override val accountNicknameLabel: String = "Нікнэйм"
    override val accountNotSet: String = "Не зададзены"
    override val accountNoName: String = "Без імя"
    override val accountSectionAbout: String = "Пра сябе"
    override val accountBioLabel: String = "Апісанне профілю"
    override val accountBioPlaceholder: String = "Напішыце крыху пра сябе…"
    override val accountPrivacyFootPrefix: String = "Хто ўбачыць ваш статус «Пра сябе» - наладжваецца ў"
    override val accountPrivacyFootLink: String = "наладах прыватнасці"
    override val accountPrivacyFootSuffix: String = "."
    override val accountLogoutTitle: String = "Выйсці з акаўнта"
    override val accountLogoutSubtitle: String = "Лакальныя чарнавікі і кэш будуць выдалены"
    override val accountLogoutDialogTitle: String = "Выйсці з акаўнта?"
    override val accountLogoutDialogText: String = "Каб вярнуцца, трэба ўвайсці зноў."
    override val a11yEditProfile: String = "Змяніць профіль"
    override val a11yCopy: String = "Скапіяваць"

    // Notification settings
    override val notifSectionGeneral: String = "Апавяшчэнні"
    override val notifSectionSound: String = "Гук"
    override val notifMuteAll: String = "Заглушыць усе чаты"
    override val notifMuteAllDesc: String = "Пушы не прыходзяць ні ад каго"
    override val notifAutoMute: String = "Аўтамут новых чатаў"
    override val notifAutoMuteDesc: String = "Новыя суразмоўцы пачынаюць без гуку"
    override val notifAutoMuteFootnote: String = "Існуючыя чаты не мяняюцца. Уключыць апавяшчэнні для канкрэтнага чата можна ў яго меню."
    override val notifSoundTitle: String = "Гук апавяшчэнняў"
    override val notifSoundSilent: String = "Без гуку"
    override val notifSoundDefault: String = "Сістэмны"
    override val notifSoundCustom: String = "Абраны гук"
    override val notifSoundFootnote: String = "Гук дзейнічае толькі на гэтай прыладзе. Налады Android і рэжым \"Не турбаваць\" маюць прыярытэт."
    override val notifNoServerResponse: String = "Няма адказу сервера. Праверце злучэнне і паспрабуйце яшчэ раз."
    override val notifNoPicker: String = "На прыладзе няма прыкладання для выбару гуку."
    override val notifSyncing: String = "Сінхранізацыя з серверам…"

    // Power saving settings
    override val powerSectionMode: String = "Рэжым"
    override val powerEnableNow: String = "Уключыць эканомію зараз"
    override val powerEnableNowDesc: String = "Эфекты адключаюцца адразу"
    override val powerAutoTitle: String = "Уключаць па ўзроўні батарэі"
    override val powerAutoDesc: String = "Аўтаматычна пры нізкім зарадзе"
    override val powerThreshold: String = "Парог уключэння"
    override val powerSectionDisable: String = "Што адключаць"
    override val powerLiquid: String = "Вадкае шкло"
    override val powerBlur: String = "Размыццё панэляў"
    override val powerGlow: String = "Фонавае ззянне"
    override val powerPreviews: String = "Анімацыя GIF і стыкераў"
    override val powerFootnote: String = "Пры зарадзе вышэй парога эфекты вяртаюцца, калі эканомія не ўключана ўручную."

    // Language settings
    override val languageSearch: String = "Пошук мовы"
    override val languageSectionTitle: String = "Мова інтэрфейсу"
    override val languageNotFound: String = "Мова не знойдзена"
    override val languageFootnote: String = "Мова ўжываецца адразу да ўсёй праграмы."

    // Two-factor: server flow and sign-in challenge
    override val twoFactorCheckingServer: String = "Правяраем стан на серверы…"
    override val twoFactorNoServerResponse: String = "Няма адказу сервера. Налады не пацверджаны."
    override val twoFactorDisconnected: String = "Няма злучэння з серверам"
    override val twoFactorPasswordTooLong: String = "Пароль занадта доўгі (максімум 72 байта)"
    override val twoFactorChallengeTitle: String = "Двухфактарная абарона"
    override val twoFactorChallengeSubtitle: String = "Увядзіце пароль, які вы задалі ў наладах бяспекі"
    override val twoFactorShowHint: String = "Паказаць падказку"
    override val twoFactorYourHint: String = "Ваша падказка"
    override val twoFactorForgotPassword: String = "Забыліся пароль?"
    override val twoFactorSignInBtn: String = "Увайсці"
    override val twoFactorAttemptsLeft: (Int) -> String = { n -> "засталося спроб: $n" }
    override val twoFactorForgotUnavailable: String = "Скід другога фактару па адным кодзе пацверджання недаступны. Звернецеся ў падтрымку."
    override val a11yShowPassword: String = "Паказаць пароль"
    override val a11yHidePassword: String = "Схаваць пароль"

    // Two-factor: резервная почта и сброс пароля
    override val twoFactorRecoveryEmailBtn: String = "Рэзервовая пошта"
    override val twoFactorRecoveryEmailNotSet: String = "Не зададзена"
    override val twoFactorRecoveryEmailFootnote: String = "На рэзервовую пошту прыйдзе код, калі вы забудзеце пароль другога фактару. Укажыце адрас, адрозны ад пошты акаўнта."
    override val twoFactorRecoveryEmailTitle: String = "Рэзервовая пошта"
    override val twoFactorRecoveryEmailSubtitle: String = "Увядзіце адрас - мы адправім на яго код пацверджання."
    override val twoFactorRecoveryEmailCurrent: (String) -> String = { email -> "Бягучы адрас: $email" }
    override val twoFactorRecoveryEmailFieldLabel: String = "Рэзервовы e-mail"
    override val twoFactorRecoveryEmailInvalid: String = "Няправільны фармат адраса"
    override val twoFactorRecoveryEmailSendCodeBtn: String = "Адправіць код"
    override val twoFactorRecoveryEmailRemoveBtn: String = "Выдаліць рэзервовую пошту"
    override val twoFactorRecoveryCodeTitle: String = "Пацвердзіце адрас"
    override val twoFactorRecoveryCodeSubtitle: (String) -> String = { masked -> "Код пацверджання адпраўлены на $masked" }
    override val twoFactorRecoveryConfirmBtn: String = "Пацвердзіць"
    override val twoFactorResendCodeBtn: String = "Адправіць код зноў"
    override val twoFactorRecoveryEmailSavedToast: String = "Рэзервовая пошта захавана"
    override val twoFactorRecoveryEmailRemovedToast: String = "Рэзервовая пошта выдалена"
    override val twoFactorResetBtn: String = "Скінуць пароль"
    override val twoFactorResetTitle: String = "Скід пароля"
    override val twoFactorResetSubtitle: (String) -> String = { masked -> "Код для скіду адпраўлены на $masked" }
    override val twoFactorResetConfirmBtn: String = "Скінуць і ўвайсці"
    override val twoFactorResetSettingsConfirmBtn: String = "Скінуць пароль"
    override val twoFactorResetWarning: String = "Пасля скіду другі фактар ​​будзе адключаны. Уключыце яго зноў і задайце новы пароль."
    override val twoFactorResetNoEmail: String = "Рэзервовая пошта не зададзена, таму скінуць пароль нельга. Звернецеся ў падтрымку."
    override val twoFactorResetFailed: String = "Не ўдалося скінуць пароль. Паспрабуйце пазней."
    override val twoFactorResetDoneToast: String = "Другі фактар ​​адключаны"

    // Emoji / sticker / GIF panel
    override val gifSearchPlaceholder: String = "Пошук GIF"
    override val gifNotFound: String = "GIF не знойдзены"
    override val gifLoadFailed: String = "Не атрымалася загрузіць GIF"
    override val gifSendCd: String = "Адправіць GIF"
    override val panelTabEmoji: String = "Эмодзі"
    override val panelTabStickers: String = "Сцікеры"
    override val panelTabGifs: String = "GIF"
    override val panelRecent: String = "Нядаўнія"

    // Passcode settings (status page in the 2FA style)
    override val passcodeStatusEnabledBadge: String = "Код-пароль уключаны"
    override val passcodeStatusEnabledDesc: String = "Пры кожным запуску прыкладанне просіць 4-значны код. Без яго чаты не адкрыць."
    override val passcodeBullet1Title: String = "Абарона пры запуску"
    override val passcodeBullet1Desc: String = "Код запытваецца кожны раз, калі вы адкрываеце Vibe"
    override val passcodeBullet2Title: String = "Захоўваецца толькі на прыладзе"
    override val passcodeBullet2Desc: String = "Код зашыфраваны і ніколі не адпраўляецца на сервер"
    override val passcodeBullet3Title: String = "Не замяняе 2FA"
    override val passcodeBullet3Desc: String = "Уваход з іншых прылад абараняе двухфактарная абарона"
    override val passcodeStepCurrentSubtitle: String = "Пацвердзіце, што гэта вы"
    override val passcodeStepNewSubtitle: String = "Абярыце 4 лічбы, якія лёгка запомніць"
    override val passcodeStepRepeatSubtitle: String = "Увядзіце код яшчэ раз, каб не памыліцца"
    override val passcodeSavedToast: String = "Код-пароль захаваны"
    override val passcodeRemovedToast: String = "Код-пароль адключаны"

    // Power saving threshold control
    override val powerThresholdHint: (Int) -> String = { n -> "Эфекты адключацца, калі зарад апусціцца да $n%" }
    override val powerThresholdPresetCd: (Int) -> String = { n -> "Парог $n%" }

// Two-factor edit screen
    override val twoFactorCurrentPasswordLabel: String = "Бягучы пароль"
    override val twoFactorNewPasswordLabel: String = "Новы пароль"
    override val twoFactorRepeatPasswordLabel: String = "Паўтарыце пароль"
    override val twoFactorHintNoPassword: String = "Падказка не павінна ўтрымліваць сам пароль"
    override val twoFactorHintTooLongShort: String = "Занадта доўгая"
    override val twoFactorHintPublicDesc: String = "Убачыць кожны, хто паспрабуе ўвайсці"
    override val twoFactorDisableButton: String = "Выключыць двухфактарную абарону"
    override val twoFactorAdditionalPasswordTitle: String = "Дадатковы пароль"
    override val twoFactorAdditionalPasswordDesc: String = "Пасля ўводу кода з ліста спатрэбіцца гэты пароль. Нават калі нехта атрымае доступ да вашай пошты, увайсці ён не зможа."
    override val passwordStrengthWeak: String = "Слабкі"
    override val passwordStrengthMedium: String = "Сярэдні"
    override val passwordStrengthGood: String = "Добры"
    override val passwordStrengthStrong: String = "Выдатны"
    override val twoFactorDisableDialogTitle: String = "Выключыць абарону?"
    override val twoFactorDisableDialogDesc: String = "Для ўваходу зноў будзе дастаткова толькі кода з ліста."

    // Privacy option screen
    override val privacyValueEverybody: String = "Усе"
    override val privacyValueNobody: String = "Ніхто"
    override val privacyValueSelected: String = "Выбраныя"
    override val privacySelectUsers: String = "Выбраць карыстальнікаў"
    override val privacySelectUsersRuleDesc: String = "Выберыце карыстальнікаў, да якіх будзе ўжывацца гэтае правіла."

    // Inline video player & media covers
    override val videoPlaybackFailed: String = "Не ўдалося прайграць відэа"
    override val videoScaleCd: String = "Маштаб відэа"
    override val videoCoverCd: String = "Вокладка відэа"
    override val videoNoFrameCd: String = "Відэа без даступнага кадра"
    override val videoPlayCd: String = "Прайграць відэа"
    override val sampleText: String = "Прыклад тэксту"
    override val a11yMuteSound: String = "Выключыць гук"
    override val a11yUnmuteSound: String = "Уключыць гук"

    // Circle recording errors
    override val circleCameraInitFailed: String = "Не ўдалося ініцыялізаваць камеру"
    override val circleNoCamera: String = "На прыладзе няма даступнай камеры"
    override val circleCameraUnavailable: (String) -> String = { msg -> "Камера недаступная: $msg" }
    override val circleCameraNotReady: String = "Камера яшчэ не гатовая"
    override val circleRecordStartFailedMsg: (String) -> String = { msg -> "Не ўдалося пачаць запіс: $msg" }
    override val circleRecordFailedMsg: (Int) -> String = { code -> "Не ўдалося запісаць: код $code" }
    override val circleEmptyRecord: String = "Пусты запіс"

    // Emoji categories
    override val emojiCategorySmileys: String = "Смайлікі"
    override val emojiCategoryGestures: String = "Жэсты і людзі"
    override val emojiCategoryHearts: String = "Сэрцы і сімвалы"
    override val emojiCategoryAnimals: String = "Жывёлы і прырода"
    override val emojiCategoryFood: String = "Ежа і напоі"
    override val emojiCategoryActivities: String = "Актыўнасці"
    override val emojiCategoryTravel: String = "Падарожжы"
    override val emojiCategoryObjects: String = "Аб'екты"

    // Download helper & notifications
    override val errorGeneric: String = "Адбылася памылка"
    override val downloadFileDescription: String = "Спампоўванне файла з Vibe"
    override val downloadStartedToast: String = "Спампоўванне пачалося..."
    override val downloadErrorToast: (String) -> String = { err -> "Памылка спампоўвання: $err" }
    override val downloadMultipleToast: (Int) -> String = { count -> "Спампоўванне: $count файл(аў)" }
    override val notifMe: String = "Я"
    override val unitSecondShort: String = "с"

    override val locale: String = "be"
}

val ruStrings: VibeStrings = RuStrings
val enStrings: VibeStrings = EnStrings
val uaStrings: VibeStrings = UaStrings
val byStrings: VibeStrings = ByStrings

/**
 * Доступ к строкам из слоёв без композиции (ViewModel, сервисы, WebSocket-колбэки).
 *
 * Composition-локали там нет, а тосты и ошибки всё равно должны быть локализованы.
 * Значение выставляется один раз там, где провайдится LocalVibeStrings (см. VibeTheme),
 * плюс страхующий SideEffect в ChatScreen.
 */
object VibeStringsHolder {
    @Volatile
    var current: VibeStrings = RuStrings
}

val LocalVibeStrings = compositionLocalOf<VibeStrings> { RuStrings }