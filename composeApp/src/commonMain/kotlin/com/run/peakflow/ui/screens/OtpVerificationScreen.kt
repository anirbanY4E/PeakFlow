package com.run.peakflow.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.OtpVerificationComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(component: OtpVerificationComponent) {
    val state by component.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { component.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Verify Your Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "We've sent a 6-digit code to",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = state.sentTo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // OTP Input
            OtpInputField(
                otpText = state.otp,
                onOtpChange = { component.onOtpChanged(it) }
            )

            // Error message
            if (state.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Countdown or resend
            if (state.resendCountdown > 0) {
                Text(
                    text = "Resend code in 0:${state.resendCountdown.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Verify button
            Button(
                onClick = { component.onVerifyClick() },
                enabled = !state.isLoading && state.otp.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Text("VERIFY")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resend button
            TextButton(
                onClick = { component.onResendClick() },
                enabled = state.resendCountdown == 0 && !state.isResending
            ) {
                if (state.isResending) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                } else {
                    Text(
                        text = "Didn't receive code? Resend OTP",
                        color = if (state.resendCountdown == 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpInputField(
    otpText: String,
    onOtpChange: (String) -> Unit
) {
    BasicTextField(
        value = otpText,
        onValueChange = { if (it.length <= 6) onOtpChange(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val char = otpText.getOrNull(index)?.toString() ?: ""
                    val isFocused = otpText.length == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = if (isFocused) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}