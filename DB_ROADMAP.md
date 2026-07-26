# SWU퍼디펜스 DB 구현 로드맵

이 문서는 현재 메모리 기반 초안을 실제 저장 구조로 교체하기 위한 작업 순서입니다.
처음부터 모든 기능을 한 DB에 넣지 말고, 데이터의 공개 범위에 따라 저장 위치를 나눕니다.

## 0. 팀에서 확정한 범위

2026-07-26 기준으로 다음 두 가지는 확정됐습니다.

- 실제 전화번호 차단은 앱 범위에서 제외합니다. 전화·통화 권한과 차단 API를 추가하지 않습니다.
- 증거 원본 파일은 클라우드가 아닌 앱 전용 내부 저장소에 로컬 보관합니다.

따라서 증거 DB에는 파일 자체를 BLOB으로 넣지 않고, 앱 내부에 복사한 파일의 경로와
제목·메모·종류·저장 시각 같은 메타데이터만 저장합니다. 앱을 삭제하면 내부 저장소의
증거도 함께 삭제된다는 점은 사용자 안내에 포함해야 합니다.

아래 항목은 아직 팀 회의에서 결정해야 합니다.

- 게시판을 여러 사용자가 서로 다른 휴대폰에서 공유해야 하는가?
- 로그인 방식은 이메일/비밀번호인가, 학교 계정이나 소셜 로그인인가?
- 익명 댓글, 공감, 스크랩을 최종 기능에 포함할 것인가?

이 앱의 기능을 기준으로 한 권장 구성은 다음과 같습니다.

| 데이터 | 권장 저장 위치 | 이유 |
|---|---|---|
| 로그인 계정/세션 | Firebase Authentication 또는 별도 인증 서버 | 비밀번호를 앱 DB에서 직접 다루지 않기 위해 |
| 게시글/댓글/공감/스크랩/읽음 | Cloud Firestore 등 서버 DB | 여러 사용자가 같은 내용을 봐야 함 |
| 증거 메타데이터 | Room | 개인 기기 안에서 빠르고 안전하게 조회 |
| 증거 이미지·음성 | 앱 전용 내부 저장소 | 큰 파일을 DB BLOB으로 넣지 않기 위해 |
| 체크리스트 진행률 | Room | 개인별 로컬 상태 |
| 스미싱 검사 이력 | Room | 개인 입력 내용 보호 및 최근 활동 연결 |
| 최근 활동 | Room | 다른 로컬 데이터의 변경 이력 |

Room만 사용하면 다른 사용자가 작성한 게시글을 볼 수 없습니다. 반대로 모든 증거 파일을
클라우드에 올리면 개인정보와 비용 문제가 생기므로, 기본값은 로컬 보관이 안전합니다.

## 1. DB 담당자가 실제로 할 일

이 문서는 설계 방향만 적은 메모가 아니라 아래 순서대로 작업을 진행하기 위한 체크리스트입니다.
단, 한 번에 전부 구현하지 말고 각 단계가 빌드·테스트를 통과한 뒤 다음 단계로 넘어갑니다.

| 순서 | 작업 | 완료 기준 |
|---|---|---|
| 1 | Room/KSP와 `AppDatabase` 추가 | 빈 DB로 앱 실행 및 단위 테스트 통과 |
| 2 | 증거 Entity·DAO·Repository 추가 | 앱 재실행 후에도 메타데이터 유지 |
| 3 | 선택 파일을 앱 내부 저장소로 복사 | 원본 파일을 이동·삭제해도 앱에서 열림 |
| 4 | 증거 추가·조회·삭제 화면을 Repository에 연결 | 삭제 시 DB 행과 내부 파일이 함께 제거 |
| 5 | 체크리스트·최근 활동·스미싱 이력 이전 | 앱 재실행 후에도 각 기록 유지 |
| 6 | 인증과 게시판 원격 DB 연결 | 서로 다른 계정·기기에서 권한과 데이터 공유 확인 |

