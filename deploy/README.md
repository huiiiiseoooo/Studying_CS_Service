# GitHub Actions에서 EC2로 자동 배포

현재 서비스: Spring Boot / React·Nginx / MySQL, EC2 Ubuntu x86_64 한 대.

## 실행 흐름

1. 모든 push와 main 대상 PR에서 백엔드 테스트·빌드, 프론트엔드 lint·빌드, 배포 스크립트 테스트를 실행한다.
2. `main`이며 저장소 변수 `CD_ENABLED`가 `true`인 경우에만 CD를 실행한다.
3. CI가 만든 JAR와 dist를 같은 워크플로의 이미지 작업으로 전달한다. 결과물 보관 기간은 1일이다.
4. Docker Hub에 `oowgnus/cstudy-backend:sha-<commit>`과 `oowgnus/cstudy-frontend:sha-<commit>`을 올린다.
5. GitHub OIDC로 임시 AWS 자격 증명을 받아 지정된 EC2에 SSM Run Command를 보낸다.
6. EC2는 이미지의 digest로 정확한 버전을 받아 backend, frontend 순서로 교체한다.
7. EC2 내부에서 웹과 API 응답 및 컨테이너의 Git revision을 확인한다. GitHub에서도 공개 웹과 API 응답을 확인한다.

PR과 작업 브랜치는 배포하지 않는다. `main` 배포는 중간에 취소하지 않고 순서대로 진행한다.
EC2의 파일 잠금으로 서버 배포도 중복 실행을 막는다.

## 처음 한 번 준비할 것

### GitHub 저장소 설정

Settings → Secrets and variables → Actions에서 다음을 등록한다.

| 종류 | 이름 | 값 |
| --- | --- | --- |
| Secret | `DOCKERHUB_TOKEN` | Docker Hub 이미지 Read/Write 권한이 있는 Personal Access Token |
| Variable | `AWS_ROLE_ARN` | 생성한 `cstudy-github-deploy` IAM 역할 ARN |
| Variable | `EC2_INSTANCE_ID` | `i-00dc189aab9936eea` |
| Variable | `PUBLIC_ORIGIN` | 현재 사이트 주소. 예: `http://3.107.45.251` |
| Variable | `CD_ENABLED` | 서버와 권한 준비를 마친 뒤 `true` |

Docker Hub 로그인 계정은 기존 보관소 주인인 `oowgnus`로 지정되어 있다.
토큰이나 서버의 `.env`를 소스 코드, PR, Actions 일반 변수에 넣지 않는다.
설정 준비 중에는 `CD_ENABLED`를 등록하지 않거나 `false`로 둔다. 이 상태에서는 CI만 실행된다.

### EC2의 SSM 연결

- 기존 EC2와 무료 플랜을 유지한다. 새 EC2, RDS, NAT Gateway, VPC Endpoint는 필요하지 않다.
- SSM Agent가 설치·실행 중이어야 한다. Ubuntu AMI는 snap 서비스인 경우가 있다.
- 인스턴스 IAM 역할에 `AmazonSSMManagedInstanceCore` 정책이 필요하다.
- 신규 역할이라면 `aws/ec2-trust.json`을 신뢰 정책으로 사용하는 `cstudy-ec2-ssm` 역할과 인스턴스 프로파일을 만든다.
- 이미 인스턴스 IAM 역할이 있다면 그 용도를 확인하고 필요한 정책만 추가한다. 기존 역할을 임의로 교체하지 않는다.
- Systems Manager에서 해당 인스턴스가 Online으로 보이는지 확인한다.
- 서버의 `/home/ubuntu/cstudy/compose.yaml`, `.env`, 실행 중인 backend/frontend/MySQL을 그대로 사용한다.
- Python 3, Docker Engine, Docker Compose 플러그인이 필요하다.
- 기존 Compose 프로젝트 이름과 기본 네트워크는 `cstudy`, `cstudy_default`여야 한다.

### GitHub의 AWS 권한

AWS IAM에서 GitHub OIDC 공급자를 등록한다.

- Provider URL: `https://token.actions.githubusercontent.com`
- Audience: `sts.amazonaws.com`

