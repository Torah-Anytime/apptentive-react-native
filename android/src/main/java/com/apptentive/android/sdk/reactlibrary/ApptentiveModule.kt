package com.apptentive.android.sdk.reactlibrary

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import apptentive.com.android.feedback.Apptentive
import apptentive.com.android.feedback.ApptentiveActivityInfo
import apptentive.com.android.feedback.ApptentiveConfiguration
import apptentive.com.android.feedback.EngagementResult
import apptentive.com.android.feedback.RegisterResult
import apptentive.com.android.util.InternalUseOnly
import apptentive.com.android.util.Log
import apptentive.com.android.util.LogLevel
import apptentive.com.android.util.LogTag
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule


@OptIn(InternalUseOnly::class)
class ApptentiveModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ApptentiveActivityInfo, LifecycleEventListener {

  private companion object {
    const val APPTENTIVE_ERROR_CODE = "Apptentive Error"
    const val UNREAD_MESSAGE_COUNT_CHANGED = "onUnreadMessageCountChanged"
    val REACT_NATIVE_TAG = LogTag("REACT NATIVE")
  }

  private var isApptentiveRegistered = false

  // Register the Apptentive Android SDK
  @ReactMethod
  fun register(credentials: ReadableMap, promise: Promise) {
    try {
      android.util.Log.d("Apptentive", "[REACT NATIVE] Registering Apptentive")

      getApplicationContext()?.let { application ->
        Apptentive.register(application, unpackCredentials(credentials)) {
          if (it == RegisterResult.Success) {
            Log.d(REACT_NATIVE_TAG, "Register Apptentive: Success")
            isApptentiveRegistered = true

            handlePostRegister()
          } else Log.d(REACT_NATIVE_TAG, "Register Apptentive: Fail")

          promise.resolve(isApptentiveRegistered)
        }
      } ?: promise.reject(
        APPTENTIVE_ERROR_CODE,
        "Apptentive instance was not initialized: application context is null"
      )
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to register Apptentive instance.", e)
    }
  }

  private fun handlePostRegister() {
    // We also do this in onResume, but doing it here as well might help
    //   avoid some race conditions that we've encountered before.
    Log.d(REACT_NATIVE_TAG, "Registering ApptentiveInfoCallback")
    Apptentive.registerApptentiveActivityInfoCallback(this)

    Log.d(REACT_NATIVE_TAG, "Observing Message Center Notification")
    Apptentive.messageCenterNotificationObservable.observe { notification ->
      Log.v(REACT_NATIVE_TAG, "Message Center notification received: $notification")
      val eventEmitter =
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      val map = WritableNativeMap()
      map.putInt("count", notification?.unreadMessageCount ?: 0)

      eventEmitter.emit(UNREAD_MESSAGE_COUNT_CHANGED, map)
    }

    Log.d(REACT_NATIVE_TAG, "Register lifecycle observe")
    reactApplicationContext.addLifecycleEventListener(this)
  }

  // Engage an event by an event name string
  @ReactMethod
  fun engage(event: String, promise: Promise) {
    try {
      Apptentive.engage(event) {
        promise.resolve(it is EngagementResult.InteractionShown)
      }
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to engage event $event.", e)
    }
  }

  // Show the Apptentive Message Center
  @ReactMethod
  fun showMessageCenter(promise: Promise) {
    try {
      Apptentive.showMessageCenter {
        promise.resolve(it is EngagementResult.InteractionShown)
      }
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to present Message Center.", e)
    }
  }

  // Set person name
  @ReactMethod
  fun setPersonName(name: String, promise: Promise) {
    try {
      Apptentive.setPersonName(name)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to set person name.", e)
    }
  }

  // Get person name, empty string if there is none
  @ReactMethod
  fun getPersonName(promise: Promise) {
    try {
      promise.resolve(Apptentive.getPersonName().orEmpty())
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to get person name.", e)
    }
  }

  // Set person email
  @ReactMethod
  fun setPersonEmail(email: String, promise: Promise) {
    try {
      Apptentive.setPersonEmail(email)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to set person email.", e)
    }
  }

  // Get person email, empty string if there is none
  @ReactMethod
  fun getPersonEmail(promise: Promise) {
    try {
      promise.resolve(Apptentive.getPersonEmail().orEmpty())
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to get person email.", e)
    }
  }

  // Add person custom data based on key string and value of type bool
  // Delegated from addCustomPersonData(key, value)
  @ReactMethod
  fun addCustomPersonDataBoolean(key: String, value: Boolean, promise: Promise) {
    Apptentive.addCustomPersonData(key, value)
    promise.resolve(true)
  }

  // Add person custom data based on key string and value of type double
  // Delegated from addCustomPersonData(key, value)
  @ReactMethod
  fun addCustomPersonDataNumber(key: String, value: Double, promise: Promise) {
    Apptentive.addCustomPersonData(key, value)
    promise.resolve(true)
  }

  // Add person custom data based on key string and value of type String
  // Delegated from addCustomPersonData(key, value)
  @ReactMethod
  fun addCustomPersonDataString(key: String, value: String, promise: Promise) {
    Apptentive.addCustomPersonData(key, value)
    promise.resolve(true)
  }

  // Remove person custom data based on key string
  @ReactMethod
  fun removeCustomPersonData(key: String, promise: Promise) {
    try {
      Apptentive.removeCustomPersonData(key)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to remove custom person data $key.", e)
    }
  }

  // Add device custom data based on key string and value of type bool
  // Delegated from addCustomPersonData(key, value)
  @ReactMethod
  fun addCustomDeviceDataBoolean(key: String, value: Boolean, promise: Promise) {
    Apptentive.addCustomDeviceData(key, value)
    promise.resolve(true)
  }

