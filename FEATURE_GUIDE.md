# SWU퍼디펜스 — 기능 구조 가이드 (게시판 ~ 최근활동)

이 문서는 **게시판 기능 구현부터 추가된 모든 화면/로직**의 구조를 정리한 것입니다.
다른 AI 모델이나 팀원이 코드를 처음 볼 때 빠르게 전체 그림을 파악할 수 있도록 작성했습니다.

패키지: `com.adroid.guru2_swuperdefense`
DB/백엔드는 아직 연동 전이며, 모든 데이터는 **앱이 켜져 있는 동안만 유지되는 메모리 저장소**(Kotlin `object`의 `companion object` 또는 최상위 `object`)에 있습니다. 백엔드 담당자가 채워야 할 자리는 전부 `// TODO: 백엔드 연동 지점` 주석으로 표시되어 있습니다.

---

## 1. 공통 아키텍처 패턴

이 앱은 **MainActivity 1개 + Fragment 여러 개** 구조입니다 (Single Activity 패턴).
`MainActivity`가 하단 탭(홈/게시판/증거/마이페이지)에 따라 `FragmentManager.replace()`로 화면을 갈아끼웁니다.

**왜 데이터를 companion object에 두는가?**
`replace()` + `addToBackStack()`으로 화면을 이동하면, 이전 Fragment는 뒤로 가기 전까지 **View가 파괴**됩니다. 즉 Fragment 인스턴스의 일반 프로퍼티(`private val`)에 데이터를 두면 화면을 벗어나는 순간 사라집니다.
그래서 "여러 화면에서 공유되어야 하고, 화면 전환 후에도 남아있어야 하는 데이터"는 각 Fragment 클래스의 **companion object**(정적 저장소, 앱 프로세스가 살아있는 동안 유지됨)에 둡니다.

```
BoardFragment.Post          → BoardFragment 클래스 안, companion object의 mutableList
EvidenceFragment.Evidence   → EvidenceFragment 클래스 안, companion object의 mutableList
SmishingAnalyzer.CheckRecord→ SmishingAnalyzer object 안의 mutableList
ActivityLog.Entry           → ActivityLog object 안의 mutableList (앱 전역 공용)
```

같은 패턴이 반복되므로, 한 곳(예: `BoardFragment`)의 구조를 이해하면 나머지도 동일하게 읽을 수 있습니다.

---

## 2. 기능별 파일 구조

### 📋 게시판 (Board)

| 파일 | 역할 |
|---|---|
| `BoardFragment.kt` | 게시글 목록, 검색, 카테고리 필터. `Post`/`Comment` 데이터 모델과 `posts` 저장소(companion object)를 소유 |
| `WritePostFragment.kt` | 글쓰기 **및** 수정. `editPostId`가 있으면 기존 글 값을 채워서 "수정 모드"로 동작 |
| `PostDetailFragment.kt` | 게시글 상세: 본문, 공감/스크랩, 댓글 목록/작성, 본인 글이면 수정·삭제 링크 |

**데이터가 실제로 사는 곳**: `BoardFragment.posts` (companion object, `private val posts = mutableListOf<Post>(...)`)
다른 파일은 전부 `BoardFragment.addPost()`, `BoardFragment.getPostById()`, `BoardFragment.updatePost()`, `BoardFragment.deletePost()`를 통해서만 접근합니다 (직접 리스트를 만지지 않음).

**화면 흐름**
```
BoardFragment ──(+ 버튼)──▶ WritePostFragment (새 글)
BoardFragment ──(글 클릭)──▶ PostDetailFragment
                                  ├─(수정, 본인 글일 때만)─▶ WritePostFragment (수정 모드)
                                  └─(삭제, 본인 글일 때만)─▶ 확인 다이얼로그 → BoardFragment로 복귀
```

**본인 글 판별**: `Post.isMine` 필드. 지금은 로그인 연동 전이라 "이 앱에서 직접 작성한 글"만 `true`. 실제로는 `currentUserId == post.authorId` 비교로 교체해야 함 (TODO 표시됨).

---

### 📁 증거 보관함 (Evidence)

| 파일 | 역할 |
|---|---|
| `EvidenceFragment.kt` | 증거 목록, 타입별(이미지/메모/파일) 필터. `Evidence` 데이터 모델과 `evidenceList` 저장소 소유 |
| `AddEvidenceFragment.kt` | 증거 추가. "글로 작성"(메모) / "파일 첨부"(이미지·음성, **여러 개 동시 선택 가능**) 두 모드 |
| `EvidenceDetailFragment.kt` | 상세보기. 타입별로 다른 콘텐츠 표시(메모 전문 / 이미지 미리보기 / 음성 재생), 삭제 |

