import base64
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from deploy import Deployment, validate_images
import send_command


REVISION = "a" * 40
BACKEND = "oowgnus/cstudy-backend@sha256:" + "b" * 64
FRONTEND = "oowgnus/cstudy-frontend@sha256:" + "c" * 64


class FakeDeployment(Deployment):
    """실제 Docker나 네트워크를 사용하지 않는 배포 시뮬레이션."""

    def __init__(self, app_dir, failure=None):
        super().__init__(app_dir, attempts=1, delay=0)
        self.failure = failure
        self.calls = []
        self.images = {"backend": "oowgnus/cstudy-backend:v2",
                       "frontend": "oowgnus/cstudy-frontend:v1"}

    def container(self, service):
        return {"Config": {"Image": self.images[service], "Labels": {
            "org.opencontainers.image.revision": REVISION}}, "State": {"Running": True}}

    def compose(self, override, *args):
        self.calls.append(args)
        images = json.loads(override.read_text())["services"]
        if args[0] == "pull" and self.failure == "pull":
            raise RuntimeError("Registry unavailable")
        if args[0] == "up":
            name = args[-1]
            image = images[name]["image"]
            self.images[name] = image
            if self.failure == "frontend" and name == "frontend" and "@" in image:
                raise RuntimeError("Frontend failed to start")

    def backend_check(self):
        if self.failure == "backend" and "@" in self.images["backend"]:
            raise RuntimeError("Backend unavailable")

    def site_check(self):
        if self.failure == "health" and "@" in self.images["frontend"]:
            raise RuntimeError("API returns 502")


class DeploymentTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.app = Path(self.temp.name)
        (self.app / "compose.yaml").write_text("name: cstudy\nservices: {}\n")
        (self.app / ".env").write_text("PRIVATE_VALUE=do-not-change\n")

    def test_success_keeps_database_configuration_and_persists_images(self):
        deploy = FakeDeployment(self.app)
        deploy.deploy(BACKEND, FRONTEND, REVISION)
        override = json.loads((self.app / "compose.override.yaml").read_text())
        self.assertEqual(override["services"]["backend"]["image"], BACKEND)
        self.assertEqual(set(override["services"]), {"backend", "frontend"})
        self.assertEqual((self.app / ".env").read_text(), "PRIVATE_VALUE=do-not-change\n")
        self.assertEqual((self.app / "compose.yaml").read_text(), "name: cstudy\nservices: {}\n")
        self.assertEqual([call[-1] for call in deploy.calls if call[0] == "up"],
                         ["backend", "frontend"])
        self.assertTrue(all("--no-deps" in call and "never" in call
                            for call in deploy.calls if call[0] == "up"))

    def test_pull_failure_does_not_replace_any_running_container(self):
        deploy = FakeDeployment(self.app, "pull")
        with self.assertRaisesRegex(RuntimeError, "Registry"):
            deploy.deploy(BACKEND, FRONTEND, REVISION)
        self.assertFalse(any(call[0] == "up" for call in deploy.calls))
        self.assertFalse((self.app / "compose.override.yaml").exists())

    def test_failed_backend_frontend_or_health_restores_previous_images(self):
        for failure in ("backend", "frontend", "health"):
            with self.subTest(failure=failure):
                deploy = FakeDeployment(self.app, failure)
                previous = dict(deploy.images)
                with self.assertRaisesRegex(RuntimeError, "previous application restored"):
                    deploy.deploy(BACKEND, FRONTEND, REVISION)
                self.assertEqual(deploy.images, previous)
                self.assertFalse((self.app / "compose.override.yaml").exists())

    def test_failed_update_preserves_existing_successful_override(self):
        previous = self.app / "compose.override.yaml"
        previous.write_text(json.dumps({"x-cstudy-cd": True, "services": {}}))
        original = previous.read_text()
        with self.assertRaises(RuntimeError):
            FakeDeployment(self.app, "health").deploy(BACKEND, FRONTEND, REVISION)
        self.assertEqual(previous.read_text(), original)

    def test_unmanaged_override_is_never_overwritten(self):
        previous = self.app / "compose.override.yaml"
        previous.write_text("services:\n  frontend:\n    restart: always\n")
        deploy = FakeDeployment(self.app)
        with self.assertRaisesRegex(RuntimeError, "unmanaged"):
            deploy.deploy(BACKEND, FRONTEND, REVISION)
        self.assertEqual(deploy.calls, [])

    def test_invalid_image_or_revision_is_rejected_before_commands(self):
        for backend, frontend, revision in [
            ("other/project:latest", FRONTEND, REVISION),
            (BACKEND, "oowgnus/cstudy-frontend:latest", REVISION),
            (BACKEND + "; touch /tmp/injected", FRONTEND, REVISION),
            (BACKEND, FRONTEND, "main"),
        ]:
            with self.subTest(backend=backend, revision=revision):
                with self.assertRaises(ValueError):
                    validate_images(backend, frontend, revision)

    def test_wait_retries_transient_failure(self):
        deployment = Deployment(self.app, attempts=3, delay=0)
        with patch.object(deployment, "site_check", side_effect=[OSError("warming"), None]) as check:
            deployment.wait(check)
        self.assertEqual(check.call_count, 2)

    def test_wrong_running_revision_triggers_rollback(self):
        deploy = FakeDeployment(self.app)
        previous = dict(deploy.images)
        with self.assertRaisesRegex(RuntimeError, "previous application restored"):
            deploy.deploy(BACKEND, FRONTEND, "d" * 40)
        self.assertEqual(deploy.images, previous)

    def test_rollback_failure_is_reported(self):
        deploy = FakeDeployment(self.app, "backend")
        original = deploy.compose

        def fail_rollback(override, *args):
            if override.name == "rollback.json":
                raise RuntimeError("Rollback failed")
            return original(override, *args)

        with patch.object(deploy, "compose", side_effect=fail_rollback):
            with self.assertRaisesRegex(RuntimeError, "rollback failed"):
                deploy.deploy(BACKEND, FRONTEND, REVISION)

    def test_second_deployment_cannot_take_an_existing_lock(self):
        import fcntl

        with (self.app / ".deploy.lock").open("a") as lock:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
            deploy = FakeDeployment(self.app)
            with self.assertRaises(BlockingIOError):
                deploy.deploy(BACKEND, FRONTEND, REVISION)
            self.assertEqual(deploy.calls, [])


