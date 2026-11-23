#!/usr/bin/env python3
"""승인된 계정에서 실행하는 일회성 IAM/SSM 연결. AWS CLI 인증이 필요하다."""

import argparse
import json
import subprocess
import time
from pathlib import Path

ACCOUNT = "267653921725"
REGION = "ap-southeast-2"
INSTANCE = "i-00dc189aab9936eea"
EC2_ROLE = "cstudy-ec2-ssm"
GITHUB_ROLE = "cstudy-github-deploy"
CORE_POLICY = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
PROVIDER = f"arn:aws:iam::{ACCOUNT}:oidc-provider/token.actions.githubusercontent.com"


def aws(*args, missing_ok=False):
    result = subprocess.run(["aws", *args, "--region", REGION, "--output", "json",
                             "--no-cli-pager"], text=True, capture_output=True)
    if result.returncode:
        if missing_ok and "NoSuchEntity" in result.stderr:
            return None
        raise RuntimeError(result.stderr.strip())
    return json.loads(result.stdout) if result.stdout.strip() else {}


def role(name, trust):
    existing = aws("iam", "get-role", "--role-name", name, missing_ok=True)
    if existing:
        if existing["Role"]["AssumeRolePolicyDocument"] != trust:
            raise RuntimeError(f"Existing role {name} has a different trust policy; review it first")
        return existing["Role"]["Arn"]
    result = aws("iam", "create-role", "--role-name", name,
                 "--assume-role-policy-document", json.dumps(trust))
    return result["Role"]["Arn"]


def configure(apply=False):
    if aws("sts", "get-caller-identity")["Account"] != ACCOUNT:
        raise RuntimeError("Wrong AWS account")
    instance = aws("ec2", "describe-instances", "--instance-ids", INSTANCE)[
        "Reservations"][0]["Instances"][0]
    profile = instance.get("IamInstanceProfile", {}).get("Arn")
    expected_profile = f"arn:aws:iam::{ACCOUNT}:instance-profile/{EC2_ROLE}"
    if profile and profile != expected_profile:
        raise RuntimeError("Existing instance profile must be reviewed; it will not be replaced")
    print(json.dumps({"account": ACCOUNT, "instance": INSTANCE,
                      "existing_profile": profile, "apply": apply}), flush=True)
    if not apply:
        return

    folder = Path(__file__).resolve().parent
    ec2_trust = json.loads((folder / "ec2-trust.json").read_text())
    github_trust = json.loads((folder / "github-trust.json").read_text())
    github_policy = json.loads((folder / "github-deploy-policy.json").read_text())

    provider = aws("iam", "get-open-id-connect-provider",
                   "--open-id-connect-provider-arn", PROVIDER, missing_ok=True)
    if provider is None:
        aws("iam", "create-open-id-connect-provider",
            "--url", "https://token.actions.githubusercontent.com",
            "--client-id-list", "sts.amazonaws.com")
    elif "sts.amazonaws.com" not in provider["ClientIDList"]:
        raise RuntimeError("Existing OIDC provider has a different audience; review it first")

    role(EC2_ROLE, ec2_trust)
    aws("iam", "attach-role-policy", "--role-name", EC2_ROLE, "--policy-arn", CORE_POLICY)
    instance_profile = aws("iam", "get-instance-profile", "--instance-profile-name",
                           EC2_ROLE, missing_ok=True)
    if instance_profile is None:
        instance_profile = aws("iam", "create-instance-profile", "--instance-profile-name", EC2_ROLE)
    roles = instance_profile["InstanceProfile"]["Roles"]
    if roles and [r["RoleName"] for r in roles] != [EC2_ROLE]:
        raise RuntimeError("Instance profile contains an unexpected role")
    if not roles:
        aws("iam", "add-role-to-instance-profile", "--instance-profile-name", EC2_ROLE,
            "--role-name", EC2_ROLE)

    github_arn = role(GITHUB_ROLE, github_trust)
    attached = aws("iam", "list-attached-role-policies", "--role-name", GITHUB_ROLE)
    inline = aws("iam", "list-role-policies", "--role-name", GITHUB_ROLE)
    if attached["AttachedPolicies"] or set(inline["PolicyNames"]) - {"CstudyDeploy"}:
        raise RuntimeError("GitHub role has other permissions; review them first")
    aws("iam", "put-role-policy", "--role-name", GITHUB_ROLE, "--policy-name", "CstudyDeploy",
        "--policy-document", json.dumps(github_policy))

    if not profile:
        for attempt in range(12):
            try:
                aws("ec2", "associate-iam-instance-profile", "--instance-id", INSTANCE,
                    "--iam-instance-profile", f"Name={EC2_ROLE}")
                break
            except RuntimeError as error:
                if attempt == 11 or not any(message in str(error) for message in (
                        "InvalidParameterValue", "InvalidIamInstanceProfile", "Invalid IAM Instance Profile")):
                    raise
                time.sleep(5)
    print(json.dumps({"github_role_arn": github_arn, "ec2_profile": expected_profile}), flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="승인 후 실제 AWS 권한을 생성한다")
    configure(parser.parse_args().apply)
