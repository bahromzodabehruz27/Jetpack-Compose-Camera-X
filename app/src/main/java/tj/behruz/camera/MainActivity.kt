package tj.behruz.camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import tj.behruz.camera.ui.theme.CameraTheme
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CameraTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val context = LocalContext.current

                var hasCameraPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(), onResult = { granted ->
                            hasCameraPermission = granted
                        })

                    val cameraProvider = remember {
                        mutableStateOf<ProcessCameraProvider?>(null)
                    }
                    val cameraExecutor = remember {
                        mutableStateOf<ExecutorService?>(Executors.newSingleThreadExecutor())
                    }
                    val imageAnalysis = remember {
                        mutableStateOf<ImageAnalysis?>(null)
                    }
                    val preview = remember {
                        mutableStateOf<androidx.camera.core.Preview?>(null)
                    }

                    val cameraSelector: MutableState<CameraSelector> = remember {
                        mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
                    }

                    // Create a CameraProvider
                    val cameraProviderFuture = remember {
                        ProcessCameraProvider.getInstance(context)
                    }
                    var camera: Camera? by remember { mutableStateOf(null) }

                    var flashIcon by remember {
                        mutableIntStateOf(R.drawable.ic_launcher_background)
                    }
                    var isFlashTurn by remember {
                        mutableStateOf(false)
                    }
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize(), factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )
                            }
                            // Set up CameraX
                            cameraProviderFuture.addListener({

                                try {
                                    cameraProvider.value = cameraProviderFuture.get()

                                    preview.value = androidx.camera.core.Preview.Builder().build()
                                    imageAnalysis.value = ImageAnalysis.Builder().setTargetResolution(
                                        android.util.Size(
                                            previewView.width, previewView.height
                                        )
                                    ).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
//                                    val analyzer: ImageAnalysis.Analyzer = MLKitQrcodeAnalyzer(qrCodeScannerCallback)
//                                    cameraExecutor.value?.let {
//                                        imageAnalysis.value?.setAnalyzer(it, analyzer)
//
//                                    }
                                    preview.value?.setSurfaceProvider(previewView.surfaceProvider)
                                    cameraProvider.value?.unbindAll()
                                    camera = cameraProvider.value?.bindToLifecycle(
                                        lifecycleOwner, cameraSelector.value, imageAnalysis.value, preview.value
                                    )
                                } catch (e: Exception) {

                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        })


                    Box(
                        modifier = Modifier
                            .fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            RoundedScannerOverlay()
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize(), contentAlignment = Alignment.BottomCenter
                        ) {
                            Image(painter = painterResource(id = R.drawable.ic_launcher_background), "")
                        }


                    }

                }
            }
        }
    }
}

@Composable
fun RoundedScannerOverlay(
    cornerRadius: Dp = 16.dp,
    lineLength: Dp = 72.dp,
    strokeWidth: Dp = 4.dp,
    overlaySize: Dp = (LocalConfiguration.current.screenWidthDp - 80).dp,
    borderColor: Color = Color.White,
) {
    Column {

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)) // Dim the background
        ) {
            // Transparent center and border
            Canvas(
                modifier = Modifier
                    .size(overlaySize)
                    .align(Alignment.Center)
            ) {
                val cornerRadiusPx = cornerRadius.toPx()
                val lineLengthPx = lineLength.toPx()
                val strokeWidthPx = strokeWidth.toPx()

                val arcSize = Size(cornerRadiusPx * 2, cornerRadiusPx * 2)

                // Draw arcs and lines for each corner
                val corners = listOf(
                    Pair(0f, 0f), // Top-left
                    Pair(size.width - arcSize.width, 0f), // Top-right
                    Pair(0f, size.height - arcSize.height), // Bottom-left
                    Pair(size.width - arcSize.width, size.height - arcSize.height) // Bottom-right
                )
                val angles = listOf(180f, 270f, 90f, 0f)


                drawRoundRect(color = Color.Transparent, blendMode = BlendMode.Clear, cornerRadius = CornerRadius(cornerRadiusPx))
                corners.forEachIndexed { index, (x, y) ->
                    // Draw arc
                    drawArc(
                        color = borderColor,
                        startAngle = angles[index],
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(x, y),
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx)
                    )

                    // Draw horizontal and vertical lines extending from the arc
                    when (index) {
                        0 -> {
                            drawLine( // Top-left horizontal
                                color = borderColor,
                                start = Offset(cornerRadiusPx, y),
                                end = Offset(cornerRadiusPx + lineLengthPx, y),
                                strokeWidth = strokeWidthPx
                            )
                            drawLine( // Top-left vertical
                                color = borderColor,
                                start = Offset(x, cornerRadiusPx),
                                end = Offset(x, cornerRadiusPx + lineLengthPx),
                                strokeWidth = strokeWidthPx
                            )
                        }

                        1 -> {
                            drawLine( // Top-right horizontal
                                color = borderColor,
                                start = Offset(size.width - cornerRadiusPx, y),
                                end = Offset(size.width - cornerRadiusPx - lineLengthPx, y),
                                strokeWidth = strokeWidthPx
                            )
                            drawLine( // Top-right vertical
                                color = borderColor,
                                start = Offset(size.width, cornerRadiusPx),
                                end = Offset(size.width, cornerRadiusPx + lineLengthPx),
                                strokeWidth = strokeWidthPx
                            )
                        }

                        2 -> {
                            drawLine(
                                color = borderColor,
                                start = Offset(cornerRadiusPx, size.height),
                                end = Offset(cornerRadiusPx + lineLengthPx, size.height),
                                strokeWidth = strokeWidthPx
                            )
                            drawLine(
                                color = borderColor,
                                start = Offset(x, size.height - cornerRadiusPx),
                                end = Offset(x, size.height - cornerRadiusPx - lineLengthPx),
                                strokeWidth = strokeWidthPx
                            )
                        }

                        3 -> {
                            drawLine(
                                color = borderColor,
                                start = Offset(size.width - cornerRadiusPx, size.height),
                                end = Offset(size.width - cornerRadiusPx - lineLengthPx, size.height),
                                strokeWidth = strokeWidthPx
                            )
                            drawLine(
                                color = borderColor,
                                start = Offset(size.width, size.height - cornerRadiusPx),
                                end = Offset(size.width, size.height - cornerRadiusPx - lineLengthPx),
                                strokeWidth = strokeWidthPx
                            )
                        }
                    }
                }
            }


        }
    }


}
