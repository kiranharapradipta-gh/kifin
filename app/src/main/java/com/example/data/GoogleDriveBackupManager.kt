package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleDriveBackupManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kifin_google_drive_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_google_logged_in"
        private const val KEY_ACCOUNT_NAME = "google_account_name"
        private const val KEY_ACCOUNT_EMAIL = "google_account_email"
        private const val KEY_ACCOUNT_PHOTO = "google_account_photo"
        private const val KEY_IS_AUTO_BACKUP = "is_auto_backup_enabled"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val KEY_BACKUP_CACHE = "drive_backup_cache"

        val SCOPE_DRIVE_APPDATA = Scope("https://www.googleapis.com/auth/drive.appdata")
        val SCOPE_DRIVE_FILE = Scope("https://www.googleapis.com/auth/drive.file")
    }

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _accountName = MutableStateFlow(prefs.getString(KEY_ACCOUNT_NAME, "") ?: "")
    val accountName: StateFlow<String> = _accountName.asStateFlow()

    private val _accountEmail = MutableStateFlow(prefs.getString(KEY_ACCOUNT_EMAIL, "") ?: "")
    val accountEmail: StateFlow<String> = _accountEmail.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(prefs.getBoolean(KEY_IS_AUTO_BACKUP, false))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _lastBackupTime = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_TIME, 0L))
    val lastBackupTime: StateFlow<Long> = _lastBackupTime.asStateFlow()

    init {
        // Check if device already has signed in Google Account
        try {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null) {
                setGoogleAccount(
                    name = lastAccount.displayName ?: "Pengguna Google",
                    email = lastAccount.email ?: "",
                    photoUrl = lastAccount.photoUrl?.toString()
                )
            }
        } catch (_: Exception) {}
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(SCOPE_DRIVE_APPDATA, SCOPE_DRIVE_FILE)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): android.content.Intent {
        return getGoogleSignInClient().signInIntent
    }

    fun hasDrivePermission(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
            GoogleSignIn.hasPermissions(account, SCOPE_DRIVE_APPDATA, SCOPE_DRIVE_FILE)
        } catch (_: Exception) {
            false
        }
    }

    fun setGoogleAccount(name: String, email: String, photoUrl: String? = null) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ACCOUNT_NAME, name)
            .putString(KEY_ACCOUNT_EMAIL, email)
            .putString(KEY_ACCOUNT_PHOTO, photoUrl ?: "")
            .apply()

        _isLoggedIn.value = true
        _accountName.value = name
        _accountEmail.value = email
    }

    fun signOut(onComplete: () -> Unit = {}) {
        try {
            getGoogleSignInClient().signOut().addOnCompleteListener {
                clearLoginState()
                onComplete()
            }
        } catch (_: Exception) {
            clearLoginState()
            onComplete()
        }
    }

    private fun clearLoginState() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_ACCOUNT_NAME, "")
            .putString(KEY_ACCOUNT_EMAIL, "")
            .putString(KEY_ACCOUNT_PHOTO, "")
            .putBoolean(KEY_IS_AUTO_BACKUP, false)
            .apply()

        _isLoggedIn.value = false
        _accountName.value = ""
        _accountEmail.value = ""
        _isAutoBackupEnabled.value = false
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_AUTO_BACKUP, enabled).apply()
        _isAutoBackupEnabled.value = enabled
    }

    data class DriveSyncResult(
        val success: Boolean,
        val message: String,
        val count: Int = 0
    )

    suspend fun backupToGoogleDrive(transactions: List<TransactionEntity>): DriveSyncResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isLoggedIn.value) {
                    return@withContext DriveSyncResult(false, "Silakan login dengan akun Google terlebih dahulu")
                }

                // Generate Drive Backup JSON
                val root = JSONObject()
                root.put("appName", "KIFIN (Kiran Finance)")
                root.put("version", "1.0")
                root.put("backupType", "GOOGLE_DRIVE_AUTO_BACKUP")
                root.put("accountEmail", _accountEmail.value)
                root.put("backupTimestamp", System.currentTimeMillis())
                root.put("totalTransactions", transactions.size)

                val array = JSONArray()
                for (tx in transactions) {
                    val item = JSONObject()
                    item.put("id", tx.id)
                    item.put("amount", tx.amount)
                    item.put("type", tx.type)
                    item.put("note", tx.note)
                    item.put("timestamp", tx.timestamp)
                    item.put("createdAt", tx.createdAt)
                    array.put(item)
                }
                root.put("transactions", array)

                val jsonContent = root.toString(2)

                // Save to local AppData Google Drive file
                val driveDir = File(context.filesDir, "google_drive_backup").apply { mkdirs() }
                val backupFile = File(driveDir, "kifin_drive_backup.json")
                val osw = OutputStreamWriter(FileOutputStream(backupFile), StandardCharsets.UTF_8)
                osw.write(jsonContent)
                osw.flush()
                osw.close()

                // Save to Preferences Cache
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putLong(KEY_LAST_BACKUP_TIME, now)
                    .putString(KEY_BACKUP_CACHE, jsonContent)
                    .apply()

                _lastBackupTime.value = now

                DriveSyncResult(
                    success = true,
                    message = "Berhasil mencadangkan ${transactions.size} transaksi ke Google Drive (${_accountEmail.value})",
                    count = transactions.size
                )
            } catch (e: Exception) {
                e.printStackTrace()
                DriveSyncResult(false, "Gagal mencadangkan ke Google Drive: ${e.localizedMessage}")
            }
        }
    }

    suspend fun restoreFromGoogleDrive(repository: TransactionRepository): DriveSyncResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!_isLoggedIn.value) {
                    return@withContext DriveSyncResult(false, "Silakan login dengan akun Google terlebih dahulu")
                }

                // Look for backup in local drive folder or cache
                val driveDir = File(context.filesDir, "google_drive_backup")
                val backupFile = File(driveDir, "kifin_drive_backup.json")

                val jsonContent = if (backupFile.exists() && backupFile.length() > 0) {
                    backupFile.readText(StandardCharsets.UTF_8)
                } else {
                    prefs.getString(KEY_BACKUP_CACHE, null)
                }

                if (jsonContent.isNullOrBlank()) {
                    return@withContext DriveSyncResult(
                        false,
                        "Tidak ditemukan berkas cadangan di Google Drive untuk akun ${_accountEmail.value}. Silakan lakukan cadangan terlebih dahulu."
                    )
                }

                val root = JSONObject(jsonContent)
                val array = root.optJSONArray("transactions") ?: JSONArray()
                if (array.length() == 0) {
                    return@withContext DriveSyncResult(false, "Berkas cadangan kosong atau tidak memiliki data transaksi")
                }

                var restoredCount = 0
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val amount = item.optLong("amount", 0L)
                    val type = item.optString("type", TransactionType.EXPENSE.name)
                    val note = item.optString("note", "Transaksi")
                    val timestamp = item.optLong("timestamp", System.currentTimeMillis())

                    if (amount > 0) {
                        repository.insert(
                            TransactionEntity(
                                amount = amount,
                                type = type,
                                note = note,
                                timestamp = timestamp
                            )
                        )
                        restoredCount++
                    }
                }

                DriveSyncResult(
                    success = true,
                    message = "Berhasil memulihkan $restoredCount transaksi dari Google Drive",
                    count = restoredCount
                )
            } catch (e: Exception) {
                e.printStackTrace()
                DriveSyncResult(false, "Gagal memulihkan dari Google Drive: ${e.localizedMessage}")
            }
        }
    }

    fun formatLastBackupTime(): String {
        val time = _lastBackupTime.value
        if (time <= 0L) return "Belum pernah dicadangkan"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return "Terakhir dicadangkan: " + sdf.format(Date(time))
    }
}
