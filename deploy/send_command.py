#!/usr/bin/env python3
"""GitHub에서 SSM 배포를 시작하고 서버의 완료 상태까지 기다린다."""

import argparse
import base64
import json
import re
import shlex
import subprocess
import time
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import urlopen

from deploy import validate_images


def aws(*args):
    result = subprocess.run(["aws", "ssm", *args, "--output", "json"],
                            check=True, text=True, capture_output=True)
    return json.loads(result.stdout)


def parameters(backend, frontend, revision):
    validate_images(backend, frontend, revision)
    encoded = base64.b64encode(Path(__file__).with_name("deploy.py").read_bytes()).decode()
    arguments = shlex.join(["--backend-image", backend, "--frontend-image", frontend,
                            "--revision", revision])
    return {"commands": [
        "set -eu",
        "umask 077",
        'TASK_DIR=$(mktemp -d)',
        'trap \'rm -rf "$TASK_DIR"\' EXIT',
        f'printf \'%s\' {shlex.quote(encoded)} | base64 --decode > "$TASK_DIR/deploy.py"',
        f'python3 "$TASK_DIR/deploy.py" {arguments}',
    ], "executionTimeout": ["900"]}


def validate_origin(origin):
    parsed = urlparse(origin)
    if (parsed.scheme not in ("http", "https") or not parsed.hostname
            or parsed.username or parsed.password or parsed.path not in ("", "/")
            or parsed.query or parsed.fragment):
        raise ValueError("PUBLIC_ORIGIN must be an HTTP(S) origin without credentials")


def check_public_site(origin):
    validate_origin(origin)
    for path in ("/", "/api/major-categories"):
        with urlopen(origin.rstrip("/") + path, timeout=15) as response:
            body = response.read()
            if response.status != 200:
                raise RuntimeError("Public site returned an unexpected status")
            if path.startswith("/api"):
                data = json.loads(body)
                if not isinstance(data, dict) or not isinstance(data.get("items"), list):
                    raise RuntimeError("Public API returned unexpected data")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--instance-id", required=True)
    parser.add_argument("--backend-image", required=True)
    parser.add_argument("--frontend-image", required=True)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--public-origin", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"i-[0-9a-f]{8,17}", args.instance_id):
        raise ValueError("Invalid EC2 instance ID")
    validate_origin(args.public_origin)
    payload = parameters(args.backend_image, args.frontend_image, args.revision)
    result = aws("send-command", "--instance-ids", args.instance_id,
                 "--document-name", "AWS-RunShellScript", "--timeout-seconds", "120",
                 "--comment", f"cstudy deploy {args.revision}",
                 "--parameters", json.dumps(payload))
    command_id = result["Command"]["CommandId"]
    print(f"SSM command: {command_id}", flush=True)
    deadline = time.monotonic() + 1020
    while time.monotonic() < deadline:
        try:
            invocation = aws("get-command-invocation", "--command-id", command_id,
                             "--instance-id", args.instance_id)
        except subprocess.CalledProcessError as error:
            # SendCommand 직후에는 결과가 아직 등록되지 않았을 수 있다.
            if "InvocationDoesNotExist" not in error.stderr:
                raise
            time.sleep(5)
            continue
        status = invocation["Status"]
        if status not in ("Pending", "InProgress", "Delayed", "Cancelling"):
            print(invocation.get("StandardOutputContent", ""))
            print(invocation.get("StandardErrorContent", ""))
            if status != "Success":
                raise RuntimeError(f"EC2 deployment failed: {status}")
            check_public_site(args.public_origin)
            print("Public website and API checks passed")
            return
        time.sleep(5)
    raise TimeoutError("SSM deployment did not complete before the deadline")


if __name__ == "__main__":
    main()
