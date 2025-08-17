package com.paxti.hdrezkaapp.utils

import com.paxti.hdrezkaapp.objects.SettingsData
import java.net.URI

object UrlUtils {
    /**
     * Converts a full URL to a relative path by removing the domain
     * @param fullUrl The complete URL (e.g., "https://hdrezka.website/films/action/123-film.html")
     * @return Relative path (e.g., "/films/action/123-film.html")
     */
    fun getRelativePathFromUrl(fullUrl: String): String {
        return try {
            val uri = URI(fullUrl)
            uri.path + if (uri.query != null) "?${uri.query}" else ""
        } catch (e: Exception) {
            // If URL parsing fails, try to extract path manually
            val parts = fullUrl.split("://")
            if (parts.size >= 2) {
                val afterProtocol = parts[1]
                val pathStart = afterProtocol.indexOf('/')
                if (pathStart != -1) {
                    afterProtocol.substring(pathStart)
                } else {
                    fullUrl
                }
            } else {
                fullUrl
            }
        }
    }

    /**
     * Builds a complete URL using the current provider and relative path
     * @param relativePath The relative path (e.g., "/films/action/123-film.html")
     * @return Complete URL using current provider (e.g., "https://hdrezka.website/films/action/123-film.html")
     */
    fun buildFullUrl(relativePath: String): String {
        val provider = SettingsData.provider ?: "https://hdrezka.website"
        return if (relativePath.startsWith("http")) {
            // If it's already a full URL, convert to relative first then rebuild
            val relPath = getRelativePathFromUrl(relativePath)
            provider + relPath
        } else {
            // If it's already relative, just prepend provider
            provider + if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        }
    }

    /**
     * Converts poster path to relative if it's a full URL
     */
    fun getRelativePosterPath(posterPath: String): String {
        return if (posterPath.startsWith("http")) {
            getRelativePathFromUrl(posterPath)
        } else {
            posterPath
        }
    }

    /**
     * Builds full poster URL using current provider
     */
    fun buildFullPosterUrl(relativePosterPath: String): String {
        if (relativePosterPath.startsWith("http")) {
            return relativePosterPath
        }
        val provider = SettingsData.provider ?: "https://hdrezka.website"
        return provider + if (relativePosterPath.startsWith("/")) relativePosterPath else "/$relativePosterPath"
    }

    /**
     * Extracts film ID from URL path
     * @param url URL like "/films/horror/81533-kuski-1982.html" or "https://hdrezka.website/films/horror/81533-kuski-1982.html"
     * @return Film ID as Int, or null if not found
     */
    fun extractFilmIdFromUrl(url: String): Int? {
        return try {
            // Extract the path part if it's a full URL
            val path = if (url.startsWith("http")) {
                getRelativePathFromUrl(url)
            } else {
                url
            }

            // Pattern: /films/category/ID-title.html
            // Extract the filename part (e.g., "81533-kuski-1982.html")
            val filename = path.substringAfterLast("/")

            // Extract the ID part before the first dash
            val idPart = filename.substringBefore("-")

            // Try to parse as integer
            idPart.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