첫 작업 범위는 **1~4번(증거 보관함)**까지만 잡는 것을 권장합니다. 이 단계가 끝나기 전에
게시판 서버 DB까지 동시에 시작하면 오류 원인을 찾기 어려워집니다.

## 2. 현재 임시 저장소 확인

현재 교체 대상은 다음과 같습니다.

| 기능 | 현재 위치 | 교체 대상 |
|---|---|---|
| 게시글/댓글 | `BoardFragment` companion object | `BoardRepository` |
| 공감/스크랩 | `Post` 객체의 임시 상태 | 사용자별 원격 컬렉션 |
| 증거 | `EvidenceFragment` companion object | `EvidenceRepository` + Room |
| 스미싱 검사 | `SmishingAnalyzer.checkHistory` | `SmishingRepository` + Room |
| 최근 활동 | `ActivityLog.entries` | `ActivityLogRepository` + Room |
| 체크리스트 | `ChecklistProgressStore` SharedPreferences | `ChecklistRepository` + Room |
| 로그인 상태 | `AppSession` SharedPreferences | 인증 SDK/서버 토큰 |

`AppSession`에는 비밀번호가 저장되지 않습니다. 실제 인증을 연결한 후에도 비밀번호 평문,
복호화 가능한 비밀번호, 비밀번호 로그를 Room이나 SharedPreferences에 저장하면 안 됩니다.

## 3. 패키지 구조부터 분리

DB 코드를 Fragment 안에 직접 추가하지 않습니다. 먼저 아래 구조를 만듭니다.

```text
com.adroid.guru2_swuperdefense
├── data
│   ├── local
│   │   ├── AppDatabase.kt
│   │   ├── dao
│   │   └── entity
│   ├── remote
│   │   └── dto
│   └── repository
├── domain
│   ├── model
│   └── repository
└── ui
    ├── board
    ├── evidence
    ├── diagnosis
    └── account
```

초기에는 모든 UI 파일을 한꺼번에 이동하지 않아도 됩니다. `Entity`, `DAO`,
`Repository`부터 분리하고, 기능 하나씩 Fragment가 Repository를 사용하도록 바꿉니다.

## 4. Room 설정

공식 Room 문서: <https://developer.android.com/training/data-storage/room>

1. `gradle/libs.versions.toml`에 Room과 KSP 버전을 추가합니다.
2. 루트와 앱 Gradle에 KSP 플러그인을 추가합니다.
3. `room-runtime`, `room-ktx`, `room-compiler`, `room-testing`을 추가합니다.
4. `AppDatabase`는 앱 전체에서 하나의 인스턴스만 사용합니다.
5. DAO 함수는 `suspend`, 조회 목록은 `Flow<List<...>>`를 우선 사용합니다.
6. 메인 스레드 DB 접근을 허용하는 `allowMainThreadQueries()`는 사용하지 않습니다.

버전은 추가하는 날 공식 문서에서 다시 확인하고, 팀원 모두가 같은 Version Catalog를
사용하도록 반드시 Git에 포함합니다.

## 5. 로컬 Entity 설계

### EvidenceEntity

```text
id: String(UUID)          PK
ownerUid: String
title: String
memo: String?
mediaType: TEXT | IMAGE | AUDIO | FILE
riskLevel: DANGER | CAUTION | SAFE
localUri: String?
mimeType: String?
originalFileName: String?
createdAt: Long
```

- 화면용 아이콘, 글자색, 배경 리소스 ID는 DB에 저장하지 않습니다.
- `mediaType`과 `riskLevel`로 UI에서 아이콘과 색을 결정합니다.
- 날짜 표시 문자열 대신 `createdAt` epoch milliseconds를 저장합니다.
- 파일 원본은 내부 저장소에 복사하고 DB에는 위치와 메타데이터만 저장합니다.
- 삭제 시 DB 행과 내부 파일을 함께 삭제합니다.

