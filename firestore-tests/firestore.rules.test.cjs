const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const assert = require("node:assert/strict");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  increment,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} = require("firebase/firestore");

const PROJECT_ID = "demo-swuper-defense";
const RULES = fs.readFileSync(
  path.join(__dirname, "..", "firestore.rules"),
  "utf8",
);

let testEnv;

function userDb(uid) {
  return testEnv.authenticatedContext(uid, {
    email: `${uid}@swuperdepense.kr`,
  }).firestore();
}

function anonymousDb() {
  return testEnv.unauthenticatedContext().firestore();
}

async function seedUsers() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "users", "alice"), {
      email: "alice@swuperdepense.kr",
      displayName: "alice",
      isAdmin: false,
      createdAt: new Date(),
    });
    await setDoc(doc(db, "users", "bob"), {
      email: "bob@swuperdepense.kr",
      displayName: "bob",
      isAdmin: false,
      createdAt: new Date(),
    });
    await setDoc(doc(db, "users", "admin"), {
      email: "admin@swuperdepense.kr",
      displayName: "admin",
      isAdmin: true,
      createdAt: new Date(),
    });
  });
}

function postData(overrides = {}) {
  return {
    localId: 1001,
    authorDisplayName: "익명",
    isAnonymous: true,
    category: "피싱/스미싱",
    title: "의심스러운 문자",
    body: "링크를 누르지 마세요.",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    viewCount: 0,
    commentCount: 0,
    likeCount: 0,
    nextAnonymousNumber: 1,
    ...overrides,
  };
}

async function createPost(db, postId = "post-1", overrides = {}) {
  const batch = writeBatch(db);
  batch.set(doc(db, "posts", postId), postData(overrides));
  batch.set(doc(db, "postOwners", postId), {
    ownerUid: "alice",
    createdAt: serverTimestamp(),
  });
  await assertSucceeds(batch.commit());
}

test.before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: RULES,
    },
  });
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
  await seedUsers();
});

test.after(async () => {
  await testEnv.cleanup();
});

test("로그인하지 않은 사용자는 게시판을 읽을 수 없다", async () => {
  await assertFails(getDoc(doc(anonymousDb(), "posts", "post-1")));
});

test("게시글과 소유권 문서는 함께 생성되고 공개 문서에는 UID를 넣을 수 없다", async () => {
  const alice = userDb("alice");
  await createPost(alice);

  const publicPost = await assertSucceeds(getDoc(doc(alice, "posts", "post-1")));
  assert.equal(publicPost.data().authorUid, undefined);

  const invalidBatch = writeBatch(alice);
  invalidBatch.set(
    doc(alice, "posts", "post-with-uid"),
    postData({ authorUid: "alice" }),
  );
  invalidBatch.set(doc(alice, "postOwners", "post-with-uid"), {
    ownerUid: "alice",
    createdAt: serverTimestamp(),
  });
  await assertFails(invalidBatch.commit());

  await assertFails(setDoc(doc(alice, "posts", "post-without-owner"), postData()));
});

test("소유권 문서는 작성자와 관리자만 읽을 수 있다", async () => {
  const alice = userDb("alice");
  const bob = userDb("bob");
  const admin = userDb("admin");
  await createPost(alice);

  await assertSucceeds(getDoc(doc(alice, "postOwners", "post-1")));
  await assertFails(getDoc(doc(bob, "postOwners", "post-1")));
  await assertSucceeds(getDoc(doc(admin, "postOwners", "post-1")));

  const ownQuery = query(
    collection(alice, "postOwners"),
    where("ownerUid", "==", "alice"),
  );
  const result = await assertSucceeds(getDocs(ownQuery));
  assert.equal(result.size, 1);
});

