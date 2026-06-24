#!/usr/bin/env bash
set -euo pipefail

work_dir=""
memory_kb=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --work-dir)
      work_dir="$2"
      shift 2
      ;;
    --memory-kb)
      memory_kb="$2"
      shift 2
      ;;
    --)
      shift
      break
      ;;
    *)
      echo "Unknown sandbox argument: $1" >&2
      exit 2
      ;;
  esac
done

if [[ -z "$work_dir" || -z "$memory_kb" || $# -eq 0 ]]; then
  echo "Usage: csuft-oj-sandbox --work-dir DIR --memory-kb KB -- COMMAND..." >&2
  exit 2
fi

java_image="${JUDGE_JAVA_IMAGE:-eclipse-temurin:17-jdk}"
go_image="${JUDGE_GO_IMAGE:-golang:1.23}"

case "$1" in
  g++) image="gcc:14"; workspace_mode="rw"; nofile_limit=1024 ;;
  javac) image="$java_image"; workspace_mode="rw"; nofile_limit=1024 ;;
  java) image="$java_image"; workspace_mode="ro"; nofile_limit=64 ;;
  python3) image="python:3.12"; workspace_mode="ro"; nofile_limit=64 ;;
  go) image="$go_image"; workspace_mode="rw"; nofile_limit=1024 ;;
  ./*) image="gcc:14"; workspace_mode="ro"; nofile_limit=64 ;;
  *)
    echo "Unsupported sandbox command: $1" >&2
    exit 2
    ;;
esac

mount_source="$work_dir"
container_work_root="${JUDGE_CONTAINER_WORK_ROOT:-}"
host_work_root="${JUDGE_HOST_WORK_ROOT:-}"
if [[ -n "$container_work_root" && -n "$host_work_root" ]]; then
  case "$work_dir" in
    "$container_work_root"/*)
      mount_source="${host_work_root}${work_dir#"$container_work_root"}"
      ;;
    *)
      echo "Judge work directory is outside JUDGE_CONTAINER_WORK_ROOT" >&2
      exit 2
      ;;
  esac
fi

container_name="csuft-oj-judge-$$-${RANDOM}"
mount_spec="type=bind,src=${mount_source},dst=/workspace"
if [[ "$workspace_mode" == "ro" ]]; then
  mount_spec="${mount_spec},readonly"
fi
cleanup() {
  docker rm -f "$container_name" >/dev/null 2>&1 || true
}
trap cleanup EXIT TERM INT

docker run --rm --interactive \
  --name "$container_name" \
  --pull never \
  --network none \
  --cap-drop ALL \
  --security-opt no-new-privileges \
  --pids-limit 64 \
  --cpus 1 \
  --memory "${memory_kb}k" \
  --memory-swap "${memory_kb}k" \
  --ulimit "nofile=${nofile_limit}:${nofile_limit}" \
  --ulimit fsize=67108864:67108864 \
  --read-only \
  --tmpfs /tmp:rw,nosuid,nodev,size=64m \
  --mount "$mount_spec" \
  --workdir /workspace \
  --user "$(id -u):$(id -g)" \
  --env HOME=/tmp \
  --env GOCACHE=/tmp/go-cache \
  --env GOMAXPROCS=1 \
  "$image" "$@"
