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

/**
 * 여러 기기에서 공유하는 게시판 데이터의 Firestore 접근 지점.
 *
 * Fragment가 Firestore 컬렉션 경로와 트랜잭션을 직접 다루지 않도록 모든 원격 작업을
 * 이 클래스에 모았다. 화면 연결은 이 저장소의 메서드만 호출하도록 진행한다.
 */
class BoardRepository private constructor() {
    data class UserPostState(
        val hasLiked: Boolean,
        val isScrapped: Boolean,
        val hasRead: Boolean
    )

    private val firestore = FirebaseFirestore.getInstance()
    private val authRepository = AuthRepository.instance
    private val posts = firestore.collection(POSTS_COLLECTION)

    fun observePosts(
        onChanged: (List<BoardPostDto>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration =
        posts.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onChanged(snapshot?.documents.orEmpty().mapNotNull(BoardPostDto::from))
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

    fun getPostByLocalId(localId: Int): Task<BoardPostDto?> =
        posts.whereEqualTo("localId", localId)
            .limit(1)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception
                        ?: IllegalStateException("게시글을 불러올 수 없습니다.")
                }
                task.result?.documents?.firstOrNull()?.let(BoardPostDto::from)
            }

    fun getScrappedPosts(): Task<List<BoardPostDto>> {
        val uid = requireNotNull(authRepository.currentUser?.uid) { "로그인이 필요합니다." }
        return posts.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .continueWithTask { postsTask ->
                if (!postsTask.isSuccessful) {
                    throw postsTask.exception
                        ?: IllegalStateException("게시글을 불러올 수 없습니다.")
                }
                val checkTasks = postsTask.result?.documents.orEmpty().mapNotNull { document ->
                    val post = BoardPostDto.from(document) ?: return@mapNotNull null
                    document.reference.collection(SCRAPS_COLLECTION).document(uid).get()
                        .continueWith { scrapTask ->
                            if (scrapTask.isSuccessful && scrapTask.result.exists()) post else null
                        }
                }
                Tasks.whenAllSuccess<BoardPostDto?>(checkTasks)
            }
            .continueWith { resultTask ->
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
        val email = user.email.orEmpty()
        val document = posts.document()
        val localId = document.id.hashCode() and Int.MAX_VALUE
        return document.set(
            mapOf(
                "localId" to localId,
                "authorUid" to user.uid,
                "authorDisplayName" to if (isAnonymous) "익명" else email.substringBefore("@"),
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
        ).continueWith { saveTask ->
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
                "authorDisplayName" to if (isAnonymous) "익명" else email.substringBefore("@"),
                "isAnonymous" to isAnonymous,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun deletePost(documentId: String): Task<Void> {
        val post = posts.document(documentId)
        val childCollections = listOf(
            COMMENTS_COLLECTION,
            LIKES_COLLECTION,
            SCRAPS_COLLECTION,
            READS_COLLECTION,
            ANONYMOUS_AUTHORS_COLLECTION
        )
        return Tasks.whenAll(childCollections.map { deleteCollection(post, it) })
            .continueWithTask { cleanupTask ->
                if (!cleanupTask.isSuccessful) {
                    throw cleanupTask.exception
                        ?: IllegalStateException("게시글의 연결 데이터를 삭제할 수 없습니다.")
                }
                post.delete()
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
        if (!isAnonymous) {
            val batch = firestore.batch()
            batch.set(
                comment,
                commentData(user.uid, user.email, body, false, null)
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
            transaction.set(
                comment,
                commentData(user.uid, user.email, body, true, number)
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

    private fun deleteCollection(
        parent: DocumentReference,
        collectionName: String
    ): Task<Void> =
        parent.collection(collectionName).get().continueWithTask { queryTask ->
            if (!queryTask.isSuccessful) {
                throw queryTask.exception
                    ?: IllegalStateException("$collectionName 데이터를 조회할 수 없습니다.")
            }
            val documents = queryTask.result?.documents.orEmpty()
            if (documents.isEmpty()) {
                Tasks.forResult(null)
            } else {
                val batch = firestore.batch()
                documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
        }

    private fun commentData(
        authorUid: String,
        authorEmail: String?,
        body: String,
        isAnonymous: Boolean,
        anonymousNumber: Int?
    ): Map<String, Any?> =
        mapOf(
            "authorUid" to authorUid,
            "authorDisplayName" to authorEmail?.substringBefore("@").orEmpty(),
            "body" to body,
            "isAnonymous" to isAnonymous,
            "anonymousNumber" to anonymousNumber,
            "createdAt" to FieldValue.serverTimestamp()
        )

    companion object {
        private const val POSTS_COLLECTION = "posts"
        private const val COMMENTS_COLLECTION = "comments"
        private const val LIKES_COLLECTION = "likes"
        private const val SCRAPS_COLLECTION = "scraps"
        private const val READS_COLLECTION = "reads"
        private const val ANONYMOUS_AUTHORS_COLLECTION = "anonymousAuthors"

        val instance: BoardRepository by lazy { BoardRepository() }
    }
}
