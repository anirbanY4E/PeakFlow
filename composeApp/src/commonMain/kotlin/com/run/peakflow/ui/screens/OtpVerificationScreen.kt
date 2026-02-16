package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.run.peakflow.presentation.components.OtpVerificationComponent
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = PeakFlowSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Text(
                text = "Verify OTP",
                style = PeakFlowTypography.screenTitle(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "We've sent a 6-digit code to ${state.sentTo}",
                style = PeakFlowTypography.bodyMain(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.otp,
                onValueChange = { if (it.length <= 6) component.onOtpChanged(it) },
                label = { Text("6-Digit Code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                shape = MaterialTheme.shapes.medium
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Button(
                onClick = { component.onVerifyClick() },
                enabled = !state.isLoading && state.otp.length == 6,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("VERIFY")
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            TextButton(
                onClick = { component.onResendClick() },
                enabled = !state.isResending && state.resendCountdown == 0
            ) {
                Text(
                    if (state.resendCountdown > 0) "Resend in ${state.resendCountdown}s"
                    else "Didn't receive a code? Resend"
                )
            }
        }
    }
}