  // Add device custom data based on key string and value of type double
  // Delegated from addCustomPersonData(key, value)
  @ReactMethod
  fun addCustomDeviceDataNumber(key: String, value: Double, promise: Promise) {
    Apptentive.addCustomDeviceData(key, value)
    promise.resolve(true)
  }

  // Add device custom data based on key string and value of type string
  // Delegated from addCustomPersonData(key, value)
  @ReactMethod
  fun addCustomDeviceDataString(key: String, value: String, promise: Promise) {
    Apptentive.addCustomDeviceData(key, value)
    promise.resolve(true)
  }

  // Remove device custom data based on key string
  @ReactMethod
  fun removeCustomDeviceData(key: String, promise: Promise) {
    try {
      Apptentive.removeCustomDeviceData(key)
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject(APPTENTIVE_ERROR_CODE, "Failed to remove custom device data $key.", e)
    }
  }

  // Check if an event name will launch an interaction
  @ReactMethod
  fun canShowInteraction(event: String, promise: Promise) {
    try {
      val canShow = Apptentive.canShowInteraction(event)
      promise.resolve(canShow)
    } catch (e: Exception) {
      promise.reject(
        APPTENTIVE_ERROR_CODE,
        "Failed to check if Apptentive interaction can be shown on event $event.",
        e
      )
    }
  }

  // Check if Message Center can be shown
  @ReactMethod
  fun canShowMessageCenter(promise: Promise) {
    try {
      val canShow = Apptentive.canShowMessageCenter()
      promise.resolve(canShow)
    } catch (e: Exception) {
      promise.reject(
        APPTENTIVE_ERROR_CODE,
        "Failed to check if Apptentive can launch Message Center.",
        e
      )
    }
  }

  // Get unread message count in Message Center
  @ReactMethod
  fun getUnreadMessageCount(promise: Promise) {
    try {
      val count = Apptentive.getUnreadMessageCount()
      promise.resolve(count)
    } catch (e: Exception) {
      promise.reject(
        APPTENTIVE_ERROR_CODE,
        "Failed to check number of unread messages in Message Center.",
        e
      )
    }
  }

  ////// Utility Functions

  // Set ApptentiveLogger log level
  private fun parseLogLevel(logLevel: String): LogLevel {
    android.util.Log.d("Apptentive", "[REACT NATIVE] Parsing log level: $logLevel")

    return when (logLevel) {
      "verbose" -> LogLevel.Verbose
      "debug" -> LogLevel.Debug
      "info" -> LogLevel.Info
      "warn" -> LogLevel.Warning
      "error" -> LogLevel.Error
      else -> {
        println("$APPTENTIVE_ERROR_CODE: Unknown log level $logLevel, setting to info by default.")
        LogLevel.Info
      }
    }
  }

  // Unpack dictionary to create and return ApptentiveConfiguration object
  private fun unpackCredentials(credentials: ReadableMap): ApptentiveConfiguration {
    // Key/Sig
    val apptentiveKey = credentials.getString("apptentiveKey")!!
    val apptentiveSignature = credentials.getString("apptentiveSignature")!!

    // Create configuration instance
    val apptentiveConfiguration = ApptentiveConfiguration(apptentiveKey, apptentiveSignature)

    // Set flags, or defaults if not present
    if (credentials.hasKey("logLevel")) {
      apptentiveConfiguration.logLevel =
        parseLogLevel(credentials.getString("logLevel") ?: "info")
    }

    // Set distribution name and version
    if (credentials.hasKey("distributionName")) {
      apptentiveConfiguration.distributionName = credentials.getString("distributionName") ?: "React Native"
    }

    if (credentials.hasKey("distributionVersion")) {
      apptentiveConfiguration.distributionVersion = credentials.getString("distributionVersion").orEmpty()
    }

    if (credentials.hasKey("shouldEncryptStorage")) {
      apptentiveConfiguration.shouldEncryptStorage = credentials.getBoolean("shouldEncryptStorage")
    }

    if (credentials.hasKey("shouldSanitizeLogMessages")) {
      apptentiveConfiguration.shouldSanitizeLogMessages = credentials.getBoolean("shouldSanitizeLogMessages")
    }

    if (credentials.hasKey("shouldInheritAppTheme")) {
      apptentiveConfiguration.shouldInheritAppTheme = credentials.getBoolean("shouldInheritAppTheme")
    }

    if (credentials.hasKey("ratingInteractionThrottleLength")) {
      apptentiveConfiguration.ratingInteractionThrottleLength =
        credentials.getDouble("ratingInteractionThrottleLength").toLong()
    }

    if (credentials.hasKey("customAppStoreURL")) {
      apptentiveConfiguration.customAppStoreURL = credentials.getString("customAppStoreURL")
    }

    return apptentiveConfiguration
  }

  override fun getName(): String {
    return "ApptentiveModule"
  }

  private fun getApplicationContext(): Application? {
    return currentActivity?.applicationContext as Application?
  }

  override fun getApptentiveActivityInfo(): Activity {
    return (currentActivity as? AppCompatActivity)
      ?: throw IllegalStateException("Activity $this could not retrieve currentActivity from React Native.")
  }

  override fun hasConstants(): Boolean {
    return true
  }

  override fun getConstants(): MutableMap<String, Any> {
    return mutableMapOf("unreadMessageCountChangedEvent" to UNREAD_MESSAGE_COUNT_CHANGED)
  }

  override fun onHostResume() {
    if (isApptentiveRegistered) {
      Log.d(REACT_NATIVE_TAG, "Registering ApptentiveInfoCallback")
      Apptentive.registerApptentiveActivityInfoCallback(this)
    }
  }

  override fun onHostPause() {}

  override fun onHostDestroy() {}
}
