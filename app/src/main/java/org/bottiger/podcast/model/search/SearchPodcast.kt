package org.bottiger.podcast.model.search

import org.bottiger.podcast.model.Library
import org.bottiger.podcast.provider.IEpisode
import org.bottiger.podcast.provider.ISubscription
import org.bottiger.podcast.provider.Subscription



/**
 * Created by bottiger on 17/02/2018.
 */
class SearchPodcast(val library: Library) {

    fun findSubscription(searchTerm: String) : ISubscription? {

        val subscriptions = library.liveSubscriptions.value.orEmpty();

        subscriptions.forEach {
            if (matchStrings(searchTerm, it.title)) {
                return it;
            }
        }

        return null
    }

    fun findEpisode(searchTerm: String) : IEpisode? {

        val episodes = library.episodes;

        episodes.forEach {
            if (matchStrings(searchTerm, it.title)) {
                return it;
            }
        }

        return null
    }

    private fun matchStrings(searchTerm: String, matchingString: String) : Boolean {
        return similarity(searchTerm.toLowerCase(), matchingString.toLowerCase()) > 0.5;
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     */
    private fun similarity(s1: String, s2: String): Double {
        var longer = s1
        var shorter = s2
        if (s1.length < s2.length) { // longer should always have greater length
            longer = s2
            shorter = s1
        }
        val longerLength = longer.length
        return if (longerLength == 0) {
            1.0 /* both strings are zero length */
        } else (longerLength - editDistance(longer, shorter)) / longerLength.toDouble()
        /* // If you have Apache Commons Text, you can use it to calculate the edit distance:
    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    return (longerLength - levenshteinDistance.apply(longer, shorter)) / (double) longerLength; */

    }

    // Example implementation of the Levenshtein Edit Distance
    // See http://rosettacode.org/wiki/Levenshtein_distance#Java
    private fun editDistance(s1: String, s2: String): Int {
        var s1 = s1
        var s2 = s2
        s1 = s1.toLowerCase()
        s2 = s2.toLowerCase()

        val costs = IntArray(s2.length + 1)
        for (i in 0..s1.length) {
            var lastValue = i
            for (j in 0..s2.length) {
                if (i == 0)
                    costs[j] = j
                else {
                    if (j > 0) {
                        var newValue = costs[j - 1]
                        if (s1[i - 1] != s2[j - 1])
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1
                        costs[j - 1] = lastValue
                        lastValue = newValue
                    }
                }
            }
            if (i > 0)
                costs[s2.length] = lastValue
        }
        return costs[s2.length]
    }

}