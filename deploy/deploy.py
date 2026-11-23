#!/usr/bin/env python3
"""SSM에서 실행하는 EC2 배포. 기존 Compose와 DB 볼륨은 보존한다."""

import argparse
import fcntl
import json
import re
import subprocess
import tempfile
import time
import urllib.request
from pathlib import Path


def validate_images(backend, frontend, revision):
    for service, image in (("backend", backend), ("frontend", frontend)):
        pattern = rf"oowgnus/cstudy-{service}@sha256:[0-9a-f]{{64}}"
        if not re.fullmatch(pattern, image):
            raise ValueError(f"Invalid {service} image digest")
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("Invalid Git revision")


class Deployment:
    def __init__(self, app_dir, attempts=36, delay=5):
        self.app_dir = Path(app_dir)
        self.attempts = attempts
        self.delay = delay
        self.base = ["docker", "compose", "--project-name", "cstudy",
                     "-f", str(self.app_dir / "compose.yaml")]

    def run(self, *args):
        return subprocess.run(args, cwd=self.app_dir, check=True,
                              text=True, stdout=subprocess.PIPE, timeout=120).stdout.strip()

    def compose(self, override, *args):
        return self.run(*self.base, "-f", str(override), *args)

    def container(self, service):
        container_id = self.run(*self.base, "ps", "-q", service)
        if not container_id:
            raise RuntimeError(f"No running {service} container")
        return json.loads(self.run("docker", "inspect", container_id))[0]

    def write_override(self, path, images):
        path.write_text(json.dumps({
            "x-cstudy-cd": True,
            "services": {name: {"image": image} for name, image in images.items()},
        }, indent=2) + "\n")

    def probe(self, url, api=False):
        with urllib.request.urlopen(url, timeout=5) as response:
            body = response.read()
            if response.status != 200:
                raise RuntimeError("Unexpected HTTP status")
            if api:
                data = json.loads(body)
                if not isinstance(data, dict) or not isinstance(data.get("items"), list):
                    raise RuntimeError("Unexpected API response")

    def wait(self, check):
        last_error = None
        for attempt in range(self.attempts):
            try:
                check()
                return
            except (OSError, ValueError, RuntimeError,
                    subprocess.CalledProcessError) as error:
                last_error = error
                if attempt + 1 < self.attempts:
                    time.sleep(self.delay)
        raise RuntimeError("Application did not become healthy") from last_error

    def backend_check(self):
        container = self.container("backend")
        address = container["NetworkSettings"]["Networks"]["cstudy_default"]["IPAddress"]
        if not address:
            raise RuntimeError("Backend has no network address")
        self.probe(f"http://{address}:8080/api/major-categories", api=True)

    def site_check(self):
        self.probe("http://127.0.0.1/")
        self.probe("http://127.0.0.1/api/major-categories", api=True)

    def deploy(self, backend, frontend, revision):
        validate_images(backend, frontend, revision)
        if not (self.app_dir / "compose.yaml").is_file():
            raise RuntimeError("Existing compose.yaml is required")
        if not (self.app_dir / ".env").is_file():
            raise RuntimeError("Existing server .env is required")
        current = self.app_dir / "compose.override.yaml"
        if current.exists():
            try:
                owned = json.loads(current.read_text()).get("x-cstudy-cd") is True
            except ValueError:
                owned = False
            if not owned:
                raise RuntimeError("An unmanaged compose.override.yaml already exists")

        # flock는 종료 시 자동 해제된다. 배포 중인 서버 작업을 중복 실행하지 않는다.
        with (self.app_dir / ".deploy.lock").open("a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            previous = {name: self.container(name)["Config"]["Image"]
                        for name in ("backend", "frontend")}
            images = {"backend": backend, "frontend": frontend}
            with tempfile.TemporaryDirectory(prefix=".deploy-", dir=self.app_dir) as temp:
                candidate = Path(temp) / "candidate.json"
                rollback = Path(temp) / "rollback.json"
                self.write_override(candidate, images)
                self.write_override(rollback, previous)
                # 다운로드 실패 시 현재 실행 중인 컨테이너는 건드리지 않는다.
                print("Pulling both application images", flush=True)
                self.compose(candidate, "pull", "backend", "frontend")
                try:
                    print("Starting backend", flush=True)
                    self.compose(candidate, "up", "-d", "--no-deps", "--pull", "never",
                                 "--force-recreate", "backend")
                    self.wait(self.backend_check)
                    # 새 backend 주소를 Nginx가 다시 해석하도록 frontend를 재생성한다.
                    print("Starting frontend", flush=True)
                    self.compose(candidate, "up", "-d", "--no-deps", "--pull", "never",
                                 "--force-recreate", "frontend")
                    self.wait(self.site_check)
                    for name, image in images.items():
                        container = self.container(name)
                        if (not container["State"]["Running"]
                                or container["Config"]["Image"] != image
                                or container["Config"].get("Labels", {}).get(
                                    "org.opencontainers.image.revision") != revision):
                            raise RuntimeError(f"Unexpected deployed {name} version")
                    # 성공한 이미지 선택을 저장한다. 이후 일반 docker compose도 이를 사용한다.
                    candidate.replace(current)
                except Exception as deploy_error:
                    print("Deployment failed; restoring previous application images", flush=True)
                    try:
                        self.compose(rollback, "up", "-d", "--no-deps", "--pull", "never",
                                     "--force-recreate", "backend")
                        self.wait(self.backend_check)
                        self.compose(rollback, "up", "-d", "--no-deps", "--pull", "never",
                                     "--force-recreate", "frontend")
                        self.wait(self.site_check)
                    except Exception as rollback_error:
                        raise RuntimeError("Deployment and application rollback failed; "
                                           "inspect the server") from rollback_error
                    raise RuntimeError("Deployment failed; previous application restored") from deploy_error
            print(f"Deployed revision {revision}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--app-dir", default="/home/ubuntu/cstudy")
    parser.add_argument("--backend-image", required=True)
    parser.add_argument("--frontend-image", required=True)
    parser.add_argument("--revision", required=True)
    args = parser.parse_args()
    Deployment(args.app_dir).deploy(args.backend_image, args.frontend_image, args.revision)


if __name__ == "__main__":
    main()
