#!/usr/bin/env python3
"""Seed 50 classic algorithm problems and judge test cases for CSUFT OJ.

Run on the production server from any directory:
    sudo python3 seed-50-classic-problems.py

The script is idempotent by title. Existing seeded problems keep their IDs, so
submissions remain attached, while statements and test cases are refreshed.
"""

from __future__ import annotations

import argparse
import heapq
import math
import os
import shutil
import subprocess
from collections import Counter, deque
from dataclasses import dataclass
from pathlib import Path
from typing import Callable


APP_DIR = Path("/srv/csuft-oj-app")
TESTCASE_BASE = Path("/srv/csuft-oj/testcases")
PREFIX = "[经典算法] "
MYSQL_CMD = [
    "docker",
    "compose",
    "--env-file",
    ".env",
    "exec",
    "-T",
    "mysql",
    "sh",
    "-lc",
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 --batch --raw --skip-column-names csuft_oj',
]


@dataclass
class ProblemSpec:
    title: str
    difficulty: str
    description: str
    input_description: str
    output_description: str
    sample_input: str
    tests: list[str]
    solver: Callable[[str], str]
    time_limit_ms: int = 1000
    memory_limit_kb: int = 262144


def out(text: str) -> str:
    return text.rstrip() + "\n"


def lines(values) -> str:
    return out("\n".join(str(v) for v in values))


