package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MovieViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val isCheckingForUpdates by viewModel.isCheckingForUpdates.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    
    val overrideTmdbKey by viewModel.overrideTmdbApiKey.collectAsState()
    val overrideBannerKey by viewModel.overrideAdsterraBannerKey.collectAsState()
    val overrideBannerDomain by viewModel.overrideAdsterraBannerDomain.collectAsState()
    val overrideSmartLink by viewModel.overrideAdsterraSmartLink.collectAsState()
    val overrideBannerWidth by viewModel.overrideAdsterraBannerWidth.collectAsState()
    val overrideBannerHeight by viewModel.overrideAdsterraBannerHeight.collectAsState()

    var inputTmdbKey by remember(overrideTmdbKey) { mutableStateOf(overrideTmdbKey) }
    var inputBannerKey by remember(overrideBannerKey) { mutableStateOf(overrideBannerKey) }
    var inputBannerDomain by remember(overrideBannerDomain) { mutableStateOf(overrideBannerDomain) }
    var inputSmartLink by remember(overrideSmartLink) { mutableStateOf(overrideSmartLink) }
    var inputBannerWidth by remember(overrideBannerWidth) { mutableStateOf(overrideBannerWidth) }
    var inputBannerHeight by remember(overrideBannerHeight) { mutableStateOf(overrideBannerHeight) }

    val isTmdbActive = inputTmdbKey.isNotBlank() || (com.example.BuildConfig.TMDB_API_KEY.isNotBlank() && 
            !com.example.BuildConfig.TMDB_API_KEY.contains("YOUR_TMDB_API_KEY") && 
            !com.example.BuildConfig.TMDB_API_KEY.contains("TMDB"))

    val isBannerKeyActive = inputBannerKey.isNotBlank() || (com.example.BuildConfig.ADSTERRA_BANNER_KEY.isNotBlank() && 
            !com.example.BuildConfig.ADSTERRA_BANNER_KEY.contains("YOUR_ADSTERRA_KEY_HERE"))

    val isBannerDomainActive = inputBannerDomain.isNotBlank() || (com.example.BuildConfig.ADSTERRA_BANNER_DOMAIN.isNotBlank() && 
            !com.example.BuildConfig.ADSTERRA_BANNER_DOMAIN.contains("embargotechniquebattle.com") && 
            !com.example.BuildConfig.ADSTERRA_BANNER_DOMAIN.contains("YOUR_ADSTERRA_KEY_HERE"))

    val isSmartLinkActive = inputSmartLink.isNotBlank() || (com.example.BuildConfig.ADSTERRA_SMART_LINK.isNotBlank() && 
            !com.example.BuildConfig.ADSTERRA_SMART_LINK.contains("YOUR_ADSTERRA_SMART_LINK_HERE"))
    
    var cacheSize by remember { mutableStateOf("جاري الحساب...") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    
    // Recalculate cache size on enter and when cleared
    LaunchedEffect(Unit) {
        cacheSize = viewModel.getCacheSize()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "الإعدادات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Appearance
            SettingsSectionHeader(title = "المظهر والتصميم")
            
            SettingsToggleCard(
                title = "الوضع الداكن (Dark Mode)",
                subtitle = "تبديل مظهر التطبيق بين الوضع الليلي والوضع الفاتح",
                icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                checked = isDarkTheme,
                onCheckedChange = { viewModel.setDarkTheme(it) },
                testTag = "dark_mode_switch"
            )

            // Section: General Settings
            SettingsSectionHeader(title = "النظام والتنبيهات")
            
            SettingsToggleCard(
                title = "تفعيل الإشعارات",
                subtitle = "استقبال تنبيهات بآخر الأفلام والحلقات المضافة حديثاً",
                icon = Icons.Default.NotificationsActive,
                checked = isNotificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                testTag = "notifications_switch"
            )

            // Section: API Keys & Advertisements Management
            SettingsSectionHeader(title = "مفاتيح الخدمات والإعلانات")

            // Help & Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "كيف تعمل لوحة المفاتيح؟",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Red,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "هنا يمكنك وضع مفاتيح الخدمات والإعلانات الخاصة بك واختبارها فوراً في التطبيق. للحفاظ على هذه الإعدادات بشكل دائم دون فقدانها عند تحديث التطبيق، ننصح بوضعها في لوحة الأسرار (Secrets) الخاصة بمشروعك في Google AI Studio بالأسماء المكتوبة أسفل كل حقل.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Right,
                        lineHeight = 16.sp
                    )
                }
            }

            // 1. TMDB Key
            SettingsKeyInputCard(
                title = "مفتاح TMDB (TMDB_API_KEY)",
                subtitle = "المفتاح الخاص بجلب الأفلام والمسلسلات مباشرة والبحث الفوري من قاعدة بيانات TMDB.",
                placeholder = "أدخل مفتاح API (مثال: a1b2c3d4...)",
                value = inputTmdbKey,
                onValueChange = { inputTmdbKey = it },
                icon = Icons.Default.Key,
                statusActive = isTmdbActive,
                statusText = if (isTmdbActive) "🟢 نشط ومفعّل" else "⚪ غير مفعّل (الوضع الافتراضي)"
            )

            // 2. Adsterra Banner Key
            SettingsKeyInputCard(
                title = "مفتاح إعلان Adsterra (ADSTERRA_BANNER_KEY)",
                subtitle = "المعرف المكون من 32 رمزاً للبانر الإعلاني الخاص بك من حساب Adsterra.",
                placeholder = "أدخل مفتاح البانر (مثال: 24c50089c680...)",
                value = inputBannerKey,
                onValueChange = { inputBannerKey = it },
                icon = Icons.Default.Campaign,
                statusActive = isBannerKeyActive,
                statusText = if (isBannerKeyActive) "🟢 نشط ومفعّل" else "⚪ غير مفعّل"
            )

            // 3. Adsterra Banner Domain
            SettingsKeyInputCard(
                title = "نطاق إعلان Adsterra (ADSTERRA_BANNER_DOMAIN)",
                subtitle = "النطاق الخاص بسكريبت البانر الإعلاني من Adsterra.",
                placeholder = "أدخل النطاق (مثال: embargotechniquebattle.com)",
                value = inputBannerDomain,
                onValueChange = { inputBannerDomain = it },
                icon = Icons.Default.Language,
                statusActive = isBannerDomainActive,
                statusText = if (isBannerDomainActive) "🟢 نشط ومفعّل" else "⚪ غير مفعّل"
            )

            // 4. Adsterra Smart Link
            SettingsKeyInputCard(
                title = "الرابط الذكي (ADSTERRA_SMART_LINK)",
                subtitle = "الرابط الذكي الذي يفتح عند نقر زر التشغيل أو تبديل السيرفرات لزيادة الأرباح.",
                placeholder = "أدخل رابط Smart Link",
                value = inputSmartLink,
                onValueChange = { inputSmartLink = it },
                icon = Icons.Default.Link,
                statusActive = isSmartLinkActive,
                statusText = if (isSmartLinkActive) "🟢 نشط ومفعّل" else "⚪ غير مفعّل"
            )

            // Dynamic size options for Banner ad
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "مقاسات إعلان البانر (بالبكسل)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = inputBannerHeight,
                            onValueChange = { inputBannerHeight = it },
                            label = { Text("الارتفاع", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Red,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputBannerWidth,
                            onValueChange = { inputBannerWidth = it },
                            label = { Text("العرض", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Red,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Save & Clear actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveKeysOverrides(
                            tmdbKey = inputTmdbKey,
                            bannerKey = inputBannerKey,
                            bannerDomain = inputBannerDomain,
                            smartLink = inputSmartLink,
                            bannerWidth = inputBannerWidth,
                            bannerHeight = inputBannerHeight
                        )
                        Toast.makeText(context, "تم حفظ وتفعيل مفاتيحك الإعلانية والخدمية بنجاح!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.5f).height(44.dp).testTag("save_keys_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("حفظ التعديلات", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.clearKeysOverrides()
                        Toast.makeText(context, "تم استعادة الإعدادات الافتراضية بنجاح!", Toast.LENGTH_LONG).show()
                    },
                    border = BorderStroke(1.dp, Color.Gray),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                    modifier = Modifier.weight(1f).height(44.dp).testTag("clear_keys_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("افتراضي", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Section: Storage
            SettingsSectionHeader(title = "التخزين والأداء")
            
            SettingsActionCard(
                title = "مسح التخزين المؤقت (Clear Cache)",
                subtitle = "حجم الملفات المؤقتة الحالي: $cacheSize",
                icon = Icons.Default.DeleteSweep,
                actionLabel = "مسح الآن",
                onClick = {
                    val cleared = viewModel.clearCache()
                    if (cleared) {
                        cacheSize = viewModel.getCacheSize()
                        Toast.makeText(context, "تم مسح التخزين المؤقت بنجاح!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "فشل مسح التخزين المؤقت.", Toast.LENGTH_SHORT).show()
                    }
                },
                testTag = "clear_cache_button"
            )

            // Section: Support & Info
            SettingsSectionHeader(title = "عن التطبيق والدعم")
            
            SettingsClickableRow(
                title = "تقييم التطبيق على المتجر",
                subtitle = "ادعمنا بتقييم خمس نجوم للمساعدة في تطوير التطبيق",
                icon = Icons.Default.Star,
                onClick = {
                    try {
                        val playStoreIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("market://details?id=${context.packageName}")
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playStoreIntent)
                    } catch (e: Exception) {
                        val webIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(webIntent)
                    }
                },
                testTag = "rate_app_button"
            )

            SettingsClickableRow(
                title = "الانضمام لقناة تليجرام",
                subtitle = "تابع آخر التحديثات والأخبار وتواصل مع الدعم الفني",
                icon = Icons.Default.Send,
                onClick = {
                    val telegramUrl = "https://t.me/CinemaStreamOfficial"
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(telegramUrl)
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر فتح الرابط، يرجى التحقق من توفر تليجرام.", Toast.LENGTH_SHORT).show()
                    }
                },
                testTag = "telegram_button"
            )

            SettingsClickableRow(
                title = if (isCheckingForUpdates) "جاري التحقق من التحديثات..." else "التحقق من وجود تحديثات",
                subtitle = "تأكد من استخدامك لأحدث إصدار للحصول على أفضل استقرار",
                icon = Icons.Default.SystemUpdate,
                onClick = {
                    if (!isCheckingForUpdates) {
                        viewModel.checkForUpdates(manual = true)
                    }
                },
                testTag = "check_updates_button"
            )

            SettingsClickableRow(
                title = "سياسة الخصوصية",
                subtitle = "عرض وثيقة سياسة الخصوصية الخاصة بالتطبيق والأمان والامتثال",
                icon = Icons.Default.PrivacyTip,
                onClick = {
                    showPrivacyDialog = true
                },
                testTag = "privacy_policy_button"
            )

            // Version info footer
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cinema Stream v1.2.0\nجميع الحقوق محفوظة © 2026",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyDialog = false })
    }

    updateState?.let { updateInfo ->
        AppUpdateDialog(
            updateInfo = updateInfo,
            onDismiss = { viewModel.dismissUpdate() },
            onUpdateClick = { url ->
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    ).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "تعذر فتح الرابط للتحديث.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = Color.Red,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        textAlign = TextAlign.Right
    )
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Red,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.DarkGray
                ),
                modifier = Modifier.testTag(testTag)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right,
                        lineHeight = 15.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionLabel: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag(testTag)
            ) {
                Text(text = actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right,
                        lineHeight = 15.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right,
                        lineHeight = 15.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("موافق ومتابعة", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "سياسة الخصوصية لـ Cinema Stream",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "نحن في Cinema Stream نلتزم بحماية خصوصية مستخدمينا وضمان تجربة آمنة تماماً. يوضح هذا المستند البنود القانونية لخصوصية البيانات:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "١. البيانات الشخصية وجمع المعلومات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "تطبيق Cinema Stream لا يقوم بجمع أو تخزين أي بيانات شخصية مباشرة من المستخدمين. لا نطلب تسجيل حساب، ولا نصل إلى بياناتك الشخصية الحساسة.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Right,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "٢. التخزين المحلي وسجل المتابعة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "يتم حفظ قائمتك المفضلة (Watchlist) وسجل متابعتك ومشاهدتك محلياً بالكامل على جهازك الشخصي باستخدام تقنيات قواعد البيانات المحلية الآمنة (Room Database). لا نقوم بمشاركة هذه البيانات خارج جهازك أبداً.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Right,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "٣. الروابط الخارجية وتليجرام",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "قد يتضمن التطبيق روابط خارجية إلى قنوات تليجرام أو مواقع الدعم الفني. تليجرام وأي أطراف خارجية لديهم سياسات خصوصية مستقلة تماماً لا نملك أي سيطرة عليها ولا نتحمل مسؤولية محتواها.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Right,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "٤. أمان الاتصال والشبكة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "يستخدم التطبيق بروتوكولات شبكة آمنة ومشفرة (HTTPS) بالكامل للتواصل مع الواجهات الخارجية (API) وجلب معلومات الأفلام لضمان حماية حركة المرور الخاصة بك ومنع أي تجسس.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Right,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "٥. التعديلات والتحديثات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right
                )
                Text(
                    text = "قد نقوم بتحديث سياسة الخصوصية هذه من وقت لآخر لمواكبة التغيرات التقنية أو التنظيمية. استمرارك في استخدام التطبيق يعتبر موافقة ضمنية على الشروط المحدثة.",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Right,
                    lineHeight = 17.sp
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("privacy_dialog")
    )
}

@Composable
fun AppUpdateDialog(
    updateInfo: com.example.ui.UpdateInfo,
    onDismiss: () -> Unit,
    onUpdateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onUpdateClick(updateInfo.updateUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تحديث الآن", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (!updateInfo.isForceUpdate) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("لاحقاً")
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "تحديث جديد متوفر! (v${updateInfo.latestVersion})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "يتوفر إصدار جديد من Cinema Stream برقم v${updateInfo.latestVersion}. يرجى التحديث فوراً للحصول على الميزات والإصلاحات الجديدة التالية:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "سجل التغييرات والتحديثات:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = updateInfo.releaseNotes,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Right,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.testTag("update_dialog")
    )
}

@Composable
fun SettingsKeyInputCard(
    title: String,
    subtitle: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    statusActive: Boolean,
    statusText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            if (statusActive) Color(0xFF2E7D32).copy(alpha = 0.15f)
                            else Color.Gray.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (statusActive) Color(0xFF4CAF50) else Color.LightGray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Red.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Right,
                lineHeight = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Left // Keys are Latin
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }
    }
}