**여러 장/여러 파일 선택 처리**: `AddEvidenceFragment`에서 이미지·음성 각각 여러 개를 고르면, **선택한 개수만큼 각각 별도의 Evidence 항목**으로 저장됩니다 (제목에 "(1)", "(2)"... 자동 부여).

**화면 흐름**
```
HomeFragment "증거 정리" 카드 ──▶ AddEvidenceFragment ──(저장)──▶ EvidenceFragment (증거 탭으로 이동)
EvidenceFragment ──(+ 버튼)──▶ AddEvidenceFragment (위와 동일)
EvidenceFragment ──(항목 클릭)──▶ EvidenceDetailFragment ──(삭제, 확인 다이얼로그)──▶ EvidenceFragment
```

---

### 🔍 스미싱 문구 점검

| 파일 | 역할 |
|---|---|
| `SmishingAnalyzer.kt` | 판별 로직(키워드 기반 점수 계산) + 검사 이력 저장소(`CheckRecord`) |
| `SmishingCheckFragment.kt` | 문구/발신번호 입력 화면 |
| `SmishingResultFragment.kt` | 결과 화면. **`checkId`를 받아서** 이력에서 조회 후 표시 (문구를 직접 넘기지 않음) |

**왜 메시지를 직접 안 넘기고 `checkId`로 넘기나?**
"최근 활동"에서 예전 검사 결과를 다시 열어볼 수 있어야 하기 때문입니다. `SmishingCheckFragment`가 분석 시점에 `SmishingAnalyzer.saveCheck(message, sender)`로 이력에 저장하고 id를 받아, 그 id로 결과 화면을 엽니다. 결과 화면은 항상 이력에서 값을 조회해서 그립니다.

---

### 👤 마이페이지

| 파일 | 역할 |
|---|---|
| `MyPageFragment.kt` | 메뉴 목록: 계정 / 설정(스텁) / 버전 / 로그아웃 |
| `AccountFragment.kt` | 아이디 표시, 비밀번호 마스킹, 비밀번호 수정·회원탈퇴 진입점 |
| `ChangePasswordFragment.kt` | 현재/새/새 비밀번호 확인 3단계 폼 |

로그인 시 저장된 아이디는 `SharedPreferences("login")`에서 읽어옵니다 (Login/Signup 화면이 쓰는 것과 같은 저장소).

---

### 🕓 최근 활동 (ActivityLog) — 앱 전역 공용

| 파일 | 역할 |
|---|---|
| `ActivityLog.kt` | **Fragment가 아닌 순수 데이터/유틸 object.** 활동 기록 저장, 카드 UI 빌더(`buildCard`), 클릭 시 이동 로직(`navigateTo`)까지 한 곳에 모아둠 |
| `ActivityLogListFragment.kt` | "전체 보기" 화면. `ActivityLog.all()`을 그대로 나열 |

**기록이 쌓이는 지점** (각 기능의 저장 함수 안에서 `ActivityLog.log(...)` 호출):
- `BoardFragment.addPost()` → 게시글 작성 기록
- `EvidenceFragment.addEvidence()` → 증거 저장 기록
- `SmishingCheckFragment`의 분석 버튼 클릭 → 스미싱 점검 기록

**홈 화면**은 `ActivityLog.recent(3)`만, **전체보기**는 `ActivityLog.all()`을 사용 — 같은 `buildCard()`/`navigateTo()`를 공유하므로 두 화면의 카드 모양과 클릭 동작이 항상 동일합니다.

**항목 클릭 시 이동 규칙** (`ActivityLog.navigateTo` 안의 `when`):
```kotlin
Type.EVIDENCE       → EvidenceDetailFragment.newInstance(refId)
Type.BOARD_POST     → PostDetailFragment.newInstance(refId)
Type.SMISHING_CHECK → SmishingResultFragment.newInstance(refId)
Type.NONE           → 이동 없이 "샘플 데이터입니다" 토스트만 (데모용 하드코딩 3개 샘플 전용)
```

---

## 3. 데이터 저장 위치 총정리

