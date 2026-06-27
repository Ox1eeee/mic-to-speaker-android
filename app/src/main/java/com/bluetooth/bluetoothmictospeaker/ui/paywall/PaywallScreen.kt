package com.bluetooth.bluetoothmictospeaker.ui.paywall

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluetooth.bluetoothmictospeaker.billing.SubscriptionViewModel
import com.bluetooth.bluetoothmictospeaker.ui.theme.AccentCyan
import com.bluetooth.bluetoothmictospeaker.ui.theme.DarkBackground
import com.bluetooth.bluetoothmictospeaker.ui.theme.TextSecondary

private val PaywallBlue = Color(0xFF2979FF)
private val PaywallBlueBright = Color(0xFF448AFF)
private val BestValueGreen = Color(0xFF00E676)
private val CardBackground = Color(0xFF1A1A2E)
private val CardBorderDefault = Color(0xFF333355)

@Composable
fun PaywallScreen(
    subscriptionViewModel: SubscriptionViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by subscriptionViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mic icon with blue glow background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PaywallBlue.copy(alpha = 0.6f),
                                PaywallBlue.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    PaywallBlue,
                                    PaywallBlueBright
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Premium Features",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Get unlimited access to all premium features",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Feature list
            FeatureItem(
                icon = Icons.Default.CheckCircle,
                text = "All Voice Effects Unlocked",
                iconTint = PaywallBlue
            )
            Spacer(modifier = Modifier.height(18.dp))
            FeatureItem(
                icon = Icons.Default.VolumeUp,
                text = "Professional Audio Quality",
                iconTint = Color.White
            )
            Spacer(modifier = Modifier.height(18.dp))
            FeatureItem(
                icon = Icons.Default.AllInclusive,
                text = "Unlimited Usage",
                iconTint = Color.White
            )
            Spacer(modifier = Modifier.height(18.dp))
            FeatureItem(
                icon = Icons.Default.Bluetooth,
                text = "Bluetooth Support",
                iconTint = Color.White
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Weekly Plan Card
            PlanCard(
                title = "Weekly Plan",
                priceText = if (state.weeklyPrice.isNotEmpty())
                    "${state.weeklyPrice} per week after 3-day trial"
                else
                    "per week after 3-day trial",
                badge = null,
                isSelected = state.selectedPlan == 0,
                onClick = { subscriptionViewModel.selectPlan(0) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lifetime Plan Card
            PlanCard(
                title = "Lifetime Plan",
                priceText = if (state.lifetimePrice.isNotEmpty())
                    state.lifetimePrice
                else
                    "",
                badge = "BEST VALUE",
                isSelected = state.selectedPlan == 1,
                onClick = { subscriptionViewModel.selectPlan(1) }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // CTA Button
            Button(
                onClick = {
                    activity?.let { subscriptionViewModel.purchaseSelected(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaywallBlue
                ),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (state.selectedPlan == 0) "Try for Free" else "Get Lifetime Access",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Restore & Terms row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Restore",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        subscriptionViewModel.restorePurchases()
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Terms of Use & Privacy Policy",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aztty.com/privacy-policy-15/"))
                        context.startActivity(intent)
                    }
                )
            }

            // Error message
            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    fontSize = 13.sp,
                    color = Color(0xFFFF5252),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Close button (rendered after Column so it's on top and clickable)
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 12.dp, end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333333)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    text: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun PlanCard(
    title: String,
    priceText: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PaywallBlue else CardBorderDefault

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BestValueGreen)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
                if (priceText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = priceText,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Radio indicator
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) PaywallBlue else Color(0xFF555577),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(PaywallBlue)
                    )
                }
            }
        }
    }
}
