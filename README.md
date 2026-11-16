# Cstudy

CS 기초를 애니메이션과 설명으로 공부하는 사이트. (요구사항명세서 v1.1 기준)

```
backend/    Spring Boot 3.5 · Java 17 · JPA · Flyway · Spring Security(JWT)
frontend/   React 19 · TypeScript · Vite · React Router
media/      애니메이션(GIF)·대체 이미지(PNG). 운영에서는 이 폴더 내용을 S3에 올린다.
tools/      애니메이션 생성 스크립트
```

## 로컬 실행

필요한 것: JDK 17+, Node 20+, Docker(로컬 MySQL용)

**1. DB** — MySQL 8 컨테이너를 띄운다. 테이블과 초기 데이터는 서버가 시작할 때 Flyway가 만든다.

```bash
docker compose up -d
```

**2. 백엔드** — `backend/.env` 가 없으면 `.env.example` 을 복사해 `JWT_SECRET`, `ADMIN_PASSWORD` 를 채운다.

```bash
cd backend && ./gradlew bootRun
```

**3. 프론트엔드**

```bash
cd frontend && npm install && npm run dev
```

- 사용자 화면: http://localhost:5173
- 운영자 로그인: http://localhost:5173/admin/login (계정은 `backend/.env` 의 `ADMIN_ID` / `ADMIN_PASSWORD`)

테스트: `cd backend && ./gradlew test` (메모리 DB 사용, MySQL 불필요)

## 환경 변수

| 위치 | 변수 | 설명 |
| --- | --- | --- |
| backend | `DB_URL` `DB_USERNAME` `DB_PASSWORD` | MySQL 접속 정보 |
| backend | `JWT_SECRET` | JWT 서명 키. 32바이트 이상, 저장소에 올리지 않는다 |
| backend | `JWT_EXPIRATION_MINUTES` | 토큰 만료(기본 60분) |
| backend | `FRONTEND_ORIGINS` | CORS 허용 주소(쉼표로 여러 개) |
| backend | `MEDIA_BASE_URL` | 영상·이미지 주소의 앞부분. 운영에서는 S3/CloudFront 주소 |
| backend | `MEDIA_SERVE_LOCAL` `MEDIA_LOCAL_DIR` | 로컬 전용. `true` 면 `media/` 폴더를 서버가 직접 내려준다 |
| backend | `ADMIN_ID` `ADMIN_PASSWORD` | 값이 있으면 시작 시 운영자 계정을 1회 생성(BCrypt 저장) |
| backend | `CACHE_TTL_SECONDS` | 조회 API 캐시 시간(기본 60초) |
| frontend | `VITE_API_BASE_URL` | 백엔드 주소. **빌드 시점**에 결과물에 들어간다 |

## API

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| GET | `/api/major-categories` | 대분류 목록 |
| GET | `/api/major-categories/{id}/medium-categories` | 중분류 목록 |
| GET | `/api/medium-categories/{id}/minor-categories` | 소분류 목록 |
| GET | `/api/minor-categories/{id}/content` | 학습 콘텐츠(설명 + 파일 URL) |
| POST | `/api/views` | 조회수 기록 `{ "mediumCategoryId": 7 }` |
| POST | `/api/auth/login` | 운영자 로그인 → JWT |
| GET | `/api/admin/statistics?start=&end=` | 기간별 중분류 조회수(운영자 전용) |

## 콘텐츠 추가 (코드 수정 없음)

1. 영상(MP4/GIF)과 대체 이미지를 `media/` (운영: S3)에 올린다.
2. DB에 행을 추가한다. `category`(분류) → `cs_content`(설명, 마크다운) → `video`(파일 경로).
3. '준비 중'인 대분류를 열려면 `UPDATE category SET active = TRUE WHERE id = ...`.

캐시 시간(기본 60초)이 지나면 화면에 반영된다. 형식은 `backend/src/main/resources/db/migration/V2__seed_categories_and_contents.sql` 참고.

## 배포할 때 알아 둘 점

- **프론트엔드**는 `npm run build` 결과물(`frontend/dist`)을 정적 호스팅한다. SPA이므로 없는 경로는 `index.html` 로 돌려보내도록 설정해야 새로고침이 동작한다.
- **백엔드**는 `./gradlew bootJar` 로 만든 `backend/build/libs/*.jar` 를 `java -jar` 로 실행하고, 위 환경 변수를 주입한다. `MEDIA_SERVE_LOCAL` 은 넣지 않는다.
- **HTTPS** 는 서버 앞단(로드밸런서·리버스 프록시)에서 적용한다. 프론트 주소가 정해지면 `FRONTEND_ORIGINS` 에 넣는다.
- **S3** 에는 `media/` 폴더 구조 그대로(`media/animations/...`, `media/fallback/...`) 올리면 DB 수정 없이 `MEDIA_BASE_URL` 만 바꿔서 전환된다.
