/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetcaster.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

/**
 * A data repository for [Podcast] instances.
 *
 * Currently this is backed only with data in memory. Ideally this would be backed by a
 * Room database, to allow persistence and easier querying.
 *
 * @param mainDispatcher The main app [CoroutineDispatcher]
 * @param computationDispatcher [CoroutineDispatcher] to run computationally heavy tasks on
 */
class PodcastStore(
    private val mainDispatcher: CoroutineDispatcher,
    private val computationDispatcher: CoroutineDispatcher
) {
    val items: Flow<Collection<Podcast>>
        get() = _items.map { it.values }

    private val _items = MutableStateFlow(emptyMap<String, Podcast>())

    /**
     * Return a flow containing the [Podcast] with the given [uri].
     */
    fun podcastWithUri(uri: String): Flow<Podcast> {
        return _items.mapNotNull { it[uri] }.distinctUntilChanged()
    }

    /**
     * Returns a flow containing the entire collection of podcasts, sorted by the last episode
     * publish date for each podcast.
     */
    fun sortedByLastEpisodeDate(
        descending: Boolean = true
    ): Flow<List<Podcast>> = _items.map { podcasts ->
        // Run on the default dispatcher, since sorting is non-trivial
        if (descending) {
            podcasts.values.sortedByDescending { it.lastEpisodeDate }
        } else {
            podcasts.values.sortedBy { it.lastEpisodeDate }
        }
    }.flowOn(computationDispatcher)

    /**
     * Add a new [Podcast] to this store.
     *
     * This automatically switches to the main thread to maintain thread consistency.
     */
    suspend fun addPodcast(podcast: Podcast) = withContext(mainDispatcher) {
        _items.value = _items.value.toMutableMap().apply { put(podcast.uri, podcast) }
    }

    /**
     * Clear any [Podcast]s currently stored in this store.
     *
     * This automatically switches to the main thread to maintain thread consistency.
     */
    suspend fun clear() = withContext(mainDispatcher) {
        _items.value = emptyMap()
    }

    fun isEmpty() = _items.value.isEmpty()
}
