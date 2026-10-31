#!/usr/bin/env bash
# =====================================================================
# CR-073 G1 双 JVM 时间矩阵运行脚本（隔离库，TC-TIME-02）
# 用途：UTC 与 Asia/Shanghai 两个独立 JVM 分别写/读同一隔离 fixture，
#       并做交叉读（UTC 写→上海读；上海写→UTC 读），比对候选验收组，
#       生成默认路径差异清单。脚本失败必须返回非零（用户确认第 5 点）。
#
# 前置：
#   - 本地 MySQL 可达（默认 127.0.0.1:3306，可用 -Dtime.it.mysql.host/port 覆盖）
#   - 环境变量 VMD_IT_MYSQL_PASSWORD（或 -Dtime.it.mysql.password）——仓库不含口令
#   - 已执行 mvn test-compile
#
# 用法：scripts/time-governance/run_time_matrix.sh
# 输出：target/time-governance/ 下 JSON 证据 + diff-default-legacy.txt + 汇总
# =====================================================================
set -u
cd "$(dirname "$0")/../.." || exit 2   # 到 edd-vmd-service 根

OUT=target/time-governance
mkdir -p "$OUT"

# ---------- 1. 口令与 MySQL 检查 ----------
if [ -z "${VMD_IT_MYSQL_PASSWORD:-}" ] && [ -z "${TIME_IT_MYSQL_PASSWORD:-}" ]; then
  echo "ERROR: 未设置 VMD_IT_MYSQL_PASSWORD（仓库不含口令）。" >&2
  exit 2
fi
PWD_FLAG="-Dtime.it.mysql.password=${VMD_IT_MYSQL_PASSWORD:-${TIME_IT_MYSQL_PASSWORD:-}}"

SCHEMA="tmp_time_it_073_$(date +%s)_$$"
cleanup() {
  # 失败也清理隔离库（CR-073 硬约束：不得残留隔离 schema）
  java -Duser.timezone=UTC "$PWD_FLAG" -cp "${CP:-}" "${MAIN:-}" drop "$SCHEMA" >/dev/null 2>&1 || true
}

echo "== CR-073 G1 双 JVM 矩阵 =="
date -u +"%Y-%m-%dT%H:%M:%SZ"

# ---------- 2. classpath ----------
mvn -q dependency:build-classpath -Dmdep.outputFile="$OUT/cp.txt" >/dev/null 2>&1 \
  || { echo "ERROR: dependency:build-classpath 失败" >&2; exit 2; }
CP="$(cat "$OUT/cp.txt"):target/classes:target/test-classes"
MAIN="net.hwyz.iov.cloud.edd.vmd.service.timeit.TimeJdbcMatrixMain"
trap cleanup EXIT   # 在 MAIN/CP 就绪后注册 EXIT trap（set -u 下避免引用未定义变量）

# ---------- 3. 双 JVM 写/读 ----------
run_jvm() { # tz  phase  outfile
  java -Duser.timezone="$1" "$PWD_FLAG" -cp "$CP" "$MAIN" "$2" "$SCHEMA" > "$OUT/$3" 2>"$OUT/$3.err" \
    || { echo "ERROR: JVM $1 phase=$2 失败（exit $?），见 $3.err" >&2; exit 2; }
}

echo "-- UTC JVM 写 --"
run_jvm UTC write matrix-utc-write.json
echo "-- UTC JVM 自读 --"
run_jvm UTC read  matrix-utc-read.json
echo "-- Asia/Shanghai JVM 交叉读（读 UTC 写的数据）--"
run_jvm Asia/Shanghai read matrix-sh-read-utcwritten.json
echo "-- Asia/Shanghai JVM 写 --"
run_jvm Asia/Shanghai write matrix-sh-write.json
echo "-- Asia/Shanghai JVM 自读 --"
run_jvm Asia/Shanghai read matrix-sh-read.json
echo "-- UTC JVM 交叉读（读 Shanghai 写的数据）--"
run_jvm UTC read matrix-utc-read-shwritten.json

# ---------- 4. 比对 ----------
python3 - "$OUT" <<'PY'
import json, os, sys
out = sys.argv[1]

