package com.example.util

import java.util.Locale

fun formatStatCount(count: Int): String {
  return when {
    count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    else -> count.toString()
  }
}