class CommandTests(unittest.TestCase):
    def test_payload_delivers_exact_script_and_validated_arguments(self):
        payload = send_command.parameters(BACKEND, FRONTEND, REVISION)
        command = payload["commands"][4]
        encoded = command.split("printf '%s' ", 1)[1].split(" |", 1)[0]
        self.assertEqual(base64.b64decode(encoded),
                         Path(send_command.__file__).with_name("deploy.py").read_bytes())
        self.assertIn(BACKEND, payload["commands"][-1])
        self.assertIn(REVISION, payload["commands"][-1])
        self.assertEqual(payload["executionTimeout"], ["900"])

    def test_origin_rejects_credentials_and_non_http_urls(self):
        for origin in ("file:///etc/passwd", "https://user:password@example.com",
                       "http://example.com/path", "http://example.com?token=secret"):
            with self.subTest(origin=origin), self.assertRaises(ValueError):
                send_command.validate_origin(origin)
        send_command.validate_origin("http://3.107.45.251")

    def run_command(self, responses):
        args = ["send_command.py", "--instance-id", "i-00dc189aab9936eea",
                "--backend-image", BACKEND, "--frontend-image", FRONTEND,
                "--revision", REVISION, "--public-origin", "http://example.com"]
        with patch("sys.argv", args), patch.object(send_command, "aws", side_effect=responses), \
                patch.object(send_command.time, "sleep"), \
                patch.object(send_command, "check_public_site") as public_check:
            send_command.main()
            return public_check

    def test_waits_for_remote_success_before_checking_public_site(self):
        not_ready = subprocess.CalledProcessError(1, "aws", stderr="InvocationDoesNotExist")
        public_check = self.run_command([
            {"Command": {"CommandId": "test-id"}}, not_ready,
            {"Status": "InProgress"}, {"Status": "Success"},
        ])
        public_check.assert_called_once_with("http://example.com")

    def test_failed_remote_command_is_not_reported_as_success(self):
        with self.assertRaisesRegex(RuntimeError, "deployment failed: Failed"):
            self.run_command([{"Command": {"CommandId": "test-id"}}, {"Status": "Failed"}])


if __name__ == "__main__":
    unittest.main()
