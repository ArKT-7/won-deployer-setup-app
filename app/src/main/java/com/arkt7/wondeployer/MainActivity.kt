@file:Suppress("DEPRECATION")

package com.arkt7.wondeployer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.arkt7.wondeployer.ui.theme.wondeployerTheme
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private val statusText = mutableStateOf("Made by °⊥⋊ɹ∀° : Telegram - @ArKT_7, Github - ArKT-7")

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                createUefiFolder()
            } else {
                statusText.value = "Permission denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            wondeployerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ApkInstallerScreen(
                        modifier = Modifier.padding(innerPadding),
                        onCreateUefiFolderClicked = ::onCreateUefiFolderClicked,
                        statusText = statusText.value,
                        onStatusTextChanged = { statusText.value = it }
                    )
                }
            }
        }

        // Check internet connectivity at startup
        if (!isInternetAvailable(this)) {
            statusText.value = "No internet connection"
        }
    }

    //added
    private var permissionActivityLaunched = false
    //added

    private fun onCreateUefiFolderClicked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                permissionActivityLaunched = true
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            } else {
                createUefiFolder()
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    //added
    override fun onResume() {
        super.onResume()
        // Check if we need to proceed after returning from the permission activity
        if (permissionActivityLaunched) {
            permissionActivityLaunched = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                createUefiFolder()
            }
        }
    }
    //added

    private fun createUefiFolder() {
        val uefiFolder = File(Environment.getExternalStorageDirectory(), "UEFI")
        if (uefiFolder.exists()) {
            statusText.value = "UEFI folder already exists"
            downloadUefiImgFile(uefiFolder)
        } else {
            val isCreated = uefiFolder.mkdirs()
            if (isCreated) {
                statusText.value = "UEFI folder created successfully"
                downloadUefiImgFile(uefiFolder)
            } else {
                statusText.value = "Failed to create UEFI folder"
            }
        }
    }

    private fun downloadUefiImgFile(folder: File) {
        val fileUrl = "https://raw.githubusercontent.com/arkt-7/won-deployer/main/files/uefi.img"
        val fileName = "uefi.img"
        thread {
            try {
                val file = File(folder, fileName)
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val fileSize = connection.contentLength
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int
                var totalBytesRead = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    val progress = (totalBytesRead * 100 / fileSize).toInt()
                    runOnUiThread {
                        statusText.value = "Downloading uefi.img: $progress% ($totalBytesRead / $fileSize bytes)"
                    }
                }
                outputStream.close()
                inputStream.close()

                runOnUiThread {
                    statusText.value = "uefi.img downloaded successfully"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    statusText.value = "Download failed: ${e.message} - Most probably Internet issue please check"
                }
            }
        }
    }
}

@Composable
fun ApkInstallerScreen(
    modifier: Modifier = Modifier,
    onCreateUefiFolderClicked: () -> Unit,
    statusText: String,
    onStatusTextChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var showProgress by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var showAppNotInstalledMessage by remember { mutableStateOf(false) }

    // Enable scrolling
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(250.dp)
                .padding(bottom = 32.dp)
        )

        Text(
            text = "Please install both applications, \nFor dual-booting from Android to Windows",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        )
        Text(
            text = "Ensure that your internet connection is active",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .padding(bottom = 16.dp)
                .fillMaxWidth()
        )

        Text(
            text = "It's IMPORTANT!!\n Complete all 3 steps",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        )

        if (showProgress) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = statusText, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
            }
        } else {
            if (!isInternetAvailable(context)) {
                LinearProgressIndicator(
                    progress = 0f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "No internet connection")
            }
        }

        Text(
            text = ""
        )

        Button(
            onClick = {
                if (isInternetAvailable(context)) {
                    showProgress = true
                    downloadAndInstallApk(
                        "https://raw.githubusercontent.com/arkt-7/won-deployer/main/files/Magisk_stable.apk",
                        "Magisk.apk",
                        context
                    ) { progressValue, message ->
                        progress = progressValue
                        onStatusTextChanged(message)
                    }
                } else {
                    onStatusTextChanged("No internet connection")
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "1. Magisk App - root",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = {
                if (isInternetAvailable(context)) {
                    showProgress = true
                    downloadAndInstallApk(
                        "https://raw.githubusercontent.com/arkt-7/won-deployer/main/files/woa_helper.apk",
                        "woa_helper.apk",
                        context
                    ) { progressValue, message ->
                        progress = progressValue
                        onStatusTextChanged(message)
                    }
                } else {
                    onStatusTextChanged("No internet connection")
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "2. WOA Helper App - dual boot",
                fontSize = 18.sp,  // Adjust as needed
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = {
                if (isInternetAvailable(context)) {
                    onCreateUefiFolderClicked()
                } else {
                    onStatusTextChanged("No internet connection")
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "3. Download UEFI image",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
        /*
        Text(
            text = "You may have to do this (step 3) two times",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        )
        */
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val packageName = "id.kuato.woahelper"
                val pm = context.packageManager
                val intent = pm.getLaunchIntentForPackage(packageName)

                if (intent != null && pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    try {
                        context.startActivity(intent)
                        Log.d("AppLauncher", "Launching app with package: $packageName")
                    } catch (e: Exception) {
                        Log.e("AppLauncher", "Error launching app: ${e.message}")
                        onStatusTextChanged("Error opening the app: ${e.message}")
                    }
                } else {
                    showAppNotInstalledMessage = true
                    onStatusTextChanged("Woa Helper app is not installed.")
                    Log.d("AppLauncher", "App is not installed or cannot be launched")
                }
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(56.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Open Woa Helper App",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }

        if (showAppNotInstalledMessage) {
            Text(
                text = "Woa Helper app isn't installed please install it, step - 2.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This all is only for Nabu - Xiaomi Pad 5",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .padding(top = 15.dp)
            )
            Text(
                text = "By °⊥⋊ɹ∀° Github - ArKT-7, Telegram - ArKT_7  @2024",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(bottom = 1.dp)
            )
        }
    }
}


fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

private fun downloadAndInstallApk(
    apkUrl: String,
    apkName: String,
    context: Context,
    updateProgress: (progress: Int, message: String) -> Unit
) {
    if (!isInternetAvailable(context)) {
        (context as ComponentActivity).runOnUiThread {
            updateProgress(0, "No internet connection")
        }
        return
    }

    thread {
        try {
            val apkFile = downloadFile(apkUrl, apkName, context, updateProgress)
            (context as ComponentActivity).runOnUiThread {
                updateProgress(100, "$apkName downloaded, Please install it")
                installApk(apkFile, context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (context as ComponentActivity).runOnUiThread {
                updateProgress(0, "Download failed: ${e.message} - Most probably Internet issue please check")
            }
        }
    }
}

@Throws(Exception::class)
private fun downloadFile(
    fileUrl: String,
    fileName: String,
    context: Context,
    updateProgress: (progress: Int, message: String) -> Unit
): File {
    val url = URL(fileUrl)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connect()

    val fileSize = connection.contentLength
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
    connection.inputStream.use { input ->
        FileOutputStream(file).use { output ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytesRead = 0

            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progressValue = (totalBytesRead * 100 / fileSize).toInt()
                updateProgress(progressValue, "Downloading $fileName: $progressValue% ($totalBytesRead / $fileSize bytes)")
            }
        }
    }
    return file
}

private fun installApk(file: File, context: Context) {
    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}
