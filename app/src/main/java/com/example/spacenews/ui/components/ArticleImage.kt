package com.example.spacenews.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.placeholder
import com.example.spacenews.R

@Composable
@OptIn(ExperimentalGlideComposeApi::class)
fun ArticleImage(url: String?, contentDescription: String, modifier: Modifier = Modifier) {
    Box(modifier) {
        GlideImage(
            model = url?.takeIf(String::isNotBlank),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = placeholder(R.drawable.space_image_placeholder),
            failure = placeholder(R.drawable.space_image_placeholder)
        )
    }
}
