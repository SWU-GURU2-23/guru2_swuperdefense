package com.adroid.guru2_swuperdefense.data.repository

import com.adroid.guru2_swuperdefense.data.remote.model.BoardCommentDto
import com.adroid.guru2_swuperdefense.data.remote.model.BoardPostDto
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/** 게시판 Firestore 접근 지점. 작성자 UID는 공개 문서 대신 별도 소유권 문서에 저장해 익명성을 DB 조회 단계에서도 유지한다. */
class BoardRepository private constructor() {
    data class UserPostState(
        val hasLiked: Boolean,
        val isScrapped: Boolean,
        val hasRead: Boolean
    )

    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = AuthRepository.instance
    private val posts = firestore.collection(POSTS_COLLECTION)
    private val postOwners = firestore.collection(POST_OWNERS_COLLECTION)

    fun observePosts(
        onChanged: (List<BoardPostDto>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        var postDocuments: List<DocumentSnapshot> = emptyList()
        var ownedPostIds: Set<String> = emptySet()
        var postsLoaded = false
        var ownershipLoaded = false

        fun emitIfReady() {
            if (!postsLoaded || !ownershipLoaded) return
            onChanged(
                postDocuments.mapNotNull { document ->
                    BoardPostDto.from(
                        document = document,
                        isMine = document.id in ownedPostIds ||
                            document.getString(LEGACY_AUTHOR_UID_FIELD) == uid
                    )
                }
            )
        }

        val postRegistration = posts.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                postDocuments = snapshot?.documents.orEmpty()
                postsLoaded = true
                emitIfReady()
            }

        val ownerRegistration = postOwners.whereEqualTo(OWNER_UID_FIELD, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                ownedPostIds = snapshot?.documents.orEmpty().mapTo(mutableSetOf()) { it.id }
                ownershipLoaded = true
                emitIfReady()
            }

        return ListenerRegistration {
            postRegistration.remove()
            ownerRegistration.remove()
        }
    }

    fun observeComments(
        postDocumentId: String,
        onChanged: (List<BoardCommentDto>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration =
        posts.document(postDocumentId)
            .collection(COMMENTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.documents.orEmpty().mapNotNull(BoardCommentDto::from))
            }

    fun getPostByLocalId(localId: Int): Task<BoardPostDto?> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        return posts.whereEqualTo("localId", localId)
            .limit(1)
            .get()
            .continueWithTask { postTask ->
                if (!postTask.isSuccessful) {
                    throw postTask.exception
                        ?: IllegalStateException("게시글을 불러올 수 없습니다.")
                }
                val document = postTask.result?.documents?.firstOrNull()
                    ?: return@continueWithTask Tasks.forResult(null)
                loadOwnedPostIds(uid).continueWith { ownerTask ->
                    if (!ownerTask.isSuccessful) {
                        throw ownerTask.exception
                            ?: IllegalStateException("게시글 소유권을 확인할 수 없습니다.")
                    }
                    BoardPostDto.from(
                        document,
                        document.id in ownerTask.result ||
                            document.getString(LEGACY_AUTHOR_UID_FIELD) == uid
                    )
                }
            }
    }