def sql_string(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def run_mysql(sql: str, dry_run: bool = False) -> str:
    if dry_run:
        return ""
    result = subprocess.run(
        MYSQL_CMD,
        input=sql,
        text=True,
        cwd=APP_DIR,
        check=True,
        capture_output=True,
    )
    return result.stdout.strip()


def one_int_output(value: int) -> str:
    return out(str(value))


def solve_a_plus_b(data: str) -> str:
    a, b = map(int, data.split())
    return one_int_output(a + b)


def solve_gcd(data: str) -> str:
    a, b = map(int, data.split())
    return one_int_output(math.gcd(a, b))


def solve_lcm(data: str) -> str:
    a, b = map(int, data.split())
    return one_int_output(abs(a // math.gcd(a, b) * b))


def solve_is_prime(data: str) -> str:
    n = int(data.strip())
    if n < 2:
        return out("NO")
    i = 2
    while i * i <= n:
        if n % i == 0:
            return out("NO")
        i += 1
    return out("YES")


def sieve(n: int) -> list[bool]:
    prime = [True] * (n + 1)
    if n >= 0:
        prime[0] = False
    if n >= 1:
        prime[1] = False
    for i in range(2, int(n**0.5) + 1):
        if prime[i]:
            for j in range(i * i, n + 1, i):
                prime[j] = False
    return prime


def solve_count_primes(data: str) -> str:
    n = int(data.strip())
    return one_int_output(sum(sieve(n)))


def solve_fib_mod(data: str) -> str:
    n, mod = map(int, data.split())
    a, b = 0, 1
    for _ in range(n):
        a, b = b % mod, (a + b) % mod
    return one_int_output(a % mod)


def solve_trailing_zeroes(data: str) -> str:
    n = int(data.strip())
    ans = 0
    while n:
        n //= 5
        ans += n
    return one_int_output(ans)


def solve_pow_mod(data: str) -> str:
    a, b, mod = map(int, data.split())
    return one_int_output(pow(a, b, mod))


def solve_palindrome(data: str) -> str:
    s = data.rstrip("\n")
    return out("YES" if s == s[::-1] else "NO")


def solve_reverse_sequence(data: str) -> str:
    nums = list(map(int, data.split()))
    n, arr = nums[0], nums[1:]
    return out(" ".join(map(str, arr[:n][::-1])))


def solve_two_sum(data: str) -> str:
    nums = list(map(int, data.split()))
    n, target = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    seen = {}
    for i, x in enumerate(arr, 1):
        if target - x in seen:
            return out(f"{seen[target - x]} {i}")
        if x not in seen:
            seen[x] = i
    return out("-1")


def solve_max_subarray(data: str) -> str:
    nums = list(map(int, data.split()))
    arr = nums[1:]
    best = cur = arr[0]
    for x in arr[1:]:
        cur = max(x, cur + x)
        best = max(best, cur)
    return one_int_output(best)


def solve_prefix_sum(data: str) -> str:
    nums = list(map(int, data.split()))
    n, q = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    pref = [0]
    for x in arr:
        pref.append(pref[-1] + x)
    p = 2 + n
    ans = []
    for _ in range(q):
        l, r = nums[p], nums[p + 1]
        p += 2
        ans.append(pref[r] - pref[l - 1])
    return lines(ans)


def solve_difference_array(data: str) -> str:
    nums = list(map(int, data.split()))
    n, q = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    diff = [0] * (n + 2)
    p = 2 + n
    for _ in range(q):
        l, r, x = nums[p], nums[p + 1], nums[p + 2]
        p += 3
        diff[l - 1] += x
        diff[r] -= x
    cur = 0
    for i in range(n):
        cur += diff[i]
        arr[i] += cur
    return out(" ".join(map(str, arr)))


def solve_binary_search(data: str) -> str:
    nums = list(map(int, data.split()))
    n, target = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    lo, hi = 0, n - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if arr[mid] == target:
            return one_int_output(mid + 1)
        if arr[mid] < target:
            lo = mid + 1
        else:
            hi = mid - 1
    return out("-1")


def solve_lower_bound(data: str) -> str:
    nums = list(map(int, data.split()))
    n, x = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    lo, hi = 0, n
    while lo < hi:
        mid = (lo + hi) // 2
        if arr[mid] >= x:
            hi = mid
        else:
            lo = mid + 1
    return one_int_output(lo + 1 if lo < n else n + 1)


def solve_merge_sorted(data: str) -> str:
    nums = list(map(int, data.split()))
    n, m = nums[0], nums[1]
    a = nums[2 : 2 + n]
    b = nums[2 + n : 2 + n + m]
    i = j = 0
    ans = []
    while i < n or j < m:
        if j == m or (i < n and a[i] <= b[j]):
            ans.append(a[i])
            i += 1
        else:
            ans.append(b[j])
            j += 1
    return out(" ".join(map(str, ans)))


def solve_sort_unique(data: str) -> str:
    nums = list(map(int, data.split()))
    arr = sorted(set(nums[1:]))
    return out(" ".join(map(str, arr)))


def solve_inversions(data: str) -> str:
    nums = list(map(int, data.split()))
    arr = nums[1:]

    def merge_count(a: list[int]) -> tuple[list[int], int]:
        if len(a) <= 1:
            return a, 0
        mid = len(a) // 2
        left, c1 = merge_count(a[:mid])
        right, c2 = merge_count(a[mid:])
        i = j = inv = 0
        merged = []
        while i < len(left) or j < len(right):
            if j == len(right) or (i < len(left) and left[i] <= right[j]):
                merged.append(left[i])
                i += 1
            else:
                merged.append(right[j])
                inv += len(left) - i
                j += 1
        return merged, c1 + c2 + inv

    return one_int_output(merge_count(arr)[1])


def solve_mode(data: str) -> str:
    nums = list(map(int, data.split()))
    cnt = Counter(nums[1:])
    best = min(cnt, key=lambda x: (-cnt[x], x))
    return out(f"{best} {cnt[best]}")


def solve_brackets(data: str) -> str:
    s = data.strip()
    mp = {")": "(", "]": "[", "}": "{"}
    st = []
    for ch in s:
        if ch in "([{":
            st.append(ch)
        elif not st or st.pop() != mp[ch]:
            return out("NO")
    return out("YES" if not st else "NO")


def solve_stack_sim(data: str) -> str:
    it = iter(data.strip().splitlines())
    n = int(next(it))
    st, ans = [], []
    for _ in range(n):
        parts = next(it).split()
        if parts[0] == "push":
            st.append(parts[1])
        elif parts[0] == "pop":
            ans.append(st.pop() if st else "EMPTY")
        else:
            ans.append(st[-1] if st else "EMPTY")
    return lines(ans)


def solve_queue_sim(data: str) -> str:
    it = iter(data.strip().splitlines())
    n = int(next(it))
    q, ans = deque(), []
    for _ in range(n):
        parts = next(it).split()
        if parts[0] == "push":
            q.append(parts[1])
        elif parts[0] == "pop":
            ans.append(q.popleft() if q else "EMPTY")
        else:
            ans.append(q[0] if q else "EMPTY")
    return lines(ans)


def solve_next_greater(data: str) -> str:
    nums = list(map(int, data.split()))
    arr = nums[1:]
    ans = [-1] * len(arr)
    st = []
    for i, x in enumerate(arr):
        while st and arr[st[-1]] < x:
            ans[st.pop()] = x
        st.append(i)
    return out(" ".join(map(str, ans)))


def solve_sliding_max(data: str) -> str:
    nums = list(map(int, data.split()))
    n, k = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    q, ans = deque(), []
    for i, x in enumerate(arr):
        while q and q[0] <= i - k:
            q.popleft()
        while q and arr[q[-1]] <= x:
            q.pop()
        q.append(i)
        if i >= k - 1:
            ans.append(arr[q[0]])
    return out(" ".join(map(str, ans)))


def solve_grid_bfs(data: str) -> str:
    lines_in = data.strip().splitlines()
    n, m = map(int, lines_in[0].split())
    grid = lines_in[1 : 1 + n]
    start = end = (-1, -1)
    for i in range(n):
        for j in range(m):
            if grid[i][j] == "S":
                start = (i, j)
            if grid[i][j] == "T":
                end = (i, j)
    q = deque([(start[0], start[1], 0)])
    seen = {start}
    while q:
        x, y, d = q.popleft()
        if (x, y) == end:
            return one_int_output(d)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < n and 0 <= ny < m and grid[nx][ny] != "#" and (nx, ny) not in seen:
                seen.add((nx, ny))
                q.append((nx, ny, d + 1))
    return out("-1")


def solve_components(data: str) -> str:
    lines_in = data.strip().splitlines()
    n, m = map(int, lines_in[0].split())
    grid = lines_in[1 : 1 + n]
    seen = [[False] * m for _ in range(n)]
    ans = 0
    for i in range(n):
        for j in range(m):
            if grid[i][j] == "1" and not seen[i][j]:
                ans += 1
                q = deque([(i, j)])
                seen[i][j] = True
                while q:
                    x, y = q.popleft()
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < n and 0 <= ny < m and grid[nx][ny] == "1" and not seen[nx][ny]:
                            seen[nx][ny] = True
                            q.append((nx, ny))
    return one_int_output(ans)


def solve_dsu(data: str) -> str:
    nums = data.strip().splitlines()
    n, q = map(int, nums[0].split())
    parent = list(range(n + 1))

    def find(x: int) -> int:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    ans = []
    for line in nums[1 : 1 + q]:
        op, a, b = line.split()
        a, b = int(a), int(b)
        if op == "U":
            parent[find(a)] = find(b)
        else:
            ans.append("YES" if find(a) == find(b) else "NO")
    return lines(ans)


def solve_mst(data: str) -> str:
    nums = list(map(int, data.split()))
    n, m = nums[0], nums[1]
    edges = []
    p = 2
    for _ in range(m):
        u, v, w = nums[p], nums[p + 1], nums[p + 2]
        p += 3
        edges.append((w, u, v))
    parent = list(range(n + 1))

    def find(x: int) -> int:
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    total = used = 0
    for w, u, v in sorted(edges):
        fu, fv = find(u), find(v)
        if fu != fv:
            parent[fu] = fv
            total += w
            used += 1
    return one_int_output(total if used == n - 1 else -1)


def solve_dijkstra(data: str) -> str:
    nums = list(map(int, data.split()))
    n, m, s = nums[0], nums[1], nums[2]
    g = [[] for _ in range(n + 1)]
    p = 3
    for _ in range(m):
        u, v, w = nums[p], nums[p + 1], nums[p + 2]
        p += 3
        g[u].append((v, w))
    dist = [10**18] * (n + 1)
    dist[s] = 0
    heap = [(0, s)]
    while heap:
        d, u = heapq.heappop(heap)
        if d != dist[u]:
            continue
        for v, w in g[u]:
            if d + w < dist[v]:
                dist[v] = d + w
                heapq.heappush(heap, (dist[v], v))
    return out(" ".join(str(-1 if dist[i] == 10**18 else dist[i]) for i in range(1, n + 1)))


def solve_floyd(data: str) -> str:
    nums = list(map(int, data.split()))
    n, m, q = nums[0], nums[1], nums[2]
    inf = 10**12
    dist = [[inf] * (n + 1) for _ in range(n + 1)]
    for i in range(1, n + 1):
        dist[i][i] = 0
    p = 3
    for _ in range(m):
        u, v, w = nums[p], nums[p + 1], nums[p + 2]
        p += 3
        dist[u][v] = dist[v][u] = min(dist[u][v], w)
    for k in range(1, n + 1):
        for i in range(1, n + 1):
            for j in range(1, n + 1):
                if dist[i][k] + dist[k][j] < dist[i][j]:
                    dist[i][j] = dist[i][k] + dist[k][j]
    ans = []
    for _ in range(q):
        a, b = nums[p], nums[p + 1]
        p += 2
        ans.append(-1 if dist[a][b] == inf else dist[a][b])
    return lines(ans)


def solve_topo_cycle(data: str) -> str:
    nums = list(map(int, data.split()))
    n, m = nums[0], nums[1]
    g = [[] for _ in range(n + 1)]
    indeg = [0] * (n + 1)
    p = 2
    for _ in range(m):
        u, v = nums[p], nums[p + 1]
        p += 2
        g[u].append(v)
        indeg[v] += 1
    q = deque(i for i in range(1, n + 1) if indeg[i] == 0)
    cnt = 0
    while q:
        u = q.popleft()
        cnt += 1
        for v in g[u]:
            indeg[v] -= 1
            if indeg[v] == 0:
                q.append(v)
    return out("YES" if cnt == n else "NO")


def solve_zero_one_knapsack(data: str) -> str:
    nums = list(map(int, data.split()))
    n, cap = nums[0], nums[1]
    dp = [0] * (cap + 1)
    p = 2
    for _ in range(n):
        w, v = nums[p], nums[p + 1]
        p += 2
        for c in range(cap, w - 1, -1):
            dp[c] = max(dp[c], dp[c - w] + v)
    return one_int_output(max(dp))


def solve_complete_knapsack(data: str) -> str:
    nums = list(map(int, data.split()))
    n, cap = nums[0], nums[1]
    dp = [0] * (cap + 1)
    p = 2
    for _ in range(n):
        w, v = nums[p], nums[p + 1]
        p += 2
        for c in range(w, cap + 1):
            dp[c] = max(dp[c], dp[c - w] + v)
    return one_int_output(max(dp))


def solve_lis(data: str) -> str:
    nums = list(map(int, data.split()))
    tails = []
    for x in nums[1:]:
        lo, hi = 0, len(tails)
        while lo < hi:
            mid = (lo + hi) // 2
            if tails[mid] >= x:
                hi = mid
            else:
                lo = mid + 1
        if lo == len(tails):
            tails.append(x)
        else:
            tails[lo] = x
    return one_int_output(len(tails))


def solve_lcs(data: str) -> str:
    a, b = data.strip().splitlines()[:2]
    dp = [0] * (len(b) + 1)
    for ca in a:
        prev = 0
        for j, cb in enumerate(b, 1):
            tmp = dp[j]
            if ca == cb:
                dp[j] = prev + 1
            else:
                dp[j] = max(dp[j], dp[j - 1])
            prev = tmp
    return one_int_output(dp[-1])


def solve_edit_distance(data: str) -> str:
    a, b = data.strip().splitlines()[:2]
    dp = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        prev = dp[0]
        dp[0] = i
        for j, cb in enumerate(b, 1):
            tmp = dp[j]
            dp[j] = prev if ca == cb else min(prev, dp[j], dp[j - 1]) + 1
            prev = tmp
    return one_int_output(dp[-1])


def solve_min_coins(data: str) -> str:
    nums = list(map(int, data.split()))
    n, amount = nums[0], nums[1]
    coins = nums[2 : 2 + n]
    inf = 10**9
    dp = [inf] * (amount + 1)
    dp[0] = 0
    for c in coins:
        for x in range(c, amount + 1):
            dp[x] = min(dp[x], dp[x - c] + 1)
    return one_int_output(-1 if dp[amount] == inf else dp[amount])


def solve_interval_schedule(data: str) -> str:
    nums = list(map(int, data.split()))
    n = nums[0]
    intervals = []
    p = 1
    for _ in range(n):
        l, r = nums[p], nums[p + 1]
        p += 2
        intervals.append((r, l))
    ans = last = 0
    for r, l in sorted(intervals):
        if l >= last:
            ans += 1
            last = r
    return one_int_output(ans)


def solve_greedy_change(data: str) -> str:
    nums = list(map(int, data.split()))
    coins = [100, 50, 20, 10, 5, 1]
    n = nums[0]
    ans = 0
    for c in coins:
        ans += n // c
        n %= c
    return one_int_output(ans)


def solve_level_order(data: str) -> str:
    nums = list(map(int, data.split()))
    n = nums[0]
    g = [[] for _ in range(n + 1)]
    p = 1
    for _ in range(n - 1):
        u, v = nums[p], nums[p + 1]
        p += 2
        g[u].append(v)
        g[v].append(u)
    ans, q, seen = [], deque([1]), {1}
    while q:
        u = q.popleft()
        ans.append(u)
        for v in sorted(g[u]):
            if v not in seen:
                seen.add(v)
                q.append(v)
    return out(" ".join(map(str, ans)))


def solve_bst_inorder(data: str) -> str:
    nums = list(map(int, data.split()))
    arr = nums[1:]
    return out(" ".join(map(str, sorted(arr))))


def solve_heap_pop(data: str) -> str:
    nums = list(map(int, data.split()))
    h = nums[1:]
    heapq.heapify(h)
    ans = [heapq.heappop(h) for _ in range(len(h))]
    return out(" ".join(map(str, ans)))


def solve_word_count(data: str) -> str:
    words = data.strip().split()
    n = int(words[0])
    cnt = Counter(words[1 : 1 + n])
    return lines(f"{k} {cnt[k]}" for k in sorted(cnt))


def solve_kmp(data: str) -> str:
    pattern, text = data.strip().splitlines()[:2]
    pi = [0] * len(pattern)
    for i in range(1, len(pattern)):
        j = pi[i - 1]
        while j and pattern[i] != pattern[j]:
            j = pi[j - 1]
        if pattern[i] == pattern[j]:
            j += 1
        pi[i] = j
    ans, j = [], 0
    for i, ch in enumerate(text):
        while j and ch != pattern[j]:
            j = pi[j - 1]
        if ch == pattern[j]:
            j += 1
        if j == len(pattern):
            ans.append(i - len(pattern) + 2)
            j = pi[j - 1]
    return out(" ".join(map(str, ans)) if ans else "-1")


def solve_trie_prefix(data: str) -> str:
    rows = data.strip().splitlines()
    n, q = map(int, rows[0].split())
    words = rows[1 : 1 + n]
    queries = rows[1 + n : 1 + n + q]
    return lines(sum(w.startswith(query) for w in words) for query in queries)


def solve_quickselect(data: str) -> str:
    nums = list(map(int, data.split()))
    n, k = nums[0], nums[1]
    arr = nums[2 : 2 + n]
    return one_int_output(sorted(arr)[k - 1])


def solve_matrix_multiply(data: str) -> str:
    nums = list(map(int, data.split()))
    n, m, p = nums[0], nums[1], nums[2]
    idx = 3
    a = []
    for _ in range(n):
        a.append(nums[idx : idx + m])
        idx += m
    b = []
    for _ in range(m):
        b.append(nums[idx : idx + p])
        idx += p
    c = [[sum(a[i][k] * b[k][j] for k in range(m)) for j in range(p)] for i in range(n)]
    return lines(" ".join(map(str, row)) for row in c)


def solve_rotate_matrix(data: str) -> str:
    nums = list(map(int, data.split()))
    n = nums[0]
    arr = nums[1:]
    mat = [arr[i * n : (i + 1) * n] for i in range(n)]
    rot = [[mat[n - 1 - i][j] for i in range(n)] for j in range(n)]
    return lines(" ".join(map(str, row)) for row in rot)


def solve_josephus(data: str) -> str:
    n, k = map(int, data.split())
    people = list(range(1, n + 1))
    idx = 0
    while len(people) > 1:
        idx = (idx + k - 1) % len(people)
        people.pop(idx)
    return one_int_output(people[0])


def spec(
    title: str,
    difficulty: str,
    description: str,
    input_description: str,
    output_description: str,
    sample_input: str,
    tests: list[str],
    solver: Callable[[str], str],
    time_limit_ms: int = 1000,
) -> ProblemSpec:
    return ProblemSpec(
        title=PREFIX + title,
        difficulty=difficulty,
        description=description,
        input_description=input_description,
        output_description=output_description,
        sample_input=sample_input,
        tests=tests,
        solver=solver,
        time_limit_ms=time_limit_ms,
    )


PROBLEMS = [
    spec("001 A+B Problem", "EASY", "给定两个整数 a 和 b，输出它们的和。", "一行两个整数 a b。", "输出一个整数，表示 a+b。", "3 5\n", ["3 5\n", "-10 7\n", "123456789 987654321\n"], solve_a_plus_b),
    spec("002 最大公约数", "EASY", "给定两个正整数，求它们的最大公约数。", "一行两个正整数 a b。", "输出最大公约数。", "24 36\n", ["24 36\n", "17 29\n", "123456 789012\n"], solve_gcd),
    spec("003 最小公倍数", "EASY", "给定两个正整数，求它们的最小公倍数。", "一行两个正整数 a b。", "输出最小公倍数。", "6 8\n", ["6 8\n", "21 6\n", "1234 5678\n"], solve_lcm),
    spec("004 素数判定", "EASY", "判断给定整数 n 是否为素数。", "一行一个整数 n。", "若 n 是素数输出 YES，否则输出 NO。", "17\n", ["17\n", "1\n", "99991\n"], solve_is_prime),
    spec("005 统计素数", "EASY", "统计不超过 n 的素数个数。", "一行一个整数 n。", "输出区间 [1,n] 中素数数量。", "10\n", ["10\n", "100\n", "10000\n"], solve_count_primes),
    spec("006 斐波那契取模", "EASY", "求第 n 项斐波那契数对 mod 取模的结果，F0=0，F1=1。", "一行两个整数 n mod。", "输出 Fn mod mod。", "10 1000\n", ["10 1000\n", "0 7\n", "100000 1000000007\n"], solve_fib_mod),
    spec("007 阶乘末尾零", "EASY", "求 n! 的十进制表示末尾有多少个 0。", "一行一个整数 n。", "输出末尾零的数量。", "10\n", ["10\n", "100\n", "1000000000\n"], solve_trailing_zeroes),
    spec("008 快速幂取模", "EASY", "计算 a 的 b 次方对 mod 取模的结果。", "一行三个整数 a b mod。", "输出 a^b mod mod。", "2 10 1000\n", ["2 10 1000\n", "7 0 13\n", "123456789 987654321 1000000007\n"], solve_pow_mod),
    spec("009 回文字符串", "EASY", "判断一个字符串是否从左到右和从右到左完全相同。", "一行一个仅由可见字符组成的字符串。", "是回文输出 YES，否则输出 NO。", "level\n", ["level\n", "algorithm\n", "abccba\n"], solve_palindrome),
    spec("010 反转整数序列", "EASY", "给定长度为 n 的整数序列，按相反顺序输出。", "第一行为 n，第二行 n 个整数。", "一行输出反转后的序列。", "5\n1 2 3 4 5\n", ["5\n1 2 3 4 5\n", "1\n42\n", "6\n-1 0 3 3 9 10\n"], solve_reverse_sequence),
    spec("011 两数之和", "EASY", "在数组中寻找两个不同位置的数，使其和等于目标值。", "第一行 n target，第二行 n 个整数。", "输出任意一组 1-based 下标；不存在则输出 -1。", "5 9\n2 7 11 15 1\n", ["5 9\n2 7 11 15 1\n", "4 100\n1 2 3 4\n", "6 6\n3 2 4 3 5 1\n"], solve_two_sum),
    spec("012 最大子段和", "EASY", "求非空连续子数组的最大元素和。", "第一行 n，第二行 n 个整数。", "输出最大子段和。", "9\n-2 1 -3 4 -1 2 1 -5 4\n", ["9\n-2 1 -3 4 -1 2 1 -5 4\n", "3\n-5 -2 -9\n", "6\n1 2 3 -10 5 6\n"], solve_max_subarray),
    spec("013 前缀和区间查询", "EASY", "给定数组和多个区间，快速回答每个闭区间的元素和。", "第一行 n q，第二行 n 个整数，之后 q 行每行 l r。", "对每个查询输出区间和。", "5 3\n1 2 3 4 5\n1 3\n2 5\n4 4\n", ["5 3\n1 2 3 4 5\n1 3\n2 5\n4 4\n", "4 2\n-1 5 -2 7\n1 4\n2 3\n", "6 3\n10 20 30 40 50 60\n3 6\n1 1\n5 5\n"], solve_prefix_sum),
    spec("014 差分数组", "EASY", "给定数组和若干区间加法操作，输出所有操作后的数组。", "第一行 n q，第二行 n 个整数，之后 q 行 l r x。", "输出最终数组。", "5 2\n1 2 3 4 5\n1 3 10\n2 5 -1\n", ["5 2\n1 2 3 4 5\n1 3 10\n2 5 -1\n", "3 1\n0 0 0\n1 3 5\n", "6 3\n1 1 1 1 1 1\n2 4 2\n3 6 3\n1 1 -1\n"], solve_difference_array),
    spec("015 二分查找", "EASY", "在升序数组中查找目标值第一次出现的位置。", "第一行 n target，第二行 n 个升序整数。", "存在输出 1-based 下标，否则输出 -1。", "5 7\n1 3 5 7 9\n", ["5 7\n1 3 5 7 9\n", "5 2\n1 3 5 7 9\n", "6 4\n1 2 3 4 4 5\n"], solve_binary_search),
    spec("016 lower_bound", "EASY", "求升序数组中第一个大于等于 x 的位置。", "第一行 n x，第二行 n 个升序整数。", "输出 1-based 位置；若所有数都小于 x，输出 n+1。", "5 6\n1 3 5 7 9\n", ["5 6\n1 3 5 7 9\n", "5 10\n1 3 5 7 9\n", "5 1\n1 1 2 2 3\n"], solve_lower_bound),
    spec("017 合并有序数组", "EASY", "合并两个非降序数组，保持整体非降序。", "第一行 n m，第二行 n 个数，第三行 m 个数。", "输出合并后的序列。", "3 4\n1 4 7\n2 3 8 9\n", ["3 4\n1 4 7\n2 3 8 9\n", "1 3\n5\n1 2 10\n", "4 4\n1 1 2 9\n1 3 3 4\n"], solve_merge_sorted),
    spec("018 排序去重", "EASY", "对整数序列排序，并删除重复元素。", "第一行 n，第二行 n 个整数。", "输出升序去重后的序列。", "7\n3 1 2 3 2 5 1\n", ["7\n3 1 2 3 2 5 1\n", "5\n5 4 3 2 1\n", "6\n-1 -1 0 2 2 -3\n"], solve_sort_unique),
    spec("019 逆序对计数", "MEDIUM", "统计数组中满足 i<j 且 ai>aj 的逆序对数量。", "第一行 n，第二行 n 个整数。", "输出逆序对数量。", "5\n5 4 3 2 1\n", ["5\n5 4 3 2 1\n", "5\n1 2 3 4 5\n", "8\n2 4 1 3 5 0 6 7\n"], solve_inversions, 2000),
    spec("020 众数", "EASY", "找出出现次数最多的整数；若有多个，输出数值最小者。", "第一行 n，第二行 n 个整数。", "输出众数及其出现次数。", "7\n1 2 2 3 3 3 4\n", ["7\n1 2 2 3 3 3 4\n", "6\n5 5 4 4 3 3\n", "5\n-1 -1 -1 2 2\n"], solve_mode),
    spec("021 括号匹配", "EASY", "判断括号串中的小括号、中括号和大括号是否正确匹配。", "一行一个只含括号字符的字符串。", "匹配输出 YES，否则输出 NO。", "([]{})\n", ["([]{})\n", "([)]\n", "(((())))\n"], solve_brackets),
    spec("022 栈模拟", "EASY", "模拟栈的 push、pop、top 操作。", "第一行操作数 n，之后每行一个操作。", "对 pop 和 top 输出结果，空栈输出 EMPTY。", "6\npush 1\npush 2\ntop\npop\npop\npop\n", ["6\npush 1\npush 2\ntop\npop\npop\npop\n", "4\ntop\npush 9\ntop\npop\n", "5\npush a\npush b\npop\ntop\npop\n"], solve_stack_sim),
    spec("023 队列模拟", "EASY", "模拟队列的 push、pop、front 操作。", "第一行操作数 n，之后每行一个操作。", "对 pop 和 front 输出结果，空队列输出 EMPTY。", "6\npush 1\npush 2\nfront\npop\npop\npop\n", ["6\npush 1\npush 2\nfront\npop\npop\npop\n", "4\nfront\npush 7\nfront\npop\n", "5\npush x\npush y\npop\nfront\npop\n"], solve_queue_sim),
    spec("024 下一个更大元素", "MEDIUM", "对数组中每个位置，求其右侧第一个严格大于它的元素。", "第一行 n，第二行 n 个整数。", "输出每个位置的答案，不存在则为 -1。", "5\n2 1 2 4 3\n", ["5\n2 1 2 4 3\n", "4\n4 3 2 1\n", "6\n1 3 2 4 2 5\n"], solve_next_greater),
    spec("025 滑动窗口最大值", "MEDIUM", "给定长度为 k 的滑动窗口，输出每个窗口的最大值。", "第一行 n k，第二行 n 个整数。", "按窗口从左到右输出最大值。", "8 3\n1 3 -1 -3 5 3 6 7\n", ["8 3\n1 3 -1 -3 5 3 6 7\n", "5 1\n5 4 3 2 1\n", "6 4\n2 2 2 1 5 3\n"], solve_sliding_max),
    spec("026 网格 BFS 最短路", "MEDIUM", "在网格中从 S 出发走到 T，# 为障碍，只能上下左右移动。", "第一行 n m，之后 n 行网格。", "输出最短步数，不可达输出 -1。", "3 4\nS..#\n.#..\n...T\n", ["3 4\nS..#\n.#..\n...T\n", "2 2\nS#\n#T\n", "5 5\nS....\n###.#\n...#.\n.#...\n...T.\n"], solve_grid_bfs),
    spec("027 连通块计数", "MEDIUM", "统计 01 网格中由字符 1 组成的四连通块数量。", "第一行 n m，之后 n 行 01 字符串。", "输出连通块数量。", "4 5\n11000\n11010\n00100\n00011\n", ["4 5\n11000\n11010\n00100\n00011\n", "2 3\n000\n000\n", "3 3\n101\n010\n101\n"], solve_components),
    spec("028 并查集连通性", "MEDIUM", "维护若干元素的连通关系，支持合并和查询。", "第一行 n q，之后 q 行：U a b 表示合并，Q a b 表示查询。", "对每个查询输出 YES 或 NO。", "5 5\nU 1 2\nQ 1 2\nQ 1 3\nU 2 3\nQ 1 3\n", ["5 5\nU 1 2\nQ 1 2\nQ 1 3\nU 2 3\nQ 1 3\n", "3 3\nQ 1 2\nU 1 2\nQ 2 1\n", "6 6\nU 1 2\nU 3 4\nQ 1 4\nU 2 3\nQ 1 4\nQ 5 6\n"], solve_dsu),
    spec("029 Kruskal 最小生成树", "MEDIUM", "给定无向带权图，求最小生成树权值和；若不连通输出 -1。", "第一行 n m，之后 m 行 u v w。", "输出最小生成树权值和或 -1。", "4 5\n1 2 1\n1 3 4\n2 3 2\n2 4 7\n3 4 3\n", ["4 5\n1 2 1\n1 3 4\n2 3 2\n2 4 7\n3 4 3\n", "3 1\n1 2 5\n", "5 7\n1 2 2\n1 3 3\n2 3 1\n2 4 4\n3 4 5\n3 5 6\n4 5 7\n"], solve_mst),
    spec("030 Dijkstra 单源最短路", "MEDIUM", "给定有向非负权图，求起点到每个点的最短路。", "第一行 n m s，之后 m 行 u v w。", "输出从 s 到 1..n 的距离，不可达为 -1。", "4 4 1\n1 2 1\n1 3 4\n2 3 2\n3 4 1\n", ["4 4 1\n1 2 1\n1 3 4\n2 3 2\n3 4 1\n", "3 1 2\n1 3 5\n", "5 6 1\n1 2 2\n1 3 10\n2 3 3\n2 4 5\n3 5 1\n4 5 2\n"], solve_dijkstra),
    spec("031 Floyd 多源最短路", "MEDIUM", "给定无向带权图和若干询问，回答两点间最短路。", "第一行 n m q，之后 m 行 u v w，再之后 q 行 a b。", "对每个询问输出最短距离，不可达为 -1。", "4 4 3\n1 2 1\n2 3 2\n3 4 3\n1 4 10\n1 4\n1 3\n2 4\n", ["4 4 3\n1 2 1\n2 3 2\n3 4 3\n1 4 10\n1 4\n1 3\n2 4\n", "3 1 2\n1 2 5\n1 3\n2 1\n", "5 5 3\n1 2 2\n2 3 2\n3 4 2\n4 5 2\n1 5 20\n1 5\n2 5\n1 4\n"], solve_floyd),
    spec("032 拓扑排序判环", "MEDIUM", "判断一个有向图是否为有向无环图。", "第一行 n m，之后 m 行 u v 表示 u 指向 v。", "无环输出 YES，有环输出 NO。", "4 3\n1 2\n2 3\n1 4\n", ["4 3\n1 2\n2 3\n1 4\n", "3 3\n1 2\n2 3\n3 1\n", "5 4\n1 2\n2 4\n3 4\n4 5\n"], solve_topo_cycle),
    spec("033 0/1 背包", "MEDIUM", "每件物品最多选择一次，在容量限制下最大化价值。", "第一行 n W，之后 n 行 wi vi。", "输出最大价值。", "4 7\n3 4\n4 5\n2 3\n1 2\n", ["4 7\n3 4\n4 5\n2 3\n1 2\n", "3 5\n6 10\n2 3\n3 4\n", "5 10\n2 6\n2 3\n6 5\n5 4\n4 6\n"], solve_zero_one_knapsack),
    spec("034 完全背包", "MEDIUM", "每种物品可选择任意多次，在容量限制下最大化价值。", "第一行 n W，之后 n 行 wi vi。", "输出最大价值。", "3 10\n2 3\n3 4\n5 8\n", ["3 10\n2 3\n3 4\n5 8\n", "2 7\n3 5\n4 6\n", "4 12\n2 2\n5 10\n6 11\n7 13\n"], solve_complete_knapsack),
    spec("035 最长上升子序列", "MEDIUM", "求数组的最长严格上升子序列长度。", "第一行 n，第二行 n 个整数。", "输出 LIS 长度。", "8\n10 9 2 5 3 7 101 18\n", ["8\n10 9 2 5 3 7 101 18\n", "5\n5 4 3 2 1\n", "7\n1 3 5 4 7 6 8\n"], solve_lis),
    spec("036 最长公共子序列", "MEDIUM", "求两个字符串的最长公共子序列长度。", "两行，分别为字符串 A 和 B。", "输出 LCS 长度。", "abcde\nace\n", ["abcde\nace\n", "abc\nabc\n", "AGGTAB\nGXTXAYB\n"], solve_lcs),
    spec("037 编辑距离", "MEDIUM", "求将字符串 A 变成 B 的最少插入、删除、替换次数。", "两行，分别为字符串 A 和 B。", "输出编辑距离。", "kitten\nsitting\n", ["kitten\nsitting\n", "horse\nros\n", "intention\nexecution\n"], solve_edit_distance),
    spec("038 硬币找零", "MEDIUM", "给定若干面额硬币，每种可无限使用，求凑出金额的最少硬币数。", "第一行 n amount，第二行 n 个面额。", "输出最少硬币数，无法凑出输出 -1。", "3 11\n1 2 5\n", ["3 11\n1 2 5\n", "1 3\n2\n", "4 27\n2 5 7 10\n"], solve_min_coins),
    spec("039 区间调度", "EASY", "从若干半开区间 [l,r) 中选择最多个互不重叠区间。", "第一行 n，之后 n 行 l r。", "输出最多可选择的区间数量。", "4\n1 3\n2 4\n3 5\n0 7\n", ["4\n1 3\n2 4\n3 5\n0 7\n", "3\n1 2\n2 3\n3 4\n", "5\n0 10\n1 2\n2 3\n3 4\n4 5\n"], solve_interval_schedule),
    spec("040 贪心找零", "EASY", "使用面额 100、50、20、10、5、1，求找零 n 元的最少张数。", "一行一个整数 n。", "输出最少张数。", "186\n", ["186\n", "0\n", "999\n"], solve_greedy_change),
    spec("041 二叉树层序遍历", "EASY", "给定一棵以 1 为根的树，按层序输出节点编号；同层按编号从小到大扩展。", "第一行 n，之后 n-1 行无向边 u v。", "输出层序遍历序列。", "5\n1 2\n1 3\n2 4\n2 5\n", ["5\n1 2\n1 3\n2 4\n2 5\n", "1\n", "6\n1 3\n1 2\n3 6\n2 4\n2 5\n"], solve_level_order),
    spec("042 二叉搜索树中序", "EASY", "按给定顺序向二叉搜索树插入互不相同的整数，输出中序遍历。", "第一行 n，第二行 n 个互不相同整数。", "输出中序遍历结果。", "5\n5 3 7 2 4\n", ["5\n5 3 7 2 4\n", "4\n1 2 3 4\n", "6\n10 5 1 7 40 50\n"], solve_bst_inorder),
    spec("043 小根堆弹出序列", "EASY", "将所有整数加入小根堆，依次弹出直到为空。", "第一行 n，第二行 n 个整数。", "输出弹出序列。", "5\n3 1 4 1 5\n", ["5\n3 1 4 1 5\n", "4\n9 8 7 6\n", "6\n0 -1 5 -3 2 2\n"], solve_heap_pop),
    spec("044 单词频次统计", "EASY", "统计给定单词的出现次数，按字典序输出。", "第一行 n，之后给出 n 个由小写字母组成的单词。", "每行输出单词和出现次数。", "6\napple banana apple cat banana apple\n", ["6\napple banana apple cat banana apple\n", "4\nz z a b\n", "5\ncsuft oj csuft acm oj\n"], solve_word_count),
    spec("045 KMP 字符串匹配", "MEDIUM", "在文本串中查找模式串出现的所有起始位置。", "第一行模式串，第二行文本串。", "输出所有 1-based 起始位置；不存在输出 -1。", "aba\nababaaba\n", ["aba\nababaaba\n", "aa\naaaaa\n", "xyz\nabcdef\n"], solve_kmp),
    spec("046 Trie 前缀计数", "MEDIUM", "给定若干单词和前缀询问，统计以每个前缀开头的单词数量。", "第一行 n q，之后 n 行单词，再之后 q 行前缀。", "对每个询问输出数量。", "5 3\napple\napp\nape\nbat\nbath\nap\napp\nba\n", ["5 3\napple\napp\nape\nbat\nbath\nap\napp\nba\n", "3 2\na\nab\nabc\na\nabcd\n", "4 3\ncsuft\ncode\ncontest\ncoder\nco\ncod\nx\n"], solve_trie_prefix),
    spec("047 第 k 小元素", "MEDIUM", "给定整数序列，求排序后第 k 小的元素。", "第一行 n k，第二行 n 个整数。", "输出第 k 小元素。", "5 2\n5 1 3 2 4\n", ["5 2\n5 1 3 2 4\n", "6 4\n7 7 1 2 9 3\n", "5 5\n-1 -3 0 2 2\n"], solve_quickselect),
    spec("048 矩阵乘法", "EASY", "计算矩阵 A(n*m) 与 B(m*p) 的乘积。", "第一行 n m p，之后 n 行矩阵 A，再之后 m 行矩阵 B。", "输出 n 行，每行 p 个整数。", "2 3 2\n1 2 3\n4 5 6\n7 8\n9 10\n11 12\n", ["2 3 2\n1 2 3\n4 5 6\n7 8\n9 10\n11 12\n", "1 2 1\n3 4\n5\n6\n", "3 2 3\n1 0\n0 1\n1 1\n1 2 3\n4 5 6\n"], solve_matrix_multiply),
    spec("049 矩阵顺时针旋转", "EASY", "将 n*n 矩阵顺时针旋转 90 度。", "第一行 n，之后 n 行矩阵。", "输出旋转后的矩阵。", "3\n1 2 3\n4 5 6\n7 8 9\n", ["3\n1 2 3\n4 5 6\n7 8 9\n", "1\n42\n", "4\n1 2 3 4\n5 6 7 8\n9 10 11 12\n13 14 15 16\n"], solve_rotate_matrix),
    spec("050 约瑟夫环", "EASY", "n 个人围成一圈，从 1 号开始每数到 k 的人出列，求最后留下的人。", "一行两个整数 n k。", "输出最后留下的编号。", "5 2\n", ["5 2\n", "7 3\n", "1 10\n"], solve_josephus),
]


def refresh_problem(problem: ProblemSpec, dry_run: bool = False) -> int:
    sample_output = problem.solver(problem.sample_input)
    select_sql = f"SELECT id FROM tb_problem WHERE title={sql_string(problem.title)} LIMIT 1;\n"
    existing = run_mysql(select_sql, dry_run=dry_run)

    if dry_run:
        problem_id = 100000 + PROBLEMS.index(problem)
    elif existing.strip():
        problem_id = int(existing.splitlines()[-1])
        update_sql = f"""
UPDATE tb_problem
SET description={sql_string(problem.description)},
    input_description={sql_string(problem.input_description)},
    output_description={sql_string(problem.output_description)},
    sample_input={sql_string(problem.sample_input)},
    sample_output={sql_string(sample_output)},
    difficulty={sql_string(problem.difficulty)},
    time_limit_ms={problem.time_limit_ms},
    memory_limit_kb={problem.memory_limit_kb},
    is_visible=1,
    updated_at=NOW()
WHERE id={problem_id};
DELETE FROM tb_test_case WHERE problem_id={problem_id};
"""
        run_mysql(update_sql)
    else:
        insert_sql = f"""
INSERT INTO tb_problem
    (title, description, input_description, output_description, sample_input, sample_output,
     difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count)
VALUES
    ({sql_string(problem.title)}, {sql_string(problem.description)}, {sql_string(problem.input_description)},
     {sql_string(problem.output_description)}, {sql_string(problem.sample_input)}, {sql_string(sample_output)},
     {sql_string(problem.difficulty)}, {problem.time_limit_ms}, {problem.memory_limit_kb}, NULL, 1, 0, 0);
SELECT LAST_INSERT_ID();
"""
        problem_id = int(run_mysql(insert_sql).splitlines()[-1])

    problem_dir = TESTCASE_BASE / str(problem_id)
    if not dry_run:
        if problem_dir.exists():
            shutil.rmtree(problem_dir)
        problem_dir.mkdir(parents=True, exist_ok=True)

    values = []
    for idx, input_text in enumerate(problem.tests, 1):
        output_text = problem.solver(input_text)
        if not dry_run:
            (problem_dir / f"{idx}.in").write_text(input_text, encoding="utf-8", newline="\n")
            (problem_dir / f"{idx}.out").write_text(output_text, encoding="utf-8", newline="\n")
        values.append(
            f"({problem_id}, {sql_string(f'{idx}.in')}, {sql_string(f'{idx}.out')}, "
            f"{sql_string(input_text[:2048])}, {sql_string(output_text[:2048])}, 0, {idx})"
        )

    if not dry_run:
        run_mysql(
            "INSERT INTO tb_test_case "
            "(problem_id, input_path, output_path, input_preview, output_preview, score, sort_order) VALUES\n"
            + ",\n".join(values)
            + ";\n"
        )
    return problem_id


def validate_specs() -> None:
    if len(PROBLEMS) != 50:
        raise RuntimeError(f"expected 50 problems, got {len(PROBLEMS)}")
    titles = [p.title for p in PROBLEMS]
    duplicates = [t for t, c in Counter(titles).items() if c > 1]
    if duplicates:
        raise RuntimeError(f"duplicate titles: {duplicates}")
    for problem in PROBLEMS:
        if problem.difficulty not in {"EASY", "MEDIUM", "HARD"}:
            raise RuntimeError(f"bad difficulty for {problem.title}")
        problem.solver(problem.sample_input)
        for test in problem.tests:
            problem.solver(test)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true", help="validate specs without touching server state")
    args = parser.parse_args()

    validate_specs()
    if not args.dry_run:
        TESTCASE_BASE.mkdir(parents=True, exist_ok=True)
    ids = [refresh_problem(problem, dry_run=args.dry_run) for problem in PROBLEMS]
    if not args.dry_run:
        subprocess.run(["chown", "-R", "10001:10001", str(TESTCASE_BASE)], check=False)
    print(f"Seeded {len(ids)} classic algorithm problems.")
    print(f"Problem id range: {min(ids)} - {max(ids)}")


if __name__ == "__main__":
    main()