test("다른 사용자는 게시글 내용을 수정할 수 없고 작성자는 허용된 필드만 수정한다", async () => {
  const alice = userDb("alice");
  const bob = userDb("bob");
  await createPost(alice);

  await assertFails(
    updateDoc(doc(bob, "posts", "post-1"), {
      title: "변조된 제목",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    updateDoc(doc(alice, "posts", "post-1"), {
      title: "수정된 제목",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(doc(alice, "posts", "post-1"), {
      likeCount: 999,
      updatedAt: serverTimestamp(),
    }),
  );
});

test("좋아요 문서는 본인만 조작하며 게시글 공감 수와 함께 변경한다", async () => {
  const alice = userDb("alice");
  const bob = userDb("bob");
  await createPost(alice);

  const likeBatch = writeBatch(bob);
  likeBatch.set(doc(bob, "posts", "post-1", "likes", "bob"), {
    createdAt: serverTimestamp(),
  });
  likeBatch.update(doc(bob, "posts", "post-1"), {
    likeCount: increment(1),
  });
  await assertSucceeds(likeBatch.commit());

  await assertFails(getDoc(doc(alice, "posts", "post-1", "likes", "bob")));
  await assertFails(
    setDoc(doc(bob, "posts", "post-1", "likes", "alice"), {
      createdAt: serverTimestamp(),
    }),
  );
});

test("익명 댓글 공개 문서에는 UID가 없고 댓글 소유권은 비공개다", async () => {
  const alice = userDb("alice");
  const bob = userDb("bob");
  await createPost(alice);

  const batch = writeBatch(bob);
  batch.set(doc(bob, "posts", "post-1", "anonymousAuthors", "bob"), {
    number: 1,
    createdAt: serverTimestamp(),
  });
  batch.set(doc(bob, "posts", "post-1", "comments", "comment-1"), {
    authorDisplayName: "익명",
    body: "익명 댓글입니다.",
    isAnonymous: true,
    anonymousNumber: 1,
    createdAt: serverTimestamp(),
  });
  batch.set(doc(bob, "posts", "post-1", "commentOwners", "comment-1"), {
    ownerUid: "bob",
    createdAt: serverTimestamp(),
  });
  batch.update(doc(bob, "posts", "post-1"), {
    commentCount: increment(1),
    nextAnonymousNumber: increment(1),
  });
  await assertSucceeds(batch.commit());

  const comment = await assertSucceeds(
    getDoc(doc(alice, "posts", "post-1", "comments", "comment-1")),
  );
  assert.equal(comment.data().authorUid, undefined);
  await assertFails(
    getDoc(doc(alice, "posts", "post-1", "commentOwners", "comment-1")),
  );
  await assertFails(
    getDoc(doc(alice, "posts", "post-1", "anonymousAuthors", "bob")),
  );
  await assertSucceeds(
    getDoc(doc(bob, "posts", "post-1", "commentOwners", "comment-1")),
  );
});

test("일반 사용자는 관리자 권한을 만들거나 변경할 수 없다", async () => {
  const alice = userDb("alice");
  await assertFails(
    updateDoc(doc(alice, "users", "alice"), {
      isAdmin: true,
    }),
  );
  await assertFails(
    setDoc(doc(alice, "users", "new-alice"), {
      email: "alice@swuperdepense.kr",
      displayName: "alice",
      isAdmin: true,
      createdAt: serverTimestamp(),
    }),
  );
});

test("기존 사용자 문서와 authorUid 형식 게시글은 계속 사용할 수 있다", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "posts", "legacy-post"), {
      ...postData({
        authorUid: "alice",
        authorDisplayName: "alice",
        isAnonymous: false,
      }),
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const alice = userDb("alice");
  const bob = userDb("bob");

  await assertSucceeds(getDoc(doc(alice, "users", "alice")));
  await assertSucceeds(getDoc(doc(alice, "posts", "legacy-post")));
  await assertSucceeds(
    updateDoc(doc(alice, "posts", "legacy-post"), {
      title: "기존 게시글 수정",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertFails(
    updateDoc(doc(bob, "posts", "legacy-post"), {
      title: "다른 사용자 수정",
      updatedAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(deleteDoc(doc(alice, "posts", "legacy-post")));
});

test("관리자는 타 사용자의 게시글과 소유권 문서를 함께 삭제할 수 있다", async () => {
  const alice = userDb("alice");
  const admin = userDb("admin");
  await createPost(alice);

  const batch = writeBatch(admin);
  batch.delete(doc(admin, "postOwners", "post-1"));
  batch.delete(doc(admin, "posts", "post-1"));
  await assertSucceeds(batch.commit());

  const deletedPost = await assertSucceeds(
    getDoc(doc(admin, "posts", "post-1")),
  );
  assert.equal(deletedPost.exists(), false);
});

test("스크랩과 읽음 상태는 해당 사용자만 읽고 쓸 수 있다", async () => {
  const alice = userDb("alice");
  const bob = userDb("bob");
  await createPost(alice);

  await assertSucceeds(
    setDoc(doc(bob, "posts", "post-1", "scraps", "bob"), {
      createdAt: serverTimestamp(),
    }),
  );
  await assertSucceeds(
    setDoc(doc(bob, "posts", "post-1", "reads", "bob"), {
      readAt: serverTimestamp(),
    }),
  );
  await assertFails(getDoc(doc(alice, "posts", "post-1", "scraps", "bob")));
  await assertFails(deleteDoc(doc(alice, "posts", "post-1", "reads", "bob")));
});
