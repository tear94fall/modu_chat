# 모두메신저 백오피스 (admin/)

회원·채팅방 조회와 푸시 발송을 위한 관리자 전용 React 프런트엔드. Vite + React + TypeScript, `react-router-dom` 으로 라우팅한다.

## 실행

```bash
cd admin
npm install
npm run dev       # http://localhost:5173
```

게이트웨이 주소는 `.env` 의 `VITE_API_BASE_URL` 로 설정한다 (`.env.example` 참고).

```bash
cp .env.example .env
# .env
VITE_API_BASE_URL=http://localhost:8000
```

값을 생략하면 기본값 `http://localhost:8000` 을 사용한다. 게이트웨이(`backend/.env` 의 `ADMIN_ALLOWED_ORIGIN`)는 `http://localhost:5173` 을 허용하도록 이미 설정되어 있어야 한다.

## 관리자 계정 준비

### 1. 전용 관리자 행 삽입

로그인은 `member` 테이블에서 `role = 'ROLE_ADMIN'` 인 회원만 통과한다 (`backend/member-service/.../member/entity/Member.java`, `backend/auth-service/.../admin/AdminLoginService.java`). 관리자 계정은 **기존 회원을 승격하는 것이 아니라 전용 행을 새로 삽입**해서 만든다:

```sql
INSERT INTO member (email, user_id, username, role, created_date, updated_date)
VALUES ('admin@modu.local', 'admin', '관리자', 'ROLE_ADMIN', NOW(), NOW());
```

관리자 로그인 이메일은 비밀번호 없이 자격 증명의 절반을 담당하므로, 실제 사용자가 쓰는 계정을 승격해서 관리자 이메일로 쓰지 않는다. 또한 `ROLE_ADMIN` 으로 지정된 계정은 앱(모바일) 로그인 자체가 거부되므로, 이 계정으로 앱에 로그인할 수는 없다.

### 2. 관리자 비밀번호 해시 생성

관리자 로그인 비밀번호는 회원 비밀번호와 별개로, auth-service 설정(`modu.admin.password-hash`, 환경변수 `ADMIN_PASSWORD_HASH`)의 bcrypt 해시 하나로 검증한다. 해시를 생성한다:

```bash
htpasswd -bnBC 10 "" 'your-password' | tr -d ':\n'
```

결과를 `backend/.env` 의 `ADMIN_PASSWORD_HASH` 에 넣을 때, docker compose 가 `.env` 안의 `$` 를 변수 참조로 해석하므로 bcrypt 해시에 포함된 모든 `$` 를 `$$` 로 이스케이프해야 한다:

```bash
htpasswd -bnBC 10 "" 'your-password' | tr -d ':\n' | sed 's/\$/$$/g'
```

```
# backend/.env
ADMIN_PASSWORD_HASH=$$2y$$10$$........................................
```

로그인 이메일은 위에서 `ROLE_ADMIN` 으로 지정한 회원의 이메일을 사용한다.

## 기능

- **로그인** (`/login`): 이메일·비밀번호로 로그인, 액세스 토큰을 로컬 스토리지에 저장.
- **회원 관리** (`/members`): 키워드 검색, 페이지네이션, 행 클릭 시 상세 이동.
- **회원 상세** (`/members/:id`): 회원 정보 + 친구 수.
- **채팅방 관리** (`/rooms`): 채팅방 목록(이름/인원/최근 메시지/최근 시각), 페이지네이션, 행 클릭 시 상세 이동.
- **채팅방 상세** (`/rooms/:roomId`): 멤버 목록 + 최근 메시지 목록.
- **푸시 발송** (`/push`): 전체 발송(그룹 브로드캐스트) / 특정 사용자 발송 탭, 제목·본문·이미지 URL 입력.
- 인증되지 않은 접근은 `/login` 으로 리다이렉트되고, API 가 401 을 반환하면 토큰을 지우고 로그인 화면으로 이동한다.

## 테스트 / 빌드

```bash
npm test        # vitest run
npm run build   # tsc -b && vite build
```