### SmishingCheckEntity

```text
id: String(UUID)          PK
ownerUid: String
message: String
sender: String?
score: Int
riskLevel: LOW | CAUTION | HIGH
createdAt: Long
```

입력 문구에는 개인정보가 있을 수 있으므로 로그에 원문을 출력하지 않습니다.

### ChecklistProgressEntity

```text
ownerUid: String          복합 PK
incidentType: String      복합 PK
stepIndex: Int            복합 PK
completed: Boolean
updatedAt: Long
```

### ActivityLogEntity

```text
id: String(UUID)          PK
ownerUid: String
type: EVIDENCE | BOARD_POST | SMISHING_CHECK | DIAGNOSIS
referenceId: String?
title: String
description: String
createdAt: Long
```

원본 데이터가 삭제됐을 때 최근 활동을 눌러도 크래시가 나지 않도록, Repository에서
`referenceId`가 유효한지 확인하고 없으면 안내 메시지를 표시합니다.

## 6. 게시판 서버 데이터 설계

Firebase를 선택한다면 공식 문서를 먼저 읽습니다.

- Authentication: <https://firebase.google.com/docs/auth/android/start>
- Firestore 데이터 모델: <https://firebase.google.com/docs/firestore/data-model>

권장 컬렉션 예시:

```text
users/{uid}
posts/{postId}
posts/{postId}/comments/{commentId}
posts/{postId}/likes/{uid}
posts/{postId}/scraps/{uid}
posts/{postId}/reads/{uid}
```

`posts/{postId}` 주요 필드:

```text
authorUid, authorDisplayName, category, title, body,
createdAt, updatedAt, viewCount, commentCount
```

주의사항:

- `isMine`은 저장하지 않고 `post.authorUid == currentUser.uid`로 계산합니다.
- `isNew`는 게시글 공통 필드가 아닙니다. `reads/{uid}` 존재 여부로 사용자마다 계산합니다.
- 공감과 스크랩은 사용자 UID를 문서 ID로 사용하면 중복 등록을 막기 쉽습니다.
- 조회수 증가는 transaction 또는 atomic increment를 사용합니다.
- 게시글 삭제 시 댓글 subcollection이 자동 삭제되지 않는 제품도 있으므로 삭제 정책을 만듭니다.
- 보안 규칙에서 작성자만 수정·삭제할 수 있도록 검사합니다. UI에서 버튼을 숨기는 것만으로는
  보안이 되지 않습니다.

익명 댓글을 채택한다면 `(postId, uid)`에 대해 한 번 발급한 익명 번호를 별도 저장합니다.
같은 사용자가 같은 게시글에서 여러 댓글을 작성해도 같은 번호가 나오게 해야 합니다.

## 7. Repository 인터페이스

UI가 Room/Firebase 구현을 직접 알지 못하도록 인터페이스를 먼저 정의합니다.

```kotlin
interface EvidenceRepository {
    fun observeAll(): Flow<List<Evidence>>
    suspend fun getById(id: String): Evidence?
    suspend fun add(draft: EvidenceDraft): String
    suspend fun delete(id: String)
}
```

동일한 방식으로 다음을 만듭니다.

- `BoardRepository`
- `EvidenceRepository`
- `SmishingRepository`
- `ChecklistRepository`
- `ActivityLogRepository`
- `AuthRepository`

Fragment에서는 companion object 대신 ViewModel을 호출하고, ViewModel만 Repository를
사용하게 합니다.

## 8. 기능별 교체 순서

한 번에 전부 바꾸지 말고 다음 순서로 진행합니다.

### 1차: 증거 보관함

- [ ] `EvidenceEntity`, `EvidenceDao`, `EvidenceRepository` 작성
- [ ] 샘플 데이터를 debug 빌드 전용 seed로 이동
- [ ] 추가/목록/상세/삭제를 Repository로 교체
- [ ] 이미지·음성을 앱 내부 저장소로 복사
- [ ] 삭제 및 존재하지 않는 파일 처리 테스트

