package com.fakka.cash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection

private val VodaRed = Color(0xFFE60000)
private val VodaRedDark = Color(0xFFB30000)
private val SurfaceDark = Color(0xFF1A1A1A)
private val OldCardStart = Color(0xFF374B6E)
private val OldCardEnd = Color(0xFF1F2A44)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            val colors = lightColorScheme(
                primary = VodaRed,
                onPrimary = Color.White,
                background = Color(0xFFF7F7F8),
                surface = Color.White,
            )
            MaterialTheme(colorScheme = colors) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppRoot()
                }
            }
        }
    }
}

class AppViewModel : ViewModel() {
    enum class AuthState { IDLE, LOGGING_IN, READY, ERROR }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    data class UiState(
        val auth: AuthState = AuthState.IDLE,
        val msisdn: String = "",
        val seamlessToken: String = "",
        val accessToken: String = "",
        val errorMessage: String = "",
        val history: List<HistoryItem> = emptyList()
    )

    data class HistoryItem(
        val product: Product,
        val receiver: String,
        val timestamp: Long,
        val success: Boolean,
        val message: String
    )

    fun login() {
        if (_state.value.auth == AuthState.LOGGING_IN) return
        _state.value = _state.value.copy(auth = AuthState.LOGGING_IN, errorMessage = "")
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val seamless = VodafoneApi.getSeamlessAndMsisdn()
                    val token = VodafoneApi.getAccessToken(seamless.seamlessToken)
                    Triple(seamless.seamlessToken, seamless.msisdn, token)
                }
                _state.value = _state.value.copy(
                    auth = AuthState.READY,
                    seamlessToken = result.first,
                    msisdn = result.second,
                    accessToken = result.third
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    auth = AuthState.ERROR,
                    errorMessage = e.message ?: "فشل تسجيل الدخول"
                )
            }
        }
    }

    suspend fun recharge(product: Product, receiver: String, pin: String): VodafoneApi.OrderResult {
        return withContext(Dispatchers.IO) {
            val refreshedToken = try {
                VodafoneApi.getAccessToken(_state.value.seamlessToken)
            } catch (_: Exception) { _state.value.accessToken }
            _state.value = _state.value.copy(accessToken = refreshedToken)

            val result = VodafoneApi.placeOrder(
                productId = product.id,
                sender = _state.value.msisdn,
                receiver = receiver,
                pin = pin,
                accessToken = refreshedToken
            )

            val item = HistoryItem(product, receiver, System.currentTimeMillis(), result.success, result.message)
            _state.value = _state.value.copy(history = listOf(item) + _state.value.history)
            result
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        if (state.auth == AppViewModel.AuthState.IDLE) vm.login()
    }

    when (state.auth) {
        AppViewModel.AuthState.LOGGING_IN, AppViewModel.AuthState.IDLE -> LoginScreen(loading = true)
        AppViewModel.AuthState.ERROR -> LoginScreen(loading = false, error = state.errorMessage, onRetry = { vm.login() })
        AppViewModel.AuthState.READY -> HomeScreen(vm, state)
    }
}

@Composable
fun LoginScreen(loading: Boolean, error: String = "", onRetry: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VodaRed, VodaRedDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(Icons.Default.SimCard, null, tint = Color.White, modifier = Modifier.size(64.dp))
            Text("شحن الفكة", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            if (loading) {
                CircularProgressIndicator(color = Color.White)
                Text("جاري تسجيل الدخول التلقائي...", color = Color.White.copy(alpha = 0.9f))
                Text("تأكد أنك متصل بداتا فودافون", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            } else {
                Text(error, color = Color.White, modifier = Modifier.padding(horizontal = 32.dp), textAlign = TextAlign.Center)
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VodaRed)) {
                    Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, state: AppViewModel.UiState) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Surface(color = VodaRed) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الرقم الدافع", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            Text(state.msisdn, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("شحن الفكة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab, containerColor = Color.White, contentColor = VodaRed) {
                listOf("جديد", "قديم", "مارد", "السجل").forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title, fontWeight = FontWeight.Bold) })
                }
            }
            when (tab) {
                0 -> ProductsGrid(Products.newCards) { selectedProduct = it }
                1 -> ProductsGrid(Products.oldCards) { selectedProduct = it }
                2 -> ProductsGrid(Products.mared) { selectedProduct = it }
                3 -> HistoryList(state.history)
            }
        }
    }

    selectedProduct?.let { product ->
        RechargeSheet(
            product = product,
            onDismiss = { selectedProduct = null },
            onConfirm = { receiver, pin -> vm.recharge(product, receiver, pin) }
        )
    }
}

@Composable
fun ProductsGrid(items: List<Product>, onClick: (Product) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { p -> ProductCard(p) { onClick(p) } }
    }
}

@Composable
fun ProductCard(p: Product, onClick: () -> Unit) {
    val brush = when (p.category) {
        Category.NEW -> Brush.linearGradient(listOf(VodaRed, VodaRedDark))
        Category.OLD -> Brush.linearGradient(listOf(OldCardStart, OldCardEnd))
        Category.MARED -> Brush.linearGradient(listOf(Color(0xFF7B2CBF), Color(0xFF3C096C)))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        if (p.category == Category.NEW) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text("جديد", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
        Icon(
            when (p.category) {
                Category.MARED -> Icons.Default.Wifi
                Category.NEW -> Icons.Default.AutoAwesome
                Category.OLD -> Icons.Default.Bolt
            },
            null, tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Column(Modifier.align(Alignment.BottomStart)) {
            if (p.category == Category.MARED) {
                Text(p.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Text(
                    if (p.price % 1.0 == 0.0) "${p.price.toInt()}" else "${p.price}",
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold
                )
                Text("جنيه", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeSheet(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: suspend (String, String) -> VodafoneApi.OrderResult
) {
    var receiver by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VodafoneApi.OrderResult?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("تأكيد الشحن", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = VodaRed, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(product.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            when (product.category) {
                                Category.NEW -> "كارت من الجيل الجديد"
                                Category.OLD -> "كارت من الجيل القديم"
                                Category.MARED -> "باقة مارد"
                            },
                            color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = receiver,
                    onValueChange = { if (it.length <= 11) receiver = it.filter { c -> c.isDigit() } },
                    label = { Text("رقم المستلم") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("الرقم السري (6 أرقام)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                result?.let { r ->
                    Surface(
                        color = if (r.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            r.message,
                            color = if (r.success) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.padding(10.dp), fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading && receiver.length == 11 && pin.length == 6,
                colors = ButtonDefaults.buttonColors(containerColor = VodaRed),
                onClick = {
                    loading = true
                    scope.launch {
                        try {
                            result = onConfirm(receiver, pin)
                        } catch (e: Exception) {
                            result = VodafoneApi.OrderResult(false, e.message ?: "خطأ", "")
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else Text("تأكيد الشحن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("إلغاء") }
        }
    )
}

@Composable
fun HistoryList(items: List<AppViewModel.HistoryItem>) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد سجل عمليات بعد", color = Color.Gray)
        }
        return
    }
    Column(Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { h ->
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (h.success) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (h.success) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(h.product.label, fontWeight = FontWeight.Bold)
                        Text("للرقم: ${h.receiver}", fontSize = 12.sp, color = Color.Gray)
                        Text(h.message, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}
