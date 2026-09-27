package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ExportHelper
import com.example.data.GoogleDriveBackupManager
import com.example.data.ImportHelper
import com.example.ui.components.GoogleLoginDialog
import com.example.ui.components.SetPinDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Bold)
)

@Composable
fun SettingsScreen(
    themeMode: String,
    isPinEnabled: Boolean,
    isPinConfigured: Boolean,
    isGoogleLoggedIn: Boolean,
    googleAccountName: String,
    googleAccountEmail: String,
    isAutoBackupEnabled: Boolean,
    lastBackupTime: Long,
    onThemeModeChange: (String) -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onSetNewPin: (String) -> Unit,
    onLockApp: () -> Unit,
    onGetGoogleSignInIntent: () -> Intent,
    onLoginGoogle: (String, String) -> Unit,
    onLogoutGoogle: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onBackupToDrive: suspend () -> GoogleDriveBackupManager.DriveSyncResult,
    onRestoreFromDrive: suspend () -> GoogleDriveBackupManager.DriveSyncResult,
    onExport: (ExportHelper.ExportFormat) -> Unit,
    onImportUri: suspend (Uri) -> ImportHelper.ImportResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showGoogleLoginDialog by remember { mutableStateOf(false) }
    var isDriveSyncing by remember { mutableStateOf(false) }

    // Real Google Play Services Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val email = account.email ?: ""
                val name = account.displayName ?: email.substringBefore("@").ifBlank { "Pengguna Google" }
                if (email.isNotBlank()) {
                    onLoginGoogle(name, email)
                    Toast.makeText(context, "Berhasil masuk sebagai $email", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }
            }
            showGoogleLoginDialog = true
        } catch (_: Exception) {
            // When play services is absent on emulator or user cancels, show dialog without hardcoded data
            showGoogleLoginDialog = true
        }
    }

    // File picker for import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = onImportUri(uri)
                Toast.makeText(context, result.message, if (result.success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen_container")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Kelola akun Google, cadangan Google Drive, tema, dan PIN",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Akun Google & Cadangan Cloud (Google Drive)
            item {
                Text(
                    text = "Akun Google & Cadangan Google Drive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_drive_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (!isGoogleLoggedIn) {
                            // Belum Login Akun Google
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Hubungkan Akun Google",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Aktifkan auto backup & restore ke Google Drive",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    try {
                                        googleSignInLauncher.launch(onGetGoogleSignInIntent())
                                    } catch (_: Exception) {
                                        showGoogleLoginDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_google_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Login dengan Akun Google",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            // Sudah Login Akun Google
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = googleAccountName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = googleAccountName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = googleAccountEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFD1FAE5)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Terhubung",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Switch Auto Backup
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Auto Backup ke Google Drive",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Cadangkan otomatis setiap ada perubahan transaksi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAutoBackupEnabled,
                                    onCheckedChange = { onAutoBackupToggle(it) },
                                    modifier = Modifier.testTag("auto_backup_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Waktu Terakhir Backup
                            val lastBackupStr = if (lastBackupTime > 0L) {
                                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
                                "Terakhir dicadangkan: " + sdf.format(Date(lastBackupTime))
                            } else {
                                "Belum pernah dicadangkan"
                            }
                            Text(
                                text = lastBackupStr,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Tombol Cadangkan Sekarang & Pulihkan dari Drive
                            if (isDriveSyncing) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Memproses sinkronisasi Google Drive...", fontSize = 12.sp)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Backup Button
                                    Button(
                                        onClick = {
                                            isDriveSyncing = true
                                            coroutineScope.launch {
                                                val res = onBackupToDrive()
                                                isDriveSyncing = false
                                                Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("backup_drive_now_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cadangkan", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Restore Button
                                    OutlinedButton(
                                        onClick = {
                                            isDriveSyncing = true
                                            coroutineScope.launch {
                                                val res = onRestoreFromDrive()
                                                isDriveSyncing = false
                                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("restore_drive_now_button"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pulihkan", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Logout Google button
                                TextButton(
                                    onClick = onLogoutGoogle,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Keluar dari Akun Google", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Ekspor Data Transaksi (PDF, CSV, EXCEL, JSON)
            item {
                Text(
                    text = "Ekspor Data Transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_data_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Pilih format dokumen untuk membagikan atau menyimpan riwayat:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportFormatButton(
                                title = "PDF",
                                subtitle = "Laporan Resmi",
                                icon = Icons.Default.PictureAsPdf,
                                iconTint = Color(0xFFEF4444),
                                modifier = Modifier.weight(1f),
                                onClick = { onExport(ExportHelper.ExportFormat.PDF) }
                            )

                            ExportFormatButton(
                                title = "CSV",
                                subtitle = "Data Tabel",
                                icon = Icons.Default.TableChart,
                                iconTint = Color(0xFF0284C7),
                                modifier = Modifier.weight(1f),
                                onClick = { onExport(ExportHelper.ExportFormat.CSV) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportFormatButton(
                                title = "EXCEL",
                                subtitle = "Lembar Kerja .xls",
                                icon = Icons.Default.Description,
                                iconTint = Color(0xFF10B981),
                                modifier = Modifier.weight(1f),
                                onClick = { onExport(ExportHelper.ExportFormat.EXCEL) }
                            )

                            ExportFormatButton(
                                title = "JSON",
                                subtitle = "Format Objek",
                                icon = Icons.Default.Code,
                                iconTint = Color(0xFFF59E0B),
                                modifier = Modifier.weight(1f),
                                onClick = { onExport(ExportHelper.ExportFormat.JSON) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Impor Data Transaksi (PDF, CSV, EXCEL, JSON)
            item {
                Text(
                    text = "Impor Data Transaksi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_data_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Pulihkan atau masukkan data transaksi dari berkas PDF, CSV, EXCEL (.xls), atau JSON yang sebelumnya diekspor:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                importFileLauncher.launch(
                                    arrayOf(
                                        "*/*",
                                        "application/json",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "application/vnd.ms-excel",
                                        "application/pdf"
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_file_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pilih Berkas untuk Diimpor",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Tema Tampilan Aplikasi (Terang / Gelap / Sistem)
            item {
                Text(
                    text = "Tema Tampilan Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        ThemeOptionRow(
                            label = "Mode Terang",
                            selected = themeMode == "LIGHT",
                            onClick = { onThemeModeChange("LIGHT") }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        ThemeOptionRow(
                            label = "Mode Gelap",
                            selected = themeMode == "DARK",
                            onClick = { onThemeModeChange("DARK") }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        ThemeOptionRow(
                            label = "Ikuti Sistem Android",
                            selected = themeMode == "SYSTEM",
                            onClick = { onThemeModeChange("SYSTEM") }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Keamanan PIN Aplikasi (Aktifkan / Setel / Ganti)
            item {
                Text(
                    text = "Keamanan PIN Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("security_settings_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (isPinEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isPinEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Kunci Aplikasi dengan PIN",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isPinEnabled) "Meminta PIN saat membuka aplikasi" else "Fitur kunci saat ini nonaktif",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isPinEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (isPinConfigured) {
                                            onPinToggle(true)
                                        } else {
                                            showSetPinDialog = true
                                        }
                                    } else {
                                        onPinToggle(false)
                                    }
                                },
                                modifier = Modifier.testTag("pin_lock_switch")
                            )
                        }

                        if (isPinEnabled || isPinConfigured) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = { showSetPinDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("change_pin_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isPinConfigured) "Ganti PIN Aplikasi" else "Setel PIN Baru",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Aksi Aplikasi
            item {
                Text(
                    text = "Aksi Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_actions_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Kunci Aplikasi Sekarang
                        Button(
                            onClick = {
                                if (isPinEnabled && isPinConfigured) {
                                    onLockApp()
                                } else {
                                    Toast.makeText(context, "Silakan aktifkan PIN terlebih dahulu", Toast.LENGTH_SHORT).show()
                                    showSetPinDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("lock_app_now_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kunci Aplikasi Sekarang",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Tutup Aplikasi
                        Button(
                            onClick = {
                                (context as? Activity)?.finishAffinity()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("close_app_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tutup Aplikasi",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Copyright KIFIN & Creator Profile (KIRAN HARA PRADIPTA) with Fancy Font & Contact Links
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 36.dp)
                        .testTag("creator_profile_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "© 2026 KIFIN • All Rights Reserved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Designed & Developed by",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Fancy Playfair Display Font for creator name
                        Text(
                            text = "KIRAN HARA PRADIPTA",
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("creator_name_text")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Punya pertanyaan, kritik, atau saran untuk KIFIN? Hubungi langsung kreator:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Email contact button
                        Button(
                            onClick = {
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:kiranharapradipta@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Tanya & Masukan Seputar KIFIN (Kiran Finance)")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Halo Kiran Hara Pradipta,\n\nSaya menggunakan aplikasi KIFIN (Kiran Finance) dan ingin menyampaikan masukan / pertanyaan:\n\n[Tulis pesan atau pertanyaan Anda di sini]\n\nTerima kasih!\n"
                                    )
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Kirim Email ke Kiran"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("contact_email_button")
                        ) {
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "kiranharapradipta@gmail.com",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Set / Change PIN Dialog
        if (showSetPinDialog) {
            SetPinDialog(
                onPinSet = { pin ->
                    onSetNewPin(pin)
                    showSetPinDialog = false
                    Toast.makeText(context, "PIN berhasil disimpan dan diaktifkan", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showSetPinDialog = false }
            )
        }

        // Google Sign In Dialog
        if (showGoogleLoginDialog) {
            GoogleLoginDialog(
                onConfirm = { name, email ->
                    onLoginGoogle(name, email)
                    showGoogleLoginDialog = false
                    Toast.makeText(context, "Berhasil masuk sebagai $name ($email)", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showGoogleLoginDialog = false }
            )
        }
    }
}

@Composable
private fun ExportFormatButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .height(72.dp)
            .testTag("export_btn_${title.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
