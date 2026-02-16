package com.run.peakflow.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.run.peakflow.presentation.components.SignInComponent
import com.run.peakflow.ui.components.GoogleSignInButton
import com.run.peakflow.ui.theme.PeakFlowSpacing
import com.run.peakflow.ui.theme.PeakFlowTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(component: SignInComponent) {
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
                .padding(horizontal = PeakFlowSpacing.screenHorizontal)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Text(
                text = "Welcome Back",
                style = PeakFlowTypography.screenTitle(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Text(
                text = "Sign in to continue your fitness journey",
                style = PeakFlowTypography.bodyMain(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.emailOrPhone,
                onValueChange = { component.onEmailOrPhoneChanged(it) },
                label = { Text("Email or Phone") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(PeakFlowSpacing.elementGap))

            OutlinedTextField(
                value = state.password,
                onValueChange = { component.onPasswordChanged(it) },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { component.onTogglePasswordVisibility() }) {
                        Icon(if (state.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                },
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            TextButton(
                onClick = { component.onForgotPasswordClick() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Forgot Password?", style = MaterialTheme.typography.labelLarge)
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Button(
                onClick = { component.onSignInClick() },
                enabled = !state.isLoading && !state.isGoogleLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                else Text("SIGN IN")
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("OR", modifier = Modifier.padding(horizontal = 16.dp), style = PeakFlowTypography.labelSecondary())
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            GoogleSignInButton(
                text = "Continue with Google",
                onClick = { component.onGoogleSignInClick() },
                isLoading = state.isGoogleLoading,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(PeakFlowSpacing.sectionGap))

            TextButton(onClick = { component.onSignUpClick() }) {
                Text("Don't have an account? Sign Up", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