### 2차: 체크리스트와 최근 활동

- [ ] `ChecklistProgressEntity`, DAO 작성
- [ ] SharedPreferences 값을 Room으로 한 번만 이전
- [ ] `ActivityLogEntity`, DAO 작성
- [ ] 홈의 최근 3개와 전체보기를 Flow로 관찰

### 3차: 스미싱 검사 이력

- [ ] 검사 결과와 원문 저장
- [ ] 이력 삭제 기능 및 보관 기간 결정
- [ ] 로그와 크래시 리포트에 메시지 원문이 포함되지 않는지 확인

### 4차: 인증

- [ ] 인증 프로젝트 및 개발용 계정 설정
- [ ] 회원가입/로그인/로그아웃/비밀번호 재설정 연결
- [ ] Splash에서 실제 로그인 상태 확인
- [ ] 회원탈퇴 시 원격 데이터 삭제 순서 정의
- [ ] `AppSession`의 임시 로그인 통과 로직 제거

### 5차: 게시판

- [ ] 게시글 CRUD
- [ ] 댓글 CRUD
- [ ] 공감/스크랩
- [ ] 사용자별 읽음 처리
- [ ] 카테고리와 제목+본문 검색
- [ ] 보안 규칙 테스트

## 9. 단계별 Git 작업 원칙

- DB 작업 전 새 기능 브랜치를 만들고, 단계별로 작은 커밋을 만듭니다.
- `gradle/libs.versions.toml`, 루트 Gradle, 앱 Gradle은 같은 커밋에 포함합니다.
- Entity 변경 시 DB 버전과 Migration을 함께 변경합니다.
- 샘플 데이터는 release 빌드에 자동으로 들어가지 않게 분리합니다.
- 각 단계마다 `testDebugUnitTest`, `lintDebug`, `assembleDebug`를 실행합니다.
- 완성 전 팀 브랜치에 직접 push하지 말고 Pull Request에서 변경 파일을 검토합니다.

## 10. 마이그레이션과 테스트

- DB 스키마 버전을 올릴 때 `fallbackToDestructiveMigration()`으로 사용자 자료를 지우지 않습니다.
- 모든 Migration을 테스트합니다.
- DAO별 insert/read/update/delete 테스트를 작성합니다.
- 중복 공감, 다른 사용자 글 삭제, 탈퇴 사용자 데이터, 파일 누락 상황을 테스트합니다.
- 디버그 샘플 데이터와 실제 사용자 데이터를 구분합니다.
- 릴리스 전에 예시 게시글과 예시 증거를 제거하거나 debug 빌드에서만 생성합니다.

## 11. 완료 기준

DB 작업 완료는 단순히 앱을 재실행해도 데이터가 남는 것만 의미하지 않습니다.

- 앱 강제 종료·재부팅 후 로컬 데이터가 유지된다.
- 다른 계정의 비공개 증거에 접근할 수 없다.
- 다른 사용자의 글을 수정·삭제할 수 없다.
- 공감·스크랩이 중복되지 않는다.
- 게시글을 읽은 사용자에게만 N 표시가 사라진다.
- 증거 파일 삭제 시 DB와 실제 파일이 함께 정리된다.
- DB 버전 업그레이드 후 기존 데이터가 유지된다.
- 비밀번호와 인증 토큰이 로그에 노출되지 않는다.

팀원에게 완료를 전달할 때는 아래 증빙도 같이 남깁니다.

- Entity/DAO/Repository 파일 목록
- DB 스키마 버전과 Migration 테스트 결과
- 앱 강제 종료·재실행 후 데이터가 남는 화면 녹화 또는 스크린샷
- 파일 원본을 이동한 뒤에도 증거가 열리는지 확인한 결과
- 증거 삭제 후 DB 행과 내부 파일이 모두 사라지는 테스트 결과
- 실행한 Gradle 검증 명령과 성공 결과