`cstudy-github-deploy` 역할의 신뢰 정책은 `aws/github-trust.json`, 권한 정책은
`aws/github-deploy-policy.json`을 사용한다. 기존 역할이 있다면 내용을 먼저 검토한다.
이 정책은 이 저장소의 main 브랜치만 지정된 EC2 한 대에 명령을 보낼 수 있게 한다.
`ssm:GetCommandInvocation`은 리소스 단위 제한을 지원하지 않아 `Resource: "*"`를 사용한다.
SSM 명령은 서버에서 root 권한으로 실행되므로 저장소 main에 대한 쓰기 권한을 신뢰할 수 있어야 한다.

저장소가 2026-07-15 이후 생성되어 GitHub의 immutable subject 형식(소유자·저장소 ID 포함)을 사용한다.
저장소 이전이나 OIDC subject 형식 변경 시 신뢰 정책도 검토한다.
AWS 액세스 키와 SSH 개인키를 GitHub에 저장할 필요는 없다.

정책 내용을 검토·승인한 뒤, 이 저장소를 받은 AWS CloudShell에서 일회성 설정을 실행할 수도 있다.
기존 인스턴스 프로파일이나 같은 이름의 역할이 다른 용도로 사용 중이면 덮어쓰지 않고 중단한다.

```bash
python3 deploy/aws/configure.py          # 계정·인스턴스 확인만 수행
python3 deploy/aws/configure.py --apply  # 검토한 역할·정책 생성 및 EC2 연결
```

### 첫 실행

1. PR의 CI 검사가 통과하는지 확인한다.
2. AWS의 SSM 연결, GitHub의 토큰과 변수를 준비한다.
3. main에 변경을 합친다.
4. `CD_ENABLED=true`로 설정한다.
5. Actions → CI/CD → Run workflow에서 main을 선택하여 첫 배포를 실행한다.
6. `Build and publish images`, `Deploy EC2 and verify website`가 모두 성공하는지 확인한다.
7. 이후 main에 반영되는 변경은 CI 통과 후 자동 배포된다.

## 서버의 동작과 복구

`deploy.py`는 기존 `compose.yaml`과 `.env`를 수정하지 않는다.
이미지 선택만 담긴 `compose.override.yaml`을 배포가 성공한 뒤 원자적으로 저장한다.
이후 서버에서 `sudo docker compose ps` 같은 일반 명령도 현재 이미지 선택을 사용한다.
기존에 직접 만든 `compose.override.yaml`이 있다면 자동으로 덮어쓰지 않고 중단한다.

두 이미지를 모두 받은 후 backend를 교체한다. 백엔드 준비를 기다린 뒤 frontend를 재생성해
Nginx가 새 backend의 주소를 읽게 한다. MySQL 컨테이너와 볼륨은 교체하거나 삭제하지 않는다.
다운로드 실패는 현재 서비스를 유지한다. 앱 교체나 서버 내부 검사 실패는 이전 앱 이미지로 복구를 시도하고
워크플로를 실패 처리한다. 복구 실패도 성공으로 표시하지 않는다.

컨테이너 교체 중 짧은 접속 중단이 있을 수 있다. 외부 접속 검사만 실패했다면 서버 내부 배포는 이미 성공했을 수 있다.
자동 복구는 애플리케이션 이미지에 대한 것이며, Flyway가 적용한 DB 스키마를 되돌리지 않는다.
DB 변경은 이전 버전과 호환되도록 설계하고 중요한 변경 전에는 별도 백업을 준비한다.

서버 확인:

```bash
cd /home/ubuntu/cstudy
sudo docker compose ps
sudo docker compose logs --tail=100 backend frontend
curl --fail http://localhost/api/major-categories
```

자동 배포를 멈추려면 저장소 변수 `CD_ENABLED`를 `false`로 바꾼다. 이미 시작한 서버 명령은 별도로 확인한다.
서버가 참조하는 이미지를 정리하거나 DB 볼륨을 삭제하는 명령은 배포에 포함하지 않는다.

## 로컬 검증

```bash
python3 -m unittest discover -s deploy -p 'test_*.py' -v
actionlint .github/workflows/ci.yml
```

공식 문서:

- [GitHub Actions의 AWS OIDC](https://docs.github.com/en/actions/how-tos/secure-your-work/security-harden-deployments/oidc-in-aws)
- [EC2용 SSM 권한](https://docs.aws.amazon.com/systems-manager/latest/userguide/setup-instance-permissions.html)
- [EC2에서 추가 요금 없는 SSM Run Command](https://docs.aws.amazon.com/systems-manager/latest/userguide/run-command.html)
- [Docker Hub 토큰](https://docs.docker.com/security/access-tokens/personal-access-tokens/)
