package com.speedcheck.android

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Dashboard : NavKey

@Serializable
data object History : NavKey

@Serializable
data object Settings : NavKey