| 데이터 | 저장 위치 | 비고 |
|---|---|---|
| 게시글/댓글 | `BoardFragment` companion object | `posts: MutableList<Post>` |
| 증거 | `EvidenceFragment` companion object | `evidenceList: MutableList<Evidence>` |
| 스미싱 검사 이력 | `SmishingAnalyzer` object | `checkHistory: MutableList<CheckRecord>` |
| 최근 활동 로그 | `ActivityLog` object | `entries: MutableList<Entry>` |
| 로그인 아이디(저장 체크 시) | `SharedPreferences("login")` | 비밀번호는 저장 안 함 |

전부 **앱을 완전히 종료하면 초기화**됩니다 (DB 아님). 백엔드 담당자가 각 저장소를 Room DB 등으로 교체하면 되고, 위 표의 "데이터"를 조회/저장하는 함수(`addPost`, `addEvidence`, `saveCheck`, `log` 등) 내부만 바꾸면 되도록 이미 접근 지점이 함수로 캡슐화되어 있습니다.

---

## 4. 백엔드 연동 필요 지점 (TODO 체크리스트)

| 파일 | 내용 |
|---|---|
| `LoginActivity.kt` | 아이디/비번 실제 DB 검증, 비밀번호 찾기 |
| `SignupActivity.kt` | 회원가입 실제 DB 저장 |
| `BoardFragment.kt` | 게시글 CRUD를 DB로, 본인 글 판별을 실제 로그인 사용자 기준으로 |
| `WritePostFragment.kt` | 글쓰기/수정 DB 반영 |
| `PostDetailFragment.kt` | 댓글 DB 저장, 공감/스크랩 상태 영구 저장 |
| `EvidenceFragment.kt` / `AddEvidenceFragment.kt` | 증거 DB 저장. 첨부파일은 URI만 저장 중이라 **앱 재실행 시 접근 권한이 사라질 수 있음** — 내부 저장소로 파일 복사 또는 `OpenDocument()` + `takePersistableUriPermission()` 필요 |
| `EvidenceDetailFragment.kt` | 삭제를 DB 반영으로 |
| `SmishingAnalyzer.kt` | 판별 로직을 서버 API/공공데이터(KISA 스미싱 URL 목록 등)로 고도화, 검사 이력 DB 저장 |
| `SmishingResultFragment.kt` | 발신번호 실제 차단 처리 |
| `AccountFragment.kt` / `MyPageFragment.kt` | 실제 로그인 세션 아이디 조회, 회원탈퇴 DB 반영, 로그아웃 세션 종료 |
| `ChangePasswordFragment.kt` | 현재 비밀번호 검증 + 새 비밀번호 DB 반영 |
| `ActivityLog.kt` | 활동 로그 DB 저장 (`ActivityLogDao`) |

---

## 5. 화면 ↔ 파일 빠른 참조표

| 화면 | Fragment | 레이아웃 |
|---|---|---|
| 게시판 목록 | `BoardFragment` | `fragment_board.xml` |
| 글쓰기/수정 | `WritePostFragment` | `fragment_write_post.xml` |
| 게시글 상세 | `PostDetailFragment` | `fragment_post_detail.xml` |
| 증거 목록 | `EvidenceFragment` | `fragment_evidence.xml` |
| 증거 추가 | `AddEvidenceFragment` | `fragment_add_evidence.xml` |
| 증거 상세 | `EvidenceDetailFragment` | `fragment_evidence_detail.xml` |
| 스미싱 점검 입력 | `SmishingCheckFragment` | `fragment_smishing_check.xml` |
| 스미싱 점검 결과 | `SmishingResultFragment` | `fragment_smishing_result.xml` |
| 마이페이지 | `MyPageFragment` | `fragment_mypage.xml` |
| 계정 | `AccountFragment` | `fragment_account.xml` |
| 비밀번호 수정 | `ChangePasswordFragment` | `fragment_change_password.xml` |
| 활동 전체보기 | `ActivityLogListFragment` | `fragment_activity_log_list.xml` |

---

## 6. 코드에서 참고할 표시 규칙

- `// TODO: 백엔드 연동 지점 ...` → 백엔드/DB 담당자가 채워야 하는 자리
- `// ==== 수정 시작 ==== / ==== 수정 끝 ====` → 기존 깃 클론 코드(팀원 원본 파일)를 나중에 수정한 부분 표시. 신규 생성 파일에는 없음 (파일 전체가 새로 만든 것이므로)