def rows(path):
    r = []
    for line in open(os.path.join(out, path)):
        line = line.strip()
        if not line or not line.startswith("{"):
            continue
        try:
            r.append(json.loads(line))
        except Exception as e:
            print("PARSE_ERR:", e, line[:80])
    return r

writes = rows("matrix-utc-write.json") + rows("matrix-sh-write.json")
reads = (rows("matrix-utc-read.json") + rows("matrix-sh-read-utcwritten.json")
         + rows("matrix-sh-read.json") + rows("matrix-utc-read-shwritten.json"))

# 候选验收组：connMode=UTC_CANDIDATE 且写成功；期望=sampleInstant（DATE 路径按毫秒承载精度），全部读 JVM 必须一致
cand = {r["rowKey"]: r for r in writes if r["connMode"] == "UTC_CANDIDATE" and r["status"] == "OK"}

def expected_for(w):
    exp = w["sampleInstant"]
    if w["writer"] == "DATE_SET_TIMESTAMP" and "." in exp and exp.endswith("Z"):
        base, frac = exp.split(".", 1)
        exp = base + "." + frac[:-1][:3] + "Z"   # java.util.Date 仅毫秒精度
    return exp

bykey = {}
for r in reads:
    bykey.setdefault(r["rowKey"], []).append(r)

failures = []
cand_checked = 0
for rk, w in cand.items():
    rd = bykey.get(rk, [])
    if not rd:
        failures.append(f"{rk}: 无读取记录"); continue
    cand_checked += 1
    expected = expected_for(w)
    # 交叉读时序：早于写入的读取为 NOT_FOUND（预期），跳过；只比对实际存在的读取
    actual = [rr for rr in rd if rr.get("readInstant") and rr["readInstant"] not in ("null", "N/A")]
    if not actual:
        failures.append(f"{rk}: 无实际读取记录（全部 NOT_FOUND/REJECTED）"); continue
    for rr in actual:
        if rr["readInstant"] != expected:
            failures.append(f"{rk}: 读取JVM={rr['jvmTz']} readInstant={rr['readInstant']} != expected {expected}")

# 默认路径风险组：记录差异清单（不失败，作为证据）
default_diff = []
for w in writes:
    if w["connMode"] != "DEFAULT":
        continue
    rk = w["rowKey"]
    rd = bykey.get(rk, [])
    for rr in rd:
        if not rr.get("readInstant") or rr["readInstant"] in ("null", "N/A"):
            continue   # REJECTED/NOT_FOUND：范围拒绝或写失败，非跨 JVM 差异
        if rr["readInstant"] != expected_for(w):
            default_diff.append(f"{rk} | jvmTz={rr['jvmTz']} | stored={rr.get('storedWallUtcCheck')} | read={rr['readInstant']} | expected={expected_for(w)}")

with open(os.path.join(out, "diff-default-legacy.txt"), "w") as f:
    f.write("# 默认URL/旧handler 路径跨JVM差异清单（CR-073 G1 证据，非验收组）\n")
    f.write("\n".join(default_diff) + ("\n" if default_diff else "(无差异)\n"))

with open(os.path.join(out, "matrix-summary.txt"), "w") as f:
    f.write(f"候选验收组行数: {cand_checked}\n")
    f.write(f"候选验收组失败: {len(failures)}\n")
    f.write(f"默认路径差异(记录): {len(default_diff)}\n")
    f.write("\n".join(failures))

print(f"候选验收组 checked={cand_checked} failures={len(failures)}")
print(f"默认路径差异(记录)={len(default_diff)}")
if failures:
    print("FAILURES:")
    print("\n".join(failures))
    sys.exit(1)
sys.exit(0)
PY
rc=$?

# ---------- 5. 清理隔离库 ----------
java -Duser.timezone=UTC "$PWD_FLAG" -cp "$CP" "$MAIN" drop "$SCHEMA" >/dev/null 2>&1

if [ "$rc" -eq 0 ]; then
  echo "== 双 JVM 矩阵：候选验收组通过 =="
else
  echo "== 双 JVM 矩阵：候选验收组失败（exit $rc）==" >&2
fi
exit "$rc"
