package com.example.recommend

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Canonical data root: `artifacts/trustlist-production/public/data/<collection>`.
 * All app reads/writes must use [trustListDataRoot] so environment cannot drift.
 */
object FirestorePaths {
    const val ARTIFACTS = "artifacts"
    const val TRUSTLIST_DOCUMENT = "trustlist-production"
    const val PUBLIC = "public"
    const val DATA = "data"
}

fun FirebaseFirestore.trustListDataRoot(): DocumentReference =
    collection(FirestorePaths.ARTIFACTS).document(FirestorePaths.TRUSTLIST_DOCUMENT)
        .collection(FirestorePaths.PUBLIC).document(FirestorePaths.DATA)