    fun getScrappedPosts(): Task<List<BoardPostDto>> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        return loadOwnedPostIds(uid).continueWithTask { ownerTask ->
            if (!ownerTask.isSuccessful) {
                throw ownerTask.exception
                    ?: IllegalStateException("게시글 소유권을 확인할 수 없습니다.")
            }
            val ownedPostIds = ownerTask.result
            posts.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .continueWithTask { postsTask ->
                    if (!postsTask.isSuccessful) {
                        throw postsTask.exception
                            ?: IllegalStateException("게시글을 불러올 수 없습니다.")
                    }
                    val checkTasks = postsTask.result?.documents.orEmpty().mapNotNull { document ->
                        val post = BoardPostDto.from(
                            document,
                            document.id in ownedPostIds ||
                                document.getString(LEGACY_AUTHOR_UID_FIELD) == uid
                        ) ?: return@mapNotNull null
                        document.reference.collection(SCRAPS_COLLECTION).document(uid).get()
                            .continueWith { scrapTask ->
                                if (scrapTask.isSuccessful && scrapTask.result.exists()) post else null
                            }
                    }
                    Tasks.whenAllSuccess<BoardPostDto?>(checkTasks)
                }
        }.continueWith { resultTask ->
            if (!resultTask.isSuccessful) {
                throw resultTask.exception
                    ?: IllegalStateException("스크랩 목록을 불러올 수 없습니다.")
            }
            resultTask.result.filterNotNull()
        }
    }

    fun createPost(
        category: String,
        title: String,
        body: String,
        isAnonymous: Boolean
    ): Task<DocumentReference> {
        val user = requireNotNull(authRepository.currentUser) { "로그인이 필요합니다." }
        val document = posts.document()
        val ownerDocument = postOwners.document(document.id)
        val localId = document.id.hashCode() and Int.MAX_VALUE
        val batch = firestore.batch()
        batch.set(
            document,
            mapOf(
                "localId" to localId,
                "authorDisplayName" to if (isAnonymous) {
                    "익명"
                } else {
                    user.email.orEmpty().substringBefore("@")
                },
                "isAnonymous" to isAnonymous,
                "category" to category,
                "title" to title,
                "body" to body,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "viewCount" to 0,
                "commentCount" to 0,
                "likeCount" to 0,
                "nextAnonymousNumber" to 1
            )
        )
        batch.set(
            ownerDocument,
            mapOf(
                OWNER_UID_FIELD to user.uid,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        return batch.commit().continueWith { saveTask ->
            if (!saveTask.isSuccessful) {
                throw saveTask.exception
                    ?: IllegalStateException("게시글을 저장할 수 없습니다.")
            }
            document
        }
    }

    fun updatePost(
        documentId: String,
        category: String,
        title: String,
        body: String,
        isAnonymous: Boolean
    ): Task<Void> {
        val email = authRepository.currentUser?.email.orEmpty()
        return posts.document(documentId).update(
            mapOf(
                "category" to category,
                "title" to title,
                "body" to body,
                "authorDisplayName" to if (isAnonymous) {
                    "익명"
                } else {
                    email.substringBefore("@")
                },
                "isAnonymous" to isAnonymous,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun deletePost(documentId: String): Task<Void> {
        val post = posts.document(documentId)
        return post.get().continueWithTask { postTask ->
            if (!postTask.isSuccessful) {
                throw postTask.exception
                    ?: IllegalStateException("게시글을 확인할 수 없습니다.")
            }
            if (postTask.result.getString(LEGACY_AUTHOR_UID_FIELD) != null) {
                post.delete()
            } else {
                val batch = firestore.batch()
                batch.delete(postOwners.document(documentId))
                batch.delete(post)
                batch.commit()
            }
        }
    }

    fun addComment(
        postDocumentId: String,
        body: String,
        isAnonymous: Boolean = false
    ): Task<Void> {
        val user = requireNotNull(authRepository.currentUser) { "로그인이 필요합니다." }
        val post = posts.document(postDocumentId)
        val comment = post.collection(COMMENTS_COLLECTION).document()
        val commentOwner = post.collection(COMMENT_OWNERS_COLLECTION).document(comment.id)

        if (!isAnonymous) {
            val batch = firestore.batch()
            batch.set(comment, commentData(user.email, body, false, null))
            batch.set(
                commentOwner,
                mapOf(
                    OWNER_UID_FIELD to user.uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(post, "commentCount", FieldValue.increment(1))
            return batch.commit()
        }

        val anonymousAuthor = post.collection(ANONYMOUS_AUTHORS_COLLECTION).document(user.uid)
        return firestore.runTransaction { transaction ->
            val existingNumber = transaction.get(anonymousAuthor)
                .getLong("number")
                ?.toInt()
            val number = existingNumber ?: (
                transaction.get(post).getLong("nextAnonymousNumber")?.toInt() ?: 1
            )
            if (existingNumber == null) {
                transaction.set(
                    anonymousAuthor,
                    mapOf(
                        "number" to number,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(post, "nextAnonymousNumber", number + 1)
            }
            transaction.set(comment, commentData(user.email, body, true, number))
            transaction.set(
                commentOwner,
                mapOf(
                    OWNER_UID_FIELD to user.uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            transaction.update(post, "commentCount", FieldValue.increment(1))
            null
        }
    }

    fun markRead(postDocumentId: String): Task<Void> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        return posts.document(postDocumentId)
            .collection(READS_COLLECTION)
            .document(uid)
            .set(mapOf("readAt" to FieldValue.serverTimestamp()))
    }

    fun getUserPostState(postDocumentId: String): Task<UserPostState> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        val post = posts.document(postDocumentId)
        val likeTask = post.collection(LIKES_COLLECTION).document(uid).get()
        val scrapTask = post.collection(SCRAPS_COLLECTION).document(uid).get()
        val readTask = post.collection(READS_COLLECTION).document(uid).get()

        return Tasks.whenAllSuccess<DocumentSnapshot>(likeTask, scrapTask, readTask)
            .continueWith { task ->
                val documents = task.result
                UserPostState(
                    hasLiked = documents[0].exists(),
                    isScrapped = documents[1].exists(),
                    hasRead = documents[2].exists()
                )
            }
    }

    fun setScrapped(postDocumentId: String, scrapped: Boolean): Task<Void> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        val scrap = posts.document(postDocumentId)
            .collection(SCRAPS_COLLECTION)
            .document(uid)
        return if (scrapped) {
            scrap.set(mapOf("createdAt" to FieldValue.serverTimestamp()))
        } else {
            scrap.delete()
        }
    }

    fun toggleLike(postDocumentId: String): Task<Boolean> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        val post = posts.document(postDocumentId)
        val like = post.collection(LIKES_COLLECTION).document(uid)
        return firestore.runTransaction { transaction ->
            val alreadyLiked = transaction.get(like).exists()
            if (alreadyLiked) {
                transaction.delete(like)
                transaction.update(post, "likeCount", FieldValue.increment(-1))
            } else {
                transaction.set(like, mapOf("createdAt" to FieldValue.serverTimestamp()))
                transaction.update(post, "likeCount", FieldValue.increment(1))
            }
            !alreadyLiked
        }
    }

    fun incrementViewCount(postDocumentId: String): Task<Void> =
        posts.document(postDocumentId)
            .update("viewCount", FieldValue.increment(1))

    private fun loadOwnedPostIds(uid: String): Task<Set<String>> =
        postOwners.whereEqualTo(OWNER_UID_FIELD, uid)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception
                        ?: IllegalStateException("게시글 소유권을 확인할 수 없습니다.")
                }
                task.result.documents.mapTo(mutableSetOf()) { it.id }
            }

    private fun commentData(
        authorEmail: String?,
        body: String,
        isAnonymous: Boolean,
        anonymousNumber: Int?
    ): Map<String, Any?> =
        mapOf(
            "authorDisplayName" to if (isAnonymous) {
                "익명"
            } else {
                authorEmail?.substringBefore("@").orEmpty()
            },
            "body" to body,
            "isAnonymous" to isAnonymous,
            "anonymousNumber" to anonymousNumber,
            "createdAt" to FieldValue.serverTimestamp()
        )

    companion object {
        private const val POSTS_COLLECTION = "posts"
        private const val POST_OWNERS_COLLECTION = "postOwners"
        private const val COMMENTS_COLLECTION = "comments"
        private const val COMMENT_OWNERS_COLLECTION = "commentOwners"
        private const val LIKES_COLLECTION = "likes"
        private const val SCRAPS_COLLECTION = "scraps"
        private const val READS_COLLECTION = "reads"
        private const val ANONYMOUS_AUTHORS_COLLECTION = "anonymousAuthors"
        private const val OWNER_UID_FIELD = "ownerUid"
        private const val LEGACY_AUTHOR_UID_FIELD = "authorUid"

        val instance: BoardRepository by lazy { BoardRepository() }
    }
}
