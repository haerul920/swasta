package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.enums.HealthProfileGroup
import com.example.domain.enums.UserRole
import com.example.ui.components.SegmentedPillBar
import com.example.ui.components.SwasanaLogoMark
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.ForestEmerald
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.MistBackground
import com.example.ui.theme.PrimaryEmerald
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftMint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuthWelcomeScreen(
    authError: String?,
    onQuickDemoLogin: (UserRole) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String, HealthProfileGroup) -> Unit,
    onClearError: () -> Unit
) {
    var showAuthSheet by remember { mutableStateOf(false) }
    var authTab by remember { mutableIntStateOf(0) } // 0 = Masuk, 1 = Daftar

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE7F5F0), MistBackground, Color(0xFFF3F8F6))
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Brand Lockup (Finora top-left style)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SwasanaLogoMark(size = 38.dp)
                    Text(
                        text = "Swasana",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Kendalikan Udara\n& Napas Anda",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Pantau AQI secara akurat, temukan zona bebas asap, dan pulihkan kesehatan paru-paru Anda dengan panduan AI.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Atmospheric Clean-Air Hero Illustration Card
                WelcomeCleanAirHeroCard()
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Primary CTA matching Finora "Get Started ->" pill button
                Button(
                    onClick = {
                        authTab = 1
                        onClearError()
                        showAuthSheet = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("welcome_get_started_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text(
                        text = "Mulai Sekarang",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PureWhite
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sudah punya akun?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TextButton(
                        onClick = {
                            authTab = 0
                            onClearError()
                            showAuthSheet = true
                        },
                        modifier = Modifier.testTag("welcome_sign_in_button")
                    ) {
                        Text(
                            text = "Masuk",
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Mode Demo Cepat bar for frictionless evaluation
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PureWhite,
                    border = BorderStroke(1.dp, CardBorder),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = null,
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mode Demo Cepat (Data Indonesia Siap Pakai)",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onQuickDemoLogin(UserRole.USER) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("demo_user_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, PrimaryEmerald),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SoftMint,
                                    contentColor = DeepEmerald
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pengguna", style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = { onQuickDemoLogin(UserRole.ADMIN) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("demo_admin_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFE0F2FE),
                                    contentColor = Color(0xFF075985)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAuthSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("raka@swasana.id") }
        var password by remember { mutableStateOf("demo123") }
        var selectedGroup by remember { mutableStateOf(HealthProfileGroup.SMOKING_RECOVERY) }

        ModalBottomSheet(
            onDismissRequest = { showAuthSheet = false },
            sheetState = sheetState,
            containerColor = PureWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (authTab == 0) "Masuk ke Swasana" else "Daftar Akun Swasana",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    IconButton(onClick = { showAuthSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                SegmentedPillBar(
                    options = listOf("Masuk", "Daftar Baru"),
                    selectedIndex = authTab,
                    onSelect = {
                        authTab = it
                        onClearError()
                        if (it == 1) {
                            email = ""
                            password = ""
                        } else {
                            email = "raka@swasana.id"
                            password = "demo123"
                        }
                    },
                    testTagPrefix = "auth_tab"
                )

                if (authTab == 1) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Kata Sandi") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (authTab == 1) {
                    Text(
                        text = "Profil Kesehatan Pernapasan",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HealthProfileGroup.entries.forEach { group ->
                            FilterChip(
                                selected = selectedGroup == group,
                                onClick = { selectedGroup = group },
                                label = { Text(group.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftMint,
                                    selectedLabelColor = DeepEmerald
                                )
                            )
                        }
                    }
                }

                if (!authError.isNullOrBlank()) {
                    Text(
                        text = authError,
                        style = MaterialTheme.typography.bodySmall,
                        color = AqiVeryUnhealthy
                    )
                }

                Button(
                    onClick = {
                        if (authTab == 0) {
                            onLogin(email, password)
                        } else {
                            onRegister(name, email, password, selectedGroup)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_button"),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald)
                ) {
                    Text(
                        text = if (authTab == 0) "Masuk Sekarang" else "Buat Akun & Mulai",
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeCleanAirHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0A4D38), Color(0xFF0F7A5A), Color(0xFF14B8A6))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Sunrise glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x66FEF3C7), Color(0x22A7F3D0), Color.Transparent),
                    center = Offset(w * 0.78f, h * 0.25f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.78f, h * 0.25f)
            )
            // Soft rolling eco hills
            val hillBack = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(w * 0.28f, h * 0.48f, w * 0.62f, h * 0.80f, w, h * 0.56f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillBack, color = Color(0x33FFFFFF))

            val hillFront = Path().apply {
                moveTo(0f, h * 0.85f)
                cubicTo(w * 0.35f, h * 0.64f, w * 0.68f, h * 0.92f, w, h * 0.72f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(hillFront, color = Color(0x4D063B2A))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x26FFFFFF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MintHighlight)
                    )
                    Text(
                        text = "Ekosistem Udara Bersih Indonesia",
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "6 Kota Terpantau • Peta Bebas Asap • AI Coach",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturePill("AQI Real-Time")
                    FeaturePill("Pulih Nikotin")
                    FeaturePill("Gemini AI")
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MintHighlight,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite
        )
    }
}
