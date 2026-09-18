package rahul.lohra.upisplit.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import rahul.lohra.upisplit.qr.UpiQrCameraAnalyzer
import rahul.lohra.upisplit.qr.UpiQrImageScanResult
import rahul.lohra.upisplit.ui.components.ScreenHeading
import rahul.lohra.upisplit.ui.components.ScreenList
import rahul.lohra.upisplit.ui.components.ScannerPrompt
import rahul.lohra.upisplit.ui.theme.dimensions

@Composable
internal fun CameraScanScreen(
    errorMessage: String?,
    onScanResult: (UpiQrImageScanResult) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        hasPermission = granted
        permissionRequested = true
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasPermission) {
        ScannerPrompt(
            title = "Camera access required",
            supportingText = "SplitUPI needs camera access only while scanning a UPI QR.",
            icon = Icons.Outlined.QrCodeScanner,
            frameTitle = "Allow camera access",
            frameSupportingText = "QR frames are processed on this device",
            actionText = "Grant camera access",
            errorMessage = if (permissionRequested) {
                "Camera permission was denied. Allow it to scan a QR code."
            } else {
                null
            },
            onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) }
        )
        return
    }

    ScreenList {
        item {
            ScreenHeading(
                title = "Scan UPI QR",
                supportingText = "Point the camera at an amount-open UPI QR."
            )
        }
        item {
            val shape = MaterialTheme.shapes.large
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(shape)
                    .border(
                        width = MaterialTheme.dimensions.selectedBorderWidth,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape
                    )
            ) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onScanResult = onScanResult,
                    onCameraError = onCameraError
                )
            }
        }
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        item {
            Text(
                text = "Scanning happens continuously on this device. No image is saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    onScanResult: (UpiQrImageScanResult) -> Unit,
    onCameraError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanResult by rememberUpdatedState(onScanResult)
    val currentOnCameraError by rememberUpdatedState(onCameraError)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val analyzer = remember {
        UpiQrCameraAnalyzer { result -> currentOnScanResult(result) }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    DisposableEffect(lifecycleOwner, previewView) {
        var disposed = false
        var cameraProvider: ProcessCameraProvider? = null
        var preview: Preview? = null
        var imageAnalysis: ImageAnalysis? = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                if (disposed) return@addListener
                try {
                    val provider = providerFuture.get()
                    val previewUseCase = Preview.Builder().build().also { useCase ->
                        useCase.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(cameraExecutor, analyzer)
                        }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        previewUseCase,
                        analysisUseCase
                    )
                    cameraProvider = provider
                    preview = previewUseCase
                    imageAnalysis = analysisUseCase
                } catch (_: Exception) {
                    currentOnCameraError("The camera could not be opened on this device.")
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed = true
            val previewUseCase = preview
            val analysisUseCase = imageAnalysis
            if (previewUseCase != null && analysisUseCase != null) {
                cameraProvider?.unbind(previewUseCase, analysisUseCase)
            }
            analyzer.close()
            cameraExecutor.shutdown()
        }
    }
}
